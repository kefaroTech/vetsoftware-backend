package com.vetsoftware.app.publicholiday.application.usecase;

import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import com.vetsoftware.app.publicholiday.application.port.in.FindPublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.publicholiday.domain.PublicHolidayNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "publicholiday.find")
@Service
public class FindPublicHolidayService implements FindPublicHolidayUseCase {

    private final PublicHolidayRepository repository;

    public FindPublicHolidayService(PublicHolidayRepository repository) {
        this.repository = repository;
    }

    /**
     * El {@code companyId} llega para que el gate del puerto pueda exigirlo, no
     * para filtrar: {@code public_holidays} no tiene empresa y un festivo es el
     * mismo para todos los tenants. Acotar la consulta por una empresa que la tabla
     * no guarda devolveria cero filas siempre.
     */
    @Override
    public PublicHolidayDto findById(Long id, Long companyId) {
        return repository.findById(id).map(PublicHolidayDto::from)
                .orElseThrow(() -> new PublicHolidayNotFoundException(id));
    }
}
