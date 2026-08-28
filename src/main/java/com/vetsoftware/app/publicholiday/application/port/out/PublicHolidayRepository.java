package com.vetsoftware.app.publicholiday.application.port.out;

import com.vetsoftware.app.publicholiday.domain.HolidayCalendar;
import com.vetsoftware.app.publicholiday.domain.PublicHoliday;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PublicHolidayRepository {

    PublicHoliday save(PublicHoliday holiday);

    Optional<PublicHoliday> findById(Long id);

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<PublicHoliday> findByYear(int year);

    PageResult<PublicHoliday> findAll(int page, int pageSize);

    /**
     * El calendario de un tramo, con la lista de anos sembrados <em>dentro del
     * mismo viaje</em>. Van juntos a proposito: separar «dame los festivos» de
     * «dime que anos tienes» permitiria que un tramo vacio se confundiera con un
     * tramo sin festivos, y esa confusion es el fallo que
     * {@code HolidayCalendarGapException} existe para hacer imposible.
     */
    HolidayCalendar loadCalendar(LocalDate from, LocalDate to);
}
