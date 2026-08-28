package com.vetsoftware.app.accountingexport.application.usecase;

import com.vetsoftware.app.accountingexport.application.command.RejectAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.in.ResolveAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los tres desenlaces de una exportacion: entregada, rechazada o reemplazada.
 *
 * <p>
 * <strong>Los tres van por el ciclo leer-modificar-guardar</strong>, que es el
 * unico camino que {@code @Version} protege: dos operadores que resuelvan el
 * mismo fichero a la vez no se pisan, el segundo se lleva un fallo de bloqueo
 * en vez de machacar el desenlace del primero. Un {@code UPDATE} masivo aqui
 * pasaria de largo del bloqueo optimista
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * <strong>Que la transicion sea legitima lo decide el dominio, no este
 * service.</strong> {@code chk_accounting_exports_lifecycle} mira la fila y no
 * de donde venia, asi que pasar de {@code DELIVERED} a {@code REJECTED} produce
 * una fila que el motor acepta sin una queja — y borra la fecha de entrega, que
 * es la prueba de que el mes se entrego a tiempo.
 *
 * <p>
 * <strong>El reloj va inyectado</strong>: la fecha de entrega y la de rechazo
 * son datos probatorios y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build
 * ante un {@code now()} pelado.
 */
@Observed(name = "accounting.export.resolve")
@Service
public class ResolveAccountingExportService implements ResolveAccountingExportUseCase {

    private final AccountingExportRepository repository;
    private final Clock clock;

    public ResolveAccountingExportService(AccountingExportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingExportDto markDelivered(Long id) {
        AccountingExport export = require(id);
        return AccountingExportDto
                .from(repository.save(export.markDelivered(LocalDateTime.now(clock))));
    }

    @Override
    @Transactional
    public AccountingExportDto markRejected(RejectAccountingExportCommand command) {
        AccountingExport export = require(command.id());
        return AccountingExportDto.from(repository
                .save(export.markRejected(LocalDateTime.now(clock), command.rejectionReason())));
    }

    @Override
    @Transactional
    public AccountingExportDto markSuperseded(Long id) {
        AccountingExport export = require(id);
        return AccountingExportDto.from(repository.save(export.supersede()));
    }

    private AccountingExport require(Long id) {
        return repository.findById(id).orElseThrow(() -> new AccountingExportNotFoundException(id));
    }
}
