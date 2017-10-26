package com.wzy.auroth.thrift.server.config;

import com.facebook.nifty.core.NettyConfigBuilderBase;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "thrift.server")
@Getter
@Setter
public class ThriftServerProperties {

    private Integer port;

    private ProtocolType protocolType = ProtocolType.BINARY;

    private ThriftServerConfig config = new ThriftServerConfig();

    public enum ProtocolType {
        BINARY;
    }

    @Setter
    @Getter
    public static class ThriftServerConfig {

        private int bossThreadCount = NettyConfigBuilderBase.DEFAULT_BOSS_THREAD_COUNT;

        private int workerThreadCount = NettyConfigBuilderBase.DEFAULT_WORKER_THREAD_COUNT;

        private ThreadType threadType = ThreadType.cache;

        private Map<String, Object> options = new HashMap<>();
    }

    public enum ThreadType {
        cache, fixed;
    }
}
