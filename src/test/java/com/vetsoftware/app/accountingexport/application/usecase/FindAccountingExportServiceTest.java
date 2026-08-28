package com.vetsoftware.app.accountingexport.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExportNotFoundException;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAccountingExportService")
class FindAccountingExportServiceTest {

    @Mock
    private AccountingExportRepository repository;

    @InjectMocks
    private FindAccountingExportService service;

    @Test
    @DisplayName("devuelve el DTO de la exportacion por su id")
    void devuelve_el_dto_de_la_exportacion_por_su_id() {
        when(repository.findById(AccountingExportMother.EXPORT_ID))
                .thenReturn(Optional.of(AccountingExportMother.generado()));

        AccountingExportDto dto = service.findById(AccountingExportMother.EXPORT_ID);

        assertThat(dto.id()).isEqualTo(AccountingExportMother.EXPORT_ID);
        assertThat(dto.status()).isEqualTo(AccountingExportStatus.GENERATED);
    }

    @Test
    @DisplayName("una exportacion inexistente lanza AccountingExportNotFoundException")
    void una_exportacion_inexistente_lanza_not_found() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(AccountingExportNotFoundException.class)
                .hasMessageContaining("Accounting export not found: 999");
    }
}
