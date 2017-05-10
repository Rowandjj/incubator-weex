package com.taobao.weex.appfram.prerender;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

interface IPreRenderConfig {
    long DEFAULT_CACHE_TTL = 5 * 60 * 1000;//ms
    int DEFAULT_MAX_CACHE_NUM = 1;

    /**开关*/
    boolean isSwitchOn();

    /**缓存失效周期,默认5min*/
    long getTTL();

    /**缓存实例数目,默认1个*/
    int getMaxCacheNum();
}
