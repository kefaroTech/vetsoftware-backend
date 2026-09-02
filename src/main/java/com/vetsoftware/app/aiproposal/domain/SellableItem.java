package com.vetsoftware.app.aiproposal.domain;

import java.math.BigDecimal;

/**
 * Un articulo del catalogo comercial, con lo que esta rodaja necesita saber de
 * el y nada mas.
 *
 * <p>
 * <strong>Lleva los articulos NO vendibles a proposito.</strong> Sin ellos el
 * motor no podria distinguir "ese codigo no existe" de "existe pero no esta
 * publicado" ni de "existe y no se vende por autoservicio", que son tres
 * veredictos distintos y la senal con la que se mide si el modelo sirve. Esa
 * distincion es interna: hacia fuera los tres son indistinguibles
 * ({@link LineVerdict}).
 *
 * <p>
 * <strong>{@code unitAmount} llega ya resuelto</strong>: {@code catalog_prices}
 * tiene escalones ({@code tier_min}/{@code tier_max}) e
 * {@code included_quantity}, y elegir el tramo que corresponde a la cantidad es
 * trabajo del adaptador que consulta el catalogo, no del motor. Asumir
 * {@code tier_min = 1} reintrodujo un error medido de 24.000 COP por cliente y
 * mes (D-66), por eso el importe entra resuelto y el dominio no lo recalcula.
 *
 * <p>
 * <strong>{@code currency} es obligatoria</strong> y viaja con cada importe: un
 * DTO de dinero sin divisa obliga al front a cablear "COP", y arreglarlo
 * despues parece aditivo y rompe los bindings de los dos fronts.
 *
 * <p>
 * &#9940; <strong>NO lleva {@code is_core}, y esa ausencia es
 * deliberada.</strong> Esa columna es un bit compartido por dos contextos que
 * la leen distinto: el alta de plataforma la usa como <em>predicado de
 * conjunto</em> —«forma parte del minimo estructural», y en el catalogo real
 * son TRES articulos: el modulo {@code CORE} y las capacidades
 * {@code CAPACITY_USER} y {@code CAPACITY_BRANCH}, semilla 308:41-49— mientras
 * que esta rodaja necesita <em>el</em> articulo que todo carrito arrastra.
 * Traerla en crudo hasta aqui fue el defecto: {@code SellableCatalog} resolvia
 * el nucleo con un {@code findFirst()} sobre los que tenian el bit, las dos
 * capacidades no son cotizables, y cuando el sorteo caia en una de ellas el
 * prospecto recibia un 200 con el carrito vacio. La traduccion vive ahora en
 * {@code JpaSellableCatalogQueryPort}, que es la frontera, y el dominio recibe
 * el concepto ya resuelto en {@link SellableCatalog#nucleo()}: aqui no queda
 * nada que se pueda volver a leer mal.
 *
 * @param trialDays
 *            dias de prueba que concede el articulo; {@code 0} es "sin prueba"
 *            ({@code NEVER_FREE}), nunca negativo: el lado seguro es no regalar
 */
public record SellableItem(String code, String name, String shortDescription, SellableItemKind kind,
        boolean active, boolean selfServiceEligible, int trialDays, BigDecimal unitAmount,
        BigDecimal taxRate, String currency) {

    public SellableItem {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("item code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("item code must be 50 chars or less: " + code);
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("item name is required: " + code);
        if (kind == null)
            throw new IllegalArgumentException("item kind is required: " + code);
        if (trialDays < 0)
            throw new IllegalArgumentException("trialDays cannot be negative: " + code);
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount must be zero or positive: " + code);
        if (taxRate == null || taxRate.signum() < 0)
            throw new IllegalArgumentException("taxRate must be zero or positive: " + code);
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("currency must be a 3-letter code: " + code);
    }

    /**
     * El paso duro de la validacion (plan S2.3, regla 1). Un articulo retirado o no
     * contratable por autoservicio produce una linea rechazada, nunca una linea
     * cotizada: cotizarlo hace que el fallo aparezca en el paso 6, despues de que
     * el prospecto se registro y verifico el correo.
     */
    public boolean esCotizable() {
        return active && selfServiceEligible;
    }

    public boolean concedePrueba() {
        return trialDays > 0;
    }
}
