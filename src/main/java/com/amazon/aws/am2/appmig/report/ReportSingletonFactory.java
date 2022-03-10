package com.amazon.aws.am2.appmig.report;

import com.amazon.aws.am2.appmig.estimate.StandardReport;

public class ReportSingletonFactory {

    private static final ReportSingletonFactory INSTANCE = new ReportSingletonFactory();
    private static StandardReport stdReport;

    private ReportSingletonFactory() {
    }

    public static ReportSingletonFactory getInstance() {
	return INSTANCE;
    }

    public synchronized StandardReport getStandardReport() {
	if (stdReport == null) {
	    stdReport = new StandardReport();
	}
	return stdReport;
    }
}
