package com.vetsoftware.app.medicamentprescription.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
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

@WebMvcTest(MedicamentPrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("MedicamentPrescriptionController — contrato HTTP")
class MedicamentPrescriptionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_VALIDO = """
            {"medicamentId":701,"presentation":"Tableta","quantity":2.0,
             "posology":"Cada 12 horas por 7 dias","observation":"Con alimento",
             "prescriptionId":702}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateMedicamentPrescriptionUseCase createUseCase;
    @MockitoBean
    private ListMedicamentPrescriptionsUseCase listUseCase;

    @BeforeEach
    void companyIdOrNullDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static MedicamentPrescriptionDto dto() {
        return MedicamentPrescriptionDto.from(MedicamentPrescriptionMother.persistida());
    }

    @Nested
    @DisplayName("POST /medicament-prescriptions")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la linea creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(MedicamentPrescriptionMother.ID))
                    .andExpect(jsonPath("$.presentation").value("Tableta"))
                    .andExpect(jsonPath("$.prescription.id")
                            .value(MedicamentPrescriptionMother.PRESCRIPTION_ID));
        }

        @Test
        @DisplayName("arma el comando exactamente con los campos del cuerpo")
        void arma_el_comando_con_los_campos_del_cuerpo() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            // La empresa la pone el controller desde el token, nunca el cuerpo: es
            // el septimo campo del comando y lo que acota la receta al tenant.
            verify(createUseCase).execute(new CreateMedicamentPrescriptionCommand(
                    MedicamentPrescriptionMother.MEDICAMENT_ID, "Tableta", 2.0,
                    "Cada 12 horas por 7 dias", "Con alimento",
                    MedicamentPrescriptionMother.PRESCRIPTION_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("sin medicamentId responde 400 y no crea nada")
        void sin_medicamento_responde_400() throws Exception {
            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"presentation":"Tableta","quantity":2.0,"posology":"Cada 12 horas",
                             "prescriptionId":702}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una cantidad no positiva responde 400")
        void cantidad_no_positiva_responde_400() throws Exception {
            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"medicamentId":701,"presentation":"Tableta","quantity":0,
                             "posology":"Cada 12 horas","prescriptionId":702}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una presentacion en blanco responde 400")
        void presentacion_en_blanco_responde_400() throws Exception {
            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"medicamentId":701,"presentation":"   ","quantity":2.0,
                             "posology":"Cada 12 horas","prescriptionId":702}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin prescriptionId responde 400")
        void sin_prescription_id_responde_400() throws Exception {
            mockMvc.perform(post("/medicament-prescriptions")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"medicamentId":701,"presentation":"Tableta","quantity":2.0,
                             "posology":"Cada 12 horas"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /medicament-prescriptions")
    class Listado {

        @Test
        @DisplayName("responde 200 con la pagina de lineas")
        void responde_200_con_la_pagina() throws Exception {
            when(listUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/medicament-prescriptions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
