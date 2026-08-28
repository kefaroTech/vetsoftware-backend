package com.vetsoftware.app.publicholiday.application.usecase;

import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import com.vetsoftware.app.publicholiday.application.port.in.ListPublicHolidaysUseCase;
import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "publicholiday.list")
@Service
public class ListPublicHolidaysService implements ListPublicHolidaysUseCase {

    private final PublicHolidayRepository repository;

    public ListPublicHolidaysService(PublicHolidayRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PublicHolidayDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAll(page, pageSize).map(PublicHolidayDto::from);
    }

    @Override
    public List<PublicHolidayDto> listByYear(int year, Long companyId) {
        return repository.findByYear(year).stream().map(PublicHolidayDto::from).toList();
    }
}
