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

    @Nested
    @DisplayName("Supresion")
    class Supresion {

        @Test
        @DisplayName("responde 200 con el desglose por tabla, no con un total suelto")
        void responde_200_con_el_desglose() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(new ProposalSuppressionDto(1, 2, 4, 7));

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
            when(suppressUseCase.execute(any())).thenReturn(new ProposalSuppressionDto(0, 0, 0, 0));

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
         * Un 404 para "ese correo no esta" seria un oraculo: cualquiera con el gate
         * SYSTEM podria enumerar quien ha pedido propuesta. Se responde 200 con ceros.
         */
        @Test
        @DisplayName("un correo sin propuestas responde 200 con ceros, nunca 404")
        void un_correo_sin_propuestas_responde_200() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(new ProposalSuppressionDto(0, 0, 0, 0));

            mockMvc.perform(
                    post("/assistant/proposals/suppress").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contactEmail\":\"nadie@vet.co\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("la respuesta no devuelve el correo: quien pregunta ya lo escribio")
        void la_respuesta_no_devuelve_el_correo() throws Exception {
            when(suppressUseCase.execute(any())).thenReturn(new ProposalSuppressionDto(1, 1, 1, 3));

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
