package com.vetsoftware.app.openaccount.testsupport;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo openaccount.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code OpenAccount.create(...)} / {@code changeStatus(...)}: esos
 * factories/mutadores ponen {@code
 * LocalDateTime.now()} y harian no deterministas las aserciones sobre
 * {@code createdDate}/{@code closedAt} (deuda de Clock registrada en el
 * CLAUDE.md).
 */
public final class OpenAccountMother {

    public static final Long OPEN_ACCOUNT_ID = 100L;
    public static final Long OWNER_ID = 2L;
    public static final Long COMPANY_ID = 9L;
    public static final Long BRANCH_ID = 1L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long OTHER_EMPLOYEE_ID = 5L;

    public static final OwnerRef OWNER = new OwnerRef(OWNER_ID, "Juan Perez", "CC123");
    public static final OwnerRef OTRO_OWNER = new OwnerRef(3L, "Maria Lopez", "CC456");
    public static final CompanyRef COMPANY = new CompanyRef(COMPANY_ID, "Vet SAS", "900123456");
    public static final CompanyRef OTRA_COMPANY = new CompanyRef(90L, "Otra Vet", "900654321");
    public static final BranchRef BRANCH = new BranchRef(BRANCH_ID, "Principal", "PRINCIPAL");
    public static final EmployeeRef CREADO_POR = new EmployeeRef(EMPLOYEE_ID, "Dra. Vet");
    public static final EmployeeRef CERRADO_POR = new EmployeeRef(OTHER_EMPLOYEE_ID, "Dr. Otro");

    public static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime CERRADA_EL = LocalDateTime.of(2026, 1, 20, 18, 0);

    private OpenAccountMother() {
    }

    /** Cuenta abierta, habilitada, sin saldo. El caso por defecto. */
    public static OpenAccount abierta() {
        return abierta(OPEN_ACCOUNT_ID);
    }

    public static OpenAccount abierta(Long id) {
        return new OpenAccount(id, OWNER, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                COMPANY, BRANCH, OpenAccountStatus.OPEN, CREADO_POR, CREADA, true, null, null, null,
                false, null, 1L);
    }

    /** Cuenta abierta con saldo pendiente (total &gt; pagado). */
    public static OpenAccount abiertaConSaldo(String total, String pagado) {
        BigDecimal totalAmount = new BigDecimal(total);
        BigDecimal paidAmount = new BigDecimal(pagado);
        return new OpenAccount(OPEN_ACCOUNT_ID, OWNER, totalAmount, paidAmount,
                totalAmount.subtract(paidAmount), COMPANY, BRANCH, OpenAccountStatus.OPEN,
                CREADO_POR, CREADA, true, null, null, null, false, null, 1L);
    }

    public static OpenAccount cerrada() {
        return new OpenAccount(OPEN_ACCOUNT_ID, OWNER, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, COMPANY, BRANCH, OpenAccountStatus.CLOSE, CREADO_POR, CREADA, true,
                CERRADO_POR, CERRADA_EL, null, false, null, 1L);
    }

    /** Cuenta cerrada cuyo reverso contable ya se estampo. */
    public static OpenAccount cerradaYaReversada(LocalDateTime reversedAt) {
        return new OpenAccount(OPEN_ACCOUNT_ID, OWNER, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, COMPANY, BRANCH, OpenAccountStatus.CLOSE, CREADO_POR, CREADA, true,
                CERRADO_POR, CERRADA_EL, null, true, reversedAt, 1L);
    }

    public static OpenAccount cancelada(String reason) {
        return new OpenAccount(OPEN_ACCOUNT_ID, OWNER, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, COMPANY, BRANCH, OpenAccountStatus.CANCEL, CREADO_POR, CREADA,
                true, CERRADO_POR, CERRADA_EL, reason, false, null, 1L);
    }

    public static OpenAccount deOtraEmpresa() {
        return new OpenAccount(200L, OTRO_OWNER, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                OTRA_COMPANY, BRANCH, OpenAccountStatus.OPEN, CREADO_POR, CREADA, true, null, null,
                null, false, null, 1L);
    }

    public static CreateOpenAccountCommand comandoCrear() {
        return new CreateOpenAccountCommand(OWNER_ID, BRANCH_ID, COMPANY_ID, EMPLOYEE_ID);
    }

    public static UpdateOpenAccountCommand comandoActualizar() {
        return new UpdateOpenAccountCommand(OPEN_ACCOUNT_ID, OTRO_OWNER.id(), COMPANY_ID, null);
    }

    public static ChangeOpenAccountStatusCommand comandoCerrar() {
        return new ChangeOpenAccountStatusCommand(OPEN_ACCOUNT_ID, "CLOSE", EMPLOYEE_ID, null,
                COMPANY_ID, "DOC_EQUIV_POS", false, null);
    }

    public static ChangeOpenAccountStatusCommand comandoCancelar(String reason) {
        return new ChangeOpenAccountStatusCommand(OPEN_ACCOUNT_ID, "CANCEL", EMPLOYEE_ID, reason,
                COMPANY_ID, null, false, null);
    }
}
