package com.wzy.auroth.thrift.client.pool;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;

public class ThriftClientPooledObject<T extends TServiceClient> extends DefaultPooledObject<T> {

    /**
     * Create a new instance that wraps the provided object so that the pool can
     * track the state of the pooled object.
     *
     * @param object The object to wrap
     */
    public ThriftClientPooledObject(T object) {
        super(object);
    }
}
