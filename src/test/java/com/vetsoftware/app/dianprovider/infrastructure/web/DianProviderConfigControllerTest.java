package com.vetsoftware.app.dianprovider.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.in.CreateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.in.FindDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.in.UpdateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
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
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. La companyId del contexto la fija
 * {@link WebMvcSliceConfig} (siempre {@code COMPANY_ID}); aqui no se prueba
 * autorizacion, esa red la sostiene {@code @PreAuthorize} + ArchUnit.
 */
@WebMvcTest(DianProviderConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DianProviderConfigController — contrato HTTP")
class DianProviderConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDianProviderConfigUseCase createUseCase;
    @MockitoBean
    private UpdateDianProviderConfigUseCase updateUseCase;
    @MockitoBean
    private FindDianProviderConfigUseCase findUseCase;

    private static DianProviderConfigDto dto() {
        return new DianProviderConfigDto(5L, WebMvcSliceConfig.COMPANY_ID, ProviderType.MATIAS,
                "https://api.matias.test", "client-id", true, true, true, false, true, "RES-001",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static final String CUERPO_CREAR = """
            {"provider":"MATIAS","baseUrl":"https://api.matias.test","clientId":"client-id",
             "clientSecret":"client-secret","username":"user@test.com","password":"secret-pass",
             "apiToken":null,"webhookSecret":"webhook-secret","numberingProviderRef":"RES-001"}
            """;

    @Test
    @DisplayName("POST /dian-provider-configs responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(dto());

        mockMvc.perform(post("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CREAR)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.provider").value("MATIAS"))
                .andExpect(jsonPath("$.baseUrl").value("https://api.matias.test"))
                // el secreto en si NUNCA sale en el JSON.
                .andExpect(jsonPath("$.clientSecretConfigured").value(true));
    }

    @Test
    @DisplayName("POST traduce el request al command con la companyId del contexto, no del body")
    void post_traduce_el_request_al_command_con_la_company_id_del_contexto() throws Exception {
        when(createUseCase.execute(any())).thenReturn(dto());

        mockMvc.perform(post("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CREAR));

        verify(createUseCase).execute(new CreateDianProviderConfigCommand(ProviderType.MATIAS,
                "https://api.matias.test", "client-id", "client-secret", "user@test.com",
                "secret-pass", null, "webhook-secret", "RES-001", WebMvcSliceConfig.COMPANY_ID));
    }

    @Test
    @DisplayName("POST sin baseUrl responde 400 y no llega al caso de uso")
    void post_sin_base_url_responde_400() throws Exception {
        mockMvc.perform(
                post("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"MATIAS","baseUrl":""}
                        """)).andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(createUseCase);
    }

    @Test
    @DisplayName("POST sin provider responde 400 y no llega al caso de uso")
    void post_sin_provider_responde_400() throws Exception {
        mockMvc.perform(
                post("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"baseUrl":"https://api.matias.test"}
                        """)).andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(createUseCase);
    }

    @Test
    @DisplayName("PUT /dian-provider-configs responde 200 con la config actualizada")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(dto());

        mockMvc.perform(put("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CREAR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value("https://api.matias.test"));
    }

    @Test
    @DisplayName("PUT traduce el request al command con la companyId del contexto")
    void put_traduce_el_request_al_command_con_la_company_id_del_contexto() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(dto());

        mockMvc.perform(put("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CREAR));

        verify(updateUseCase).execute(new UpdateDianProviderConfigCommand(ProviderType.MATIAS,
                "https://api.matias.test", "client-id", "client-secret", "user@test.com",
                "secret-pass", null, "webhook-secret", "RES-001", WebMvcSliceConfig.COMPANY_ID));
    }

    @Test
    @DisplayName("PUT sobre una empresa sin config responde 404, no 500")
    void put_sobre_empresa_sin_config_responde_404() throws Exception {
        when(updateUseCase.execute(any()))
                .thenThrow(new DianProviderConfigNotFoundException(WebMvcSliceConfig.COMPANY_ID));

        mockMvc.perform(put("/dian-provider-configs").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_CREAR)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /dian-provider-configs devuelve la config de la empresa del contexto")
    void get_devuelve_la_config_de_la_empresa_del_contexto() throws Exception {
        when(findUseCase.findByCompany(WebMvcSliceConfig.COMPANY_ID)).thenReturn(dto());

        mockMvc.perform(get("/dian-provider-configs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.numberingProviderRef").value("RES-001"));
    }

    @Test
    @DisplayName("GET sobre una empresa sin config responde 404, no 500")
    void get_sobre_empresa_sin_config_responde_404() throws Exception {
        when(findUseCase.findByCompany(WebMvcSliceConfig.COMPANY_ID))
                .thenThrow(new DianProviderConfigNotFoundException(WebMvcSliceConfig.COMPANY_ID));

        mockMvc.perform(get("/dian-provider-configs")).andExpect(status().isNotFound());
    }
}
