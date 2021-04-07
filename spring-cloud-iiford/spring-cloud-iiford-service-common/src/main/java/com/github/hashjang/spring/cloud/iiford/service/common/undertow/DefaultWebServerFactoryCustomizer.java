package com.github.hashjang.spring.cloud.iiford.service.common.undertow;

import io.undertow.UndertowOptions;
import io.undertow.attribute.ResponseTimeAttribute;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

public class DefaultWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory> {

    private final ServerProperties serverProperties;

    public DefaultWebServerFactoryCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableUndertowWebServerFactory factory) {
        String pattern = serverProperties.getUndertow().getAccesslog().getPattern();
        // 如果 accesslog 配置中打印了响应时间，则打开记录请求开始时间配置
        if (logRequestProcessingTiming(pattern)) {
            factory.addBuilderCustomizers(builder -> builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true));
        }
    }

    private boolean logRequestProcessingTiming(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        //判断 accesslog 是否配置了查看响应时间
        return pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MICROS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MILLIS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_NANOS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MILLIS_SHORT)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_SECONDS_SHORT);
    }
}
