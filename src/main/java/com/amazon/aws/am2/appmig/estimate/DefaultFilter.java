package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_BUILD;
import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_TARGET;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_CLASS;
import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_SETTINGS;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_CLASSPATH;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

public class DefaultFilter implements IFilter {

    private String[] arrFileFilter = { EXT_CLASS, EXT_CLASSPATH };
    private String[] arrDirFilter = { DIR_BUILD, DIR_TARGET, DIR_SETTINGS };

    public boolean filter(Path path) {
	boolean consider = true;
	try {
	    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
		consider = applyFilterOnDir(path);
	    } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
		consider = applyFilterOnFile(path);
	    } else {
		consider = false;
	    }
	} catch (Exception e) {
	    consider = false;
	}
	return consider;
    }

    private boolean applyFilterOnFile(Path path) {
	String ext = FilenameUtils.getExtension(path.getFileName().toString());
	boolean value = Arrays.stream(arrFileFilter).anyMatch(ele -> {
	    return ext.equals(ele);
	});
	return !value;
    }

    private boolean applyFilterOnDir(Path path) {
	String dirName = path.getFileName().toString();
	boolean value = Arrays.stream(arrDirFilter).anyMatch(ele -> {
	    return dirName.contentEquals(ele);
	});
	return !value;
    }
}
