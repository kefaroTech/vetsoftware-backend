package com.vetsoftware.app.employeebranch.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.BranchAccessDeniedException;
import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.in.GetEmployeeBranchesUseCase;
import com.vetsoftware.app.employeebranch.application.port.in.SetEmployeeBranchesUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.LinkedHashSet;
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
 * Rodaja HTTP de {@link EmployeeBranchController}. Lo que decide por su cuenta
 * y que por eso se afirma aqui: la empresa nunca viaja en el cuerpo (la pone
 * {@code Authz}), y {@code allBranches=true} expande al ALCANCE del actor —no a
 * toda la empresa— mientras que un set explicito pasa por
 * {@code authz.requireAssignableBranches}, que puede rechazarlo con 403.
 */
@WebMvcTest(EmployeeBranchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("EmployeeBranchController — contrato HTTP")
class EmployeeBranchControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private GetEmployeeBranchesUseCase getUseCase;
    @MockitoBean
    private SetEmployeeBranchesUseCase setUseCase;

    /**
     * {@code authz} es un mock manual expuesto por {@link WebMvcSliceConfig}, no un
     * {@code @MockitoBean}: Spring no lo resetea entre metodos porque el contexto
     * de la rodaja se cachea. Sin este limpiado, un {@code verify(authz, never())}
     * veria interacciones acumuladas de tests anteriores.
     */
    @BeforeEach
    void limpiarInteraccionesDeAuthz() {
        clearInvocations(authz);
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET devuelve las sedes que resuelve el caso de uso, acotadas a la empresa del contexto")
        void get_devuelve_las_sedes_del_empleado() throws Exception {
            when(getUseCase.execute(EMPLOYEE_ID, COMPANY_ID))
                    .thenReturn(new EmployeeBranchesDto(EMPLOYEE_ID, List.of(910L, 911L)));

            mockMvc.perform(get("/employees/100/branches")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId").value(100))
                    .andExpect(jsonPath("$.branchIds[0]").value(910))
                    .andExpect(jsonPath("$.branchIds[1]").value(911));
        }
    }

    @Nested
    @DisplayName("reasignacion")
    class Reasignacion {

        @Test
        @DisplayName("allBranches=true expande a las sedes del ALCANCE del actor, no a toda la empresa")
        void all_branches_expande_al_alcance_del_actor() throws Exception {
            when(authz.currentBranchIds()).thenReturn(new LinkedHashSet<>(List.of(910L, 911L)));
            when(setUseCase.execute(any()))
                    .thenReturn(new EmployeeBranchesDto(EMPLOYEE_ID, List.of(910L, 911L)));

            mockMvc.perform(put("/employees/100/branches").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"allBranches":true}
                            """)).andExpect(status().isOk());

            verify(setUseCase).execute(new SetEmployeeBranchesCommand(EMPLOYEE_ID, COMPANY_ID,
                    false, List.of(910L, 911L)));
            verify(authz, never()).requireAssignableBranches(any());
        }

        @Test
        @DisplayName("un set explicito exige que cada sede este en el alcance del actor")
        void un_set_explicito_exige_alcance_del_actor() throws Exception {
            when(setUseCase.execute(any()))
                    .thenReturn(new EmployeeBranchesDto(EMPLOYEE_ID, List.of(910L)));

            mockMvc.perform(put("/employees/100/branches").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"allBranches":false,"branchIds":[910]}
                            """)).andExpect(status().isOk());

            verify(authz).requireAssignableBranches(List.of(910L));
            verify(setUseCase).execute(
                    new SetEmployeeBranchesCommand(EMPLOYEE_ID, COMPANY_ID, false, List.of(910L)));
        }

        @Test
        @DisplayName("una sede fuera del alcance del actor responde 403 y no llega al caso de uso")
        void sede_fuera_de_alcance_responde_403() throws Exception {
            doThrow(new BranchAccessDeniedException("Branch not assignable by employee: 999"))
                    .when(authz).requireAssignableBranches(List.of(999L));

            mockMvc.perform(put("/employees/100/branches").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"allBranches":false,"branchIds":[999]}
                            """)).andExpect(status().isForbidden());

            verifyNoInteractions(setUseCase);
        }
    }
}
