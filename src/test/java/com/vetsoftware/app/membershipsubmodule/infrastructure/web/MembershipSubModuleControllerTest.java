package com.vetsoftware.app.membershipsubmodule.infrastructure.web;

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

import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.DeleteMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.FindMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ListMembershipSubModulesUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ReactivateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.UpdateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
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
 * Rodaja HTTP del controller de enlaces membresia-submodulo: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * Esta feature no tiene tenant: los seis puertos de entrada son
 * {@code hasRole('SYSTEM')} a secas, asi que a diferencia de otros controllers
 * no hay {@code companyId} que verificar en el command.
 */
@WebMvcTest(MembershipSubModuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("MembershipSubModuleController — contrato HTTP")
class MembershipSubModuleControllerTest {

    private static final String CUERPO_VALIDO = """
            {"membershipId":900,"subModuleId":980}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateMembershipSubModuleUseCase createUseCase;
    @MockitoBean
    private UpdateMembershipSubModuleUseCase updateUseCase;
    @MockitoBean
    private FindMembershipSubModuleUseCase findUseCase;
    @MockitoBean
    private ListMembershipSubModulesUseCase listUseCase;
    @MockitoBean
    private DeleteMembershipSubModuleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateMembershipSubModuleUseCase reactivateUseCase;

    private static MembershipSubModuleDto facturacion() {
        return new MembershipSubModuleDto(500L, new MembershipSummaryDto(900L, "Plan Premium"),
                new SubModuleSummaryDto(980L, "Facturacion", "FACT"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Nested
    @DisplayName("POST /membership-sub-modules")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(facturacion());

            mockMvc.perform(post("/membership-sub-modules").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.membership.id").value(900))
                    .andExpect(jsonPath("$.membership.name").value("Plan Premium"))
                    .andExpect(jsonPath("$.subModule.code").value("FACT"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(facturacion());

            mockMvc.perform(post("/membership-sub-modules").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase).execute(new CreateMembershipSubModuleCommand(900L, 980L));
        }

        @Test
        @DisplayName("membershipId nulo responde 400 y no llega al caso de uso")
        void membership_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/membership-sub-modules").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"subModuleId":980}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("subModuleId nulo responde 400 y no llega al caso de uso")
        void sub_module_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/membership-sub-modules").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"membershipId":900}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una membresia inexistente sale como 400, no como 500")
        void membresia_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Membership not found: 900"));

            mockMvc.perform(post("/membership-sub-modules").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /membership-sub-modules lista todos los enlaces")
        void get_lista_todos_los_enlaces() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(facturacion()));

            mockMvc.perform(get("/membership-sub-modules")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].subModule.name").value("Facturacion"));
        }

        @Test
        @DisplayName("GET /membership-sub-modules/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(500L)).thenReturn(facturacion());

            mockMvc.perform(get("/membership-sub-modules/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.membership.id").value(900));
        }

        @Test
        @DisplayName("GET /membership-sub-modules/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L))
                    .thenThrow(new MembershipSubModuleNotFoundException(999L));

            mockMvc.perform(get("/membership-sub-modules/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre un enlace existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /membership-sub-modules/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(facturacion());

            mockMvc.perform(put("/membership-sub-modules/500")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(500));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(facturacion());

            mockMvc.perform(put("/membership-sub-modules/500")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateMembershipSubModuleCommand(500L, 900L, 980L));
        }

        @Test
        @DisplayName("PUT sin subModuleId responde 400 y no llega al caso de uso")
        void put_sin_sub_module_id_responde_400() throws Exception {
            mockMvc.perform(put("/membership-sub-modules/500")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"membershipId":900}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre un enlace inexistente responde 404")
        void put_de_un_enlace_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new MembershipSubModuleNotFoundException(500L));

            mockMvc.perform(put("/membership-sub-modules/500")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /membership-sub-modules/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/membership-sub-modules/500"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(500L);
        }

        @Test
        @DisplayName("DELETE de un enlace inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new MembershipSubModuleNotFoundException(999L)).when(deleteUseCase)
                    .execute(999L);

            mockMvc.perform(delete("/membership-sub-modules/999")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /membership-sub-modules/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(500L)).thenReturn(facturacion());

            mockMvc.perform(patch("/membership-sub-modules/500/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de un enlace inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L))
                    .thenThrow(new MembershipSubModuleNotFoundException(999L));

            mockMvc.perform(patch("/membership-sub-modules/999/enable"))
                    .andExpect(status().isNotFound());
        }
    }
}
