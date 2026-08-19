package com.resumego.ai;

final class ProviderHttpException extends RuntimeException {
    private final int status;

    ProviderHttpException(int status) {
        super("Provider HTTP " + status);
        this.status = status;
    }

    int status() {
        return status;
    }
}
