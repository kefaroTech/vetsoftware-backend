package com.vetsoftware.app.promotion.testsupport;

import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Object mother de la feature promotion. Un metodo por variante y valores
 * validos por defecto, igual que AnimalMother.
 */
public final class PromotionMother {

    private PromotionMother() {
    }

    public static final Long COMPANY_ID = 5L;
    public static final Long OTRA_COMPANY_ID = 9L;
    public static final Long CATEGORY_ID = 3L;
    public static final Long PROMOTION_ID = 44L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 0, 0);
    public static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 31, 23, 59);
    public static final LocalDateTime CREADA = LocalDateTime.of(2025, 12, 20, 9, 0);

    /** Promocion activa, persistida (con id), lista para devolver desde un mock. */
    public static Promotion activa(Long id) {
        return new Promotion(id, "Enero perruno", PromotionType.DISCOUNT, ApplicationType.CATEGORY,
                CATEGORY_ID, ValueType.PERCENTAGE, new BigDecimal("15.00"), INICIO, FIN,
                PromotionStatus.ACTIVE, CLINICA, CREADA, true);
    }

    public static Promotion activa() {
        return activa(PROMOTION_ID);
    }
}
