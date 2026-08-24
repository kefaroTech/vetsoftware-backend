package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAllSubscriptionPaymentsService - tesoreria cross-tenant")
class ListAllSubscriptionPaymentsServiceTest {

    @Mock
    private SubscriptionPaymentRepository repository;
    @InjectMocks
    private ListAllSubscriptionPaymentsService service;

    @Test
    @DisplayName("sin companyId usa el barrido global y conserva la pagina")
    void sin_company_id_lista_todos_los_tenants() {
        when(repository.findAll(2, 5)).thenReturn(
                PageResult.of(List.of(SubscriptionPaymentMother.pagoPendiente()), 2, 5, 11));

        PageResult<SubscriptionPaymentDto> result = service.listAll(null, 2, 5);

        assertThat(result.content()).singleElement().satisfies(payment -> {
            assertThat(payment.id()).isEqualTo(7L);
            assertThat(payment.companyId()).isEqualTo(SubscriptionPaymentMother.EMPRESA);
        });
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(3);
        verify(repository).findAll(2, 5);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("con companyId delega en la consulta acotada")
    void con_company_id_filtra_el_tenant_solicitado() {
        when(repository.findAllByCompanyId(SubscriptionPaymentMother.EMPRESA, 0, 20))
                .thenReturn(PageResult.empty(0, 20));

        PageResult<SubscriptionPaymentDto> result = service
                .listAll(SubscriptionPaymentMother.EMPRESA, 0, 20);

        assertThat(result.content()).isEmpty();
        verify(repository).findAllByCompanyId(SubscriptionPaymentMother.EMPRESA, 0, 20);
        verifyNoMoreInteractions(repository);
    }
}
