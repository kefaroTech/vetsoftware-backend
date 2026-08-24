package com.vetsoftware.app.subscriptionbilling.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El cálculo del desglose fiscal de un documento a partir de sus cargos.
 *
 * <p>
 * Cálculo puro, sin estado y sin Spring: agrupa los cargos por
 * {@code (tratamiento, tarifa)}, suma la base de cada grupo y calcula el
 * impuesto <b>una sola vez sobre esa base agregada</b>. Es lo que permite
 * emitir una factura con <b>tarifas mixtas</b> —unos módulos gravados y otros
 * excluidos—, que con un solo importe de impuesto por documento sería
 * literalmente inexpresable.
 */
public final class TaxBreakdown {

    private final List<BillingDocumentTax> lineas;
    private final BigDecimal subtotalAmount;
    private final BigDecimal taxAmount;

    private TaxBreakdown(List<BillingDocumentTax> lineas, BigDecimal subtotalAmount,
            BigDecimal taxAmount) {
        this.lineas = List.copyOf(lineas);
        this.subtotalAmount = subtotalAmount;
        this.taxAmount = taxAmount;
    }

    /**
     * Construye el desglose y, de paso, comprueba las dos reglas de signo que la
     * base no puede imponer.
     *
     * <p>
     * <b>1. Una nota crédito no mezcla cargos de los dos signos.</b> Si los
     * mezclara, la suma se compensaría parcialmente, el {@code ABS(SUM(...))} de la
     * conciliación R6 dejaría de ser el subtotal del documento y la vigilancia
     * mentiría <b>sin devolver ninguna fila</b>.
     *
     * <p>
     * <b>2. Ningún grupo de tarifa puede quedar en negativo dentro de un documento
     * cuyo neto es positivo</b>, ni al revés. Es la trampa silenciosa del valor
     * absoluto: una factura con una cuota gravada de +100 y un descuento gravado de
     * −20 da un grupo de +80 y todo cuadra; pero si el descuento fuera de −120, el
     * grupo quedaría en −20 y {@code ABS} lo convertiría en una base declarable de
     * +20 que no existe. La base rechazaría el negativo con
     * {@code chk_sbdt_amounts_positive} y el positivo falso lo aceptaría sin
     * pestañear — así que la comprobación tiene que estar aquí.
     *
     * @param charges
     *            los cargos que agrupa el documento, con su signo
     * @param kind
     *            el tipo del documento, que decide el signo esperado
     */
    public static TaxBreakdown of(List<SubscriptionCharge> charges, DocumentKind kind,
            Long companyId, LocalDateTime createdDate) {
        if (charges == null || charges.isEmpty())
            throw new IllegalArgumentException("a billing document needs at least one charge");
        validarSignoUniforme(charges, kind);

        Map<Clave, BigDecimal> porTarifa = new LinkedHashMap<>();
        BigDecimal neto = Money.zero();
        for (SubscriptionCharge charge : charges) {
            Clave clave = new Clave(charge.getTaxTreatment(), charge.getTaxRate());
            porTarifa.merge(clave, charge.getSubtotalAmount(), BigDecimal::add);
            neto = neto.add(charge.getSubtotalAmount());
        }
        int signoDelDocumento = neto.signum();

        List<BillingDocumentTax> lineas = new ArrayList<>();
        BigDecimal impuestoTotal = Money.zero();
        for (Map.Entry<Clave, BigDecimal> grupo : porTarifa.entrySet()) {
            BigDecimal baseDelGrupo = grupo.getValue();
            validarGrupo(grupo.getKey(), baseDelGrupo, signoDelDocumento);
            BillingDocumentTax linea = BillingDocumentTax.of(companyId, grupo.getKey().treatment(),
                    grupo.getKey().rate(), baseDelGrupo, createdDate);
            lineas.add(linea);
            impuestoTotal = impuestoTotal.add(linea.taxAmount());
        }
        lineas.sort(Comparator.comparing((BillingDocumentTax l) -> l.taxTreatment().name())
                .thenComparing(BillingDocumentTax::taxRate));
        return new TaxBreakdown(lineas, Money.scaled(neto.abs()), Money.scaled(impuestoTotal));
    }

    private static void validarSignoUniforme(List<SubscriptionCharge> charges, DocumentKind kind) {
        Integer signoExigido = kind.signoExigidoALosCargos();
        if (signoExigido == null)
            return;
        int positivos = (int) charges.stream().filter(c -> c.signo() > 0).count();
        int negativos = (int) charges.stream().filter(c -> c.signo() < 0).count();
        if (positivos > 0 && negativos > 0)
            throw new MixedSignChargesException(positivos, negativos);
        if (positivos > 0)
            throw new IllegalArgumentException(
                    "a " + kind + " groups charges that subtract, but all " + positivos
                            + " of these add: what it needs is an invoice or a debit note");
    }

    private static void validarGrupo(Clave clave, BigDecimal baseDelGrupo, int signoDelDocumento) {
        if (baseDelGrupo.signum() == 0 || signoDelDocumento == 0)
            return;
        if (baseDelGrupo.signum() != signoDelDocumento)
            throw new IllegalArgumentException("the " + clave.treatment() + " " + clave.rate()
                    + "% group nets " + baseDelGrupo
                    + ", which is the opposite sign of the document: its absolute value would be"
                    + " declared as a taxable base that does not exist");
    }

    /** Las líneas de {@code subscription_billing_document_taxes}, ya calculadas. */
    public List<BillingDocumentTax> lineas() {
        return lineas;
    }

    /** El subtotal del documento: la suma de los cargos, en valor absoluto. */
    public BigDecimal subtotalAmount() {
        return subtotalAmount;
    }

    /** El impuesto del documento: la suma del desglose, nunca un cálculo aparte. */
    public BigDecimal taxAmount() {
        return taxAmount;
    }

    /** El total: {@code subtotal + impuesto}, espejo de {@code chk_sbd_total}. */
    public BigDecimal totalAmount() {
        return Money.scaled(subtotalAmount.add(taxAmount));
    }

    private record Clave(TaxTreatment treatment, BigDecimal rate) {
        Clave {
            rate = rate.stripTrailingZeros();
        }
    }
}
