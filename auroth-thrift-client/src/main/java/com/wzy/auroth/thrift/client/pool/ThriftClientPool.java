package com.wzy.auroth.thrift.client.pool;

import com.wzy.auroth.thrift.client.ThriftClientKey;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;

public class ThriftClientPool extends GenericKeyedObjectPool<ThriftClientKey, TServiceClient> {

    public ThriftClientPool(KeyedPooledObjectFactory<ThriftClientKey, TServiceClient> factory) {
        super(factory);
    }

    public ThriftClientPool(KeyedPooledObjectFactory<ThriftClientKey, TServiceClient> factory, GenericKeyedObjectPoolConfig config) {
        super(factory, config);
    }
}
