package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code bank_receipts} — el extracto bancario.
 *
 * <p>
 * <strong>Esta entidad NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y eso es una propiedad que hay que conservar.</strong> No es que
 * la relacion todavia no se haya escrito: es que antes de identificar una
 * entrada no hay cliente. Ademas tiene una consecuencia mecanica que conviene
 * saber antes de añadir el primer {@code @ManyToOne}: el discriminador de las
 * cuatro reglas duras de BE-COV es «alguna entidad de la feature llega a la
 * tabla de empresas». Colgar aqui una asociacion que llegue —aunque sea
 * indirecta y aunque sea {@code LAZY}— las activa de golpe sobre <em>toda</em>
 * la feature, y los seis puertos pasarian a tener que acotar por un
 * {@code companyId} que la tabla no tiene.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el estado muta cuando la entrada se
 * identifica o se descarta. Dos operarios atendiendo la misma bandeja leen los
 * dos {@code UNIDENTIFIED}, los dos pasan la comprobacion del dominio y sin el
 * bloqueo optimista el segundo pisaria al primero sin excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: no hay borrado
 * logico, asi que tampoco existe aqui la trampa de los dos parametros que
 * documenta {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * <strong>Las dos referencias no llevan {@code columnDefinition}.</strong> Su
 * juego de caracteres {@code ascii} y su colacion {@code ascii_bin} los fija el
 * changeset 325 con un {@code MODIFY COLUMN}; declararlos otra vez aqui seria
 * duplicar la decision en dos sitios que pueden divergir, y el que manda es el
 * esquema.
 */
@Entity
@Table(name = "bank_receipts")
public class BankReceiptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_account_ref", nullable = false, length = 60)
    private String bankAccountRef;

    @Column(name = "bank_reference", nullable = false, length = 120)
    private String bankReference;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankReceiptStatus status;

    @Column(name = "identified_at")
    private LocalDateTime identifiedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected BankReceiptJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBankAccountRef() {
        return bankAccountRef;
    }

    public void setBankAccountRef(String bankAccountRef) {
        this.bankAccountRef = bankAccountRef;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BankReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(BankReceiptStatus status) {
        this.status = status;
    }

    public LocalDateTime getIdentifiedAt() {
        return identifiedAt;
    }

    public void setIdentifiedAt(LocalDateTime identifiedAt) {
        this.identifiedAt = identifiedAt;
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
