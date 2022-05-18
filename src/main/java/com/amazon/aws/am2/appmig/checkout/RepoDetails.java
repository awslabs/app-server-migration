package com.amazon.aws.am2.appmig.checkout;

public class RepoDetails {

	private RepoType repoType;
	private String userName;
	private String password;

	
	private RepoDetails() {
		
	}
	
	public RepoType getRepoType() {
		return repoType;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	
	 public static class Builder {
			private RepoType repoType;
			private String userName;
			private String password;
		
	        public static Builder newInstance()
	        {
	            return new Builder();
	        }
	  
	        private Builder() {}
	        
	        public Builder repoType(RepoType repoType) {
	        	this.repoType = repoType;
	        	return this;
	        }
	        
	        public Builder userName(String userName) {
	        	this.userName = userName;
	        	return this;
	        }
	        
	    	
	        public Builder password(String password) {
	        	this.password = password;
	        	return this;
	        }

	        
	        public RepoDetails build() {
	        	RepoDetails repoDetails = new RepoDetails();
	        	
	        	repoDetails.repoType = repoType;
	        	repoDetails.userName = userName;
	        	repoDetails.password = password;

	        	return repoDetails;
	        }
	 }
}
