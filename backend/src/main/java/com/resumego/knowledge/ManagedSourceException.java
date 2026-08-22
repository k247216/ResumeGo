package com.resumego.knowledge;

/** 受管原文查询的稳定失败分类。 */
public class ManagedSourceException extends RuntimeException {

    private final String code;

    public ManagedSourceException(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
