package com.amazon.aws.am2.appmig.utils;

import java.io.File;
import java.io.FilenameFilter;
import static com.amazon.aws.am2.appmig.constants.IConstants.RULE_FILE_PREFIX;

public class RuleFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
	if (name.startsWith(RULE_FILE_PREFIX)) {
	    return true;
	} else {
	    return false;
	}
    }

}
