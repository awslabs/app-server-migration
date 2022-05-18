package com.amazon.aws.am2.appmig.estimate.exception;

public class UnsupportedRepoException extends Exception{

	private static final long serialVersionUID = 1L;

	public UnsupportedRepoException(String msg) {
		super(msg);
	}

	public UnsupportedRepoException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
