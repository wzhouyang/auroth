package com.wzy.auroth.thrift.utils;

import com.wzy.auroth.thrift.annotation.TReferenceMeta;
import org.springframework.core.NamedThreadLocal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * annotation utils
 */
public class ThriftUtils {

    private static final Map<Class, TReferenceMeta> REFERENCE_META_MAP = new ConcurrentHashMap<>(256);

    public static final ThreadLocal<TReferenceMeta> REFERENCE_META_THREAD_LOCAL = new NamedThreadLocal<>
            ("TReferenceMetaThreadLocal");

    public static void addReferenceMeta(TReferenceMeta referenceMeta) {
        REFERENCE_META_MAP.put(referenceMeta.getClazz(), referenceMeta);
    }

    public static TReferenceMeta getReferenceMeta(Class clazz) {
        return REFERENCE_META_MAP.get(clazz);
    }
}
