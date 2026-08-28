package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code accounting_accounts} (changeset 342) — el plan de cuentas propio.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y no es estetica.</strong> La tabla es un catalogo global sin
 * columna de empresa; el dia que alguien le cuelgue un {@code @ManyToOne} a
 * companies, las cuatro reglas duras de aislamiento de BE-COV se activan sobre
 * la feature entera y rompen el build.
 *
 * <p>
 * <strong>{@code parent_code} va como escalar y no como asociacion.</strong> La
 * clave foranea autorreferente {@code fk_accounting_accounts_parent} apunta a
 * {@code code}, no a {@code id}: un {@code @ManyToOne} sobre esta misma entidad
 * traeria el arbol entero para usar diez caracteres, y obligaria a un
 * {@code @EntityGraph} en cada finder para evitar el N+1. La clave sigue
 * existiendo y vigilando en la base; lo que no existe es la navegacion desde
 * Java.
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna y
 * porque hay dos escrituras que editan: la correccion del nombre y el cierre de
 * la vigencia. Sin el, dos ediciones concurrentes se pisarian sin excepcion y
 * sin log.
 */
@Entity
@Table(name = "accounting_accounts")
public class AccountingAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", nullable = false, length = 20)
    private AccountClass accountClass;

    /** Nulo solo en la raiz. La otra mitad la cuida el CHECK del esquema. */
    @Column(name = "parent_code", length = 10)
    private String parentCode;

    /**
     * {@code account_level} y no {@code level}: {@code LEVEL} es palabra reservada
     * de MySQL 8 y obligaria a comillas invertidas en cada consulta.
     *
     * <p>
     * <strong>Es {@code byte} y no {@code int}, y eso NO es una micro-optimizacion:
     * es lo unico que deja arrancar el contexto.</strong> El changeset 342 declara
     * la columna {@code TINYINT} —correcto para un nivel que solo vale 1, 2, 4 o
     * 6—, Hibernate mapea {@code byte} a {@code Types.TINYINT} e {@code int} a
     * {@code Types.INTEGER}, y con {@code ddl-auto: validate} ese desajuste no
     * falla en esta rodaja: impide construir el {@code SessionFactory} y <b>ningun
     * {@code @SpringBootTest} del repositorio arranca</b>. Mismo precedente escrito
     * en {@code PlatformBillingConfigJpaEntity.singleton}.
     *
     * <p>
     * El dominio lo expone como {@code int} —su rango cabe de sobra y evita
     * conversiones en cada llamada—; la conversion vive en el mapper, que es el
     * unico sitio que conoce las dos formas.
     */
    @Column(name = "account_level", nullable = false)
    private byte accountLevel;

    /**
     * {@code TINYINT} pelado: un {@code TINYINT(1)} lo reporta el driver como
     * {@code BIT} y rompe {@code ddl-auto: validate}.
     */
    @Column(name = "postable", nullable = false)
    private boolean postable;

    @Column(name = "requires_third_party", nullable = false)
    private boolean requiresThirdParty;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountingAccountJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountClass getAccountClass() {
        return accountClass;
    }

    public void setAccountClass(AccountClass accountClass) {
        this.accountClass = accountClass;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public byte getAccountLevel() {
        return accountLevel;
    }

    public void setAccountLevel(byte accountLevel) {
        this.accountLevel = accountLevel;
    }

    public boolean isPostable() {
        return postable;
    }

    public void setPostable(boolean postable) {
        this.postable = postable;
    }

    public boolean isRequiresThirdParty() {
        return requiresThirdParty;
    }

    public void setRequiresThirdParty(boolean requiresThirdParty) {
        this.requiresThirdParty = requiresThirdParty;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
