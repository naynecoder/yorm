package org.yorm.exception;

public class YormException extends Exception {

    public YormException(String cause) {
        super(cause);
    }

    public YormException(String message, Throwable cause) {
        super(message, cause);
    }
}
