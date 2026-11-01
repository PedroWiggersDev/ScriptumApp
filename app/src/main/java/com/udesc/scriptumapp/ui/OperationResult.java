package com.udesc.scriptumapp.ui;

final class OperationResult<T> {
    final T data;
    final Throwable error;

    private OperationResult(T data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    static <T> OperationResult<T> success(T data) {
        return new OperationResult<>(data, null);
    }

    static <T> OperationResult<T> failure(Throwable error) {
        return new OperationResult<>(null, error);
    }
}
