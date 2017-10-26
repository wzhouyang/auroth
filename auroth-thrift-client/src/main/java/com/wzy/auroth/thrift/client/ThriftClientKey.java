package com.wzy.auroth.thrift.client;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.thrift.TServiceClient;
import org.springframework.util.StringUtils;

/**
 * ThriftClientKey
 *
 * @since 1.0.0
 */
@Builder
@EqualsAndHashCode
@ToString
public class ThriftClientKey {

    private String serviceId;

    @Getter
    private String host;

    @Getter
    private int port;

    @Getter
    private Class<? extends TServiceClient> clazz;

    public String getServiceId() {
        if (StringUtils.isEmpty(serviceId))
            return WordUtils.uncapitalize(clazz.getEnclosingClass().getSimpleName());
        return serviceId;
    }

    public boolean validate() {
        return !StringUtils.isEmpty(serviceId) || !StringUtils.isEmpty(host);
    }

    public String getProtocolServiceName() {
        return this.clazz.getDeclaringClass().getSimpleName();
    }
}
