package com.taobao.weex.appfram.prerender;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.utils.WXLogUtils;

/**
 * Description:
 *
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class PreRenderManager {
    private static final String TAG = "PreRenderManager";

    private PreRenderManager() {
    }

    public static boolean isCacheExist(@Nullable String targetUrl) {
        if(TextUtils.isEmpty(targetUrl)) {
            return false;
        }
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
        return cache.get(targetUrl) != null;
    }

    @Nullable
    public static WXSDKInstance renderFromCache(@Nullable Context context, @Nullable String targetUrl, @Nullable IWXRenderListener listener) {
        if(TextUtils.isEmpty(targetUrl) || context == null || listener == null) {
            WXLogUtils.e(TAG,"illegal arguments");
            return null;
        }
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
//        IPreRenderCache.Entry entry = cache.get(targetUrl);
//        if(!entry.isFresh()) {
//            cache.remove(targetUrl);
//            listener.onException(entry.data,"-1","instance is not fresh");
//            return null;
//        }
        IPreRenderCache.Entry entry = cache.remove(targetUrl);

        //TODO 版本校验等等
        WXSDKInstance cachedInstance = entry.data;
        cachedInstance.replaceContext(context);
        cachedInstance.setPreRenderMode(false);

        cachedInstance.registerRenderListener(listener);

        //consume ui tasks
        WXSDKManager.getInstance().getWXDomManager().postRenderTask(cachedInstance.getInstanceId());

        //remove cache

        return cachedInstance;
    }

}
