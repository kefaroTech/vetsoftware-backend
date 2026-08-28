package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import com.vetsoftware.app.externalinvoicingoutage.application.command.OpenExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.in.OpenExternalInvoicingOutageUseCase;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la ficha de una caida de la emision fiscal.
 *
 * <p>
 * <strong>El service hace exactamente una cosa, y no es validar la
 * caida.</strong> Sella la fecha de creacion con el reloj inyectado. Todo lo
 * demas —que el fin sea posterior al inicio, que el aviso no preceda al inicio,
 * que el contador no sea negativo, que haya resumen— son invariantes y viven en
 * el constructor de {@link ExternalInvoicingOutage}. Ahi no se pueden saltar;
 * aqui si, llamando al constructor desde otro sitio.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes, y es a
 * proposito.</strong> «Una sola caida abierta por causante» la cuida
 * {@code uq_eio_open} sobre la columna generada {@code open_outage_marker}. Un
 * {@code exists} previo seria una comprobacion que dos peticiones concurrentes
 * pasarian las dos —y el llamador tipico de este caso de uso <em>es</em> un
 * proceso de deteccion que sondea en bucle—, dejando el rastro de caidas vivas
 * que nunca se cierran que el changeset 358 se propuso cerrar. Aqui el
 * duplicado llega como violacion de integridad, que es la unica respuesta que
 * no miente.
 *
 * <p>
 * El reloj entra por parametro y no se llama a {@code LocalDateTime.now()} aqui
 * dentro ({@code RELOJ_INYECTADO_EN_VEZ_DE_NOW}). Ojo con la otra fecha:
 * {@code startedAt} <b>no</b> sale del reloj, sale del command. Una caida se
 * detecta despues de haber empezado, y ponerle la hora del servidor acortaria
 * la interrupcion medida —siempre en la direccion de parecer mejores—.
 */
@Observed(name = "external.invoicing.outage.open")
@Service
public class OpenExternalInvoicingOutageService implements OpenExternalInvoicingOutageUseCase {

    private final ExternalInvoicingOutageRepository repository;
    private final Clock clock;

    public OpenExternalInvoicingOutageService(ExternalInvoicingOutageRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ExternalInvoicingOutageDto execute(OpenExternalInvoicingOutageCommand command) {
        ExternalInvoicingOutage outage = ExternalInvoicingOutage.open(command.startedAt(),
                command.causeParty(), command.summary(), command.affectedCompanyCount(),
                command.externalIncidentRef(), LocalDateTime.now(clock));
        return ExternalInvoicingOutageDto.from(repository.save(outage));
    }
}
