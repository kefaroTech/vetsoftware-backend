package com.vetsoftware.app.service.testsupport;

import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.service.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Object mother de la feature {@code service}: valores válidos por defecto para
 * no repetir los 13 argumentos del constructor en cada test.
 */
public final class ServiceMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long SERVICE_CATEGORY_ID = 20L;
    public static final Long TAX_ID = 30L;
    public static final Long SERVICE_ID = 1L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Veterinaria de prueba",
            "900123456");
    public static final ServiceCategoryRef CONSULTAS = new ServiceCategoryRef(SERVICE_CATEGORY_ID,
            "Consultas");
    public static final TaxRef IVA_19 = new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00"));
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 8, 0);

    private ServiceMother() {
    }

    public static Service consultaGeneral() {
        return new Service(SERVICE_ID, "Consulta general", new BigDecimal("50000.00"),
                TaxTreatment.GRAVADO, "Consulta veterinaria estandar", CONSULTAS, IVA_19, CLINICA,
                CREADO, null, null, 0L, true);
    }

    public static Service exenta() {
        return new Service(2L, "Vacunacion antirrabica", new BigDecimal("30000.00"),
                TaxTreatment.EXENTO, null, CONSULTAS, null, CLINICA, CREADO, null, null, 0L, true);
    }
}
