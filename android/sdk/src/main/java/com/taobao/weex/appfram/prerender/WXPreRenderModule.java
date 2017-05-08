package com.taobao.weex.appfram.prerender;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.annotation.JSMethod;
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
 * Created by rowandjj(chuyi)<br/>
 */

public class WXPreRenderModule extends WXSDKEngine.DestroyableModule {

    private static final String TAG = "WXPreRenderModule";

    private static final long DEFAULT_CACHE_TTL = 5 * 60 * 1000;
    private static final int DEFAULT_VERSION = 0;

    private String mTargetUrl;
    private Map<String,Object> mOptions;

    /**
     * @param targetUrl 待preRender的页面
     * @param options   扩展参数 { ignore_params:['foo','bar'], version: 1.0
     *
     *                  }
     * @param callback  callback
     */
    @JSMethod
    public void addTask(@Nullable final String targetUrl, @Nullable final Map<String, Object> options, @Nullable final JSCallback callback) {
        if (TextUtils.isEmpty(targetUrl) || mWXSDKInstance == null || mWXSDKInstance.getContext() == null) {
            WXLogUtils.e(TAG, "add task failed. [url:" + targetUrl + ",instance:" + mWXSDKInstance + "]");
            return;
        }
        this.mTargetUrl = targetUrl;
        this.mOptions = options;

        //TODO if cache exist,return

        WXSDKInstance newInstance = new WXSDKInstance(mWXSDKInstance.getContext());
        newInstance.setPreRenderMode(true);
        newInstance.setLayoutFinishListener(new LayoutFinishListener() {
            @Override
            public void onLayoutFinish(@NonNull WXSDKInstance instance) {
                IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
                try {
                    cache.put(targetUrl, createEntry(instance, options));
                } catch (Exception e) {
                    WXLogUtils.e(TAG, "create cache entry failed.\n" + e.getMessage());
                }

                if (callback != null) {
                    Map<String, String> data = new HashMap<>(4);
                    data.put("status", "success");
                    data.put("target", targetUrl);
                    callback.invoke(data);
                }

                //test
                Toast.makeText(mWXSDKInstance.getContext(),"create task success",Toast.LENGTH_SHORT).show();
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
                    Map<String, String> data = new HashMap<>(4);
                    data.put("status", "failed");
                    data.put("target", targetUrl);
                    data.put("msg", msg);
                    callback.invoke(data);
                }
            }
        });

        //fire
        //todo 参数自定义
        newInstance.renderByUrl("",targetUrl,null,null, WXRenderStrategy.APPEND_ASYNC);
    }

    @Override
    public void destroy() {
        //remove 缓存
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
        //TODO 仅当前instance
        cache.clear();

    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        //refresh
        addTask(mTargetUrl,mOptions,null);
    }

    @NonNull
    private IPreRenderCache.Entry createEntry(@NonNull WXSDKInstance instance, @Nullable Map<String, Object> options) {
        IPreRenderCache.Entry newEntry = new IPreRenderCache.Entry();
        newEntry.data = instance;
        newEntry.ignoreParams = Collections.emptyList();
        newEntry.ttl = DEFAULT_CACHE_TTL;
        newEntry.version = DEFAULT_VERSION;
        newEntry.lastModified = System.currentTimeMillis();

        if (options != null && !options.isEmpty()) {
            for (Map.Entry<String, Object> en : options.entrySet()) {
                if ("ignore_params".equals(en.getKey()) && en.getValue() != null && en.getValue() instanceof List) {
                    newEntry.ignoreParams = (List<String>) en.getValue();
                }
                //TODO extend options
            }
        }
        return newEntry;
    }


}
