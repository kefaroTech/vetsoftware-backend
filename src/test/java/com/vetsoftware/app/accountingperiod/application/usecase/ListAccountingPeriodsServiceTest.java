package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAccountingPeriodsService — el calendario paginado")
class ListAccountingPeriodsServiceTest {

    @Mock
    private AccountingPeriodRepository repository;
    @InjectMocks
    private ListAccountingPeriodsService service;

    @Test
    @DisplayName("proyecta el contenido y CONSERVA los totales de la consulta")
    void proyecta_el_contenido_y_conserva_los_totales() {
        // Recalcular los totales sobre el contenido ya paginado es como se acaba
        // reportando «2 de 2» en un calendario de treinta y siete meses.
        PageResult<AccountingPeriod> pagina = PageResult
                .of(List.of(AccountingPeriodMother.persistidoAbierto(8800L),
                        AccountingPeriodMother.cerradoEnBlando(8801L)), 1, 2, 37L);
        when(repository.findAll(1, 2)).thenReturn(pagina);

        PageResult<AccountingPeriodDto> resultado = service.listAll(1, 2);

        assertThat(resultado.content()).extracting(AccountingPeriodDto::id).containsExactly(8800L,
                8801L);
        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.pageSize()).isEqualTo(2);
        assertThat(resultado.totalElements()).isEqualTo(37L);
        assertThat(resultado.totalPages()).isEqualTo(19);
    }

    @Test
    @DisplayName("una pagina vacia sigue siendo una pagina, no un fallo")
    void una_pagina_vacia_sigue_siendo_una_pagina() {
        when(repository.findAll(9, 20)).thenReturn(PageResult.empty(9, 20));

        assertThat(service.listAll(9, 20).content()).isEmpty();
    }
}
