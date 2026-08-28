package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.MatchExternalInvoiceCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.MatchExternalInvoiceUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la factura que emitio el tercero.
 *
 * <p>
 * <strong>Este servicio no clasifica, y no es por brevedad.</strong> Calcular
 * {@code difference} y decidir entre {@code MATCHED}, {@code WITHIN_TOLERANCE}
 * y {@code MISMATCH} es la invariante central de la feature -la regla de los
 * dos pesos- y vive en {@link ExternalInvoiceReconciliation#match}. Aqui solo
 * se carga la fila, se le pasa el hecho externo y se guarda. Si la
 * clasificacion viviera en este metodo, cualquier segundo camino de escritura
 * -una importacion masiva, un job- podria escribir un estado que no corresponde
 * a su diferencia, y la base lo aceptaria: {@code chk_eir_difference} comprueba
 * la resta, no la clasificacion.
 *
 * <p>
 * {@code @Transactional} porque son dos operaciones de repositorio -{@code
 * findById} y {@code save}- y porque el {@code @Version} de la entidad solo
 * protege dentro del ciclo leer-modificar-guardar de una misma transaccion.
 */
@Observed(name = "external.invoice.reconciliation.match")
@Service
public class MatchExternalInvoiceService implements MatchExternalInvoiceUseCase {

    private final ExternalInvoiceReconciliationRepository repository;

    public MatchExternalInvoiceService(ExternalInvoiceReconciliationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ExternalInvoiceReconciliationDto execute(MatchExternalInvoiceCommand command) {
        ExternalInvoiceReconciliation reconciliation = repository.findById(command.id())
                .orElseThrow(
                        () -> new ExternalInvoiceReconciliationNotFoundException(command.id()));
        reconciliation.match(command.externalInvoiceId(), command.externalCufe(),
                command.externalTotal(), command.externalTax(), command.externalResolutionNumber(),
                command.externalRangeFrom(), command.externalRangeTo(),
                command.resolutionValidUntil());
        return ExternalInvoiceReconciliationDto.from(repository.save(reconciliation));
    }
}
