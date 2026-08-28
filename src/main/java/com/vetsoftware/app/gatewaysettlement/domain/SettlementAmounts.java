package com.vetsoftware.app.gatewaysettlement.domain;

import java.math.BigDecimal;

/**
 * Los <strong>cinco</strong> importes de una liquidacion, y la identidad que
 * los ata.
 *
 * <p>
 * <strong>Cinco y no cuatro, y el que sobra es el que nadie ve.</strong> Lo
 * intuitivo es guardar bruto, comision y neto; esta rodaja guarda ademas
 * {@code feeTax} y {@code gmf} porque cada uno responde una pregunta que sin su
 * columna no se puede contestar:
 *
 * <ul>
 * <li><b>{@code feeTax}</b> — el impuesto de la comision, aparte. Si el
 * servicio de la pasarela resulta excluido, ese impuesto no es descontable y se
 * vuelve mayor valor del gasto. Sumarlo dentro de {@code fee} hace imposible
 * saberlo despues: quedan mezclados dos numeros con destino contable distinto y
 * ya no hay forma de separarlos.</li>
 * <li><b>{@code gmf}</b> — el gravamen a los movimientos financieros de la
 * salida, el cuatro por mil. Son unos 408.000 al ano que hoy no aparecen en
 * ningun informe de margen, y la mitad es deducible si esta certificada.
 * Sepultado dentro de la comision, el margen del negocio se calcula con un
 * gasto que no existe donde se cree que esta.</li>
 * </ul>
 *
 * <p>
 * <strong>La identidad se comprueba aqui porque la base tambien la
 * comprueba.</strong> {@code chk_gateway_settlements_net} exige
 * {@code net = gross - fee - fee_tax - gmf}. Sin esta invariante el desajuste
 * saldria como un {@code Check constraint violated} del driver a mitad de una
 * carga, sin decir que fila ni que importe; con ella sale un mensaje que nombra
 * los dos numeros que no cuadran.
 *
 * <p>
 * <strong>Los signos son los del {@code CHECK}, ni mas estrictos ni mas
 * laxos.</strong> {@code chk_gateway_settlements_amounts} es
 * {@code gross > 0 AND fee >= 0 AND fee_tax >= 0 AND gmf >= 0 AND net > 0}, asi
 * que eso es exactamente lo que se valida:
 *
 * <ul>
 * <li><b>Mas estricto seria inexpresable.</b> Un lote sin comision
 * —renegociacion, promocion del proveedor— tiene {@code fee = 0} y es legitimo;
 * exigir {@code fee > 0} rechazaria una liquidacion real.</li>
 * <li><b>Mas laxo seria un 500.</b> Admitir aqui un negativo —el contracargo
 * que se lleva por delante el lote entero— no lo hace representable: la base lo
 * rechaza igual, y el operario recibe un error del driver en vez de un mensaje
 * que le diga que le pasa a su fichero. <b>Hoy un lote cuyo neto no es positivo
 * NO SE PUEDE GUARDAR</b>, y cerrar ese hueco es un changeset que relaje
 * {@code chk_gateway_settlements_amounts}, no una validacion mas permisiva de
 * este lado.</li>
 * </ul>
 *
 * <p>
 * <strong>La escala se comprueba porque MySQL no la comprueba.</strong> Las
 * cinco columnas son {@code DECIMAL(19,2)} y un tercer decimal no es un error
 * para el motor: lo redondea y sigue. Un centavo redondeado en silencio dentro
 * del bruto rompe la identidad del neto en la fila siguiente y el cuadre no
 * cierra sin que nadie sepa por que.
 */
public record SettlementAmounts(BigDecimal gross, BigDecimal fee, BigDecimal feeTax, BigDecimal gmf,
        BigDecimal net) {

    /**
     * {@code DECIMAL(19,2)}: un tercer decimal lo redondearia la base en silencio.
     */
    private static final int MAX_SCALE = 2;

    public SettlementAmounts {
        requirePositive("gross", gross);
        requireNotNegative("fee", fee);
        requireNotNegative("feeTax", feeTax);
        requireNotNegative("gmf", gmf);
        requirePositive("net", net);
        requireNetIdentity(gross, fee, feeTax, gmf, net);
    }

    /**
     * Lo que la pasarela se quedo del lote: comision mas su impuesto mas el
     * gravamen de la salida. Es el numero que hay que poder mirar de un vistazo
     * cuando alguien pregunta cuanto cuesta cobrar, y se deriva en vez de
     * guardarse: una sexta columna se desincronizaria de las cinco a la primera
     * correccion.
     */
    public BigDecimal totalCost() {
        return fee.add(feeTax).add(gmf);
    }

    private static void requirePositive(String field, BigDecimal value) {
        requireAmount(field, value);
        if (value.signum() <= 0)
            throw new IllegalArgumentException(field + " amount must be greater than zero");
    }

    private static void requireNotNegative(String field, BigDecimal value) {
        requireAmount(field, value);
        if (value.signum() < 0)
            throw new IllegalArgumentException(field + " amount cannot be negative");
    }

    private static void requireAmount(String field, BigDecimal value) {
        if (value == null)
            throw new IllegalArgumentException(field + " amount is required");
        if (value.stripTrailingZeros().scale() > MAX_SCALE)
            throw new IllegalArgumentException(field + " amount must have 2 decimals or less");
    }

    /**
     * Espejo de {@code chk_gateway_settlements_net}. La comparacion es
     * {@code compareTo} y no {@code equals}: {@code 0.00} y {@code 0} son el mismo
     * importe con distinta escala, y {@code equals} los daria por distintos.
     */
    private static void requireNetIdentity(BigDecimal gross, BigDecimal fee, BigDecimal feeTax,
            BigDecimal gmf, BigDecimal net) {
        BigDecimal expected = gross.subtract(fee).subtract(feeTax).subtract(gmf);
        if (net.compareTo(expected) != 0)
            throw new IllegalArgumentException(
                    "net amount must be gross minus fee, fee tax and" + " gmf: expected "
                            + expected.toPlainString() + " but was " + net.toPlainString());
    }
}
