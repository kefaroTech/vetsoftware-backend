package com.vetsoftware.app.accountingaccount.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Una cuenta del plan contable propio: el catalogo contra el que se asienta
 * todo lo que mueve dinero.
 *
 * <h2>Catalogo global: aqui no hay empresa, y es a proposito</h2>
 *
 * <p>
 * El plan de cuentas es de VetSoftware, no de la clinica: son <em>nuestros</em>
 * libros. La tabla {@code accounting_accounts} no tiene columna
 * {@code company_id} y {@code AccountingAccountJpaEntity} no alcanza
 * {@code CompanyJpaEntity} por ninguna asociacion. El dia que alguien le
 * cuelgue un {@code @ManyToOne} a companies «para saber quien la cargo», las
 * cuatro reglas duras de aislamiento de BE-COV se activan sobre la feature
 * entera y rompen el build, aunque no haya nada que acotar.
 *
 * <h2>El codigo es unico globalmente, no por vigencia</h2>
 *
 * <p>
 * La cuenta lleva {@code validFrom}/{@code validTo}, pero
 * {@code uq_accounting_accounts_code} es sobre {@code code} a secas: un codigo
 * no puede significar dos cosas distintas en dos epocas. Si el plan cambia el
 * significado de un codigo, se abre un codigo nuevo. La columna es
 * {@code ascii_bin} para que la comparacion sea exacta —es un identificador
 * ajeno, no texto de negocio—: con la colacion heredada, dos codigos que
 * difirieran solo en el relleno o en el caso serian la misma cuenta.
 *
 * <h2>Las dos invariantes que sostienen el balance de prueba</h2>
 *
 * <ul>
 * <li><b>Solo el ultimo nivel admite asiento</b>
 * ({@code chk_accounting_accounts_postable}): {@code postable} exige
 * {@code accountLevel == 6}. Sin esto se asienta contra un grupo y el balance
 * deja de cuadrar por arrastre, sin un solo error.</li>
 * <li><b>La raiz es la unica sin padre</b>
 * ({@code chk_accounting_accounts_parent}), y la regla lleva las <em>dos</em>
 * ramas escritas. En SQL un {@code CHECK} que evalua a {@code NULL} acepta la
 * fila, asi que sin la segunda mitad una subcuenta huerfana entraria en
 * silencio; aqui pasa lo mismo si solo se comprueba una direccion.</li>
 * </ul>
 */
public class AccountingAccount {

    private static final int MAX_CODE_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 120;

    /**
     * Clase, grupo, cuenta y subcuenta. Espejo de
     * {@code chk_accounting_accounts_level}. La columna se llama
     * {@code account_level} y no {@code level} porque {@code LEVEL} es palabra
     * reservada de MySQL 8 y obligaria a comillas invertidas en cada consulta.
     */
    private static final Set<Integer> LEVELS = Set.of(1, 2, 4, 6);

    /** El unico nivel que admite asiento. */
    private static final int POSTABLE_LEVEL = 6;

    /** El unico nivel que no tiene padre. */
    private static final int ROOT_LEVEL = 1;

    private final Long id;
    private final String code;
    private final String name;
    private final AccountClass accountClass;

    /** Vacio <b>solo</b> en la raiz. */
    private final String parentCode;

    private final int accountLevel;
    private final boolean postable;
    private final boolean requiresThirdParty;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public AccountingAccount(Long id, String code, String name, AccountClass accountClass,
            String parentCode, int accountLevel, boolean postable, boolean requiresThirdParty,
            LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate, boolean enabled,
            Long version) {
        validate(code, name, accountClass, parentCode, accountLevel, postable, validFrom, validTo,
                createdDate);
        this.id = id;
        this.code = code;
        this.name = name;
        this.accountClass = accountClass;
        this.parentCode = parentCode;
        this.accountLevel = accountLevel;
        this.postable = postable;
        this.requiresThirdParty = requiresThirdParty;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /**
     * Cuenta nueva. Nace habilitada y sin version: la asigna Hibernate al insertar.
     *
     * <p>
     * Acepta {@code validTo} porque cargar el plan historico es un caso real: una
     * cuenta que dejo de usarse en 2024 entra ya cerrada.
     */
    public static AccountingAccount create(String code, String name, AccountClass accountClass,
            String parentCode, int accountLevel, boolean postable, boolean requiresThirdParty,
            LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate) {
        return new AccountingAccount(null, code, name, accountClass, parentCode, accountLevel,
                postable, requiresThirdParty, validFrom, validTo, createdDate, true, null);
    }

    /**
     * Lo unico editable de una cuenta ya publicada: como se llama y si exige
     * tercero identificado.
     *
     * <p>
     * <strong>El codigo, la clase, el nivel y el padre no se tocan.</strong>
     * Cambiar cualquiera de los cuatro reescribiria el significado de todos los
     * asientos que ya apuntan a esta cuenta —{@code account_mappings} la referencia
     * <b>por codigo</b>, con tres claves foraneas— y despues no habria forma de
     * distinguir los asientos hechos con el significado viejo. Si el plan cambia,
     * se abre un codigo nuevo y se cierra este.
     *
     * <p>
     * Conserva la version, que es lo que mantiene el {@code save} posterior dentro
     * del ciclo leer-modificar-guardar con bloqueo optimista.
     */
    public AccountingAccount update(String newName, boolean newRequiresThirdParty) {
        return new AccountingAccount(id, code, newName, accountClass, parentCode, accountLevel,
                postable, newRequiresThirdParty, validFrom, validTo, createdDate, enabled, version);
    }

    /**
     * Cierra la vigencia poniendo la fecha de fin.
     *
     * <p>
     * <strong>Se niega a cerrar lo que ya estaba cerrado</strong>, y esa negativa
     * es toda la barandilla que hay: la base no la pone.
     */
    public AccountingAccount close(LocalDate closedOn) {
        if (validTo != null)
            throw new AccountingAccountAlreadyClosedException(id, validTo);
        return new AccountingAccount(id, code, name, accountClass, parentCode, accountLevel,
                postable, requiresThirdParty, validFrom, closedOn, createdDate, enabled, version);
    }

    /**
     * Si la cuenta esta vigente ese dia.
     *
     * <p>
     * <strong>El limite superior es estricto y el inferior no.</strong> El dia
     * escrito en {@code validTo} es el primero en que la cuenta <em>ya no</em>
     * vale, de modo que la que se cierra el 1 de enero y la que la releva ese mismo
     * dia se turnan sin dejar hueco ni pisarse.
     */
    public boolean isEffectiveOn(LocalDate on) {
        return !validFrom.isAfter(on) && (validTo == null || validTo.isAfter(on));
    }

    /** La vigencia sigue abierta: no tiene fecha de fin. */
    public boolean isOpen() {
        return validTo == null;
    }

    private static void validate(String code, String name, AccountClass accountClass,
            String parentCode, int accountLevel, boolean postable, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate) {
        validateCode(code);
        validateName(name);
        if (accountClass == null)
            throw new IllegalArgumentException("accountClass is required");
        validateLevel(accountLevel, postable);
        validateParent(accountLevel, parentCode);
        validateValidity(validFrom, validTo);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * La columna es {@code ascii_bin}: la comparacion es byte a byte, asi que un
     * espacio de relleno abriria una segunda cuenta con el mismo codigo aparente y
     * {@code uq_accounting_accounts_code} no las veria como iguales.
     */
    private static void validateCode(String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > MAX_CODE_LENGTH)
            throw new IllegalArgumentException("code must be 10 chars or less");
        if (!code.equals(code.trim()))
            throw new IllegalArgumentException("code must not have leading or trailing spaces");
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > MAX_NAME_LENGTH)
            throw new IllegalArgumentException("name must be 120 chars or less");
    }

    /**
     * Espejo de {@code chk_accounting_accounts_level} y de
     * {@code chk_accounting_accounts_postable}. La segunda mitad es la que impide
     * asentar contra un grupo: sin ella el balance de prueba deja de cuadrar por
     * arrastre y no hay ningun error que lo delate.
     */
    private static void validateLevel(int accountLevel, boolean postable) {
        if (!LEVELS.contains(accountLevel))
            throw new IllegalArgumentException("accountLevel must be one of 1, 2, 4 or 6");
        if (postable && accountLevel != POSTABLE_LEVEL)
            throw new IllegalArgumentException("only a level 6 account can be postable");
    }

    /**
     * Espejo de {@code chk_accounting_accounts_parent}, con las <b>dos</b> ramas.
     * Comprobar solo que la raiz no tiene padre dejaria entrar una subcuenta
     * huerfana, que es el arbol roto sin un solo error.
     */
    private static void validateParent(int accountLevel, String parentCode) {
        if (accountLevel == ROOT_LEVEL) {
            if (parentCode != null)
                throw new IllegalArgumentException("the root account must not have a parent code");
            return;
        }
        if (parentCode == null || parentCode.isBlank())
            throw new IllegalArgumentException("parentCode is required below level 1");
        if (parentCode.length() > MAX_CODE_LENGTH)
            throw new IllegalArgumentException("parentCode must be 10 chars or less");
    }

    /** Espejo de {@code chk_accounting_accounts_validity}. */
    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null)
            throw new IllegalArgumentException("validFrom is required");
        if (validTo != null && !validTo.isAfter(validFrom))
            throw new IllegalArgumentException("validTo must be after validFrom");
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountClass getAccountClass() {
        return accountClass;
    }

    public String getParentCode() {
        return parentCode;
    }

    public int getAccountLevel() {
        return accountLevel;
    }

    public boolean isPostable() {
        return postable;
    }

    public boolean isRequiresThirdParty() {
        return requiresThirdParty;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }
}
