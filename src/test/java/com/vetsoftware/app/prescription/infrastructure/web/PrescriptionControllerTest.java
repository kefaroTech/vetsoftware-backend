package com.vetsoftware.app.prescription.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.CreatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.ExportPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.FindPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.ListPrescriptionsUseCase;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
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

@WebMvcTest(PrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PrescriptionController — contrato HTTP")
class PrescriptionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final String CUERPO_VALIDO = """
            {"date":"2026-01-10","diagnosis":"Otitis externa","observations":"Control en 7 dias",
             "animalId":501,"consultationId":502}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreatePrescriptionUseCase createUseCase;
    @MockitoBean
    private FindPrescriptionUseCase findUseCase;
    @MockitoBean
    private ListPrescriptionsUseCase listUseCase;
    @MockitoBean
    private ExportPrescriptionUseCase exportUseCase;

    @BeforeEach
    void companyIdOrNullDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static PrescriptionDto dto() {
        return PrescriptionDto.from(PrescriptionMother.persistida(),
                PrescriptionMother.unMedicamento());
    }

    @Nested
    @DisplayName("POST /prescriptions")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la receta creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/prescriptions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PrescriptionMother.PRESCRIPTION_ID))
                    .andExpect(jsonPath("$.diagnosis").value("Otitis externa"))
                    .andExpect(jsonPath("$.medicaments.length()").value(1));
        }

        @Test
        @DisplayName("la empresa la pone el backend desde el contexto, no el request")
        void la_empresa_la_pone_el_backend() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/prescriptions").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase)
                    .execute(new CreatePrescriptionCommand(java.time.LocalDate.of(2026, 1, 10),
                            "Otitis externa", "Control en 7 dias", 501L, 502L, COMPANY_ID));
        }

        @Test
        @DisplayName("sin fecha responde 400 y no crea nada")
        void sin_fecha_responde_400() throws Exception {
            mockMvc.perform(
                    post("/prescriptions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":501,"consultationId":502}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /prescriptions")
    class Listado {

        @Test
        @DisplayName("responde 200 con la pagina de recetas")
        void responde_200_con_la_pagina() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/prescriptions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /prescriptions/{id}")
    class Busqueda {

        @Test
        @DisplayName("responde 200 con la receta buscada en la empresa del contexto")
        void responde_200() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(PrescriptionMother.PRESCRIPTION_ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(get("/prescriptions/{id}", PrescriptionMother.PRESCRIPTION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PrescriptionMother.PRESCRIPTION_ID));
        }

        @Test
        @DisplayName("una receta inexistente responde 404")
        void receta_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new PrescriptionNotFoundException(999L));

            mockMvc.perform(get("/prescriptions/{id}", 999L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /prescriptions/{id}/export.pdf")
    class Exportacion {

        @Test
        @DisplayName("responde 200 con el PDF adjunto y el nombre de archivo esperado")
        void responde_200_con_el_pdf_adjunto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(exportUseCase.execute(PrescriptionMother.PRESCRIPTION_ID, COMPANY_ID, EMPLOYEE_ID))
                    .thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(
                    get("/prescriptions/{id}/export.pdf", PrescriptionMother.PRESCRIPTION_ID))
                    .andExpect(status().isOk()).andExpect(
                            header().string("Content-Disposition", "attachment; filename=\"receta-"
                                    + PrescriptionMother.PRESCRIPTION_ID + ".pdf\""));
        }
    }
}
