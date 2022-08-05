package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.REPORT_NAME_SUFFIX;
import static com.amazon.aws.am2.appmig.constants.IConstants.TMPL_REPORT_EXT;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.exception.InvalidPathException;
import com.amazon.aws.am2.appmig.estimate.exception.UnsupportedProjectException;
import com.amazon.aws.am2.appmig.glassviewer.JavaGlassViewer;
import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.db.QueryBuilder;
import com.amazon.aws.am2.appmig.utils.Utility;

/**
 * {@code Estimator} has a template method definition defined in the build
 * method. Need to extend this class to provide custom implementation of a
 * specific method or all of the methods based on the build file
 * 
 * @author agoteti
 *
 */
public abstract class Estimator {

	private final static Logger LOGGER = LoggerFactory.getLogger(Estimator.class);
	protected Map<String, List<String>> files = new HashMap<>();
	protected String basePackage = null;
	protected String src;
	protected String target;
	protected String projectId;
	protected List<String> lstProjects;

	/**
	 * This is a template method which loads the filter, scans the source project
	 * directory and identifies all the files which needs to be analyzed. Analyzes
	 * the files and identifies, what needs to be changed, provides estimates and
	 * complexity of migration in order to migrate the project to the target server
	 * 
	 * @param src
	 * @param target
	 * @throws InvalidPathException
	 * @throws UnsupportedProjectException
	 */
	public void build(String src, String target) throws InvalidPathException, UnsupportedProjectException {
		this.src = src;
		this.target = target;
		IFilter filter = loadFilter();
		scan(Paths.get(src), filter);
		String report_name = "";
		String proj_folder_name = "";
		if(!target.endsWith(TMPL_REPORT_EXT)) {
			Path projFolder = Paths.get(src).getFileName();
			proj_folder_name = projFolder.toString();
			report_name = proj_folder_name + REPORT_NAME_SUFFIX;
		} else {
			Path projFolder = Paths.get(src).getFileName();
			proj_folder_name = projFolder.toString();
		}
		projectId = new JavaGlassViewer().storeProject(proj_folder_name);
		StandardReport report = estimate(projectId);
		// update the complexity of the project
		IAppDiscoveryGraphDB db = AppDiscoveryGraphDB.getInstance();
		db.saveNode(QueryBuilder.updateProjectComplexity(projectId, report.fetchComplexity()));
		generateReport(report, Paths.get(target, report_name));
	}

	/**
	 * Loads the default filter
	 * 
	 * @return {@code com.amazon.aws.am2.appmig.estimate.DefaultFilter}
	 */
	protected IFilter loadFilter() {
		return new DefaultFilter(lstProjects);
	}
	
	protected void scan(Path src, IFilter filter) throws InvalidPathException {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
			ds.forEach(path -> {
				if (filter.filter(path)) {
					process(path, filter);
				} else {
					LOGGER.debug("ignoring {}", path.getFileName().toString());
				}
			});
		} catch (IOException ioe) {
			LOGGER.error("Unable to scan the given path {} due to {}", src, Utility.parse(ioe));
		}
	}

	protected abstract StandardReport estimate(String projectId) throws InvalidPathException, UnsupportedProjectException;

	protected abstract void generateReport(StandardReport report, Path target);
	
	private void process(Path path, IFilter filter) {
		try {
			if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
				scan(path, filter);
			} else {
				String ext = FilenameUtils.getExtension(path.getFileName().toString());
				String absPath = path.toAbsolutePath().toString();
				if (files.containsKey(ext)) {
					List<String> values = files.get(ext);
					values.add(absPath);
				} else {
					List<String> values = new ArrayList<>();
					values.add(absPath);
					files.put(ext, values);
				}
			}
		} catch (InvalidPathException exp) {
			LOGGER.error("Unable to process {} due to {}", path.toString(), Utility.parse(exp));
		}
	}

	protected abstract void setBasePackage(File buildFile);

	public String getSource() {
		return src;
	}

	public String getTarget() {
		return target;
	}

	public List<String> getLstProjects() {
		return lstProjects;
	}

	public void setLstProjects(List<String> lstProjects) {
		this.lstProjects = lstProjects;
	}
	
}
