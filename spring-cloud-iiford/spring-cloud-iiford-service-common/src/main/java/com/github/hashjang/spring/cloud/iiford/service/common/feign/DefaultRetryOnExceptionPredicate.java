package com.github.hashjang.spring.cloud.iiford.service.common.feign;

import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class DefaultRetryOnExceptionPredicate implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable throwable) {
        boolean shouldRetry = false;
        if (throwable instanceof RetryableException) {
            RetryableException e = (RetryableException) throwable;
            /**
             * 需要验证，这个 e 究竟是我们的 ErrorDecoder 生成的，还是调用 如果直接调用以下这个
             * @see feign.FeignException#errorExecuting
             * 方法封装的 e
             * 如果是调用 errorExecuting 封装的 e，那么会有 cause，目前 feign 只有一个地方调用了这个方法，即：
             * @see feign.SynchronousMethodHandler#executeAndDecode
             * 在 IOException 的时候会调用 errorExecuting 封装异常
             * 对于 IOException，一般有 connect 异常和 read 异常
             * 对于 connect 异常，由于请求还没有发送过去，所以所有的 connect 异常，我们都可以直接重试，不管是不是查询请求
             * 对于 read 异常，由于请求已经发出，对于非查询请求，我们是不能重试的
             * 所以，这里我们需要检查究竟是不是 read 异常，通过 cause 来确认，但是目前没有能区分两种异常的 Exception，他们可能共用一个 Exception 但是 message 不一样
             * 因此，我们采用检查 cause 的方式确认是否是 read异常
             */
            Throwable cause = e.getCause();
            if (cause != null) {
                boolean containsRead = cause.getMessage().toLowerCase().contains("read");
                if (containsRead) {
                    log.info("{}-{} exception contains read, which indicates the request has been sent", e.getMessage(), cause.getMessage());
                    //如果是 read 异常，只有是 query 请求才可以重试
                    shouldRetry = RetryUtil.isQueryRequest(e.request());
                } else {
                    shouldRetry = true;
                }
            } else {
                shouldRetry = true;
            }
        }
        log.info("{} should retry: {}", throwable.getLocalizedMessage(), shouldRetry);
        return shouldRetry;
    }
}
