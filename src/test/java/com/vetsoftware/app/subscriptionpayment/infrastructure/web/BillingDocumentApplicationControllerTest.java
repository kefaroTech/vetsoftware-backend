package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentSummaryDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListBillingDocumentApplicationsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReverseBillingDocumentApplicationUseCase;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingDocumentApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BillingDocumentApplicationController — contrato HTTP")
class BillingDocumentApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ApplyBillingDocumentUseCase applyUseCase;
    @MockitoBean
    private ReverseBillingDocumentApplicationUseCase reverseUseCase;
    @MockitoBean
    private ListBillingDocumentApplicationsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("lista únicamente las aplicaciones del documento y empresa autenticados")
    void lista_unicamente_las_aplicaciones_del_documento_y_empresa_autenticados() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(listUseCase.listByTargetDocument(9L, 77L, 2, 5)).thenReturn(PageResult.empty(2, 5));

        mockMvc.perform(get("/billing-document-applications").param("targetDocumentId", "9")
                .param("page", "2").param("pageSize", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(5));

        verify(listUseCase).listByTargetDocument(9L, 77L, 2, 5);
    }

    /**
     * <b>La firma del castigo la pone el backend, nunca el cuerpo.</b> Si el id
     * llegara en el JSON, quien da una deuda por incobrable elegiria a quien
     * atribuirsela, que es lo contrario de firmar.
     */
    @Test
    @DisplayName("un castigo toma la firma nominal del principal, no del cuerpo")
    void un_castigo_toma_la_firma_del_principal() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(authz.currentSystemUserIdOrNull()).thenReturn(11L);
        when(applyUseCase.execute(any())).thenReturn(aplicacionDeCastigo());

        mockMvc.perform(
                post("/billing-document-applications").contentType(APPLICATION_JSON).content("""
                        {"targetDocumentId":9,"sourceKind":"WRITE_OFF","appliedAmount":1000,
                         "writeOffReason":"Cliente liquidado"}
                        """)).andExpect(status().isCreated());

        ArgumentCaptor<ApplyBillingDocumentCommand> comando = ArgumentCaptor
                .forClass(ApplyBillingDocumentCommand.class);
        verify(applyUseCase).execute(comando.capture());
        assertThat(comando.getValue().writeOffAuthorizedBySystemUserId()).isEqualTo(11L);
        assertThat(comando.getValue().writeOffReason()).isEqualTo("Cliente liquidado");
        assertThat(comando.getValue().companyId()).isEqualTo(77L);
    }

    /**
     * La otra mitad: en los otros cinco origenes el autorizante no se pone. Ponerlo
     * afirmaria que alguien autorizo algo que nadie tuvo que autorizar, y el
     * dominio rechaza la fila ({@code chk_bda_write_off_signature}).
     */
    @Test
    @DisplayName("una retencion no lleva firma de castigo aunque haya principal de plataforma")
    void una_retencion_no_lleva_firma_de_castigo() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(applyUseCase.execute(any())).thenReturn(aplicacionDeCastigo());

        mockMvc.perform(
                post("/billing-document-applications").contentType(APPLICATION_JSON).content("""
                        {"targetDocumentId":9,"sourceKind":"WITHHOLDING","withholdingId":300,
                         "appliedAmount":7160,"valueDate":"2026-10-30"}
                        """)).andExpect(status().isCreated());

        ArgumentCaptor<ApplyBillingDocumentCommand> comando = ArgumentCaptor
                .forClass(ApplyBillingDocumentCommand.class);
        verify(applyUseCase).execute(comando.capture());
        assertThat(comando.getValue().writeOffAuthorizedBySystemUserId()).isNull();
        assertThat(comando.getValue().withholdingId()).isEqualTo(300L);
        assertThat(comando.getValue().valueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    @DisplayName("el lote de saldo a favor viaja en el comando")
    void el_lote_de_saldo_a_favor_viaja() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(applyUseCase.execute(any())).thenReturn(aplicacionDeCastigo());

        mockMvc.perform(
                post("/billing-document-applications").contentType(APPLICATION_JSON).content("""
                        {"targetDocumentId":9,"sourceKind":"CUSTOMER_CREDIT","creditEntryId":800,
                         "appliedAmount":50000}
                        """)).andExpect(status().isCreated());

        ArgumentCaptor<ApplyBillingDocumentCommand> comando = ArgumentCaptor
                .forClass(ApplyBillingDocumentCommand.class);
        verify(applyUseCase).execute(comando.capture());
        assertThat(comando.getValue().creditEntryId()).isEqualTo(800L);
    }

    @Test
    @DisplayName("un motivo de castigo mas largo que la columna se rechaza con 400")
    void motivo_demasiado_largo_es_400() throws Exception {
        mockMvc.perform(post("/billing-document-applications").contentType(APPLICATION_JSON)
                .content("{\"targetDocumentId\":9,\"sourceKind\":\"WRITE_OFF\","
                        + "\"appliedAmount\":1000,\"writeOffReason\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(applyUseCase, never()).execute(any());
    }

    /** La respuesta expone la referencia del origen y la fecha valor. */
    @Test
    @DisplayName("la respuesta lleva el origen, su referencia y la fecha valor")
    void la_respuesta_lleva_el_origen_y_la_fecha_valor() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(applyUseCase.execute(any())).thenReturn(aplicacionDeRetencion());

        mockMvc.perform(
                post("/billing-document-applications").contentType(APPLICATION_JSON).content("""
                        {"targetDocumentId":9,"sourceKind":"WITHHOLDING","withholdingId":300,
                         "appliedAmount":7160}
                        """)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceKind").value("WITHHOLDING"))
                .andExpect(jsonPath("$.withholdingId").value(300))
                .andExpect(jsonPath("$.valueDate").value("2026-10-30"))
                .andExpect(jsonPath("$.paymentId").doesNotExist());
    }

    private static BillingDocumentSummaryDto documento() {
        return new BillingDocumentSummaryDto(9L, 77L, "FAC-2026-0001", "INVOICE",
                new BigDecimal("213010.00"), new BigDecimal("7160.00"));
    }

    private static BillingDocumentApplicationDto aplicacionDeRetencion() {
        return new BillingDocumentApplicationDto(1L, 77L, documento(),
                ApplicationSourceKind.WITHHOLDING, null, null, 300L, null,
                new BigDecimal("7160.00"), null, null, null, LocalDateTime.of(2026, 11, 3, 9, 0),
                LocalDate.of(2026, 10, 30), LocalDateTime.of(2026, 11, 3, 9, 0));
    }

    private static BillingDocumentApplicationDto aplicacionDeCastigo() {
        return new BillingDocumentApplicationDto(2L, 77L, documento(),
                ApplicationSourceKind.WRITE_OFF, null, null, null, null, new BigDecimal("1000.00"),
                null, 11L, "Cliente liquidado", LocalDateTime.of(2026, 11, 3, 9, 0),
                LocalDate.of(2026, 11, 3), LocalDateTime.of(2026, 11, 3, 9, 0));
    }
}
