package com.vetsoftware.app.diagnosticimaging.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
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
import com.vetsoftware.app.diagnosticimaging.application.command.CreateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.CreateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.DeleteDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.FindDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsByAnimalUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.UpdateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
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

@WebMvcTest(DiagnosticImagingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DiagnosticImagingController — contrato HTTP")
class DiagnosticImagingControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_VALIDO = """
            {"date":"2026-01-10","diagnosticImagingTypeId":703,"clinicalSigns":"Cojera pata trasera",
             "studyType":"Radiografia de cadera","diagnosis":"Displasia leve",
             "observations":"Control en 30 dias","animalId":701,"consultationId":702}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateDiagnosticImagingUseCase createUseCase;
    @MockitoBean
    private UpdateDiagnosticImagingUseCase updateUseCase;
    @MockitoBean
    private FindDiagnosticImagingUseCase findUseCase;
    @MockitoBean
    private ListDiagnosticImagingsUseCase listUseCase;
    @MockitoBean
    private ListDiagnosticImagingsByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteDiagnosticImagingUseCase deleteUseCase;

    @BeforeEach
    void companyIdOrNullDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static DiagnosticImagingDto dto() {
        return DiagnosticImagingDto.from(DiagnosticImagingMother.persistida());
    }

    @Nested
    @DisplayName("POST /diagnostic-imagings")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la imagen creada")
        void responde_201_con_la_imagen_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(DiagnosticImagingMother.IMAGING_ID))
                    .andExpect(jsonPath("$.diagnosis").value("Displasia leve"))
                    .andExpect(jsonPath("$.status").value("PENDIENTE"))
                    .andExpect(jsonPath("$.diagnosticImagingType.name").value("Radiografia"))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.consultation.id")
                            .value(DiagnosticImagingMother.CONSULTATION_ID))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria crear una imagen en otra empresa.
            verify(createUseCase).execute(new CreateDiagnosticImagingCommand(
                    DiagnosticImagingMother.FECHA, DiagnosticImagingMother.TYPE_ID,
                    "Cojera pata trasera", "Radiografia de cadera", "Displasia leve",
                    "Control en 30 dias", DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.CONSULTATION_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("sin fecha responde 400 y no crea nada")
        void sin_fecha_responde_400() throws Exception {
            mockMvc.perform(
                    post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"diagnosticImagingTypeId":703,"clinicalSigns":"Cojera pata trasera",
                             "studyType":"Radiografia de cadera","diagnosis":"Displasia leve",
                             "animalId":701}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("signos clinicos en blanco responden 400")
        void signos_clinicos_en_blanco_responde_400() throws Exception {
            mockMvc.perform(
                    post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-01-10","diagnosticImagingTypeId":703,"clinicalSigns":"",
                             "studyType":"Radiografia de cadera","diagnosis":"Displasia leve",
                             "animalId":701}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin tipo de imagen diagnostica responde 400")
        void sin_tipo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-01-10","clinicalSigns":"Cojera pata trasera",
                             "studyType":"Radiografia de cadera","diagnosis":"Displasia leve",
                             "animalId":701}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin animalId responde 400")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/diagnostic-imagings").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-01-10","diagnosticImagingTypeId":703,
                             "clinicalSigns":"Cojera pata trasera",
                             "studyType":"Radiografia de cadera","diagnosis":"Displasia leve"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /diagnostic-imagings lista todas las imagenes")
        void get_lista_todas_las_imagenes() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/diagnostic-imagings")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DiagnosticImagingMother.IMAGING_ID));
        }

        @Test
        @DisplayName("GET /diagnostic-imagings/by-animal/{animalId} responde con la pagina")
        void get_by_animal_responde_con_la_pagina() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(listByAnimalUseCase.listByAnimal(DiagnosticImagingMother.ANIMAL_ID, COMPANY_ID,
                    null, 0, 20)).thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/diagnostic-imagings/by-animal/{animalId}",
                    DiagnosticImagingMother.ANIMAL_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("GET /diagnostic-imagings/{id} devuelve el recurso en la empresa del contexto")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(DiagnosticImagingMother.IMAGING_ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(get("/diagnostic-imagings/{id}", DiagnosticImagingMother.IMAGING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DiagnosticImagingMother.IMAGING_ID));
        }

        @Test
        @DisplayName("GET /diagnostic-imagings/{id} de otra empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new DiagnosticImagingNotFoundException(99L));

            mockMvc.perform(get("/diagnostic-imagings/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una imagen existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /diagnostic-imagings/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/diagnostic-imagings/{id}", DiagnosticImagingMother.IMAGING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DiagnosticImagingMother.IMAGING_ID));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_con_el_id_de_la_ruta() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/diagnostic-imagings/{id}", DiagnosticImagingMother.IMAGING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateDiagnosticImagingCommand(
                    DiagnosticImagingMother.IMAGING_ID, DiagnosticImagingMother.FECHA,
                    DiagnosticImagingMother.TYPE_ID, "Cojera pata trasera", "Radiografia de cadera",
                    "Displasia leve", "Control en 30 dias", DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.CONSULTATION_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("PUT sobre una imagen inexistente responde 404")
        void put_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any())).thenThrow(
                    new DiagnosticImagingNotFoundException(DiagnosticImagingMother.IMAGING_ID));

            mockMvc.perform(put("/diagnostic-imagings/{id}", DiagnosticImagingMother.IMAGING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /diagnostic-imagings/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/diagnostic-imagings/{id}", DiagnosticImagingMother.IMAGING_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(DiagnosticImagingMother.IMAGING_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una imagen inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new DiagnosticImagingNotFoundException(99L))
                    .when(deleteUseCase).execute(99L, COMPANY_ID);

            mockMvc.perform(delete("/diagnostic-imagings/99")).andExpect(status().isNotFound());
        }
    }
}
