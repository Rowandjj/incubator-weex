package com.alibaba.weex.extend.module.prerender;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

class PreRenderConfigImpl implements IPreRenderConfig {

//    private IConfigAdapter mAdapter;

    private static final String GROUP = "weex_prerender_config";

    private static final String KEY_IS_SWITCH_ON = "is_switch_on";//true or false
    private static final String KEY_TTL = "ttl";
    private static final String KEY_MAX_CACHE_NUM = "max_cache_num";

    private static final String TAG = "PreRenderConfigImpl";

    PreRenderConfigImpl() {
//        mAdapter = AliWeex.getInstance().getConfigAdapter();
    }

    @Override
    public boolean isSwitchOn() {
        return true;
    }

    @Override
    public long getTTL() {
        return DEFAULT_CACHE_TTL;
    }

    @Override
    public int getMaxCacheNum() {

        return DEFAULT_MAX_CACHE_NUM;
    }
}
