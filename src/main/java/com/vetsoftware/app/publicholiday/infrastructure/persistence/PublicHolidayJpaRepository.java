package com.vetsoftware.app.publicholiday.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicHolidayJpaRepository extends JpaRepository<PublicHolidayJpaEntity, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<PublicHolidayJpaEntity> findByEnabledTrueAndHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate from, LocalDate to);

    /**
     * Los anos con al menos un festivo sembrado.
     *
     * <p>
     * Es un {@code SELECT}, no una mutacion: no entra en
     * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} ni en
     * {@code UPDATE_MASIVO_MUEVE_LA_VERSION}. Y no lleva filtro de empresa porque
     * la tabla no la tiene: un festivo nacional es el mismo para todos los tenants.
     */
    @Query("select distinct year(h.holidayDate) from PublicHolidayJpaEntity h "
            + "where h.enabled = true")
    List<Integer> findCoveredYears();
}
