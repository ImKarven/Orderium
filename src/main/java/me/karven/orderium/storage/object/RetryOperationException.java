package me.karven.orderium.storage.object;

public class RetryOperationException extends RuntimeException {
    public RetryOperationException(String message) {
        super(message);
    }
    public RetryOperationException() {
        super();
    }
}
