package com.vetsoftware.app.auth.infrastructure.web;

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
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
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

    private static final String EMPLOYEE_COOKIE = "vet_refresh_employee";
    private static final String SYSTEM_COOKIE = "vet_refresh_system";

    private static final ResponseCookie COOKIE_EMPLOYEE_EMITIDA = ResponseCookie
            .from(EMPLOYEE_COOKIE, "cookie-emitida").httpOnly(true).secure(true).path("/auth")
            .build();
    private static final ResponseCookie COOKIE_SYSTEM_EMITIDA = ResponseCookie
            .from(SYSTEM_COOKIE, "cookie-emitida").httpOnly(true).secure(true).path("/auth")
            .build();
    private static final ResponseCookie COOKIE_EMPLOYEE_BORRADA = ResponseCookie
            .from(EMPLOYEE_COOKIE, "").httpOnly(true).secure(true).path("/auth").maxAge(0).build();

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("login de empleado exitoso: 200, cookie vet_refresh_employee y el token nunca en el cuerpo")
        void login_empleado_exitoso() throws Exception {
            when(loginEmployeeUseCase.execute(new LoginEmployeeCommand("EMP-1", "secret")))
                    .thenReturn(new TokenDto("access", AuthSubjectType.EMPLOYEE, "raw-refresh"));
            when(refreshTokenCookie.issue(AuthSubjectType.EMPLOYEE, "raw-refresh"))
                    .thenReturn(COOKIE_EMPLOYEE_EMITIDA);

            mockMvc.perform(
                    post("/auth/login/employee").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeCode":"EMP-1","password":"secret"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("access"))
                    .andExpect(jsonPath("$.type").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.refreshToken").doesNotExist())
                    .andExpect(cookie().value(EMPLOYEE_COOKIE, "cookie-emitida"));
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
        @DisplayName("login de usuario de sistema exitoso: 200 y cookie vet_refresh_system")
        void login_sistema_exitoso() throws Exception {
            when(loginSystemUserUseCase.execute(new LoginSystemUserCommand("ADMIN", "secret")))
                    .thenReturn(
                            new TokenDto("access-sys", AuthSubjectType.SYSTEM_USER, "raw-refresh"));
            when(refreshTokenCookie.issue(AuthSubjectType.SYSTEM_USER, "raw-refresh"))
                    .thenReturn(COOKIE_SYSTEM_EMITIDA);

            mockMvc.perform(
                    post("/auth/login/system").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"ADMIN","password":"secret"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("SYSTEM_USER"))
                    .andExpect(cookie().exists(SYSTEM_COOKIE));
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
        @DisplayName("con la cookie del tipo pedido presente, se rota su token")
        void con_cookie_presente_usa_la_cookie() throws Exception {
            when(refreshTokenCookie.nameFor(AuthSubjectType.EMPLOYEE)).thenReturn(EMPLOYEE_COOKIE);
            when(refreshTokenUseCase.execute("de-la-cookie", AuthSubjectType.EMPLOYEE)).thenReturn(
                    new TokenDto("nuevo-access", AuthSubjectType.EMPLOYEE, "nuevo-refresh"));
            when(refreshTokenCookie.issue(AuthSubjectType.EMPLOYEE, "nuevo-refresh"))
                    .thenReturn(COOKIE_EMPLOYEE_EMITIDA);

            mockMvc.perform(
                    post("/auth/refresh").cookie(new Cookie(EMPLOYEE_COOKIE, "de-la-cookie"))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"type":"EMPLOYEE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("nuevo-access"));

            verify(refreshTokenUseCase).execute("de-la-cookie", AuthSubjectType.EMPLOYEE);
        }

        @Test
        @DisplayName("sin la cookie del tipo pedido responde 401 y no llama al caso de uso")
        void sin_cookie_del_tipo_pedido_responde_401() throws Exception {
            when(refreshTokenCookie.nameFor(AuthSubjectType.SYSTEM_USER)).thenReturn(SYSTEM_COOKIE);

            mockMvc.perform(
                    post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"SYSTEM_USER"}
                            """)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

            verifyNoInteractions(refreshTokenUseCase);
        }

        @Test
        @DisplayName("sin cuerpo responde 400 y no llama al caso de uso")
        void sin_cuerpo_responde_400() throws Exception {
            mockMvc.perform(post("/auth/refresh")).andExpect(status().isBadRequest());

            verifyNoInteractions(refreshTokenUseCase);
        }

        @Test
        @DisplayName("el caso de uso rechaza un token que no es de la audiencia pedida: 401")
        void refresh_con_tipo_que_no_casa_responde_401() throws Exception {
            when(refreshTokenCookie.nameFor(AuthSubjectType.SYSTEM_USER)).thenReturn(SYSTEM_COOKIE);
            when(refreshTokenUseCase.execute("de-otro-tipo", AuthSubjectType.SYSTEM_USER))
                    .thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(SYSTEM_COOKIE, "de-otro-tipo"))
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"SYSTEM_USER"}
                            """)).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("responde 204, revoca en servidor y borra solo la cookie del tipo revocado")
        void logout_responde_204_y_borra_la_cookie() throws Exception {
            when(logoutUseCase.execute()).thenReturn(AuthSubjectType.EMPLOYEE);
            when(refreshTokenCookie.clear(AuthSubjectType.EMPLOYEE))
                    .thenReturn(COOKIE_EMPLOYEE_BORRADA);

            mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge(EMPLOYEE_COOKIE, 0));

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
