package com.vetsoftware.app.daycare.testsupport;

import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DayCareMother {

    public static final AnimalRef FIRULAIS = new AnimalRef(1L, "Firulais", "A-001");
    public static final AnimalRef MICHI = new AnimalRef(2L, "Michi", "A-002");
    public static final CompanyRef CLINICA = new CompanyRef(10L, "Veterinaria de prueba",
            "900123456");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(11L, "Veterinaria ajena",
            "900654321");
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private DayCareMother() {
    }

    public static DayCare guarderiaValida() {
        return new DayCare(5L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa, plato", "Sin novedades",
                FIRULAIS, CLINICA, CREADO, true);
    }

    public static DayCare hotelValido() {
        return new DayCare(6L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), DayCareType.HOTEL, null, null, MICHI, CLINICA, CREADO,
                true);
    }
}
