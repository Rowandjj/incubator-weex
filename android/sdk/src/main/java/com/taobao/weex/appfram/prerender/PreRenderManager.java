package com.taobao.weex.appfram.prerender;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.WXEnvironment;
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

    @Nullable
    public static WXSDKInstance takeCachedInstance(@Nullable String targetUrl) {
        if(TextUtils.isEmpty(targetUrl)) {
            return null;
        }
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
        IPreRenderCache.Entry entry = cache.get(targetUrl);
        if(entry != null && entry.data != null && entry.isFresh()) {
            return entry.data;
        } else {
            return null;
        }
    }

    public static void renderFromCache(@Nullable Context context, @Nullable WXSDKInstance cachedInstance, @Nullable IWXRenderListener listener) {
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
    }





    @Deprecated
    public static boolean isCacheAvailable(@Nullable String targetUrl) {
        if(TextUtils.isEmpty(targetUrl)) {
            return false;
        }
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
        IPreRenderCache.Entry entry = cache.get(targetUrl);
        return entry!=null && entry.data != null && entry.isFresh();
    }

    @Deprecated
    @Nullable
    public static WXSDKInstance renderFromCache(@Nullable Context context, @Nullable String targetUrl, @Nullable IWXRenderListener listener) {
        if(TextUtils.isEmpty(targetUrl) || context == null || listener == null) {
            WXLogUtils.e(TAG,"illegal arguments");
            return null;
        }
        IPreRenderCache cache = WXSDKManager.getInstance().getPreRenderCache();
        IPreRenderCache.Entry entry = cache.remove(targetUrl);
        if(entry == null || entry.data == null) {
            return null;
        }

        //TODO 版本校验等等
        WXSDKInstance cachedInstance = entry.data;
        if(!entry.isFresh()) {
            if(WXEnvironment.isApkDebugable()) {
                WXLogUtils.d(TAG,"entry not fresh.["+targetUrl+"]");
            }
            return null;
        }
        cachedInstance.replaceContext(context);
        cachedInstance.setPreRenderMode(false);

        cachedInstance.registerRenderListener(listener);

        //consume ui tasks
        WXSDKManager.getInstance().getWXDomManager().postRenderTask(cachedInstance.getInstanceId());

        return cachedInstance;
    }

}
