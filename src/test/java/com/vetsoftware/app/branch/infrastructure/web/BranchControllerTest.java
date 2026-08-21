package com.vetsoftware.app.branch.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.command.UpdateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.dto.CitySummaryDto;
import com.vetsoftware.app.branch.application.dto.CompanySummaryDto;
import com.vetsoftware.app.branch.application.port.in.ActivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.CreateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.DeactivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.ListBranchesUseCase;
import com.vetsoftware.app.branch.application.port.in.UpdateBranchUseCase;
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
 * Rodaja HTTP de {@code BranchController}: rutas, binding, validación del
 * request, códigos de estado y forma del JSON — incluidos los companion
 * {@code CitySummary}/{@code CompanySummary} que arma {@code toResponse}.
 *
 * <p>
 * La empresa nunca viaja en el cuerpo: la pone {@code Authz} desde el contexto.
 * Cada traducción request→command se comprueba contra el {@code companyId} fijo
 * de {@link WebMvcSliceConfig}.
 */
@WebMvcTest(BranchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BranchController — contrato HTTP")
class BranchControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long CITY_ID = 5L;
    private static final Long BRANCH_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBranchUseCase createUseCase;
    @MockitoBean
    private UpdateBranchUseCase updateUseCase;
    @MockitoBean
    private ListBranchesUseCase listUseCase;
    @MockitoBean
    private ActivateBranchUseCase activateUseCase;
    @MockitoBean
    private DeactivateBranchUseCase deactivateUseCase;

    private static BranchDto sedeDto() {
        return new BranchDto(BRANCH_ID, "Sede Norte", "NORTE", "Cra 1", "3001112233",
                new CitySummaryDto(CITY_ID, "Bogotá"),
                new CompanySummaryDto(COMPANY_ID, "Vet SAS", "900123456"),
                LocalDateTime.of(2026, 1, 15, 8, 0), true);
    }

    @Nested
    @DisplayName("creación")
    class Creacion {

        @Test
        @DisplayName("POST /branches sella la empresa del contexto y responde 201 con el body mapeado")
        void post_crea_y_sella_la_empresa_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(sedeDto());

            mockMvc.perform(post("/branches").contentType(MediaType.APPLICATION_JSON).content(
                    """
                            {"name":"Sede Norte","code":"NORTE","address":"Cra 1","phone":"3001112233","cityId":5}
                            """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.city.id").value(5))
                    .andExpect(jsonPath("$.city.name").value("Bogotá"))
                    .andExpect(jsonPath("$.company.id").value(COMPANY_ID))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.active").value(true));

            verify(createUseCase).execute(new CreateBranchCommand("Sede Norte", "NORTE", "Cra 1",
                    "3001112233", 5L, COMPANY_ID));
        }

        @Test
        @DisplayName("POST /branches sin nombre responde 400 y no llama al caso de uso")
        void post_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/branches").contentType(MediaType.APPLICATION_JSON).content("""
                    {"code":"NORTE","cityId":5}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET /branches devuelve la lista mapeada de la empresa del contexto")
        void get_lista_todas_las_sedes_de_la_empresa() throws Exception {
            when(listUseCase.listAll(COMPANY_ID)).thenReturn(List.of(sedeDto()));

            mockMvc.perform(get("/branches")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("NORTE"))
                    .andExpect(jsonPath("$[0].city.id").value(5));
        }
    }

    @Nested
    @DisplayName("actualización")
    class Actualizacion {

        @Test
        @DisplayName("PUT /branches/{id} traslada el id de la ruta y la empresa del contexto")
        void put_actualiza_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(sedeDto());

            mockMvc.perform(put("/branches/1").contentType(MediaType.APPLICATION_JSON).content(
                    """
                            {"name":"Sede Norte","code":"NORTE","address":"Cra 1","phone":"3001112233","cityId":5}
                            """))
                    .andExpect(status().isOk());

            verify(updateUseCase).execute(new UpdateBranchCommand(BRANCH_ID, "Sede Norte", "NORTE",
                    "Cra 1", "3001112233", 5L, COMPANY_ID));
        }

        @Test
        @DisplayName("PUT /branches/{id} con código en blanco responde 400")
        void put_con_codigo_en_blanco_responde_400() throws Exception {
            mockMvc.perform(put("/branches/1").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Sede Norte","code":"   ","cityId":5}
                    """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("ciclo de estado")
    class CicloDeEstado {

        @Test
        @DisplayName("PATCH /branches/{id}/activate delega en el caso de uso con id y empresa")
        void patch_activate_delega_en_el_caso_de_uso() throws Exception {
            when(activateUseCase.execute(BRANCH_ID, COMPANY_ID)).thenReturn(sedeDto());

            mockMvc.perform(patch("/branches/1/activate")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("PATCH /branches/{id}/deactivate delega en el caso de uso con id y empresa")
        void patch_deactivate_delega_en_el_caso_de_uso() throws Exception {
            when(deactivateUseCase.execute(BRANCH_ID, COMPANY_ID)).thenReturn(sedeDto());

            mockMvc.perform(patch("/branches/1/deactivate")).andExpect(status().isOk());

            verify(deactivateUseCase).execute(BRANCH_ID, COMPANY_ID);
        }
    }
}
