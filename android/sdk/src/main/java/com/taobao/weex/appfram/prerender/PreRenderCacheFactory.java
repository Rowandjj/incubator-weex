package com.taobao.weex.appfram.prerender;

import android.support.annotation.NonNull;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class PreRenderCacheFactory {
    private PreRenderCacheFactory(){
    }

    @NonNull
    public static IPreRenderCache createDefault() {
        return new PreRenderCacheImpl();
    }
}
