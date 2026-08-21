package com.vetsoftware.app.employee.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.vetsoftware.app.employee.application.command.ChangeMyPasswordCommand;
import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.command.ResendInvitationCommand;
import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.port.in.ChangeMyPasswordUseCase;
import com.vetsoftware.app.employee.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.InviteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesByCompanyUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.in.ReactivateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.ResendInvitationUseCase;
import com.vetsoftware.app.employee.application.port.in.SearchEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.employee.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
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
 * Rodaja HTTP de {@code EmployeeController}: rutas, binding, validación del
 * request, códigos de estado y forma del JSON (incluidos los companion
 * {@code CompanySummary}/{@code EmployeeRoleSummary}/{@code BranchSummary}). La
 * empresa siempre viene del contexto — nunca del cliente.
 */
@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("EmployeeController — contrato HTTP")
class EmployeeControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private InviteEmployeeUseCase inviteUseCase;
    @MockitoBean
    private ResendInvitationUseCase resendInvitationUseCase;
    @MockitoBean
    private ChangeMyPasswordUseCase changeMyPasswordUseCase;
    @MockitoBean
    private UpdateEmployeeUseCase updateUseCase;
    @MockitoBean
    private FindEmployeeUseCase findUseCase;
    @MockitoBean
    private ListEmployeesUseCase listUseCase;
    @MockitoBean
    private ListEmployeesByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private SearchEmployeesUseCase searchUseCase;
    @MockitoBean
    private DeleteEmployeeUseCase deleteUseCase;
    @MockitoBean
    private ReactivateEmployeeUseCase reactivateUseCase;
    @MockitoBean
    private SuggestEmployeeCodeUseCase suggestCodeUseCase;
    @MockitoBean
    private CheckEmployeeCodeAvailabilityUseCase checkCodeAvailabilityUseCase;

    @BeforeEach
    void resellarEmpresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(COMPANY_ID);
        // La edicion y la reactivacion acotan por empresa y toleran el principal
        // cross-tenant, asi que leen la variante que devuelve null para SYSTEM.
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    @Nested
    @DisplayName("alta")
    class Alta {

        @Test
        @DisplayName("POST /employees sella la empresa del contexto y responde 201 con el body mapeado")
        void post_crea_y_sella_la_empresa_del_contexto() throws Exception {
            when(inviteUseCase.execute(any())).thenReturn(EmployeeMother.dto());

            mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content("""
                    {"employeeCode":"VV-MARIANA","password":"Temporal123*","name":"Mariana Rojas",
                     "email":"mariana@vetrina.co","roleIds":[3],"branchIds":[7]}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeCode").value("VV-MARIANA"))
                    .andExpect(jsonPath("$.company.id").value(COMPANY_ID))
                    .andExpect(jsonPath("$.roles[0].code").value("VET"))
                    .andExpect(jsonPath("$.branches[0].name").value("Sede Norte"));

            verify(inviteUseCase).execute(new InviteEmployeeCommand("VV-MARIANA", "Temporal123*",
                    "Mariana Rojas", "mariana@vetrina.co", COMPANY_ID, List.of(3L), List.of(7L)));
        }

        @Test
        @DisplayName("POST /employees sin sedes responde 400 y no llama al caso de uso")
        void post_sin_sedes_responde_400() throws Exception {
            mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content("""
                    {"employeeCode":"VV-MARIANA","password":"Temporal123*","name":"Mariana Rojas",
                     "email":"mariana@vetrina.co","roleIds":[3],"branchIds":[]}
                    """)).andExpect(status().isBadRequest());

            verify(inviteUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("reenvio de invitacion")
    class ReenvioDeInvitacion {

        @Test
        @DisplayName("POST /employees/{id}/resend-invitation traslada el id de la ruta y la empresa del contexto")
        void post_resend_invitation_traslada_el_id_y_la_empresa() throws Exception {
            when(resendInvitationUseCase.execute(any())).thenReturn(EmployeeMother.dto());

            mockMvc.perform(post("/employees/55/resend-invitation")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"password":"Nueva123*"}
                            """)).andExpect(status().isOk());

            verify(resendInvitationUseCase)
                    .execute(new ResendInvitationCommand(55L, "Nueva123*", COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("cambio de la propia clave")
    class CambioDeClave {

        @Test
        @DisplayName("POST /employees/me/change-password responde 204 y usa el empleado del contexto")
        void post_change_password_responde_204() throws Exception {
            when(authz.currentEmployeeId()).thenReturn(55L);
            when(changeMyPasswordUseCase.execute(any())).thenReturn(false);

            mockMvc.perform(post("/employees/me/change-password")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"newPassword":"Nueva123*"}
                            """)).andExpect(status().isNoContent());

            // El controller inyecta las dos cosas desde el contexto: el empleado y su
            // empresa. El request solo trae la contrasena nueva.
            verify(changeMyPasswordUseCase)
                    .execute(new ChangeMyPasswordCommand(55L, "Nueva123*", COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET /employees devuelve todos los empleados — endpoint SYSTEM")
        void get_lista_todos_los_empleados() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(EmployeeMother.dto()));

            mockMvc.perform(get("/employees")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].employeeCode").value("VV-MARIANA"));
        }

        @Test
        @DisplayName("GET /employees/by-company usa la empresa del contexto")
        void get_by_company_usa_la_empresa_del_contexto() throws Exception {
            when(listByCompanyUseCase.listByCompany(COMPANY_ID))
                    .thenReturn(List.of(EmployeeMother.dto()));

            mockMvc.perform(get("/employees/by-company")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].company.id").value(COMPANY_ID));
        }

        @Test
        @DisplayName("GET /employees/search pagina y filtra por la empresa del contexto")
        void get_search_pagina_y_filtra_por_la_empresa() throws Exception {
            when(searchUseCase.search(new SearchEmployeesCommand(COMPANY_ID, "mariana", 0, 15)))
                    .thenReturn(new PageResult<>(List.of(EmployeeMother.dto()), 0, 15, 1, 1));

            mockMvc.perform(get("/employees/search").param("q", "mariana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeCode").value("VV-MARIANA"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /employees/suggest-code delega en el caso de uso con la empresa del contexto")
        void get_suggest_code_delega_con_la_empresa_del_contexto() throws Exception {
            when(suggestCodeUseCase.suggest(COMPANY_ID, "Mariana")).thenReturn("VV-MARIANA");

            mockMvc.perform(get("/employees/suggest-code").param("name", "Mariana"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VV-MARIANA"));
        }

        @Test
        @DisplayName("GET /employees/code-availability responde con la disponibilidad del codigo")
        void get_code_availability_responde_con_la_disponibilidad() throws Exception {
            when(checkCodeAvailabilityUseCase.isAvailable("VV-MARIANA")).thenReturn(false);

            mockMvc.perform(get("/employees/code-availability").param("code", "VV-MARIANA"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        }

        @Test
        @DisplayName("GET /employees/{id} resuelve la empresa desde el contexto, no del cliente")
        void get_por_id_usa_la_empresa_del_contexto() throws Exception {
            when(findUseCase.findById(55L, COMPANY_ID)).thenReturn(EmployeeMother.dto());

            mockMvc.perform(get("/employees/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeCode").value("VV-MARIANA"));
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT /employees/{id} traslada el id de la ruta y sella la empresa del contexto")
        void put_actualiza_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(EmployeeMother.dto());

            mockMvc.perform(put("/employees/55").contentType(MediaType.APPLICATION_JSON).content("""
                    {"employeeCode":"VV-NUEVO","name":"Mariana Rojas","email":"mariana@vetrina.co"}
                    """)).andExpect(status().isOk());

            verify(updateUseCase).execute(new UpdateEmployeeCommand(55L, "VV-NUEVO",
                    "Mariana Rojas", "mariana@vetrina.co", COMPANY_ID));
        }

        @Test
        @DisplayName("PUT /employees/{id} con correo invalido responde 400")
        void put_con_correo_invalido_responde_400() throws Exception {
            mockMvc.perform(put("/employees/55").contentType(MediaType.APPLICATION_JSON).content("""
                    {"employeeCode":"VV-NUEVO","name":"Mariana Rojas","email":"no-es-un-correo"}
                    """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("DELETE /employees/{id} responde 204 y delega en el caso de uso")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/employees/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(eq(55L), any());
        }

        @Test
        @DisplayName("PATCH /employees/{id}/enable reactiva con la empresa del contexto")
        void patch_enable_reactiva_y_devuelve_el_empleado() throws Exception {
            when(reactivateUseCase.execute(55L, COMPANY_ID)).thenReturn(EmployeeMother.dto());

            mockMvc.perform(patch("/employees/55/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeCode").value("VV-MARIANA"));

            verify(reactivateUseCase).execute(55L, COMPANY_ID);
        }
    }
}
