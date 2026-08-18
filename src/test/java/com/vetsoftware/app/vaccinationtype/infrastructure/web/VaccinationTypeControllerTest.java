package com.vetsoftware.app.vaccinationtype.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.CreateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.FindVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListAvailableVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ReactivateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
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
 * Rodaja HTTP del controller de tipos de vacuna: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles de
 * los puertos de entrada.
 *
 * <p>
 * La comprobacion que mas vale de todas es que el {@code companyId} del command
 * sale del contexto de autorizacion y no del cuerpo: el request no tiene donde
 * declararlo, y si algun dia lo tuviera este test lo delataria.
 */
@WebMvcTest(VaccinationTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("VaccinationTypeController — contrato HTTP")
class VaccinationTypeControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Rabia","description":"Vacuna antirrabica","general":false}
            """;

    private static final String NOMBRE_DE_101_CARACTERES = "x".repeat(101);
    private static final String DESCRIPCION_DE_501_CARACTERES = "x".repeat(501);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateVaccinationTypeUseCase createUseCase;
    @MockitoBean
    private UpdateVaccinationTypeUseCase updateUseCase;
    @MockitoBean
    private FindVaccinationTypeUseCase findUseCase;
    @MockitoBean
    private ListVaccinationTypesUseCase listUseCase;
    @MockitoBean
    private ListAvailableVaccinationTypesUseCase listAvailableUseCase;
    @MockitoBean
    private DeleteVaccinationTypeUseCase deleteUseCase;
    @MockitoBean
    private ReactivateVaccinationTypeUseCase reactivateUseCase;

    /**
     * El doble de {@code Authz} lo aporta {@link WebMvcSliceConfig}; se inyecta
     * aqui para afirmar que el borrado propaga la empresa del contexto, que es la
     * mitad del arreglo de aislamiento (la otra mitad vive en el service).
     */
    @Autowired
    private com.vetsoftware.app.auth.infrastructure.security.Authz authz;

    private static VaccinationTypeDto rabia() {
        return new VaccinationTypeDto(50L, "Rabia", "Vacuna antirrabica",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"), false,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static VaccinationTypeDto vacunaUniversal() {
        return new VaccinationTypeDto(60L, "Vacuna universal", "Disponible para todas", null, true,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static CreateVaccinationTypeCommand comandoDeCreacionEsperado() {
        return new CreateVaccinationTypeCommand("Rabia", "Vacuna antirrabica", COMPANY_ID, false);
    }

    @Nested
    @DisplayName("POST /vaccination-types")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el tipo creado")
        void responde_201_con_el_tipo_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(rabia());

            mockMvc.perform(post("/vaccination-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(50))
                    .andExpect(jsonPath("$.name").value("Rabia"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.general").value(false))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(rabia());

            mockMvc.perform(post("/vaccination-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("un tipo general responde 201 sin compania en el cuerpo")
        void un_tipo_general_responde_201_sin_compania() throws Exception {
            when(createUseCase.execute(any())).thenReturn(vacunaUniversal());

            mockMvc.perform(
                    post("/vaccination-types").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Vacuna universal","description":"Disponible para todas","general":true}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.company").doesNotExist())
                    .andExpect(jsonPath("$.general").value(true));

            verify(createUseCase).execute(new CreateVaccinationTypeCommand("Vacuna universal",
                    "Disponible para todas", COMPANY_ID, true));
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    post("/vaccination-types").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"Vacuna antirrabica","general":false}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 100 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/vaccination-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + NOMBRE_DE_101_CARACTERES
                            + "\",\"description\":\"Vacuna antirrabica\",\"general\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("descripcion de mas de 500 caracteres responde 400")
        void descripcion_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(post("/vaccination-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Rabia\",\"description\":\""
                            + DESCRIPCION_DE_501_CARACTERES + "\",\"general\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un tipo invalido segun el dominio sale como 400, no 500")
        void un_tipo_invalido_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("general type cannot have company"));

            mockMvc.perform(post("/vaccination-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /vaccination-types lista todos los tipos")
        void get_lista_todos_los_tipos() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(rabia()));

            mockMvc.perform(get("/vaccination-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(50));
        }

        @Test
        @DisplayName("GET /vaccination-types/available lista los disponibles para la empresa del contexto")
        void get_available_lista_los_disponibles_de_la_empresa() throws Exception {
            when(listAvailableUseCase.listAvailable(COMPANY_ID))
                    .thenReturn(List.of(rabia(), vacunaUniversal()));

            mockMvc.perform(get("/vaccination-types/available")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].general").value(true));
        }

        @Test
        @DisplayName("GET /vaccination-types/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(50L, COMPANY_ID)).thenReturn(rabia());

            mockMvc.perform(get("/vaccination-types/50")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(50))
                    .andExpect(jsonPath("$.description").value("Vacuna antirrabica"));
        }

        @Test
        @DisplayName("GET /vaccination-types/{id} inexistente para la empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new VaccinationTypeNotFoundException(99L));

            mockMvc.perform(get("/vaccination-types/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un tipo existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /vaccination-types/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(rabia());

            mockMvc.perform(put("/vaccination-types/50").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(50));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la empresa del contexto")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(rabia());

            mockMvc.perform(put("/vaccination-types/50").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateVaccinationTypeCommand(50L, "Rabia",
                    "Vacuna antirrabica", COMPANY_ID, false));
        }

        @Test
        @DisplayName("PUT con nombre vacio responde 400 y no llega al caso de uso")
        void put_con_nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    put("/vaccination-types/50").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"Vacuna antirrabica","general":false}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un tipo inexistente responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new VaccinationTypeNotFoundException(50L));

            mockMvc.perform(put("/vaccination-types/50").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /vaccination-types/{id} responde 204 y propaga la empresa del contexto")
        void delete_responde_204() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);

            mockMvc.perform(delete("/vaccination-types/50")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(50L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un tipo inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            doThrow(new VaccinationTypeNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/vaccination-types/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de un tipo con vacunas activas responde 409")
        void delete_con_vacunas_activas_responde_409() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            doThrow(new VaccinationTypeHasActiveChildrenException(50L, "vaccination"))
                    .when(deleteUseCase).execute(50L, COMPANY_ID);

            mockMvc.perform(delete("/vaccination-types/50")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /vaccination-types/{id}/enable reactiva y responde 200 propagando la empresa")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(50L, COMPANY_ID)).thenReturn(rabia());

            mockMvc.perform(patch("/vaccination-types/50/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(50))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(50L, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH enable de un tipo inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new VaccinationTypeNotFoundException(99L));

            mockMvc.perform(patch("/vaccination-types/99/enable")).andExpect(status().isNotFound());
        }
    }
}
