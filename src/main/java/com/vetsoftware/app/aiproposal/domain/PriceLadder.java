package com.vetsoftware.app.aiproposal.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La escalera de precios de <em>un</em> articulo en <em>un</em> ciclo, y la
 * unica aritmetica autorizada para convertir una cantidad en dinero dentro de
 * esta rodaja.
 *
 * <p>
 * ⛔ <strong>LOS TRAMOS SON ACUMULATIVOS, no de volumen.</strong> Es la decision
 * D-66, escrita en la propia semilla ({@code 310_seed_price_list_2026.xml:57})
 * y con su comprobacion al lado: {@code EXTRA_USER} cobra 12.000 hasta la
 * unidad 8 y 9.000 de la 9 en adelante, asi que <strong>trece unidades son 8 x
 * 12.000 + 5 x 9.000 = 141.000</strong>. Multiplicar la cantidad por el precio
 * del tramo de entrada da <strong>156.000</strong>, y esos 15.000 de mas por
 * cliente y mes son exactamente el defecto que D-66 cerro. La escalera de
 * volumen -cobrar las trece al precio del tramo que las contiene, 13 x 9.000 =
 * 117.000- es igual de falsa por el otro lado.
 *
 * <p>
 * <strong>Por que vive en el dominio y no en el adaptador.</strong> El
 * adaptador es lo que ninguna prueba unitaria puede ejercitar sin una base de
 * datos; la aritmetica del dinero es justo lo contrario. Separarlas deja la
 * regla cara -la que costo 24.000 COP por cliente y mes- comprobable en
 * milisegundos y sin Docker, y deja al adaptador con una sola responsabilidad:
 * leer <em>todas</em> las filas de la escalera en vez de filtrar por
 * {@code tier_min = 1}.
 *
 * <p>
 * <strong>El contrato de {@code tier_min = 1} que publica {@code GET /catalog}
 * no es este.</strong> {@code JpaPublicCatalogQueryPort.SQL_ITEMS} acota al
 * tramo de entrada a proposito -es un precio "desde" de portada y la escalera
 * completa es politica comercial que no se publica-. Aqui se cotiza, y una
 * cotizacion con el precio "desde" es una cotizacion mal.
 *
 * <p>
 * <strong>Invariantes que se comprueban, no se suponen</strong>: la escalera
 * empieza en la unidad 1, no tiene huecos ni solapes, y termina en un tramo
 * abierto. Sin la ultima, {@link #amountFor(int)} devolveria un total
 * silenciosamente incompleto para las cantidades por encima del ultimo techo,
 * que es la peor forma de equivocarse con dinero.
 */
public record PriceLadder(String itemCode, List<PriceTier> tiers, String currency) {

    public PriceLadder {
        if (itemCode == null || itemCode.isBlank())
            throw new IllegalArgumentException("ladder itemCode is required");
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException(
                    "ladder currency must be a 3-letter code: " + itemCode);
        if (tiers == null || tiers.isEmpty())
            throw new IllegalArgumentException(
                    "a price ladder needs at least one tier: " + itemCode);
        List<PriceTier> ordenados = new ArrayList<>(tiers);
        ordenados.sort(Comparator.comparingInt(PriceTier::tierMin));
        validarCadena(itemCode, ordenados);
        tiers = List.copyOf(ordenados);
    }

    /**
     * La cadena tiene que cubrir {@code 1..infinito} sin hueco ni solape. Un hueco
     * deja unidades sin precio y un solape deja dos precios para la misma unidad;
     * las dos cosas se resuelven en silencio si nadie mira, y las dos cobran de
     * menos o de mas.
     */
    private static void validarCadena(String itemCode, List<PriceTier> ordenados) {
        if (ordenados.get(0).tierMin() != 1)
            throw new IllegalArgumentException("a price ladder must start at unit 1: " + itemCode);
        for (int i = 0; i < ordenados.size() - 1; i++) {
            PriceTier actual = ordenados.get(i);
            if (actual.esAbierto())
                throw new IllegalArgumentException("only the last tier can be open: " + itemCode);
            if (ordenados.get(i + 1).tierMin() != actual.tierMax() + 1)
                throw new IllegalArgumentException("price ladder has a gap or an overlap at unit "
                        + actual.tierMax() + ": " + itemCode);
        }
        if (!ordenados.get(ordenados.size() - 1).esAbierto())
            throw new IllegalArgumentException(
                    "the last tier of a price ladder must be open (tier_max NULL): " + itemCode);
    }

    /**
     * Las unidades que el precio regala. Sale del primer tramo porque
     * {@code included_quantity} describe al articulo, no al escalon: en la tarifa
     * real solo {@code CAPACITY_USER} la lleva distinta de cero, y es lo que hace
     * que el nucleo traiga dos usuarios y el tercero sea el primero que se factura.
     */
    public int includedQuantity() {
        return tiers.get(0).includedQuantity();
    }

    /** El impuesto del articulo: mismo en todos sus tramos en una tarifa sana. */
    public BigDecimal taxRate() {
        return tiers.get(0).taxRate();
    }

    /**
     * El total <strong>acumulativo</strong> de {@code quantity} unidades,
     * redondeado a centavos una sola vez al final.
     *
     * <p>
     * Las {@link #includedQuantity()} primeras no se cobran; las siguientes se
     * numeran desde 1 y cada una paga el precio del tramo que la contiene. Esa
     * renumeracion es la traduccion que la semilla deja escrita: "usuarios 3 a 10"
     * son las unidades extra 1-8.
     */
    public BigDecimal amountFor(int quantity) {
        int facturables = quantity - includedQuantity();
        if (facturables <= 0)
            return Money.zero();
        BigDecimal total = Money.zero();
        for (PriceTier tramo : tiers) {
            int desde = tramo.tierMin();
            if (desde > facturables)
                break;
            int hasta = tramo.esAbierto() ? facturables : Math.min(tramo.tierMax(), facturables);
            total = total.add(
                    Money.multiply(tramo.unitAmount(), BigDecimal.valueOf(hasta - desde + 1L)));
        }
        return Money.scaled(total);
    }

    /**
     * Lo que cuesta <strong>una</strong> unidad, que es la unica cantidad que el
     * motor determinista cotiza hoy: {@code ProposalCart} escribe todas sus lineas
     * con {@code quantity = 1} porque un modulo se tiene o no se tiene.
     *
     * <p>
     * ⚠️ <strong>Es un caso particular de {@link #amountFor(int)}, no un
     * atajo.</strong> {@code CartLine.base()} multiplica este importe por la
     * cantidad, asi que seria correcto solo mientras la cantidad valga 1. El dia
     * que se coticen capacidades -hoy no se puede: ningun {@code EXTRA_*} cuelga de
     * un paquete publicado y por tanto ninguno es {@code selfServiceEligible} (plan
     * S2.3)- el importe tiene que salir de {@link #amountFor(int)} y no de
     * multiplicar este.
     */
    public BigDecimal unitAmountForOne() {
        return amountFor(1);
    }
}
