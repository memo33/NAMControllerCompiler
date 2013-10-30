package controller;

import java.util.regex.Pattern;

final class PatternAttribute {

    final Pattern pattern;
    final String[] requiredSiblingNodes;
    
    PatternAttribute(String regex, String... requiredSiblingNodes) {
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.requiredSiblingNodes = requiredSiblingNodes;
    }
}
