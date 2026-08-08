package com.vetsoftware.app.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieTest {

    private static final long THIRTY_DAYS = 30L;

    @Test
    void la_cookie_emitida_no_es_legible_por_javascript() {
        ResponseCookie cookie = new RefreshTokenCookie(true, "Lax", THIRTY_DAYS)
                .issue("token-opaco");

        assertTrue(cookie.isHttpOnly(), "sin HttpOnly la cookie no aporta nada sobre localStorage");
        assertTrue(cookie.isSecure());
        assertEquals("token-opaco", cookie.getValue());
        assertEquals(Duration.ofDays(THIRTY_DAYS), cookie.getMaxAge());
    }

    @Test
    void la_cookie_solo_viaja_a_las_rutas_de_auth() {
        ResponseCookie cookie = new RefreshTokenCookie(true, "Lax", THIRTY_DAYS).issue("t");

        // Con Path=/ la credencial de 30 dias acompañaria a cada peticion de negocio.
        assertEquals("/auth", cookie.getPath());
    }

    @Test
    void borrar_repite_los_atributos_de_la_original() {
        RefreshTokenCookie factory = new RefreshTokenCookie(true, "Strict", THIRTY_DAYS);
        ResponseCookie issued = factory.issue("t");
        ResponseCookie cleared = factory.clear();

        // El navegador identifica la cookie por (nombre, dominio, path). Si el
        // borrado cambia alguno, la vieja sobrevive y el logout no cierra nada.
        assertEquals(issued.getName(), cleared.getName());
        assertEquals(issued.getPath(), cleared.getPath());
        assertEquals(issued.getSameSite(), cleared.getSameSite());
        assertEquals(issued.isSecure(), cleared.isSecure());
        assertEquals(Duration.ZERO, cleared.getMaxAge());
        assertTrue(cleared.getValue().isEmpty());
    }

    @Test
    void same_site_none_sin_secure_no_arranca() {
        // El navegador descarta SameSite=None sin Secure en silencio: el fallo se
        // manifestaria como sesiones que no persisten, no como un error visible.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new RefreshTokenCookie(false, "None", THIRTY_DAYS));

        assertTrue(ex.getMessage().contains("secure=true"));
    }

    @Test
    void same_site_none_con_secure_es_valido() {
        ResponseCookie cookie = new RefreshTokenCookie(true, "None", THIRTY_DAYS).issue("t");

        assertEquals("None", cookie.getSameSite());
    }

    @Test
    void local_puede_servir_la_cookie_sobre_http() {
        ResponseCookie cookie = new RefreshTokenCookie(false, "Lax", THIRTY_DAYS).issue("t");

        assertFalse(cookie.isSecure());
        assertTrue(cookie.isHttpOnly(), "HttpOnly no se negocia ni en local");
    }
}
