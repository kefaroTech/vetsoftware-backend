package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code gateway_settlements} — lo que la pasarela liquido de verdad.
 *
 * <p>
 * <strong>Esta entidad NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y eso es una propiedad que hay que conservar.</strong> No es que
 * la relacion no se haya escrito todavia: es que el lote agrupa los cobros de
 * muchas empresas y no hay una a la que pertenezca. Ademas tiene una
 * consecuencia mecanica que conviene saber antes de anadir el primer
 * {@code @ManyToOne}: el discriminador de las cuatro reglas duras de BE-COV es
 * «alguna entidad de la feature llega a la tabla de empresas». Colgar aqui una
 * asociacion que llegue —aunque sea indirecta y aunque sea {@code LAZY}— las
 * activa de golpe sobre <em>toda</em> la feature, y los seis puertos pasarian a
 * tener que acotar por un {@code companyId} que la tabla no tiene.
 *
 * <p>
 * <strong>{@code bank_receipt_id} es un {@code Long} y no un
 * {@code @ManyToOne}.</strong> Esta rodaja no necesita ni un dato de la entrada
 * del extracto —el dominio guarda el id pelado—, asi que una asociacion solo
 * anadiria un proxy que inicializar, un {@code @EntityGraph} obligatorio para
 * no caer en N+1 en el listado y una sobrecarga de mapper para el camino de
 * escritura. El CLAUDE.md lo dice explicitamente: si solo necesitas el id, el
 * id basta.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el lote muta dos veces despues de
 * nacer —la factura del proveedor y la entrada de banco—, en dos momentos
 * distintos y por dos operarios distintos. Sin bloqueo optimista el segundo
 * pisaria al primero sin excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: no hay borrado
 * logico, asi que tampoco existe aqui la trampa de los dos parametros que
 * documenta {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * <strong>Las cuatro referencias no llevan {@code columnDefinition}.</strong>
 * Su juego de caracteres {@code ascii} y su colacion {@code ascii_bin} los fija
 * el changeset 326 con un {@code MODIFY COLUMN}; declararlos otra vez aqui
 * seria duplicar la decision en dos sitios que pueden divergir, y el que manda
 * es el esquema.
 */
@Entity
@Table(name = "gateway_settlements")
public class GatewaySettlementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway", nullable = false, length = 40)
    private String gateway;

    @Column(name = "settlement_reference", nullable = false, length = 120)
    private String settlementReference;

    @Column(name = "provider_invoice_ref", length = 60)
    private String providerInvoiceRef;

    @Column(name = "provider_tax_id", length = 50)
    private String providerTaxId;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    /** El impuesto de la comision, en su propia columna. Ver el dominio. */
    @Column(name = "fee_tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeTaxAmount;

    /** El cuatro por mil de la salida, en su propia columna. Ver el dominio. */
    @Column(name = "gmf_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal gmfAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    /**
     * {@code int} y no {@code Integer}: la columna es {@code NOT NULL} con
     * {@code CHECK (payment_count > 0)}, asi que no hay estado «sin contar» que
     * representar y un envoltorio solo anadiria una desreferencia que puede ser
     * nula.
     */
    @Column(name = "payment_count", nullable = false)
    private int paymentCount;

    @Column(name = "settled_on", nullable = false)
    private LocalDate settledOn;

    @Column(name = "bank_receipt_id")
    private Long bankReceiptId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected GatewaySettlementJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public void setSettlementReference(String settlementReference) {
        this.settlementReference = settlementReference;
    }

    public String getProviderInvoiceRef() {
        return providerInvoiceRef;
    }

    public void setProviderInvoiceRef(String providerInvoiceRef) {
        this.providerInvoiceRef = providerInvoiceRef;
    }

    public String getProviderTaxId() {
        return providerTaxId;
    }

    public void setProviderTaxId(String providerTaxId) {
        this.providerTaxId = providerTaxId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getFeeTaxAmount() {
        return feeTaxAmount;
    }

    public void setFeeTaxAmount(BigDecimal feeTaxAmount) {
        this.feeTaxAmount = feeTaxAmount;
    }

    public BigDecimal getGmfAmount() {
        return gmfAmount;
    }

    public void setGmfAmount(BigDecimal gmfAmount) {
        this.gmfAmount = gmfAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public LocalDate getSettledOn() {
        return settledOn;
    }

    public void setSettledOn(LocalDate settledOn) {
        this.settledOn = settledOn;
    }

    public Long getBankReceiptId() {
        return bankReceiptId;
    }

    public void setBankReceiptId(Long bankReceiptId) {
        this.bankReceiptId = bankReceiptId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
