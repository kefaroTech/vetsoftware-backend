package com.vetsoftware.app.servicecategory.infrastructure.web;

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

import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.CompanySummaryDto;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.CreateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.DeleteServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.ListServiceCategoriesUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.UpdateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
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
 * Rodaja HTTP del controller de categorias de servicio: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * La comprobacion que mas vale de todas es que el {@code companyId} del command
 * sale del contexto de autorizacion y no del cuerpo: la peticion no tiene donde
 * declararlo, y si algun dia lo tuviera este test lo delataria.
 */
@WebMvcTest(ServiceCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ServiceCategoryController — contrato HTTP")
class ServiceCategoryControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final String CUERPO_VALIDO = """
            {"name":"Consultas","description":"Categoria de consultas"}
            """;

    private static final String CUERPO_VALIDO_UPDATE = """
            {"name":"Consultas","description":"Categoria de consultas","version":3}
            """;

    private static final String NOMBRE_DE_101_CARACTERES = "x".repeat(101);
    private static final String DESCRIPCION_DE_501_CARACTERES = "x".repeat(501);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateServiceCategoryUseCase createUseCase;
    @MockitoBean
    private UpdateServiceCategoryUseCase updateUseCase;
    @MockitoBean
    private ListServiceCategoriesUseCase listUseCase;
    @MockitoBean
    private DeleteServiceCategoryUseCase deleteUseCase;

    private static ServiceCategoryDto consultas() {
        return new ServiceCategoryDto(70L, "Consultas", "Categoria de consultas",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, null, 3L, true);
    }

    private static CreateServiceCategoryCommand comandoDeCreacionEsperado() {
        return new CreateServiceCategoryCommand("Consultas", "Categoria de consultas", COMPANY_ID);
    }

    @Nested
    @DisplayName("POST /service-categories")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(consultas());

            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.name").value("Consultas"))
                    .andExpect(jsonPath("$.description").value("Categoria de consultas"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.version").value(3))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(consultas());

            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("un companyId colado en el cuerpo no suplanta a la empresa del contexto")
        void un_company_id_colado_en_el_cuerpo_no_suplanta_al_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(consultas());

            mockMvc.perform(
                    post("/service-categories").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Consultas","description":"Categoria de consultas",
                             "companyId":777}
                            """));

            // El request no declara companyId: lo pone Authz. Si alguien lo anadiera al
            // record, este command llevaria 777 y la categoria caeria en otra veterinaria.
            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    post("/service-categories").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"","description":"Categoria de consultas"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 100 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + NOMBRE_DE_101_CARACTERES
                            + "\",\"description\":\"Categoria de consultas\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("descripcion vacia responde 400")
        void descripcion_vacia_responde_400() throws Exception {
            mockMvc.perform(
                    post("/service-categories").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Consultas","description":""}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("descripcion de mas de 500 caracteres responde 400")
        void descripcion_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Consultas\",\"description\":\""
                            + DESCRIPCION_DE_501_CARACTERES + "\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre repetido en la empresa responde 409")
        void nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new ServiceCategoryNameAlreadyExistsException("Consultas"));

            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una empresa inexistente sale como 400, no como 500")
        void empresa_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Company not found: 9"));

            mockMvc.perform(post("/service-categories").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /service-categories lista las de la empresa del contexto")
        void get_lista_las_de_la_empresa_del_contexto() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(consultas()));

            mockMvc.perform(get("/service-categories")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70))
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("escrituras sobre una categoria existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /service-categories/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(consultas());

            mockMvc.perform(put("/service-categories/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y el empleado")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(consultas());

            mockMvc.perform(put("/service-categories/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE));

            verify(updateUseCase).execute(new UpdateServiceCategoryCommand(70L, "Consultas",
                    "Categoria de consultas", COMPANY_ID, EMPLOYEE_ID, 3L));
        }

        @Test
        @DisplayName("PUT sin version responde 400 y no llega al caso de uso")
        void put_sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/service-categories/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre una categoria de otra empresa responde 404")
        void put_de_otra_empresa_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new ServiceCategoryNotFoundException(70L));

            mockMvc.perform(put("/service-categories/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT con un nombre ya usado en la empresa responde 409")
        void put_con_nombre_repetido_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new ServiceCategoryNameAlreadyExistsException("Consultas"));

            mockMvc.perform(put("/service-categories/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /service-categories/{id} responde 204 sin cuerpo y pasa la empresa")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/service-categories/70")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(70L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una categoria inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new ServiceCategoryNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/service-categories/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de una categoria con servicios activos responde 409")
        void delete_con_servicios_activos_responde_409() throws Exception {
            doThrow(new ServiceCategoryHasActiveChildrenException(70L, "service"))
                    .when(deleteUseCase).execute(70L, COMPANY_ID);

            mockMvc.perform(delete("/service-categories/70")).andExpect(status().isConflict());
        }
    }
}
