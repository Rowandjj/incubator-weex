package com.taobao.weex.appfram.prerender;

import android.support.annotation.NonNull;

import com.taobao.weex.WXSDKInstance;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

public interface LayoutFinishListener {
    void onLayoutFinish(@NonNull WXSDKInstance instance);
}
