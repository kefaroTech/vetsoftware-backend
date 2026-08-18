package com.vetsoftware.app.medicament.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.CompanySummaryDto;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.DeleteMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.FindMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListAvailableMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ReactivateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.UpdateMedicamentUseCase;
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
 * Rodaja HTTP de {@link MedicamentController}: rutas, binding, forma del JSON y
 * que la empresa siempre llega de {@code Authz}, nunca del cuerpo.
 */
@WebMvcTest(MedicamentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("MedicamentController — contrato HTTP")
class MedicamentControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateMedicamentUseCase createUseCase;
    @MockitoBean
    private UpdateMedicamentUseCase updateUseCase;
    @MockitoBean
    private FindMedicamentUseCase findUseCase;
    @MockitoBean
    private ListMedicamentsUseCase listUseCase;
    @MockitoBean
    private ListAvailableMedicamentsUseCase listAvailableUseCase;
    @MockitoBean
    private ListDisabledMedicamentsUseCase listDisabledUseCase;
    @MockitoBean
    private DeleteMedicamentUseCase deleteUseCase;
    @MockitoBean
    private ReactivateMedicamentUseCase reactivateUseCase;

    private static MedicamentDto medicamentoGeneral() {
        return new MedicamentDto(1L, "Amoxicilina", "Antibiotico", null, true,
                LocalDateTime.of(2026, 1, 1, 0, 0), true);
    }

    private static MedicamentDto medicamentoDeEmpresa() {
        return new MedicamentDto(2L, "Suero", "Formula propia",
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "900123456"), false,
                LocalDateTime.of(2026, 1, 1, 0, 0), true);
    }

    @Nested
    @DisplayName("POST /medicaments")
    class Creacion {

        @Test
        @DisplayName("siempre crea con general=false y la empresa del contexto, nunca del cuerpo")
        void crea_con_general_false_y_la_empresa_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(medicamentoDeEmpresa());

            mockMvc.perform(post("/medicaments").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Suero","description":"Formula propia"}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Suero"));

            org.mockito.ArgumentCaptor<CreateMedicamentCommand> captor = org.mockito.ArgumentCaptor
                    .forClass(CreateMedicamentCommand.class);
            verify(createUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().companyId())
                    .isEqualTo(COMPANY_ID);
            org.assertj.core.api.Assertions.assertThat(captor.getValue().general()).isFalse();
        }

        @Test
        @DisplayName("rechaza un name en blanco con 400")
        void rechaza_name_en_blanco() throws Exception {
            mockMvc.perform(post("/medicaments").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","description":"x"}
                    """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /medicaments")
    class Listado {

        @Test
        @DisplayName("lista el catalogo paginado")
        void lista_el_catalogo_paginado() throws Exception {
            when(listUseCase.listAll(0, 20))
                    .thenReturn(PageResult.of(List.of(medicamentoGeneral()), 0, 20, 1L));

            mockMvc.perform(get("/medicaments")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Amoxicilina"));
        }

        @Test
        @DisplayName("/available usa la empresa del contexto")
        void available_usa_la_empresa_del_contexto() throws Exception {
            when(listAvailableUseCase.listAvailable(COMPANY_ID))
                    .thenReturn(List.of(medicamentoGeneral(), medicamentoDeEmpresa()));

            mockMvc.perform(get("/medicaments/available")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("/disabled usa la empresa del contexto")
        void disabled_usa_la_empresa_del_contexto() throws Exception {
            when(listDisabledUseCase.listDisabled(COMPANY_ID)).thenReturn(List.of());

            mockMvc.perform(get("/medicaments/disabled")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /medicaments/{id}")
    class Busqueda {

        @Test
        @DisplayName("busca con el id y la empresa del contexto")
        void busca_con_id_y_empresa_del_contexto() throws Exception {
            when(findUseCase.findById(eq(1L), eq(COMPANY_ID))).thenReturn(medicamentoGeneral());

            mockMvc.perform(get("/medicaments/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Amoxicilina"));
        }
    }

    @Nested
    @DisplayName("PUT /medicaments/{id}")
    class Actualizacion {

        @Test
        @DisplayName("traduce el path id + el cuerpo al command")
        void traduce_el_path_id_y_el_cuerpo_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(medicamentoDeEmpresa());

            mockMvc.perform(
                    put("/medicaments/2").contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Suero fisiologico","description":"Actualizado"}
                            """)).andExpect(status().isOk());

            org.mockito.ArgumentCaptor<UpdateMedicamentCommand> captor = org.mockito.ArgumentCaptor
                    .forClass(UpdateMedicamentCommand.class);
            verify(updateUseCase).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().id()).isEqualTo(2L);
            // El request NO trae companyId: lo pone el controller desde el contexto.
            org.assertj.core.api.Assertions.assertThat(captor.getValue().companyId())
                    .isEqualTo(WebMvcSliceConfig.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH /enable")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete responde 204")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/medicaments/1")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(1L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("enable reactiva y devuelve el medicamento acotando por la empresa del contexto")
        void enable_reactiva_y_devuelve_el_medicamento() throws Exception {
            when(reactivateUseCase.execute(1L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(medicamentoGeneral());

            mockMvc.perform(patch("/medicaments/1/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Amoxicilina"));

            verify(reactivateUseCase).execute(1L, WebMvcSliceConfig.COMPANY_ID);
        }
    }
}
