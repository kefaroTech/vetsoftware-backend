package com.vetsoftware.app.companyactivitymonth.application.usecase;

import com.vetsoftware.app.companyactivitymonth.application.command.UpdateCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.UpdateCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcula el mes en curso sobre su propia fila.
 *
 * <p>
 * <strong>Leer, modificar y guardar — y ese camino es toda la
 * proteccion.</strong> El recalculo va por la entidad gestionada, que es el
 * unico camino donde Hibernate compara {@code version} en el {@code WHERE} y la
 * incrementa en el {@code SET}. Escrito como una {@code @Query} de
 * {@code UPDATE} —que seria mas corto y parece equivalente— iria directo a la
 * base sin comprobar ni incrementar nada, y el {@code save} concurrente que
 * llegara con la version vieja casaria igual y pisaria el recalculo: sin
 * excepcion, sin log y sin 409 ({@code UPDATE_MASIVO_MUEVE_LA_VERSION},
 * incidencia #53). Es exactamente el modo de fallo que esta tabla no se puede
 * permitir, porque su fila viva se reescribe todos los dias.
 *
 * <p>
 * <strong>Los tres campos que no se tocan</strong> —empresa, periodo y fecha de
 * creacion— los conserva {@link CompanyActivityMonth#recalculate}, que ademas
 * arrastra la {@code version}. Sin ella la entidad pareceria transitoria y el
 * {@code save} escribiria una fila nueva que chocaria contra
 * {@code uq_cam_month}.
 *
 * <p>
 * <strong>{@code @Transactional} porque son dos operaciones de
 * repositorio</strong> —{@code findById} y {@code save}— y entre las dos tiene
 * que haber una sola unidad de trabajo: es lo que hace que el chequeo de
 * version del flush ocurra dentro de la misma transaccion que la lectura.
 */
@Observed(name = "companyactivitymonth.update")
@Service
public class UpdateCompanyActivityMonthService implements UpdateCompanyActivityMonthUseCase {

    private final CompanyActivityMonthRepository repository;

    public UpdateCompanyActivityMonthService(CompanyActivityMonthRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyActivityMonthDto execute(UpdateCompanyActivityMonthCommand command) {
        CompanyActivityMonth current = repository.findById(command.id())
                .orElseThrow(() -> new CompanyActivityMonthNotFoundException(command.id()));
        CompanyActivityMonth recalculated = current.recalculate(command.commercialState(),
                command.activeDays(), command.activeUsers(), command.recordsCreated(),
                command.mrrSnapshot());
        return CompanyActivityMonthDto.from(repository.save(recalculated));
    }
}
