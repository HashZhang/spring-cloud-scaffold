package com.github.hashjang.spring.cloud.iiford.service.common.undertow;

import io.undertow.Undertow;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.undertow.UndertowWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "server.undertow.accesslog.pattern=%D" })
public class TestAccessLog implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    public static class SampleConfig {}

    @Test
    public void testLogContainsResponseTime() throws NoSuchFieldException {
//        Field undertow = ReflectionUtils.findField(UndertowWebServer.class, "undertow");
//        ReflectionUtils.makeAccessible(undertow);
//        Undertow field = (Undertow) ReflectionUtils.getField(undertow, undertowWebServer);
//        Field serverOptions = ReflectionUtils.findField(Undertow.class, "serverOptions");
//        ReflectionUtils.makeAccessible(serverOptions);
//        Object field1 = ReflectionUtils.getField(serverOptions, field);

        System.out.println();
    }
}
