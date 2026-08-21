package com.vetsoftware.app.openaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpenAccount {
    private Long id;
    private OwnerRef owner;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    // Saldo = totalAmount - paidAmount. Su significado depende del estado:
    // - OPEN: saldo realmente pendiente de cobro (cuenta por cobrar).
    // - CLOSE: siempre 0 (el cierre cobrado exige saldo cero; ver changeStatus).
    // - CANCEL: monto dado de baja / incobrable (pérdida), NO una cuenta por
    // cobrar.
    // SALVAGUARDA: cualquier agregado de "cuentas por cobrar" debe sumar SOLO
    // cuentas OPEN
    // (filtrar por status), nunca todas, o contaría como cobrable la pérdida de las
    // canceladas.
    private BigDecimal outstandingAmount;
    private CompanyRef company;
    private BranchRef branch;
    private OpenAccountStatus status;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private EmployeeRef closedBy;
    private LocalDateTime closedAt;
    private String closeReason;
    // Reverso contable de una cuenta ya facturada: solo se marca tras la validacion
    // DIAN de la nota
    // credito que la corrige (subordinacion fiscal del void). NUNCA se setea antes
    // de esa validacion.
    private boolean reversed;
    private LocalDateTime reversedAt;
    private Long version;

    public OpenAccount(Long id, OwnerRef owner, BigDecimal totalAmount, BigDecimal paidAmount,
            BigDecimal outstandingAmount, CompanyRef company, BranchRef branch,
            OpenAccountStatus status, EmployeeRef createdBy, LocalDateTime createdDate,
            boolean enabled, EmployeeRef closedBy, LocalDateTime closedAt, String closeReason,
            boolean reversed, LocalDateTime reversedAt, Long version) {
        validate(owner, totalAmount, paidAmount, outstandingAmount, company, branch, status,
                createdBy);
        this.id = id;
        this.owner = owner;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.outstandingAmount = outstandingAmount;
        this.company = company;
        this.branch = branch;
        this.status = status;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.closeReason = closeReason;
        this.reversed = reversed;
        this.reversedAt = reversedAt;
        this.version = version;
    }

    public static OpenAccount create(OwnerRef owner, CompanyRef company, BranchRef branch,
            EmployeeRef createdBy) {
        return new OpenAccount(null, owner, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                company, branch, OpenAccountStatus.OPEN, createdBy, LocalDateTime.now(), true, null,
                null, null, false, null, null);
    }

    /**
     * Marca la cuenta como reversada por nota credito validada. Es el efecto en
     * cartera del void subordinado a la nota electronica, y la unica via para
     * estamparlo: el adaptador que escribia la fila a mano reimplementaba media
     * regla (#124).
     *
     * <p>
     * Solo aplica a una cuenta CLOSE —una OPEN no tiene todavia factura que
     * corregir y una CANCEL se dio de baja sin emitir ninguna—, y es idempotente:
     * una segunda llamada no reescribe la fecha. Devuelve si esta llamada cambio
     * algo, para que el llamador sepa si tiene que persistir.
     */
    public boolean markReversed(LocalDateTime when) {
        if (this.reversed)
            return false;
        if (this.status != OpenAccountStatus.CLOSE) {
            throw new IllegalStateException(
                    "Solo se puede reversar una cuenta cerrada (facturada); estado actual: "
                            + this.status);
        }
        this.reversed = true;
        this.reversedAt = when != null ? when : LocalDateTime.now();
        return true;
    }

    /**
     * Cambia el estado de la cuenta. Reglas de negocio: - Solo desde OPEN (CLOSE y
     * CANCEL son terminales). - Solo hacia CLOSE o CANCEL. - CANCEL exige motivo
     * (anulación/incobrable = pérdida, debe justificarse). CLOSE no. Registra
     * quién, cuándo y por qué (trazabilidad contable).
     */
    public void changeStatus(OpenAccountStatus newStatus, EmployeeRef closedBy, String reason) {
        if (newStatus == null)
            throw new IllegalArgumentException("status is required");
        if (this.status != OpenAccountStatus.OPEN || (newStatus != OpenAccountStatus.CLOSE
                && newStatus != OpenAccountStatus.CANCEL)) {
            throw new InvalidOpenAccountStatusTransitionException(this.status, newStatus);
        }
        // CLOSE = cobrada: invariante contable, el saldo debe estar en cero. El cobro
        // del saldo se
        // registra como abono ANTES del cierre (el front lo hace; el recálculo deja
        // outstanding=0).
        // CANCEL (incobrable/anulada) sí permite saldo > 0.
        if (newStatus == OpenAccountStatus.CLOSE
                && this.outstandingAmount.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(
                    "No se puede cerrar como cobrada una cuenta con saldo pendiente; "
                            + "registra el cobro del saldo o cancélala.");
        }
        if (newStatus == OpenAccountStatus.CANCEL && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("reason is required to cancel");
        }
        this.status = newStatus;
        this.closedBy = closedBy;
        this.closedAt = LocalDateTime.now();
        this.closeReason = reason;
    }

    public void recalculate(BigDecimal total, BigDecimal paid) {
        if (total == null)
            throw new IllegalArgumentException("totalAmount is required");
        if (total.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("totalAmount cannot be negative");
        if (paid == null)
            throw new IllegalArgumentException("paidAmount is required");
        if (paid.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("paidAmount cannot be negative");
        BigDecimal recalculated = total.subtract(paid);
        // ULTIMA LINEA DE DEFENSA DEL SALDO. Aqui se validaban los dos sumandos y NO
        // el signo de la resta, asi que un cobrado mayor que el facturado se
        // persistia tal cual: total 100.000 y un abono editado a 150.000 dejaban el
        // outstanding en -50.000 con HTTP 200, sin concurrencia ninguna. Cobrado >
        // facturado no es un saldo negativo legitimo sino un estado IMPOSIBLE, y
        // desde la fila corrupta el numero rojo se propaga a la cartera, al cierre de
        // caja y a cualquier agregado de cuentas por cobrar, donde ya no se sabe de
        // donde salio. Las guardas de sobrepago viven en los casos de uso que
        // registran o editan el abono, que es donde se puede dar un mensaje util;
        // esta cierra las rutas que hoy no la tienen —reactivar un abono
        // deshabilitado recalcula sin guard— y las que se escriban manana. La
        // mutacion va DESPUES de la comprobacion: una cuenta rechazada no puede
        // quedar a medio actualizar.
        if (recalculated.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalStateException(
                    "El total cobrado (" + paid + ") no puede superar el total facturado (" + total
                            + "): dejaria la cuenta con saldo negativo.");
        this.totalAmount = total;
        this.paidAmount = paid;
        this.outstandingAmount = recalculated;
    }

    private static void validate(OwnerRef owner, BigDecimal totalAmount, BigDecimal paidAmount,
            BigDecimal outstandingAmount, CompanyRef company, BranchRef branch,
            OpenAccountStatus status, EmployeeRef createdBy) {
        if (owner == null)
            throw new IllegalArgumentException("owner is required");
        if (totalAmount == null)
            throw new IllegalArgumentException("totalAmount is required");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("totalAmount cannot be negative");
        if (paidAmount == null)
            throw new IllegalArgumentException("paidAmount is required");
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("paidAmount cannot be negative");
        if (outstandingAmount == null)
            throw new IllegalArgumentException("outstandingAmount is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (branch == null)
            throw new IllegalArgumentException("branch is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (createdBy == null)
            throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() {
        return id;
    }

    public OwnerRef getOwner() {
        return owner;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public BranchRef getBranch() {
        return branch;
    }

    public OpenAccountStatus getStatus() {
        return status;
    }

    public EmployeeRef getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public EmployeeRef getClosedBy() {
        return closedBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public boolean isReversed() {
        return reversed;
    }

    public LocalDateTime getReversedAt() {
        return reversedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
