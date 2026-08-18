package com.vetsoftware.app.surgery.infrastructure.web;

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
import com.vetsoftware.app.surgery.application.command.ChangeSurgeryStatusCommand;
import com.vetsoftware.app.surgery.application.command.CreateSurgeryCommand;
import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.ChangeSurgeryStatusUseCase;
import com.vetsoftware.app.surgery.application.port.in.CreateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.DeleteSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.FindSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesByAnimalUseCase;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesUseCase;
import com.vetsoftware.app.surgery.application.port.in.ReactivateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.UpdateSurgeryUseCase;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles — aqui no se prueba
 * el caso de uso, se prueba el contrato que ve el front.
 */
@WebMvcTest(SurgeryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SurgeryController — contrato HTTP")
class SurgeryControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_CREACION_VALIDO = """
            {"date":"2026-03-10","surgeryTypeId":5,"description":"Ovariohisterectomia electiva",
             "medicament":"Ketamina 10mg","observations":"Recuperacion normal","animalId":100,
             "consultationId":200}
            """;

    private static final String CUERPO_ACTUALIZACION_VALIDO = """
            {"date":"2026-03-15","surgeryTypeId":6,"description":"Castracion electiva",
             "medicament":"Anestesia local","observations":"Observaciones nuevas","animalId":101,
             "consultationId":201}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateSurgeryUseCase createUseCase;
    @MockitoBean
    private UpdateSurgeryUseCase updateUseCase;
    @MockitoBean
    private ChangeSurgeryStatusUseCase changeStatusUseCase;
    @MockitoBean
    private FindSurgeryUseCase findUseCase;
    @MockitoBean
    private ListSurgeriesUseCase listUseCase;
    @MockitoBean
    private ListSurgeriesByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteSurgeryUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSurgeryUseCase reactivateUseCase;

    @BeforeEach
    void companyIdOrNullDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static SurgeryDto dto() {
        return SurgeryDto.from(SurgeryMother.cirugiaValida());
    }

    @Nested
    @DisplayName("POST /surgeries")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la cirugia creada")
        void responde_201_con_la_cirugia_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/surgeries").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SurgeryMother.SURGERY_ID))
                    .andExpect(jsonPath("$.description").value("Ovariohisterectomia electiva"))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.status").value("PROGRAMADA"));
        }

        @Test
        @DisplayName("traduce el request al command con el companyId del contexto, no del body")
        void traduce_el_request_al_command_con_el_company_id_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/surgeries").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO));

            verify(createUseCase).execute(new CreateSurgeryCommand(SurgeryMother.FECHA, 5L,
                    "Ovariohisterectomia electiva", "Ketamina 10mg", "Recuperacion normal", null,
                    100L, 200L, COMPANY_ID));
        }

        @Test
        @DisplayName("con descripcion vacia responde 400 y no llega al caso de uso")
        void con_descripcion_vacia_responde_400() throws Exception {
            mockMvc.perform(post("/surgeries").contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-03-10","surgeryTypeId":5,"description":"","animalId":100}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin animalId responde 400 y no llega al caso de uso")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(post("/surgeries").contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-03-10","surgeryTypeId":5,
                     "description":"Ovariohisterectomia electiva"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /surgeries")
    class Listado {

        @Test
        @DisplayName("devuelve el listado global tal cual lo entrega el caso de uso")
        void devuelve_el_listado_global() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/surgeries")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(SurgeryMother.SURGERY_ID));
        }
    }

    @Nested
    @DisplayName("GET /surgeries/by-animal/{animalId}")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve la pagina con el companyId del contexto y los parametros de la ruta")
        void devuelve_la_pagina_con_el_company_id_del_contexto() throws Exception {
            when(listByAnimalUseCase.listByAnimal(SurgeryMother.ANIMAL_ID, COMPANY_ID, null, 0, 20))
                    .thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/surgeries/by-animal/{animalId}", SurgeryMother.ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(SurgeryMother.SURGERY_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /surgeries/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve la cirugia encontrada")
        void devuelve_la_cirugia_encontrada() throws Exception {
            when(findUseCase.findById(SurgeryMother.SURGERY_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(get("/surgeries/{id}", SurgeryMother.SURGERY_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Ovariohisterectomia electiva"));
        }

        @Test
        @DisplayName("una cirugia inexistente responde 404, no 500")
        void una_cirugia_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new SurgeryNotFoundException(99L));

            mockMvc.perform(get("/surgeries/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /surgeries/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con la cirugia actualizada")
        void responde_200_con_la_cirugia_actualizada() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/surgeries/{id}", SurgeryMother.SURGERY_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SurgeryMother.SURGERY_ID));
        }

        @Test
        @DisplayName("traduce el request y el id de la ruta al command, con el companyId del contexto")
        void traduce_el_request_y_el_id_de_la_ruta_al_command() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/surgeries/{id}", SurgeryMother.SURGERY_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO));

            verify(updateUseCase).execute(new UpdateSurgeryCommand(SurgeryMother.SURGERY_ID,
                    SurgeryMother.FECHA.plusDays(5), 6L, "Castracion electiva", "Anestesia local",
                    "Observaciones nuevas", null, 101L, 201L, COMPANY_ID));
        }

        @Test
        @DisplayName("con descripcion vacia responde 400 y no llega al caso de uso")
        void con_descripcion_vacia_responde_400() throws Exception {
            mockMvc.perform(put("/surgeries/{id}", SurgeryMother.SURGERY_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-15","surgeryTypeId":6,"description":"","animalId":101}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("PATCH /surgeries/{id}/status")
    class CambioDeEstado {

        @Test
        @DisplayName("responde 200 con el nuevo estado y usa currentCompanyIdOrNull")
        void responde_200_con_el_nuevo_estado() throws Exception {
            when(changeStatusUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(patch("/surgeries/{id}/status", SurgeryMother.SURGERY_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"COMPLETADO"}
                            """)).andExpect(status().isOk());

            verify(changeStatusUseCase).execute(new ChangeSurgeryStatusCommand(
                    SurgeryMother.SURGERY_ID, "COMPLETADO", COMPANY_ID));
        }

        @Test
        @DisplayName("con status vacio responde 400 y no llega al caso de uso")
        void con_status_vacio_responde_400() throws Exception {
            mockMvc.perform(patch("/surgeries/{id}/status", SurgeryMother.SURGERY_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":""}
                            """)).andExpect(status().isBadRequest());

            verify(changeStatusUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE /surgeries/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/surgeries/{id}", SurgeryMother.SURGERY_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("delega con la empresa del contexto, nunca con la que pida el cliente")
        void delega_con_la_empresa_del_contexto() throws Exception {
            mockMvc.perform(delete("/surgeries/{id}", SurgeryMother.SURGERY_ID));

            verify(deleteUseCase).execute(SurgeryMother.SURGERY_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /surgeries/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con la cirugia habilitada, acotada por la empresa del contexto")
        void responde_200_con_la_cirugia_habilitada() throws Exception {
            when(reactivateUseCase.execute(SurgeryMother.SURGERY_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(patch("/surgeries/{id}/enable", SurgeryMother.SURGERY_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(SurgeryMother.SURGERY_ID, COMPANY_ID);
        }
    }
}
