package com.amazon.aws.am2.appmig.search;

public interface ISearch {

    public boolean find(String pattern, String source, boolean caseInSensitive);

}
