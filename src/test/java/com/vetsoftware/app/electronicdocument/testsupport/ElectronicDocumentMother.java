package com.vetsoftware.app.electronicdocument.testsupport;

import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures de la feature de facturacion electronica. Todo se construye de
 * verdad (nada mockeado): un documento con invariantes saltadas no probaria lo
 * que produccion acepta.
 *
 * <p>
 * La factura canonica es una linea gravada al 19 % de base 1.000 → IVA 190 →
 * total 1.190, pagada en efectivo. Los montos son "redondos" a proposito: un
 * fallo de aritmetica se lee de un vistazo.
 */
public final class ElectronicDocumentMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long OTHER_COMPANY_ID = 77L;
    public static final Long BRANCH_ID = 7L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long OPEN_ACCOUNT_ID = 100L;
    public static final BigDecimal UVT = new BigDecimal("49799");
    public static final String CUFE = "CUFE-ORIGINAL-1";

    private ElectronicDocumentMother() {
    }

    public static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    public static IssuerSnapshot issuer() {
        return new IssuerSnapshot("NIT", "900123456", "7", "Veterinaria Vet SAS", "RESPONSABLE",
                "facturacion@vet.co", List.of("O-13", "O-15"));
    }

    public static CustomerSnapshot customer() {
        return new CustomerSnapshot("CEDULA_CIUDADANIA", "1020304050", "3", "NATURAL",
                "Ana Maria Perez", "Ana Perez", "ana@correo.co", "05001",
                TaxRegime.NO_RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA);
    }

    /** Linea gravada con IVA: base + impuesto a la tarifa indicada. */
    public static ElectronicDocumentLine ivaLine(int number, String base, String rate, String tax) {
        return new ElectronicDocumentLine(null, number, "Gravado " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.GRAVADO, TaxScheme.IVA, bd(rate), bd(tax),
                bd(base).add(bd(tax)));
    }

    /** Linea de impuesto al consumo: base de retencion si, IVA generado no. */
    public static ElectronicDocumentLine incLine(int number, String base, String rate, String tax) {
        return new ElectronicDocumentLine(null, number, "INC " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.INC, TaxScheme.INC, bd(rate), bd(tax),
                bd(base).add(bd(tax)));
    }

    /**
     * Exento: conserva esquema IVA a tarifa cero (el XML lo distingue de EXCLUIDO).
     */
    public static ElectronicDocumentLine exentoLine(int number, String base) {
        return new ElectronicDocumentLine(null, number, "Exento " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.EXENTO, TaxScheme.IVA, BigDecimal.ZERO,
                BigDecimal.ZERO, bd(base));
    }

    /** Excluido / item libre del POS: sin esquema tributario. */
    public static ElectronicDocumentLine excluidoLine(int number, String base) {
        return new ElectronicDocumentLine(null, number, "Excluido " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.EXCLUIDO, null, null, BigDecimal.ZERO, bd(base));
    }

    /** La linea canonica: base 1.000, IVA 19 % = 190, total 1.190. */
    public static List<ElectronicDocumentLine> unaLineaGravada() {
        return List.of(ivaLine(1, "1000.00", "19", "190.00"));
    }

    public static List<ElectronicDocumentPayment> efectivo(String amount) {
        return List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, bd(amount)));
    }

    /**
     * Factura PENDIENTE recien construida (sin id, sin numeracion ni sellos), tal
     * como sale de {@code createPending}.
     */
    public static ElectronicDocument facturaPendiente() {
        return ElectronicDocument.createPending(COMPANY_ID, OPEN_ACCOUNT_ID,
                ElectronicDocumentType.FE_VENTA, issuer(), customer(), unaLineaGravada(),
                efectivo("1190.00"), PaymentForm.CONTADO, false, null, null, null, UVT, "req-1",
                EMPLOYEE_ID, BRANCH_ID);
    }

    /**
     * Documento equivalente POS PENDIENTE (numeracion del proveedor, sin cuenta).
     */
    public static ElectronicDocument posPendiente() {
        return ElectronicDocument.createPending(COMPANY_ID, null,
                ElectronicDocumentType.DOC_EQUIV_POS, issuer(), CustomerSnapshot.finalConsumer(),
                unaLineaGravada(), efectivo("1190.00"), PaymentForm.CONTADO, false, null, null,
                null, UVT, "req-pos-1", EMPLOYEE_ID, BRANCH_ID);
    }

    /** Factura persistida y PENDIENTE (ya con id, aun sin numerar). */
    public static ElectronicDocument facturaPendienteConId(Long id) {
        return documento(id, COMPANY_ID, ElectronicDocumentType.FE_VENTA, DianStatus.PENDIENTE,
                null, null, null, null, false, OPEN_ACCOUNT_ID);
    }

    /**
     * Factura VALIDADA por la DIAN, con numero fiscal y CUFE: la que admite notas.
     */
    public static ElectronicDocument facturaValidada(Long id) {
        return facturaValidada(id, COMPANY_ID);
    }

    public static ElectronicDocument facturaValidada(Long id, Long companyId) {
        return documento(id, companyId, ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO,
                "SETP", 990L, CUFE, null, false, OPEN_ACCOUNT_ID);
    }

    /** Factura VALIDADA de venta directa de POS (sin cuenta abierta). */
    public static ElectronicDocument posValidado(Long id) {
        return documento(id, COMPANY_ID, ElectronicDocumentType.DOC_EQUIV_POS, DianStatus.VALIDADO,
                "POS", 55L, null, "CUDE-POS-1", false, null);
    }

    /** Factura VALIDADA que ya fue anulada por una nota credito previa. */
    public static ElectronicDocument facturaYaReversada(Long id) {
        return documento(id, COMPANY_ID, ElectronicDocumentType.FE_VENTA, DianStatus.VALIDADO,
                "SETP", 990L, CUFE, null, true, OPEN_ACCOUNT_ID);
    }

    /** Nota credito TOTAL (anulacion) sobre {@link #facturaValidada(Long)}. */
    public static ElectronicDocument notaCreditoTotal(Long id) {
        return nota(id, ElectronicDocumentType.NOTA_CREDITO, DianStatus.VALIDADO, bd("1190.00"));
    }

    /** Nota credito PARCIAL: no cubre el total de la factura referenciada. */
    public static ElectronicDocument notaCreditoParcial(Long id) {
        return nota(id, ElectronicDocumentType.NOTA_CREDITO, DianStatus.VALIDADO, bd("500.00"));
    }

    public static ElectronicDocument notaCreditoPendiente(Long id) {
        return nota(id, ElectronicDocumentType.NOTA_CREDITO, DianStatus.PENDIENTE, bd("1190.00"));
    }

    public static ElectronicDocument notaDebito(Long id) {
        return nota(id, ElectronicDocumentType.NOTA_DEBITO, DianStatus.VALIDADO, bd("1190.00"));
    }

    private static ElectronicDocument nota(Long id, ElectronicDocumentType type, DianStatus status,
            BigDecimal payable) {
        DocumentReference reference = new DocumentReference(CUFE, "SETP", 990L,
                LocalDate.of(2026, 3, 10));
        return new ElectronicDocument(id, COMPANY_ID, OPEN_ACCOUNT_ID, type, "NC", 12L,
                "18760000002", LocalDate.of(2026, 3, 12), "11:00:00-05:00", null, "CUDE-NOTA-1",
                "uuid-nota", null, null, null, null, status, null, issuer(), customer(),
                payable.subtract(bd("190.00")).max(BigDecimal.ZERO),
                payable.subtract(bd("190.00")).max(BigDecimal.ZERO), payable, payable,
                PaymentForm.CONTADO, unaLineaGravada(), efectivo("1190.00"),
                LocalDateTime.of(2026, 3, 12, 11, 0), null, true, reference, "2",
                "Anulacion de factura electronica", false, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, EMPLOYEE_ID, BRANCH_ID);
    }

    /**
     * Constructor completo con las variantes que los tests necesitan mover. Se usa
     * el constructor publico (no {@code createPending}) porque los casos de uso
     * trabajan sobre documentos YA persistidos: con id, numero y estado.
     */
    public static ElectronicDocument documento(Long id, Long companyId, ElectronicDocumentType type,
            DianStatus status, String prefix, Long consecutive, String cufe, String cude,
            boolean reversed, Long openAccountId) {
        return new ElectronicDocument(id, companyId, openAccountId, type, prefix, consecutive,
                consecutive == null ? null : "18760000001", LocalDate.of(2026, 3, 10),
                "10:15:00-05:00", cufe, cude, cufe == null ? null : "uuid-1", null, null, null,
                null, status,
                status == DianStatus.VALIDADO ? LocalDateTime.of(2026, 3, 10, 10, 16) : null,
                issuer(), customer(), bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                PaymentForm.CONTADO, unaLineaGravada(), efectivo("1190.00"),
                LocalDateTime.of(2026, 3, 10, 10, 15), null, true, null, null, null, reversed,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, EMPLOYEE_ID, BRANCH_ID);
    }
}
