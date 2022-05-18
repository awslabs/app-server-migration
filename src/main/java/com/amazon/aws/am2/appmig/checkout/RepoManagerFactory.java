package com.amazon.aws.am2.appmig.checkout;

public class RepoManagerFactory {

	 public static IRepoManager getRepoManager(RepoDetails repoDetails) {
		 
		 IRepoManager repoMgr = null;
		 
		switch(repoDetails.getRepoType()){
		case SVN:
			repoMgr = new SVNRepoManager(repoDetails); 
		default:
			break;
			
		}
		return repoMgr;
	 }
}
