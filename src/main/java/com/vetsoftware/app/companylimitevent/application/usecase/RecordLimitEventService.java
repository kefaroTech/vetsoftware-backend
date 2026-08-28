package com.vetsoftware.app.companylimitevent.application.usecase;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe el hecho de cupo en una transacción propia.
 *
 * <h2>Por qué {@code REQUIRES_NEW} y no otra cosa</h2>
 *
 * <p>
 * El hecho más valioso de esta bitácora es el portazo, y el portazo
 * <em>revierte la operación</em>. Con la propagación por defecto, el
 * {@code INSERT} viviría dentro de la transacción que está a punto de
 * deshacerse y se iría con ella: la fila que existe para demostrar el límite
 * desaparecería justo en el caso que hay que demostrar. Con propagación
 * independiente, el hecho se confirma solo y sobrevive.
 *
 * <p>
 * <strong>Dos consecuencias que hay que respetar al usarlo.</strong> La
 * primera: este servicio se invoca <em>desde otro bean</em>, nunca desde un
 * método de esta misma clase — la propagación la aplica el proxy, y una llamada
 * interna lo esquiva sin avisar y sin fallar, que es la peor forma de romperlo.
 * La segunda: la transacción externa sigue viva mientras esta corre, así que el
 * hecho tiene que ser corto y no puede tocar las filas que la externa tiene
 * bloqueadas — de ahí que todos sus números lleguen ya resueltos en el command
 * y no se consulten aquí.
 */
@Service
public class RecordLimitEventService implements RecordLimitEventUseCase {

    private final CompanyLimitEventRepository repository;
    private final Clock clock;

    public RecordLimitEventService(CompanyLimitEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompanyLimitEventDto execute(RecordLimitEventCommand command) {
        CompanyLimitEvent event = CompanyLimitEvent.record(command.companyId(),
                command.limitDimensionId(), command.eventType(), command.limitQuantity(),
                command.usedQuantity(), command.requestedDelta(), command.limitSource(),
                command.overrideId(), command.actor(), command.reasonCode(), command.reason(),
                LocalDateTime.now(clock));
        return CompanyLimitEventDto.from(repository.append(event));
    }
}
