package com.amazon.aws.am2.appmig.estimate;

import static com.amazon.aws.am2.appmig.constants.IConstants.TAB;

public class CodeMetaData implements Comparable<CodeMetaData> {

	private Integer lineNumber;
	private String statement;
	private String language;

	public CodeMetaData(String statement) {
		this.statement = statement;
	}

	public CodeMetaData(int lineNumber, String statement) {
		this.lineNumber = lineNumber;
		this.statement = statement;
	}
	
	public CodeMetaData(int lineNumber, String statement, String language) {
		this.lineNumber = lineNumber;
		this.statement = statement;
		this.language = language;
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

	/**
	 * 
	 * This is to define the CSS class to beautify the code in the report. Depending
	 * upon the language that is set here, the styling, indentation and coloring is
	 * done for the corresponding code displayed in the report generated. Returns
	 * the language string. Possible values are markup, clike, java, javaScript,
	 * bash, javadoclike, javastacktrace, plsql, powershell, sql, yaml and json5
	 *
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
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
