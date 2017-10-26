package com.wzy.auroth.thrift.client.exception;

public class ThriftClientException extends Exception {

    public ThriftClientException() {
        super();
    }

    public ThriftClientException(String message) {
        super(message);
    }

    public ThriftClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThriftClientException(Throwable cause) {
        super(cause);
    }

    protected ThriftClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
