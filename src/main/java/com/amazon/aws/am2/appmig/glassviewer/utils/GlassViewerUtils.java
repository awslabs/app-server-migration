package com.amazon.aws.am2.appmig.glassviewer.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlassViewerUtils {

	private final static Logger LOGGER = LoggerFactory.getLogger(GlassViewerUtils.class);

	public static String parse(Throwable exp) {
		String strException = null;
		try (StringWriter sw = new StringWriter()) {
			exp.printStackTrace(new PrintWriter(sw));
			strException = sw.toString();
		} catch (IOException ioe) {
			LOGGER.error("Got exception while converting Throwable object to String due to {} ", ioe.getMessage());
		}
		return strException;
	}
	
}
