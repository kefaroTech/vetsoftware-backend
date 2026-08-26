package com.vetsoftware.app.laboratorytesttype.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.CreateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.DeleteLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.FindLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListAvailableLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.UpdateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeHasActiveChildrenException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNameAlreadyExistsException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
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
 * Rodaja HTTP del controller de tipos de examen de laboratorio: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * La comprobacion que mas vale de todas es que el {@code companyId} del command
 * sale del contexto de autorizacion y no del cuerpo: el request no tiene donde
 * declararlo, y si algun dia lo tuviera este test lo delataria.
 */
@WebMvcTest(LaboratoryTestTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("LaboratoryTestTypeController — contrato HTTP")
class LaboratoryTestTypeControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Hemograma","description":"Hemograma completo","general":false}
            """;

    private static final String NOMBRE_DE_101_CARACTERES = "x".repeat(101);
    private static final String DESCRIPCION_DE_501_CARACTERES = "x".repeat(501);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateLaboratoryTestTypeUseCase createUseCase;
    @MockitoBean
    private UpdateLaboratoryTestTypeUseCase updateUseCase;
    @MockitoBean
    private FindLaboratoryTestTypeUseCase findUseCase;
    @MockitoBean
    private ListLaboratoryTestTypesUseCase listUseCase;
    @MockitoBean
    private ListAvailableLaboratoryTestTypesUseCase listAvailableUseCase;
    @MockitoBean
    private DeleteLaboratoryTestTypeUseCase deleteUseCase;

    /**
     * El doble de {@code Authz} lo aporta {@link WebMvcSliceConfig}; se inyecta
     * aqui para afirmar que el borrado propaga la empresa del contexto, que es la
     * mitad del arreglo de aislamiento (la otra mitad vive en el service).
     */
    @Autowired
    private com.vetsoftware.app.auth.infrastructure.security.Authz authz;

    /**
     * {@code WebMvcSliceConfig} stubea {@code currentCompanyId()} pero NO
     * {@code currentCompanyIdOrNull()} —lo comparten 92 rodajas y varias dependen
     * de que devuelva {@code null}—, asi que la empresa del contexto para las
     * ESCRITURAS se pone aqui. Desde el arreglo de #565 el {@code create} y el
     * {@code update} leen esa segunda, igual que ya hacia el {@code delete}: sin
     * este stub el command llegaria con {@code companyId} nulo y el tipo caeria en
     * el catalogo de plataforma en vez de en la veterinaria.
     *
     * <p>
     * El doble es un {@code mock()} de la configuracion compartida, no un
     * {@code @MockitoBean}: nadie lo resetea entre casos, y re-stubearlo antes de
     * cada uno es lo que deja el caso del principal de plataforma —que lo pone en
     * {@code null}— sin contaminar a los siguientes.
     */
    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static LaboratoryTestTypeDto hemograma() {
        return new LaboratoryTestTypeDto(70L, "Hemograma", "Hemograma completo",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"), false,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static LaboratoryTestTypeDto perfilRenalGeneral() {
        return new LaboratoryTestTypeDto(71L, "Perfil renal", "Perfil renal basico", null, true,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static CreateLaboratoryTestTypeCommand comandoDeCreacionEsperado() {
        return new CreateLaboratoryTestTypeCommand("Hemograma", "Hemograma completo", COMPANY_ID,
                false);
    }

    @Nested
    @DisplayName("POST /laboratory-test-types")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(hemograma());

            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.name").value("Hemograma"))
                    .andExpect(jsonPath("$.description").value("Hemograma completo"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.general").value(false))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("un tipo general responde con company nula")
        void un_tipo_general_responde_con_company_nula() throws Exception {
            when(createUseCase.execute(any())).thenReturn(perfilRenalGeneral());

            mockMvc.perform(
                    post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Perfil renal","description":"Perfil renal basico","general":true}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.company").doesNotExist())
                    .andExpect(jsonPath("$.general").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(hemograma());

            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("un companyId colado en el cuerpo no suplanta a la empresa del contexto")
        void un_company_id_colado_en_el_cuerpo_no_suplanta_al_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(hemograma());

            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Hemograma","description":"Hemograma completo","general":false,
                             "companyId":777}
                            """));

            // El request no declara companyId: lo pone Authz. Si alguien lo anadiera al
            // record, este command llevaria 777 y el tipo caeria en otra veterinaria.
            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"","description":"Hemograma completo","general":false}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 100 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + NOMBRE_DE_101_CARACTERES
                            + "\",\"description\":\"d\",\"general\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("descripcion de mas de 500 caracteres responde 400")
        void descripcion_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Hemograma\",\"description\":\""
                            + DESCRIPCION_DE_501_CARACTERES + "\",\"general\":false}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no como 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: 9"));

            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("un nombre ya usado en el ambito responde 409, no 500")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new LaboratoryTestTypeNameAlreadyExistsException("Hemograma"));

            mockMvc.perform(post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("un principal de plataforma crea un tipo global: el command va sin empresa y con general")
        void un_principal_de_plataforma_crea_un_tipo_global() throws Exception {
            // El arreglo de #565. Con currentCompanyId() ningun actor podia crear un
            // tipo global: al principal de plataforma le saltaba un AccessDeniedException
            // sin contexto y al empleado le colaban SU empresa, que choca contra el XOR
            // del dominio en cuanto general = true.
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(createUseCase.execute(any())).thenReturn(perfilRenalGeneral());

            mockMvc.perform(
                    post("/laboratory-test-types").contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Perfil renal","description":"Perfil renal basico","general":true}
                                    """))
                    .andExpect(status().isCreated());

            verify(createUseCase).execute(new CreateLaboratoryTestTypeCommand("Perfil renal",
                    "Perfil renal basico", null, true));
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /laboratory-test-types lista todos, sin filtrar por empresa")
        void get_lista_todos_sin_filtrar() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(hemograma(), perfilRenalGeneral()));

            mockMvc.perform(get("/laboratory-test-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70))
                    .andExpect(jsonPath("$[1].id").value(71));
        }

        @Test
        @DisplayName("GET /laboratory-test-types/available lista los de la empresa del contexto")
        void get_available_lista_los_de_la_empresa_del_contexto() throws Exception {
            when(listAvailableUseCase.listAvailable(COMPANY_ID))
                    .thenReturn(List.of(hemograma(), perfilRenalGeneral()));

            mockMvc.perform(get("/laboratory-test-types/available")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70))
                    .andExpect(jsonPath("$[1].general").value(true));
        }

        @Test
        @DisplayName("GET /laboratory-test-types/{id} devuelve el recurso, acotado a la empresa")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(70L, COMPANY_ID)).thenReturn(hemograma());

            mockMvc.perform(get("/laboratory-test-types/70")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.description").value("Hemograma completo"));
        }

        @Test
        @DisplayName("GET /laboratory-test-types/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new LaboratoryTestTypeNotFoundException(99L));

            mockMvc.perform(get("/laboratory-test-types/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un tipo existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /laboratory-test-types/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(hemograma());

            mockMvc.perform(put("/laboratory-test-types/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y la empresa del contexto")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(hemograma());

            mockMvc.perform(put("/laboratory-test-types/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateLaboratoryTestTypeCommand(70L, "Hemograma",
                    "Hemograma completo", COMPANY_ID, false));
        }

        @Test
        @DisplayName("PUT con nombre vacio responde 400 y no llega al caso de uso")
        void put_con_nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(put("/laboratory-test-types/70").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"","description":"d","general":false}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un tipo de otra empresa responde 404")
        void put_de_otro_tipo_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new LaboratoryTestTypeNotFoundException(70L));

            mockMvc.perform(put("/laboratory-test-types/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT con un nombre ya usado en el ambito responde 409, no 500")
        void put_con_nombre_repetido_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new LaboratoryTestTypeNameAlreadyExistsException("Hemograma"));

            mockMvc.perform(put("/laboratory-test-types/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PUT de un principal de plataforma edita el catalogo global: el command va sin empresa")
        void put_de_un_principal_de_plataforma_va_sin_empresa() throws Exception {
            // La otra mitad de #565: el update tambien pasa a currentCompanyIdOrNull().
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(updateUseCase.execute(any())).thenReturn(perfilRenalGeneral());

            mockMvc.perform(put("/laboratory-test-types/71").contentType(MediaType.APPLICATION_JSON)
                    .content(
                            """
                                    {"name":"Perfil renal","description":"Perfil renal basico","general":true}
                                    """));

            verify(updateUseCase).execute(new UpdateLaboratoryTestTypeCommand(71L, "Perfil renal",
                    "Perfil renal basico", null, true));
        }

        @Test
        @DisplayName("DELETE /laboratory-test-types/{id} responde 204 y propaga la empresa del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/laboratory-test-types/70")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(70L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un tipo inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new LaboratoryTestTypeNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/laboratory-test-types/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de un tipo con examenes activos responde 409")
        void delete_con_examenes_activos_responde_409() throws Exception {
            doThrow(new LaboratoryTestTypeHasActiveChildrenException(70L, "laboratoryTest"))
                    .when(deleteUseCase).execute(70L, COMPANY_ID);

            mockMvc.perform(delete("/laboratory-test-types/70")).andExpect(status().isConflict());
        }
    }
}
