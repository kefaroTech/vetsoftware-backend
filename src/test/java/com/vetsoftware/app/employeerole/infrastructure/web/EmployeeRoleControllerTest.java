package com.vetsoftware.app.employeerole.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.FindEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.ListEmployeeRolesUseCase;
import com.vetsoftware.app.employeerole.application.port.in.ReactivateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.UpdateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
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
 * Rodaja HTTP de la asignacion empleado-rol: rutas, binding, validacion del
 * request y forma del JSON. La baja y la reactivacion sellan la empresa desde
 * {@code Authz} —igual que {@code RolePermissionController}—, asi que el
 * contexto se stubea aqui: el cliente nunca elige el tenant. Se importa
 * {@code WebMvcSliceConfig} por el {@code GlobalExceptionHandler} real, que es
 * medio contrato de la API (sin el, una excepcion de dominio saldria 500).
 */
@WebMvcTest(EmployeeRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("EmployeeRoleController — contrato HTTP")
class EmployeeRoleControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateEmployeeRoleUseCase createUseCase;
    @MockitoBean
    private UpdateEmployeeRoleUseCase updateUseCase;
    @MockitoBean
    private FindEmployeeRoleUseCase findUseCase;
    @MockitoBean
    private ListEmployeeRolesUseCase listUseCase;
    @MockitoBean
    private DeleteEmployeeRoleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateEmployeeRoleUseCase reactivateUseCase;

    @BeforeEach
    void sellarLaEmpresaDelContexto() {
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static EmployeeRoleDto asignacion() {
        return EmployeeRoleDto.from(EmployeeRoleMother.habilitado());
    }

    @Nested
    @DisplayName("POST /employee-roles")
    class Creacion {

        @Test
        @DisplayName("responde 201 y sella la empresa del contexto en el command")
        void responde_201_con_la_asignacion_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(asignacion());

            mockMvc.perform(
                    post("/employee-roles").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":7,"roleId":3}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .andExpect(jsonPath("$.employee.id").value(EmployeeRoleMother.EMPLEADO.id()))
                    .andExpect(jsonPath("$.employee.employeeCode")
                            .value(EmployeeRoleMother.EMPLEADO.employeeCode()))
                    .andExpect(jsonPath("$.role.code")
                            .value(EmployeeRoleMother.ROL_VETERINARIO.code()))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(createUseCase).execute(new CreateEmployeeRoleCommand(7L, 3L, COMPANY_ID));
        }

        /**
         * El cliente no elige el tenant: aunque el JSON traiga {@code companyId}, el
         * request no lo declara y el command se arma con la empresa del principal.
         */
        @Test
        @DisplayName("un companyId en el body se ignora: la empresa la pone el contexto")
        void un_company_id_en_el_body_se_ignora() throws Exception {
            when(createUseCase.execute(any())).thenReturn(asignacion());

            mockMvc.perform(
                    post("/employee-roles").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":7,"roleId":3,"companyId":999}
                            """)).andExpect(status().isCreated());

            verify(createUseCase).execute(new CreateEmployeeRoleCommand(7L, 3L, COMPANY_ID));
        }

        @Test
        @DisplayName("sin employeeId responde 400 y no crea nada")
        void sin_employee_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/employee-roles").contentType(MediaType.APPLICATION_JSON).content("""
                            {"roleId":3}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un empleado inexistente responde 400, no 500")
        void empleado_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Employee not found: 7"));

            mockMvc.perform(
                    post("/employee-roles").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":7,"roleId":3}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /employee-roles")
    class Listado {

        @Test
        @DisplayName("lista todas las asignaciones")
        void lista_todas_las_asignaciones() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(asignacion()));

            mockMvc.perform(get("/employee-roles")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].role.name").value("Veterinario"));
        }
    }

    @Nested
    @DisplayName("GET /employee-roles/{id}")
    class Busqueda {

        @Test
        @DisplayName("responde 200 con la asignacion encontrada")
        void responde_200_con_la_asignacion() throws Exception {
            when(findUseCase.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(asignacion());

            mockMvc.perform(get("/employee-roles/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
        }

        @Test
        @DisplayName("una asignacion inexistente responde 404")
        void asignacion_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID)).thenThrow(
                    new EmployeeRoleNotFoundException(EmployeeRoleMother.EMPLOYEE_ROLE_ID));

            mockMvc.perform(get("/employee-roles/500")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /employee-roles/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta")
        void responde_200_y_arma_el_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(asignacion());

            mockMvc.perform(
                    put("/employee-roles/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":8,"roleId":4}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EmployeeRoleMother.EMPLOYEE_ROLE_ID));

            verify(updateUseCase).execute(
                    new UpdateEmployeeRoleCommand(EmployeeRoleMother.EMPLOYEE_ROLE_ID, 8L, 4L));
        }

        @Test
        @DisplayName("un rol inexistente responde 400, no 500")
        void rol_inexistente_responde_400() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Role not found: 4"));

            mockMvc.perform(
                    put("/employee-roles/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":8,"roleId":4}
                            """)).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("una asignacion inexistente responde 404")
        void asignacion_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(
                    new EmployeeRoleNotFoundException(EmployeeRoleMother.EMPLOYEE_ROLE_ID));

            mockMvc.perform(
                    put("/employee-roles/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":8,"roleId":4}
                            """)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("sin roleId responde 400 y no actualiza")
        void sin_role_id_responde_400() throws Exception {
            mockMvc.perform(
                    put("/employee-roles/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"employeeId":8}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH /employee-roles/{id}")
    class EstadoDeLaAsignacion {

        @Test
        @DisplayName("DELETE responde 204 y borra por el id de la ruta, con la empresa del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/employee-roles/500")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(EmployeeRoleMother.EMPLOYEE_ROLE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una asignacion inexistente responde 404")
        void delete_de_asignacion_inexistente_responde_404() throws Exception {
            doThrow(new EmployeeRoleNotFoundException(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .when(deleteUseCase).execute(EmployeeRoleMother.EMPLOYEE_ROLE_ID, COMPANY_ID);

            mockMvc.perform(delete("/employee-roles/500")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /enable reactiva con la empresa del contexto y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(EmployeeRoleMother.EMPLOYEE_ROLE_ID, COMPANY_ID))
                    .thenReturn(asignacion());

            mockMvc.perform(patch("/employee-roles/500/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(EmployeeRoleMother.EMPLOYEE_ROLE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("reactivar una asignacion inexistente responde 404")
        void reactivar_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(anyLong(), anyLong())).thenThrow(
                    new EmployeeRoleNotFoundException(EmployeeRoleMother.EMPLOYEE_ROLE_ID));

            mockMvc.perform(patch("/employee-roles/500/enable")).andExpect(status().isNotFound());
        }
    }
}
