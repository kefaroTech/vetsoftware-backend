package com.vetsoftware.app.paymentgateway.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import com.vetsoftware.app.paymentgateway.domain.WompiChecksumMismatchException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
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
 * Ruta pública: sin JWT, la autenticidad la valida el caso de uso con el
 * checksum del cuerpo. Aquí solo se comprueba el mapeo HTTP —200 en el camino
 * feliz, 401 cuando el caso de uso rechaza el checksum— y que el cuerpo crudo y
 * la cabecera llegan intactos al comando.
 */
@WebMvcTest(WompiWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("WompiWebhookController — contrato HTTP")
class WompiWebhookControllerTest {

    private static final String CUERPO = "{\"event\":\"transaction.updated\"}";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProcessWompiEventUseCase processEventUseCase;

    @Test
    @DisplayName("un evento autentico responde 200 vacio")
    void evento_autentico_responde_200() throws Exception {
        mockMvc.perform(
                post("/payment-gateway/wompi/events").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Event-Checksum", "abc123").content(CUERPO))
                .andExpect(status().isOk());

        ArgumentCaptor<ProcessWompiEventCommand> captor = ArgumentCaptor
                .forClass(ProcessWompiEventCommand.class);
        verify(processEventUseCase).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().rawBody()).isEqualTo(CUERPO);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().checksumHeader())
                .isEqualTo("abc123");
    }

    @Test
    @DisplayName("un checksum invalido responde 401, no 200")
    void checksum_invalido_responde_401() throws Exception {
        doThrow(new WompiChecksumMismatchException("checksum invalido")).when(processEventUseCase)
                .execute(any());

        mockMvc.perform(
                post("/payment-gateway/wompi/events").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Event-Checksum", "forjado").content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("sin cabecera de checksum tambien llega al caso de uso, que es quien decide")
    void sin_cabecera_llega_igual() throws Exception {
        mockMvc.perform(post("/payment-gateway/wompi/events")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());

        ArgumentCaptor<ProcessWompiEventCommand> captor = ArgumentCaptor
                .forClass(ProcessWompiEventCommand.class);
        verify(processEventUseCase).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().checksumHeader()).isNull();
    }
}
