package com.vetsoftware.app.tax.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.application.dto.CompanySummaryDto;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.CreateTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.DeleteTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.FindTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.ListTaxesUseCase;
import com.vetsoftware.app.tax.application.port.in.ReactivateTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.UpdateTaxUseCase;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import com.vetsoftware.app.tax.domain.TaxScheme;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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

@WebMvcTest(TaxController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("TaxController — contrato HTTP")
class TaxControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final Long TAX_ID = 500L;

    private static final String CUERPO_VALIDO = """
            {"name":"IVA General","percentage":19.00,"taxScheme":"IVA"}
            """;

    private static final String CUERPO_VALIDO_UPDATE = """
            {"name":"IVA General","percentage":19.00,"taxScheme":"IVA","version":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTaxUseCase createUseCase;
    @MockitoBean
    private UpdateTaxUseCase updateUseCase;
    @MockitoBean
    private FindTaxUseCase findUseCase;
    @MockitoBean
    private ListTaxesUseCase listUseCase;
    @MockitoBean
    private DeleteTaxUseCase deleteUseCase;
    @MockitoBean
    private ReactivateTaxUseCase reactivateUseCase;

    private static TaxDto impuesto(boolean enabled) {
        return new TaxDto(TAX_ID, "IVA General", new BigDecimal("19.00"), TaxScheme.IVA,
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "900123456"),
                LocalDateTime.of(2026, 3, 12, 9, 15), null, null, 0L, enabled);
    }

    private static TaxDto activo() {
        return impuesto(true);
    }

    @Nested
    @DisplayName("POST /taxes")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el impuesto creado")
        void responde_201_con_el_impuesto_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(
                    post("/taxes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.name").value("IVA General"))
                    .andExpect(jsonPath("$.percentage").value(19.00))
                    .andExpect(jsonPath("$.taxScheme").value("IVA"))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.version").value(0))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(
                    post("/taxes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria crear un impuesto en otra empresa.
            verify(createUseCase).execute(new CreateTaxCommand("IVA General",
                    new BigDecimal("19.00"), TaxScheme.IVA, COMPANY_ID));
        }

        @Test
        @DisplayName("un nombre en blanco responde 400 y no llega al caso de uso")
        void nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(post("/taxes").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","percentage":19.00,"taxScheme":"IVA"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un porcentaje negativo responde 400")
        void porcentaje_negativo_responde_400() throws Exception {
            mockMvc.perform(post("/taxes").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"IVA General","percentage":-1,"taxScheme":"IVA"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un porcentaje mayor a 100 responde 400")
        void porcentaje_mayor_a_100_responde_400() throws Exception {
            mockMvc.perform(post("/taxes").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"IVA General","percentage":100.01,"taxScheme":"IVA"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin esquema tributario responde 400")
        void sin_esquema_responde_400() throws Exception {
            mockMvc.perform(post("/taxes").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"IVA General","percentage":19.00}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un nombre ya usado responde 409")
        void nombre_ya_usado_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new TaxNameAlreadyExistsException("IVA General"));

            mockMvc.perform(
                    post("/taxes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: " + COMPANY_ID));

            mockMvc.perform(
                    post("/taxes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /taxes lista los impuestos activos de la empresa del contexto")
        void get_lista_los_activos() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(activo()));

            mockMvc.perform(get("/taxes")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(500));
        }

        @Test
        @DisplayName("GET /taxes/disabled lista los pausados de la empresa del contexto")
        void get_disabled_lista_los_pausados() throws Exception {
            when(listUseCase.listDisabledByCompany(COMPANY_ID))
                    .thenReturn(List.of(impuesto(false)));

            mockMvc.perform(get("/taxes/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].enabled").value(false));
        }

        @Test
        @DisplayName("GET /taxes/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(TAX_ID, COMPANY_ID)).thenReturn(activo());

            mockMvc.perform(get("/taxes/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500));
        }

        @Test
        @DisplayName("GET /taxes/{id} de otra empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID)).thenThrow(new TaxNotFoundException(99L));

            mockMvc.perform(get("/taxes/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un impuesto existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /taxes/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(put("/taxes/500").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la version")
        void put_traduce_el_request_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(put("/taxes/500").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE));

            verify(updateUseCase)
                    .execute(new UpdateTaxCommand(TAX_ID, "IVA General", new BigDecimal("19.00"),
                            TaxScheme.IVA, COMPANY_ID, WebMvcSliceConfig.EMPLOYEE_ID, 3L));
        }

        @Test
        @DisplayName("PUT sin version responde 400: sin ella no hay lock optimista")
        void put_sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/taxes/500").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un nombre ya usado por otro impuesto responde 409")
        void put_con_nombre_ya_usado_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new TaxNameAlreadyExistsException("IVA General"));

            mockMvc.perform(put("/taxes/500").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PUT sobre un impuesto de otra empresa responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new TaxNotFoundException(TAX_ID));

            mockMvc.perform(put("/taxes/500").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /taxes/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/taxes/500")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(TAX_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un impuesto inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new TaxNotFoundException(99L)).when(deleteUseCase)
                    .execute(99L, COMPANY_ID);

            mockMvc.perform(delete("/taxes/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de un impuesto con hijos activos responde 409")
        void delete_con_hijos_activos_responde_409() throws Exception {
            org.mockito.Mockito
                    .doThrow(new TaxHasActiveChildrenException(TAX_ID, "product/service"))
                    .when(deleteUseCase).execute(TAX_ID, COMPANY_ID);

            mockMvc.perform(delete("/taxes/500")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /taxes/{id}/enable responde 200 con el impuesto reactivado")
        void enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(TAX_ID, COMPANY_ID)).thenReturn(activo());

            mockMvc.perform(patch("/taxes/500/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de un impuesto inexistente responde 404")
        void enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new TaxNotFoundException(99L));

            mockMvc.perform(patch("/taxes/99/enable")).andExpect(status().isNotFound());
        }
    }
}
