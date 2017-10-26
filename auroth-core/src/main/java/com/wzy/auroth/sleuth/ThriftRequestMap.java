package com.wzy.auroth.sleuth;

import com.facebook.nifty.core.RequestContexts;
import com.wzy.auroth.core.AurothConstants;
import org.springframework.cloud.sleuth.SpanTextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ThriftRequestMap implements SpanTextMap {

    public ThriftRequestMap() {
        this(new HashMap<>());
    }

    public ThriftRequestMap(Map<String, String> carrier) {
        RequestContexts.getCurrentContext().setContextData(AurothConstants.REQUEST_HEADERS, carrier);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return ((Map<String, String>)RequestContexts.getCurrentContext().getContextData(AurothConstants
                .REQUEST_HEADERS)).entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        ((Map<String, String>)RequestContexts.getCurrentContext().getContextData(AurothConstants
                .REQUEST_HEADERS)).put(key, value);
    }
}
