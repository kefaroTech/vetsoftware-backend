package com.vetsoftware.app.quote.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * La cotizacion: el primer documento con valor legal del proceso.
 *
 * <p>
 * <b>A partir de aqui nada se recalcula.</b> Las lineas guardan copias
 * congeladas del catalogo y la cabecera guarda los cuatro totales. Si manana
 * cambia el precio de un modulo o la forma de redondear, la cotizacion enviada
 * ayer sigue diciendo lo que decia.
 *
 * <p>
 * <b>El caso raro del tenant.</b> {@code company} es NULABLE a proposito: se
 * cotiza a un prospecto que todavia no es cliente, y tambien una ampliacion a
 * quien ya tiene contrato. La frontera de tenant de todo el bloque es esta
 * cabecera: {@code quote_lines} y {@code quote_answers} no llevan
 * {@code company_id} y solo se alcanzan pasando por aqui.
 */
public class Quote {

    private static final int MAX_NUMBER = 30;
    private static final int MAX_PROSPECT_NAME = 150;
    private static final int MAX_PROSPECT_EMAIL = 120;
    private static final int MAX_PROSPECT_DOCUMENT = 50;
    private static final int MAX_PROSPECT_PHONE = 30;
    private static final int MAX_ACCEPTED_IP = 45;
    private static final int MAX_CLIENT_REQUEST_ID = 64;

    private final Long id;
    private final String quoteNumber;
    private final CompanyRef company;
    private final String prospectName;
    private final String prospectEmail;
    private final String prospectDocument;
    private final String prospectPhone;
    private final Long priceListId;
    private final BillingCycle billingCycle;
    private final BigDecimal subtotalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private QuoteStatus status;
    private final LocalDate validUntil;
    private final int trialDays;
    private LocalDateTime acceptedAt;
    private String acceptedByEmail;
    private String acceptedIp;
    private final String clientRequestId;
    private final LocalDateTime createdDate;
    private final Long version;
    private final boolean enabled;
    private final List<QuoteLine> lines;
    private final List<QuoteAnswer> answers;

    public Quote(Long id, String quoteNumber, CompanyRef company, String prospectName,
            String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
            BillingCycle billingCycle, BigDecimal subtotalAmount, BigDecimal discountAmount,
            BigDecimal taxAmount, BigDecimal totalAmount, QuoteStatus status, LocalDate validUntil,
            int trialDays, LocalDateTime acceptedAt, String acceptedByEmail, String acceptedIp,
            String clientRequestId, LocalDateTime createdDate, Long version, boolean enabled,
            List<QuoteLine> lines, List<QuoteAnswer> answers) {
        validateHeader(quoteNumber, company, prospectName, prospectEmail, prospectDocument,
                prospectPhone, priceListId, billingCycle, status, validUntil, trialDays,
                clientRequestId);
        validateAcceptance(status, acceptedAt, acceptedIp);
        List<QuoteLine> safeLines = lines == null ? List.of() : List.copyOf(lines);
        List<QuoteAnswer> safeAnswers = answers == null ? List.of() : List.copyOf(answers);
        validateLines(safeLines);
        this.id = id;
        this.quoteNumber = quoteNumber;
        this.company = company;
        this.prospectName = prospectName;
        this.prospectEmail = prospectEmail;
        this.prospectDocument = prospectDocument;
        this.prospectPhone = prospectPhone;
        this.priceListId = priceListId;
        this.billingCycle = billingCycle;
        this.subtotalAmount = requireAmount(subtotalAmount, "subtotalAmount");
        this.discountAmount = requireAmount(discountAmount, "discountAmount");
        this.taxAmount = requireAmount(taxAmount, "taxAmount");
        this.totalAmount = requireAmount(totalAmount, "totalAmount");
        this.status = status;
        this.validUntil = validUntil;
        this.trialDays = trialDays;
        this.acceptedAt = acceptedAt;
        this.acceptedByEmail = acceptedByEmail;
        this.acceptedIp = acceptedIp;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
        this.lines = safeLines;
        this.answers = safeAnswers;
        verifyTotals();
    }

    /**
     * Crea la cotizacion CALCULANDO los cuatro totales desde sus lineas.
     *
     * <p>
     * Los totales no se aceptan de fuera en ningun camino: es lo que hace que R5
     * -"los totales de una cotizacion cuadran con la suma de sus lineas"- sea
     * estructuralmente cierta en vez de una regla que alguien tiene que recordar.
     * El cliente firma exactamente el numero que suman las lineas que leyo.
     */
    public static Quote create(String quoteNumber, CompanyRef company, String prospectName,
            String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
            BillingCycle billingCycle, LocalDate validUntil, int trialDays, String clientRequestId,
            List<QuoteLine> lines, List<QuoteAnswer> answers, LocalDateTime createdDate) {
        List<QuoteLine> safeLines = lines == null ? List.of() : List.copyOf(lines);
        return new Quote(null, quoteNumber, company, prospectName, prospectEmail, prospectDocument,
                prospectPhone, priceListId, billingCycle, sumGross(safeLines),
                sumDiscount(safeLines), sumTax(safeLines), sumTotal(safeLines), QuoteStatus.DRAFT,
                validUntil, trialDays, null, null, null, clientRequestId, createdDate, null, true,
                safeLines, answers);
    }

    /**
     * DRAFT -> SENT. Enviar una oferta ya vencida no tiene sentido y se rechaza.
     */
    public void send(LocalDate today) {
        requireStatus(QuoteStatus.SENT, QuoteStatus.DRAFT);
        requireNotExpired(today);
        this.status = QuoteStatus.SENT;
    }

    /**
     * SENT -> ACCEPTED, dejando la prueba de la aceptacion: cuando, quien y desde
     * donde. Sin ese trio, "el cliente acepto" es una afirmacion sin respaldo.
     */
    public void accept(String byEmail, String ip, LocalDateTime at, LocalDate today) {
        requireStatus(QuoteStatus.ACCEPTED, QuoteStatus.SENT);
        requireNotExpired(today);
        if (byEmail == null || byEmail.isBlank())
            throw new IllegalArgumentException("acceptedByEmail is required");
        if (byEmail.length() > MAX_PROSPECT_EMAIL)
            throw new IllegalArgumentException("acceptedByEmail must be 120 chars or less");
        if (ip != null && ip.length() > MAX_ACCEPTED_IP)
            throw new IllegalArgumentException("acceptedIp must be 45 chars or less");
        if (at == null)
            throw new IllegalArgumentException("acceptedAt is required");
        this.status = QuoteStatus.ACCEPTED;
        this.acceptedAt = at;
        this.acceptedByEmail = byEmail;
        this.acceptedIp = ip;
    }

    /** SENT -> REJECTED. */
    public void reject() {
        requireStatus(QuoteStatus.REJECTED, QuoteStatus.SENT);
        this.status = QuoteStatus.REJECTED;
    }

    /** DRAFT o SENT -> EXPIRED, y solo si de verdad vencio. */
    public void expire(LocalDate today) {
        requireStatus(QuoteStatus.EXPIRED, QuoteStatus.DRAFT, QuoteStatus.SENT);
        if (today == null)
            throw new IllegalArgumentException("today is required");
        if (!validUntil.isBefore(today))
            throw new IllegalStateException("Quote " + id + " is still valid until " + validUntil);
        this.status = QuoteStatus.EXPIRED;
    }

    /**
     * Solo un borrador se puede dar de baja. Una vez enviada, la oferta salio al
     * cliente y desactivarla borraria la prueba de lo que se le ofrecio; el camino
     * correcto es rechazarla o dejarla vencer.
     */
    public void requireDeletable() {
        if (status != QuoteStatus.DRAFT)
            throw new InvalidQuoteStatusTransitionException(status, QuoteStatus.DRAFT);
    }

    public boolean isExpiredOn(LocalDate today) {
        return validUntil.isBefore(today);
    }

    private void requireStatus(QuoteStatus target, QuoteStatus... allowedFrom) {
        for (QuoteStatus allowed : allowedFrom) {
            if (status == allowed)
                return;
        }
        throw new InvalidQuoteStatusTransitionException(status, target);
    }

    private void requireNotExpired(LocalDate today) {
        if (today == null)
            throw new IllegalArgumentException("today is required");
        if (isExpiredOn(today))
            throw new QuoteExpiredException(id, validUntil);
    }

    private static void validateHeader(String quoteNumber, CompanyRef company, String prospectName,
            String prospectEmail, String prospectDocument, String prospectPhone, Long priceListId,
            BillingCycle billingCycle, QuoteStatus status, LocalDate validUntil, int trialDays,
            String clientRequestId) {
        requireText(quoteNumber, "quoteNumber", MAX_NUMBER);
        requireOptionalText(prospectName, "prospectName", MAX_PROSPECT_NAME);
        requireOptionalText(prospectEmail, "prospectEmail", MAX_PROSPECT_EMAIL);
        requireOptionalText(prospectDocument, "prospectDocument", MAX_PROSPECT_DOCUMENT);
        requireOptionalText(prospectPhone, "prospectPhone", MAX_PROSPECT_PHONE);
        // chk_quotes_party: o es de una empresa, o al menos tiene el nombre del
        // prospecto. Una cotizacion sin destinatario no significa nada en el embudo.
        if (company == null && (prospectName == null || prospectName.isBlank()))
            throw new IllegalArgumentException("quote requires a company or a prospect name");
        if (priceListId == null)
            throw new IllegalArgumentException("priceListId is required");
        if (billingCycle == null)
            throw new IllegalArgumentException("billingCycle is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (validUntil == null)
            throw new IllegalArgumentException("validUntil is required");
        if (trialDays < 0)
            throw new IllegalArgumentException("trialDays cannot be negative");
        requireText(clientRequestId, "clientRequestId", MAX_CLIENT_REQUEST_ID);
    }

    /**
     * chk_quotes_accepted: aceptada sin fecha de aceptacion es un estado imposible.
     */
    private static void validateAcceptance(QuoteStatus status, LocalDateTime acceptedAt,
            String acceptedIp) {
        if (status == QuoteStatus.ACCEPTED && acceptedAt == null)
            throw new IllegalArgumentException("acceptedAt is required for an ACCEPTED quote");
        if (acceptedIp != null && acceptedIp.length() > MAX_ACCEPTED_IP)
            throw new IllegalArgumentException("acceptedIp must be 45 chars or less");
    }

    /**
     * uq_quote_lines_item y uq_quote_lines_number, traidas al dominio: cotizar el
     * mismo articulo dos veces es la via rapida a un total que no cuadra con lo que
     * el cliente cree que compro, y dos lineas con el mismo numero hacen que el
     * orden impreso deje de ser un contrato.
     */
    private static void validateLines(List<QuoteLine> lines) {
        if (lines.isEmpty())
            throw new IllegalArgumentException("quote requires at least one line");
        Set<Long> items = new HashSet<>();
        Set<Integer> numbers = new HashSet<>();
        for (QuoteLine line : lines) {
            if (!items.add(line.getCatalogItemId()))
                throw new IllegalArgumentException(
                        "duplicate catalog item in quote: " + line.getCatalogItemId());
            if (!numbers.add(line.getLineNumber()))
                throw new IllegalArgumentException(
                        "duplicate line number in quote: " + line.getLineNumber());
        }
    }

    /**
     * Regla R5, comprobada tambien AL LEER. Los cuatro totales estan guardados -no
     * calculados al vuelo- para que un cambio de redondeo no mueva un documento
     * viejo; el precio de guardarlos es demostrar que siguen cuadrando con las
     * lineas. Sin esta comprobacion, una linea desactivada despues de enviar la
     * oferta descuadra el total sin borrar nada.
     */
    private void verifyTotals() {
        compare("subtotalAmount", subtotalAmount, sumGross(lines));
        compare("discountAmount", discountAmount, sumDiscount(lines));
        compare("taxAmount", taxAmount, sumTax(lines));
        compare("totalAmount", totalAmount, sumTotal(lines));
        BigDecimal fromHeader = subtotalAmount.subtract(discountAmount).add(taxAmount);
        compare("totalAmount", totalAmount, fromHeader);
    }

    private static void compare(String concept, BigDecimal stored, BigDecimal fromLines) {
        if (stored.compareTo(fromLines) != 0)
            throw new QuoteTotalsMismatchException(concept, stored, fromLines);
    }

    private static BigDecimal sumGross(List<QuoteLine> lines) {
        return sum(lines, QuoteLine::grossAmount);
    }

    private static BigDecimal sumDiscount(List<QuoteLine> lines) {
        return sum(lines, QuoteLine::getDiscountAmount);
    }

    private static BigDecimal sumTax(List<QuoteLine> lines) {
        return sum(lines, QuoteLine::getTaxAmount);
    }

    private static BigDecimal sumTotal(List<QuoteLine> lines) {
        return sum(lines, QuoteLine::getLineTotal);
    }

    private static BigDecimal sum(List<QuoteLine> lines,
            Function<QuoteLine, BigDecimal> extractor) {
        return lines.stream().map(extractor).reduce(Money.zero(), BigDecimal::add);
    }

    private static BigDecimal requireAmount(BigDecimal value, String name) {
        if (value == null || value.signum() < 0)
            throw new IllegalArgumentException(name + " must be zero or positive");
        return Money.scaled(value);
    }

    private static void requireText(String value, String name, int max) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " is required");
        if (value.length() > max)
            throw new IllegalArgumentException(name + " must be " + max + " chars or less");
    }

    private static void requireOptionalText(String value, String name, int max) {
        if (value != null && value.length() > max)
            throw new IllegalArgumentException(name + " must be " + max + " chars or less");
    }

    public Long getId() {
        return id;
    }

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public Long getCompanyId() {
        return company == null ? null : company.id();
    }

    public String getProspectName() {
        return prospectName;
    }

    public String getProspectEmail() {
        return prospectEmail;
    }

    public String getProspectDocument() {
        return prospectDocument;
    }

    public String getProspectPhone() {
        return prospectPhone;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getAcceptedByEmail() {
        return acceptedByEmail;
    }

    public String getAcceptedIp() {
        return acceptedIp;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<QuoteLine> getLines() {
        return new ArrayList<>(lines);
    }

    public List<QuoteAnswer> getAnswers() {
        return new ArrayList<>(answers);
    }
}
