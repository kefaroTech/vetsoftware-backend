package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import com.vetsoftware.app.companyusageevent.domain.UsagePeriodKey;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota un hecho de consumo.
 *
 * <p>
 * <strong>El servicio hace exactamente dos cosas, y ninguna es validar el
 * hecho.</strong> Resuelve el eje contra el catalogo —que es un dato externo,
 * hay que preguntarselo a otra tabla— y sella {@code createdDate} con el reloj
 * inyectado. Todo lo demas —que la rama corresponda al eje, que la clave del
 * periodo tenga una de las cuatro formas, que un hecho con cargo sea
 * facturable— son invariantes y viven en {@link CompanyUsageEvent} y en
 * {@link UsagePeriodKey}. Ahi no se pueden saltar; aqui si, llamando al
 * constructor desde otro sitio.
 *
 * <h2>Los dos relojes, que no son el mismo</h2>
 *
 * <p>
 * {@code LocalDateTime.now(clock)} se usa <b>solo</b> para {@code createdDate},
 * el instante en que se anota. El instante del hecho, {@code occurredAt},
 * <b>llega en el command</b> y no se toca.
 *
 * <p>
 * <strong>Confundirlos rompe la unica proteccion antiduplicados que hay, y lo
 * hace en silencio.</strong> {@code uq_cue_fact} incluye {@code occurred_at};
 * si el medidor reintenta un lote y la segunda pasada escribe la hora actual en
 * vez de la del registro consumido, las dos filas dejan de chocar, el hecho
 * queda duplicado y el excedente se cobra dos veces. No hay excepcion, ni log,
 * ni fila roja: solo una factura mas alta que nadie sabe explicar. Es la razon
 * de que este parametro no se derive aqui aunque seria comodo.
 *
 * <p>
 * <strong>La unicidad no se comprueba preguntando antes, y es a
 * proposito.</strong> La cuida la base sobre una columna generada. Un
 * {@code exists} previo seria una comprobacion que dos pasadas concurrentes
 * pasarian las dos. Aqui el duplicado llega como violacion de integridad, que
 * es la unica respuesta que no miente.
 */
@Observed(name = "company.usage.event.record")
@Service
public class RecordCompanyUsageEventService implements RecordCompanyUsageEventUseCase {

    private final CompanyUsageEventRepository repository;
    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final Clock clock;

    public RecordCompanyUsageEventService(CompanyUsageEventRepository repository,
            LimitDimensionQueryPort limitDimensionQueryPort, Clock clock) {
        this.repository = repository;
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyUsageEventDto execute(RecordCompanyUsageEventCommand command) {
        LimitDimensionRef dimension = limitDimensionQueryPort
                .findByCode(command.limitDimensionCode())
                .orElseThrow(() -> unknownDimension(command));
        CompanyUsageEvent event = CompanyUsageEvent.record(command.companyId(), dimension.id(),
                dimension.branch(), command.usageReferenceId(), command.occurredAt(),
                UsagePeriodKey.of(command.periodKey()), command.billable(),
                LocalDateTime.now(clock));
        return CompanyUsageEventDto.from(repository.save(event));
    }

    /**
     * Un codigo de eje que no esta en el catalogo es un error de programacion o una
     * siembra incompleta, nunca un hecho que se descarta. Se denuncia en voz alta
     * porque tragarselo dejaria de medir una funcion entera sin que nadie lo notara
     * hasta la factura.
     */
    private static IllegalArgumentException unknownDimension(
            RecordCompanyUsageEventCommand command) {
        return new IllegalArgumentException("Unknown limit dimension code: "
                + command.limitDimensionCode() + ". Usage facts resolve their axis against"
                + " limit_dimensions; seed the row before recording consumption for it");
    }
}
