package com.alibaba.weex.extend.module.prerender;

import android.view.View;

import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.WXSDKInstance;

public class SimpleRenderListener implements IWXRenderListener {

    @Override
    public void onViewCreated(WXSDKInstance instance, View view) {
    }

    @Override
    public void onRenderSuccess(WXSDKInstance instance, int width, int height) {
    }

    @Override
    public void onRefreshSuccess(WXSDKInstance instance, int width, int height) {
    }

    @Override
    public void onException(WXSDKInstance instance, String errCode, String msg) {
    }
}