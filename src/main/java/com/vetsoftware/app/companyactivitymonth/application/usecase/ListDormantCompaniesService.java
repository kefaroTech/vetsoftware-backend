package com.vetsoftware.app.companyactivitymonth.application.usecase;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListDormantCompaniesUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las clinicas que apenas entraron en un mes.
 *
 * <p>
 * <strong>El umbral se acota aqui, y el motivo es el indice.</strong>
 * {@code ix_cam_dormant (period_key, active_days)} sirve «una igualdad y un
 * rango, en ese orden»; un umbral negativo no devolveria nada y uno por encima
 * de 31 devolveria la tabla entera del mes, que no es un barrido de dormidos
 * sino un listado completo disfrazado. Rechazarlo en voz alta es mas util que
 * servir un informe que dice que todo el mundo esta dormido.
 *
 * <p>
 * <strong>El periodo se valida antes de consultar</strong>: un
 * {@code period_key} mal formado no existe en la tabla —lo impide
 * {@code chk_cam_period_key}—, asi que la consulta saldria vacia y se leeria
 * como «no hay dormidos», que es la respuesta mas peligrosa que puede dar esta
 * pantalla.
 */
@Observed(name = "companyactivitymonth.dormant")
@Service
public class ListDormantCompaniesService implements ListDormantCompaniesUseCase {

    /**
     * El techo de {@code chk_cam_active_days}: por encima no hay rango que acotar.
     */
    private static final int MAX_ACTIVE_DAYS = 31;

    private final CompanyActivityMonthRepository repository;

    public ListDormantCompaniesService(CompanyActivityMonthRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyActivityMonthDto> listDormant(String periodKey,
            int activeDaysThreshold, int page, int pageSize) {
        if (activeDaysThreshold < 0 || activeDaysThreshold > MAX_ACTIVE_DAYS) {
            throw new IllegalArgumentException("activeDaysThreshold must be between 0 and "
                    + MAX_ACTIVE_DAYS + ": " + activeDaysThreshold);
        }
        return repository.findDormant(new ActivityPeriodKey(periodKey).value(), activeDaysThreshold,
                page, pageSize).map(CompanyActivityMonthDto::from);
    }
}
