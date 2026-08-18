package com.vetsoftware.app.numberingresolution.infrastructure.web;

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

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.CompanySummaryDto;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.CreateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.DeleteNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.FindNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.ListNumberingResolutionsUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.ReactivateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.UpdateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionAlreadyActiveException;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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

@WebMvcTest(NumberingResolutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("NumberingResolutionController — contrato HTTP")
class NumberingResolutionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long RESOLUTION_ID = 700L;

    private static final String CUERPO_VALIDO = """
            {"documentType":"FE_VENTA","resolutionNumber":"18760000001","resolutionDate":"2026-01-02",
             "prefix":"SETP","rangeFrom":100,"rangeTo":199,"validFrom":"2026-01-01","validTo":"2026-12-31",
             "technicalKey":"clave-tecnica"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateNumberingResolutionUseCase createUseCase;
    @MockitoBean
    private UpdateNumberingResolutionUseCase updateUseCase;
    @MockitoBean
    private FindNumberingResolutionUseCase findUseCase;
    @MockitoBean
    private ListNumberingResolutionsUseCase listUseCase;
    @MockitoBean
    private DeleteNumberingResolutionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateNumberingResolutionUseCase reactivateUseCase;

    /**
     * El doble de {@code Authz} lo aporta {@link WebMvcSliceConfig}; se inyecta
     * aqui para poder afirmar que el borrado propaga la empresa del contexto, que
     * es la mitad del arreglo de aislamiento (la otra mitad vive en el service).
     */
    @Autowired
    private com.vetsoftware.app.auth.infrastructure.security.Authz authz;

    private static NumberingResolutionDto resolucion(boolean enabled) {
        return new NumberingResolutionDto(RESOLUTION_ID,
                new CompanySummaryDto(COMPANY_ID, "Veterinaria Central", "900123456"), null,
                ElectronicDocumentType.FE_VENTA, "18760000001", LocalDate.of(2026, 1, 2), "SETP",
                100L, 199L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "clave-tecnica",
                100L, LocalDateTime.of(2026, 1, 2, 9, 0), enabled);
    }

    private static NumberingResolutionDto activa() {
        return resolucion(true);
    }

    @Nested
    @DisplayName("POST /numbering-resolutions")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la resolucion creada")
        void responde_201_con_la_resolucion_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(700))
                    .andExpect(jsonPath("$.resolutionNumber").value("18760000001"))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria crear una resolucion en otra empresa.
            verify(createUseCase).execute(new CreateNumberingResolutionCommand(
                    ElectronicDocumentType.FE_VENTA, "18760000001", LocalDate.of(2026, 1, 2),
                    "SETP", 100L, 199L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "clave-tecnica", null, COMPANY_ID));
        }

        @Test
        @DisplayName("sin numero de resolucion responde 400 y no llega al caso de uso")
        void sin_numero_de_resolucion_responde_400() throws Exception {
            mockMvc.perform(
                    post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"documentType":"FE_VENTA","resolutionNumber":"","resolutionDate":"2026-01-02",
                                     "rangeFrom":100,"rangeTo":199,"validFrom":"2026-01-01","validTo":"2026-12-31"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin tipo de documento responde 400")
        void sin_tipo_de_documento_responde_400() throws Exception {
            mockMvc.perform(
                    post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"resolutionNumber":"18760000001","resolutionDate":"2026-01-02",
                                     "rangeFrom":100,"rangeTo":199,"validFrom":"2026-01-01","validTo":"2026-12-31"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un rangeFrom menor a 1 responde 400")
        void range_from_menor_a_uno_responde_400() throws Exception {
            mockMvc.perform(
                    post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"documentType":"FE_VENTA","resolutionNumber":"18760000001","resolutionDate":"2026-01-02",
                                     "rangeFrom":0,"rangeTo":199,"validFrom":"2026-01-01","validTo":"2026-12-31"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin fecha de vigencia desde responde 400")
        void sin_valid_from_responde_400() throws Exception {
            mockMvc.perform(
                    post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"documentType":"FE_VENTA","resolutionNumber":"18760000001","resolutionDate":"2026-01-02",
                                     "rangeFrom":100,"rangeTo":199,"validTo":"2026-12-31"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una resolucion ya activa en el alcance responde 409")
        void resolucion_ya_activa_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new NumberingResolutionAlreadyActiveException(COMPANY_ID,
                            ElectronicDocumentType.FE_VENTA));

            mockMvc.perform(post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una empresa o sede inexistente sale como 400, no 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: " + COMPANY_ID));

            mockMvc.perform(post("/numbering-resolutions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /numbering-resolutions lista las resoluciones de la empresa del contexto")
        void get_lista_las_resoluciones_de_la_empresa() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(activa()));

            mockMvc.perform(get("/numbering-resolutions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(700));
        }

        @Test
        @DisplayName("GET /numbering-resolutions/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(RESOLUTION_ID, COMPANY_ID)).thenReturn(activa());

            mockMvc.perform(get("/numbering-resolutions/700")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(700));
        }

        @Test
        @DisplayName("GET /numbering-resolutions/{id} de otra empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new NumberingResolutionNotFoundException(99L));

            mockMvc.perform(get("/numbering-resolutions/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("una resolucion sin empresa resuelta en el dto no rompe la respuesta")
        void una_resolucion_sin_empresa_resuelta_no_rompe_la_respuesta() throws Exception {
            NumberingResolutionDto sinEmpresa = new NumberingResolutionDto(RESOLUTION_ID, null,
                    null, ElectronicDocumentType.FE_VENTA, "18760000001", LocalDate.of(2026, 1, 2),
                    "SETP", 100L, 199L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "clave-tecnica", 100L, LocalDateTime.of(2026, 1, 2, 9, 0), true);
            when(findUseCase.findById(RESOLUTION_ID, COMPANY_ID)).thenReturn(sinEmpresa);

            mockMvc.perform(get("/numbering-resolutions/700")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.company").doesNotExist());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una resolucion existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /numbering-resolutions/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(put("/numbering-resolutions/700")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(700));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la empresa del contexto")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activa());

            mockMvc.perform(put("/numbering-resolutions/700")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateNumberingResolutionCommand(RESOLUTION_ID,
                    ElectronicDocumentType.FE_VENTA, "18760000001", LocalDate.of(2026, 1, 2),
                    "SETP", 100L, 199L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    "clave-tecnica", null, COMPANY_ID));
        }

        @Test
        @DisplayName("PUT con datos invalidos responde 400 y no llega al caso de uso")
        void put_con_datos_invalidos_responde_400() throws Exception {
            mockMvc.perform(put("/numbering-resolutions/700")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"documentType":"FE_VENTA","resolutionNumber":"18760000001","resolutionDate":"2026-01-02",
                                     "rangeFrom":100,"validFrom":"2026-01-01","validTo":"2026-12-31"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre una resolucion inexistente responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new NumberingResolutionNotFoundException(RESOLUTION_ID));

            mockMvc.perform(put("/numbering-resolutions/700")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT que solapa con otra resolucion activa responde 409")
        void put_con_solapamiento_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new NumberingResolutionAlreadyActiveException(COMPANY_ID,
                            ElectronicDocumentType.FE_VENTA));

            mockMvc.perform(put("/numbering-resolutions/700")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /numbering-resolutions/{id} responde 204 y propaga la empresa del contexto")
        void delete_responde_204() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);

            mockMvc.perform(delete("/numbering-resolutions/700")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(RESOLUTION_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una resolucion inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            org.mockito.Mockito.doThrow(new NumberingResolutionNotFoundException(99L))
                    .when(deleteUseCase).execute(99L, COMPANY_ID);

            mockMvc.perform(delete("/numbering-resolutions/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /numbering-resolutions/{id}/enable responde 200 y propaga la empresa del contexto")
        void enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(RESOLUTION_ID, COMPANY_ID)).thenReturn(activa());

            mockMvc.perform(patch("/numbering-resolutions/700/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(RESOLUTION_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH enable de una resolucion inexistente responde 404")
        void enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new NumberingResolutionNotFoundException(99L));

            mockMvc.perform(patch("/numbering-resolutions/99/enable"))
                    .andExpect(status().isNotFound());
        }
    }
}
