package com.vetsoftware.app.withholdingcertificate.testsupport;

import com.vetsoftware.app.withholdingcertificate.application.command.RegisterWithholdingCertificateCommand;
import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature. Vive dentro de {@code withholdingcertificate} porque
 * el vertical slicing aplica igual en {@code src/test}.
 *
 * <p>
 * <b>Todas las fechas y todos los importes son distintos entre si a
 * proposito.</b> Este agregado tiene cuatro fechas -expedicion, vencimiento
 * legal, recepcion y creacion- y dos importes decimales; con valores repetidos,
 * un mapper que cruzara {@code issuedOn} con {@code legalDeadlineOn} pasaria
 * todas las aserciones.
 */
public final class WithholdingCertificateMother {

    public static final Long COMPANY_ID = 900L;
    public static final Long OTRA_COMPANY_ID = 901L;

    public static final String NIT_DEL_CLIENTE = "830012345";
    public static final Integer ANO_GRAVABLE = 2025;

    /** El certificado del ano gravable 2025 se expide a comienzos de 2026. */
    public static final LocalDate EXPEDIDO_EL = LocalDate.of(2026, 2, 10);

    /** Ultimo dia habil de marzo de 2026: 31 de marzo, martes. */
    public static final LocalDate VENCE_EL = LocalDate.of(2026, 3, 31);

    public static final LocalDate RECIBIDO_EL = LocalDate.of(2026, 3, 18);
    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 2, 12, 9, 15, 30);

    public static final BigDecimal IMPORTE_CERTIFICADO = new BigDecimal("1847320.55");

    /**
     * <b>6,9 por mil expresado como porcentaje: 0,69.</b> Es la tarifa real de
     * industria y comercio de varios municipios y el motivo de que la columna lleve
     * seis decimales; si alguien reinterpretara la unidad como fraccion, el numero
     * seguiria entrando y el importe calculado saldria cien veces menor.
     */
    public static final BigDecimal TARIFA_ICA_POR_MIL = new BigDecimal("0.690000");

    public static final BigDecimal TARIFA_RENTA = new BigDecimal("2.500000");

    private WithholdingCertificateMother() {
    }

    /** Retencion en la fuente del ano gravable 2025, aun sin recibir. */
    public static WithholdingCertificate deRenta() {
        return WithholdingCertificate.register(COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, CREADO_EL);
    }

    /** Retencion de ICA del tercer bimestre, con la tarifa por mil de verdad. */
    public static WithholdingCertificate deIca() {
        return WithholdingCertificate.register(COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0002",
                WithholdingType.ICA, ANO_GRAVABLE, "2025-B03", TARIFA_ICA_POR_MIL,
                new BigDecimal("412900.10"), EXPEDIDO_EL, VENCE_EL, CREADO_EL);
    }

    /** El mismo certificado de renta, ya con id: sirve de fila persistida. */
    public static WithholdingCertificate conId(Long id) {
        return new WithholdingCertificate(id, COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL);
    }

    /** Certificado que ya llego, con su fecha y su archivo. */
    public static WithholdingCertificate recibido(Long id) {
        return new WithholdingCertificate(id, COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, RECIBIDO_EL,
                "s3://certificados/2025/CERT-2025-0001.pdf", null, null, CREADO_EL);
    }

    /** Certificado que no llego y se acredita con el comprobante de pago. */
    public static WithholdingCertificate conSustituto(Long id) {
        return new WithholdingCertificate(id, COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null,
                SubstituteEvidenceKind.PAYMENT_RECEIPT, "s3://pagos/2025/REC-77120.pdf", CREADO_EL);
    }

    public static RegisterWithholdingCertificateCommand comandoDeRegistro() {
        return new RegisterWithholdingCertificateCommand(COMPANY_ID, NIT_DEL_CLIENTE,
                "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL);
    }
}
