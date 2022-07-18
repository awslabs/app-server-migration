package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_BUILD;
import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_TARGET;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_CLASS;
import static com.amazon.aws.am2.appmig.constants.IConstants.DIR_SETTINGS;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_CLASSPATH;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_GIT;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_DS_STORE;
import static com.amazon.aws.am2.appmig.constants.IConstants.EXT_PROJECT;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;

public class DefaultFilter implements IFilter {

	private String[] arrFileFilter = { EXT_CLASS, EXT_CLASSPATH, EXT_GIT, EXT_DS_STORE, EXT_PROJECT };
	private String[] arrDirFilter = { DIR_BUILD, DIR_TARGET, DIR_SETTINGS };
	private final List<String> subProjDirsFilter;

	public DefaultFilter() {
		subProjDirsFilter = new ArrayList<String>();
	}
	public DefaultFilter(List<String> subProjDirsFilter) {
		this.subProjDirsFilter = new ArrayList<String>(subProjDirsFilter);
	}
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
		String dirAbsPath = path.getParent()+File.separator+dirName;
		boolean value1 = Arrays.stream(arrDirFilter).anyMatch(ele -> {
			return dirName.contentEquals(ele);
		});
		// The below filtering process filters the sub projects if they are listed as maven projects 
		boolean value2 = subProjDirsFilter.stream().anyMatch(ele -> {
			return dirAbsPath.equals(ele);
		});
		return !(value1 || value2);
	}
}
