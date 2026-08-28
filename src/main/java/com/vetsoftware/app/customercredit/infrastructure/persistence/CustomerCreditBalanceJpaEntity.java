package com.vetsoftware.app.customercredit.infrastructure.persistence;

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
 * {@code customer_credit_balances} — una fila por empresa, y no es la verdad.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>, porque muta en cada aplicacion de
 * saldo. Que ademas se mueva con un {@code UPDATE} condicional no la exime: la
 * columna existe en el esquema y la regla exige que la entidad la declare o
 * figure en la lista de exenciones, y aqui declararla es lo correcto.
 *
 * <p>
 * <strong>Sin {@code created_date} y sin {@code enabled}</strong>, porque la
 * tabla no los tiene: es una tabla derivada que se tira y se rehace, y darla de
 * baja logicamente crearia un estado que nadie sabe interpretar.
 *
 * <p>
 * <strong>El importe no lleva valor por defecto</strong>, tambien por
 * convencion del modelo: un importe con defecto cero convierte un fallo de
 * calculo en un dato plausible. La fila nace escribiendo su cero.
 *
 * <p>
 * <strong>Ojo con como se escribe esta entidad.</strong> El saldo NO se mueve
 * por el ciclo leer-modificar-guardar: se mueve con el {@code UPDATE}
 * condicional de {@link CustomerCreditBalanceJpaRepository#applyDelta}, que
 * lleva la barandilla dentro de su propio {@code WHERE}. Por eso esta clase no
 * la usa ningun {@code save} de negocio y sus mutadores existen solo para el
 * mapeo de lectura.
 */
@Entity
@Table(name = "customer_credit_balances")
public class CustomerCreditBalanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAmount;

    @Column(name = "next_expiry_on")
    private LocalDate nextExpiryOn;

    @Column(name = "recalculated_at", nullable = false)
    private LocalDateTime recalculatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CustomerCreditBalanceJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public LocalDate getNextExpiryOn() {
        return nextExpiryOn;
    }

    public void setNextExpiryOn(LocalDate nextExpiryOn) {
        this.nextExpiryOn = nextExpiryOn;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public void setRecalculatedAt(LocalDateTime recalculatedAt) {
        this.recalculatedAt = recalculatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
