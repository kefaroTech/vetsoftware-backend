package com.vetsoftware.app.employee.application.usecase;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Genera una SUGERENCIA de código de empleado a partir del nombre de la empresa
 * y del empleado:
 * {@code PREFIJO(iniciales de la empresa)-SUFIJO(nombre sin espacios, máx 10)}.
 * El admin puede editar la sugerencia; {@link #generateAvailable} garantiza que
 * esté libre en el momento de proponerla (sufijo -2, -3…) y que quepa en
 * {@code employee_code(50)} por los dos caminos, con y sin sufijo.
 */
final class EmployeeCodeGenerator {

    private static final int MAX_NAME_CHARS = 10;
    private static final int MAX_EMPLOYEE_CODE_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 999;

    private EmployeeCodeGenerator() {
    }

    static String generate(String companyName, String employeeName) {
        return prefix(companyName) + "-" + suffix(employeeName);
    }

    static String generateAvailable(String companyName, String employeeName,
            Predicate<String> taken) {
        // El recorte a 50 va ANTES de consultar disponibilidad, no solo en la rama del
        // sufijo: el candidato que se consulta tiene que ser exactamente el que se
        // devuelve y se persiste. Si se recortara al salir, se sugeriría un código que
        // nunca se comprobó; si no se recortara, una razón social larga devolvería ~56
        // caracteres y reventaría la invariante de Employee (employee_code(50)) en
        // mitad de la transacción de invitación.
        String base = truncate(generate(companyName, employeeName), MAX_EMPLOYEE_CODE_LENGTH);
        if (!taken.test(base))
            return base;
        for (int i = 2; i <= MAX_SUFFIX_ATTEMPTS; i++) {
            String candidate = withSuffix(base, i);
            if (!taken.test(candidate))
                return candidate;
        }
        throw new IllegalStateException("Could not generate unique employee code after "
                + MAX_SUFFIX_ATTEMPTS + " attempts");
    }

    private static String withSuffix(String base, int n) {
        String suffix = "-" + n;
        // La base ya viene acotada a 50; aquí se reservan además los caracteres del
        // sufijo, así que el desempate sigue cabiendo en la columna.
        return truncate(base, MAX_EMPLOYEE_CODE_LENGTH - suffix.length()) + suffix;
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
        return truncate(upper, MAX_NAME_CHARS);
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String normalize(String input) {
        if (input == null)
            return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z\\s]", "").trim();
    }
}
