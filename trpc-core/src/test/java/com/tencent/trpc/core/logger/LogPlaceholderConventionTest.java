/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guard the log placeholder style of the whole repository.
 *
 * <p>The logger of the framework delegates to slf4j, which only recognizes the {@code {}} placeholder. A printf
 * style placeholder(such as {@code %s} or {@code %d}) is never replaced by slf4j, so the real values are silently
 * lost from the log. Besides, the number of the placeholders must match the number of the arguments, otherwise
 * either an argument is dropped or a raw {@code {}} is left in the log message.</p>
 *
 * <p>This test scans the production sources so that such defects can be found before they reach the production
 * environment.</p>
 */
public class LogPlaceholderConventionTest {

    /**
     * The pattern of a logging call of the framework logger.
     */
    private static final Pattern LOG_CALL = Pattern.compile(
            "\\b(?:logger|LOG|log|LOGGER)\\s*\\.\\s*(?:error|warn|info|debug|trace)\\s*\\(");

    /**
     * The pattern of a printf style placeholder, {@code %%} is excluded because it is an escaped percent sign.
     */
    private static final Pattern PRINTF_PLACEHOLDER = Pattern.compile(
            "%(?!%)[-#+ 0,(]*\\d*(?:\\.\\d+)?[sdifxXoeEgGbBhHnc]");

    /**
     * The pattern of a string literal.
     */
    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * The name of the variables which are very likely to be a throwable, a throwable is allowed to be the last
     * argument of a logging call without a matching placeholder.
     */
    private static final Pattern THROWABLE_LIKE = Pattern.compile(
            "^(?:e|ex|t|th|err|error|cause|throwable|exception|ignored|ignore)\\d*$"
                    + "|(?:\\.getCause\\(\\)|\\.cause\\(\\))$");

    /**
     * The max number of the lines a logging call may span.
     */
    private static final int MAX_CALL_LINES = 12;

    private static final String MAIN_SOURCE_DIR = "src/main/java";

    @Test
    public void testNoPrintfPlaceholderInLoggingCall() throws IOException {
        List<String> defects = scan(LogCall::hasPrintfPlaceholder,
                "printf style placeholder is not supported by slf4j, use {} instead");
        assertTrue(defects.isEmpty(), String.join("\n", defects));
    }

    @Test
    public void testPlaceholderCountMatchesArgumentCount() throws IOException {
        List<String> defects = scan(LogCall::hasMismatchedPlaceholder,
                "the number of the placeholders does not match the number of the arguments");
        assertTrue(defects.isEmpty(), String.join("\n", defects));
    }

    /**
     * Make sure the scanner really works, otherwise the two test cases above would pass silently even if the
     * scanner is broken.
     */
    @Test
    public void testScannerDetectsPrintfPlaceholder() {
        LogCall call = LogCall.of("\"Configured tickDuration %d smaller then %d, using 1ms.\", a, b");
        assertTrue(call.hasPrintfPlaceholder());
        assertFalse(LogCall.of("\"Configured tickDuration {} smaller then {}\", a, b").hasPrintfPlaceholder());
        // a percent sign of a plain text is not a placeholder
        assertFalse(LogCall.of("\"progress is 100%\"").hasPrintfPlaceholder());
    }

    @Test
    public void testScannerDetectsMismatchedPlaceholder() {
        assertTrue(LogCall.of("\"request {}, basePath is {}\", path").hasMismatchedPlaceholder());
        assertTrue(LogCall.of("\"request {}\", path, basePath").hasMismatchedPlaceholder());
        assertFalse(LogCall.of("\"request {}, basePath is {}\", path, basePath").hasMismatchedPlaceholder());
        // a throwable is allowed to be the last argument without a placeholder
        assertFalse(LogCall.of("\"request {} failed\", path, e").hasMismatchedPlaceholder());
        assertFalse(LogCall.of("\"request failed\", e").hasMismatchedPlaceholder());
        // the string is built by concatenation, there is no placeholder at all
        assertFalse(LogCall.of("\"request \" + path + \" failed\", e").hasMismatchedPlaceholder());
    }

    @Test
    public void testScannerSkipsStringFormat() {
        // String.format uses the printf style, it is the caller of the logger that formats the message
        assertFalse(LogCall.of("String.format(\"request %s failed\", path), e").hasPrintfPlaceholder());
    }

    private List<String> scan(java.util.function.Predicate<LogCall> predicate, String reason) throws IOException {
        Path root = findRepositoryRoot();
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> sources = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().replace(File.separatorChar, '/').contains(MAIN_SOURCE_DIR))
                    .collect(Collectors.toList());
            return sources.stream()
                    .flatMap(p -> scanFile(p, predicate, reason).stream())
                    .collect(Collectors.toList());
        }
    }

    private List<String> scanFile(Path path, java.util.function.Predicate<LogCall> predicate, String reason) {
        List<String> defects = new java.util.ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            return defects;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (!LOG_CALL.matcher(lines.get(i)).find()) {
                continue;
            }
            String arguments = extractArguments(lines, i);
            if (arguments == null) {
                continue;
            }
            LogCall call = LogCall.of(arguments);
            if (predicate.test(call)) {
                defects.add(String.format("%s:%d: %s%n         %s", path, i + 1, reason,
                        arguments.replaceAll("\\s+", " ")));
            }
        }
        return defects;
    }

    /**
     * Extract the argument list of the logging call which starts at the given line.
     *
     * @param lines all the lines of the source file
     * @param begin the index of the line the logging call starts at
     * @return the argument list without the enclosing parentheses, or null if it can not be extracted
     */
    private String extractArguments(List<String> lines, int begin) {
        StringBuilder buffer = new StringBuilder();
        for (int i = begin; i < Math.min(begin + MAX_CALL_LINES, lines.size()); i++) {
            buffer.append(lines.get(i)).append(' ');
            Matcher matcher = LOG_CALL.matcher(buffer);
            if (!matcher.find()) {
                continue;
            }
            String arguments = readUntilClosed(buffer.substring(matcher.end()));
            if (arguments != null) {
                return arguments;
            }
        }
        return null;
    }

    /**
     * Read the content until the parenthesis of the logging call is closed.
     *
     * @param text the text just after the opening parenthesis
     * @return the content of the parentheses, or null if the parenthesis is not closed yet
     */
    private static String readUntilClosed(String text) {
        StringBuilder result = new StringBuilder();
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        for (char c : text.toCharArray()) {
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }
            if (!inString) {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0) {
                        return result.toString();
                    }
                }
            }
            result.append(c);
        }
        return null;
    }

    private Path findRepositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("trpc-core"))) {
            current = current.getParent();
        }
        return current == null ? Paths.get("").toAbsolutePath() : current;
    }

    /**
     * A parsed logging call.
     */
    private static final class LogCall {

        private final String arguments;

        private final List<String> topLevelArguments;

        private LogCall(String arguments) {
            this.arguments = arguments;
            this.topLevelArguments = splitTopLevel(arguments);
        }

        static LogCall of(String arguments) {
            return new LogCall(arguments);
        }

        /**
         * Whether the message contains a printf style placeholder while it is not formatted by String.format.
         *
         * @return true if the logging call is defective
         */
        boolean hasPrintfPlaceholder() {
            if (arguments.contains("String.format")) {
                return false;
            }
            if (topLevelArguments.size() < 2) {
                // without any argument the percent sign is only a part of the plain text
                return false;
            }
            return PRINTF_PLACEHOLDER.matcher(literalOf(firstArgument())).find();
        }

        /**
         * Whether the number of the placeholders does not match the number of the arguments.
         *
         * @return true if the logging call is defective
         */
        boolean hasMismatchedPlaceholder() {
            String first = firstArgument();
            if (!first.contains("\"") || arguments.contains("String.format")) {
                return false;
            }
            if (first.contains("+")) {
                // the message is built by concatenation, the braces of the literals are not placeholders, and the
                // values are already embedded in the message
                return false;
            }
            int placeholders = countOccurrences(literalOf(first));
            List<String> rest = topLevelArguments.subList(1, topLevelArguments.size());
            int arguments = rest.size();
            boolean lastIsThrowable = !rest.isEmpty()
                    && THROWABLE_LIKE.matcher(rest.get(rest.size() - 1).trim()).find();
            return placeholders != arguments && placeholders != arguments - (lastIsThrowable ? 1 : 0);
        }

        private String firstArgument() {
            return topLevelArguments.isEmpty() ? "" : topLevelArguments.get(0);
        }

        /**
         * Concatenate all the string literals of the given text, the concatenated literals form the log message.
         *
         * @param text the text to be parsed
         * @return the concatenated string literals
         */
        private static String literalOf(String text) {
            StringBuilder builder = new StringBuilder();
            Matcher matcher = STRING_LITERAL.matcher(text);
            while (matcher.find()) {
                builder.append(matcher.group(1));
            }
            return builder.toString();
        }

        private static int countOccurrences(String literal) {
            int count = 0;
            int index = literal.indexOf("{}");
            while (index >= 0) {
                count++;
                index = literal.indexOf("{}", index + 2);
            }
            return count;
        }

        /**
         * Split the argument list by the top level commas, the commas inside a string, a parenthesis, a bracket or a
         * brace are ignored.
         *
         * @param text the argument list
         * @return the top level arguments
         */
        private static List<String> splitTopLevel(String text) {
            List<String> result = new java.util.ArrayList<>();
            StringBuilder current = new StringBuilder();
            int depth = 0;
            boolean inString = false;
            boolean inChar = false;
            boolean escaped = false;
            for (char c : text.toCharArray()) {
                if (escaped) {
                    current.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    current.append(c);
                    escaped = true;
                    continue;
                }
                if (c == '\'' && !inString) {
                    inChar = !inChar;
                    current.append(c);
                    continue;
                }
                if (c == '"' && !inChar) {
                    inString = !inString;
                    current.append(c);
                    continue;
                }
                if (inString || inChar) {
                    current.append(c);
                    continue;
                }
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                }
                if (c == ',' && depth == 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
                current.append(c);
            }
            String last = current.toString().trim();
            if (!last.isEmpty() || !result.isEmpty()) {
                result.add(last);
            }
            return result;
        }
    }

    /**
     * Make sure the constant is used, it also documents the expected placeholder style.
     */
    @Test
    public void testPlaceholderStyle() {
        assertEquals(2, LogCall.countOccurrences("a={}, b={}"));
        assertEquals(0, LogCall.countOccurrences("a=%s"));
    }
}
