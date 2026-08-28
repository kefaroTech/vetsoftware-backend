package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAccountingPeriodService — la lectura por id")
class FindAccountingPeriodServiceTest {

    private static final Long ID = 8800L;

    @Mock
    private AccountingPeriodRepository repository;
    @InjectMocks
    private FindAccountingPeriodService service;

    @Test
    @DisplayName("devuelve el mes con su estado y su cierre")
    void devuelve_el_mes_con_su_estado_y_su_cierre() {
        when(repository.findById(ID))
                .thenReturn(Optional.of(AccountingPeriodMother.cerradoEnBlando(ID)));

        AccountingPeriodDto encontrado = service.findById(ID);

        assertThat(encontrado.id()).isEqualTo(ID);
        assertThat(encontrado.periodKey()).isEqualTo("2026-03");
        assertThat(encontrado.status()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        assertThat(encontrado.closedBySystemUserId()).isEqualTo(AccountingPeriodMother.CERRADO_POR);
    }

    @Test
    @DisplayName("un mes inexistente es un 404 que nombra el id pedido")
    void un_mes_inexistente_es_un_404() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID))
                .isInstanceOf(AccountingPeriodNotFoundException.class)
                .hasMessageContaining("Accounting period not found: 8800");
    }
}
