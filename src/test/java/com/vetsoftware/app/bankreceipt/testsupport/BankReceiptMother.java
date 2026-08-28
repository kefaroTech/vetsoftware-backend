package com.vetsoftware.app.bankreceipt.testsupport;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entradas de extracto listas para usar.
 *
 * <p>
 * <b>Los cuatro valores temporales son deliberadamente distintos entre si</b>
 * —una fecha de recepcion, un instante de sellado y un instante de creacion—
 * para que cruzar dos columnas en un mapper o en un command haga caer la
 * asercion. Con la misma fecha en los tres, no caeria.
 *
 * <p>
 * <b>El importe lleva centavos que no son cero</b> por lo mismo: un truncado a
 * la unidad se ve.
 */
public final class BankReceiptMother {

    public static final String CUENTA = "BANCOLOMBIA-AHORROS-00912";
    public static final String REFERENCIA = "TRX-2026-03-0099A";
    public static final LocalDate RECIBIDA_EL = LocalDate.of(2026, 3, 5);
    public static final BigDecimal IMPORTE = new BigDecimal("217345.61");
    public static final String DESCRIPCION = "Consignacion Clinica San Roque";
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);
    public static final LocalDateTime SELLADA_EL = LocalDateTime.of(2026, 3, 9, 16, 20, 30);

    private BankReceiptMother() {
    }

    /** Recien cargada del extracto: en la bandeja y sin sellar. */
    public static BankReceipt enLaBandeja() {
        return BankReceipt.register(CUENTA, REFERENCIA, RECIBIDA_EL, IMPORTE, DESCRIPCION,
                CREADA_EL);
    }

    /**
     * Ya persistida: con id y con version, que es lo que ve un {@code findById}.
     */
    public static BankReceipt persistida(Long id) {
        return new BankReceipt(id, CUENTA, REFERENCIA, RECIBIDA_EL, IMPORTE, DESCRIPCION,
                BankReceiptStatus.UNIDENTIFIED, null, CREADA_EL, 0L);
    }

    public static BankReceipt conReferencia(String referencia) {
        return BankReceipt.register(CUENTA, referencia, RECIBIDA_EL, IMPORTE, DESCRIPCION,
                CREADA_EL);
    }

    public static BankReceipt conImporte(BigDecimal importe) {
        return BankReceipt.register(CUENTA, REFERENCIA, RECIBIDA_EL, importe, DESCRIPCION,
                CREADA_EL);
    }

    public static BankReceipt recibidaEl(LocalDate fecha, String referencia) {
        return BankReceipt.register(CUENTA, referencia, fecha, IMPORTE, DESCRIPCION, CREADA_EL);
    }

    /**
     * Entrada en el estado pedido, con la combinacion de {@code identifiedAt} que
     * el {@code CHECK} admite para ese estado.
     *
     * <p>
     * El ternario vive aqui, en el andamio, y no en el cuerpo de ningun caso: la
     * convencion prohibe la logica dentro de un test, no dentro del fixture que lo
     * alimenta.
     */
    public static BankReceipt enEstado(BankReceiptStatus estado) {
        return enEstadoConImporte(estado, IMPORTE);
    }

    public static BankReceipt enEstadoConImporte(BankReceiptStatus estado, BigDecimal importe) {
        return new BankReceipt(70L, CUENTA, REFERENCIA, RECIBIDA_EL, importe, DESCRIPCION, estado,
                estado == BankReceiptStatus.UNIDENTIFIED ? null : SELLADA_EL, CREADA_EL, 0L);
    }
}
