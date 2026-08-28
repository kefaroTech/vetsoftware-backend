package com.vetsoftware.app.accountingexport.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Las dos bandejas: la del mes y la completa. Ninguna filtra por empresa. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListAccountingExportsService")
class ListAccountingExportsServiceTest {

    @Mock
    private AccountingExportRepository repository;

    @InjectMocks
    private ListAccountingExportsService service;

    @Nested
    @DisplayName("listAll")
    class Completa {

        @Test
        @DisplayName("trae el barrido de plataforma traducido a DTO")
        void trae_el_barrido_de_plataforma() {
            PageResult<AccountingExport> pagina = new PageResult<>(
                    List.of(AccountingExportMother.generado()), 0, 20, 1L, 1);
            when(repository.findAll(0, 20)).thenReturn(pagina);

            PageResult<AccountingExportDto> resultado = service.listAll(0, 20);

            assertThat(resultado.content()).extracting(AccountingExportDto::id)
                    .containsExactly(AccountingExportMother.EXPORT_ID);
            assertThat(resultado.totalElements()).isEqualTo(1L);
            verify(repository).findAll(0, 20);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("listByPeriod")
    class DelMes {

        @Test
        @DisplayName("trae la bandeja del mes traducida a DTO")
        void trae_la_bandeja_del_mes() {
            PageResult<AccountingExport> pagina = new PageResult<>(
                    List.of(AccountingExportMother.generado()), 0, 20, 1L, 1);
            when(repository.findAllByPeriodKey(AccountingExportMother.PERIOD_KEY, 0, 20))
                    .thenReturn(pagina);

            PageResult<AccountingExportDto> resultado = service
                    .listByPeriod(AccountingExportMother.PERIOD_KEY, 0, 20);

            assertThat(resultado.content()).extracting(AccountingExportDto::periodKey)
                    .containsExactly(AccountingExportMother.PERIOD_KEY);
            verify(repository).findAllByPeriodKey(AccountingExportMother.PERIOD_KEY, 0, 20);
            verifyNoMoreInteractions(repository);
        }
    }
}
