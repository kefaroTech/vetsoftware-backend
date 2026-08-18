package com.vetsoftware.app.companysettings.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.in.ListCompanySettingsUseCase;
import com.vetsoftware.app.companysettings.application.port.in.SetCompanySettingUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de los ajustes por empresa. {@code Authz} viene ya stubeado por
 * {@link WebMvcSliceConfig} con {@code currentCompanyId() == COMPANY_ID}: es lo
 * que permite comprobar que la empresa del command sale del contexto y nunca
 * del cuerpo del request.
 */
@WebMvcTest(CompanySettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanySettingController — contrato HTTP")
class CompanySettingControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCompanySettingsUseCase listUseCase;
    @MockitoBean
    private SetCompanySettingUseCase setUseCase;

    @Nested
    @DisplayName("GET /company-settings")
    class Listado {

        @Test
        @DisplayName("lista los ajustes de la empresa del contexto")
        void lista_los_ajustes_de_la_empresa_del_contexto() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(
                    List.of(new CompanySettingDto("inventory.allow_negative_stock", "true")));

            mockMvc.perform(get("/company-settings")).andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$[0].propertyName").value("inventory.allow_negative_stock"))
                    .andExpect(jsonPath("$[0].value").value("true"));
        }

        @Test
        @DisplayName("una empresa sin ajustes responde una lista vacia")
        void una_empresa_sin_ajustes_responde_una_lista_vacia() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of());

            mockMvc.perform(get("/company-settings")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /company-settings")
    class Escritura {

        @Test
        @DisplayName("responde 200 con el ajuste actualizado")
        void responde_200_con_el_ajuste_actualizado() throws Exception {
            when(setUseCase.set(any())).thenReturn(new CompanySettingDto("k", "v"));

            mockMvc.perform(
                    put("/company-settings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"propertyName":"k","value":"v"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.propertyName").value("k"))
                    .andExpect(jsonPath("$.value").value("v"));
        }

        @Test
        @DisplayName("la empresa la pone el backend desde el contexto, nunca el cliente")
        void la_empresa_la_pone_el_backend_desde_el_contexto() throws Exception {
            when(setUseCase.set(any())).thenReturn(new CompanySettingDto("k", "v"));

            mockMvc.perform(
                    put("/company-settings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"propertyName":"k","value":"v"}
                            """));

            verify(setUseCase).set(new SetCompanySettingCommand(COMPANY_ID, "k", "v"));
        }

        @Test
        @DisplayName("un propertyName en blanco responde 400 y no llama al caso de uso")
        void property_name_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    put("/company-settings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"propertyName":"","value":"v"}
                            """)).andExpect(status().isBadRequest());

            verify(setUseCase, never()).set(any());
        }

        @Test
        @DisplayName("un value en blanco responde 400 y no llama al caso de uso")
        void value_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    put("/company-settings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"propertyName":"k","value":""}
                            """)).andExpect(status().isBadRequest());

            verify(setUseCase, never()).set(any());
        }

        @Test
        @DisplayName("un propertyName de mas de 100 caracteres responde 400")
        void property_name_demasiado_largo_responde_400() throws Exception {
            String largo = "x".repeat(101);

            mockMvc.perform(put("/company-settings").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"propertyName\":\"" + largo + "\",\"value\":\"v\"}"))
                    .andExpect(status().isBadRequest());

            verify(setUseCase, never()).set(any());
        }
    }
}
