package com.amazon.aws.am2.appmig.estimate.exception;

public class InvalidRuleException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidRuleException(String msg) {
	super(msg);
    }

    public InvalidRuleException(String msg, Throwable cause) {
	super(msg, cause);
    }

}
