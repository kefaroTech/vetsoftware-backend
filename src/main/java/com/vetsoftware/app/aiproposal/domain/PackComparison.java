package com.vetsoftware.app.aiproposal.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compara el carrito contra los paquetes. <strong>Pura</strong>: sin Spring,
 * sin repositorios y sin reloj.
 *
 * <p>
 * ⛔ <strong>Compara, no sustituye</strong> (plan S1.5, D-2). Esta clase no
 * devuelve un carrito nuevo ni toca el que recibe: devuelve, como mucho, una
 * oferta que el cliente decide. La v1 sustituia en silencio y eso era un patron
 * oscuro con numeros detras.
 *
 * <p>
 * <strong>Tres reglas, y las tres se rompieron alguna vez:</strong>
 *
 * <ol>
 * <li><strong>La contencion se evalua solo sobre componentes
 * {@code MODULE}.</strong> {@code CAPACITY_TERMINAL} pertenece a los tres
 * paquetes y no entra al carrito por si solo, asi que "el paquete esta
 * contenido en el carrito" no se cumplia nunca: la funcion estrella de la v1
 * era codigo muerto.</li>
 * <li><strong>La comparacion es estricta</strong> ({@code precio < suma}). En
 * el ejemplo de la v1 eran 189.000 = 189.000 y la oferta no debia
 * aparecer.</li>
 * <li><strong>Lleva las dos dimensiones.</strong> Los tres paquetes son
 * {@code NEVER_FREE} mientras 11 de los 13 modulos dan 14 o 30 dias gratis: una
 * oferta que solo diga el precio esconde justo la mitad que le cuesta dinero al
 * cliente.</li>
 * </ol>
 *
 * <p>
 * <strong>Cuando el paquete no sale mas barato -o no esta contenido- devuelve
 * {@link Optional#empty()}</strong>: no se pinta ninguna tarjeta, no se sugiere
 * nada, y el carrito de modulos sueltos queda exactamente como estaba.
 */
public final class PackComparison {

    private PackComparison() {
    }

    /**
     * La mejor oferta entre las que cumplen las tres reglas, o vacio si no hay
     * ninguna. Empata primero por ahorro, despues por menor coste en prueba y al
     * final por codigo, para que el resultado sea reproducible: dos ejecuciones
     * sobre el mismo carrito tienen que ofrecer el mismo paquete.
     */
    public static Optional<PackComparisonResult> mejorOferta(CartResult carrito,
            SellableCatalog catalog) {
        if (carrito == null || catalog == null)
            return Optional.empty();
        Map<String, CartLine> modulos = modulosDelCarrito(carrito);
        if (modulos.isEmpty())
            return Optional.empty();
        return catalog.packs().stream().filter(PackOffer::esComparable)
                .map(pack -> evaluar(pack, modulos, carrito.currency())).flatMap(Optional::stream)
                .max(Comparator.comparing(PackComparisonResult::ahorroMensual)
                        .thenComparing(
                                Comparator.comparingInt(PackComparisonResult::diasDePruebaPerdidos)
                                        .reversed())
                        .thenComparing(
                                Comparator.comparing(PackComparisonResult::packCode).reversed()));
    }

    /**
     * Solo los modulos aceptados. Las capacidades del carrito no participan: el
     * paquete ya las concede, y contarlas en la suma inflaria el ahorro con dinero
     * que el cliente seguiria pagando.
     */
    private static Map<String, CartLine> modulosDelCarrito(CartResult carrito) {
        Map<String, CartLine> modulos = new LinkedHashMap<>();
        for (CartLine linea : carrito.aceptadas()) {
            if (linea.kind() == SellableItemKind.MODULE)
                modulos.put(linea.code(), linea);
        }
        return modulos;
    }

    private static Optional<PackComparisonResult> evaluar(PackOffer pack,
            Map<String, CartLine> modulos, String currency) {
        if (!modulos.keySet().containsAll(pack.moduleComponentCodes()))
            return Optional.empty();

        BigDecimal suma = Money.zero();
        List<String> pierdenPrueba = new ArrayList<>();
        int mayorPerdida = 0;
        for (String code : ordenados(pack, modulos)) {
            CartLine linea = modulos.get(code);
            suma = suma.add(linea.base());
            int perdidos = Math.max(0, linea.trialDays() - pack.trialDays());
            if (perdidos > 0) {
                pierdenPrueba.add(linea.name());
                mayorPerdida = Math.max(mayorPerdida, perdidos);
            }
        }

        BigDecimal precio = Money.scaled(pack.unitAmount());
        if (precio.compareTo(suma) >= 0)
            return Optional.empty();

        return Optional.of(new PackComparisonResult(pack.code(), pack.name(), precio, suma,
                suma.subtract(precio), currency, mayorPerdida, pierdenPrueba));
    }

    /**
     * Recorre los componentes en el orden en que estan en el carrito, no en el del
     * {@code Set} del paquete: la lista de nombres que lee el cliente tiene que
     * salir igual en dos ejecuciones.
     */
    private static List<String> ordenados(PackOffer pack, Map<String, CartLine> modulos) {
        return modulos.keySet().stream().filter(pack.moduleComponentCodes()::contains).toList();
    }
}
