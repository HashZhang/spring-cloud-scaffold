package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.MethodMetadata;
import feign.Request;
import feign.RequestTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

/**
 * Util 类静态方法测试
 */
public class OpenfeignUtilTest {
    public static class SimpleClass {
        public void testSimple() {}
        @RetryableMethod
        public void testAnnotated() {}
    }

    @RetryableMethod
    public static class AnnotatedClass {
        public void testSimple() {}
    }

    @Test
    public void testGetMethod() {
        Request request = Mockito.mock(Request.class);
        Mockito.when(request.httpMethod()).thenReturn(Request.HttpMethod.GET);
        Assert.assertTrue(OpenfeignUtil.isRetryableRequest(request));
    }

    private Request getPostRequest(Method method) {
        Request request = Mockito.mock(Request.class);
        Mockito.when(request.httpMethod()).thenReturn(Request.HttpMethod.POST);
        RequestTemplate requestTemplate = Mockito.mock(RequestTemplate.class);
        Mockito.when(request.requestTemplate()).thenReturn(requestTemplate);
        MethodMetadata methodMetadata = Mockito.mock(MethodMetadata.class);
        Mockito.when(requestTemplate.methodMetadata()).thenReturn(methodMetadata);
        Mockito.when(methodMetadata.method()).thenReturn(method);
        return request;
    }

    @Test
    public void testPostMethod() throws Exception {
        Request testSimple = getPostRequest(SimpleClass.class.getMethod("testSimple"));
        Assert.assertFalse(OpenfeignUtil.isRetryableRequest(testSimple));
    }

    @Test
    public void testAnnotatedMethod() throws Exception {
        Request testAnnotated = getPostRequest(SimpleClass.class.getMethod("testAnnotated"));
        Assert.assertTrue(OpenfeignUtil.isRetryableRequest(testAnnotated));
    }

    @Test
    public void testAnnotatedClass() throws Exception {
        Request testSimple = getPostRequest(AnnotatedClass.class.getMethod("testSimple"));
        Assert.assertTrue(OpenfeignUtil.isRetryableRequest(testSimple));
    }
}