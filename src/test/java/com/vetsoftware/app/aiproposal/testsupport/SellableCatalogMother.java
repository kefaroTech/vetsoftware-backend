package com.vetsoftware.app.aiproposal.testsupport;

import com.vetsoftware.app.aiproposal.domain.PackOffer;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El catalogo con el que se ejercita el motor determinista.
 *
 * <p>
 * <b>Los precios y los dias de prueba son los reales</b> de las semillas 308 y
 * 310 -{@code CORE} 69.000 con 30 dias, {@code CASH_REGISTER} 46.000 con 14, el
 * {@code PACK_CLINIC} a 189.000 y {@code NEVER_FREE}-, y eso no es decoracion:
 * la comparacion de paquete de S1.5 es una afirmacion sobre <em>estos</em>
 * numeros -"cuesta 35.000 menos al mes pero te quita 30 dias de prueba"-, y con
 * cifras inventadas el test pasaria diciendo otra cosa.
 *
 * <p>
 * Trae ademas las tres formas de "no se puede cotizar", que es lo unico que
 * distingue los tres veredictos de rechazo: un articulo que no existe, uno en
 * borrador y uno vivo que no se vende por autoservicio.
 */
public final class SellableCatalogMother {

    public static final String COP = "COP";

    private static final BigDecimal IVA = new BigDecimal("19.00");

    private SellableCatalogMother() {
    }

    /**
     * El catalogo completo. Las dependencias forman la cadena de tres saltos que el
     * plan pone de ejemplo: {@code LAB_IMAGING → CLINICAL_HISTORY → SCHEDULING}.
     */
    public static SellableCatalog completo() {
        Map<String, SellableItem> items = new LinkedHashMap<>();
        anadir(items, nucleo());
        anadir(items, modulo("CLINICAL_HISTORY", "Historia clinica y consultas", 49_000, 30));
        anadir(items, modulo("VACCINATION", "Vacunacion y desparasitacion", 25_000, 30));
        anadir(items, modulo("SCHEDULING", "Agenda de citas", 35_000, 30));
        anadir(items, modulo("CASH_REGISTER", "Caja y punto de venta", 46_000, 14));
        anadir(items, modulo("LAB_IMAGING", "Laboratorio e imagen", 45_000, 30));
        anadir(items, capacidadContratable());
        anadir(items, capacidadNoAutoservicio());
        anadir(items, moduloEnBorrador());

        Map<String, List<String>> requiere = new LinkedHashMap<>();
        requiere.put("LAB_IMAGING", List.of("CLINICAL_HISTORY"));
        requiere.put("CLINICAL_HISTORY", List.of("SCHEDULING"));

        return new SellableCatalog(items, requiere,
                List.of(packClinicaMasBarato(), packAlMismoPrecio(), packSinModulos()), nucleo());
    }

    /** Un catalogo sin ningun paquete, para los casos que no comparan nada. */
    public static SellableCatalog sinPaquetes() {
        SellableCatalog completo = completo();
        return new SellableCatalog(completo.items(), completo.requires(), List.of(),
                completo.nucleo());
    }

    /**
     * El del ejemplo del plan: cinco modulos que sueltos suman 224.000 y en paquete
     * cuestan 189.000. Ahorra 35.000 al mes y cuesta 30 dias de prueba.
     */
    public static PackOffer packClinicaMasBarato() {
        return new PackOffer("PACK_CLINIC", "Pack Clinica", new BigDecimal("189000.00"), IVA, 0,
                Set.of("CORE", "SCHEDULING", "CLINICAL_HISTORY", "VACCINATION", "CASH_REGISTER"));
    }

    /**
     * <b>El caso que la v1 ofrecia y no debia.</b> Sus cuatro modulos suman
     * exactamente 155.000, que es lo que cuesta el paquete: la comparacion es
     * estricta, asi que empatar no es una oferta.
     */
    public static PackOffer packAlMismoPrecio() {
        return new PackOffer("PACK_EMPATE", "Pack al mismo precio", new BigDecimal("155000.00"),
                IVA, 0, Set.of("SCHEDULING", "CLINICAL_HISTORY", "VACCINATION", "CASH_REGISTER"));
    }

    /**
     * <b>El paquete que solo trae capacidades.</b> Su conjunto de modulos es vacio,
     * y la contencion de un conjunto vacio es cierta por vacuidad: sin la guarda de
     * {@code PackOffer.esComparable} se ofreceria siempre, incluso con el carrito
     * vacio.
     */
    public static PackOffer packSinModulos() {
        return new PackOffer("PACK_SOLO_CAPACIDADES", "Pack de capacidades", BigDecimal.ONE, IVA, 0,
                Set.of());
    }

    /**
     * <b>Ya no recibe el {@code is_core}</b>: {@link SellableItem} dejo de llevarlo
     * cuando la traduccion de esa columna bajo al adaptador. Quien sea el nucleo lo
     * decide ahora el cuarto componente de {@link SellableCatalog}.
     */
    public static SellableItem modulo(String code, String nombre, int precio, int diasDePrueba) {
        return new SellableItem(code, nombre, "Descripcion de " + nombre, SellableItemKind.MODULE,
                true, true, diasDePrueba, new BigDecimal(precio + ".00"), IVA, COP);
    }

    /** El nucleo del catalogo de laboratorio, ya resuelto. */
    public static SellableItem nucleo() {
        return modulo("CORE", "Nucleo: clientes y mascotas", 69_000, 30);
    }

    /**
     * {@code CAPACITY_TERMINAL} si cuelga de los tres paquetes, asi que si se
     * vende.
     */
    public static SellableItem capacidadContratable() {
        return new SellableItem("CAPACITY_TERMINAL", "Terminal de caja incluida",
                "Un punto de venta", SellableItemKind.CAPACITY, true, true, 0,
                new BigDecimal("0.00"), IVA, COP);
    }

    /**
     * La DC-1 en forma de dato: los {@code EXTRA_*} no cuelgan de ningun paquete,
     * asi que {@code JpaCatalogQueryPorts} no los da por contratables.
     */
    public static SellableItem capacidadNoAutoservicio() {
        return new SellableItem("EXTRA_USER", "Usuario adicional", "Una persona mas",
                SellableItemKind.CAPACITY, true, false, 0, new BigDecimal("15000.00"), IVA, COP);
    }

    public static SellableItem moduloEnBorrador() {
        return new SellableItem("DRAFT_MODULE", "Modulo sin publicar", "Todavia no se vende",
                SellableItemKind.MODULE, false, true, 0, new BigDecimal("10000.00"), IVA, COP);
    }

    private static void anadir(Map<String, SellableItem> items, SellableItem item) {
        items.put(item.code(), item);
    }
}
