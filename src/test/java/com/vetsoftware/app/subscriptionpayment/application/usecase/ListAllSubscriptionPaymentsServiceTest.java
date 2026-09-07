package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAllSubscriptionPaymentsService - tesoreria cross-tenant")
class ListAllSubscriptionPaymentsServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private SubscriptionPaymentRepository repository;

    private ListAllSubscriptionPaymentsService service;

    @BeforeEach
    void setUp() {
        service = new ListAllSubscriptionPaymentsService(repository, RELOJ);
    }

    @Nested
    class SinPendingOlderThanMinutes {

        @Test
        @DisplayName("propaga companyId, estado y rango de fechas tal cual")
        void propaga_los_filtros_explicitos() {
            when(repository.findAllFiltered(SubscriptionPaymentMother.EMPRESA,
                    SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                    LocalDateTime.of(2026, 8, 31, 23, 59), null, 2, 5)).thenReturn(
                            PageResult.of(List.of(SubscriptionPaymentMother.pagoConfirmado("1.00")),
                                    2, 5, 11));

            PageResult<SubscriptionPaymentDto> result = service
                    .listAll(new ListAllSubscriptionPaymentsQuery(SubscriptionPaymentMother.EMPRESA,
                            SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                            LocalDateTime.of(2026, 8, 31, 23, 59), null, 2, 5));

            assertThat(result.totalElements()).isEqualTo(11);
            verify(repository).findAllFiltered(SubscriptionPaymentMother.EMPRESA,
                    SubscriptionPaymentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 0, 0),
                    LocalDateTime.of(2026, 8, 31, 23, 59), null, 2, 5);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    class ConPendingOlderThanMinutes {

        @Test
        @DisplayName("fuerza el estado PENDING y calcula el umbral desde el reloj inyectado")
        void fuerza_pending_y_calcula_el_umbral() {
            when(repository.findAllFiltered(isNull(), eq(SubscriptionPaymentStatus.PENDING),
                    isNull(), isNull(), eq(LocalDateTime.of(2026, 9, 6, 11, 30)), eq(0), eq(20)))
                    .thenReturn(PageResult.empty(0, 20));

            service.listAll(new ListAllSubscriptionPaymentsQuery(null,
                    SubscriptionPaymentStatus.CONFIRMED, null, null, 30, 0, 20));

            verify(repository).findAllFiltered(null, SubscriptionPaymentStatus.PENDING, null, null,
                    LocalDateTime.of(2026, 9, 6, 11, 30), 0, 20);
            verifyNoMoreInteractions(repository);
        }
    }
}
