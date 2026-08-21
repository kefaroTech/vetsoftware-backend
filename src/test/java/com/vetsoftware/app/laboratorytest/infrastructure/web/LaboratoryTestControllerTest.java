package com.vetsoftware.app.laboratorytest.infrastructure.web;

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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ChangeLaboratoryTestStatusUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.CreateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.DeleteLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.FindLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsByAnimalUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.SearchLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.UpdateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja HTTP de {@link LaboratoryTestController}: rutas, binding, validacion
 * del request, codigos de estado y forma del JSON.
 *
 * <p>
 * Lo que esta rodaja protege y ninguna otra capa cubre: que la empresa y la
 * sede <strong>nunca</strong> viajen tal cual del cuerpo — la empresa sale de
 * {@code Authz.currentCompanyId()} y la sede siempre pasa por
 * {@code resolveAccessibleBranch}, con un doble que devuelve una sede distinta
 * de la pedida a proposito.
 */
@WebMvcTest(LaboratoryTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("LaboratoryTestController — contrato HTTP")
class LaboratoryTestControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    /** Sede que resuelve {@code Authz}; distinta de la que manda el request. */
    private static final Long SEDE_RESUELTA = 31L;
    private static final long SEDE_PEDIDA = 77L;

    private static final String ALTA_VALIDA = """
            {"date":"2026-03-15","testTypeId":4,"quantity":2,
             "diagnosis":"Sospecha de anemia","status":"PENDING_COLLECTION",
             "prioridad":"NORMAL","animalId":7,"consultationId":11,"branchId":77}
            """;

    private static final String ACTUALIZACION_VALIDA = """
            {"date":"2026-03-16","testTypeId":4,"quantity":3,
             "diagnosis":"Anemia regenerativa","prioridad":"URGENTE","animalId":7,
             "consultationId":11}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateLaboratoryTestUseCase createUseCase;
    @MockitoBean
    private UpdateLaboratoryTestUseCase updateUseCase;
    @MockitoBean
    private ChangeLaboratoryTestStatusUseCase changeStatusUseCase;
    @MockitoBean
    private FindLaboratoryTestUseCase findUseCase;
    @MockitoBean
    private ListLaboratoryTestsUseCase listUseCase;
    @MockitoBean
    private ListLaboratoryTestsByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private SearchLaboratoryTestsUseCase searchUseCase;
    @MockitoBean
    private DeleteLaboratoryTestUseCase deleteUseCase;

    /**
     * Sin este stub {@code resolveAccessibleBranch} devolveria 0L —el default de
     * Mockito para un {@code Long}, no null— y la muestra se crearia en una sede
     * inexistente sin que el test lo notara.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static LaboratoryTestDto pendiente() {
        return LaboratoryTestDto.from(LaboratoryTestMother.pendienteDeToma());
    }

    @Nested
    @DisplayName("POST /laboratory-tests")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la muestra creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(pendiente());

            mockMvc.perform(post("/laboratory-tests").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(LaboratoryTestMother.ID))
                    .andExpect(jsonPath("$.testType.name").value("Hemograma"))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"));
        }

        @Test
        @DisplayName("la empresa sale del contexto y la sede de resolveAccessibleBranch, nunca del cuerpo")
        void la_empresa_y_la_sede_salen_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(pendiente());

            mockMvc.perform(post("/laboratory-tests").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA));

            verify(createUseCase).execute(new CreateLaboratoryTestCommand(LocalDate.of(2026, 3, 15),
                    4L, 2, "Sospecha de anemia", "PENDING_COLLECTION", "NORMAL", 7L, 11L,
                    COMPANY_ID, SEDE_RESUELTA, null, null));
        }

        @Test
        @DisplayName("sin tipo de examen responde 400 y no crea nada")
        void sin_tipo_de_examen_responde_400() throws Exception {
            mockMvc.perform(
                    post("/laboratory-tests").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-15","quantity":2,"animalId":7}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin animal responde 400 y no crea nada")
        void sin_animal_responde_400() throws Exception {
            mockMvc.perform(
                    post("/laboratory-tests").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-15","testTypeId":4,"quantity":2}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una cantidad menor a uno responde 400")
        void una_cantidad_menor_a_uno_responde_400() throws Exception {
            mockMvc.perform(
                    post("/laboratory-tests").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-15","testTypeId":4,"quantity":0,"animalId":7}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /laboratory-tests")
    class Listado {

        @Test
        @DisplayName("devuelve la lista completa, sin envoltura de pagina")
        void devuelve_la_lista_completa() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(pendiente()));

            mockMvc.perform(get("/laboratory-tests")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].animal.name").value("Firulais"));
        }
    }

    @Nested
    @DisplayName("GET /laboratory-tests/by-animal/{animalId}")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve la envoltura paginada de la muestras del animal")
        void devuelve_la_envoltura_paginada() throws Exception {
            when(listByAnimalUseCase.listByAnimal(7L, COMPANY_ID, null, 0, 20))
                    .thenReturn(new PageResult<>(List.of(pendiente()), 0, 20, 1L, 1));

            mockMvc.perform(get("/laboratory-tests/by-animal/7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].animal.id").value(7))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("la empresa del filtro sale del contexto, no de un parametro")
        void la_empresa_del_filtro_sale_del_contexto() throws Exception {
            when(listByAnimalUseCase.listByAnimal(7L, COMPANY_ID, "anemia", 1, 10))
                    .thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

            mockMvc.perform(get("/laboratory-tests/by-animal/7").param("q", "anemia")
                    .param("page", "1").param("pageSize", "10")).andExpect(status().isOk());

            verify(listByAnimalUseCase).listByAnimal(7L, COMPANY_ID, "anemia", 1, 10);
        }
    }

    @Nested
    @DisplayName("GET /laboratory-tests/search")
    class Busqueda {

        @Test
        @DisplayName("sin parametros busca solo con la empresa y la sede resuelta")
        void sin_parametros_busca_solo_con_empresa_y_sede() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(pendiente()), 0, 20, 1L, 1));

            mockMvc.perform(get("/laboratory-tests/search")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(searchUseCase).execute(new SearchLaboratoryTestsCommand(COMPANY_ID,
                    SEDE_RESUELTA, List.of(), null, null, null, null, null, 0, 20));
        }

        @Test
        @DisplayName("acepta el estado y la prioridad en minusculas como los manda el front")
        void acepta_estado_y_prioridad_en_minusculas() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/laboratory-tests/search").param("statuses", "pending_collection")
                    .param("prioridad", "urgente")).andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchLaboratoryTestsCommand(COMPANY_ID,
                    SEDE_RESUELTA, List.of(LaboratoryTestStatus.PENDING_COLLECTION), null, null,
                    LaboratoryTestPriority.URGENTE, null, null, 0, 20));
        }

        @Test
        @DisplayName("un estado desconocido responde 400, no 500")
        void un_estado_desconocido_responde_400() throws Exception {
            mockMvc.perform(get("/laboratory-tests/search").param("statuses", "archivado"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /laboratory-tests/{id}")
    class BusquedaPorId {

        @Test
        @DisplayName("devuelve el detalle de la muestra")
        void devuelve_el_detalle() throws Exception {
            when(findUseCase.findById(LaboratoryTestMother.ID, COMPANY_ID)).thenReturn(pendiente());

            mockMvc.perform(get("/laboratory-tests/" + LaboratoryTestMother.ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(LaboratoryTestMother.ID));
        }

        @Test
        @DisplayName("una muestra inexistente responde 404, no 500")
        void una_muestra_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new LaboratoryTestNotFoundException(999L));

            mockMvc.perform(get("/laboratory-tests/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /laboratory-tests/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con la muestra actualizada")
        void responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(pendiente());

            mockMvc.perform(put("/laboratory-tests/" + LaboratoryTestMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.animal.name").value("Firulais"));
        }

        @Test
        @DisplayName("el id sale de la ruta y la empresa del contexto, nunca del cuerpo")
        void el_id_y_la_empresa_salen_de_ruta_y_contexto() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(pendiente());

            mockMvc.perform(put("/laboratory-tests/" + LaboratoryTestMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA));

            verify(updateUseCase).execute(new UpdateLaboratoryTestCommand(LaboratoryTestMother.ID,
                    LocalDate.of(2026, 3, 16), 4L, 3, "Anemia regenerativa", "URGENTE", 7L, 11L,
                    COMPANY_ID, null, null));
        }

        @Test
        @DisplayName("sin tipo de examen responde 400 y no actualiza nada")
        void sin_tipo_de_examen_responde_400() throws Exception {
            mockMvc.perform(put("/laboratory-tests/" + LaboratoryTestMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-16","quantity":3,"animalId":7}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("actualizar una muestra inexistente responde 404")
        void actualizar_una_muestra_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new LaboratoryTestNotFoundException(999L));

            mockMvc.perform(put("/laboratory-tests/999").contentType(MediaType.APPLICATION_JSON)
                    .content(ACTUALIZACION_VALIDA)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /laboratory-tests/{id}/status")
    class CambioDeEstado {

        @Test
        @DisplayName("responde 200 con la muestra en el nuevo estado")
        void responde_200() throws Exception {
            when(changeStatusUseCase.execute(any()))
                    .thenReturn(LaboratoryTestDto.from(LaboratoryTestMother.validada()));

            mockMvc.perform(patch("/laboratory-tests/" + LaboratoryTestMother.ID + "/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"COMPLETED"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("el empleado que firma y la empresa salen del contexto, no del cuerpo")
        void el_empleado_y_la_empresa_salen_del_contexto() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(changeStatusUseCase.execute(any()))
                    .thenReturn(LaboratoryTestDto.from(LaboratoryTestMother.validada()));

            mockMvc.perform(patch("/laboratory-tests/" + LaboratoryTestMother.ID + "/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"PENDING_VALIDATION"}
                            """));

            verify(changeStatusUseCase).execute(new ChangeLaboratoryTestStatusCommand(
                    LaboratoryTestMother.ID, "PENDING_VALIDATION", EMPLOYEE_ID, COMPANY_ID));
        }

        /**
         * BE-31: el {@code OrNull} es lo que deja vivo el camino SYSTEM. Con
         * {@code currentCompanyId()} un principal sin empresa reventaria aqui con
         * {@code AccessDeniedException} en vez de cargar por la via ancha.
         */
        @Test
        @DisplayName("un principal sin empresa manda companyId null")
        void un_principal_sin_empresa_manda_null() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(changeStatusUseCase.execute(any()))
                    .thenReturn(LaboratoryTestDto.from(LaboratoryTestMother.validada()));

            mockMvc.perform(patch("/laboratory-tests/" + LaboratoryTestMother.ID + "/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"COMPLETED"}
                            """)).andExpect(status().isOk());

            verify(changeStatusUseCase).execute(new ChangeLaboratoryTestStatusCommand(
                    LaboratoryTestMother.ID, "COMPLETED", EMPLOYEE_ID, null));
        }

        @Test
        @DisplayName("sin estado responde 400 y no cambia nada")
        void sin_estado_responde_400() throws Exception {
            mockMvc.perform(patch("/laboratory-tests/" + LaboratoryTestMother.ID + "/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":""}
                            """)).andExpect(status().isBadRequest());

            verify(changeStatusUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE /laboratory-tests/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo y delega con la empresa del contexto")
        void responde_204() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);

            mockMvc.perform(delete("/laboratory-tests/" + LaboratoryTestMother.ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(LaboratoryTestMother.ID, COMPANY_ID);
        }

        @Test
        @DisplayName("borrar una muestra inexistente responde 404")
        void borrar_una_muestra_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            org.mockito.Mockito.doThrow(new LaboratoryTestNotFoundException(999L))
                    .when(deleteUseCase).execute(999L, COMPANY_ID);

            mockMvc.perform(delete("/laboratory-tests/999")).andExpect(status().isNotFound());
        }
    }
}
