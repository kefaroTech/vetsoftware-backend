package com.vetsoftware.app.electronicdocument.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Documento electronico (cabecera + lineas + pagos): la entidad fiscal
 * INMUTABLE. Se construye congelando una cuenta cerrada (ver
 * {@link #createPending}). En F2 nace PENDIENTE, sin numero
 * (prefix/consecutive) ni sellos DIAN (cufe/cude/uuid/qr/xml/pdf): los llenan
 * F4 y F3. No expone update ni soft-delete: la unica correccion valida es una
 * nota credito/debito (F5).
 */
public class ElectronicDocument {
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final String COLOMBIA_OFFSET = "-05:00";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Long id;
    private final Long companyId;
    private final Long openAccountId;
    // Sucursal (sede) emisora del documento. Id pelado sin FK JPA, igual que
    // openAccountId/issuedByEmployeeId
    // (cruce entre features por id). Metadato fiscal: qué sede emitió la
    // venta/nota.
    private final Long branchId;
    private final ElectronicDocumentType documentType;

    // Campos del ciclo de vida DIAN: NO finales. Los rellena la transmisión (F3)
    // mediante la máquina
    // de estados forward-only (markValidated/markRejected/markContingency). El
    // contenido fiscal
    // (líneas,
    // totales, snapshots) sí es inmutable.
    private String prefix;
    private Long consecutive;
    // Numero de la resolucion DIAN que autoriza el consecutivo. Se asigna al emitir
    // (F4) desde la
    // NumberingResolution activa de la empresa, junto con prefix/consecutive.
    private String resolutionNumber;

    private final LocalDate issueDate;
    private final String issueTime;

    private String cufe;
    private String cude;
    private String uuid;
    private String qrData;
    private String qrUrl;
    private String xmlSigned;
    private String pdfRepresentation;
    private DianStatus dianStatus;
    private LocalDateTime dianValidationDate;

    private final IssuerSnapshot issuer;
    private final CustomerSnapshot customer;

    private final BigDecimal lineExtensionAmount;
    private final BigDecimal taxExclusiveAmount;
    private final BigDecimal taxInclusiveAmount;
    private final BigDecimal payableAmount;

    // F6 - retenciones que el adquiriente (agente retenedor) practica, congeladas.
    // Cero cuando no
    // aplica.
    private final BigDecimal reteFuenteAmount;
    private final BigDecimal reteIvaAmount;
    private final BigDecimal reteIcaAmount;

    private final PaymentForm paymentForm;

    private final List<ElectronicDocumentLine> lines;
    private final List<ElectronicDocumentPayment> payments;

    // F5 - correcciones. Solo poblados en notas (NOTA_CREDITO/NOTA_DEBITO):
    // referencia al documento
    // corregido + concepto DIAN (codigo + texto). En facturas son null. `reversed`
    // marca que ESTA
    // factura fue anulada por una nota credito validada (no final: lo estampa la
    // validacion DIAN).
    private final DocumentReference reference;
    private final String noteReasonCode;
    private final String noteReasonText;
    private boolean reversed;

    private final LocalDateTime createdDate;
    private final boolean enabled;

    // Idempotencia de la venta POS: UUID que el cliente genera por apertura del
    // cobro. Null en
    // documentos que
    // no vienen del POS (cierre de cuenta, notas). La unicidad (empresa,
    // clientRequestId) la respalda
    // la BD.
    private final String clientRequestId;

    // Actor fiscal: empleado que emite el documento (venta POS, nota
    // credito/debito), tomado del
    // contexto de
    // autenticacion. Da trazabilidad de autoria a acciones sensibles (una NC anula
    // factura y reversa
    // cartera).
    // NULL en las vias sin empleado directo: emision al cerrar la cuenta (autor via
    // OpenAccount.closedBy),
    // procesos async/sistema y documentos legacy.
    private final Long issuedByEmployeeId;

    public ElectronicDocument(Long id, Long companyId, Long openAccountId,
            ElectronicDocumentType documentType, String prefix, Long consecutive,
            String resolutionNumber, LocalDate issueDate, String issueTime, String cufe,
            String cude, String uuid, String qrData, String qrUrl, String xmlSigned,
            String pdfRepresentation, DianStatus dianStatus, LocalDateTime dianValidationDate,
            IssuerSnapshot issuer, CustomerSnapshot customer, BigDecimal lineExtensionAmount,
            BigDecimal taxExclusiveAmount, BigDecimal taxInclusiveAmount, BigDecimal payableAmount,
            PaymentForm paymentForm, List<ElectronicDocumentLine> lines,
            List<ElectronicDocumentPayment> payments, LocalDateTime createdDate, boolean enabled,
            DocumentReference reference, String noteReasonCode, String noteReasonText,
            boolean reversed, BigDecimal reteFuenteAmount, BigDecimal reteIvaAmount,
            BigDecimal reteIcaAmount, String clientRequestId, Long issuedByEmployeeId,
            Long branchId) {
        validate(companyId, documentType, issueDate, issueTime, dianStatus, issuer, customer,
                lineExtensionAmount, taxExclusiveAmount, taxInclusiveAmount, payableAmount,
                paymentForm, lines);
        validateNoteConsistency(documentType, reference, noteReasonCode);
        if (branchId == null)
            throw new IllegalArgumentException("branchId is required");
        this.id = id;
        this.companyId = companyId;
        this.openAccountId = openAccountId;
        this.branchId = branchId;
        this.documentType = documentType;
        this.prefix = prefix;
        this.consecutive = consecutive;
        this.resolutionNumber = resolutionNumber;
        this.issueDate = issueDate;
        this.issueTime = issueTime;
        this.cufe = cufe;
        this.cude = cude;
        this.uuid = uuid;
        this.qrData = qrData;
        this.qrUrl = qrUrl;
        this.xmlSigned = xmlSigned;
        this.pdfRepresentation = pdfRepresentation;
        this.dianStatus = dianStatus;
        this.dianValidationDate = dianValidationDate;
        this.issuer = issuer;
        this.customer = customer;
        this.lineExtensionAmount = lineExtensionAmount;
        this.taxExclusiveAmount = taxExclusiveAmount;
        this.taxInclusiveAmount = taxInclusiveAmount;
        this.payableAmount = payableAmount;
        this.paymentForm = paymentForm;
        this.lines = List.copyOf(lines);
        this.payments = payments == null ? List.of() : List.copyOf(payments);
        this.reference = reference;
        this.noteReasonCode = noteReasonCode;
        this.noteReasonText = noteReasonText;
        this.reversed = reversed;
        this.reteFuenteAmount = reteFuenteAmount == null ? BigDecimal.ZERO : reteFuenteAmount;
        this.reteIvaAmount = reteIvaAmount == null ? BigDecimal.ZERO : reteIvaAmount;
        this.reteIcaAmount = reteIcaAmount == null ? BigDecimal.ZERO : reteIcaAmount;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.clientRequestId = clientRequestId;
        this.issuedByEmployeeId = issuedByEmployeeId;
    }

    /**
     * Construye un documento PENDIENTE a partir de los datos ya congelados de una
     * cuenta cerrada. Estampa fecha/hora Colombia (-05:00) y totaliza desde las
     * lineas. Sin numero ni sellos DIAN.
     */
    public static ElectronicDocument createPending(Long companyId, Long openAccountId,
            ElectronicDocumentType documentType, IssuerSnapshot issuer, CustomerSnapshot customer,
            List<ElectronicDocumentLine> lines, List<ElectronicDocumentPayment> payments,
            PaymentForm paymentForm, boolean withholdingAgent, BigDecimal reteFuenteRate,
            BigDecimal reteIvaRate, BigDecimal reteIcaRate, BigDecimal uvt, String clientRequestId,
            Long issuedByEmployeeId, Long branchId) {
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("a document requires at least one line");
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);
        String issueTime = now.toLocalTime().format(TIME_FORMAT) + COLOMBIA_OFFSET;
        BigDecimal base = sum(lines, ElectronicDocumentLine::getLineExtensionAmount);
        BigDecimal totalWithTax = sum(lines, ElectronicDocumentLine::getTotalAmount);
        // Cuadre de caja: toda venta es de contado, así que la suma de pagos debe
        // igualar el total a
        // pagar.
        // Evita persistir un documento con pagos que no cierran el total.
        if (payments != null && !payments.isEmpty()) {
            BigDecimal paid = Money
                    .scaled(payments.stream().map(ElectronicDocumentPayment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            if (paid.compareTo(totalWithTax) != 0) {
                throw new IllegalArgumentException("La suma de pagos (" + paid
                        + ") no coincide con el total del documento (" + totalWithTax + ").");
            }
        }
        // F6/3.10: retenciones del adquiriente agente retenedor. reteIVA SOLO sobre el
        // IVA generado
        // (esquema
        // IVA), NO sobre IVA+INC; reteFuente/reteICA sobre la base de las líneas
        // gravadas (excluye
        // EXCLUIDO/GENERAL).
        WithholdingAmounts wh = WithholdingCalculator.compute(withholdingAgent,
                withholdingBaseOf(lines), ivaTotalOf(lines), reteFuenteRate, reteIvaRate,
                reteIcaRate, uvt);
        return new ElectronicDocument(null, companyId, openAccountId, documentType, null, null,
                null, now.toLocalDate(), issueTime, null, null, null, null, null, null, null,
                DianStatus.PENDIENTE, null, issuer, customer, base, base, totalWithTax,
                totalWithTax, paymentForm, lines, payments, LocalDateTime.now(), true, null, null,
                null, false, wh.reteFuente(), wh.reteIva(), wh.reteIca(), clientRequestId,
                issuedByEmployeeId, branchId);
    }

    /**
     * Construye una NOTA CREDITO PENDIENTE que corrige (anula, total) una factura
     * VALIDADA. Clona el contenido fiscal del original (lineas, snapshots, totales)
     * y fija la referencia + concepto DIAN. La nota usa CUDE propio y consecutivo
     * de su resolucion (los estampa la validacion DIAN, F3).
     */
    public static ElectronicDocument createCreditNote(ElectronicDocument original,
            String reasonCode, String reasonText, Long issuedByEmployeeId,
            BigDecimal partialAmount) {
        return createNote(original, ElectronicDocumentType.NOTA_CREDITO, reasonCode, reasonText,
                issuedByEmployeeId, partialAmount);
    }

    /**
     * Construye una NOTA DEBITO PENDIENTE (aumento) que referencia una factura
     * VALIDADA. Con {@code
     * additionalAmount} null clona el original (heredado); con un monto {@code > 0}
     * la nota representa ese INCREMENTO real (escala proporcional del original), en
     * vez de duplicar el valor de la factura.
     */
    public static ElectronicDocument createDebitNote(ElectronicDocument original, String reasonCode,
            String reasonText, Long issuedByEmployeeId, BigDecimal additionalAmount) {
        return createNote(original, ElectronicDocumentType.NOTA_DEBITO, reasonCode, reasonText,
                issuedByEmployeeId, additionalAmount);
    }

    /**
     * 3.9 — Construye la nota (crédito/débito). {@code amount} null ⇒ nota TOTAL
     * (clona el original: NC de anulación). {@code amount} presente ⇒ nota
     * PARCIAL/INCREMENTAL: escala líneas, totales y retenciones por el ratio
     * {@code amount/total}, preservando la mezcla de tarifas y el IVA proporcional.
     * La NC no puede exceder el total del original; la ND admite cualquier
     * incremento positivo.
     */
    private static ElectronicDocument createNote(ElectronicDocument original,
            ElectronicDocumentType type, String reasonCode, String reasonText,
            Long issuedByEmployeeId, BigDecimal amount) {
        if (original == null)
            throw new IllegalArgumentException("original document is required");
        if (original.cufe == null || original.cufe.isBlank())
            throw new IllegalArgumentException("original document has no CUFE to reference");
        DocumentReference ref = new DocumentReference(original.cufe, original.prefix,
                original.consecutive, original.issueDate);
        BigDecimal total = original.payableAmount;
        BigDecimal ratio;
        if (amount == null) {
            ratio = BigDecimal.ONE;
        } else {
            if (amount.signum() <= 0)
                throw new IllegalArgumentException("el monto de la nota debe ser mayor que cero");
            if (total == null || total.signum() == 0)
                throw new IllegalArgumentException(
                        "la factura referenciada no tiene total para calcular la nota");
            if (type == ElectronicDocumentType.NOTA_CREDITO && amount.compareTo(total) > 0)
                throw new IllegalArgumentException(
                        "una nota crédito no puede exceder el total de la factura (" + total + ")");
            ratio = amount.divide(total, 6, Money.ROUND);
        }
        List<ElectronicDocumentLine> noteLines = original.lines.stream()
                .map(l -> scaleLine(l, ratio)).toList();
        // BE-19: los totales de la nota SON la suma de sus líneas, nunca un escalado
        // aparte del total del original. El adaptador envía LegalMonetaryTotal y las
        // líneas en dos bloques distintos, y la DIAN los compara: si cada bloque se
        // redondea por su cuenta, con N líneas el descuadre llega a ~N/2 centavos y el
        // rechazo fiscal bloquea la anulación de la factura delante del cliente.
        // Derivándolos de las líneas ya redondeadas, cuadran por construcción — que es
        // lo que createPending ya hacía para la factura.
        BigDecimal noteLineExtension = sum(noteLines,
                ElectronicDocumentLine::getLineExtensionAmount);
        BigDecimal noteTaxTotal = sum(noteLines, ElectronicDocumentLine::getTaxAmount);
        BigDecimal noteTaxInclusive = Money.scaled(noteLineExtension.add(noteTaxTotal));
        List<ElectronicDocumentPayment> notePayments = scalePayments(original.payments, ratio,
                noteTaxInclusive);
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);
        String issueTime = now.toLocalTime().format(TIME_FORMAT) + COLOMBIA_OFFSET;
        return new ElectronicDocument(null, original.companyId, original.openAccountId, type, null,
                null, null, now.toLocalDate(), issueTime, null, null, null, null, null, null, null,
                DianStatus.PENDIENTE, null, original.issuer, original.customer, noteLineExtension,
                noteLineExtension, noteTaxInclusive, noteTaxInclusive, original.paymentForm,
                noteLines, notePayments, LocalDateTime.now(), true, ref, reasonCode, reasonText,
                false, Money.scaled(original.reteFuenteAmount.multiply(ratio)),
                Money.scaled(original.reteIvaAmount.multiply(ratio)),
                Money.scaled(original.reteIcaAmount.multiply(ratio)), null, issuedByEmployeeId,
                original.branchId);
    }

    /**
     * Prorratea los medios de pago y carga el residuo del redondeo al último, para
     * que la suma de pagos cuadre con el total de la nota. Es la misma regla que
     * {@code createPending} impone a la factura, donde un descuadre entre pagos y
     * total es motivo de rechazo antes de persistir.
     */
    private static List<ElectronicDocumentPayment> scalePayments(
            List<ElectronicDocumentPayment> payments, BigDecimal ratio, BigDecimal payable) {
        if (payments.isEmpty())
            return List.of();
        List<ElectronicDocumentPayment> scaled = new ArrayList<>(payments.size());
        for (ElectronicDocumentPayment p : payments) {
            scaled.add(new ElectronicDocumentPayment(null, p.getPaymentMeans(),
                    Money.scaled(p.getAmount().multiply(ratio))));
        }
        BigDecimal residual = payable
                .subtract(scaled.stream().map(ElectronicDocumentPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (residual.signum() != 0) {
            int last = scaled.size() - 1;
            ElectronicDocumentPayment p = scaled.get(last);
            scaled.set(last, new ElectronicDocumentPayment(null, p.getPaymentMeans(),
                    Money.scaled(p.getAmount().add(residual))));
        }
        return List.copyOf(scaled);
    }

    /**
     * Clona una línea escalando sus montos por {@code ratio} (NC parcial / ND
     * incremental). ratio=1 ⇒ copia.
     */
    private static ElectronicDocumentLine scaleLine(ElectronicDocumentLine l, BigDecimal ratio) {
        if (ratio.compareTo(BigDecimal.ONE) == 0) {
            return new ElectronicDocumentLine(null, l.getLineNumber(), l.getDescription(),
                    l.getQuantity(), l.getUnitMeasureCode(), l.getUnitPrice(),
                    l.getLineExtensionAmount(), l.getTaxCategory(), l.getTaxScheme(),
                    l.getTaxRate(), l.getTaxAmount(), l.getTotalAmount());
        }
        BigDecimal base = Money.scaled(l.getLineExtensionAmount().multiply(ratio));
        BigDecimal tax = Money.scaled(l.getTaxAmount().multiply(ratio));
        // BE-19, mismo problema una escala más abajo: el total de la línea se DERIVA de
        // sus dos componentes ya redondeados. Escalándolo por su cuenta,
        // scaled(total·ratio) puede no ser scaled(base·ratio) + scaled(iva·ratio) y la
        // propia línea sale descuadrada.
        return new ElectronicDocumentLine(null, l.getLineNumber(), l.getDescription(),
                l.getQuantity(), l.getUnitMeasureCode(),
                Money.scaled(l.getUnitPrice().multiply(ratio)), base, l.getTaxCategory(),
                l.getTaxScheme(), l.getTaxRate(), tax, base.add(tax));
    }

    private static BigDecimal sum(List<ElectronicDocumentLine> lines,
            java.util.function.Function<ElectronicDocumentLine, BigDecimal> field) {
        return Money.scaled(lines.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static void validate(Long companyId, ElectronicDocumentType documentType,
            LocalDate issueDate, String issueTime, DianStatus dianStatus, IssuerSnapshot issuer,
            CustomerSnapshot customer, BigDecimal lineExtensionAmount,
            BigDecimal taxExclusiveAmount, BigDecimal taxInclusiveAmount, BigDecimal payableAmount,
            PaymentForm paymentForm, List<ElectronicDocumentLine> lines) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (documentType == null)
            throw new IllegalArgumentException("documentType is required");
        if (issueDate == null)
            throw new IllegalArgumentException("issueDate is required");
        if (issueTime == null || issueTime.isBlank())
            throw new IllegalArgumentException("issueTime is required");
        if (dianStatus == null)
            throw new IllegalArgumentException("dianStatus is required");
        if (issuer == null)
            throw new IllegalArgumentException("issuer snapshot is required");
        if (customer == null)
            throw new IllegalArgumentException("customer snapshot is required");
        if (lineExtensionAmount == null)
            throw new IllegalArgumentException("lineExtensionAmount is required");
        if (taxExclusiveAmount == null)
            throw new IllegalArgumentException("taxExclusiveAmount is required");
        if (taxInclusiveAmount == null)
            throw new IllegalArgumentException("taxInclusiveAmount is required");
        if (payableAmount == null)
            throw new IllegalArgumentException("payableAmount is required");
        if (paymentForm == null)
            throw new IllegalArgumentException("paymentForm is required");
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("a document requires at least one line");
    }

    /**
     * Una nota (credito/debito) exige referencia + concepto; una factura no puede
     * llevarlos.
     */
    private static void validateNoteConsistency(ElectronicDocumentType documentType,
            DocumentReference reference, String noteReasonCode) {
        boolean isNote = documentType == ElectronicDocumentType.NOTA_CREDITO
                || documentType == ElectronicDocumentType.NOTA_DEBITO;
        if (isNote) {
            if (reference == null)
                throw new IllegalArgumentException(
                        "a credit/debit note requires a document reference");
            if (noteReasonCode == null || noteReasonCode.isBlank())
                throw new IllegalArgumentException("a credit/debit note requires a reason code");
        } else if (reference != null) {
            throw new IllegalArgumentException("an invoice cannot reference another document");
        }
    }

    public boolean isNote() {
        return documentType == ElectronicDocumentType.NOTA_CREDITO
                || documentType == ElectronicDocumentType.NOTA_DEBITO;
    }

    /**
     * Código DIAN del medio de pago predominante (el de mayor monto). Si el
     * documento no tiene pagos registrados, asume EFECTIVO (10). Lo usan los
     * adaptadores de proveedor para `payment_method_code`.
     */
    public String primaryPaymentMeansCode() {
        return payments.stream().max(Comparator.comparing(ElectronicDocumentPayment::getAmount))
                .map(p -> p.getPaymentMeans().dianCode()).orElse(PaymentMeans.EFECTIVO.dianCode());
    }

    /**
     * Asigna la numeración fiscal (resolución DIAN + prefijo + consecutivo) al
     * emitir, antes de transmitir. Tomada de la {@code NumberingResolution} activa
     * de la empresa. Solo sobre un documento PENDIENTE y aún sin numerar
     * (idempotencia/seguridad fiscal: el consecutivo no se reasigna).
     */
    public void assignNumber(String resolutionNumber, String prefix, Long consecutive) {
        if (dianStatus != DianStatus.PENDIENTE)
            throw new IllegalStateException("solo se puede numerar un documento PENDIENTE");
        if (this.consecutive != null)
            throw new IllegalStateException("el documento ya tiene consecutivo asignado");
        if (resolutionNumber == null || resolutionNumber.isBlank())
            throw new IllegalArgumentException("resolutionNumber is required");
        if (consecutive == null)
            throw new IllegalArgumentException("consecutive is required");
        this.resolutionNumber = resolutionNumber;
        this.prefix = prefix;
        this.consecutive = consecutive;
    }

    /**
     * El consecutivo de los documentos equivalentes POS lo asigna el proveedor DIAN
     * (MATIAS auto-increment), no nuestra resolución local; por eso no consumimos
     * consecutivo local para POS.
     */
    public boolean usesProviderAssignedConsecutive() {
        return documentType == ElectronicDocumentType.DOC_EQUIV_POS;
    }

    /**
     * Asigna SOLO resolución + prefijo (sin consecutivo) al emitir, para los
     * documentos cuyo número lo asigna el proveedor DIAN
     * ({@link #usesProviderAssignedConsecutive()}). El consecutivo real lo sella
     * {@link #markValidated} con la respuesta del proveedor. No consume numeración
     * local.
     */
    public void assignResolutionOnly(String resolutionNumber, String prefix) {
        if (dianStatus != DianStatus.PENDIENTE)
            throw new IllegalStateException("solo se puede numerar un documento PENDIENTE");
        if (this.consecutive != null)
            throw new IllegalStateException("el documento ya tiene consecutivo asignado");
        if (resolutionNumber == null || resolutionNumber.isBlank())
            throw new IllegalArgumentException("resolutionNumber is required");
        this.resolutionNumber = resolutionNumber;
        this.prefix = prefix;
    }

    /**
     * Sella el documento como VALIDADO por la DIAN: número fiscal + sellos del
     * proveedor. Forward-only: solo desde PENDIENTE/CONTINGENCIA. No toca el
     * contenido fiscal.
     */
    public void markValidated(String prefix, Long consecutive, String cufe, String cude,
            String uuid, String xmlSigned, String qrData, String qrUrl, String pdfRepresentation,
            LocalDateTime validationDate) {
        ensureNotTerminal();
        // El numero fiscal lo asignamos nosotros al emitir (assignNumber); no lo
        // sobreescribas si el
        // proveedor no lo devuelve (MATIAS no lo regresa en /invoice). Solo lo aplica
        // si viene poblado.
        if (prefix != null)
            this.prefix = prefix;
        if (consecutive != null)
            this.consecutive = consecutive;
        this.cufe = cufe;
        this.cude = cude;
        this.uuid = uuid;
        this.xmlSigned = xmlSigned;
        this.qrData = qrData;
        this.qrUrl = qrUrl;
        this.pdfRepresentation = pdfRepresentation;
        this.dianValidationDate = validationDate;
        this.dianStatus = DianStatus.VALIDADO;
    }

    /**
     * Marca el documento como NO_ELECTRONICO: la empresa no tiene facturación
     * electrónica habilitada (sin submódulo BILLING). Queda guardado localmente,
     * sin numeración fiscal ni sellos, y nunca se transmite al proveedor. Solo
     * aplicable desde PENDIENTE (un documento ya numerado/validado no se degrada).
     */
    public void markLocal() {
        if (dianStatus != DianStatus.PENDIENTE)
            throw new IllegalStateException(
                    "solo un documento PENDIENTE puede marcarse como NO_ELECTRONICO");
        this.dianStatus = DianStatus.NO_ELECTRONICO;
    }

    /**
     * La DIAN rechazó el documento. El motivo se registra en la bitácora de
     * transmisión.
     */
    public void markRejected() {
        ensureNotTerminal();
        this.dianStatus = DianStatus.RECHAZADO;
    }

    /**
     * Libera la numeración fiscal de un documento RECHAZADO (contrapartida de
     * {@link #assignNumber}) para que el consecutivo pueda reutilizarse y no quede
     * un hueco en la secuencia. El intento (número enviado + motivo) queda en la
     * bitácora de transmisión, no en el documento. Solo aplicable tras marcar
     * RECHAZADO, y solo se invoca cuando la resolución pudo recuperar el
     * consecutivo.
     */
    public void releaseFiscalNumber() {
        if (dianStatus != DianStatus.RECHAZADO)
            throw new IllegalStateException(
                    "solo se libera la numeración de un documento RECHAZADO");
        this.prefix = null;
        this.consecutive = null;
        this.resolutionNumber = null;
    }

    /**
     * Proveedor/DIAN indisponible: queda en contingencia para retransmitir dentro
     * del plazo.
     */
    public void markContingency() {
        ensureNotTerminal();
        this.dianStatus = DianStatus.CONTINGENCIA;
    }

    /**
     * Documento "provisional": en contingencia y aún SIN sello fiscal DIAN (sin
     * CUFE/CUDE). Lo que se entrega al cliente mientras tanto es un comprobante NO
     * fiscal; el documento fiscal real llega al regularizar (cuando el
     * proveedor/DIAN vuelve y la transmisión valida). Útil para etiquetar la
     * representación gráfica.
     */
    public boolean isProvisional() {
        return dianStatus == DianStatus.CONTINGENCIA && cufe == null && cude == null;
    }

    /**
     * Adjunta la referencia (clave/URL S3) de la representación gráfica PDF ya
     * generada.
     */
    public void attachRepresentation(String pdfRepresentation) {
        this.pdfRepresentation = pdfRepresentation;
    }

    /**
     * Marca esta factura como reversada por una nota credito ya VALIDADA.
     * Idempotente. Es la marca fiscal que subordina el void de cartera a la
     * validacion DIAN de la nota (no es soft-delete).
     */
    public void markReversed() {
        this.reversed = true;
    }

    private void ensureNotTerminal() {
        if (dianStatus == DianStatus.VALIDADO || dianStatus == DianStatus.RECHAZADO) {
            throw new IllegalStateException("El documento ya está en estado terminal (" + dianStatus
                    + ") y no puede transicionar; corrige por nota crédito/débito.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getOpenAccountId() {
        return openAccountId;
    }

    /** Sucursal (sede) emisora del documento. */
    public Long getBranchId() {
        return branchId;
    }

    public ElectronicDocumentType getDocumentType() {
        return documentType;
    }

    public String getPrefix() {
        return prefix;
    }

    public Long getConsecutive() {
        return consecutive;
    }

    public String getResolutionNumber() {
        return resolutionNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public String getCufe() {
        return cufe;
    }

    public String getCude() {
        return cude;
    }

    public String getUuid() {
        return uuid;
    }

    public String getQrData() {
        return qrData;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public String getXmlSigned() {
        return xmlSigned;
    }

    public String getPdfRepresentation() {
        return pdfRepresentation;
    }

    public DianStatus getDianStatus() {
        return dianStatus;
    }

    public LocalDateTime getDianValidationDate() {
        return dianValidationDate;
    }

    public IssuerSnapshot getIssuer() {
        return issuer;
    }

    public CustomerSnapshot getCustomer() {
        return customer;
    }

    public BigDecimal getLineExtensionAmount() {
        return lineExtensionAmount;
    }

    public BigDecimal getTaxExclusiveAmount() {
        return taxExclusiveAmount;
    }

    public BigDecimal getTaxInclusiveAmount() {
        return taxInclusiveAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public BigDecimal getReteFuenteAmount() {
        return reteFuenteAmount;
    }

    public BigDecimal getReteIvaAmount() {
        return reteIvaAmount;
    }

    public BigDecimal getReteIcaAmount() {
        return reteIcaAmount;
    }

    /**
     * Neto a pagar tras restar las retenciones del adquiriente (total − reteFuente
     * − reteIva − reteIca).
     */
    public BigDecimal getNetPayableAmount() {
        return payableAmount.subtract(reteFuenteAmount).subtract(reteIvaAmount)
                .subtract(reteIcaAmount);
    }

    /**
     * IVA generado del documento = suma del impuesto de las líneas con esquema IVA
     * (a tarifa real). Es la base correcta de la reteIVA: NO incluye el INC (3.10).
     * Recalculado desde las líneas para que el reporte (buildTaxTotals) coincida
     * con el monto retenido en {@link #createPending}.
     */
    public BigDecimal getIvaTotal() {
        return ivaTotalOf(lines);
    }

    /**
     * Base de retención en fuente / ICA = suma de la base de las líneas GRAVADAS
     * (con esquema tributario: GRAVADO/INC/EXENTO-IVA-0%). Excluye EXCLUIDO y los
     * ítems libres del POS (sin esquema), que no forman parte de la base gravable
     * (3.10).
     */
    public BigDecimal getWithholdingBase() {
        return withholdingBaseOf(lines);
    }

    private static BigDecimal ivaTotalOf(List<ElectronicDocumentLine> lines) {
        return sum(lines.stream().filter(l -> l.getTaxScheme() == TaxScheme.IVA).toList(),
                ElectronicDocumentLine::getTaxAmount);
    }

    private static BigDecimal withholdingBaseOf(List<ElectronicDocumentLine> lines) {
        return sum(lines.stream().filter(l -> l.getTaxScheme() != null).toList(),
                ElectronicDocumentLine::getLineExtensionAmount);
    }

    public PaymentForm getPaymentForm() {
        return paymentForm;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    /**
     * Empleado que emitió el documento (POS/NC/ND); null en emisión al cierre,
     * procesos sistema o legacy.
     */
    public Long getIssuedByEmployeeId() {
        return issuedByEmployeeId;
    }

    public List<ElectronicDocumentLine> getLines() {
        return lines;
    }

    public List<ElectronicDocumentPayment> getPayments() {
        return payments;
    }

    public DocumentReference getReference() {
        return reference;
    }

    public String getNoteReasonCode() {
        return noteReasonCode;
    }

    public String getNoteReasonText() {
        return noteReasonText;
    }

    public boolean isReversed() {
        return reversed;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
