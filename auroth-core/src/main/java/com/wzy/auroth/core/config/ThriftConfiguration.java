package com.wzy.auroth.core.config;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThriftConfiguration {

    @Bean
    @ConditionalOnMissingBean(TProtocolFactory.class)
    public TProtocolFactory thriftProtocolFactory() {
        return new TBinaryProtocol.Factory();
    }
}
