package com.vetsoftware.app.companyactivitymonth.application.usecase;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.FindCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee una fila de actividad, por identificador o por el par empresa-mes.
 *
 * <p>
 * <strong>La carga por id es ancha —sin empresa— y eso es correcto
 * aqui.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} exime al servicio que
 * solo alcanza {@code SYSTEM}, y este lo es: un principal {@code SYSTEM} no
 * tiene empresa con la que acotar, asi que la variante acotada ni siquiera
 * existe en el puerto de salida. La regla se cumple por construccion, no por
 * exencion escrita.
 *
 * <p>
 * <strong>El periodo se valida antes de bajar a la base.</strong> Construir el
 * {@link ActivityPeriodKey} convierte un {@code 2026-13} en un 400 con su
 * mensaje, en vez de en una consulta que no devuelve nada y se lee como «esa
 * clinica no tuvo actividad».
 */
@Observed(name = "companyactivitymonth.find")
@Service
public class FindCompanyActivityMonthService implements FindCompanyActivityMonthUseCase {

    private final CompanyActivityMonthRepository repository;

    public FindCompanyActivityMonthService(CompanyActivityMonthRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyActivityMonthDto findById(Long id) {
        return repository.findById(id).map(CompanyActivityMonthDto::from)
                .orElseThrow(() -> new CompanyActivityMonthNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyActivityMonthDto findByCompanyIdAndPeriodKey(Long companyId, String periodKey) {
        String validated = new ActivityPeriodKey(periodKey).value();
        return repository.findByCompanyIdAndPeriodKey(companyId, validated)
                .map(CompanyActivityMonthDto::from)
                .orElseThrow(() -> new CompanyActivityMonthNotFoundException(companyId, validated));
    }
}
