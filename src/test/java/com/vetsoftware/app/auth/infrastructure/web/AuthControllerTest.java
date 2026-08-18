package com.vetsoftware.app.auth.infrastructure.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.port.in.GetCurrentUserUseCase;
import com.vetsoftware.app.auth.application.port.in.LoginEmployeeUseCase;
import com.vetsoftware.app.auth.application.port.in.LoginSystemUserUseCase;
import com.vetsoftware.app.auth.application.port.in.LogoutUseCase;
import com.vetsoftware.app.auth.application.port.in.RefreshTokenUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de los endpoints de autenticación. La autorización de negocio no
 * se prueba aquí (los cuatro puertos de entrada llevan su propio
 * {@code @PreAuthorize} o {@code @NoAuthorizationRequired}, verificado por
 * ArchUnit); lo que sí es contrato HTTP: dónde viaja el refresh token —cookie
 * {@code HttpOnly}, nunca en el cuerpo de la respuesta— y qué código devuelve
 * cada rechazo.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AuthController — contrato HTTP")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginEmployeeUseCase loginEmployeeUseCase;
    @MockitoBean
    private LoginSystemUserUseCase loginSystemUserUseCase;
    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;
    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean
    private LogoutUseCase logoutUseCase;
    @MockitoBean
    private RefreshTokenCookie refreshTokenCookie;

    private static final ResponseCookie COOKIE_EMITIDA = ResponseCookie
            .from(RefreshTokenCookie.NAME, "cookie-emitida").httpOnly(true).secure(true)
            .path("/auth").build();
    private static final ResponseCookie COOKIE_BORRADA = ResponseCookie
            .from(RefreshTokenCookie.NAME, "").httpOnly(true).secure(true).path("/auth").maxAge(0)
            .build();

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("login de empleado exitoso: 200, cookie de refresh y el token nunca en el cuerpo")
        void login_empleado_exitoso() throws Exception {
            when(loginEmployeeUseCase.execute(new LoginEmployeeCommand("EMP-1", "secret")))
                    .thenReturn(new TokenDto("access", AuthSubjectType.EMPLOYEE, "raw-refresh"));
            when(refreshTokenCookie.issue("raw-refresh")).thenReturn(COOKIE_EMITIDA);

            mockMvc.perform(
                    post("/auth/login/employee").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeCode":"EMP-1","password":"secret"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("access"))
                    .andExpect(jsonPath("$.type").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.refreshToken").doesNotExist())
                    .andExpect(cookie().value(RefreshTokenCookie.NAME, "cookie-emitida"));
        }

        @Test
        @DisplayName("login de empleado sin password responde 400 y no llama al caso de uso")
        void login_empleado_sin_password_responde_400() throws Exception {
            mockMvc.perform(
                    post("/auth/login/employee").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeCode":"EMP-1","password":""}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(loginEmployeeUseCase);
        }

        @Test
        @DisplayName("login de usuario de sistema exitoso: 200 y cookie de refresh")
        void login_sistema_exitoso() throws Exception {
            when(loginSystemUserUseCase.execute(new LoginSystemUserCommand("ADMIN", "secret")))
                    .thenReturn(
                            new TokenDto("access-sys", AuthSubjectType.SYSTEM_USER, "raw-refresh"));
            when(refreshTokenCookie.issue("raw-refresh")).thenReturn(COOKIE_EMITIDA);

            mockMvc.perform(
                    post("/auth/login/system").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"ADMIN","password":"secret"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("SYSTEM_USER"))
                    .andExpect(cookie().exists(RefreshTokenCookie.NAME));
        }

        @Test
        @DisplayName("login de usuario de sistema sin código responde 400")
        void login_sistema_sin_codigo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/auth/login/system").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"","password":"secret"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(loginSystemUserUseCase);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("con la cookie presente, el token se toma de la cookie y no del cuerpo")
        void con_cookie_presente_usa_la_cookie() throws Exception {
            when(refreshTokenUseCase.execute("de-la-cookie")).thenReturn(
                    new TokenDto("nuevo-access", AuthSubjectType.EMPLOYEE, "nuevo-refresh"));
            when(refreshTokenCookie.issue("nuevo-refresh")).thenReturn(COOKIE_EMITIDA);

            mockMvc.perform(post("/auth/refresh")
                    .cookie(new Cookie(RefreshTokenCookie.NAME, "de-la-cookie"))
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"refreshToken":"del-cuerpo"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("nuevo-access"));

            verify(refreshTokenUseCase).execute("de-la-cookie");
            verify(refreshTokenUseCase, never()).execute("del-cuerpo");
        }

        @Test
        @DisplayName("sin cookie, respalda con el token del cuerpo durante el despliegue coordinado")
        void sin_cookie_usa_el_cuerpo() throws Exception {
            when(refreshTokenUseCase.execute("del-cuerpo")).thenReturn(
                    new TokenDto("nuevo-access", AuthSubjectType.EMPLOYEE, "nuevo-refresh"));
            when(refreshTokenCookie.issue(anyString())).thenReturn(COOKIE_EMITIDA);

            mockMvc.perform(
                    post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                            {"refreshToken":"del-cuerpo"}
                            """)).andExpect(status().isOk());

            verify(refreshTokenUseCase).execute("del-cuerpo");
        }

        @Test
        @DisplayName("sin cookie y sin cuerpo responde 401 y no llama al caso de uso")
        void sin_cookie_ni_cuerpo_responde_401() throws Exception {
            mockMvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

            verifyNoInteractions(refreshTokenUseCase);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("responde 204, revoca en servidor y borra la cookie con los mismos atributos")
        void logout_responde_204_y_borra_la_cookie() throws Exception {
            when(refreshTokenCookie.clear()).thenReturn(COOKIE_BORRADA);

            mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge(RefreshTokenCookie.NAME, 0));

            verify(logoutUseCase).execute();
        }
    }

    @Nested
    @DisplayName("me")
    class Me {

        @Test
        @DisplayName("expone el perfil del actor autenticado con permisos y sedes como listas")
        void expone_el_perfil_del_actor_autenticado() throws Exception {
            when(getCurrentUserUseCase.execute()).thenReturn(new MeDto(7L, AuthSubjectType.EMPLOYEE,
                    3L, "Ana Ruiz", "EMP-1", false, Set.of("company.read"), Set.of(10L)));

            mockMvc.perform(get("/auth/me")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.companyId").value(3))
                    .andExpect(jsonPath("$.permissions[0]").value("company.read"))
                    .andExpect(jsonPath("$.branchIds[0]").value(10));
        }
    }
}
