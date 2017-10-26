package com.wzy.auroth.thrift.annotation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * TReference meta data
 *
 * @see com.wzy.auroth.thrift.annotation.TReference
 */
@Getter
@ToString
@EqualsAndHashCode
public class TReferenceMeta {

    private final String serviceId;

    private final String host;

    private final int port;

    private final Class clazz;

    public TReferenceMeta(TReference reference, Class clazz) {
        this.serviceId = reference.serviceId();
        this.host = reference.host();
        this.port = reference.port();
        this.clazz = clazz;
    }
}
