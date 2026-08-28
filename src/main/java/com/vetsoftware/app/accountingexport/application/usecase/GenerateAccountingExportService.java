package com.vetsoftware.app.accountingexport.application.usecase;

import com.vetsoftware.app.accountingexport.application.command.GenerateAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.in.GenerateAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingPeriodValidationPort;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un fichero de exportacion recien generado.
 *
 * <h2>El numero de intento lo calcula el service, no el llamador</h2>
 *
 * <p>
 * Es el ultimo del mismo mes y clase mas uno. <strong>No es una garantia de
 * unicidad y no pretende serlo</strong>: dos generaciones concurrentes leerian
 * el mismo maximo y la segunda chocaria contra
 * {@code uq_accounting_exports_attempt}, que es exactamente lo que tiene que
 * pasar. Lo que este calculo compra es que el caso normal no obligue al
 * llamador a llevar la cuenta.
 *
 * <p>
 * <strong>La otra unicidad —{@code uq_accounting_exports_current}— tampoco se
 * comprueba preguntando antes.</strong> Es la que impide dos ficheros vivos del
 * mismo mes y clase, y la cuida la base sobre una columna generada. Un
 * {@code exists} previo seria una comprobacion que dos peticiones concurrentes
 * pasarian las dos.
 *
 * <h2>Que el mes exista se pregunta; que este abierto, no</h2>
 *
 * <p>
 * Lo primero es una clave foranea {@code RESTRICT} y sin preguntarlo el fallo
 * saldria como error de integridad. Lo segundo lo impone el disparador
 * {@code trg_accounting_exports_bi_period_open} del changeset 346, que es donde
 * tiene que estar: una comprobacion previa desde Java la pasarian dos
 * peticiones concurrentes, y el cierre del mes puede ocurrir entre la pregunta
 * y el {@code INSERT}.
 */
@Observed(name = "accounting.export.generate")
@Service
public class GenerateAccountingExportService implements GenerateAccountingExportUseCase {

    /**
     * El primer intento de un mes y una clase. Espejo de
     * {@code chk_accounting_exports_attempt}.
     */
    private static final int FIRST_ATTEMPT = 1;

    private final AccountingExportRepository repository;
    private final AccountingPeriodValidationPort accountingPeriodValidationPort;
    private final Clock clock;

    public GenerateAccountingExportService(AccountingExportRepository repository,
            AccountingPeriodValidationPort accountingPeriodValidationPort, Clock clock) {
        this.repository = repository;
        this.accountingPeriodValidationPort = accountingPeriodValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingExportDto execute(GenerateAccountingExportCommand command) {
        if (!accountingPeriodValidationPort.existsByPeriodKey(command.periodKey()))
            throw new IllegalArgumentException(
                    "Accounting period not found: " + command.periodKey());
        int attemptNumber = repository
                .findLastAttemptNumber(command.periodKey(), command.exportKind())
                .map(last -> last + 1).orElse(FIRST_ATTEMPT);
        LocalDateTime now = LocalDateTime.now(clock);
        AccountingExport export = AccountingExport.generate(command.periodKey(),
                command.exportKind(), attemptNumber, now, command.generatedBySystemUserId(),
                command.totalDebit(), command.totalCredit(), command.totalsHash(),
                command.fileRef(), now);
        return AccountingExportDto.from(repository.save(export));
    }
}
