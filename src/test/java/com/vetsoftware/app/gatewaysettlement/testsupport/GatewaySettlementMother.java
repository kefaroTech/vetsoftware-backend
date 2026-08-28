package com.vetsoftware.app.gatewaysettlement.testsupport;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.SettlementAmounts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Liquidaciones de pasarela listas para usar.
 *
 * <p>
 * <b>Los cinco importes son deliberadamente distintos entre si y ninguno es
 * redondo</b>: si un mapper, un command o una Response cruzan dos columnas
 * —comision con su impuesto, neto con bruto— la asercion cae. Con importes
 * iguales o redondos no caeria, y ese cruce es justo el defecto que esta rodaja
 * no puede permitirse: {@code fee_tax_amount} y {@code gmf_amount} existen
 * precisamente para no quedar sumados dentro de otro numero.
 *
 * <p>
 * <b>La fecha de liquidacion y la de creacion son distintas</b> —la pasarela
 * paga con dias de retraso y el lote se carga despues— por la misma razon.
 *
 * <p>
 * <b>Y el lote declara 37 cobros</b>, que es el numero del caso canonico del
 * documento maestro: si dice 37 y hay 36, hay un pago perdido.
 */
public final class GatewaySettlementMother {

    public static final String PASARELA = "WOMPI";
    public static final String REFERENCIA = "LOTE-2026-03-0042";

    public static final BigDecimal BRUTO = new BigDecimal("12450800.00");
    public static final BigDecimal COMISION = new BigDecimal("373524.00");
    public static final BigDecimal IMPUESTO_DE_LA_COMISION = new BigDecimal("70969.56");
    public static final BigDecimal GMF = new BigDecimal("46423.10");
    /** {@code BRUTO - COMISION - IMPUESTO_DE_LA_COMISION - GMF}, al centavo. */
    public static final BigDecimal NETO = new BigDecimal("11959883.34");

    public static final int COBROS_DECLARADOS = 37;
    public static final LocalDate LIQUIDADA_EL = LocalDate.of(2026, 3, 12);
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 15);

    public static final String FACTURA_DEL_PROVEEDOR = "FE-WOMPI-88213";
    public static final String NIT_DEL_PROVEEDOR = "900123456-7";

    private GatewaySettlementMother() {
    }

    public static SettlementAmounts importes() {
        return new SettlementAmounts(BRUTO, COMISION, IMPUESTO_DE_LA_COMISION, GMF, NETO);
    }

    /** Recien cargada: sin factura del proveedor y sin entrada de banco. */
    public static GatewaySettlement reciencargada() {
        return GatewaySettlement.register(PASARELA, REFERENCIA, importes(), COBROS_DECLARADOS,
                LIQUIDADA_EL, CREADA_EL);
    }

    /**
     * Ya persistida: con id y con version, que es lo que ve un {@code findById}.
     */
    public static GatewaySettlement persistida(Long id) {
        return new GatewaySettlement(id, PASARELA, REFERENCIA, null, null, importes(),
                COBROS_DECLARADOS, LIQUIDADA_EL, null, CREADA_EL, 0L);
    }

    /** Ya conciliada del todo: con soporte del gasto y atada al extracto. */
    public static GatewaySettlement conciliada(Long id, Long bankReceiptId) {
        return new GatewaySettlement(id, PASARELA, REFERENCIA, FACTURA_DEL_PROVEEDOR,
                NIT_DEL_PROVEEDOR, importes(), COBROS_DECLARADOS, LIQUIDADA_EL, bankReceiptId,
                CREADA_EL, 0L);
    }

    public static GatewaySettlement conReferencia(String referencia) {
        return GatewaySettlement.register(PASARELA, referencia, importes(), COBROS_DECLARADOS,
                LIQUIDADA_EL, CREADA_EL);
    }

    public static GatewaySettlement deLaPasarela(String pasarela, String referencia) {
        return GatewaySettlement.register(pasarela, referencia, importes(), COBROS_DECLARADOS,
                LIQUIDADA_EL, CREADA_EL);
    }

    public static GatewaySettlement liquidadaEl(LocalDate fecha, String referencia) {
        return GatewaySettlement.register(PASARELA, referencia, importes(), COBROS_DECLARADOS,
                fecha, CREADA_EL);
    }

    public static GatewaySettlement conCobros(int cobros, String referencia) {
        return GatewaySettlement.register(PASARELA, referencia, importes(), cobros, LIQUIDADA_EL,
                CREADA_EL);
    }

    public static GatewaySettlement conImportes(SettlementAmounts importes, String referencia) {
        return GatewaySettlement.register(PASARELA, referencia, importes, COBROS_DECLARADOS,
                LIQUIDADA_EL, CREADA_EL);
    }
}
