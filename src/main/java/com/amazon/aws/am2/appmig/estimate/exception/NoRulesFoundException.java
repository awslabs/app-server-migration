package com.amazon.aws.am2.appmig.estimate.exception;

public class NoRulesFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoRulesFoundException(String msg) {
	super(msg);
    }

    public NoRulesFoundException(String msg, Throwable cause) {
	super(msg, cause);
    }
}
