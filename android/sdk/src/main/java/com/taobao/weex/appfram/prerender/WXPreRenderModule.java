package com.taobao.weex.appfram.prerender;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.utils.WXLogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class WXPreRenderModule extends WXSDKEngine.DestroyableModule {

    private static final String TAG = "WXPreRenderModule";

    private final List<Params> mCachedParams = new ArrayList<>();

    /**
     * @param targetUrl 待preRender的页面
     * @param options   扩展参数 { ignore_params:['foo','bar'], version: 1.0
     *
     *                  }
     * @param callback  callback
     */
    @JSMethod
    @SuppressWarnings("unused")
    public void addTask(@Nullable String targetUrl, @Nullable Map<String, Object> options, @Nullable JSCallback callback) {
        if (TextUtils.isEmpty(targetUrl) || mWXSDKInstance == null || mWXSDKInstance.getContext() == null) {
            WXLogUtils.e(TAG, "add task failed. [url:" + targetUrl + ",instance:" + mWXSDKInstance + "]");
            return;
        }

        mCachedParams.add(new Params(targetUrl,options));
        //TODO if cache exist,return
        PreRenderManager.getInstance().addTaskInternal(mWXSDKInstance.getContext(),targetUrl,options,callback);
    }

    @Override
    public void destroy() {
        //remove 缓存
        IPreRenderCache cache = PreRenderManager.getInstance().getInternalCache();
        if(!mCachedParams.isEmpty()) {
            for(Params params : mCachedParams) {
                cache.remove(params.targetUrl);
            }
            mCachedParams.clear();
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        //refresh
        if(!mCachedParams.isEmpty()) {
            for(Params params : mCachedParams) {
                PreRenderManager.getInstance().addTaskInternal(mWXSDKInstance.getContext(),params.targetUrl,params.options,null);
            }
        }
    }

    private static class Params {
        String targetUrl;
        Map<String,Object> options;

        Params(String targetUrl, Map<String,Object> options) {
            this.targetUrl = targetUrl;
            this.options = options;
        }
    }
}
