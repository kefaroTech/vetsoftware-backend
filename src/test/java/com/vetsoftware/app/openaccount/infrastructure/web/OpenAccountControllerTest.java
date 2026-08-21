package com.vetsoftware.app.openaccount.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.DeleteOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.FindOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.GetOpenAccountsSummaryUseCase;
import com.vetsoftware.app.openaccount.application.port.in.ListOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.in.SearchOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.domain.InvalidOpenAccountStatusTransitionException;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
 * Rodaja HTTP de la cuenta abierta: rutas, binding, validacion del request,
 * codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * Lo que esta rodaja protege y ninguna otra capa cubre: que ni
 * {@code companyId} ni {@code employeeId} viajan en el cuerpo (los pone
 * {@code Authz}), que la sede sale de {@code resolveAccessibleBranch} y no del
 * request, y que los conflictos de dominio (transicion invalida, version
 * optimista) llegan al cliente como 409, no como 500.
 */
@WebMvcTest(OpenAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("OpenAccountController — contrato HTTP")
class OpenAccountControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    /** Sede que resuelve {@code Authz}; distinta de la que manda el request. */
    private static final Long SEDE_RESUELTA = 31L;
    /** Sede que manda el cliente: nunca debe llegar tal cual al command. */
    private static final long SEDE_PEDIDA = 77L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateOpenAccountUseCase createUseCase;
    @MockitoBean
    private FindOpenAccountUseCase findUseCase;
    @MockitoBean
    private ListOpenAccountsUseCase listUseCase;
    @MockitoBean
    private SearchOpenAccountsUseCase searchUseCase;
    @MockitoBean
    private GetOpenAccountsSummaryUseCase summaryUseCase;
    @MockitoBean
    private DeleteOpenAccountUseCase deleteUseCase;
    @MockitoBean
    private ChangeOpenAccountStatusUseCase changeStatusUseCase;

    /**
     * {@code WebMvcSliceConfig} no stubea el alcance de sedes ni el empleado por
     * {@code currentEmployeeId()} (solo el OrNull). Sin esto Mockito devolveria 0L
     * o null y el command llegaria firmado con datos que no existen.
     */
    @BeforeEach
    void resolverContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
        when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    private static OpenAccountDto abierta() {
        return OpenAccountDto.from(OpenAccountMother.abierta());
    }

    private static OpenAccountDto cerrada() {
        return OpenAccountDto.from(OpenAccountMother.cerrada());
    }

    @Nested
    @DisplayName("POST /open-accounts")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la cuenta creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(
                    post("/open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"ownerId":2}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @DisplayName("la empresa, el empleado y la sede los pone el backend, no el request")
        void el_command_lo_sella_el_backend() throws Exception {
            when(createUseCase.execute(any())).thenReturn(abierta());

            mockMvc.perform(
                    post("/open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"ownerId":2,"branchId":77}
                            """));

            // SEDE_RESUELTA != SEDE_PEDIDA: si el controller pasara la sede del cuerpo tal
            // cual, un empleado podria abrir cuenta en una sede que no le toca.
            verify(createUseCase).execute(
                    new CreateOpenAccountCommand(2L, SEDE_RESUELTA, COMPANY_ID, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("sin ownerId responde 400 y no crea nada")
        void sin_owner_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/open-accounts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /open-accounts")
    class Listado {

        @Test
        @DisplayName("responde con la pagina mapeada a la sede accesible")
        void responde_con_la_pagina_mapeada() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID, SEDE_RESUELTA, 0, 20))
                    .thenReturn(new PageResult<>(List.of(abierta()), 0, 20, 1L, 1));

            mockMvc.perform(get("/open-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(OpenAccountMother.OPEN_ACCOUNT_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("traslada los parametros de pagina al caso de uso")
        void traslada_los_parametros_de_pagina() throws Exception {
            when(listUseCase.listByCompany(COMPANY_ID, SEDE_RESUELTA, 1, 5))
                    .thenReturn(new PageResult<>(List.of(), 1, 5, 0L, 0));

            mockMvc.perform(get("/open-accounts").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk());

            verify(listUseCase).listByCompany(COMPANY_ID, SEDE_RESUELTA, 1, 5);
        }
    }

    @Nested
    @DisplayName("GET /open-accounts/search")
    class Busqueda {

        @Test
        @DisplayName("traduce los filtros del request al command")
        void traduce_los_filtros_al_command() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(abierta()), 0, 20, 1L, 1));

            mockMvc.perform(get("/open-accounts/search").param("ownerId", "2")
                    .param("enabled", "true").param("status", "CLOSE", "CANCEL").param("q", "juan"))
                    .andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchOpenAccountsCommand(COMPANY_ID, 2L, true,
                    List.of(OpenAccountStatus.CLOSE, OpenAccountStatus.CANCEL), "juan", 0, 20,
                    SEDE_RESUELTA));
        }

        @Test
        @DisplayName("sin parametros usa pagina 0, tamano 20 y sin filtros")
        void sin_parametros_usa_valores_por_defecto() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/open-accounts/search")).andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchOpenAccountsCommand(COMPANY_ID, null, null,
                    null, null, 0, 20, SEDE_RESUELTA));
        }
    }

    @Nested
    @DisplayName("GET /open-accounts/summary")
    class Resumen {

        @Test
        @DisplayName("responde con los contadores y el saldo pendiente")
        void responde_con_los_contadores_y_el_saldo() throws Exception {
            when(summaryUseCase.summarize(COMPANY_ID, SEDE_RESUELTA))
                    .thenReturn(new OpenAccountsSummaryDto(3L, 5L, new BigDecimal("40000.00")));

            mockMvc.perform(get("/open-accounts/summary")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.openCount").value(3))
                    .andExpect(jsonPath("$.closedCount").value(5))
                    .andExpect(jsonPath("$.totalOutstanding").value(40000.00));
        }
    }

    @Nested
    @DisplayName("GET /open-accounts/{id}")
    class Detalle {

        @Test
        @DisplayName("responde con la cuenta de la empresa")
        void responde_con_la_cuenta() throws Exception {
            when(findUseCase.findById(OpenAccountMother.OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(abierta());

            mockMvc.perform(get("/open-accounts/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));
        }

        @Test
        @DisplayName("una cuenta inexistente responde 404, no 500")
        void cuenta_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new OpenAccountNotFoundException(999L));

            mockMvc.perform(get("/open-accounts/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /open-accounts/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 y borra la cuenta de la empresa")
        void responde_204() throws Exception {
            mockMvc.perform(delete("/open-accounts/100")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(100L, COMPANY_ID);
        }

        @Test
        @DisplayName("una cuenta inexistente responde 404, no 500")
        void cuenta_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new OpenAccountNotFoundException(999L)).when(deleteUseCase)
                    .execute(999L, COMPANY_ID);

            mockMvc.perform(delete("/open-accounts/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /open-accounts/{id}/status")
    class CambioDeEstado {

        @Test
        @DisplayName("cierra la cuenta y responde 200")
        void cierra_y_responde_200() throws Exception {
            when(changeStatusUseCase.execute(any())).thenReturn(cerrada());

            mockMvc.perform(patch("/open-accounts/100/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"CLOSE","documentType":"FE_VENTA","finalConsumer":true,
                             "expectedVersion":1}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSE"));

            // El employeeId sale de Authz, nunca del cuerpo: en caja el responsable del
            // cierre es el contexto de auth, no un dato que el cliente pueda suplantar.
            verify(changeStatusUseCase).execute(new ChangeOpenAccountStatusCommand(100L, "CLOSE",
                    EMPLOYEE_ID, null, COMPANY_ID, "FE_VENTA", true, 1L));
        }

        @Test
        @DisplayName("una transicion invalida responde 409, no 500")
        void transicion_invalida_responde_409() throws Exception {
            when(changeStatusUseCase.execute(any())).thenThrow(
                    new InvalidOpenAccountStatusTransitionException(OpenAccountStatus.CLOSE,
                            OpenAccountStatus.CLOSE));

            mockMvc.perform(patch("/open-accounts/100/status")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"status":"CLOSE","finalConsumer":false}
                            """)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("sin status responde 400 y no cambia nada")
        void sin_status_responde_400() throws Exception {
            mockMvc.perform(patch("/open-accounts/100/status")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(changeStatusUseCase, never()).execute(any());
        }
    }
}
