package com.vetsoftware.app.platformaccess.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.vetsoftware.app.platformaccess.application.port.in.AcceptPlatformInvitationUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ApprovePlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RejectPlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RequestPlatformAccessUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformAccessTokenUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformInvitationTokenUseCase;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessClosedException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * <b>Todo el diseño de este flujo se sostiene sobre una idea: la respuesta no
 * puede decir nada que el cliente no supiera ya.</b> Este archivo la comprueba
 * comparando respuestas completas entre sí, que es la única forma de afirmarlo:
 * un {@code jsonPath} por campo pasa aunque el {@code detail} delate el estado,
 * porque nadie escribió el {@code jsonPath} de ese campo.
 *
 * <p>
 * Tres oráculos de enumeración se cierran aquí, y los tres son explotables por
 * un anónimo sin credencial ninguna:
 *
 * <ol>
 * <li><b>«¿Existió este token alguna vez?»</b> Un enlace inexistente, uno
 * caducado y uno ya usado tienen que producir la <b>misma respuesta byte a
 * byte</b>. Si difieren, quien pruebe tokens al azar separa los que existieron
 * de los que no, y eso convierte un espacio de 2²⁵⁶ en una lista de objetivos
 * confirmados.</li>
 * <li><b>«¿Hay ya una cuenta con este correo?»</b> El 202 del formulario tiene
 * que ser idéntico exista o no. Es el mismo oráculo de enumeración de cuentas
 * que en un «¿olvidó su contraseña?», solo que aquí las cuentas son
 * superadministradores.</li>
 * <li><b>«¿Por qué está cerrado?»</b> El 404 del formulario cerrado no puede
 * explicar el motivo. Que el front pinte texto propio no basta: el cuerpo se
 * lee en la pestaña de red.</li>
 * </ol>
 */
@WebMvcTest(PlatformAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("Opacidad de las respuestas — ningún endpoint delata lo que no debe")
class PlatformAccessOpacityTest {

    private static final String MOTIVO = "Necesito acceso para operar la plataforma";

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

    private MockHttpServletResponse validarAprobacion(String token) throws Exception {
        return mockMvc.perform(get("/platform/access-request/validate").param("token", token))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse validarInvitacion(String token) throws Exception {
        return mockMvc.perform(get("/platform/invitation/validate").param("token", token))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse solicitar(String email) throws Exception {
        return mockMvc.perform(
                post("/platform/access-request").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Ana Ramirez","email":"%s","reason":"%s"}
                        """.formatted(email, MOTIVO))).andReturn().getResponse();
    }

    @Nested
    @DisplayName("los tres estados muertos del token de aprobación")
    class TokenDeAprobacion {

        @Test
        @DisplayName("inexistente, caducado y YA USADO producen la misma respuesta byte a byte")
        void los_tres_estados_muertos_son_identicos() throws Exception {
            when(validateAccessTokenUseCase.execute("inexistente"))
                    .thenThrow(new InvalidApprovalTokenException("Approval token does not exist"));
            when(validateAccessTokenUseCase.execute("caducado"))
                    .thenThrow(new InvalidApprovalTokenException("Approval token expired"));
            when(validateAccessTokenUseCase.execute("usado")).thenThrow(
                    new InvalidApprovalTokenException("Access request is no longer resolvable"));

            MockHttpServletResponse inexistente = validarAprobacion("inexistente");
            MockHttpServletResponse caducado = validarAprobacion("caducado");
            MockHttpServletResponse usado = validarAprobacion("usado");

            // El mensaje de la excepcion SI distingue los tres —vive en el evento de
            // auditoria— y por eso este handler no puede devolver getMessage() crudo,
            // que es justo lo que hace el handleNotFound generico del repositorio.
            assertThat(inexistente.getStatus()).isEqualTo(caducado.getStatus())
                    .isEqualTo(usado.getStatus()).isEqualTo(404);
            assertThat(inexistente.getContentAsString()).isEqualTo(caducado.getContentAsString())
                    .isEqualTo(usado.getContentAsString());
            assertThat(inexistente.getContentType()).isEqualTo(caducado.getContentType())
                    .isEqualTo(usado.getContentType());
        }

        @Test
        @DisplayName("el cuerpo no contiene ninguna de las tres palabras que separarian los estados")
        void el_cuerpo_no_contiene_las_palabras_que_separan() throws Exception {
            when(validateAccessTokenUseCase.execute("caducado"))
                    .thenThrow(new InvalidApprovalTokenException("Approval token expired"));

            String cuerpo = validarAprobacion("caducado").getContentAsString();

            assertThat(cuerpo).doesNotContain("expired").doesNotContain("exist")
                    .doesNotContain("resolvable").doesNotContain("caduc");
        }
    }

    @Nested
    @DisplayName("los cuatro estados muertos del token de invitación")
    class TokenDeInvitacion {

        @Test
        @DisplayName("inexistente, caducada, ya consumida y CORREO YA TOMADO son la misma respuesta")
        void los_cuatro_estados_muertos_son_identicos() throws Exception {
            when(validateInvitationTokenUseCase.execute("inexistente")).thenThrow(
                    new InvalidInvitationTokenException("Invitation token does not exist"));
            when(validateInvitationTokenUseCase.execute("muerta")).thenThrow(
                    new InvalidInvitationTokenException("Invitation is no longer usable"));
            when(validateInvitationTokenUseCase.execute("consumida")).thenThrow(
                    new InvalidInvitationTokenException("Invitation was already consumed"));
            when(validateInvitationTokenUseCase.execute("correo-tomado"))
                    .thenThrow(new InvalidInvitationTokenException(
                            "There is already a system user for this email"));

            MockHttpServletResponse inexistente = validarInvitacion("inexistente");
            MockHttpServletResponse muerta = validarInvitacion("muerta");
            MockHttpServletResponse consumida = validarInvitacion("consumida");
            MockHttpServletResponse correoTomado = validarInvitacion("correo-tomado");

            // El cuarto es el que de verdad importa: responder algo distinto ahi
            // convertiria el endpoint publico en un reseteo de contrasena de
            // superadministrador, o como minimo en la confirmacion de que ese correo
            // ya tiene una cuenta de plataforma.
            assertThat(inexistente.getContentAsString()).isEqualTo(muerta.getContentAsString())
                    .isEqualTo(consumida.getContentAsString())
                    .isEqualTo(correoTomado.getContentAsString());
            assertThat(correoTomado.getStatus()).isEqualTo(404);
            assertThat(correoTomado.getContentAsString()).doesNotContain("email")
                    .doesNotContain("system user").doesNotContain("consumed");
        }

        @Test
        @DisplayName("el 404 del token de aprobacion y el de invitacion sí se distinguen entre sí")
        void las_dos_familias_si_se_distinguen_entre_si() throws Exception {
            when(validateAccessTokenUseCase.execute(any()))
                    .thenThrow(new InvalidApprovalTokenException("Approval token expired"));
            when(validateInvitationTokenUseCase.execute(any())).thenThrow(
                    new InvalidInvitationTokenException("Invitation is no longer usable"));

            // Y es correcto: son dos pantallas distintas del front, y saber «esto es
            // un enlace de invitacion» no revela nada que quien lo abrio no supiera.
            assertThat(validarAprobacion("x").getContentAsString())
                    .isNotEqualTo(validarInvitacion("x").getContentAsString());
        }
    }

    @Nested
    @DisplayName("el 202 del formulario público")
    class FormularioPublico {

        @Test
        @DisplayName("responde IGUAL exista o no una cuenta con ese correo: mismo status y mismo cuerpo vacio")
        void responde_igual_exista_o_no_la_cuenta() throws Exception {
            // El caso de uso ni siquiera puede distinguirlo —no recibe el puerto de
            // aprovisionamiento— y aqui se comprueba lo que ve el cliente.
            doNothing().when(requestUseCase).execute(any());

            MockHttpServletResponse nueva = solicitar("nadie@vetrina.co");
            MockHttpServletResponse yaExiste = solicitar("superadmin@vetrina.co");

            assertThat(nueva.getStatus()).isEqualTo(202).isEqualTo(yaExiste.getStatus());
            assertThat(nueva.getContentAsString()).isEqualTo(yaExiste.getContentAsString());
            assertThat(nueva.getContentAsString()).isEmpty();
        }

        @Test
        @DisplayName("una solicitud duplicada tambien sale 202 sin cuerpo, no 409")
        void una_solicitud_duplicada_tambien_sale_202() throws Exception {
            // El servicio ignora el duplicado y termina sin error justamente para
            // que el cliente no pueda separar «es la primera vez» de «ya pediste».
            doNothing().when(requestUseCase).execute(any());

            assertThat(solicitar("ana@vetrina.co").getStatus()).isEqualTo(202);
        }

        @Test
        @DisplayName("el 404 de formulario cerrado no explica el motivo en el cuerpo")
        void el_404_de_formulario_cerrado_no_explica_el_motivo() throws Exception {
            doThrow(new PlatformAccessClosedException(
                    "Platform access request form is closed because a superadmin already exists"))
                    .when(requestUseCase).execute(any());

            String cuerpo = solicitar("ana@vetrina.co").getContentAsString();

            // El mensaje de la excepcion es para el log. Si saliera al cuerpo, la
            // pestana de red diria por que esta cerrado — y «ya existe un
            // superadministrador» es informacion de plataforma.
            assertThat(cuerpo).doesNotContain("superadmin").doesNotContain("closed")
                    .doesNotContain("already").contains("PLATFORM_ACCESS_UNAVAILABLE");
        }

        @Test
        @DisplayName("el 404 de formulario cerrado es indistinguible de una ruta que no existe... salvo por su code")
        void el_404_de_formulario_cerrado_solo_lleva_su_code() throws Exception {
            doThrow(new PlatformAccessClosedException("cerrado")).when(requestUseCase)
                    .execute(any());

            MockHttpServletResponse cerrado = solicitar("ana@vetrina.co");

            // El front necesita ESE code para saber que pintar; lo que no puede
            // llevar es el motivo. Las dos cosas a la vez son el contrato. Se
            // comprueba sobre el code y no sobre el detail: el detail lleva tildes y
            // getContentAsString() decodifica con el charset de la respuesta, no con
            // UTF-8 fijo — esa comparacion es del test de contrato, con jsonPath.
            assertThat(cerrado.getStatus()).isEqualTo(404);
            assertThat(cerrado.getContentAsString()).contains("PLATFORM_ACCESS_UNAVAILABLE")
                    .contains("\"status\":404");
        }
    }
}
