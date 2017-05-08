package com.taobao.weex.appfram.prerender;

import com.taobao.weex.WXSDKInstance;

import java.util.List;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

public interface IPreRenderCache {
    Entry get(String key);
    void put(String key, Entry entry);
    Entry remove(String key);
    void clear();

    class Entry {
        public WXSDKInstance data;
        public List<String> ignoreParams;
        public int version;
        public long ttl;
        public long lastModified;
        public boolean isFresh() {
            return (System.currentTimeMillis()-lastModified) <= ttl;
        }
    }
}
