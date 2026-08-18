package com.vetsoftware.app.owner.infrastructure.web;

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

import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.CreateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.DeleteOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.FindOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.ListOwnersUseCase;
import com.vetsoftware.app.owner.application.port.in.ReactivateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.SearchOwnersUseCase;
import com.vetsoftware.app.owner.application.port.in.UpdateOwnerUseCase;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja HTTP del controller de propietarios: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 * Owner SI tiene {@code companyId}: lo inyecta el controller desde
 * {@code authz.currentCompanyId()}, que {@link WebMvcSliceConfig} deja fijo en
 * 9L — el mismo {@link OwnerMother#COMPANY_ID}.
 */
@WebMvcTest(OwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("OwnerController — contrato HTTP")
class OwnerControllerTest {

    private static final String CUERPO_VALIDO = """
            {"name":"Ana Ruiz","email":"ana@vet.com","document":"1020304050",
             "documentType":"CEDULA_CIUDADANIA","personType":"NATURAL",
             "address":"Calle 1 # 2-3","phone":"3001112233","cityId":5,
             "withholdingAgent":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOwnerUseCase createUseCase;
    @MockitoBean
    private UpdateOwnerUseCase updateUseCase;
    @MockitoBean
    private FindOwnerUseCase findUseCase;
    @MockitoBean
    private ListOwnersUseCase listUseCase;
    @MockitoBean
    private SearchOwnersUseCase searchUseCase;
    @MockitoBean
    private DeleteOwnerUseCase deleteUseCase;
    @MockitoBean
    private ReactivateOwnerUseCase reactivateUseCase;

    private static OwnerDto anaRuiz() {
        return OwnerDto.from(OwnerMother.personaNatural());
    }

    private static CreateOwnerCommand comandoDeCreacionEsperado() {
        return new CreateOwnerCommand("Ana Ruiz", "ana@vet.com", "1020304050",
                OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                "Calle 1 # 2-3", "3001112233", 5L, WebMvcSliceConfig.COMPANY_ID, false, null, null);
    }

    @Nested
    @DisplayName("POST /owners")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(anaRuiz());

            mockMvc.perform(
                    post("/owners").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(OwnerMother.OWNER_ID))
                    .andExpect(jsonPath("$.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.city.name").value("Bogota"))
                    .andExpect(jsonPath("$.company.name").value("Clinica Norte"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command con el companyId del contexto")
        void traduce_el_request_al_command_con_el_company_id_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(anaRuiz());

            mockMvc.perform(
                    post("/owners").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","document":"1020304050","documentType":"CEDULA_CIUDADANIA",
                     "personType":"NATURAL","cityId":5,"withholdingAgent":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("email con formato invalido responde 400")
        void email_con_formato_invalido_responde_400() throws Exception {
            mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Ana Ruiz","email":"no-es-un-correo","document":"1020304050",
                     "documentType":"CEDULA_CIUDADANIA","personType":"NATURAL","cityId":5,
                     "withholdingAgent":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("documentType nulo responde 400")
        void document_type_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Ana Ruiz","document":"1020304050","personType":"NATURAL",
                     "cityId":5,"withholdingAgent":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("cityId nulo responde 400")
        void city_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Ana Ruiz","document":"1020304050",
                     "documentType":"CEDULA_CIUDADANIA","personType":"NATURAL",
                     "withholdingAgent":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una ciudad inexistente sale como 400, no como 500")
        void ciudad_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("City not found: 5"));

            mockMvc.perform(
                    post("/owners").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /owners lista la pagina de la empresa del contexto")
        void get_lista_la_pagina_de_la_empresa_del_contexto() throws Exception {
            when(listUseCase.listAll(WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(anaRuiz()), 0, 20, 1L, 1));

            mockMvc.perform(get("/owners")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(OwnerMother.OWNER_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /owners/search filtra por termino dentro de la empresa")
        void get_search_filtra_por_termino() throws Exception {
            when(searchUseCase.search(WebMvcSliceConfig.COMPANY_ID, "ana", 0, 20))
                    .thenReturn(new PageResult<>(List.of(anaRuiz()), 0, 20, 1L, 1));

            mockMvc.perform(get("/owners/search").param("q", "ana")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Ana Ruiz"));
        }

        @Test
        @DisplayName("GET /owners/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(OwnerMother.OWNER_ID, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(anaRuiz());

            mockMvc.perform(get("/owners/" + OwnerMother.OWNER_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(OwnerMother.OWNER_ID))
                    .andExpect(jsonPath("$.document").value("1020304050"));
        }

        @Test
        @DisplayName("GET /owners/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new OwnerNotFoundException(99L));

            mockMvc.perform(get("/owners/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un owner existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /owners/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(anaRuiz());

            mockMvc.perform(put("/owners/" + OwnerMother.OWNER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(OwnerMother.OWNER_ID));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta y el companyId del contexto")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(anaRuiz());

            mockMvc.perform(put("/owners/" + OwnerMother.OWNER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateOwnerCommand(OwnerMother.OWNER_ID, "Ana Ruiz",
                    "ana@vet.com", "1020304050", OwnerDocumentType.CEDULA_CIUDADANIA,
                    PersonType.NATURAL, null, null, "Calle 1 # 2-3", "3001112233", 5L,
                    WebMvcSliceConfig.COMPANY_ID, false, null, null));
        }

        @Test
        @DisplayName("PUT sobre un owner inexistente responde 404")
        void put_de_owner_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new OwnerNotFoundException(OwnerMother.OWNER_ID));

            mockMvc.perform(put("/owners/" + OwnerMother.OWNER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /owners/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/owners/" + OwnerMother.OWNER_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(OwnerMother.OWNER_ID, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de un owner inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new OwnerNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/owners/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de un owner con animales activos responde 409")
        void delete_con_animales_activos_responde_409() throws Exception {
            doThrow(new OwnerHasActiveChildrenException(OwnerMother.OWNER_ID, "animal"))
                    .when(deleteUseCase)
                    .execute(OwnerMother.OWNER_ID, WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/owners/" + OwnerMother.OWNER_ID))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /owners/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(OwnerMother.OWNER_ID, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(anaRuiz());

            mockMvc.perform(patch("/owners/" + OwnerMother.OWNER_ID + "/enable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(OwnerMother.OWNER_ID))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de un owner inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new OwnerNotFoundException(99L));

            mockMvc.perform(patch("/owners/99/enable")).andExpect(status().isNotFound());
        }
    }
}
