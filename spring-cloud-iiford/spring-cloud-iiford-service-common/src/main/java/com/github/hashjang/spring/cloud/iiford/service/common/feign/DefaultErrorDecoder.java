package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import static feign.FeignException.errorStatus;

public class DefaultErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        boolean queryRequest = RetryUtil.isQueryRequest(response.request());
        //对于查询请求重试，即抛出可重试异常
        if (queryRequest) {
            throw new RetryableException(response.status(), response.reason(), response.request().httpMethod(), null, response.request());
        } else {
            throw errorStatus(methodKey, response);
        }
    }
}
