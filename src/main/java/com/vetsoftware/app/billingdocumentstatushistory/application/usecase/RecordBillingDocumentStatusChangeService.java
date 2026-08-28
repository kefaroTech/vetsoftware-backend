package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import com.vetsoftware.app.billingdocumentstatushistory.application.command.RecordBillingDocumentStatusChangeCommand;
import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.RecordBillingDocumentStatusChangeUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apunta un cambio de estado de un documento de cobro.
 *
 * <p>
 * <strong>Este servicio valida la clave foranea y nada mas; las invariantes
 * viven en el constructor de {@link BillingDocumentStatusHistory}.</strong> El
 * reparto no es de estilo: «una transicion al mismo estado no es un hecho» o
 * «todo cambio lleva actor y motivo» son verdades del fotograma y valen aunque
 * nadie llame a este metodo, mientras que «ese documento existe y es de esta
 * empresa» solo se puede saber preguntando. Poner lo primero aqui lo dejaria
 * fuera de cualquier otro camino que construya la entidad —el mapper de
 * lectura, por ejemplo— y las filas mal formadas entrarian por ahi.
 *
 * <p>
 * <strong>El momento del cambio lo pone el reloj del servidor, no el
 * cliente.</strong> {@code occurredAt} es la columna por la que se ordena la
 * pelicula y por la que se corta a una fecha; aceptarlo del cuerpo dejaria
 * antedatar un movimiento y con ello reescribir cuantos documentos estaban
 * esperando factura externa a 31 de marzo, que es justo el numero que esta
 * tabla existe para sostener. El {@code Clock} va inyectado —no
 * {@code LocalDateTime.now()} suelto— para que el test pueda fijarlo.
 *
 * <p>
 * <strong>Sin llave de idempotencia y sin unicidad que lo impida.</strong> Dos
 * llamadas identicas producen dos fotogramas, y eso es correcto: un documento
 * puede ir y volver del mismo par de estados varias veces, asi que una unicidad
 * por {@code (documento, from, to)} rechazaria historia legitima. Lo que
 * distingue el reintento del hecho repetido es {@code occurredAt}, y por eso el
 * llamador que necesite idempotencia tiene que resolverla el, no esperarla
 * aqui.
 */
@Observed(name = "billing.document.status.history.record")
@Service
public class RecordBillingDocumentStatusChangeService
        implements
            RecordBillingDocumentStatusChangeUseCase {

    private final BillingDocumentStatusHistoryRepository repository;
    private final BillingDocumentValidationPort billingDocumentValidationPort;
    private final Clock clock;

    public RecordBillingDocumentStatusChangeService(
            BillingDocumentStatusHistoryRepository repository,
            BillingDocumentValidationPort billingDocumentValidationPort, Clock clock) {
        this.repository = repository;
        this.billingDocumentValidationPort = billingDocumentValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentStatusHistoryDto execute(
            RecordBillingDocumentStatusChangeCommand command) {
        validateBillingDocument(command);

        LocalDateTime ahora = LocalDateTime.now(clock);
        BillingDocumentStatusHistory entry = BillingDocumentStatusHistory.register(
                command.companyId(), command.billingDocumentId(), command.fromStatus(),
                command.toStatus(), ahora, command.actor(), command.reason(), ahora);

        return BillingDocumentStatusHistoryDto.from(repository.save(entry));
    }

    /**
     * El documento es obligatorio —un fotograma sin pelicula no cuenta nada— y
     * tiene que ser de la misma empresa, o la FK compuesta lo rechazaria mas tarde
     * y como un error de integridad que el operador leeria como un 500.
     */
    private void validateBillingDocument(RecordBillingDocumentStatusChangeCommand command) {
        if (!billingDocumentValidationPort.existsByIdAndCompanyId(command.billingDocumentId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Billing document not found: " + command.billingDocumentId());
    }
}
