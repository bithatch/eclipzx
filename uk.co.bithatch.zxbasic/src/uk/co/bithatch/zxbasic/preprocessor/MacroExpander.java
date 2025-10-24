package uk.co.bithatch.zxbasic.preprocessor;

import java.util.*;
import java.util.regex.*;

public class MacroExpander {

    private static final Pattern PARAM_MACRO_PATTERN = Pattern.compile("^(\\w+)\\((.*?)\\)$");
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "\\s+|\"(?:\\\\.|[^\"\\\\])*\"|\\b\\w+\\b|[^\\s\\w\"]+");

    private static final Set<String> COMMENT_KEYWORDS = Set.of("REM");

    private static final int MAX_RECURSION_DEPTH = 10;

    public static String expandLine(String line, Map<String, String> defines) {
        return expandLine(line, defines, 0);
    }

    private static String expandLine(String line, Map<String, String> defines, int depth) {
        if (depth > MAX_RECURSION_DEPTH) return line; // prevent infinite recursion

    	
    	String trimmed =line.trim();
    	if(trimmed.startsWith("'") || trimmed.startsWith("REM")) {
    		return line;
    	}
    	
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inMultilineComment = false;
        String quoteChar = null;

        int index = 0;
        while (index < line.length()) {
            Matcher matcher = TOKEN_PATTERN.matcher(line);
            if (!matcher.find(index)) break;

            String token = matcher.group();
            int start = matcher.start();

            if (inMultilineComment) {
                result.append(token);
                if (token.equals("'/")) {
                    inMultilineComment = false;
                }
                index = matcher.end();
                continue;
            }

            if (!inString && token.equals("/'")) {
                inMultilineComment = true;
                result.append(token);
                index = matcher.end();
                continue;
            }

            if (!inString && (token.equals("\""))) {
                inString = true;
                quoteChar = token;
                result.append(token);
                index = matcher.end();
                continue;
            }

            if (inString) {
                result.append(token);
                if (token.equals(quoteChar)) {
                    inString = false;
                }
                index = matcher.end();
                continue;
            }

            if (!inString && !inMultilineComment && token.contains("'")) {
                int commentIdx = line.indexOf('\'', matcher.start());
                if (commentIdx != -1) {
                    result.append(line, matcher.start(), commentIdx);
                    result.append(line.substring(commentIdx));
                    break;
                }
            }
            if (COMMENT_KEYWORDS.contains(token.toUpperCase())) {
                result.append(line.substring(start));
                break;
            }

            if (token.trim().isEmpty()) {
                result.append(token);
                index = matcher.end();
                continue;
            }

            // Handle parameterized macros with lookahead
            if (Character.isLetter(token.charAt(0))) {
                String lookahead = line.substring(matcher.end()).trim();
                if (lookahead.startsWith("(")) {
                    int endParen = findMatchingParen(line, matcher.end());
                    if (endParen > 0) {
                        String callArgs = line.substring(matcher.end(), endParen + 1).trim();
                        String fullCall = token + callArgs;
                        String macroBody = expandParameterizedMacro(fullCall, defines, depth + 1);
                        if (macroBody != null) {
                            result.append(macroBody);
                            index = endParen + 1;
                            continue;
                        }
                    }
                }

                // Simple macro
                String replacement = defines.get(token);
                if (replacement != null) {
                    result.append(expandLine(replacement, defines, depth + 1));
                    index = matcher.end();
                    continue;
                }
            }

            result.append(token);
            index = matcher.end();
        }

        return result.toString();
    }

    private static int findMatchingParen(String text, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String expandParameterizedMacro(String call, Map<String, String> defines, int depth) {
        Matcher matcher = PARAM_MACRO_PATTERN.matcher(call);
        if (!matcher.matches()) return null;

        String macroName = matcher.group(1);
        String[] args = splitArgs(matcher.group(2), defines, depth);

        for (String key : defines.keySet()) {
            Matcher defMatcher = PARAM_MACRO_PATTERN.matcher(key);
            if (defMatcher.matches() && defMatcher.group(1).equals(macroName)) {
                String[] paramNames = splitArgs(defMatcher.group(2), defines, depth);
                if (paramNames.length != args.length) return null;

                String body = defines.get(key);
                for (int i = 0; i < paramNames.length; i++) {
                    body = body.replaceAll("\\b" + Pattern.quote(paramNames[i]) + "\\b",
                        Matcher.quoteReplacement(args[i]));
                }
                return expandLine(body, defines, depth + 1);
            }
        }
        return null;
    }

    private static String[] splitArgs(String argString, Map<String, String> defines, int depth) {
        List<String> args = new ArrayList<>();
        int depthLevel = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < argString.length(); i++) {
            char c = argString.charAt(i);
            if (c == ',' && depthLevel == 0) {
                args.add(expandLine(current.toString().trim(), defines, depth));
                current.setLength(0);
            } else {
                if (c == '(') depthLevel++;
                else if (c == ')') depthLevel--;
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(expandLine(current.toString().trim(), defines, depth));
        }

        return args.toArray(new String[0]);
    }
}
