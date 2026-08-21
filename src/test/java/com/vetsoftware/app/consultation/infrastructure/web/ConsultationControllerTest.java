package com.vetsoftware.app.consultation.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.CreateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.FindConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
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
@WebMvcTest(ConsultationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ConsultationController — contrato HTTP")
class ConsultationControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_CREACION_VALIDO = """
            {"date":"2026-03-01","consultationTypeId":5,"anamnesis":"Anamnesis del paciente",
             "diagnosis":"Diagnostico","prognosis":"Pronostico reservado","nextControl":"2026-03-16",
             "animalId":100,"temperature":38.5,"heartRate":90}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateConsultationUseCase createUseCase;
    @MockitoBean
    private FindConsultationUseCase findUseCase;
    @MockitoBean
    private ListConsultationsUseCase listUseCase;

    @BeforeEach
    void companyIdOrNullDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static ConsultationDto dto() {
        return ConsultationDto.from(ConsultationMother.consultaValida());
    }

    @Nested
    @DisplayName("POST /consultations")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la consulta creada")
        void responde_201_con_la_consulta_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/consultations").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ConsultationMother.CONSULTATION_ID))
                    .andExpect(jsonPath("$.anamnesis").value("Anamnesis del paciente"))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"));
        }

        @Test
        @DisplayName("traduce el request al command con el companyId del contexto, no del body")
        void traduce_el_request_al_command_con_el_company_id_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/consultations").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO));

            verify(createUseCase).execute(new CreateConsultationCommand(ConsultationMother.FECHA,
                    5L, "Anamnesis del paciente", "Diagnostico", "Pronostico reservado",
                    ConsultationMother.FECHA.plusDays(15), 100L, COMPANY_ID, null, null,
                    new java.math.BigDecimal("38.5"), 90, null, null, null, null, null, null, null,
                    null));
        }

        @Test
        @DisplayName("con anamnesis vacia responde 400 y no llega al caso de uso")
        void con_anamnesis_vacia_responde_400() throws Exception {
            mockMvc.perform(
                    post("/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","consultationTypeId":5,"anamnesis":"",
                             "animalId":100}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin animalId responde 400 y no llega al caso de uso")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","consultationTypeId":5,
                             "anamnesis":"Anamnesis del paciente"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /consultations")
    class Listado {

        @Test
        @DisplayName("devuelve la pagina con el companyId del contexto")
        void devuelve_la_pagina_con_el_company_id_del_contexto() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/consultations")).andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.content[0].id").value(ConsultationMother.CONSULTATION_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /consultations/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve la consulta encontrada")
        void devuelve_la_consulta_encontrada() throws Exception {
            when(findUseCase.findById(ConsultationMother.CONSULTATION_ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(get("/consultations/{id}", ConsultationMother.CONSULTATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.anamnesis").value("Anamnesis del paciente"));
        }

        @Test
        @DisplayName("una consulta inexistente responde 404, no 500")
        void una_consulta_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new ConsultationNotFoundException(99L));

            mockMvc.perform(get("/consultations/{id}", 99L)).andExpect(status().isNotFound());
        }
    }
}
