package com.github.hashjang.spring.cloud.iiford.service.common.misc;

/**
 * 一些特殊的
 */
public enum SpecialHttpStatus {
    /**
     * 断路器打开
     */
    CIRCUIT_BREAKER_ON(581),
    ;
    private int value;

    SpecialHttpStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
