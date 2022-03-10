package com.amazon.aws.am2.appmig.estimate.exception;

public class InvalidPathException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidPathException(String msg) {
	super(msg);
    }

    public InvalidPathException(String msg, Throwable cause) {
	super(msg, cause);
    }

}
