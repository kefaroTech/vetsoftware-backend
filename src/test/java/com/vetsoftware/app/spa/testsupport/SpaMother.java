package com.vetsoftware.app.spa.testsupport;

import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SpaMother {

    public static final AnimalRef FIRULAIS = new AnimalRef(1L, "Firulais", "A-001");
    public static final AnimalRef MICHI = new AnimalRef(2L, "Michi", "A-002");
    public static final CompanyRef CLINICA = new CompanyRef(10L, "Veterinaria de prueba",
            "900123456");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(11L, "Veterinaria ajena",
            "900654321");
    public static final SpaTypeRef BANO_BASICO = new SpaTypeRef(20L, "Baño básico");
    public static final SpaTypeRef CORTE_DE_PELO = new SpaTypeRef(21L, "Corte de pelo");
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SpaMother() {
    }

    public static Spa spaValido() {
        return new Spa(5L, LocalDate.of(2026, 2, 1), BANO_BASICO, "Baño mensual",
                "Shampoo hipoalergenico", "Sin novedades", SpaStatus.AGENDADA, FIRULAIS, CLINICA,
                CREADO, true);
    }
}
