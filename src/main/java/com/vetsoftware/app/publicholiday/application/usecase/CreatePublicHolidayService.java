package com.vetsoftware.app.publicholiday.application.usecase;

import com.vetsoftware.app.publicholiday.application.command.CreatePublicHolidayCommand;
import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import com.vetsoftware.app.publicholiday.application.port.in.CreatePublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import com.vetsoftware.app.publicholiday.domain.PublicHolidayAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "publicholiday.create")
@Service
public class CreatePublicHolidayService implements CreatePublicHolidayUseCase {

    private final PublicHolidayRepository repository;
    private final Clock clock;

    public CreatePublicHolidayService(PublicHolidayRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicHolidayDto execute(CreatePublicHolidayCommand command) {
        if (command.holidayDate() != null
                && repository.existsByHolidayDate(command.holidayDate())) {
            throw new PublicHolidayAlreadyExistsException(command.holidayDate());
        }
        PublicHoliday holiday = PublicHoliday.create(command.holidayDate(), command.name(),
                command.nominalDate(), command.moved(), command.legalReference(),
                LocalDateTime.now(clock));
        return PublicHolidayDto.from(repository.save(holiday));
    }
}
