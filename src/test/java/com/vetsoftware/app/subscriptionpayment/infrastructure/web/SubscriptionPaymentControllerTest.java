package com.vetsoftware.app.subscriptionpayment.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.FindSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ListSubscriptionPaymentsUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ReconcileSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.RegisterSubscriptionPaymentUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubscriptionPaymentController — contrato HTTP")
class SubscriptionPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RegisterSubscriptionPaymentUseCase registerUseCase;
    @MockitoBean
    private ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase;
    @MockitoBean
    private ReconcileSubscriptionPaymentUseCase reconcileUseCase;
    @MockitoBean
    private FindSubscriptionPaymentUseCase findUseCase;
    @MockitoBean
    private ListSubscriptionPaymentsUseCase listUseCase;
    @MockitoBean
    private Authz authz;

    @Test
    @DisplayName("lista pagos únicamente para la empresa autenticada")
    void lista_pagos_unicamente_para_la_empresa_autenticada() throws Exception {
        when(authz.currentCompanyId()).thenReturn(77L);
        when(listUseCase.listByCompany(77L, 0, 4)).thenReturn(PageResult.empty(0, 4));

        mockMvc.perform(get("/subscription-payments").param("pageSize", "4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.pageSize").value(4));

        verify(listUseCase).listByCompany(77L, 0, 4);
    }
}
