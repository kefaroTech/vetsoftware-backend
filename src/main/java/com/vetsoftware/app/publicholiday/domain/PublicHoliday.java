package com.vetsoftware.app.publicholiday.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un festivo colombiano, tal como la ley lo hace <em>observable</em>.
 *
 * <p>
 * <strong>La identidad es {@link #getHolidayDate() la fecha observada}, no la
 * nominal</strong>, y esa no es una preferencia de modelado sino lo unico que
 * hace escribible el calendario. La Ley 51 de 1983 traslada diez efemerides al
 * lunes siguiente, y dos efemerides distintas pueden aterrizar en el mismo
 * lunes: el 1 de julio de 2019 lo hicieron a la vez el Sagrado Corazon
 * (Pascua+68) y San Pedro y San Pablo (29 de junio, sabado, trasladado). Por
 * eso {@code uq_public_holidays_date} cae sobre {@code holiday_date} y
 * {@code nominal_date} no lleva unicidad: una unicidad sobre la nominal habria
 * hecho ese ano inescribible.
 *
 * <p>
 * <strong>Sin {@code @Version}</strong> (exenta {@code E1_APPEND_ONLY}): un
 * festivo se siembra por ano y no se reescribe. Corregir un ano es publicar la
 * fila que falta, no editar la que hay.
 *
 * <p>
 * Las invariantes de aqui son el espejo de {@code chk_public_holidays_move} y
 * {@code chk_public_holidays_range}: si la base va a rechazar la fila, este
 * constructor la rechaza antes y con un mensaje que nombra el campo.
 */
public class PublicHoliday {

    /** Espejo de {@code chk_public_holidays_range}, extremo inferior. */
    public static final LocalDate MIN_DATE = LocalDate.of(2020, 1, 1);

    /** Espejo de {@code chk_public_holidays_range}, extremo superior. */
    public static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    private static final int MAX_NAME = 120;
    private static final int MAX_LEGAL_REFERENCE = 255;

    private final Long id;
    private final LocalDate holidayDate;
    private final String name;
    private final LocalDate nominalDate;
    private final boolean moved;
    private final String legalReference;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public PublicHoliday(Long id, LocalDate holidayDate, String name, LocalDate nominalDate,
            boolean moved, String legalReference, LocalDateTime createdDate, boolean enabled) {
        if (holidayDate == null) {
            throw new IllegalArgumentException("holidayDate is required");
        }
        if (holidayDate.isBefore(MIN_DATE) || holidayDate.isAfter(MAX_DATE)) {
            throw new IllegalArgumentException(
                    "holidayDate must be between " + MIN_DATE + " and " + MAX_DATE);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > MAX_NAME) {
            throw new IllegalArgumentException("name must be " + MAX_NAME + " chars or less");
        }
        if (legalReference == null || legalReference.isBlank()) {
            throw new IllegalArgumentException("legalReference is required");
        }
        if (legalReference.length() > MAX_LEGAL_REFERENCE) {
            throw new IllegalArgumentException(
                    "legalReference must be " + MAX_LEGAL_REFERENCE + " chars or less");
        }
        if (moved && (nominalDate == null || !nominalDate.isBefore(holidayDate))) {
            throw new IllegalArgumentException(
                    "a moved holiday needs a nominalDate strictly before holidayDate");
        }
        if (!moved && nominalDate != null && !nominalDate.isEqual(holidayDate)) {
            throw new IllegalArgumentException(
                    "a holiday that was not moved cannot have a different nominalDate");
        }
        this.id = id;
        this.holidayDate = holidayDate;
        this.name = name;
        this.nominalDate = nominalDate;
        this.moved = moved;
        this.legalReference = legalReference;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    /**
     * Alta de un festivo. {@code createdDate} llega de fuera —del reloj inyectado
     * del servicio, con la zona del negocio— y no de {@code LocalDateTime.now()}:
     * un festivo sembrado a las 19:30 de Bogota no puede quedar fechado al dia
     * siguiente.
     */
    public static PublicHoliday create(LocalDate holidayDate, String name, LocalDate nominalDate,
            boolean moved, String legalReference, LocalDateTime createdDate) {
        return new PublicHoliday(null, holidayDate, name, nominalDate, moved, legalReference,
                createdDate, true);
    }

    public Long getId() {
        return id;
    }

    /**
     * La fecha en la que el descanso se disfruta. Es la que decide si un dia es
     * habil.
     */
    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getName() {
        return name;
    }

    /** La efemeride antes del traslado. Documenta, no identifica. */
    public LocalDate getNominalDate() {
        return nominalDate;
    }

    public boolean isMoved() {
        return moved;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
