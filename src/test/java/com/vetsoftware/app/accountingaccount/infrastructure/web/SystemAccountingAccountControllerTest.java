package com.vetsoftware.app.accountingaccount.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.accountingaccount.application.command.CreateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.CloseAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.CreateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.FindAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.ListAccountingAccountsUseCase;
import com.vetsoftware.app.accountingaccount.application.port.in.UpdateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
 * Rodaja web del plan de cuentas.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio:
 *
 * <ul>
 * <li><b>Los nueve campos del cuerpo llegan al command en su posicion.</b> Dos
 * son booleanos consecutivos —{@code postable} y {@code requiresThirdParty}— y
 * cruzarlos compila sin una queja; el resultado seria una cuenta que admite
 * asiento cuando no debe, que es como el balance de prueba deja de cuadrar por
 * arrastre.</li>
 * <li><b>El cuerpo se valida.</b> Sin {@code @Valid} delante del
 * {@code @RequestBody}, el binder no dispara el validador y los
 * {@code @NotBlank} del request estan escritos sin evaluarse nunca.</li>
 * </ul>
 *
 * <p>
 * <b>Lo que esta clase NO cubre todavia:</b> el 404 de
 * {@code AccountingAccountNotFoundException} y el 409 de
 * {@code AccountingAccountAlreadyClosedException}.
 * {@code GlobalExceptionHandler} aun no las enumera —son excepciones nuevas— y
 * hoy saldrian como 500. Cuando se cableen, aqui van sus dos casos.
 */
@WebMvcTest(SystemAccountingAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemAccountingAccountController — contrato HTTP de plataforma")
class SystemAccountingAccountControllerTest {

    private static final AccountingAccountDto BANCOS = new AccountingAccountDto(8400L, "11050501",
            "Bancos", AccountClass.ASSET, "1", 6, true, false, LocalDate.of(2026, 1, 1), null,
            LocalDateTime.of(2026, 1, 1, 8, 0), true);

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateAccountingAccountUseCase createUseCase;
    @MockitoBean
    private UpdateAccountingAccountUseCase updateUseCase;
    @MockitoBean
    private CloseAccountingAccountUseCase closeUseCase;
    @MockitoBean
    private FindAccountingAccountUseCase findUseCase;
    @MockitoBean
    private ListAccountingAccountsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("responde 201 y traslada los nueve campos del cuerpo sin cruzarlos")
        void responde_201_y_traslada_los_nueve_campos_sin_cruzarlos() throws Exception {
            when(createUseCase.execute(any())).thenReturn(BANCOS);

            mockMvc.perform(post("/system/accounting-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "code": "11050501",
                              "name": "Bancos",
                              "accountClass": "ASSET",
                              "parentCode": "1",
                              "accountLevel": 6,
                              "postable": true,
                              "requiresThirdParty": false,
                              "validFrom": "2026-01-01",
                              "validTo": null
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(8400))
                    .andExpect(jsonPath("$.accountLevel").value(6))
                    .andExpect(jsonPath("$.postable").value(true));

            ArgumentCaptor<CreateAccountingAccountCommand> command = ArgumentCaptor
                    .forClass(CreateAccountingAccountCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.code()).isEqualTo("11050501");
                assertThat(cmd.name()).isEqualTo("Bancos");
                assertThat(cmd.accountClass()).isEqualTo(AccountClass.ASSET);
                assertThat(cmd.parentCode()).isEqualTo("1");
                assertThat(cmd.accountLevel()).isEqualTo(6);
                // Los dos booleanos, sin cruzar: es el error que compila.
                assertThat(cmd.postable()).isTrue();
                assertThat(cmd.requiresThirdParty()).isFalse();
                assertThat(cmd.validFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(cmd.validTo()).isNull();
            });
        }

        @Test
        @DisplayName("un cuerpo sin codigo responde 400 y no llega al caso de uso")
        void un_cuerpo_sin_codigo_responde_400() throws Exception {
            mockMvc.perform(post("/system/accounting-accounts")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "code": "  ",
                              "name": "Bancos",
                              "accountClass": "ASSET",
                              "accountLevel": 1,
                              "postable": false,
                              "requiresThirdParty": false,
                              "validFrom": "2026-01-01"
                            }
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("la busqueda por codigo usa su propio segmento y no compite con /{id}")
        void la_busqueda_por_codigo_usa_su_propio_segmento() throws Exception {
            when(findUseCase.findByCode("11050501")).thenReturn(BANCOS);

            mockMvc.perform(get("/system/accounting-accounts/by-code/11050501"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("11050501"));

            verify(findUseCase).findByCode("11050501");
        }

        @Test
        @DisplayName("el listado sale con la forma de pagina que el contrato promete")
        void el_listado_sale_con_la_forma_de_pagina() throws Exception {
            when(listUseCase.listAll(0, 20)).thenReturn(PageResult.of(List.of(BANCOS), 0, 20, 1L));

            mockMvc.perform(get("/system/accounting-accounts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].code").value("11050501"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0));
        }
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("cerrar es un PATCH sobre una fila que se queda, no un DELETE")
        void cerrar_es_un_patch_sobre_una_fila_que_se_queda() throws Exception {
            when(closeUseCase.execute(any())).thenReturn(BANCOS);

            mockMvc.perform(patch("/system/accounting-accounts/8400/close")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"validTo\": \"2027-01-01\"}")).andExpect(status().isOk());

            verify(closeUseCase).execute(any());
        }

        @Test
        @DisplayName("cerrar sin fecha responde 400 y no llega al caso de uso")
        void cerrar_sin_fecha_responde_400() throws Exception {
            mockMvc.perform(patch("/system/accounting-accounts/8400/close")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"validTo\": null}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(closeUseCase);
        }
    }
}
