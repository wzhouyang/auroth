package com.wzy.auroth.thrift.client.config;

import com.facebook.nifty.core.NettyConfigBuilderBase;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * thrift client properties
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "thrift.client")
public class ThriftClientProperties {

    ThriftClientConfig config = new ThriftClientConfig();

    @Getter
    @Setter
    public static class ThriftClientConfig {

        private int connectTimeout = 8000;

        private int receiveTimeout = 2000;

        private int readTimeout = 2000;

        private int sendTimeout = 2000;

        private int maxFrameSize = 16777216;

        private int bossThreadCount = NettyConfigBuilderBase.DEFAULT_BOSS_THREAD_COUNT;

        private int workerThreadCount = NettyConfigBuilderBase.DEFAULT_WORKER_THREAD_COUNT;

        private ThreadType threadType = ThreadType.cache;
    }

    public enum ThreadType {
        cache, fixed
    }
}
