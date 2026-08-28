package com.vetsoftware.app.paymentrefund.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand;
import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.ListAllPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.RegisterPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.domain.RefundExceedsPaymentAmountException;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
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
 * Rodaja web del camino de <b>escritura</b>, que en este bloque es solo de
 * plataforma: sacar plata de la caja de VetSoftware es tesoreria de la
 * plataforma y exige firma, y una firma que pudiera poner el propio
 * beneficiario no es una firma.
 *
 * <p>
 * Lo que congela esta clase y no ve ningun test de servicio: <b>que los doce
 * campos del cuerpo llegan al {@code RegisterPaymentRefundCommand} en su
 * posicion</b>. El command es un {@code record} de doce componentes, cinco de
 * ellos {@code Long} y tres fechas; cruzar {@code sourceDocumentId} con
 * {@code authorizedBySystemUserId}, o {@code refundedAt} con {@code valueDate},
 * compila sin una queja y solo se descubre cuadrando la caja. Por eso el caso
 * feliz captura el command y compara componente a componente con valores todos
 * distintos entre si.
 */
@WebMvcTest(SystemPaymentRefundController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemPaymentRefundController — contrato HTTP de plataforma")
class SystemPaymentRefundControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterPaymentRefundUseCase registerUseCase;
    @MockitoBean
    private ListAllPaymentRefundsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("responde 201 y traslada los doce campos del cuerpo al command sin cruzarlos")
        void responde_201_y_traslada_los_doce_campos_sin_cruzarlos() throws Exception {
            when(registerUseCase.execute(any())).thenReturn(unaDevolucion());

            mockMvc.perform(post("/system/payment-refunds").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "paymentId": 8100,
                              "sourceDocumentId": 6200,
                              "amount": 217345.61,
                              "method": "BANK_TRANSFER",
                              "destinationReference": "CTA-AHORROS-0099",
                              "refundedAt": "2026-03-05T14:30:15",
                              "valueDate": "2026-03-09",
                              "reasonCode": "BILLING_ERROR",
                              "reason": "Cobro duplicado de febrero",
                              "authorizedBySystemUserId": 990,
                              "clientRequestId": "req-0001"
                            }
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.amount").value(217345.61))
                    .andExpect(jsonPath("$.method").value("BANK_TRANSFER"));

            ArgumentCaptor<RegisterPaymentRefundCommand> command = ArgumentCaptor
                    .forClass(RegisterPaymentRefundCommand.class);
            verify(registerUseCase).execute(command.capture());
            assertThat(command.getValue()).satisfies(cmd -> {
                assertThat(cmd.companyId()).isEqualTo(900L);
                assertThat(cmd.paymentId()).isEqualTo(8100L);
                assertThat(cmd.sourceDocumentId()).isEqualTo(6200L);
                assertThat(cmd.amount()).isEqualByComparingTo("217345.61");
                assertThat(cmd.method()).isEqualTo(RefundMethod.BANK_TRANSFER);
                assertThat(cmd.destinationReference()).isEqualTo("CTA-AHORROS-0099");
                assertThat(cmd.refundedAt()).isEqualTo(LocalDateTime.of(2026, 3, 5, 14, 30, 15));
                assertThat(cmd.valueDate()).isEqualTo(LocalDate.of(2026, 3, 9));
                assertThat(cmd.reasonCode()).isEqualTo(RefundReasonCode.BILLING_ERROR);
                assertThat(cmd.reason()).isEqualTo("Cobro duplicado de febrero");
                assertThat(cmd.authorizedBySystemUserId()).isEqualTo(990L);
                assertThat(cmd.clientRequestId()).isEqualTo("req-0001");
            });
        }

        @Test
        @DisplayName("un cuerpo sin motivo ni firma sale 400 con los dos campos nombrados")
        void un_cuerpo_sin_motivo_ni_firma_sale_400() throws Exception {
            // El @Valid del @RequestBody es lo unico que dispara el validador; sin el,
            // el @NotBlank del DTO esta escrito y no se evalua nunca (#135). Este caso
            // se pone rojo el dia que alguien lo quite.
            mockMvc.perform(post("/system/payment-refunds").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "paymentId": 8100,
                              "amount": 217345.61,
                              "method": "BANK_TRANSFER",
                              "destinationReference": "CTA-AHORROS-0099",
                              "refundedAt": "2026-03-05T14:30:15",
                              "valueDate": "2026-03-09",
                              "reasonCode": "BILLING_ERROR",
                              "reason": "   "
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers
                            .containsInAnyOrder("reason", "authorizedBySystemUserId")));
        }

        @Test
        @DisplayName("un importe negativo sale 400 y no llega al caso de uso")
        void un_importe_negativo_sale_400_y_no_llega_al_caso_de_uso() throws Exception {
            mockMvc.perform(post("/system/payment-refunds").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "paymentId": 8100,
                              "amount": -1.00,
                              "method": "BANK_TRANSFER",
                              "destinationReference": "CTA-AHORROS-0099",
                              "refundedAt": "2026-03-05T14:30:15",
                              "valueDate": "2026-03-09",
                              "reasonCode": "BILLING_ERROR",
                              "reason": "Devolucion en negativo",
                              "authorizedBySystemUserId": 990
                            }
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));

            // La mitad del valor del caso: que la peticion invalida NO escribe.
            org.mockito.Mockito.verifyNoInteractions(registerUseCase);
        }

        @Test
        @DisplayName("pasarse del pago sale 409, no 500, y con el codigo que el front distingue")
        void pasarse_del_pago_sale_409() throws Exception {
            when(registerUseCase.execute(any())).thenThrow(
                    new RefundExceedsPaymentAmountException(8100L, new BigDecimal("500000.00"),
                            new BigDecimal("400000.00"), new BigDecimal("100001.00")));

            // Es un conflicto: el cuerpo es valido y lo que falla es el estado del
            // pago en este instante. Un 400 aqui le diria al operador que corrija un
            // campo que esta bien.
            mockMvc.perform(post("/system/payment-refunds").param("companyId", "900")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "paymentId": 8100,
                              "amount": 100001.00,
                              "method": "CARD",
                              "destinationReference": "TARJ-D",
                              "refundedAt": "2026-03-05T14:30:15",
                              "valueDate": "2026-03-09",
                              "reasonCode": "WITHDRAWAL",
                              "reason": "Se pasa por un centavo",
                              "authorizedBySystemUserId": 990
                            }
                            """)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_PAYMENT_AMOUNT"))
                    .andExpect(jsonPath("$.detail")
                            .value(org.hamcrest.Matchers.containsString("already refunded")));
        }
    }

    @Nested
    @DisplayName("Barrido de plataforma")
    class BarridoDePlataforma {

        @Test
        @DisplayName("sin companyId barre todas las empresas y lo dice pasando null")
        void sin_company_id_barre_todas_las_empresas() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unaDevolucion()), 0, 20, 1L));

            mockMvc.perform(get("/system/payment-refunds")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].companyId").value(900))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // null y no 0L: un 0 filtraria por una empresa inexistente y el barrido
            // saldria vacio sin que nadie lo notara.
            verify(listUseCase).listAll(null, 0, 20);
        }

        @Test
        @DisplayName("con companyId acota el barrido a esa empresa")
        void con_company_id_acota_el_barrido() throws Exception {
            when(listUseCase.listAll(any(), anyInt(), anyInt())).thenReturn(PageResult.empty(3, 7));

            mockMvc.perform(get("/system/payment-refunds").param("companyId", "901")
                    .param("page", "3").param("pageSize", "7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.pageSize").value(7));

            verify(listUseCase).listAll(901L, 3, 7);
        }
    }

    private static PaymentRefundDto unaDevolucion() {
        return new PaymentRefundDto(41L, 900L, 8100L, 6200L, new BigDecimal("217345.61"),
                RefundMethod.BANK_TRANSFER, "CTA-AHORROS-0099",
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDate.of(2026, 3, 9),
                RefundReasonCode.BILLING_ERROR, "Cobro duplicado de febrero", 990L,
                LocalDateTime.of(2026, 3, 7, 8, 45, 0));
    }
}
