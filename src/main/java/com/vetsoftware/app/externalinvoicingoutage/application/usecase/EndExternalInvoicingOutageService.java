package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.command.EndExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.EndExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra una caida.
 *
 * <p>
 * <strong>Leer-modificar-guardar, que es el unico camino que {@code @Version}
 * protege.</strong> Un {@code UPDATE} masivo por {@code @Query} pasaria de
 * largo del bloqueo optimista y dejaria la fila cambiada con su version
 * intacta: el {@code save} concurrente que llegara con la version vieja casaria
 * igual y pisaria el cierre, sin excepcion y sin log
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * <strong>Sin reloj inyectado, y a proposito.</strong> Es el unico de los tres
 * services de escritura que no lo lleva: la hora del cierre la trae el command
 * porque mide <em>cuando volvio el servicio</em>, no cuando alguien se sento a
 * cerrar la ficha. Poner aqui {@code LocalDateTime.now(clock)} alargaria la
 * interrupcion medida por todo el tiempo que tardara el operador en enterarse.
 */
@Observed(name = "external.invoicing.outage.end")
@Service
public class EndExternalInvoicingOutageService implements EndExternalInvoicingOutageUseCase {

    private final ExternalInvoicingOutageRepository repository;

    public EndExternalInvoicingOutageService(ExternalInvoicingOutageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ExternalInvoicingOutageDto execute(EndExternalInvoicingOutageCommand command) {
        ExternalInvoicingOutage outage = repository.findById(command.id())
                .orElseThrow(() -> new ExternalInvoicingOutageNotFoundException(command.id()));
        return ExternalInvoicingOutageDto.from(repository.save(outage.end(command.endedAt())));
    }
}
