package com.vetsoftware.app.registration.application.usecase;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Genera una SUGERENCIA de código de acceso (usuario) a partir del nombre de la empresa y del empleado.
 * En la Opción A el dueño puede editar la sugerencia; este generador solo propone un valor por defecto y,
 * con {@link #generateAvailable}, garantiza que la sugerencia esté libre en el momento de proponerla.
 */
final class EmployeeCodeGenerator {

    private static final int MAX_NAME_CHARS = 10;
    private static final int MAX_EMPLOYEE_CODE_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 999;

    private EmployeeCodeGenerator() {}

    static String generate(String companyName, String employeeName) {
        return prefix(companyName) + "-" + suffix(employeeName);
    }

    /** Sugerencia base y, si está tomada, con sufijo {@code -2}, {@code -3}… hasta encontrar una libre. */
    static String generateAvailable(String companyName, String employeeName, Predicate<String> taken) {
        String base = generate(companyName, employeeName);
        if (!taken.test(base)) return base;
        for (int i = 2; i <= MAX_SUFFIX_ATTEMPTS; i++) {
            String candidate = withSuffix(base, i);
            if (!taken.test(candidate)) return candidate;
        }
        throw new IllegalStateException(
                "Could not generate unique employee code after " + MAX_SUFFIX_ATTEMPTS + " attempts");
    }

    private static String withSuffix(String base, int n) {
        String suffix = "-" + n;
        int reserved = MAX_EMPLOYEE_CODE_LENGTH - suffix.length();
        String trimmed = base.length() > reserved ? base.substring(0, reserved) : base;
        return trimmed + suffix;
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
