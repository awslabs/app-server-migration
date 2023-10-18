package com.amazon.aws.am2.appmig.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSearch implements ISearch {

    @Override
    public boolean find(String strPattern, String source, boolean caseInSensitive) {
        Pattern pattern;
        if (caseInSensitive) {
            pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        } else {
            pattern = Pattern.compile(strPattern);
        }
        Matcher matcher = pattern.matcher(source);
        return matcher.find();
    }
}
