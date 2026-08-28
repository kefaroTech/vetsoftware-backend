package com.vetsoftware.app.documentwithholding.testsupport;

import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Retenciones de ejemplo para los tests de esta rodaja.
 *
 * <p>
 * <b>Todos los valores son distintos entre si a proposito.</b> La entidad tiene
 * catorce componentes, tres de ellos {@code BigDecimal} y dos de ellos fechas:
 * cruzar {@code taxableBase} con {@code amount}, o {@code practicedOn} con
 * {@code createdDate}, compila sin una queja y solo se descubre cuando una
 * declaracion no cuadra. Con importes como {@code 1000000.00} y
 * {@code 690000.00} repetidos, un mapper cruzado pasaria en verde.
 *
 * <p>
 * <b>La tarifa por defecto es una de verdad y es por mil.</b> El 6,9 por mil de
 * industria y comercio se escribe {@code 0.690000} en porcentaje. Es el valor
 * que caza el error mas caro de esta feature —guardar {@code 6.9} porque «la
 * tarifa es 6,9»— y por eso vive aqui y no en un caso suelto.
 */
public final class DocumentWithholdingMother {

    public static final Long EMPRESA = 900L;
    public static final Long OTRA_EMPRESA = 901L;
    public static final Long FACTURA = 8400L;
    public static final Long CERTIFICADO = 8410L;

    /** 6,9 por mil expresado en porcentaje, que es lo que la columna guarda. */
    public static final BigDecimal TARIFA_ICA_POR_MIL = new BigDecimal("0.690000");

    /** 2,5 % de retefuente por servicios: tarifa nacional tipica. */
    public static final BigDecimal TARIFA_RENTA = new BigDecimal("2.500000");

    public static final BigDecimal BASE_GRAVABLE = new BigDecimal("1234567.89");

    /** El 2,5 % de {@link #BASE_GRAVABLE}, redondeado al peso mas cercano. */
    public static final BigDecimal RETENIDO = new BigDecimal("30864.20");

    /**
     * El 0,69 % de {@link #BASE_GRAVABLE}: lo que de verdad sale de aplicar 6,9 por
     * mil. <b>Este numero es el caso testigo de la unidad.</b> Quien confunda la
     * tarifa por mil con un porcentaje guardaria {@code 6.900000} y retendria
     * {@code 85172.18} — casi cien veces mas—; quien la trate como fraccion
     * guardaria {@code 0.0069} y retendria {@code 85.17}. Solo el valor correcto da
     * esta cifra.
     */
    public static final BigDecimal RETENIDO_ICA = new BigDecimal("8518.52");

    /**
     * El 15 % de {@link #BASE_GRAVABLE}, que en IVA se aplica sobre el impuesto.
     */
    public static final BigDecimal RETENIDO_IVA = new BigDecimal("185185.18");

    public static final int ANO_GRAVABLE = 2026;
    public static final String MUNICIPIO_MEDELLIN = "05001";

    public static final LocalDate PRACTICADA_EL = LocalDate.of(2026, 3, 5);
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);

    private DocumentWithholdingMother() {
    }

    /** Retencion de renta, anual, sin municipio y todavia sin respaldo. */
    public static DocumentWithholding renta() {
        return DocumentWithholding.register(EMPRESA, FACTURA, WithholdingType.INCOME_TAX,
                BASE_GRAVABLE, TARIFA_RENTA, RETENIDO, null, ANO_GRAVABLE, "2026-A", PRACTICADA_EL,
                CREADA_EL);
    }

    /** Retencion de IVA, bimestral, sin municipio. */
    public static DocumentWithholding iva() {
        return DocumentWithholding.register(EMPRESA, FACTURA, WithholdingType.VAT, BASE_GRAVABLE,
                new BigDecimal("15.000000"), RETENIDO_IVA, null, ANO_GRAVABLE, "2026-B02",
                PRACTICADA_EL, CREADA_EL);
    }

    /** Retencion de industria y comercio: bimestral, municipal y por mil. */
    public static DocumentWithholding ica() {
        return DocumentWithholding.register(EMPRESA, FACTURA, WithholdingType.ICA, BASE_GRAVABLE,
                TARIFA_ICA_POR_MIL, RETENIDO_ICA, MUNICIPIO_MEDELLIN, ANO_GRAVABLE, "2026-B02",
                PRACTICADA_EL, CREADA_EL);
    }

    /** Retencion ya escrita en la base: con id, con version y sin certificado. */
    public static DocumentWithholding yaRegistrada(Long id) {
        return new DocumentWithholding(id, EMPRESA, FACTURA, WithholdingType.INCOME_TAX,
                BASE_GRAVABLE, TARIFA_RENTA, RETENIDO, null, ANO_GRAVABLE, "2026-A", PRACTICADA_EL,
                null, CREADA_EL, 0L);
    }

    /** La misma, ya respaldada por su certificado. */
    public static DocumentWithholding yaCertificada(Long id, Long certificateId) {
        return new DocumentWithholding(id, EMPRESA, FACTURA, WithholdingType.INCOME_TAX,
                BASE_GRAVABLE, TARIFA_RENTA, RETENIDO, null, ANO_GRAVABLE, "2026-A", PRACTICADA_EL,
                certificateId, CREADA_EL, 3L);
    }

    public static RegisterDocumentWithholdingCommand comandoDeRenta() {
        return new RegisterDocumentWithholdingCommand(EMPRESA, FACTURA, WithholdingType.INCOME_TAX,
                BASE_GRAVABLE, TARIFA_RENTA, RETENIDO, null, ANO_GRAVABLE, "2026-A", PRACTICADA_EL);
    }

    public static RegisterDocumentWithholdingCommand comandoDeIca() {
        return new RegisterDocumentWithholdingCommand(EMPRESA, FACTURA, WithholdingType.ICA,
                BASE_GRAVABLE, TARIFA_ICA_POR_MIL, RETENIDO_ICA, MUNICIPIO_MEDELLIN, ANO_GRAVABLE,
                "2026-B02", PRACTICADA_EL);
    }

    public static DocumentWithholdingDto dto(Long id, Long certificateId) {
        return new DocumentWithholdingDto(id, EMPRESA, FACTURA, WithholdingType.ICA, BASE_GRAVABLE,
                TARIFA_ICA_POR_MIL, RETENIDO_ICA, MUNICIPIO_MEDELLIN, ANO_GRAVABLE, "2026-B02",
                PRACTICADA_EL, certificateId, CREADA_EL);
    }
}
