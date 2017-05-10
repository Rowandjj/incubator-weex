package com.taobao.weex.appfram.prerender;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

//TODO Test ONLY !!!!!!
public class FakePreRenderConfig implements IPreRenderConfig {
    @Override
    public boolean isSwitchOn() {
        return true;
    }

    @Override
    public long getTTL() {
        return 1000*5;
    }

    @Override
    public int getMaxCacheNum() {
        return DEFAULT_MAX_CACHE_NUM;
    }
}
