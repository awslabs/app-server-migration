package com.amazon.aws.am2.appmig.estimate.exception;

public class UnsupportedProjectException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsupportedProjectException(String msg) {
	super(msg);
    }

    public UnsupportedProjectException(String msg, Throwable cause) {
	super(msg, cause);
    }

}
