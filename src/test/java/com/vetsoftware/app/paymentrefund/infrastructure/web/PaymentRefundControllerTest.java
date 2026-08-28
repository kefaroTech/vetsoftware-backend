package com.vetsoftware.app.paymentrefund.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.FindPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsByPaymentUseCase;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefundNotFoundException;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vetsoftware.app.testsupport.WebMvcSliceConfig;

/**
 * Rodaja web del camino de <b>lectura</b> del tenant.
 *
 * <p>
 * Las dos cosas que esta clase congela y que ningun test de servicio ve:
 *
 * <ul>
 * <li><b>El {@code companyId} lo pone el contexto de autorizacion, no el
 * cliente.</b> Ninguno de los tres endpoints acepta una empresa por parametro;
 * si alguien añadiera un {@code @RequestParam Long companyId} «para la
 * consola», el {@code verify} con el valor exacto de
 * {@code authz.currentCompanyId()} lo caza.</li>
 * <li><b>La respuesta no lleva la llave de idempotencia.</b>
 * {@code PaymentRefundDto} la omite a proposito: publicarla dejaria que un
 * lector adivinara las llaves de otros y colisionara con ellas.</li>
 * </ul>
 */
@WebMvcTest(PaymentRefundController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PaymentRefundController — contrato HTTP del tenant")
class PaymentRefundControllerTest {

    /**
     * Distinto del {@code COMPANY_ID} por defecto para que se vea de donde sale.
     */
    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindPaymentRefundUseCase findUseCase;
    @MockitoBean
    private ListPaymentRefundsUseCase listUseCase;
    @MockitoBean
    private ListPaymentRefundsByPaymentUseCase listByPaymentUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Consulta por id")
    class ConsultaPorId {

        @Test
        @DisplayName("devuelve la devolucion con cada campo en su lugar del JSON")
        void devuelve_la_devolucion_con_cada_campo_en_su_lugar() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN)).thenReturn(unaDevolucion());

            mockMvc.perform(get("/payment-refunds/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(41))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.paymentId").value(8100))
                    .andExpect(jsonPath("$.sourceDocumentId").value(6200))
                    .andExpect(jsonPath("$.amount").value(217345.61))
                    .andExpect(jsonPath("$.method").value("BANK_TRANSFER"))
                    .andExpect(jsonPath("$.destinationReference").value("CTA-AHORROS-0099"))
                    .andExpect(jsonPath("$.refundedAt").value("2026-03-05T14:30:15"))
                    .andExpect(jsonPath("$.valueDate").value("2026-03-09"))
                    .andExpect(jsonPath("$.reasonCode").value("BILLING_ERROR"))
                    .andExpect(jsonPath("$.reason").value("Cobro duplicado de febrero"))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-07T08:45:00"));
        }

        @Test
        @DisplayName("no expone el operador de plataforma que autorizo la devolucion")
        void no_expone_el_operador_que_autorizo() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN)).thenReturn(unaDevolucion());

            // authorizedBySystemUserId es el id interno del operador de VetSoftware:
            // entero pequeño y enumerable, servido al tenant permite mapear la
            // plantilla interna y correlacionar que operador atiende a que clinica. El
            // corte vive en el tipo de la frontera —PaymentRefundResponse no declara el
            // campo— y no en un if del controller, que alguien puede olvidar. Lo
            // publica SystemPaymentRefundResponse, que solo sale por /system/**.
            mockMvc.perform(get("/payment-refunds/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorizedBySystemUserId").doesNotExist());
        }

        @Test
        @DisplayName("no expone la llave de idempotencia de la devolucion")
        void no_expone_la_llave_de_idempotencia() throws Exception {
            when(findUseCase.findById(41L, EMPRESA_DEL_TOKEN)).thenReturn(unaDevolucion());

            // El dia que alguien añada clientRequestId al DTO o a la respuesta «por
            // comodidad», este caso se pone rojo. Es toda su razon de ser.
            mockMvc.perform(get("/payment-refunds/{id}", 41L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientRequestId").doesNotExist());
        }

        @Test
        @DisplayName("acota la carga con la empresa del token y nunca con una del cliente")
        void acota_la_carga_con_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(unaDevolucion());

            // El parametro companyId que se cuela en la query string no debe llegar
            // al caso de uso: la ruta no lo declara y la empresa sale del token.
            mockMvc.perform(get("/payment-refunds/{id}", 41L).param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(41L, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("una devolucion inexistente sale como 404 con su codigo de error")
        void una_devolucion_inexistente_sale_como_404() throws Exception {
            when(findUseCase.findById(404L, EMPRESA_DEL_TOKEN))
                    .thenThrow(new PaymentRefundNotFoundException(404L));

            mockMvc.perform(get("/payment-refunds/{id}", 404L)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_REFUND_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value("Payment refund not found: 404"));
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el listado de la empresa devuelve la pagina con su contenido y sus totales")
        void el_listado_de_la_empresa_devuelve_la_pagina() throws Exception {
            when(listUseCase.listByCompany(EMPRESA_DEL_TOKEN, 2, 5))
                    .thenReturn(PageResult.of(List.of(unaDevolucion()), 2, 5, 11L));

            mockMvc.perform(get("/payment-refunds").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(41))
                    .andExpect(jsonPath("$.content[0].amount").value(217345.61))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros pagina desde la primera y de veinte en veinte")
        void sin_parametros_pagina_desde_la_primera() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/payment-refunds")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(EMPRESA_DEL_TOKEN, 0, 20);
        }

        @Test
        @DisplayName("el listado por pago lleva el pago de la ruta y la empresa del token")
        void el_listado_por_pago_lleva_el_pago_y_la_empresa() throws Exception {
            when(listByPaymentUseCase.listByPaymentAndCompany(anyLong(), anyLong(), anyInt(),
                    anyInt())).thenReturn(PageResult.of(List.of(unaDevolucion()), 0, 20, 1L));

            mockMvc.perform(get("/payment-refunds/by-payment/{paymentId}", 8100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].paymentId").value(8100));

            // Acotar por paymentId no basta: el pago es de alguien. Los dos
            // argumentos exactos, en su orden, o el caso cae.
            ArgumentCaptor<Long> pago = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(listByPaymentUseCase).listByPaymentAndCompany(pago.capture(), empresa.capture(),
                    eq(0), eq(20));
            assertThat(pago.getValue()).isEqualTo(8100L);
            assertThat(empresa.getValue()).isEqualTo(EMPRESA_DEL_TOKEN);
        }
    }

    private static PaymentRefundDto unaDevolucion() {
        return new PaymentRefundDto(41L, EMPRESA_DEL_TOKEN, 8100L, 6200L,
                new BigDecimal("217345.61"), RefundMethod.BANK_TRANSFER, "CTA-AHORROS-0099",
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDate.of(2026, 3, 9),
                RefundReasonCode.BILLING_ERROR, "Cobro duplicado de febrero", 990L,
                LocalDateTime.of(2026, 3, 7, 8, 45, 0));
    }
}
