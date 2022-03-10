package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.TAB;

public class CodeMetaData implements Comparable<CodeMetaData> {

    private Integer lineNumber;
    private String statement;

    public CodeMetaData(String statement) {
	this.statement = statement;
    }

    public CodeMetaData(int lineNumber, String statement) {
	this.lineNumber = lineNumber;
	this.statement = statement;
    }

    public Integer getLineNumber() {
	return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
	this.lineNumber = lineNumber;
    }

    public String getStatement() {
	return statement;
    }

    public void setStatement(String statement) {
	this.statement = statement;
    }
    
    @Override
    public int hashCode() {
	return (lineNumber != null) ? (lineNumber * statement.hashCode()) : statement.hashCode();
    }
    
    @Override
    public String toString() {
	return lineNumber + TAB + statement;
    }

    @Override
    public int compareTo(CodeMetaData obj) {
	int comparision = -1;
	if (this.lineNumber == null && obj.lineNumber == null) {
	    comparision = 0;
	} else if (obj.lineNumber == null) {
	    comparision = 1;
	} else if (this.lineNumber == null) {
	    comparision = -1;
	} else {
	    comparision = this.lineNumber.compareTo(obj.getLineNumber());
	}
	return comparision;
    }
}
