package com.vetsoftware.app.platformaccess.application.usecase;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Codigo de login del superadministrador que nace al aceptar la invitacion.
 *
 * <p>
 * <b>Por que hace falta generarlo.</b> El login de las cuentas de sistema es
 * por {@code code} y el flujo solo pide contrasena, asi que si nadie lo genera
 * y nadie se lo comunica al invitado, la cuenta queda creada y su dueno sin
 * saber con que usuario entrar. Se genera aqui, en el momento del alta, y viaja
 * en el correo de bienvenida.
 *
 * <p>
 * <b>La convencion es nueva</b>: {@code system_users} no tenia ninguna —los
 * codigos existentes se escriben a mano al crear la cuenta—. Se adopta la forma
 * del unico generador comparable del repositorio,
 * {@code EmployeeCodeGenerator}: normalizar, mayusculas, sin acentos, acotado a
 * la columna, y desempate con sufijo numerico consultando disponibilidad. El
 * prefijo {@code SYS-} deja ver de un vistazo que la cuenta nacio por
 * invitacion.
 *
 * <p>
 * La unicidad real la impone el {@code UNIQUE (code)} de la tabla: si dos
 * aceptaciones colisionan, la segunda revienta con violacion de unicidad. El
 * predicado de disponibilidad reduce las colisiones, no las elimina, y no puede
 * hacerlo: es unicidad entre dos tablas y MySQL no tiene assertions.
 */
final class PlatformSystemUserCodes {

    private static final String PREFIX = "SYS-";
    private static final int MAX_NAME_CHARS = 12;
    private static final int MAX_CODE_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 999;
    private static final String FALLBACK = "ADMIN";

    private PlatformSystemUserCodes() {
    }

    static String generateAvailable(String fullName, Predicate<String> taken) {
        String base = truncate(PREFIX + suffix(fullName), MAX_CODE_LENGTH);
        if (!taken.test(base)) {
            return base;
        }
        for (int i = 2; i <= MAX_SUFFIX_ATTEMPTS; i++) {
            String candidate = withSuffix(base, i);
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a free system user code after "
                + MAX_SUFFIX_ATTEMPTS + " attempts");
    }

    private static String withSuffix(String base, int n) {
        String suffix = "-" + n;
        return truncate(base, MAX_CODE_LENGTH - suffix.length()) + suffix;
    }

    private static String suffix(String fullName) {
        String upper = normalize(fullName).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return upper.isEmpty() ? FALLBACK : truncate(upper, MAX_NAME_CHARS);
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z\\s]", "").trim();
    }
}
