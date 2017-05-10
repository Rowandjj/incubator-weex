package com.taobao.weex.appfram.prerender;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.taobao.weex.IWXRenderListener;
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

/**
 * Description:
 *
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class PreRenderManager {
    private static final String TAG = "PreRenderManager";

    private static final String DEFAULT_VERSION = "1.0";

    private PreRenderManager() {
        mInternalCache = PreRenderCacheFactory.createDefault();
        mRemoteConfig = new FakePreRenderConfig();//TODO 待替换
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


    /**
     * 从缓存中获取{@link WXSDKInstance}实例
     *
     * @param targetUrl 目标url
     * @return 若缓存中存在且足够"新鲜"，则返回{@link WXSDKInstance}实例，否则返回null。
     * */
    @Nullable
    public WXSDKInstance takeCachedInstance(@Nullable String targetUrl) {
        if(!mRemoteConfig.isSwitchOn()) {
            WXLogUtils.d(TAG,"takeCachedInstance failed. switch is off");
            return null;
        }
        if(TextUtils.isEmpty(targetUrl)) {
            return null;
        }
        IPreRenderCache.Entry entry = mInternalCache.remove(targetUrl);
        if(entry != null && entry.data != null && entry.isFresh()) {
            return entry.data;
        } else {
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
    public void renderFromCache(@Nullable Context context, @Nullable WXSDKInstance cachedInstance, @Nullable IWXRenderListener listener) {
        if(!mRemoteConfig.isSwitchOn()) {
            WXLogUtils.d(TAG,"renderFromCache failed. switch is off");
            return;
        }
        if(cachedInstance == null || context == null || listener == null) {
            WXLogUtils.e(TAG,"illegal arguments");
            return;
        }
        if(!cachedInstance.isPreRenderMode()) {
            WXLogUtils.e(TAG, "illegal state");
            return;
        }
        cachedInstance.replaceContext(context);
        cachedInstance.setPreRenderMode(false);

        cachedInstance.registerRenderListener(listener);

        //consume ui tasks
        WXSDKManager.getInstance().getWXDomManager().postRenderTask(cachedInstance.getInstanceId());

        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG,"renderFromCache begin. instance id is "+cachedInstance.getInstanceId());
        }
    }


    /*******************internal usage*********************/

    void addTaskInternal(@NonNull final Context context, @NonNull final String targetUrl, @Nullable final Map<String, Object> options, @Nullable final JSCallback callback) {
        if(!mRemoteConfig.isSwitchOn()) {
            return;
        }
        WXSDKInstance newInstance = new WXSDKInstance(context);
        newInstance.setPreRenderMode(true);
        newInstance.setLayoutFinishListener(new LayoutFinishListener() {
            @Override
            public void onLayoutFinish(@NonNull WXSDKInstance instance) {
                if(isCacheGranted()) {
                    mInternalCache.put(targetUrl, createEntry(instance, options));
                    if (callback != null) {
                        fireEvent(callback,targetUrl,"success","success");
                    }

                    if(WXEnvironment.isApkDebugable()) {
                        WXLogUtils.d(TAG,"preRender success. [targetUrl:"+targetUrl+"]");
                    }

                    //TODO remove later
                    Toast.makeText(context,"create task success",Toast.LENGTH_SHORT).show();
                } else {
                    if(callback != null) {
                        fireEvent(callback,targetUrl,"failed","cache_num_exceed");
                    }

                    if(WXEnvironment.isApkDebugable()) {
                        WXLogUtils.d(TAG,"preRender failed because of exceed max cache num. [targetUrl:"+targetUrl+"]");
                    }

                    //TODO remove later
                    Toast.makeText(context,"create task failed,exceed cache num",Toast.LENGTH_SHORT).show();
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

        newInstance.renderByUrl(targetUrl,targetUrl,null,null, WXRenderStrategy.APPEND_ASYNC);
    }

    private boolean isCacheGranted() {
        int size = mInternalCache.size();
        boolean result = size < mRemoteConfig.getMaxCacheNum();
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d("cacheGranted:"+result+" [current size:"+size+",max size:"+mRemoteConfig.getMaxCacheNum()+"]");
        }
        return result;
    }

    @NonNull
    IPreRenderCache getInternalCache() {
        return mInternalCache;
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

        if (options != null && !options.isEmpty()) {
            for (Map.Entry<String, Object> en : options.entrySet()) {
                if ("ignore_params".equals(en.getKey()) && en.getValue() != null && en.getValue() instanceof List) {
                    //attention:可能存在其他数据类型
                    newEntry.ignoreParams = (List<String>) en.getValue();
                } else if("version".equals(en.getKey()) && en.getValue() != null && en.getValue() instanceof String) {
                    newEntry.version = (String) en.getValue();
                }
            }
        }
        return newEntry;
    }

    private void fireEvent(@NonNull JSCallback callback,@NonNull String url,@NonNull String result,@NonNull String message) {
        Map<String, String> data = new HashMap<>(4);
        data.put("url", url);
        data.put("result", result);
        data.put("message", message);
        callback.invoke(data);
    }

}
