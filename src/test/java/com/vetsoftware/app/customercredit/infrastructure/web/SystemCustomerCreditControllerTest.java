package com.vetsoftware.app.customercredit.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.customercredit.application.command.ConsumeCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.command.ExpireCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ConsumeCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ExpireCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.GrantCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditBalancesUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.application.port.in.ListExpiringCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.customercredit.domain.InsufficientCustomerCreditException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
 * Rodaja web del camino de escritura del saldo a favor, que es solo de
 * plataforma.
 *
 * <p>
 * Lo especifico de este controller y lo que esta clase congela: <b>aplicar
 * saldo devuelve una LISTA de asientos, no uno</b>. Un cobro de 130.000 contra
 * dos lotes de 100.000 y 50.000 se parte en dos asientos, y el orden en que
 * salen <em>es</em> el orden de consumo —primero el que antes caduca—. Si
 * alguien cambiara el tipo de retorno a un solo asiento «porque casi siempre es
 * uno», la consola dejaria de ver de que lotes salio el dinero; el caso feliz
 * de {@link Aplicacion} afirma los dos elementos y su orden.
 *
 * <p>
 * Y que quedarse sin saldo es un <b>409</b>, no un 500 ni un 400: el cuerpo es
 * valido y lo que falla es el estado de la cuenta en este instante.
 */
@WebMvcTest(SystemCustomerCreditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemCustomerCreditController — contrato HTTP de plataforma")
class SystemCustomerCreditControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private GrantCustomerCreditUseCase grantUseCase;
    @MockitoBean
    private ConsumeCustomerCreditUseCase consumeUseCase;
    @MockitoBean
    private ExpireCustomerCreditUseCase expireUseCase;
    @MockitoBean
    private ListAllCustomerCreditEntriesUseCase listAllEntriesUseCase;
    @MockitoBean
    private ListAllCustomerCreditBalancesUseCase listAllBalancesUseCase;
    @MockitoBean
    private ListExpiringCustomerCreditUseCase listExpiringUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Abono")
    class Abono {

        @Test
        @DisplayName("responde 201 y traslada el origen y su referencia sin cruzarlos")
        void responde_201_y_traslada_el_origen_sin_cruzarlo() throws Exception {
            when(grantUseCase.execute(any())).thenReturn(unAbono());

            mockMvc.perform(post("/system/customer-credit/grants").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 83450.75,
                              "originKind": "CREDIT_NOTE",
                              "originDocumentId": 8300,
                              "expiresOn": "2026-12-31",
                              "clientRequestId": "abono-0001"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(30))
                    .andExpect(jsonPath("$.entryKind").value("GRANT"))
                    .andExpect(jsonPath("$.amount").value(83450.75))
                    .andExpect(jsonPath("$.expiresOn").value("2026-12-31"));

            // Tres huecos de origen y uno solo relleno: si el controller pusiera el
            // documento en originPaymentId, el dominio lo rechazaria en produccion y
            // el error saldria disfrazado. Aqui se ve antes.
            ArgumentCaptor<GrantCustomerCreditCommand> command = ArgumentCaptor
                    .forClass(GrantCustomerCreditCommand.class);
            verify(grantUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.amount()).isEqualByComparingTo("83450.75");
                assertThat(cmd.originKind()).isEqualTo(CreditOriginKind.CREDIT_NOTE);
                assertThat(cmd.originDocumentId()).isEqualTo(8300L);
                assertThat(cmd.originPaymentId()).isNull();
                assertThat(cmd.originSubscriptionId()).isNull();
                assertThat(cmd.expiresOn()).isEqualTo(LocalDate.of(2026, 12, 31));
                assertThat(cmd.clientRequestId()).isEqualTo("abono-0001");
            });
        }

        @Test
        @DisplayName("un abono sin llave de idempotencia sale 400 y no escribe")
        void un_abono_sin_llave_sale_400_y_no_escribe() throws Exception {
            // R13: toda peticion que mueve dinero lleva llave. Sin @Valid delante del
            // @RequestBody el @NotBlank esta escrito y no se evalua nunca (#135).
            mockMvc.perform(post("/system/customer-credit/grants").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 1000.00,
                              "originKind": "MANUAL"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("clientRequestId"));

            verifyNoInteractions(grantUseCase);
        }

        @Test
        @DisplayName("un abono de importe cero sale 400: un asiento de cero no dice nada")
        void un_abono_de_importe_cero_sale_400() throws Exception {
            mockMvc.perform(post("/system/customer-credit/grants").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 0,
                              "originKind": "MANUAL",
                              "clientRequestId": "abono-cero"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));

            verifyNoInteractions(grantUseCase);
        }
    }

    @Nested
    @DisplayName("Aplicacion")
    class Aplicacion {

        @Test
        @DisplayName("devuelve un asiento por lote y en el orden en que se consumieron")
        void devuelve_un_asiento_por_lote_y_en_orden() throws Exception {
            when(consumeUseCase.execute(any()))
                    .thenReturn(List.of(unConsumo(41L, 30L, new BigDecimal("-100000.00")),
                            unConsumo(42L, 33L, new BigDecimal("-30000.00"))));

            mockMvc.perform(post("/system/customer-credit/consumptions").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 130000.00,
                              "originDocumentId": 8300,
                              "clientRequestId": "aplicacion-0001"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    // Importes distintos y lotes distintos: si la lista saliera al
                    // reves, o si se colapsara en un solo asiento, esto cae.
                    .andExpect(jsonPath("$[0].lotEntryId").value(30))
                    .andExpect(jsonPath("$[0].amount").value(-100000.00))
                    .andExpect(jsonPath("$[1].lotEntryId").value(33))
                    .andExpect(jsonPath("$[1].amount").value(-30000.00));

            ArgumentCaptor<ConsumeCustomerCreditCommand> command = ArgumentCaptor
                    .forClass(ConsumeCustomerCreditCommand.class);
            verify(consumeUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                // El command lleva el importe en POSITIVO: el signo lo pone el
                // dominio al construir el asiento.
                assertThat(cmd.amount()).isEqualByComparingTo("130000.00");
                assertThat(cmd.originDocumentId()).isEqualTo(8300L);
                assertThat(cmd.clientRequestId()).isEqualTo("aplicacion-0001");
            });
        }

        @Test
        @DisplayName("quedarse sin saldo sale 409 con su codigo, no 400 ni 500")
        void quedarse_sin_saldo_sale_409() throws Exception {
            when(consumeUseCase.execute(any())).thenThrow(
                    new InsufficientCustomerCreditException(900L, new BigDecimal("130000.00")));

            mockMvc.perform(post("/system/customer-credit/consumptions").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 130000.00,
                              "originDocumentId": 8300,
                              "clientRequestId": "aplicacion-sin-saldo"
                            }
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_CUSTOMER_CREDIT"))
                    .andExpect(jsonPath("$.detail").value(
                            "Insufficient customer credit for company 900 to apply 130000.00"));
        }

        @Test
        @DisplayName("una llave con caracteres prohibidos sale 400: el separador de lote es reservado")
        void una_llave_con_caracteres_prohibidos_sale_400() throws Exception {
            // La llave de la operacion se concatena con '#' + indice para nombrar cada
            // asiento de lote. Dejar pasar un '#' del cliente le permitiria fabricar
            // la llave de un asiento que aun no existe y romper la deduplicacion.
            mockMvc.perform(post("/system/customer-credit/consumptions").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "amount": 1000.00,
                              "originDocumentId": 8300,
                              "clientRequestId": "llave#0"
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("clientRequestId"));

            verifyNoInteractions(consumeUseCase);
        }
    }

    @Nested
    @DisplayName("Caducidad y barridos")
    class CaducidadYBarridos {

        @Test
        @DisplayName("caducar devuelve los asientos escritos para esa empresa")
        void caducar_devuelve_los_asientos_escritos() throws Exception {
            when(expireUseCase.execute(any()))
                    .thenReturn(List.of(unConsumo(51L, 30L, new BigDecimal("-45000.00"))));

            mockMvc.perform(post("/system/customer-credit/expirations").param("companyId", "901"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].amount").value(-45000.00));

            ArgumentCaptor<ExpireCustomerCreditCommand> command = ArgumentCaptor
                    .forClass(ExpireCustomerCreditCommand.class);
            verify(expireUseCase).execute(command.capture());
            assertThat(command.getValue().companyId()).isEqualTo(901L);
        }

        @Test
        @DisplayName("el barrido de asientos sin companyId recorre todas las empresas")
        void el_barrido_de_asientos_sin_company_id_recorre_todas() throws Exception {
            when(listAllEntriesUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unAbono()), 0, 20, 1L));

            mockMvc.perform(get("/system/customer-credit/entries")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(30))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listAllEntriesUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("el barrido de saldos no acepta filtro de empresa: es cross-tenant por diseño")
        void el_barrido_de_saldos_no_acepta_filtro_de_empresa() throws Exception {
            when(listAllBalancesUseCase.listAll(anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unSaldo()), 0, 20, 1L));

            // El companyId de la query no existe como parametro de la ruta; si alguien
            // lo añadiera, este verify de dos argumentos dejaria de compilar.
            mockMvc.perform(get("/system/customer-credit/balances").param("companyId", "900"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].balanceAmount").value(340500.25));

            verify(listAllBalancesUseCase).listAll(0, 20);
        }

        @Test
        @DisplayName("el barrido de lo que caduca exige la fecha de corte y la traslada tal cual")
        void el_barrido_de_lo_que_caduca_exige_la_fecha_de_corte() throws Exception {
            when(listExpiringUseCase.listExpiring(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/system/customer-credit/expiring").param("before", "2026-06-01"))
                    .andExpect(status().isOk());

            verify(listExpiringUseCase).listExpiring(LocalDate.of(2026, 6, 1), 0, 20);
        }

        @Test
        @DisplayName("sin fecha de corte el barrido de lo que caduca sale 400")
        void sin_fecha_de_corte_el_barrido_sale_400() throws Exception {
            // Sin corte, «lo que caduca» seria todo el historico: mejor 400 que un
            // barrido de la tabla entera disfrazado de consulta.
            mockMvc.perform(get("/system/customer-credit/expiring"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listExpiringUseCase);
        }
    }

    private static CustomerCreditEntryDto unAbono() {
        return new CustomerCreditEntryDto(30L, 900L, CreditEntryKind.GRANT,
                new BigDecimal("83450.75"), null, CreditOriginKind.CREDIT_NOTE, null, 8300L, null,
                LocalDateTime.of(2026, 2, 14, 11, 22, 33), LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 12, 31), LocalDateTime.of(2026, 2, 15, 6, 5, 4));
    }

    private static CustomerCreditEntryDto unConsumo(Long id, Long lote, BigDecimal importe) {
        return new CustomerCreditEntryDto(id, 900L, CreditEntryKind.CONSUMPTION, importe, lote,
                CreditOriginKind.APPLICATION, null, 8300L, null,
                LocalDateTime.of(2026, 2, 14, 11, 22, 33), LocalDate.of(2026, 2, 20), null,
                LocalDateTime.of(2026, 2, 15, 6, 5, 4));
    }

    private static CustomerCreditBalanceDto unSaldo() {
        return new CustomerCreditBalanceDto(5L, 900L, new BigDecimal("340500.25"),
                LocalDate.of(2026, 6, 30), LocalDateTime.of(2026, 3, 15, 17, 42, 9), 12L);
    }
}
