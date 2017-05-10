package com.taobao.weex.appfram.prerender;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.utils.WXLogUtils;

import java.util.Map;

/**
 * Description:
 * Weex预渲染组件
 *
 * 文档: https://aone.alibaba-inc.com/task/10627333
 * 需求: https://aone.alibaba-inc.com/task/11163333
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class WXPreRenderModule extends WXSDKEngine.DestroyableModule {

    static final String TAG = "WXPreRenderModule";

    private final ArrayMap<String/*url*/,Params> mCachedParams = new ArrayMap<>();

    /**
     *
     * 添加一个"预渲染"任务。
     *
     * @param targetUrl 待preRender的页面
     * @param options   扩展参数
     * @param callback  回调
     */
    @JSMethod
    @SuppressWarnings("unused")
    public void addTask(@Nullable String targetUrl, @Nullable Map<String, Object> options, @Nullable JSCallback callback) {
        if (TextUtils.isEmpty(targetUrl) || mWXSDKInstance == null || mWXSDKInstance.getContext() == null) {
            WXLogUtils.e(TAG, "add task failed. [url:" + targetUrl + ",instance:" + mWXSDKInstance + "]");
            return;
        }

        mCachedParams.put(targetUrl, new Params(targetUrl,options));//old value will be replaced
        PreRenderManager.getInstance().addTaskInternal(mWXSDKInstance.getContext(),targetUrl,options,callback,false);
    }

    @Override
    public void destroy() {
        //父instance销毁会同时销毁该页面持有的preRenderInstance
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG,"module destroying. [cache params num:"+mCachedParams.size()+"]");
        }
        IPreRenderCache cache = PreRenderManager.getInstance().getInternalCache();
        if(!mCachedParams.isEmpty()) {
            for(String url : mCachedParams.keySet()) {
                cache.remove(url);
            }
            mCachedParams.clear();
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if(WXEnvironment.isApkDebugable()) {
            WXLogUtils.d(TAG,"activity resuming. [cache params num : " + mCachedParams.size()+"]");
        }
        //refresh instance
        if(!mCachedParams.isEmpty()) {
            for (int i = 0; i < mCachedParams.size(); i++) {
                String url = mCachedParams.keyAt(i);
                Params params = mCachedParams.valueAt(i);
                PreRenderManager.getInstance().addTaskInternal(mWXSDKInstance.getContext(),url,params.options,null,true);
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
