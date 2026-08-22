package com.resumego.knowledge;

public class KnowledgeImportException extends RuntimeException {

    private final String errorCode;

    public KnowledgeImportException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
