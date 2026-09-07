package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.application.query.ListAllSubscriptionPaymentsQuery;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportSubscriptionPaymentsService - cierre de mes")
class ExportSubscriptionPaymentsServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private SubscriptionPaymentRepository repository;

    private ExportSubscriptionPaymentsService service;

    @BeforeEach
    void setUp() {
        service = new ExportSubscriptionPaymentsService(repository, RELOJ);
    }

    @Test
    @DisplayName("devuelve el filtro entero sin paginar, ignorando page/pageSize del query")
    void exporta_sin_paginar() {
        when(repository.findAllFilteredForExport(SubscriptionPaymentMother.EMPRESA,
                SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59), null))
                .thenReturn(List.of(SubscriptionPaymentMother.pagoConfirmado("1.00")));

        List<SubscriptionPaymentDto> result = service
                .export(new ListAllSubscriptionPaymentsQuery(SubscriptionPaymentMother.EMPRESA,
                        SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                        LocalDateTime.of(2026, 8, 31, 23, 59), null, 3, 7));

        assertThat(result).hasSize(1);
        verify(repository).findAllFilteredForExport(SubscriptionPaymentMother.EMPRESA,
                SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59), null);
        verifyNoMoreInteractions(repository);
    }
}
