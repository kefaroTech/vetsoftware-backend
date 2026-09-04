package com.vetsoftware.app.bankreceipt.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una linea del extracto bancario: la ultima milla de la conciliacion.
 *
 * <p>
 * <strong>Sin empresa, y no por descuido.</strong> Antes de identificar una
 * entrada no hay cliente: una consignacion que llega al banco es un importe,
 * una fecha y una referencia, y quien la mando es justamente lo que hay que
 * averiguar. Poner aqui un {@code companyId} obligaria a inventarselo en el
 * momento de la carga, que es exactamente el dato que todavia no se tiene.
 *
 * <p>
 * <strong>El importe admite negativos, y esa es la regla que mas facil se
 * rompe.</strong> El {@code CHECK} del esquema es {@code amount <> 0}, no
 * {@code amount > 0}: un cargo del banco, una comision o la devolucion de un
 * cheque entran en el extracto con signo negativo. Escribir aqui un
 * {@code amount > 0} —el reflejo aprendido de las demas tablas de dinero, donde
 * el importe es una magnitud— seria mas restrictivo que la base y volveria
 * inexpresable la mitad del extracto: el operario no podria cargar el fichero
 * del mes y no sabria por que.
 *
 * <p>
 * <strong>La referencia se compara EXACTO.</strong> Las dos columnas de texto
 * son {@code ascii_bin}, asi que {@code AB12} y {@code ab12} son entradas
 * distintas. No es una sutileza de collation: bajo la colacion heredada del
 * esquema las dos serian la misma fila y la segunda consignacion del dia se
 * descartaria como duplicada.
 *
 * <p>
 * <strong>No se borra, ni en logico ni en fisico.</strong> No hay
 * {@code enabled} en la tabla ni metodo de baja aqui. Una entrada que no
 * corresponde a nadie pasa a {@link BankReceiptStatus#DISCARDED} y queda: el
 * extracto es el espejo de lo que hizo el banco y una linea que desaparece deja
 * el cuadre sin la mitad de su explicacion.
 *
 * <p>
 * <strong>Con {@code version}</strong>: el estado muta cuando la entrada se
 * identifica, y dos operarios atendiendo la misma bandeja se pisarian sin
 * ruido.
 */
public class BankReceipt {

    private static final int MAX_BANK_ACCOUNT_REF_LENGTH = 60;
    private static final int MAX_BANK_REFERENCE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    /**
     * {@code DECIMAL(19,2)}: un tercer decimal lo redondearia la base en silencio.
     */
    private static final int MAX_AMOUNT_SCALE = 2;

    private static final int MAX_ASCII_CODE_POINT = 127;

    private final Long id;

    /**
     * La cuenta de Lumbre a la que entro el dinero, tal como la nombra el banco.
     */
    private final String bankAccountRef;

    /** El identificador que puso el banco a la operacion. Se compara exacto. */
    private final String bankReference;

    private final LocalDate receivedOn;

    /** Importe con signo: negativo es un cargo o una devolucion del banco. */
    private final BigDecimal amount;

    private final String description;

    private BankReceiptStatus status;

    /** Cuando salio de la bandeja. Nulo si y solo si sigue sin identificar. */
    private LocalDateTime identifiedAt;

    private final LocalDateTime createdDate;
    private final Long version;

    public BankReceipt(Long id, String bankAccountRef, String bankReference, LocalDate receivedOn,
            BigDecimal amount, String description, BankReceiptStatus status,
            LocalDateTime identifiedAt, LocalDateTime createdDate, Long version) {
        validate(bankAccountRef, bankReference, receivedOn, amount, description, status,
                identifiedAt);
        this.id = id;
        this.bankAccountRef = bankAccountRef;
        this.bankReference = bankReference;
        this.receivedOn = receivedOn;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.identifiedAt = identifiedAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Entrada recien cargada del extracto: nace
     * {@link BankReceiptStatus#UNIDENTIFIED} y <strong>sin</strong>
     * {@code identifiedAt}, que es la unica combinacion que la base admite para ese
     * estado.
     */
    public static BankReceipt register(String bankAccountRef, String bankReference,
            LocalDate receivedOn, BigDecimal amount, String description,
            LocalDateTime createdDate) {
        return new BankReceipt(null, bankAccountRef, bankReference, receivedOn, amount, description,
                BankReceiptStatus.UNIDENTIFIED, null, createdDate, null);
    }

    /**
     * Se supo de quien era la consignacion: sale de la bandeja y queda sellada la
     * hora.
     *
     * @param identifiedAt
     *            del reloj inyectado del caso de uso, nunca de un
     *            {@code LocalDateTime.now()} pelado: la fecha en que una entrada se
     *            resolvio es la que decide en que mes cuenta el trabajo pendiente
     */
    public void identify(LocalDateTime identifiedAt) {
        resolve(BankReceiptStatus.IDENTIFIED, identifiedAt);
    }

    /**
     * No corresponde a ningun cliente. Sale de la bandeja igual que una
     * identificada —y sella la misma columna— porque el {@code CHECK} trata los dos
     * estados finales por igual: lo que la base exige no es «que se sepa el dueño»
     * sino «que conste cuando se dejo de buscar».
     */
    public void discard(LocalDateTime identifiedAt) {
        resolve(BankReceiptStatus.DISCARDED, identifiedAt);
    }

    /** Sigue en la bandeja del mes: es trabajo pendiente. */
    public boolean isUnidentified() {
        return status == BankReceiptStatus.UNIDENTIFIED;
    }

    private void resolve(BankReceiptStatus target, LocalDateTime resolvedAt) {
        if (!isUnidentified())
            throw new BankReceiptAlreadyResolvedException(id, status);
        if (resolvedAt == null)
            throw new IllegalArgumentException("identifiedAt is required");
        this.status = target;
        this.identifiedAt = resolvedAt;
    }

    private static void validate(String bankAccountRef, String bankReference, LocalDate receivedOn,
            BigDecimal amount, String description, BankReceiptStatus status,
            LocalDateTime identifiedAt) {
        validateAsciiReference("bankAccountRef", bankAccountRef, MAX_BANK_ACCOUNT_REF_LENGTH);
        validateAsciiReference("bankReference", bankReference, MAX_BANK_REFERENCE_LENGTH);
        if (receivedOn == null)
            throw new IllegalArgumentException("receivedOn is required");
        validateAmount(amount);
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH)
            throw new IllegalArgumentException("description must be 255 chars or less");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        validateIdentifiedAt(status, identifiedAt);
    }

    /**
     * Espejo de {@code chk_bank_receipts_amount}, que es {@code amount <> 0} y NO
     * {@code amount > 0}: en un extracto el signo es informacion, y el unico
     * importe que no significa nada es el cero.
     *
     * <p>
     * La escala se comprueba aqui porque la columna es {@code DECIMAL(19,2)} y un
     * tercer decimal <em>no</em> es un error para MySQL: lo redondea y sigue. Un
     * centavo perdido en silencio en el extracto es un cuadre que no cierra y nadie
     * sabe por que.
     */
    private static void validateAmount(BigDecimal amount) {
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        if (amount.signum() == 0)
            throw new IllegalArgumentException("amount cannot be zero");
        if (amount.stripTrailingZeros().scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException("amount must have 2 decimals or less");
    }

    /**
     * Espejo de {@code chk_bank_receipts_identified}, y con la misma forma de
     * bicondicional que la constraint: {@code identified_at} es nulo <em>si y solo
     * si</em> el estado es {@code UNIDENTIFIED}. Las dos mitades importan — sin la
     * primera, una entrada archivada pierde la fecha en que se archivo; sin la
     * segunda, una que sigue en la bandeja aparece resuelta.
     */
    private static void validateIdentifiedAt(BankReceiptStatus status, LocalDateTime identifiedAt) {
        if (status == BankReceiptStatus.UNIDENTIFIED && identifiedAt != null)
            throw new IllegalArgumentException("identifiedAt must be absent while unidentified");
        if (status != BankReceiptStatus.UNIDENTIFIED && identifiedAt == null)
            throw new IllegalArgumentException("identifiedAt is required once resolved");
    }

    /**
     * Las dos columnas de referencia son {@code CHARACTER SET ascii}: un caracter
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

    public String getBankAccountRef() {
        return bankAccountRef;
    }

    public String getBankReference() {
        return bankReference;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public BankReceiptStatus getStatus() {
        return status;
    }

    public LocalDateTime getIdentifiedAt() {
        return identifiedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
