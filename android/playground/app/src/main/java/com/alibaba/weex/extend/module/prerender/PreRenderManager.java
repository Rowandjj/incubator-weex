package com.alibaba.weex.extend.module.prerender;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.LayoutFinishListener;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXRenderStrategy;
import com.taobao.weex.utils.WXLogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rowandjj(chuyi)<br/>
 */

public class PreRenderManager {
    private static final String TAG = WXPreRenderModule.TAG;

    private static final String DEFAULT_VERSION = "1.0";

    private WXInstanceCreator mWxInstanceCreator;

    private static final String WX_TPL = "_wx_tpl";
    private static final String WH_WX = "wh_weex";

    private PreRenderManager() {
        mInternalCache = new PreRenderCacheImpl();
        mRemoteConfig = new PreRenderConfigImpl();
    }

    @NonNull
    private final IPreRenderCache mInternalCache;
    @NonNull
    private final IPreRenderConfig mRemoteConfig;

    private static PreRenderManager sInstance = null;

    public static PreRenderManager getInstance() {
        if(sInstance == null) {
            synchronized (PreRenderManager.class) {
                if(sInstance == null) {
                    sInstance = new PreRenderManager();
                }
            }
        }
        return sInstance;
    }

    public void setInstanceCreator(@Nullable WXInstanceCreator creator) {
        this.mWxInstanceCreator = creator;
    }

    /**
     * 从缓存中获取{@link WXSDKInstance}实例
     *
     * @param targetUrl 目标url
     * @return 若缓存中存在且足够"新鲜"，则返回{@link WXSDKInstance}实例，否则返回null。
     * */
    @Nullable
    public WXSDKInstance takeCachedInstance(String targetUrl) {
        if(!mRemoteConfig.isSwitchOn()) {
            return null;
        }
        if(TextUtils.isEmpty(targetUrl)) {
            return null;
        }
        IPreRenderCache.Entry entry = mInternalCache.get(targetUrl);
        if(entry != null && entry.data != null && entry.isFresh() && !entry.used) {
            //防止reload时，使用了错误状态的instance
            entry.used = true;
            return entry.data;
        } else {
            if(WXEnvironment.isApkDebugable() && entry!=null) {
                WXLogUtils.d(TAG,"takeCachedInstance return null.[fresh:"+entry.isFresh()+",used:"+entry.used+"]");
            }
            return null;
        }
    }

    /**
     * 渲染缓存中的{@link WXSDKInstance}实例
     *
     * @param context 上下文
     * @param cachedInstance 缓存中的{@link WXSDKInstance}实例，由方法{@link PreRenderManager#takeCachedInstance(String)}返回
     * @param listener 渲染结果的回调
     * */
    public void renderFromCache(Context context, WXSDKInstance cachedInstance, IWXRenderListener listener) {
        if(cachedInstance == null || context == null) {
            WXLogUtils.e(TAG,"illegal arguments");
            return;
        }
        cachedInstance.setRenderStartTime(System.currentTimeMillis());
        if(!mRemoteConfig.isSwitchOn()) {
            WXLogUtils.d(TAG,"renderFromCache failed. switch is off");
            return;
        }
        if(!cachedInstance.isPreRenderMode()) {
            WXLogUtils.e(TAG, "illegal state");
            return;
        }
        cachedInstance.setContext(context);
        cachedInstance.setPreRenderMode(false);

        if(listener != null) {
            cachedInstance.registerRenderListener(listener);
        }

        //consume ui tasks
        WXSDKManager.getInstance().getWXDomManager().postRenderTask(cachedInstance.getInstanceId());

        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG,"renderFromCache begin. instance id is "+cachedInstance.getInstanceId());
        }
    }


    /*******************internal usage*********************/

    @NonNull
    IPreRenderCache getInternalCache() {
        return mInternalCache;
    }

    void addTaskInternal(@NonNull final Context context, @NonNull final String targetUrl, @Nullable final Map<String, Object> options,
                         @Nullable final JSCallback callback, final boolean isResumeState) {
        if(!mRemoteConfig.isSwitchOn()) {
            WXLogUtils.d(TAG,"addTask failed. switch is off");
            return;
        }
        if(!isResumeState && !isCacheGranted()) {
            if(callback != null) {
                fireEvent(callback,targetUrl,"failed","cache_num_exceed");
            }
            if(WXEnvironment.isApkDebugable()) {
                WXLogUtils.d(TAG,"preRender failed because of exceed max cache num. [targetUrl:"+targetUrl+"]");
            }

            return;
        }
        WXLogUtils.d(TAG,"add task begin. url is "+targetUrl);
        WXSDKInstance newInstance = null;
        if(mWxInstanceCreator != null) {
            try {
                newInstance = mWxInstanceCreator.create(context);
                if(WXEnvironment.isApkDebugable()) {
                    WXLogUtils.d(TAG,"create instance use InstanceCreator. [" + newInstance.getClass().getSimpleName()+"]");
                }
            }catch (Exception e) {
                WXLogUtils.e(TAG,e.getMessage());
                newInstance = new WXSDKInstance(context);
            }
        }
        if(newInstance == null) {
            newInstance = new WXSDKInstance(context);
        }
        newInstance.setPreRenderMode(true);
        newInstance.setLayoutFinishListener(new LayoutFinishListener() {
            @Override
            public void onLayoutFinish(@NonNull WXSDKInstance instance) {
                saveEntryToCache(targetUrl,instance,options,isResumeState);
                if (callback != null) {
                    fireEvent(callback,targetUrl,"success","success");
                }

                if(WXEnvironment.isApkDebugable()) {
                    WXLogUtils.d(TAG,"preRender success. [targetUrl:"+targetUrl+"]");
                }
            }
        });

        newInstance.registerRenderListener(new SimpleRenderListener() {

            @Override
            public void onRenderSuccess(WXSDKInstance instance, int width, int height) {
                /*CreateFinishAction已经执行*/
                //Will Never Be Called
            }
            @Override
            public void onException(WXSDKInstance instance, String errCode, String msg) {
                if (callback != null) {
                    fireEvent(callback,targetUrl,"failed",msg);
                }
                if(WXEnvironment.isApkDebugable()) {
                    WXLogUtils.e(TAG,"preRender failed because of "+msg);
                }
            }
        });

        //需要解析原始url，找到jsbundle， targeturl本身可能就是jsbundle
        Uri temp = getBundleUri(targetUrl);
        if(temp != null) {
            String jsBundleUrl = temp.toString();
            if(!TextUtils.isEmpty(jsBundleUrl)) {
                newInstance.renderByUrl(targetUrl,jsBundleUrl,null,null, WXRenderStrategy.APPEND_ASYNC);
            }
        }
    }

    private boolean isCacheGranted() {
        int size = mInternalCache.size();
        boolean result = size < mRemoteConfig.getMaxCacheNum();
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG,"cacheGranted:"+result+" [current size:"+size+",max size:"+mRemoteConfig.getMaxCacheNum()+"]");
        }
        return result;
    }

    private void saveEntryToCache(@NonNull String targetUrl, @NonNull WXSDKInstance instance,
                                  @Nullable Map<String,Object> options, boolean isResumeState) {
        IPreRenderCache.Entry evictEntry = mInternalCache.remove(targetUrl);
        IPreRenderCache.Entry newEntry = createEntry(instance,options);
        if(evictEntry != null && isResumeState) {
            //防止resume时，时间戳被错误更新
            newEntry.lastModified = evictEntry.lastModified;
        }
        mInternalCache.put(targetUrl, newEntry);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private IPreRenderCache.Entry createEntry(@NonNull WXSDKInstance instance, @Nullable Map<String, Object> options) {
        IPreRenderCache.Entry newEntry = new IPreRenderCache.Entry();
        newEntry.data = instance;
        newEntry.ignoreParams = Collections.emptyList();
        newEntry.ttl = mRemoteConfig.getTTL();
        newEntry.version = DEFAULT_VERSION;
        newEntry.lastModified = System.currentTimeMillis();
        newEntry.used = false;

        if (options != null && !options.isEmpty()) {
            for (Map.Entry<String, Object> en : options.entrySet()) {
                if ("ignore_params".equals(en.getKey()) && en.getValue() != null && en.getValue() instanceof List) {
                    //attention:可能存在其他数据类型
                    newEntry.ignoreParams = Collections.unmodifiableList((List<String>) en.getValue());
                } else if("version".equals(en.getKey()) && en.getValue() != null && en.getValue() instanceof String) {
                    newEntry.version = (String) en.getValue();
                }
            }
        }
        return newEntry;
    }

    private void fireEvent(@NonNull JSCallback callback, @NonNull String url, @NonNull String result, @NonNull String message) {
        Map<String, String> data = new HashMap<>(4);
        data.put("url", url);
        data.put("result", result);
        data.put("message", message);
        callback.invoke(data);
    }

    public interface WXInstanceCreator{
        @NonNull
        WXSDKInstance create(@NonNull Context context);
    }

    @Nullable
    private static Uri getBundleUri(@NonNull String url) {
        Uri uri = Uri.parse(url);
        if (uri.getBooleanQueryParameter(WH_WX, false)) {
            return uri;
        }
        String bundleUrl = uri.getQueryParameter(WX_TPL);
        if (!TextUtils.isEmpty(bundleUrl)) {
            Uri bundleUri = Uri.parse(bundleUrl);
            if (bundleUri != null) {
                Set<String> keys = uri.getQueryParameterNames();
                Uri.Builder builder = bundleUri.buildUpon();
                for (String key : keys) {
                    if (!TextUtils.equals(key, WX_TPL)) {
                        String value = uri.getQueryParameter(key);
                        builder.appendQueryParameter(key, value);
                    }
                }
                return builder.build();
            }
        }
        return null;
    }
}
