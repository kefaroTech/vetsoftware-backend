package com.vetsoftware.app.accountingexport.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingPeriodValidationPort;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateAccountingExportService")
class GenerateAccountingExportServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 4, 1, 9, 0);

    /** El {@code Clock} no es un puerto: se inyecta de verdad y fijo. */
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private AccountingExportRepository repository;
    @Mock
    private AccountingPeriodValidationPort accountingPeriodValidationPort;

    @Captor
    private ArgumentCaptor<AccountingExport> exportCaptor;

    private GenerateAccountingExportService service;

    @BeforeEach
    void servicio() {
        service = new GenerateAccountingExportService(repository, accountingPeriodValidationPort,
                RELOJ);
    }

    @Test
    @DisplayName("el primer fichero de un mes y una clase nace con intento 1")
    void el_primer_fichero_nace_con_intento_1() {
        when(accountingPeriodValidationPort.existsByPeriodKey(AccountingExportMother.PERIOD_KEY))
                .thenReturn(true);
        when(repository.findLastAttemptNumber(AccountingExportMother.PERIOD_KEY,
                AccountingExportMother.KIND)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(AccountingExportMother.comandoGenerar());

        verify(repository).save(exportCaptor.capture());
        assertThat(exportCaptor.getValue().getAttemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("rehacer un fichero toma el siguiente numero al ultimo intento registrado")
    void rehacer_un_fichero_toma_el_siguiente_numero() {
        when(accountingPeriodValidationPort.existsByPeriodKey(AccountingExportMother.PERIOD_KEY))
                .thenReturn(true);
        when(repository.findLastAttemptNumber(AccountingExportMother.PERIOD_KEY,
                AccountingExportMother.KIND)).thenReturn(Optional.of(3));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(AccountingExportMother.comandoGenerar());

        verify(repository).save(exportCaptor.capture());
        assertThat(exportCaptor.getValue().getAttemptNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("usa el reloj inyectado para sellar generatedAt y createdDate")
    void usa_el_reloj_inyectado_para_sellar_las_fechas() {
        when(accountingPeriodValidationPort.existsByPeriodKey(AccountingExportMother.PERIOD_KEY))
                .thenReturn(true);
        when(repository.findLastAttemptNumber(AccountingExportMother.PERIOD_KEY,
                AccountingExportMother.KIND)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountingExportDto dto = service.execute(AccountingExportMother.comandoGenerar());

        assertThat(dto.generatedAt()).isEqualTo(AHORA);
        assertThat(dto.createdDate()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("no toca el repositorio si el periodo contable no existe: la FK es RESTRICT")
    void no_toca_el_repositorio_si_el_periodo_no_existe() {
        when(accountingPeriodValidationPort.existsByPeriodKey(AccountingExportMother.PERIOD_KEY))
                .thenReturn(false);

        assertThatThrownBy(() -> service.execute(AccountingExportMother.comandoGenerar()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                        "Accounting period not found: " + AccountingExportMother.PERIOD_KEY);

        verifyNoInteractions(repository);
    }
}
