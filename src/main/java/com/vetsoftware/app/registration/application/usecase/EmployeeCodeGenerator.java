package com.vetsoftware.app.registration.application.usecase;

import java.text.Normalizer;
import java.util.Locale;

final class EmployeeCodeGenerator {

    private static final int MAX_NAME_CHARS = 10;

    private EmployeeCodeGenerator() {}

    static String generate(String companyName, String employeeName) {
        return prefix(companyName) + "-" + suffix(employeeName);
    }

    private static String prefix(String companyName) {
        StringBuilder out = new StringBuilder();
        for (String word : normalize(companyName).split("\\s+")) {
            if (!word.isEmpty()) {
                out.append(word.charAt(0));
            }
        }
        return out.toString().toUpperCase(Locale.ROOT);
    }

    private static String suffix(String employeeName) {
        String upper = normalize(employeeName).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return upper.length() > MAX_NAME_CHARS ? upper.substring(0, MAX_NAME_CHARS) : upper;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("[^A-Za-z\\s]", "")
            .trim();
    }
}
