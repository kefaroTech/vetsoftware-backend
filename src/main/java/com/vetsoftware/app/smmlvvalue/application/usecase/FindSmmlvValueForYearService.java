package com.vetsoftware.app.smmlvvalue.application.usecase;

import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.application.port.in.FindSmmlvValueForYearUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.out.SmmlvValueRepository;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValueNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "smmlv.find")
@Service
public class FindSmmlvValueForYearService implements FindSmmlvValueForYearUseCase {

    private final SmmlvValueRepository repository;

    public FindSmmlvValueForYearService(SmmlvValueRepository repository) {
        this.repository = repository;
    }

    /**
     * Devuelve la fila del ano <strong>sea cual sea su estado</strong>. Filtrar
     * aqui las suspendidas dejaria a quien liquida sin cifra alguna, que es peor:
     * la de 2026 esta suspendida y se sigue aplicando. Quien decide que hacer con
     * la disputa es el consumidor, y para eso recibe {@code status}.
     */
    @Override
    public SmmlvValueDto findByYear(int fiscalYear, Long companyId) {
        return repository.findByFiscalYear(fiscalYear).map(SmmlvValueDto::from)
                .orElseThrow(() -> new SmmlvValueNotFoundException(fiscalYear));
    }
}
