package com.vetsoftware.app.paymentattempt.infrastructure.web;

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
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.FindPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsByDocumentUseCase;
import com.vetsoftware.app.paymentattempt.application.port.in.ListPaymentAttemptsUseCase;
import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttemptNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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

/**
 * Rodaja web del tenant, y <b>la unica red del secreto de esta feature</b>.
 *
 * <p>
 * El codigo crudo que devuelve la pasarela se guarda —hace falta para revisar
 * despues la traduccion, porque las pasarelas cambian su catalogo y una
 * traduccion hecha hoy envejece— y <b>no se le enseña al cliente</b>: al tenant
 * se le da la <em>clase</em> del rechazo y nada mas. Esa frontera no la
 * materializa el dominio, que guarda el codigo tal cual, sino la eleccion de
 * {@code PaymentAttemptResponse} frente a {@code SystemPaymentAttemptResponse}:
 * dos records casi identicos donde el del tenant omite un campo. Nada en el
 * compilador impide añadirselo «por comodidad» —los dos se construyen desde el
 * mismo DTO, que si lo lleva—, asi que la unica cosa que puede ponerse roja ese
 * dia es {@link Confidencialidad}. Es la razon de ser de esta clase.
 */
@WebMvcTest(PaymentAttemptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PaymentAttemptController — contrato HTTP del tenant")
class PaymentAttemptControllerTest {

    private static final Long EMPRESA_DEL_TOKEN = 77L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindPaymentAttemptUseCase findUseCase;
    @MockitoBean
    private ListPaymentAttemptsUseCase listUseCase;
    @MockitoBean
    private ListPaymentAttemptsByDocumentUseCase listByDocumentUseCase;
    @MockitoBean
    private Authz authz;

    @BeforeEach
    void empresaDelContexto() {
        when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_TOKEN);
    }

    @Nested
    @DisplayName("Confidencialidad del rechazo")
    class Confidencialidad {

        @Test
        @DisplayName("la respuesta del tenant NO lleva el codigo crudo de la pasarela")
        void la_respuesta_del_tenant_no_lleva_el_codigo_crudo() throws Exception {
            // El DTO que alimenta la respuesta SI lo trae —ver unIntento()—, asi que
            // este doesNotExist no es vacuo: mide la eleccion del record de respuesta.
            when(findUseCase.findById(31L, EMPRESA_DEL_TOKEN)).thenReturn(unIntento());

            mockMvc.perform(get("/payment-attempts/{id}", 31L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.gatewayDeclineCode").doesNotExist())
                    // Y lo que si se le da: la clase del rechazo.
                    .andExpect(jsonPath("$.declineKind").value("SOFT"));
        }

        @Test
        @DisplayName("tampoco lo lleva dentro de una pagina del listado")
        void tampoco_lo_lleva_dentro_de_una_pagina() throws Exception {
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(unIntento()), 0, 20, 1L));

            // El agujero clasico: se redacta el detalle y se olvida el listado.
            mockMvc.perform(get("/payment-attempts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].gatewayDeclineCode").doesNotExist())
                    .andExpect(jsonPath("$.content[0].declineKind").value("SOFT"));
        }

        @Test
        @DisplayName("ni en el historial de una factura")
        void ni_en_el_historial_de_una_factura() throws Exception {
            when(listByDocumentUseCase.listByDocumentAndCompany(anyLong(), anyLong(), anyInt(),
                    anyInt())).thenReturn(PageResult.of(List.of(unIntento()), 0, 20, 1L));

            mockMvc.perform(get("/payment-attempts/by-document/{id}", 8400L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].gatewayDeclineCode").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Consulta por id")
    class ConsultaPorId {

        @Test
        @DisplayName("devuelve el intento con su numero, su pasarela y sus dos instantes")
        void devuelve_el_intento_con_sus_dos_instantes() throws Exception {
            when(findUseCase.findById(31L, EMPRESA_DEL_TOKEN)).thenReturn(unIntento());

            mockMvc.perform(get("/payment-attempts/{id}", 31L)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.companyId").value(77))
                    .andExpect(jsonPath("$.billingDocumentId").value(8400))
                    .andExpect(jsonPath("$.paymentMethodId").value(8410))
                    .andExpect(jsonPath("$.attemptNumber").value(2))
                    .andExpect(jsonPath("$.gateway").value("wompi"))
                    .andExpect(jsonPath("$.requestedAmount").value(119000.00))
                    // Instantes distintos: intentado, siguiente y creado.
                    .andExpect(jsonPath("$.attemptedAt").value("2026-03-05T14:30:15"))
                    .andExpect(jsonPath("$.nextAttemptAt").value("2026-03-08T06:00:00"))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-05T14:30:20"))
                    .andExpect(jsonPath("$.version").value(3));
        }

        @Test
        @DisplayName("acota la carga con la empresa del token y nunca con una del cliente")
        void acota_la_carga_con_la_empresa_del_token() throws Exception {
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(unIntento());

            mockMvc.perform(get("/payment-attempts/{id}", 31L).param("companyId", "999"))
                    .andExpect(status().isOk());

            verify(findUseCase).findById(31L, EMPRESA_DEL_TOKEN);
        }

        @Test
        @DisplayName("un intento inexistente sale 404 con su codigo de error")
        void un_intento_inexistente_sale_404() throws Exception {
            when(findUseCase.findById(404L, EMPRESA_DEL_TOKEN))
                    .thenThrow(new PaymentAttemptNotFoundException(404L));

            mockMvc.perform(get("/payment-attempts/{id}", 404L)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_ATTEMPT_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value("Payment attempt not found: 404"));
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el listado de la empresa devuelve la pagina con sus totales")
        void el_listado_de_la_empresa_devuelve_la_pagina() throws Exception {
            when(listUseCase.listByCompany(EMPRESA_DEL_TOKEN, 2, 5))
                    .thenReturn(PageResult.of(List.of(unIntento()), 2, 5, 11L));

            mockMvc.perform(get("/payment-attempts").param("page", "2").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("el historial lleva el documento de la ruta y la empresa del token")
        void el_historial_lleva_el_documento_y_la_empresa() throws Exception {
            when(listByDocumentUseCase.listByDocumentAndCompany(anyLong(), anyLong(), anyInt(),
                    anyInt())).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/payment-attempts/by-document/{id}", 8400L))
                    .andExpect(status().isOk());

            // Acotar por documento no basta: la factura es de alguien. Los dos
            // argumentos exactos y en su orden.
            ArgumentCaptor<Long> documento = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(listByDocumentUseCase).listByDocumentAndCompany(documento.capture(),
                    empresa.capture(), eq(0), eq(20));
            assertThat(documento.getValue()).isEqualTo(8400L);
            assertThat(empresa.getValue()).isEqualTo(EMPRESA_DEL_TOKEN);
        }
    }

    /**
     * El DTO lleva {@code gatewayDeclineCode} relleno a proposito: si viniera
     * vacio, los {@code doesNotExist} de {@link Confidencialidad} pasarian solos y
     * no probarian nada.
     */
    private static PaymentAttemptDto unIntento() {
        return new PaymentAttemptDto(31L, EMPRESA_DEL_TOKEN, 8400L, 8410L, 2, "wompi",
                new BigDecimal("119000.00"), "insufficient_funds", DeclineKind.SOFT,
                LocalDateTime.of(2026, 3, 5, 14, 30, 15), LocalDateTime.of(2026, 3, 8, 6, 0, 0),
                LocalDateTime.of(2026, 3, 5, 14, 30, 20), 3L);
    }
}
