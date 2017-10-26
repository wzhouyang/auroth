package com.wzy.auroth.thrift.client.concurrency;

import com.wzy.auroth.thrift.annotation.TReferenceMeta;
import com.wzy.auroth.thrift.utils.ThriftUtils;

public class DefaultReferenceMetaProvider implements ReferenceMetaProvider {

    @Override
    public TReferenceMeta getReferenceMeta() {
        return ThriftUtils.REFERENCE_META_THREAD_LOCAL.get();
    }

}
