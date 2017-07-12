package com.alibaba.weex.extend.module.prerender;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

class PreRenderCacheImpl implements IPreRenderCache {

    @NonNull
    private final ConcurrentHashMap<String,Entry> mInternalCache;
    private static final int DEFAULT_CAPACITY = 8;

    PreRenderCacheImpl() {
        mInternalCache = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
    }

    @Override
    public Entry get(String key) {
        if(TextUtils.isEmpty(key)) {
            return null;
        }
        return mInternalCache.get(key);
    }

    @Override
    public void put(String key, Entry entry) {
        if(TextUtils.isEmpty(key) || entry == null) {
            return;
        }
        mInternalCache.put(key,entry);
    }

    @Override
    public Entry remove(String key) {
        if(TextUtils.isEmpty(key)) {
            return null;
        }
        return mInternalCache.remove(key);
    }

    @Override
    public int size() {
        return mInternalCache.size();
    }

    @Override
    public void clear() {
        mInternalCache.clear();
    }
}
