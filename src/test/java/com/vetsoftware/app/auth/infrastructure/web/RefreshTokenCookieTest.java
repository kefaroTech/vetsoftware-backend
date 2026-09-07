package com.vetsoftware.app.auth.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieTest {

    private static final long THIRTY_DAYS = 30L;

    @Test
    void la_cookie_emitida_no_es_legible_por_javascript() {
        ResponseCookie cookie = new RefreshTokenCookie(true, "Lax", THIRTY_DAYS)
                .issue(AuthSubjectType.EMPLOYEE, "token-opaco");

        assertTrue(cookie.isHttpOnly(), "sin HttpOnly la cookie no aporta nada sobre localStorage");
        assertTrue(cookie.isSecure());
        assertEquals("token-opaco", cookie.getValue());
        assertEquals(Duration.ofDays(THIRTY_DAYS), cookie.getMaxAge());
    }

    @Test
    void la_cookie_solo_viaja_a_las_rutas_de_auth() {
        ResponseCookie cookie = new RefreshTokenCookie(true, "Lax", THIRTY_DAYS)
                .issue(AuthSubjectType.EMPLOYEE, "t");

        // Con Path=/ la credencial de 30 dias acompañaria a cada peticion de negocio.
        assertEquals("/auth", cookie.getPath());
    }

    @Test
    void cada_tipo_de_sujeto_ocupa_un_nombre_de_cookie_distinto() {
        RefreshTokenCookie factory = new RefreshTokenCookie(true, "Lax", THIRTY_DAYS);

        // El navegador identifica la cookie por (nombre, dominio, path): con un solo
        // nombre, la consola (SYSTEM_USER) y la app del tenant (EMPLOYEE) se pisarian
        // la sesion la una a la otra.
        assertNotEquals(factory.nameFor(AuthSubjectType.EMPLOYEE),
                factory.nameFor(AuthSubjectType.SYSTEM_USER));
    }

    @Test
    void borrar_repite_los_atributos_de_la_original() {
        RefreshTokenCookie factory = new RefreshTokenCookie(true, "Strict", THIRTY_DAYS);
        ResponseCookie issued = factory.issue(AuthSubjectType.EMPLOYEE, "t");
        ResponseCookie cleared = factory.clear(AuthSubjectType.EMPLOYEE);

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
    void borrar_solo_afecta_a_la_cookie_del_tipo_pedido() {
        RefreshTokenCookie factory = new RefreshTokenCookie(true, "Strict", THIRTY_DAYS);

        ResponseCookie cleared = factory.clear(AuthSubjectType.EMPLOYEE);

        assertEquals(factory.nameFor(AuthSubjectType.EMPLOYEE), cleared.getName());
        assertNotEquals(factory.nameFor(AuthSubjectType.SYSTEM_USER), cleared.getName());
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
        ResponseCookie cookie = new RefreshTokenCookie(true, "None", THIRTY_DAYS)
                .issue(AuthSubjectType.EMPLOYEE, "t");

        assertEquals("None", cookie.getSameSite());
    }

    @Test
    void local_puede_servir_la_cookie_sobre_http() {
        ResponseCookie cookie = new RefreshTokenCookie(false, "Lax", THIRTY_DAYS)
                .issue(AuthSubjectType.EMPLOYEE, "t");

        assertFalse(cookie.isSecure());
        assertTrue(cookie.isHttpOnly(), "HttpOnly no se negocia ni en local");
    }
}
