package com.vetsoftware.app.aiproposal.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que produce {@link ProposalCart}: <em>todas</em> las lineas -aceptadas y
 * rechazadas- mas los totales que salen de las aceptadas.
 *
 * <p>
 * ⛔ <strong>El contrato de serializacion vive aqui, y es un limite de
 * seguridad</strong> (plan S4.2.3):
 *
 * <ul>
 * <li>Se serializan <strong>solo</strong> las lineas de {@link #aceptadas()},
 * con codigo, nombre, precio, prueba y motivo.</li>
 * <li>{@link #recomendaciones()} va aparte, sin marcar y <strong>sin sumar al
 * total</strong>.</li>
 * <li>De todo lo demas va <strong>como mucho</strong> el entero de
 * {@link #descartadas()}: sin codigos, sin veredictos y sin desglose. Y no
 * distingue causas -un contador que valiera 1 solo para {@code UNKNOWN_CODE}
 * seria el mismo oraculo con menos ruido-.</li>
 * </ul>
 *
 * <p>
 * {@link #lineas()} devuelve el conjunto entero <strong>para
 * persistir</strong>, nunca para responder. Quien escriba la capa web serializa
 * desde {@link #aceptadas()} y {@link #recomendaciones()}, jamas desde aqui.
 */
public record CartResult(List<CartLine> lineas, String currency) {

    public CartResult {
        if (lineas == null)
            throw new IllegalArgumentException("cart lines are required");
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("cart currency must be a 3-letter code");
        lineas = List.copyOf(lineas);
    }

    /** El carrito por defecto: lo aceptado que no es una recomendacion. */
    public List<CartLine> aceptadas() {
        return lineas.stream().filter(l -> l.verdict().esAceptado())
                .filter(l -> !l.source().esRecomendacion()).toList();
    }

    /**
     * "Tambien podria interesarte". Pasa la validacion igual que el resto, pero
     * <strong>no entra en el carrito por defecto y no dispara el cierre de
     * {@code REQUIRES}</strong> hasta que el cliente la acepta (plan S4.4).
     */
    public List<CartLine> recomendaciones() {
        return lineas.stream().filter(l -> l.verdict().esAceptado())
                .filter(l -> l.source().esRecomendacion()).toList();
    }

    /**
     * El unico dato de las lineas rechazadas que puede cruzar la frontera HTTP:
     * cuantas hubo. La pantalla lo necesita para poder decir "no todo lo que
     * propusimos se puede contratar"; nada mas.
     */
    public int descartadas() {
        return (int) lineas.stream().filter(l -> !l.verdict().esAceptado()).count();
    }

    public BigDecimal subtotal() {
        return sumar(aceptadas().stream().map(CartLine::base).toList());
    }

    public BigDecimal impuestos() {
        return sumar(aceptadas().stream().map(CartLine::impuesto).toList());
    }

    public BigDecimal total() {
        return subtotal().add(impuestos());
    }

    /**
     * Lo que se paga el primer periodo: las lineas con dias de prueba no se cobran.
     * Es la dimension que la sustitucion silenciosa de la v1 se llevaba por delante
     * sin decirlo.
     */
    public BigDecimal totalPrimerPeriodo() {
        return sumar(aceptadas().stream().filter(l -> !l.gratisElPrimerPeriodo())
                .map(CartLine::totalConImpuesto).toList());
    }

    private static BigDecimal sumar(List<BigDecimal> importes) {
        return importes.stream().reduce(Money.zero(), BigDecimal::add);
    }
}
