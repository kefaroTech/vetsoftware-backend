package com.vetsoftware.app.medicamentprescription.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.DeleteMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.FindMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ReactivateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.UpdateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
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
    private UpdateMedicamentPrescriptionUseCase updateUseCase;
    @MockitoBean
    private FindMedicamentPrescriptionUseCase findUseCase;
    @MockitoBean
    private ListMedicamentPrescriptionsUseCase listUseCase;
    @MockitoBean
    private DeleteMedicamentPrescriptionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateMedicamentPrescriptionUseCase reactivateUseCase;

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

    @Nested
    @DisplayName("GET /medicament-prescriptions/{id}")
    class Busqueda {

        @Test
        @DisplayName("responde 200 con la linea de la empresa del contexto")
        void responde_200() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(MedicamentPrescriptionMother.ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(get("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(MedicamentPrescriptionMother.ID));
        }

        @Test
        @DisplayName("una linea inexistente responde 404")
        void linea_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new MedicamentPrescriptionNotFoundException(999L));

            mockMvc.perform(get("/medicament-prescriptions/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /medicament-prescriptions/{id}")
    class Actualizacion {

        @Test
        @DisplayName("arma el comando con el id de la ruta y la empresa del contexto")
        void arma_el_comando_y_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk());

            verify(updateUseCase).execute(new UpdateMedicamentPrescriptionCommand(
                    MedicamentPrescriptionMother.ID, MedicamentPrescriptionMother.MEDICAMENT_ID,
                    "Tableta", 2.0, "Cada 12 horas por 7 dias", "Con alimento",
                    MedicamentPrescriptionMother.PRESCRIPTION_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("una linea que ya no existe responde 404")
        void linea_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(
                    new MedicamentPrescriptionNotFoundException(MedicamentPrescriptionMother.ID));

            mockMvc.perform(put("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("una referencia inexistente responde 400")
        void referencia_inexistente_responde_400() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Medicament not found: 701"));

            mockMvc.perform(put("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH /medicament-prescriptions/{id}")
    class BorradoYReactivacion {

        @Test
        @DisplayName("DELETE responde 204 y usa el companyId (o null) del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(
                    delete("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(MedicamentPrescriptionMother.ID, COMPANY_ID);
        }

        @Test
        @DisplayName("borrar una linea inexistente responde 404")
        void borrar_linea_inexistente_responde_404() throws Exception {
            org.mockito.Mockito
                    .doThrow(new MedicamentPrescriptionNotFoundException(
                            MedicamentPrescriptionMother.ID))
                    .when(deleteUseCase).execute(anyLong(), any());

            mockMvc.perform(
                    delete("/medicament-prescriptions/{id}", MedicamentPrescriptionMother.ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /enable reactiva y responde 200")
        void patch_enable_reactiva() throws Exception {
            when(reactivateUseCase.execute(MedicamentPrescriptionMother.ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(
                    patch("/medicament-prescriptions/{id}/enable", MedicamentPrescriptionMother.ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar una linea inexistente responde 404")
        void reactivar_linea_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L, COMPANY_ID))
                    .thenThrow(new MedicamentPrescriptionNotFoundException(999L));

            mockMvc.perform(patch("/medicament-prescriptions/{id}/enable", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}
