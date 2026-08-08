package com.vetsoftware.app.auth.infrastructure.web;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye la cookie que transporta el refresh token.
 *
 * <p>
 * El refresh token vivía en {@code localStorage} de los dos frontends, junto al
 * access token. Cualquier script de la página podía leerlo, y dura 30 días: un
 * XSS no robaba una sesión de 15 minutos, robaba un mes de acceso reutilizable
 * fuera del navegador de la víctima. En cookie {@code HttpOnly} el JavaScript
 * de la página ya no puede leerlo.
 *
 * <p>
 * <strong>Lo que esto no arregla.</strong> Un XSS sigue pudiendo llamar a
 * {@code POST /auth/refresh} desde la propia página: el navegador adjunta la
 * cookie él solo. Lo que deja de poder hacer es <em>exfiltrar</em> la
 * credencial de 30 días. La diferencia importa: el ataque pasa de silencioso y
 * persistente a acotado a la vida de la pestaña comprometida. Cerrarlo del todo
 * pide detección de reuso de refresh token, que es un hallazgo aparte (BE-13).
 *
 * <p>
 * <strong>{@code Path=/auth}</strong> y no {@code /}: la cookie solo se
 * necesita en {@code /auth/refresh} y {@code /auth/logout}. Limitar el path
 * significa que no viaja en las cientos de peticiones de negocio que hace la
 * aplicación, y que un endpoint que refleje sus propias cabeceras no la puede
 * filtrar.
 *
 * <p>
 * <strong>{@code SameSite}</strong> es configurable por una razón concreta: los
 * frontends y la API comparten dominio registrable ({@code kefaro.tech} en dev,
 * {@code vetsoftware.co} en producción), así que son <em>same-site</em> y
 * {@code Lax} basta. Si un front pasa a servirse desde otro dominio registrable
 * —{@code *.pages.dev}, por ejemplo— hay que poner {@code None}, y {@code None}
 * exige {@code Secure}. El validador de abajo impide dejar esa combinación mal.
 */
@Component
public class RefreshTokenCookie {

    /**
     * Mismo nombre que la clave que ocupaba en localStorage, para no confundir al
     * depurar.
     */
    public static final String NAME = "vet_refresh";

    private static final String PATH = "/auth";

    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public RefreshTokenCookie(@Value("${jwt.refresh-cookie.secure:true}") boolean secure,
            @Value("${jwt.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${jwt.refresh-expiration-days}") long refreshExpirationDays) {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                    "jwt.refresh-cookie.same-site=None exige secure=true; el navegador descarta la cookie en caso contrario");
        }
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofDays(refreshExpirationDays);
    }

    /**
     * Cookie con el token recién emitido. El {@code maxAge} acompaña a la
     * expiración real del token en base de datos; si divergen, manda la de base de
     * datos y el usuario ve un 401 en vez de un token que el navegador ya borró.
     */
    public ResponseCookie issue(String rawRefreshToken) {
        return base(rawRefreshToken).maxAge(maxAge).build();
    }

    /**
     * Cookie de borrado. Debe repetir {@code path}, {@code secure} y
     * {@code sameSite} exactos de la original: el navegador identifica la cookie
     * por (nombre, dominio, path), y una discrepancia deja la vieja viva.
     */
    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).path(PATH)
                .sameSite(sameSite);
    }
}
