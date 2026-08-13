package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.ListCompanyClinicalEventsUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "clinical.history.list.by.company")
@Service
public class ListCompanyClinicalEventsService implements ListCompanyClinicalEventsUseCase {
    /**
     * Ventana por defecto cuando el cliente no manda rango, y tope de la que pida.
     * BE-06, mismo criterio que la agenda: esto alimenta una vista de calendario,
     * asi que se pinta el periodo entero o no se pinta —paginarlo no significa
     * nada— pero el rango no puede quedar libre. Sin esto,
     * {@code GET /clinical-history} sin parametros devuelve la historia clinica
     * COMPLETA de la empresa: todas las consultas, vacunas, cirugias y
     * hospitalizaciones de todos los pacientes desde el primer dia.
     */
    private static final int DEFAULT_WINDOW_DAYS = 31;
    private static final int MAX_WINDOW_DAYS = 366;

    private final ClinicalEventRepository repository;
    private final Clock clock;

    public ListCompanyClinicalEventsService(ClinicalEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalEventDto> execute(ListCompanyClinicalEventsQuery query) {
        return repository.findByCompany(bound(query)).stream().map(ClinicalEventDto::from).toList();
    }

    /**
     * El propio {@code query} ya rechaza un rango invertido, asi que aqui solo hay
     * que rellenar los extremos ausentes y recortar la amplitud.
     */
    private ListCompanyClinicalEventsQuery bound(ListCompanyClinicalEventsQuery query) {
        LocalDate from = query.from();
        LocalDate to = query.to();

        if (from == null && to == null) {
            from = LocalDate.now(clock);
            to = from.plusDays(DEFAULT_WINDOW_DAYS);
        } else if (from == null) {
            // «Hasta X» mira hacia atras desde X: anclarlo en hoy devolveria vacio
            // siempre que X fuese una fecha pasada, que es cuando se filtra asi.
            from = to.minusDays(DEFAULT_WINDOW_DAYS);
        } else if (to == null) {
            to = from.plusDays(DEFAULT_WINDOW_DAYS);
        }

        if (from.plusDays(MAX_WINDOW_DAYS).isBefore(to)) {
            to = from.plusDays(MAX_WINDOW_DAYS);
        }
        return new ListCompanyClinicalEventsQuery(query.companyId(), query.types(), from, to);
    }
}
