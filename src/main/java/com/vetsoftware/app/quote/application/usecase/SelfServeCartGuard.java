package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <strong>Lo que una cesta de autoservicio tiene que cumplir antes de que se le
 * ponga precio.</strong>
 *
 * <p>
 * Vive aqui y no dentro de un servicio porque hay <em>dos</em> caminos por los
 * que un anonimo llega a una cifra: la vista previa que le dice cuanto costaria
 * y la contratacion que se lo cobra. <strong>Los dos tienen que aceptar
 * exactamente la misma cesta.</strong> Si divergieran, volveria a existir lo
 * que este slice lleva toda la noche cerrando: un numero que la portada promete
 * y el contrato niega. Con una sola implementacion no pueden divergir.
 *
 * <p>
 * Son dos reglas y cierran dos abusos distintos:
 *
 * <ul>
 * <li><b>Nadie paga dos veces.</b> Un paquete y una pieza suya en la misma
 * cesta son dos cobros por la misma funcionalidad, y el motor de precios no lo
 * puede ver: recorre las lineas que recibe sin expandir nada. El duplicado
 * exacto va en la misma comprobacion — la cantidad de un modulo es una casilla
 * encendida, no una unidad que se acumule.</li>
 * <li><b>Nadie compra algo que no va a poder usar.</b> Los nueve arcos
 * {@code REQUIRES} del changeset 309 llevaban sin evaluarse: facturar
 * electronicamente sin Caja se cotizaba tal cual.</li>
 * </ul>
 *
 * <p>
 * <strong>Aqui si se nombra lo que se rechaza</strong>, al contrario que en la
 * traduccion de rotulos: los codigos del mensaje los acaba de mandar quien
 * llama y el catalogo publico los ensena a cualquiera, asi que no hay oraculo
 * que reabrir — y un error mudo obligaria al cliente a adivinar que sobra o que
 * falta.
 */
final class SelfServeCartGuard {

    private SelfServeCartGuard() {
    }

    /** Las dos reglas, en el orden en que conviene descubrirlas. */
    static void assertContractable(List<String> codigos,
            PublishedCatalogItemQueryPort publishedCatalogItemQueryPort) {
        Set<String> vistos = rechazarCobroDoble(codigos, publishedCatalogItemQueryPort);
        rechazarCestaIncoherente(List.copyOf(vistos), publishedCatalogItemQueryPort);
    }

    private static Set<String> rechazarCobroDoble(List<String> codigos,
            PublishedCatalogItemQueryPort publishedCatalogItemQueryPort) {
        Set<String> vistos = new LinkedHashSet<>();
        for (String codigo : codigos) {
            if (!vistos.add(codigo)) {
                throw new IllegalArgumentException("Duplicated catalog item code: " + codigo);
            }
        }
        Set<String> componentes = Set
                .copyOf(publishedCatalogItemQueryPort.findComponentCodesOfBundles(vistos));
        for (String codigo : vistos) {
            if (componentes.contains(codigo)) {
                throw new IllegalArgumentException(
                        "Catalog item " + codigo + " is already included in a bundle in the same"
                                + " request and cannot be charged twice");
            }
        }
        return vistos;
    }

    private static void rechazarCestaIncoherente(List<String> codigos,
            PublishedCatalogItemQueryPort publishedCatalogItemQueryPort) {
        List<String> faltantes = publishedCatalogItemQueryPort.findMissingRequirements(codigos);
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "The request is missing catalog items required by what it asks for: "
                            + String.join(", ", faltantes));
        }
    }
}
