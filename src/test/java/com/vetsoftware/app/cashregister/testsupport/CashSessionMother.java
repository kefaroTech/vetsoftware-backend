package com.vetsoftware.app.cashregister.testsupport;

import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionCount;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fixtures del modulo cashregister.
 *
 * <p>
 * Las sesiones se construyen con el constructor publico y no con
 * {@code CashSession.open(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code openedAt}.
 *
 * <p>
 * La sesion con movimientos deja unos numeros que se comprueban de cabeza: base
 * 100.000 en efectivo, venta de 50.000 en efectivo, venta de 30.000 con tarjeta
 * y retiro de 20.000 en efectivo. Esperado: 130.000 en efectivo y 30.000 en
 * tarjeta.
 */
public final class CashSessionMother {

    public static final Long COMPANY_ID = 1L;
    public static final Long OTRA_COMPANY_ID = 2L;
    public static final Long BRANCH_ID = 10L;
    public static final Long OTRA_BRANCH_ID = 20L;
    public static final Long TERMINAL_ID = 100L;
    public static final String TERMINAL = "principal";
    public static final Long SESSION_ID = 500L;
    public static final Long EMPLEADO_ID = 7L;
    public static final Long OTRO_EMPLEADO_ID = 8L;

    public static final BigDecimal BASE = new BigDecimal("100000");
    public static final BigDecimal VENTA_EFECTIVO = new BigDecimal("50000");
    public static final BigDecimal VENTA_TARJETA = new BigDecimal("30000");
    public static final BigDecimal RETIRO = new BigDecimal("20000");
    public static final BigDecimal ESPERADO_EFECTIVO = new BigDecimal("130000");

    public static final LocalDateTime ABIERTA = LocalDateTime.of(2026, 1, 15, 8, 0);
    public static final LocalDateTime MOVIDA = LocalDateTime.of(2026, 1, 15, 11, 30);
    public static final LocalDateTime CERRADA = LocalDateTime.of(2026, 1, 15, 18, 0);

    private CashSessionMother() {
    }

    /** Sesion OPEN recien abierta: solo la base, sin movimientos. */
    public static CashSession sesionAbierta() {
        return sesion(CashSessionStatus.OPEN, List.of(), List.of());
    }

    /** Sesion OPEN con venta en efectivo, venta con tarjeta y un retiro. */
    public static CashSession sesionConMovimientos() {
        CashSession sesion = sesionAbierta();
        sesion.addMovement(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 1L));
        sesion.addMovement(venta(CashPaymentMethod.CARD, VENTA_TARJETA, 2L));
        sesion.addMovement(movimiento(CashMovementType.WITHDRAWAL, CashPaymentMethod.CASH, RETIRO,
                CashReferenceType.MANUAL, null));
        return sesion;
    }

    /**
     * Sesion CLOSED con el conteo ya materializado: faltan 5.000 en efectivo y la
     * tarjeta cuadra.
     */
    public static CashSession sesionCerrada() {
        return sesion(CashSessionStatus.CLOSED,
                List.of(venta(CashPaymentMethod.CASH, VENTA_EFECTIVO, 1L),
                        venta(CashPaymentMethod.CARD, VENTA_TARJETA, 2L),
                        movimiento(CashMovementType.WITHDRAWAL, CashPaymentMethod.CASH, RETIRO,
                                CashReferenceType.MANUAL, null)),
                List.of(CashSessionCount.create(CashPaymentMethod.CASH, ESPERADO_EFECTIVO,
                        new BigDecimal("125000")),
                        CashSessionCount.create(CashPaymentMethod.CARD, VENTA_TARJETA,
                                VENTA_TARJETA)));
    }

    public static CashSession sesion(CashSessionStatus status, List<CashMovement> movimientos,
            List<CashSessionCount> conteos) {
        return new CashSession(SESSION_ID, COMPANY_ID, BRANCH_ID, TERMINAL_ID, TERMINAL,
                EMPLEADO_ID, ABIERTA, BASE, status,
                status == CashSessionStatus.CLOSED ? OTRO_EMPLEADO_ID : null,
                status == CashSessionStatus.CLOSED ? CERRADA : null, "Turno de la manana", 3L,
                movimientos, conteos);
    }

    /** Venta del POS con su documento de referencia. */
    public static CashMovement venta(CashPaymentMethod method, BigDecimal amount,
            Long documentoId) {
        return movimiento(CashMovementType.SALE_IN, method, amount, CashReferenceType.POS_DOCUMENT,
                documentoId);
    }

    /** Abono a una cuenta abierta con su referencia. */
    public static CashMovement abono(CashPaymentMethod method, BigDecimal amount, Long abonoId) {
        return movimiento(CashMovementType.OPEN_ACCOUNT_IN, method, amount,
                CashReferenceType.OPEN_ACCOUNT_PAYMENT, abonoId);
    }

    /**
     * Movimiento con fecha fija: {@code CashMovement.create} usa {@code now()} y
     * haria no deterministas las aserciones sobre el listado del arqueo.
     */
    public static CashMovement movimiento(CashMovementType type, CashPaymentMethod method,
            BigDecimal amount, CashReferenceType referenceType, Long referenceId) {
        return new CashMovement(null, type, method, amount, referenceType, referenceId, EMPLEADO_ID,
                MOVIDA, null);
    }

    /** Conteo declarado al cerrar: efectivo con faltante, tarjeta sin declarar. */
    public static Map<CashPaymentMethod, BigDecimal> conteoConFaltante() {
        return Map.of(CashPaymentMethod.CASH, new BigDecimal("125000"));
    }
}
