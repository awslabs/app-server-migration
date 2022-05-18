package com.amazon.aws.am2.appmig.checkout;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SVNRepoManager implements IRepoManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(SVNRepoManager.class);

	private ISVNAuthenticationManager authManager;
	private SVNClientManager clientManager;
	private SVNUpdateClient updateClient;

	
	public SVNRepoManager(RepoDetails projDetails) {
		init(projDetails.getUserName(), projDetails.getPassword());
	}
	
	private void init(String svnUserName, String svnPassword )
	{
		DAVRepositoryFactory.setup();
		
		//create authentication data
		authManager = SVNWCUtil.createDefaultAuthenticationManager(svnUserName, svnPassword.toCharArray());
		
		//create client manager and set authentication
		clientManager = SVNClientManager.newInstance();
		clientManager.setAuthenticationManager(authManager);
		//get SVNUpdateClient to do the export
		updateClient = clientManager.getUpdateClient();

	}	
	
	@Override
	public void getSourceCode(String url,String dest) {

		try {
			updateClient.setIgnoreExternals(false);
			updateClient.doCheckout(SVNURL.parseURIEncoded(url), new File(dest), SVNRevision.HEAD, SVNRevision.HEAD,
					SVNDepth.INFINITY, false);
			LOGGER.info("Checked out code successfully from "+url);
		} catch (SVNException e) {
			LOGGER.error("Error message :" + e.getMessage());
		}

	}

}
