package com.vetsoftware.app.aiproposal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.aiproposal.application.command.SuppressProposalDataCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import com.vetsoftware.app.aiproposal.application.port.in.SuppressProposalDataUseCase;
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
 * El contrato HTTP de la supresion a peticion del titular.
 *
 * <p>
 * Lo que solo se ve desde aqui: que el correo entra por el <b>cuerpo</b> y no
 * por la ruta —donde {@code RequestLoggingContextFilter} lo copiaria a un log
 * con 31 dias de retencion, es decir justo el dato que este endpoint existe
 * para borrar—, que el {@code @Valid} llega a dispararse, y que un correo que
 * no esta responde 200 con ceros en vez de 404: un 404 convertiria el endpoint
 * en un oraculo que dice si una direccion pidio propuesta alguna vez.
 */
@WebMvcTest(AiProposalRetentionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AiProposalRetentionController — contrato HTTP de la supresion")
class AiProposalRetentionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SuppressProposalDataUseCase suppressUseCase;

    private static final LocalDateTime SUPRIMIDO_A_LAS = LocalDateTime.of(2026, 8, 30, 15, 0);

    private static ProposalSuppressionDto acuse(int proposals, int turns, int lines) {
        return new ProposalSuppressionDto(proposals, turns, lines, proposals + turns + lines,
                SUPRIMIDO_A_LAS, null);
    }

    @Nested
    @DisplayName("Supresion")
    class Supresion {

        @Test
        @DisplayName("responde 200 con el desglose por tabla, no con un total suelto")
        void responde_200_con_el_desglose() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(1, 2, 4));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.proposals").value(1))
                    .andExpect(jsonPath("$.turns").value(2)).andExpect(jsonPath("$.lines").value(4))
                    .andExpect(jsonPath("$.total").value(7));
        }

        @Test
        @DisplayName("el correo del cuerpo llega al command tal cual")
        void el_correo_del_cuerpo_llega_al_command() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(0, 0, 0));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<SuppressProposalDataCommand> command = ArgumentCaptor
                    .forClass(SuppressProposalDataCommand.class);
            verify(suppressUseCase).execute(command.capture());
            assertThat(command.getValue().contactEmail()).isEqualTo("laura@vetchapinero.co");
        }

        /**
         * &#9940; <b>El actor sale de la sesion, no del JSON.</b> Es lo unico que hace
         * que la fila de {@code ai_proposal_suppression_requests} valga como rastro de
         * auditoria: si viajara en el cuerpo, lo estaria escribiendo el auditado.
         */
        @Test
        @DisplayName("quien ejecuta la supresion sale de la sesion, no del cuerpo")
        void quien_ejecuta_la_supresion_sale_de_la_sesion() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(1, 0, 0));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<SuppressProposalDataCommand> command = ArgumentCaptor
                    .forClass(SuppressProposalDataCommand.class);
            verify(suppressUseCase).execute(command.capture());
            assertThat(command.getValue().executedBySystemUserId())
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        /**
         * El cuerpo no tiene ese campo y no lo va a tener. Mandarlo de todas formas
         * -que es lo que haria quien quisiera firmar la supresion con el id de otro- no
         * puede cambiar quien queda escrito.
         */
        @Test
        @DisplayName("un actor colado en el cuerpo se ignora: manda la sesion")
        void un_actor_colado_en_el_cuerpo_se_ignora() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(1, 0, 0));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\","
                                    + "\"executedBySystemUserId\":999}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<SuppressProposalDataCommand> command = ArgumentCaptor
                    .forClass(SuppressProposalDataCommand.class);
            verify(suppressUseCase).execute(command.capture());
            assertThat(command.getValue().executedBySystemUserId())
                    .as("el cuerpo no puede firmar por la sesion")
                    .isEqualTo(WebMvcSliceConfig.SYSTEM_USER_ID);
        }

        /**
         * La fecha del acuse la pone el SERVIDOR. Sin ella el front se la inventaba con
         * el reloj del navegador, que es exactamente el dato falso que no se puede
         * permitir en el acuse de una obligacion legal.
         */
        @Test
        @DisplayName("el acuse lleva la fecha del servidor, no la que se invente el front")
        void el_acuse_lleva_la_fecha_del_servidor() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(1, 2, 4));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suppressedAt").value("2026-08-30T15:00:00"));
        }

        /**
         * Los dos ceros que sin esta fecha se leen igual: "ya se le borro" y "nunca
         * hubo nada suyo". El campo es nulable, asi que tiene que viajar cuando lo hay.
         */
        @Test
        @DisplayName("una peticion repetida devuelve la fecha de la anterior")
        void una_peticion_repetida_devuelve_la_fecha_de_la_anterior() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(new ProposalSuppressionDto(0, 0, 0, 0,
                    SUPRIMIDO_A_LAS, LocalDateTime.of(2026, 7, 3, 9, 0)));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.previouslySuppressedAt").value("2026-07-03T09:00:00"));
        }

        /**
         * Un 404 para "ese correo no esta" seria un oraculo: cualquiera con el gate
         * SYSTEM podria enumerar quien ha pedido propuesta. Se responde 200 con ceros.
         */
        @Test
        @DisplayName("un correo sin propuestas responde 200 con ceros, nunca 404")
        void un_correo_sin_propuestas_responde_200() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(0, 0, 0));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"nadie@vet.co\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("la respuesta no devuelve el correo: quien pregunta ya lo escribio")
        void la_respuesta_no_devuelve_el_correo() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(acuse(1, 1, 1));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"laura@vetchapinero.co\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contactEmail").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        /**
         * Sin {@code @Valid} delante del {@code @RequestBody}, el binder no dispara el
         * validador: el {@code @Email} estaria escrito y no se evaluaria nunca (#135).
         * Esto es lo unico que lo comprueba de verdad.
         */
        @Test
        @DisplayName("un correo con forma invalida es 400 y no llega al caso de uso")
        void un_correo_invalido_es_400() throws Exception {
            mockMvc.perform(post("/assistant/proposals/suppress")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"contactEmail\":\"nope\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(suppressUseCase);
        }

        @Test
        @DisplayName("un cuerpo sin correo es 400")
        void un_cuerpo_sin_correo_es_400() throws Exception {
            mockMvc.perform(post("/assistant/proposals/suppress")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(suppressUseCase);
        }
    }
}
