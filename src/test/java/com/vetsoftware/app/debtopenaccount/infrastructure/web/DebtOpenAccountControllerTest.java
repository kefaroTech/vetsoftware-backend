package com.vetsoftware.app.debtopenaccount.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.CreateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.VoidDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
 * Rodaja HTTP del controller de abonos (BE-10): rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * <b>Lo que aqui NO se prueba.</b> La autorizacion vive en el
 * {@code @PreAuthorize} de cada puerto de entrada y en {@code @WebMvcTest} los
 * puertos estan mockeados, asi que el gate no se ejercita. Esa red la ponen
 * ArchUnit y la auditoria de autorizacion.
 */
@WebMvcTest(DebtOpenAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DebtOpenAccountController — contrato HTTP")
class DebtOpenAccountControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long PAYMENT_ID = 100L;
    private static final Long OPEN_ACCOUNT_ID = 50L;
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final LocalDateTime ANULADO = LocalDateTime.of(2026, 2, 1, 8, 0);

    private static final String CREAR_JSON = """
            {"amount":30000,"paymentMethod":"CASH","openAccountId":50,
             "clientRequestId":"8f14e45f-ea01-4d0a-9c1a-000000000001","expectedVersion":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateDebtOpenAccountUseCase createUseCase;
    @MockitoBean
    private ListDebtOpenAccountsUseCase listUseCase;
    @MockitoBean
    private ListDebtOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    @MockitoBean
    private VoidDebtOpenAccountUseCase voidUseCase;

    /**
     * {@code WebMvcSliceConfig} solo stubea {@code currentEmployeeIdOrNull()}. El
     * controller sella la autoria de create/void con {@code currentEmployeeId()},
     * asi que sin este stub el command viajaria con el empleado en null.
     */
    @BeforeEach
    void resolverElEmpleadoDesdeElContexto() {
        when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    private static DebtOpenAccountDto abono() {
        return abono(null);
    }

    private static DebtOpenAccountDto abonoAnulado() {
        return new DebtOpenAccountDto(PAYMENT_ID, new BigDecimal("30000"), PaymentMethod.CASH,
                new OpenAccountSummaryDto(OPEN_ACCOUNT_ID, COMPANY_ID),
                new EmployeeSummaryDto(EMPLOYEE_ID, "Ana Ruiz"), CREADO, true, true,
                new EmployeeSummaryDto(8L, "Luis Paz"), ANULADO, "Cobrado por error");
    }

    private static DebtOpenAccountDto abono(BigDecimal monto) {
        return new DebtOpenAccountDto(PAYMENT_ID, monto == null ? new BigDecimal("30000") : monto,
                PaymentMethod.CASH, new OpenAccountSummaryDto(OPEN_ACCOUNT_ID, COMPANY_ID),
                new EmployeeSummaryDto(EMPLOYEE_ID, "Ana Ruiz"), CREADO, true, false, null, null,
                null);
    }

    @Nested
    @DisplayName("POST /debt-open-accounts")
    class Crear {

        @Test
        @DisplayName("responde 201 con el abono creado y sus sumarios anidados")
        void responde_201_con_el_abono_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(abono());

            mockMvc.perform(post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.amount").value(30000))
                    .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                    .andExpect(jsonPath("$.openAccount.id").value(50))
                    .andExpect(jsonPath("$.openAccount.companyId").value(9))
                    .andExpect(jsonPath("$.createdBy.id").value(4))
                    .andExpect(jsonPath("$.createdBy.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.voided").value(false))
                    .andExpect(jsonPath("$.voidedBy").doesNotExist());
        }

        @Test
        @DisplayName("traduce el request al command con la company y el empleado del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(abono());

            mockMvc.perform(post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated());

            // Ni la company ni el empleado que cobra viajan crudos desde el cliente: salen
            // del AuthContext, no del cuerpo del request.
            verify(createUseCase).execute(new CreateDebtOpenAccountCommand(new BigDecimal("30000"),
                    "CASH", OPEN_ACCOUNT_ID, COMPANY_ID, EMPLOYEE_ID,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001", 3L));
        }

        @Test
        @DisplayName("sin clientRequestId ni expectedVersion el command viaja con ambos en null")
        void sin_clave_ni_version_el_command_viaja_con_nulls() throws Exception {
            when(createUseCase.execute(any())).thenReturn(abono());

            mockMvc.perform(
                    post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":30000,"paymentMethod":"CASH","openAccountId":50}
                            """)).andExpect(status().isCreated());

            verify(createUseCase).execute(new CreateDebtOpenAccountCommand(new BigDecimal("30000"),
                    "CASH", OPEN_ACCOUNT_ID, COMPANY_ID, EMPLOYEE_ID, null, null));
        }

        @Test
        @DisplayName("con amount negativo o cero responde 400 y no llega al caso de uso")
        void amount_no_positivo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":0,"paymentMethod":"CASH","openAccountId":50}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin paymentMethod responde 400")
        void sin_payment_method_responde_400() throws Exception {
            mockMvc.perform(
                    post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":30000,"openAccountId":50}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin openAccountId responde 400")
        void sin_open_account_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":30000,"paymentMethod":"CASH"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con clientRequestId mas largo que 36 caracteres responde 400")
        void clave_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(
                    post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":30000,"paymentMethod":"CASH","openAccountId":50,
                             "clientRequestId":"0123456789012345678901234567890123456789"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("OpenAccount not found: 50"));

            mockMvc.perform(post("/debt-open-accounts").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /debt-open-accounts")
    class Listado {

        @Test
        @DisplayName("arma la pagina con la company del contexto y los metadatos de la consulta")
        void arma_la_pagina_con_la_company_del_contexto() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(PageResult.of(List.of(abono()), 0, 20, 1L));

            mockMvc.perform(get("/debt-open-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("respeta la pagina y el tamano enviados por parametro")
        void respeta_la_pagina_y_el_tamano_enviados() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 2, 5))
                    .thenReturn(PageResult.of(List.of(), 2, 5, 0L));

            mockMvc.perform(get("/debt-open-accounts").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5));

            verify(listUseCase).listAll(COMPANY_ID, 2, 5);
        }
    }

    @Nested
    @DisplayName("GET /debt-open-accounts/by-open-account/{openAccountId}")
    class ListarPorCuenta {

        @Test
        @DisplayName("devuelve los abonos de la cuenta acotados a la company del contexto")
        void devuelve_los_abonos_de_la_cuenta() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(abono()));

            mockMvc.perform(get("/debt-open-accounts/by-open-account/50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100));

            verify(listByOpenAccountUseCase).listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("una cuenta sin abonos responde 200 con un arreglo vacio")
        void cuenta_sin_abonos_responde_200_vacio() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get("/debt-open-accounts/by-open-account/50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("PATCH /debt-open-accounts/{id}/void")
    class Anular {

        @Test
        @DisplayName("responde 200 con quien anulo, cuando y el motivo")
        void responde_200_con_quien_anulo_cuando_y_el_motivo() throws Exception {
            when(voidUseCase.execute(any())).thenReturn(abonoAnulado());

            mockMvc.perform(patch("/debt-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error","expectedVersion":3}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.voided").value(true))
                    .andExpect(jsonPath("$.voidedBy.id").value(8))
                    .andExpect(jsonPath("$.voidedBy.name").value("Luis Paz"))
                    .andExpect(jsonPath("$.voidReason").value("Cobrado por error"));
        }

        @Test
        @DisplayName("arma el command con la company, el empleado que anula y el motivo")
        void arma_el_command_con_la_company_y_el_empleado() throws Exception {
            when(voidUseCase.execute(any())).thenReturn(abonoAnulado());

            mockMvc.perform(patch("/debt-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error","expectedVersion":3}
                            """)).andExpect(status().isOk());

            verify(voidUseCase).execute(new VoidDebtOpenAccountCommand(PAYMENT_ID, COMPANY_ID,
                    EMPLOYEE_ID, "Cobrado por error", 3L));
        }

        @Test
        @DisplayName("sin motivo responde 400 y no llega al caso de uso")
        void sin_motivo_responde_400() throws Exception {
            mockMvc.perform(patch("/debt-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(voidUseCase);
        }

        @Test
        @DisplayName("un abono ya anulado sale como 409 con codigo propio")
        void un_abono_ya_anulado_sale_como_409() throws Exception {
            when(voidUseCase.execute(any()))
                    .thenThrow(new DebtOpenAccountAlreadyVoidedException(PAYMENT_ID));

            mockMvc.perform(patch("/debt-open-accounts/100/void")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error"}
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DEBT_OPEN_ACCOUNT_ALREADY_VOIDED"));
        }
    }
}
