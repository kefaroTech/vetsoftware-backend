package com.vetsoftware.app.publicholiday.infrastructure.persistence;

import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.publicholiday.domain.HolidayCalendar;
import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPublicHolidayRepository implements PublicHolidayRepository {

    private final PublicHolidayJpaRepository jpaRepository;
    private final PublicHolidayJpaMapper mapper;

    public JpaPublicHolidayRepository(PublicHolidayJpaRepository jpaRepository,
            PublicHolidayJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PublicHoliday save(PublicHoliday holiday) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(holiday)));
    }

    @Override
    public Optional<PublicHoliday> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByHolidayDate(LocalDate holidayDate) {
        return jpaRepository.existsByHolidayDate(holidayDate);
    }

    @Override
    public List<PublicHoliday> findByYear(int year) {
        return jpaRepository
                .findByEnabledTrueAndHolidayDateBetweenOrderByHolidayDateAsc(
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<PublicHoliday> findAll(int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "holidayDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }

    /**
     * Los festivos del tramo y los anos sembrados, en el mismo metodo. El
     * calendario se construye aqui —no en el dominio— porque el dominio no puede
     * saber que anos hay en la tabla; lo que si hace el dominio es negarse a
     * calcular fuera de lo que este objeto declara cubierto.
     */
    @Override
    public HolidayCalendar loadCalendar(LocalDate from, LocalDate to) {
        Set<LocalDate> observados = jpaRepository
                .findByEnabledTrueAndHolidayDateBetweenOrderByHolidayDateAsc(from, to).stream()
                .map(PublicHolidayJpaEntity::getHolidayDate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> anos = new LinkedHashSet<>(jpaRepository.findCoveredYears());
        return new HolidayCalendar(from, to, anos, observados);
    }
}
