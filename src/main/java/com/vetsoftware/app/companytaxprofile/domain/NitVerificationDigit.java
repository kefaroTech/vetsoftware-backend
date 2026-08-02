package com.vetsoftware.app.companytaxprofile.domain;

/**
 * Calcula el dígito de verificación (DV) de un NIT colombiano con el algoritmo
 * de módulo 11 de la DIAN. Los pesos se aplican de derecha a izquierda sobre
 * los dígitos del NIT (sin incluir el propio DV).
 */
public final class NitVerificationDigit {
    private static final int[] WEIGHTS = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

    private NitVerificationDigit() {
    }

    /**
     * @param nit
     *            NIT numérico (solo dígitos, sin el DV).
     * @return el dígito de verificación como un único carácter "0"–"9".
     * @throws IllegalArgumentException
     *             si el NIT es nulo, vacío, no numérico o más largo que los pesos
     *             disponibles.
     */
    public static String calculate(String nit) {
        if (nit == null || nit.isBlank())
            throw new IllegalArgumentException(
                    "nit is required to calculate the verification digit");
        String digits = nit.trim();
        if (!digits.matches("\\d+"))
            throw new IllegalArgumentException(
                    "nit must contain only digits to calculate the verification digit");
        if (digits.length() > WEIGHTS.length)
            throw new IllegalArgumentException(
                    "nit is too long to calculate the verification digit");

        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(digits.length() - 1 - i) - '0';
            sum += digit * WEIGHTS[i];
        }
        int mod = sum % 11;
        int dv = mod >= 2 ? 11 - mod : mod;
        return Integer.toString(dv);
    }
}
