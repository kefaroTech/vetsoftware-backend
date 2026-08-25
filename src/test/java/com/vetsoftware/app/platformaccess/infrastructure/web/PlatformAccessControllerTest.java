package com.vetsoftware.app.platformaccess.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.dto.PlatformAccessRequestDto;
import com.vetsoftware.app.platformaccess.application.dto.PlatformInvitationDto;
import com.vetsoftware.app.platformaccess.application.port.in.AcceptPlatformInvitationUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ApprovePlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RejectPlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RequestPlatformAccessUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformAccessTokenUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformInvitationTokenUseCase;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessBlockedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessClosedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessCodeMismatchException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de los seis endpoints del alta de superadministradores.
 *
 * <p>
 * Lo que se fija aquí y no en ninguna otra capa: los <b>códigos</b> —incluidos
 * el 202 y el 422, que este flujo introduce en el repositorio—, la forma del
 * cuerpo y, sobre todo, que los tres estados muertos de token salgan
 * <b>indistinguibles</b>. Esa última afirmación no es sobre el servicio sino
 * sobre lo que ve el cliente, así que solo se puede comprobar contra la
 * respuesta real: el {@code GlobalExceptionHandler} entra de verdad en la
 * rodaja.
 */
@WebMvcTest(PlatformAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PlatformAccessController — contrato HTTP del alta de superadministradores")
class PlatformAccessControllerTest {

    private static final String MOTIVO_VALIDO = "Necesito acceso para operar la plataforma";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestPlatformAccessUseCase requestUseCase;
    @MockitoBean
    private ValidatePlatformAccessTokenUseCase validateAccessTokenUseCase;
    @MockitoBean
    private ApprovePlatformAccessRequestUseCase approveUseCase;
    @MockitoBean
    private RejectPlatformAccessRequestUseCase rejectUseCase;
    @MockitoBean
    private ValidatePlatformInvitationTokenUseCase validateInvitationTokenUseCase;
    @MockitoBean
    private AcceptPlatformInvitationUseCase acceptUseCase;

    private static String cuerpoSolicitud(String fullName, String email, String reason) {
        return """
                {"fullName":"%s","email":"%s","reason":"%s"}
                """.formatted(fullName, email, reason);
    }

    @Nested
    @DisplayName("POST /platform/access-request")
    class CrearSolicitud {

        @Test
        @DisplayName("responde 202 sin cuerpo y traslada los tres campos al command")
        void responde_202_y_construye_el_command() throws Exception {
            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("Ana Ramirez", "ana@vetrina.co", MOTIVO_VALIDO)))
                    .andExpect(status().isAccepted()).andExpect(content().string(""));

            ArgumentCaptor<RequestPlatformAccessCommand> captor = ArgumentCaptor
                    .forClass(RequestPlatformAccessCommand.class);
            verify(requestUseCase).execute(captor.capture());
            assertThat(captor.getValue().fullName()).isEqualTo("Ana Ramirez");
            assertThat(captor.getValue().email()).isEqualTo("ana@vetrina.co");
            assertThat(captor.getValue().reason()).isEqualTo(MOTIVO_VALIDO);
        }

        @Test
        @DisplayName("el formulario cerrado sale como 404 y su detail NO explica por que")
        void formulario_cerrado_sale_como_404_sin_explicacion() throws Exception {
            doThrow(new PlatformAccessClosedException("Platform access request form is closed"))
                    .when(requestUseCase).execute(any());

            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("Ana Ramirez", "ana@vetrina.co", MOTIVO_VALIDO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_UNAVAILABLE"))
                    // El front pinta texto propio para este estado. Si el detail
                    // explicara el motivo, se leeria en la pestana de red del
                    // navegador de cualquiera que lo abra.
                    .andExpect(jsonPath("$.detail")
                            .value("El recurso solicitado no está disponible."));
        }

        @Test
        @DisplayName("un motivo de menos de 20 caracteres se rechaza con 400 y no llega al use case")
        void rechaza_un_motivo_demasiado_corto() throws Exception {
            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("Ana Ramirez", "ana@vetrina.co", "muy corto")))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }

        @Test
        @DisplayName("un motivo de mas de 500 caracteres se rechaza con 400: Resend corta a 2.000 por variable")
        void rechaza_un_motivo_demasiado_largo() throws Exception {
            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("Ana Ramirez", "ana@vetrina.co", "x".repeat(501))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }

        @Test
        @DisplayName("un nombre de mas de 120 caracteres se rechaza con 400")
        void rechaza_un_nombre_demasiado_largo() throws Exception {
            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("x".repeat(121), "ana@vetrina.co", MOTIVO_VALIDO)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }

        @Test
        @DisplayName("un correo sin formato valido se rechaza con 400")
        void rechaza_un_correo_invalido() throws Exception {
            mockMvc.perform(post("/platform/access-request").contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoSolicitud("Ana Ramirez", "no-es-un-correo", MOTIVO_VALIDO)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(requestUseCase);
        }
    }

    @Nested
    @DisplayName("GET /platform/access-request/validate")
    class ValidarTokenDeAprobacion {

        @Test
        @DisplayName("devuelve los cuatro campos, con requestedAt como instante crudo")
        void devuelve_los_cuatro_campos() throws Exception {
            when(validateAccessTokenUseCase.execute("tok-1"))
                    .thenReturn(new PlatformAccessRequestDto("Ana Ramirez", "ana@vetrina.co",
                            MOTIVO_VALIDO, LocalDateTime.of(2026, 3, 14, 9, 30)));

            mockMvc.perform(get("/platform/access-request/validate").param("token", "tok-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Ana Ramirez"))
                    .andExpect(jsonPath("$.email").value("ana@vetrina.co"))
                    .andExpect(jsonPath("$.reason").value(MOTIVO_VALIDO))
                    // El front lo formatea. Si el backend mandara texto ya formateado
                    // ataria la presentacion al servidor y romperia la zona horaria.
                    .andExpect(jsonPath("$.requestedAt").value("2026-03-14T09:30:00"));
        }

        @Test
        @DisplayName("un token muerto sale 404 con un codigo y un detail que no dicen cual de los estados era")
        void un_token_muerto_sale_404_indistinguible() throws Exception {
            when(validateAccessTokenUseCase.execute(anyString()))
                    .thenThrow(new InvalidApprovalTokenException("Approval token expired"));

            mockMvc.perform(get("/platform/access-request/validate").param("token", "tok-muerto"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVALID_PLATFORM_ACCESS_TOKEN"))
                    .andExpect(jsonPath("$.detail")
                            .value("El enlace no es válido o ya no está disponible."));
        }

        @Test
        @DisplayName("caducado y no encontrado producen EXACTAMENTE la misma respuesta")
        void caducado_y_no_encontrado_son_indistinguibles() throws Exception {
            when(validateAccessTokenUseCase.execute("caducado"))
                    .thenThrow(new InvalidApprovalTokenException("Approval token expired"));
            when(validateAccessTokenUseCase.execute("inexistente"))
                    .thenThrow(new InvalidApprovalTokenException("Approval token does not exist"));

            String caducado = mockMvc
                    .perform(get("/platform/access-request/validate").param("token", "caducado"))
                    .andExpect(status().isNotFound()).andReturn().getResponse()
                    .getContentAsString();
            String inexistente = mockMvc
                    .perform(get("/platform/access-request/validate").param("token", "inexistente"))
                    .andExpect(status().isNotFound()).andReturn().getResponse()
                    .getContentAsString();

            // Sin esta igualdad, quien pruebe tokens al azar sabe cuales existieron
            // alguna vez: el oraculo de enumeracion que todo el diseno evita. El
            // traceId es lo unico que puede diferir, y no viaja en esta rodaja.
            assertThat(caducado).isEqualTo(inexistente);
        }
    }

    @Nested
    @DisplayName("POST /platform/access-request/approve y /reject")
    class Resolver {

        @Test
        @DisplayName("aprobar responde 204 y traslada token y codigo al command")
        void aprobar_responde_204() throws Exception {
            mockMvc.perform(
                    post("/platform/access-request/approve").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"tok-1\",\"code\":\"123456\"}"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            ArgumentCaptor<ResolvePlatformAccessCommand> captor = ArgumentCaptor
                    .forClass(ResolvePlatformAccessCommand.class);
            verify(approveUseCase).execute(captor.capture());
            assertThat(captor.getValue().token()).isEqualTo("tok-1");
            assertThat(captor.getValue().code()).isEqualTo("123456");
        }

        @Test
        @DisplayName("rechazar responde 204 y usa el mismo cuerpo que aprobar")
        void rechazar_responde_204() throws Exception {
            mockMvc.perform(
                    post("/platform/access-request/reject").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"tok-1\",\"code\":\"123456\"}"))
                    .andExpect(status().isNoContent());

            verify(rejectUseCase).execute(any());
        }

        @Test
        @DisplayName("el codigo incorrecto sale como 422 con remainingAttempts")
        void codigo_incorrecto_sale_como_422_con_intentos_restantes() throws Exception {
            doThrow(new PlatformAccessCodeMismatchException("Verification code does not match", 3))
                    .when(approveUseCase).execute(any());

            mockMvc.perform(
                    post("/platform/access-request/approve").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"tok-1\",\"code\":\"000000\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_CODE_MISMATCH"))
                    .andExpect(jsonPath("$.remainingAttempts").value(3));
        }

        @Test
        @DisplayName("los intentos agotados salen como 429, no como 422 con cero")
        void intentos_agotados_salen_como_429() throws Exception {
            doThrow(new PlatformAccessBlockedException("Access request is permanently blocked"))
                    .when(approveUseCase).execute(any());

            mockMvc.perform(
                    post("/platform/access-request/approve").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"tok-1\",\"code\":\"000000\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_BLOCKED"));
        }

        @Test
        @DisplayName("un codigo que no son 6 digitos se rechaza con 400 y no gasta intento")
        void un_codigo_mal_formado_no_llega_al_use_case() throws Exception {
            mockMvc.perform(
                    post("/platform/access-request/approve").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"tok-1\",\"code\":\"12345\"}"))
                    .andExpect(status().isBadRequest());

            // Importa que no llegue: cada llamada al use case gasta uno de los cinco
            // intentos, y un codigo de cinco digitos no es un intento, es un error de
            // forma.
            verifyNoInteractions(approveUseCase);
        }
    }

    @Nested
    @DisplayName("Invitacion")
    class Invitacion {

        @Test
        @DisplayName("validar devuelve solo el correo")
        void validar_devuelve_solo_el_correo() throws Exception {
            when(validateInvitationTokenUseCase.execute("inv-1"))
                    .thenReturn(new PlatformInvitationDto("ana@vetrina.co"));

            mockMvc.perform(get("/platform/invitation/validate").param("token", "inv-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("ana@vetrina.co"))
                    .andExpect(jsonPath("$.fullName").doesNotExist());
        }

        @Test
        @DisplayName("una invitacion muerta sale 404 con su propio codigo y detail constante")
        void una_invitacion_muerta_sale_404() throws Exception {
            when(validateInvitationTokenUseCase.execute(anyString())).thenThrow(
                    new InvalidInvitationTokenException("Invitation is no longer usable"));

            mockMvc.perform(get("/platform/invitation/validate").param("token", "muerto"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVALID_PLATFORM_INVITATION_TOKEN"))
                    .andExpect(jsonPath("$.detail")
                            .value("El enlace no es válido o ya no está disponible."));
        }

        @Test
        @DisplayName("aceptar responde 204 sin cuerpo: no hay autologin ni token de sesion")
        void aceptar_responde_204_sin_sesion() throws Exception {
            mockMvc.perform(
                    post("/platform/invitation/accept").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"inv-1\",\"password\":\"contrasena-larga-1\"}"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""))
                    // Ni cookie de sesion ni cabecera de autorizacion: emitirlas
                    // convertiria la posesion del token en una sesion viva de
                    // superadministrador sin pasar por el login.
                    .andExpect(result -> assertThat(result.getResponse().getCookies()).isEmpty());

            ArgumentCaptor<AcceptPlatformInvitationCommand> captor = ArgumentCaptor
                    .forClass(AcceptPlatformInvitationCommand.class);
            verify(acceptUseCase).execute(captor.capture());
            assertThat(captor.getValue().token()).isEqualTo("inv-1");
        }

        @Test
        @DisplayName("una contrasena de menos de 12 caracteres se rechaza con 400")
        void rechaza_una_contrasena_corta() throws Exception {
            mockMvc.perform(
                    post("/platform/invitation/accept").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"inv-1\",\"password\":\"corta123\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(acceptUseCase);
        }

        @Test
        @DisplayName("un correo en el cuerpo se ignora: el command solo lleva token y contrasena")
        void un_correo_en_el_cuerpo_se_ignora() throws Exception {
            mockMvc.perform(post("/platform/invitation/accept")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"token":"inv-1","password":"contrasena-larga-1",
                             "email":"atacante@dominio.invalid"}
                            """)).andExpect(status().isNoContent());

            ArgumentCaptor<AcceptPlatformInvitationCommand> captor = ArgumentCaptor
                    .forClass(AcceptPlatformInvitationCommand.class);
            verify(acceptUseCase).execute(captor.capture());
            // El record no tiene donde guardarlo. Si alguien anadiera el campo, este
            // test seguiria pasando y por eso el javadoc del command dice que se
            // ignora: la barrera de verdad es que el correo sale del token.
            assertThat(captor.getValue())
                    .isEqualTo(new AcceptPlatformInvitationCommand("inv-1", "contrasena-larga-1"));
        }
    }
}
