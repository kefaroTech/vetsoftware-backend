package com.vetsoftware.app.electronicdocument.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.ProcessProviderWebhookUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del webhook DIAN — ruta publica, cuerpo crudo y cabecera de firma
 * trasladados tal cual al comando; la verificacion HMAC vive dentro del caso de
 * uso, no aqui.
 */
@WebMvcTest(DianWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DianWebhookController — contrato HTTP")
class DianWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessProviderWebhookUseCase useCase;

    @Test
    @DisplayName("traslada proveedor, cuerpo crudo y firma al comando, y responde 200")
    void traslada_provider_cuerpo_y_firma_al_comando() throws Exception {
        mockMvc.perform(post("/dian/webhooks/MATIAS").contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Signature", "sha256=abc123")
                .content("{\"event\":\"invoice.validated\"}")).andExpect(status().isOk());

        verify(useCase).execute(new ProcessProviderWebhookCommand("MATIAS",
                "{\"event\":\"invoice.validated\"}", "sha256=abc123"));
    }

    @Test
    @DisplayName("sin cabecera de firma, la traslada como null")
    void sin_cabecera_de_firma_la_traslada_como_null() throws Exception {
        mockMvc.perform(
                post("/dian/webhooks/MATIAS").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(useCase).execute(new ProcessProviderWebhookCommand("MATIAS", "{}", null));
    }
}
