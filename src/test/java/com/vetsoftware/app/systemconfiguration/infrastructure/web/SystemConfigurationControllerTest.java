package com.vetsoftware.app.systemconfiguration.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.in.ListSystemConfigurationsUseCase;
import com.vetsoftware.app.systemconfiguration.application.port.in.SetSystemConfigurationUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
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
 * Rodaja HTTP de la configuracion global. A diferencia de
 * {@code CompanySettingController}, este controller no usa {@code Authz}: la
 * configuracion no esta scoped a empresa.
 */
@WebMvcTest(SystemConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemConfigurationController — contrato HTTP")
class SystemConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListSystemConfigurationsUseCase listUseCase;
    @MockitoBean
    private SetSystemConfigurationUseCase setUseCase;

    @Nested
    @DisplayName("GET /system-configurations")
    class Listado {

        @Test
        @DisplayName("lista todas las configuraciones del sistema")
        void lista_todas_las_configuraciones_del_sistema() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(new SystemConfigurationDto(1L, "uvt",
                    "47065", LocalDateTime.of(2026, 1, 15, 10, 30), true)));

            mockMvc.perform(get("/system-configurations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].propertyName").value("uvt"))
                    .andExpect(jsonPath("$[0].value").value("47065"))
                    .andExpect(jsonPath("$[0].enabled").value(true));
        }

        @Test
        @DisplayName("sin configuraciones responde una lista vacia")
        void sin_configuraciones_responde_una_lista_vacia() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/system-configurations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /system-configurations")
    class Escritura {

        @Test
        @DisplayName("responde 200 con la configuracion actualizada")
        void responde_200_con_la_configuracion_actualizada() throws Exception {
            when(setUseCase.execute(any())).thenReturn(new SystemConfigurationDto(1L, "uvt",
                    "48000", LocalDateTime.of(2026, 1, 15, 10, 30), true));

            mockMvc.perform(put("/system-configurations").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"propertyName":"uvt","value":"48000"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.value").value("48000"));
        }

        @Test
        @DisplayName("traduce el request en el command tal cual: no hay sello de tenant que inyectar")
        void traduce_el_request_en_el_command_tal_cual() throws Exception {
            when(setUseCase.execute(any())).thenReturn(new SystemConfigurationDto(1L, "uvt",
                    "48000", LocalDateTime.of(2026, 1, 15, 10, 30), true));

            mockMvc.perform(put("/system-configurations").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"propertyName":"uvt","value":"48000"}
                            """));

            verify(setUseCase).execute(new SetSystemConfigurationCommand("uvt", "48000"));
        }

        @Test
        @DisplayName("un propertyName en blanco responde 400 y no llama al caso de uso")
        void property_name_en_blanco_responde_400() throws Exception {
            mockMvc.perform(put("/system-configurations").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"propertyName":"","value":"48000"}
                            """)).andExpect(status().isBadRequest());

            verify(setUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un value de mas de 255 caracteres responde 400")
        void value_demasiado_largo_responde_400() throws Exception {
            String largo = "x".repeat(256);

            mockMvc.perform(put("/system-configurations").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"propertyName\":\"uvt\",\"value\":\"" + largo + "\"}"))
                    .andExpect(status().isBadRequest());

            verify(setUseCase, never()).execute(any());
        }
    }
}
