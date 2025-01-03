package com.amazon.aws.am2.appmig.estimate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class DefaultFilter implements IFilter {

    private final String[] arrFileFilter = {EXT_CLASS, EXT_CLASSPATH, EXT_GIT, EXT_DS_STORE, EXT_PROJECT};
    private final String[] arrDirFilter = {DIR_BUILD, DIR_TARGET, DIR_SETTINGS, DIR_MVN};
    private final List<String> subProjDirsFilter;
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultFilter.class);

    public DefaultFilter(List<String> subProjDirsFilter) {
        this.subProjDirsFilter = new ArrayList<>(subProjDirsFilter);
    }

    public boolean filter(Path path) {
        boolean consider;
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
        try {
            if (Files.isHidden(path)) {
                return false;
            }
        } catch (IOException ioe) {
            LOGGER.warn("!Ignoring filter for this file due to unable to read the attributes of the file {} due to {}", path, ioe.getMessage());
        }
        String ext = FilenameUtils.getExtension(path.getFileName().toString());
        boolean value = Arrays.asList(arrFileFilter).contains(ext);
        return !value;
    }

    private boolean applyFilterOnDir(Path path) {
        String dirName = path.getFileName().toString();
        String dirAbsPath = path.getParent() + File.separator + dirName;
        boolean value1 = Arrays.stream(arrDirFilter).anyMatch(dirName::contentEquals);
        // The below filtering process filters the subprojects if they are listed as maven projects
        boolean value2 = subProjDirsFilter.stream().anyMatch(dirAbsPath::equals);
        return !(value1 || value2);
    }
}
