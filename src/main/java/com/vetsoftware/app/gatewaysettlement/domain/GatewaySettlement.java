package com.vetsoftware.app.gatewaysettlement.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que la pasarela liquido de verdad: un lote con su bruto, su comision, el
 * impuesto de esa comision, el gravamen de la salida y lo que cayo al banco.
 *
 * <p>
 * <strong>Una fila de aqui agrupa los cobros de MUCHAS clinicas, y esa es la
 * fuga que hay que vigilar.</strong> La pasarela paga en lotes y con dias de
 * retraso, juntando sesenta cobros de sesenta empresas distintas en un solo
 * abono. Si el detalle del pago de un cliente enseña su
 * {@code settlementReference} y ese dato abre el lote, se le estan enseñando
 * los importes de las otras cincuenta y nueve. Por eso esta tabla la escribe
 * plataforma y la <strong>lee solo plataforma</strong>: los seis puertos de
 * entrada de la feature van cerrados a {@code hasRole('SYSTEM')} a secas y no
 * existe controller de tenant.
 *
 * <p>
 * <strong>Sin empresa, y no por descuido.</strong> No hay {@code companyId} que
 * pudiera llevar: el lote no es de nadie en particular, es de todos a la vez.
 * Esa ausencia tiene una consecuencia mecanica que conviene saber antes de
 * colgar el primer {@code @ManyToOne} en la entidad JPA — ver
 * {@code GatewaySettlementJpaEntity}.
 *
 * <p>
 * <strong>Las dos referencias del proveedor van juntas o ninguna.</strong>
 * {@code chk_gateway_settlements_provider_invoice} es un bicondicional: o estan
 * las dos escritas o las dos nulas. Nulables porque la factura del proveedor
 * llega <em>despues</em> de la liquidacion, no porque sean opcionales: sin la
 * referencia del soporte, cinco millones al ano de gasto y casi un millon de
 * impuesto quedan expuestos a que los rechacen, y sin el NIT no se puede armar
 * el reporte anual de terceros.
 *
 * <p>
 * <strong>Las dos referencias del lote se comparan EXACTO.</strong>
 * {@code gateway} y {@code settlement_reference} son {@code ascii_bin}, asi que
 * {@code LOTE-9F2A} y {@code lote-9f2a} son lotes distintos. No es una
 * sutileza: bajo la colacion heredada del esquema serian el mismo y
 * {@code uq_gateway_settlements_reference} descartaria el segundo como
 * duplicado, dejando un abono real fuera del cuadre.
 *
 * <p>
 * <strong>No se borra, ni en logico ni en fisico.</strong> No hay
 * {@code enabled} en la tabla ni metodo de baja aqui. Un lote es lo que hizo la
 * pasarela; una liquidacion que desaparece deja el extracto bancario sin la
 * mitad de su explicacion y la clave hacia atras desde
 * {@code subscription_payments} apuntando al vacio — por eso su
 * {@code ON DELETE} es {@code RESTRICT}.
 *
 * <p>
 * <strong>Con {@code version}</strong>: el lote muta dos veces despues de nacer
 * —cuando llega la factura del proveedor y cuando se ata a la entrada del
 * banco— y son dos operarios distintos en dos momentos distintos.
 */
public class GatewaySettlement {

    private static final int MAX_GATEWAY_LENGTH = 40;
    private static final int MAX_SETTLEMENT_REFERENCE_LENGTH = 120;
    private static final int MAX_PROVIDER_INVOICE_REF_LENGTH = 60;
    private static final int MAX_PROVIDER_TAX_ID_LENGTH = 50;

    private static final int MAX_ASCII_CODE_POINT = 127;

    private final Long id;

    /**
     * La pasarela que pago el lote. <strong>No es una lista propia</strong>:
     * referencia el codigo de la fila de procesadores cuyo tipo es pasarela de
     * pago. Dos vocabularios para la misma pasarela son exactamente el defecto que
     * el modelo persigue, asi que aqui no hay enum — el valor lo fija ese catalogo.
     */
    private final String gateway;

    /** El identificador que la pasarela le puso al lote. Se compara exacto. */
    private final String settlementReference;

    /** La factura del proveedor. Nula hasta que llega; va con el NIT o no va. */
    private String providerInvoiceRef;

    /** El NIT del proveedor. Va con la referencia de la factura o no va. */
    private String providerTaxId;

    private final SettlementAmounts amounts;

    /** Cuantos cobros dice el lote que trae. Se contrasta con los enlazados. */
    private final int paymentCount;

    private final LocalDate settledOn;

    /** La entrada del extracto donde cayo el neto. Nula hasta que se ata. */
    private Long bankReceiptId;

    private final LocalDateTime createdDate;
    private final Long version;

    public GatewaySettlement(Long id, String gateway, String settlementReference,
            String providerInvoiceRef, String providerTaxId, SettlementAmounts amounts,
            int paymentCount, LocalDate settledOn, Long bankReceiptId, LocalDateTime createdDate,
            Long version) {
        validateAsciiReference("gateway", gateway, MAX_GATEWAY_LENGTH);
        validateAsciiReference("settlementReference", settlementReference,
                MAX_SETTLEMENT_REFERENCE_LENGTH);
        validateProviderInvoice(providerInvoiceRef, providerTaxId);
        if (amounts == null)
            throw new IllegalArgumentException("settlement amounts are required");
        validatePaymentCount(paymentCount);
        if (settledOn == null)
            throw new IllegalArgumentException("settledOn is required");
        this.id = id;
        this.gateway = gateway;
        this.settlementReference = settlementReference;
        this.providerInvoiceRef = providerInvoiceRef;
        this.providerTaxId = providerTaxId;
        this.amounts = amounts;
        this.paymentCount = paymentCount;
        this.settledOn = settledOn;
        this.bankReceiptId = bankReceiptId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Lote recien cargado: <strong>sin</strong> factura del proveedor y
     * <strong>sin</strong> entrada de banco, que es como llega siempre. Las dos
     * cosas se saben despues.
     */
    public static GatewaySettlement register(String gateway, String settlementReference,
            SettlementAmounts amounts, int paymentCount, LocalDate settledOn,
            LocalDateTime createdDate) {
        return new GatewaySettlement(null, gateway, settlementReference, null, null, amounts,
                paymentCount, settledOn, null, createdDate, null);
    }

    /**
     * Llego la factura del proveedor: se escriben las dos referencias a la vez.
     *
     * <p>
     * <strong>No se sobrescribe una ya escrita.</strong> Cambiar el soporte de un
     * gasto ya archivado no es una correccion, es perder el rastro del anterior: el
     * numero viejo desaparece sin quedar en ningun sitio y el reporte anual de
     * terceros deja de cuadrar con lo que se declaro. Si de verdad llego otra
     * factura, lo que cambio fue el gasto y eso es otra fila.
     */
    public void attachProviderInvoice(String invoiceRef, String taxId) {
        if (hasProviderInvoice())
            throw new ProviderInvoiceAlreadyAttachedException(id, this.providerInvoiceRef);
        if (invoiceRef == null || taxId == null)
            throw new IllegalArgumentException(
                    "providerInvoiceRef and providerTaxId are both required");
        validateProviderInvoice(invoiceRef, taxId);
        this.providerInvoiceRef = invoiceRef;
        this.providerTaxId = taxId;
    }

    /**
     * Se ato el lote a la linea del extracto por la que entro su neto. Es la ultima
     * milla de la conciliacion: hasta aqui el lote y el banco eran dos papeles
     * separados.
     *
     * <p>
     * <strong>Tampoco se reata.</strong> Mover un lote ya conciliado a otra entrada
     * del extracto deja la primera cuadrada contra nada, y ese descuadre no lo
     * denuncia ninguna constraint: la clave foranea sigue siendo valida.
     */
    public void linkBankReceipt(Long receiptId) {
        if (this.bankReceiptId != null)
            throw new BankReceiptAlreadyLinkedException(id, this.bankReceiptId);
        if (receiptId == null)
            throw new IllegalArgumentException("bankReceiptId is required");
        this.bankReceiptId = receiptId;
    }

    /** Si ya tiene el soporte del gasto. Lo que no lo tiene no es deducible. */
    public boolean hasProviderInvoice() {
        return providerInvoiceRef != null;
    }

    /**
     * Contrasta lo que el lote declara contra los cobros que de verdad cuelgan de
     * el. La cuenta de enlazados la trae quien llama, porque vive en otra tabla.
     */
    public PaymentCountReconciliation reconcileWith(long linkedPayments) {
        return new PaymentCountReconciliation(paymentCount, linkedPayments);
    }

    /**
     * Espejo de {@code chk_gateway_settlements_payment_count}. Un lote de cero
     * cobros no existe: si la pasarela no liquido nada, no hay liquidacion.
     */
    private static void validatePaymentCount(int paymentCount) {
        if (paymentCount <= 0)
            throw new IllegalArgumentException("paymentCount must be greater than zero");
    }

    /**
     * Espejo de {@code chk_gateway_settlements_provider_invoice}, con la misma
     * forma de bicondicional que la constraint: o las dos escritas o las dos nulas.
     * Las dos mitades importan — sin el NIT no hay reporte de terceros, y sin la
     * referencia no hay soporte que enseñar si rechazan la deduccion.
     */
    private static void validateProviderInvoice(String invoiceRef, String taxId) {
        if ((invoiceRef == null) != (taxId == null))
            throw new IllegalArgumentException(
                    "providerInvoiceRef and providerTaxId must be both present or both absent");
        if (invoiceRef == null)
            return;
        validateAsciiReference("providerInvoiceRef", invoiceRef, MAX_PROVIDER_INVOICE_REF_LENGTH);
        validateAsciiReference("providerTaxId", taxId, MAX_PROVIDER_TAX_ID_LENGTH);
    }

    /**
     * Las cuatro columnas de texto son {@code CHARACTER SET ascii}: un caracter
     * fuera de ASCII no lo trunca la base, lo <em>rechaza</em> con un
     * {@code Incorrect string value} que no dice de que fila viene. Comprobarlo
     * aqui convierte ese fallo de carga masiva en un mensaje que nombra el campo.
     */
    private static void validateAsciiReference(String field, String value, int maxLength) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
        if (value.length() > maxLength)
            throw new IllegalArgumentException(field + " must be " + maxLength + " chars or less");
        if (value.chars().anyMatch(codePoint -> codePoint > MAX_ASCII_CODE_POINT))
            throw new IllegalArgumentException(field + " must be ASCII");
    }

    public Long getId() {
        return id;
    }

    public String getGateway() {
        return gateway;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public String getProviderInvoiceRef() {
        return providerInvoiceRef;
    }

    public String getProviderTaxId() {
        return providerTaxId;
    }

    public SettlementAmounts getAmounts() {
        return amounts;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public LocalDate getSettledOn() {
        return settledOn;
    }

    public Long getBankReceiptId() {
        return bankReceiptId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
