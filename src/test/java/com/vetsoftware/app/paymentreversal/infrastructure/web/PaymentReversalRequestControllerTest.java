package com.vetsoftware.app.paymentreversal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.FindPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.in.ListPaymentReversalRequestsUseCase;
import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequestNotFoundException;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
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
 * La cara de tenant del expediente: <b>solo lecturas</b>.
 *
 * <p>
 * Hay tres cosas que congelar aqui, y las tres son regresiones que compilan.
 *
 * <p>
 * <b>Una, que la empresa sale del contexto de autorizacion y no de la
 * peticion.</b> Los casos afirman con el {@code companyId} del contexto puesto
 * a un valor <em>distinto</em> de cualquiera que viaje en la URL, y capturan el
 * que llega al caso de uso: si alguien cablea un dia
 * {@code @RequestParam Long companyId}, el caso se pone rojo en vez de servir
 * el expediente de la clinica vecina.
 *
 * <p>
 * <b>Dos, que la respuesta lleva las tres fechas y el lado propio del
 * expediente.</b> Sin {@code claimReceivedAt} y {@code consumerBecameAwareAt}
 * el cliente no puede comprobar que su reclamacion se atendio dentro de plazo,
 * y sin la oposicion la defensa de la plataforma deja de ser auditable. Se
 * afirma campo a campo porque un {@code record} al que se le cae un componente
 * sigue compilando y sigue devolviendo 200.
 *
 * <p>
 * <b>Y tres, que por aqui no se escribe.</b> Instruir una reversion vive en
 * {@link SystemPaymentReversalRequestController}, y esa separacion es lo que
 * impide que la clinica reclamante escriba en el expediente con el que la
 * plataforma se defiende. Un {@code @PostMapping} anadido aqui por comodidad
 * rompe el caso.
 */
@WebMvcTest(PaymentReversalRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PaymentReversalRequestController — contrato HTTP")
class PaymentReversalRequestControllerTest {

    /**
     * Distinto del {@link WebMvcSliceConfig#COMPANY_ID} por defecto, a proposito.
     */
    private static final Long EMPRESA_DEL_CONTEXTO = 4_242L;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FindPaymentReversalRequestUseCase findUseCase;
    @MockitoBean
    private ListPaymentReversalRequestsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("el expediente sale con las tres fechas y con el lado propio —acuse,"
                + " oposicion y desenlace—, que es la mitad que lo hace auditable")
        void el_expediente_sale_con_las_tres_fechas_y_el_lado_propio() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(expediente());

            mockMvc.perform(get("/payment-reversal-requests/77")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(77))
                    .andExpect(jsonPath("$.companyId").value(EMPRESA_DEL_CONTEXTO))
                    .andExpect(jsonPath("$.paymentId").value(8001))
                    .andExpect(jsonPath("$.origin").value("CONSUMER_CLAIM"))
                    .andExpect(jsonPath("$.causal").value("PRODUCT_NOT_RECEIVED"))
                    .andExpect(jsonPath("$.consumerDetermination").value("CONSUMER"))
                    .andExpect(jsonPath("$.consumerBecameAwareAt").value("2026-03-01T08:15:11"))
                    .andExpect(jsonPath("$.claimReceivedAt").value("2026-03-05T09:26:22"))
                    .andExpect(jsonPath("$.issuerNotifiedAt").value("2026-03-07T10:37:33"))
                    .andExpect(jsonPath("$.deadlineAt").value("2026-04-19T11:48:44"))
                    .andExpect(jsonPath("$.claimEvidenceRef").value("EV-QUEJA-0001"))
                    .andExpect(jsonPath("$.acknowledgementRef").value("ACU-0001"))
                    .andExpect(jsonPath("$.acknowledgedAt").value("2026-03-06T13:05:06"))
                    .andExpect(jsonPath("$.oppositionGround").value("CAUSAL_NOT_REPORTED"))
                    .andExpect(jsonPath("$.oppositionEvidenceRef").value("EV-OPOSICION-0001"))
                    .andExpect(jsonPath("$.opposedAt").value("2026-03-08T14:16:17"))
                    .andExpect(jsonPath("$.outcome").value("PARTIALLY_ACCEPTED"))
                    .andExpect(jsonPath("$.appliedAmount").value(137500.75))
                    .andExpect(jsonPath("$.resultingRefundId").value(4400))
                    .andExpect(jsonPath("$.createdDate").value("2026-03-05T12:59:55"));
        }

        @Test
        @DisplayName("el listado devuelve la pagina con sus cinco campos y el contenido dentro")
        void el_listado_devuelve_la_pagina_con_sus_cinco_campos() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(expediente()), 3, 7, 15L));

            mockMvc.perform(
                    get("/payment-reversal-requests").param("page", "3").param("pageSize", "7"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(77))
                    .andExpect(
                            jsonPath("$.content[0].claimReceivedAt").value("2026-03-05T09:26:22"))
                    .andExpect(jsonPath("$.page").value(3))
                    .andExpect(jsonPath("$.pageSize").value(7))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3));

            verify(listUseCase).listByCompany(EMPRESA_DEL_CONTEXTO, 3, 7);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa del expediente sale del contexto de autorizacion y no de un"
                + " parametro que escriba el cliente")
        void la_empresa_sale_del_contexto_y_no_de_la_peticion() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findUseCase.findById(anyLong(), anyLong())).thenReturn(expediente());

            mockMvc.perform(get("/payment-reversal-requests/77").param("companyId", "999"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> id = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> companyId = ArgumentCaptor.forClass(Long.class);
            verify(findUseCase).findById(id.capture(), companyId.capture());
            assertThat(id.getValue()).isEqualTo(77L);
            assertThat(companyId.getValue()).isEqualTo(EMPRESA_DEL_CONTEXTO);
        }

        @Test
        @DisplayName("el listado tambien toma la empresa del contexto e ignora la que venga en la"
                + " peticion")
        void el_listado_tambien_toma_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(listUseCase.listByCompany(anyLong(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/payment-reversal-requests").param("companyId", "999"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> companyId = ArgumentCaptor.forClass(Long.class);
            verify(listUseCase).listByCompany(companyId.capture(), anyInt(), anyInt());
            assertThat(companyId.getValue()).isEqualTo(EMPRESA_DEL_CONTEXTO);
        }
    }

    @Nested
    @DisplayName("Solo lectura")
    class SoloLectura {

        @Test
        @DisplayName("la clinica reclamante no puede escribir en el expediente con el que la"
                + " plataforma se defiende: ni abrirlo ni resolverlo")
        void la_clinica_reclamante_no_puede_escribir_en_el_expediente() throws Exception {
            mockMvc.perform(post("/payment-reversal-requests"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(patch("/payment-reversal-requests/77"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Errores de dominio")
    class Errores {

        @Test
        @DisplayName("un expediente inexistente responde 404 con su codigo de error")
        void un_expediente_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(EMPRESA_DEL_CONTEXTO);
            when(findUseCase.findById(anyLong(), anyLong()))
                    .thenThrow(new PaymentReversalRequestNotFoundException(99L));

            mockMvc.perform(get("/payment-reversal-requests/99")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_REVERSAL_REQUEST_NOT_FOUND"))
                    .andExpect(jsonPath("$.detail")
                            .value(Matchers.containsString("Payment reversal request")));
        }
    }

    private static PaymentReversalRequestDto expediente() {
        return new PaymentReversalRequestDto(77L, EMPRESA_DEL_CONTEXTO, 8_001L,
                ReversalOrigin.CONSUMER_CLAIM, ReversalCausal.PRODUCT_NOT_RECEIVED,
                ConsumerDetermination.CONSUMER, LocalDateTime.of(2026, 3, 1, 8, 15, 11),
                LocalDateTime.of(2026, 3, 5, 9, 26, 22), LocalDateTime.of(2026, 3, 7, 10, 37, 33),
                "EV-QUEJA-0001", "ACU-0001", LocalDateTime.of(2026, 3, 6, 13, 5, 6),
                OppositionGround.CAUSAL_NOT_REPORTED, "EV-OPOSICION-0001",
                LocalDateTime.of(2026, 3, 8, 14, 16, 17), LocalDateTime.of(2026, 4, 19, 11, 48, 44),
                new BigDecimal("137500.75"), ReversalOutcome.PARTIALLY_ACCEPTED, 4_400L,
                LocalDateTime.of(2026, 3, 5, 12, 59, 55), 3L);
    }
}
