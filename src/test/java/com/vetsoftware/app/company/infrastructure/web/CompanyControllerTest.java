package com.vetsoftware.app.company.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.dto.MembershipSummaryDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.SearchCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.domain.CompanyHasActiveChildrenException;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja HTTP del controller de empresas: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON (incluidos los companion summary
 * de ciudad y membresia). Lo que hay debajo son dobles; la autorizacion de
 * {@code @PreAuthorize} (SYSTEM o la authority puntual) vive en los puertos de
 * entrada y se prueba aparte, no en esta rodaja con seguridad deshabilitada.
 */
@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CompanyController — contrato HTTP")
class CompanyControllerTest {

    private static final String CUERPO_VALIDO = """
            {"name":"Clinica Norte","identifier":"NIT-900","address":"Calle 123 #45-67","contactNumber":"3001234567","cityId":11,"membershipId":21}
            """;

    @Autowired
    private MockMvc mockMvc;

    /**
     * El doble que {@link WebMvcSliceConfig} publica: aqui se re-stubea para
     * distinguir los dos actores de {@code GET /companies} —el principal de
     * plataforma, que no tiene empresa, y el empleado, que si—.
     */
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateCompanyUseCase createUseCase;
    @MockitoBean
    private UpdateCompanyUseCase updateUseCase;
    @MockitoBean
    private FindCompanyUseCase findUseCase;
    @MockitoBean
    private ListCompaniesUseCase listUseCase;
    @MockitoBean
    private SearchCompaniesUseCase searchUseCase;
    @MockitoBean
    private DeleteCompanyUseCase deleteUseCase;

    private static CompanyDto clinicaNorte() {
        return new CompanyDto(9L, "Clinica Norte", "NIT-900", "Calle 123 #45-67", "3001234567",
                new CitySummaryDto(11L, "Bogota"),
                new MembershipSummaryDto(21L, "Premium", "ACTIVE"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    /**
     * Pagina de una sola empresa, tal como la devuelve un caso de uso ya paginado.
     * Los metadatos son los de la consulta, no los del contenido: el controller
     * solo los arrastra a la envoltura HTTP.
     */
    private static PageResult<CompanyDto> unaPagina() {
        return new PageResult<>(List.of(clinicaNorte()), 0, 20, 1L, 1);
    }

    private static CreateCompanyCommand comandoDeCreacionEsperado() {
        return new CreateCompanyCommand("Clinica Norte", "NIT-900", "Calle 123 #45-67",
                "3001234567", 11L, 21L);
    }

    @Nested
    @DisplayName("POST /companies")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado, incluidos los companion de ciudad y membresia")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.name").value("Clinica Norte"))
                    .andExpect(jsonPath("$.city.id").value(11))
                    .andExpect(jsonPath("$.city.name").value("Bogota"))
                    .andExpect(jsonPath("$.membership.id").value(21))
                    .andExpect(jsonPath("$.membership.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre en blanco responde 400 y no llega al caso de uso")
        void nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","identifier":"NIT-900","cityId":11,"membershipId":21}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("identificador de mas de 50 caracteres responde 400")
        void identificador_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Clinica Norte\",\"identifier\":\"" + "x".repeat(51)
                            + "\",\"cityId\":11,\"membershipId\":21}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("cityId nulo responde 400")
        void city_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Clinica Norte","identifier":"NIT-900","membershipId":21}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("membershipId nulo responde 400")
        void membership_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Clinica Norte","identifier":"NIT-900","cityId":11}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una ciudad inexistente sale como 400, no como 500")
        void ciudad_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("City not found: 11"));

            mockMvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        /**
         * VUE-06: la respuesta es una envoltura de pagina, no un array desnudo. El
         * cambio de forma es parte del contrato que ven los dos frontends, asi que se
         * afirma el sobre completo —{@code content} y los cuatro metadatos—, no solo la
         * primera fila.
         */
        @Test
        @DisplayName("GET /companies devuelve la envoltura de pagina, no un array desnudo")
        void get_devuelve_la_envoltura_de_pagina() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null, 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(9))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }

        /**
         * El alcance no viaja en la peticion ni se puede falsear desde fuera: el
         * controller lo toma de {@code currentCompanyIdOrNull()}. Este test fija
         * justamente eso —que el {@code companyId} que llega al puerto es el del
         * principal—, que es lo que impedia que un empleado de una veterinaria listara
         * el registro de todas.
         */
        @Test
        @DisplayName("GET /companies acota al principal: pasa su companyId, no null")
        void get_acota_al_company_id_del_principal() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(9L);
            when(listUseCase.listAll(9L, 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(9))
                    .andExpect(jsonPath("$.content[1]").doesNotExist());

            verify(listUseCase, never()).listAll(isNull(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("GET /companies/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(9L)).thenReturn(clinicaNorte());

            mockMvc.perform(get("/companies/9")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.identifier").value("NIT-900"));
        }

        @Test
        @DisplayName("GET /companies/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new CompanyNotFoundException(99L));

            mockMvc.perform(get("/companies/99")).andExpect(status().isNotFound());
        }
    }

    /**
     * Los dos query params de VUE-06. Lo que se fija aqui es el reparto de
     * responsabilidades: el controller <b>traslada</b> lo que pide el cliente y el
     * servidor <b>decide</b> lo que devuelve. Normalizar el indice y topar el
     * tamaño es trabajo de {@code Pages}, aguas abajo, y se prueba contra la base
     * en {@code CompanyPersistenceIT}.
     */
    @Nested
    @DisplayName("GET /companies — la pagina la pide el cliente, la decide el servidor")
    class Paginacion {

        @Test
        @DisplayName("sin query params usa los defectos del contrato: page=0 y pageSize=20")
        void sin_query_params_usa_los_defectos_del_contrato() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null, 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies")).andExpect(status().isOk());

            // Si el defecto cambiara, el stub no encajaria y el 200 se caeria: el
            // contrato de los dos frontends es exactamente este par de valores.
            verify(listUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("page y pageSize llegan al caso de uso tal como los pidio el cliente")
        void page_y_page_size_llegan_tal_cual() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null, 2, 50))
                    .thenReturn(new PageResult<>(List.of(clinicaNorte()), 2, 50, 137L, 3));

            mockMvc.perform(get("/companies").param("page", "2").param("pageSize", "50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(50))
                    .andExpect(jsonPath("$.totalElements").value(137));
        }

        /**
         * El tope no lo fija el cliente. Se pide {@code pageSize=100000} y la respuesta
         * dice 200 ({@code Pages.MAX_SIZE}): sin ese tope, un query param convierte un
         * listado paginado en un {@code SELECT *} y deshace VUE-06 entero.
         */
        @Test
        @DisplayName("un pageSize desmedido sale acotado en la respuesta: el tope es del servidor")
        void un_page_size_desmedido_sale_acotado() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null, 0, 100_000))
                    .thenReturn(new PageResult<>(List.of(clinicaNorte()), 0, 200, 1L, 1));

            mockMvc.perform(get("/companies").param("pageSize", "100000"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(200));
        }

        /**
         * {@code ?page=-1} no revienta con un {@code IllegalArgumentException} desde
         * dentro de Spring Data: se normaliza a 0 y la respuesta lo declara.
         */
        @Test
        @DisplayName("un page negativo sale normalizado a 0 en la respuesta, no como error")
        void un_page_negativo_sale_normalizado_a_cero() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(listUseCase.listAll(null, -1, 20))
                    .thenReturn(new PageResult<>(List.of(clinicaNorte()), 0, 20, 1L, 1));

            mockMvc.perform(get("/companies").param("page", "-1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("un pageSize no numerico responde 400 y no llega al caso de uso")
        void un_page_size_no_numerico_responde_400() throws Exception {
            mockMvc.perform(get("/companies").param("pageSize", "muchas"))
                    .andExpect(status().isBadRequest());

            verify(listUseCase, never()).listAll(any(), anyInt(), anyInt());
        }
    }

    /**
     * {@code GET /companies/search}, que nace con el listado paginado: filtrar en
     * cliente dejo de valer cuando el cliente solo tiene la pagina que esta
     * mirando.
     */
    @Nested
    @DisplayName("GET /companies/search")
    class Busqueda {

        @Test
        @DisplayName("devuelve la misma envoltura de pagina que el listado")
        void devuelve_la_misma_envoltura_de_pagina() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(searchUseCase.search(null, "norte", 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies/search").param("q", "norte")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(9))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("q es obligatorio: sin el responde 400 y no llega al caso de uso")
        void q_es_obligatorio() throws Exception {
            mockMvc.perform(get("/companies/search")).andExpect(status().isBadRequest());

            verify(searchUseCase, never()).search(any(), anyString(), anyInt(), anyInt());
        }

        /**
         * Vacio no es lo mismo que ausente: es lo que manda el buscador cuando se borra
         * lo escrito, y el caso de uso lo trata como «devuelve el listado».
         */
        @Test
        @DisplayName("q vacio se acepta y viaja como cadena vacia, no como null")
        void q_vacio_viaja_como_cadena_vacia() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(searchUseCase.search(null, "", 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies/search").param("q", "")).andExpect(status().isOk());

            verify(searchUseCase).search(null, "", 0, 20);
        }

        /**
         * El alcance de la busqueda es el del listado, y sale del principal: si aqui
         * fuera mas ancho, buscar seria el camino corto para leer lo que el listado
         * niega.
         */
        @Test
        @DisplayName("acota al principal: el companyId viaja junto al termino, nunca null")
        void acota_al_principal_junto_al_termino() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(9L);
            when(searchUseCase.search(9L, "norte", 0, 20)).thenReturn(unaPagina());

            mockMvc.perform(get("/companies/search").param("q", "norte")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(9));

            verify(searchUseCase, never()).search(isNull(), anyString(), anyInt(), anyInt());
        }

        /**
         * El escenario que cierra el agujero: un empleado escribe el nombre de otra
         * veterinaria y recibe una pagina vacia con 200, no la ficha de esa veterinaria
         * y no un 403 que confirmaria que existe.
         */
        @Test
        @DisplayName("buscar el nombre de otra veterinaria devuelve pagina vacia con 200")
        void buscar_otra_veterinaria_devuelve_pagina_vacia() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(9L);
            when(searchUseCase.search(9L, "Clinica Sur", 0, 20))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/companies/search").param("q", "Clinica Sur"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        @DisplayName("page y pageSize tienen los mismos defectos y el mismo trato que el listado")
        void page_y_page_size_tienen_el_mismo_trato_que_el_listado() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(null);
            when(searchUseCase.search(null, "norte", 0, 100_000))
                    .thenReturn(new PageResult<>(List.of(clinicaNorte()), 0, 200, 1L, 1));

            mockMvc.perform(
                    get("/companies/search").param("q", "norte").param("pageSize", "100000"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(200));
        }
    }

    @Nested
    @DisplayName("escrituras sobre una empresa existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /companies/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(clinicaNorte());

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateCompanyCommand(9L, "Clinica Norte", "NIT-900",
                    "Calle 123 #45-67", "3001234567", 11L, 21L));
        }

        @Test
        @DisplayName("PUT sobre una empresa inexistente responde 404")
        void put_de_empresa_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new CompanyNotFoundException(9L));

            mockMvc.perform(put("/companies/9").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /companies/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/companies/9")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(9L);
        }

        @Test
        @DisplayName("DELETE de una empresa inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new CompanyNotFoundException(99L)).when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/companies/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de una empresa con hijos activos responde 409")
        void delete_con_hijos_activos_responde_409() throws Exception {
            doThrow(new CompanyHasActiveChildrenException(9L, "animal")).when(deleteUseCase)
                    .execute(9L);

            mockMvc.perform(delete("/companies/9")).andExpect(status().isConflict());
        }
    }
}
