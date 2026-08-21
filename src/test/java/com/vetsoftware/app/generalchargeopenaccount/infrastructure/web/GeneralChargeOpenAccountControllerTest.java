package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.CreateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.VoidGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del cargo libre: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON (incluidos los companion {@code *Summary}, que
 * {@code toResponse} construye a mano). Lo que hay debajo son dobles.
 *
 * <p>
 * Lo que esta rodaja protege y ninguna otra capa cubre: que ni
 * {@code companyId} ni {@code createdById}/{@code voidedById} viajan en el
 * cuerpo (los pone {@code Authz}).
 */
@WebMvcTest(GeneralChargeOpenAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("GeneralChargeOpenAccountController — contrato HTTP")
class GeneralChargeOpenAccountControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long CHARGE_ID = GeneralChargeOpenAccountMother.CHARGE_ID;
    private static final Long OPEN_ACCOUNT_ID = GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateGeneralChargeOpenAccountUseCase createUseCase;
    @MockitoBean
    private ListGeneralChargeOpenAccountsUseCase listUseCase;
    @MockitoBean
    private ListGeneralChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    @MockitoBean
    private VoidGeneralChargeOpenAccountUseCase voidUseCase;

    /**
     * {@code WebMvcSliceConfig} solo stubea {@code currentEmployeeIdOrNull()}; este
     * controller sella autoria con {@code currentEmployeeId()} (create y void), asi
     * que sin este stub Mockito devolveria 0L y el command quedaria firmado por un
     * empleado que no existe.
     */
    @BeforeEach
    void resolverContexto() {
        when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    private static GeneralChargeOpenAccountDto cargo() {
        return GeneralChargeOpenAccountDto.from(GeneralChargeOpenAccountMother.cargo());
    }

    private static GeneralChargeOpenAccountDto cargoAnulado() {
        return GeneralChargeOpenAccountDto.from(GeneralChargeOpenAccountMother.cargoAnulado());
    }

    @Nested
    @DisplayName("POST /general-charge-open-accounts")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el cargo creado, incluido el impuesto")
        void responde_201_con_el_cargo_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargo());

            mockMvc.perform(post("/general-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Traslado en ambulancia","unitAmount":5950.00,
                             "quantity":2,"taxId":4,"openAccountId":50}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CHARGE_ID))
                    .andExpect(jsonPath("$.name").value("Traslado en ambulancia"))
                    .andExpect(jsonPath("$.hasTax").value(true))
                    .andExpect(jsonPath("$.tax.id").value(4))
                    .andExpect(jsonPath("$.tax.name").value("IVA 19%"))
                    .andExpect(jsonPath("$.openAccount.id").value(OPEN_ACCOUNT_ID))
                    .andExpect(jsonPath("$.createdBy.id").value(7))
                    .andExpect(jsonPath("$.voided").value(false))
                    // sin anular: v == null → voidedBy no debe venir poblado.
                    .andExpect(jsonPath("$.voidedBy").value(nullValue()));
        }

        @Test
        @DisplayName("la empresa y el empleado los pone el backend, no el request")
        void el_command_lo_sella_el_backend() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cargo());

            mockMvc.perform(post("/general-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Traslado en ambulancia","unitAmount":5950.00,
                             "quantity":2,"taxId":4,"openAccountId":50,
                             "clientRequestId":"req-1","expectedVersion":3}
                            """));

            ArgumentCaptor<CreateGeneralChargeOpenAccountCommand> captor = ArgumentCaptor
                    .forClass(CreateGeneralChargeOpenAccountCommand.class);
            verify(createUseCase).execute(captor.capture());
            CreateGeneralChargeOpenAccountCommand command = captor.getValue();
            assertThat(command.name()).isEqualTo("Traslado en ambulancia");
            assertThat(command.unitAmount()).isEqualByComparingTo("5950.00");
            assertThat(command.quantity()).isEqualByComparingTo("2");
            assertThat(command.taxId()).isEqualTo(4L);
            assertThat(command.openAccountId()).isEqualTo(50L);
            // Ni companyId ni createdById viajan en el cuerpo: los resuelve Authz.
            assertThat(command.companyId()).isEqualTo(COMPANY_ID);
            assertThat(command.createdById()).isEqualTo(EMPLOYEE_ID);
            assertThat(command.clientRequestId()).isEqualTo("req-1");
            assertThat(command.expectedVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("sin name responde 400 y no crea nada")
        void sin_name_responde_400() throws Exception {
            mockMvc.perform(post("/general-charge-open-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"unitAmount":5950.00,"quantity":2,"openAccountId":50}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /general-charge-open-accounts")
    class Listado {

        @Test
        @DisplayName("responde con la pagina de cargos de la empresa")
        void responde_con_la_pagina_de_la_empresa() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(cargo()), 0, 20, 1L, 1));

            mockMvc.perform(get("/general-charge-open-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(CHARGE_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("traslada los parametros de pagina al caso de uso")
        void traslada_los_parametros_de_pagina() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 1, 5))
                    .thenReturn(new PageResult<>(List.of(), 1, 5, 0L, 0));

            mockMvc.perform(
                    get("/general-charge-open-accounts").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk());

            verify(listUseCase).listAll(COMPANY_ID, 1, 5);
        }
    }

    @Nested
    @DisplayName("GET /general-charge-open-accounts/by-open-account/{openAccountId}")
    class ListadoPorCuenta {

        @Test
        @DisplayName("responde con los cargos de esa cuenta, sin paginar")
        void responde_con_los_cargos_de_esa_cuenta() throws Exception {
            when(listByOpenAccountUseCase.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(List.of(cargo()));

            mockMvc.perform(
                    get("/general-charge-open-accounts/by-open-account/{id}", OPEN_ACCOUNT_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(CHARGE_ID));
        }
    }

    @Nested
    @DisplayName("PATCH /general-charge-open-accounts/{id}/void")
    class Anulacion {

        @Test
        @DisplayName("anula el cargo y responde 200 con quien, cuando y por que")
        void anula_y_responde_200() throws Exception {
            when(voidUseCase.execute(any())).thenReturn(cargoAnulado());

            mockMvc.perform(patch("/general-charge-open-accounts/{id}/void", CHARGE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.voided").value(true))
                    // anulado: v != null → voidedBy si viene poblado.
                    .andExpect(jsonPath("$.voidedBy.id").value(8))
                    .andExpect(jsonPath("$.voidedBy.name").value("Luis Paz"))
                    .andExpect(jsonPath("$.voidReason").value("Cobrado por error"));
        }

        @Test
        @DisplayName("el empleado que anula lo pone el backend, no el request")
        void el_command_lo_sella_el_backend() throws Exception {
            when(voidUseCase.execute(any())).thenReturn(cargoAnulado());

            mockMvc.perform(patch("/general-charge-open-accounts/{id}/void", CHARGE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason":"Cobrado por error","expectedVersion":5}
                            """));

            ArgumentCaptor<VoidGeneralChargeOpenAccountCommand> captor = ArgumentCaptor
                    .forClass(VoidGeneralChargeOpenAccountCommand.class);
            verify(voidUseCase).execute(captor.capture());
            VoidGeneralChargeOpenAccountCommand command = captor.getValue();
            assertThat(command.id()).isEqualTo(CHARGE_ID);
            assertThat(command.companyId()).isEqualTo(COMPANY_ID);
            assertThat(command.voidedById()).isEqualTo(EMPLOYEE_ID);
            assertThat(command.reason()).isEqualTo("Cobrado por error");
            assertThat(command.expectedVersion()).isEqualTo(5L);
        }

        @Test
        @DisplayName("sin motivo responde 400 y no anula nada")
        void sin_motivo_responde_400() throws Exception {
            mockMvc.perform(patch("/general-charge-open-accounts/{id}/void", CHARGE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(voidUseCase, never()).execute(any());
        }
    }
}
