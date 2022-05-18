package com.amazon.aws.am2.appmig.checkout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.aws.am2.appmig.estimate.Main;
import com.amazon.aws.am2.appmig.estimate.exception.UnsupportedRepoException;
import com.opencsv.bean.CsvToBeanBuilder;

public class SourceCodeManager {
	private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static String downloadCode(String configFilePath, String target) throws Exception {
		List<ConfigurationBean> beans;
		FileReader configReader = null;
		try {
			configReader = new FileReader(configFilePath);
			beans = new CsvToBeanBuilder<ConfigurationBean>(configReader)
					.withType(ConfigurationBean.class)
					.withSkipLines(1)//Skip header of CSV File
					.build()
					.parse();
		} catch (IllegalStateException | FileNotFoundException e) {
			LOGGER.error("Error while reading configuration : " + e.getMessage());
			throw e;
		} finally {
			if (configReader != null) {
				configReader.close();
			}
		}
		for (ConfigurationBean bean : beans) {
			RepoDetails details = RepoDetails.Builder.newInstance()
											.repoType(RepoType.valueOf(bean.getRepoType()))
											.userName(bean.getUserName())
											.password(bean.getPassword())
											.build();
			IRepoManager repomgr = RepoManagerFactory.getRepoManager(details);
			if(repomgr != null) {
				repomgr.getSourceCode(bean.getRepoUrl(),target + File.separator + bean.getProjectName());
			}
			else {
				throw new UnsupportedRepoException("Repo type "+bean.getRepoType()+" is not supported ");
			}
		}
		return target;
	}

}
