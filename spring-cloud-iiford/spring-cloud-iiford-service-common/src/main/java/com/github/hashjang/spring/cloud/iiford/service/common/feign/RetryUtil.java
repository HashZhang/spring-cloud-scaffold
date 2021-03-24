package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.Request;

import java.util.Objects;

public class RetryUtil {
    public static boolean isQueryRequest(Request request) {
        Request.HttpMethod httpMethod = request.httpMethod();
        if (Objects.equals(httpMethod, Request.HttpMethod.GET)) {
            return true;
        }
        RetryableMethod annotation = request.requestTemplate().methodMetadata().method().getAnnotation(RetryableMethod.class);
        if (annotation == null) {
            annotation = request.requestTemplate().methodMetadata().method().getClass().getAnnotation(RetryableMethod.class);
        }
        //如果类上面或者方法上面有注解，则为查询类型的请求，是可以重试的
        return annotation != null;
    }
}
