package com.vetsoftware.app.pricelist.application.dto;

/**
 * Un arco {@code REQUIRES} del catalogo, por rotulos: el articulo y lo que
 * necesita para funcionar.
 *
 * <p>
 * <strong>Es un arco DIRECTO, no el cierre transitivo, y esa es la decision de
 * diseno de esta pieza.</strong> La semilla 309 encadena
 * {@code EXTRA_STORAGE → LAB_IMAGING → CLINICAL_HISTORY}. Publicando el cierre,
 * {@code EXTRA_STORAGE} saldria exigiendo {@code CLINICAL_HISTORY} <em>como si
 * fuera un requisito suyo</em>, y no lo es: lo es de {@code LAB_IMAGING}. El
 * front que tiene que explicarle al cliente por que se le anadio algo que no
 * pidio necesita justamente ese eslabon —«has pedido Almacenamiento extra, que
 * necesita Laboratorio e imagen, que a su vez necesita Historia clinica»—, y un
 * cierre aplanado ya no lo tiene. La cadena se puede reconstruir desde los
 * arcos; los arcos <strong>no</strong> se pueden reconstruir desde la cadena.
 *
 * <p>
 * <strong>Y es el mismo grafo que recorre el servidor.</strong>
 * {@code RequiredItemsClosure.expand} hace un recorrido en anchura sobre estos
 * arcos y {@code SelfServeCartGuard} rechaza contra ellos. Publicar el cierre
 * ya calculado obligaria al back a mantener dos verdades —la que anuncia y la
 * que aplica—; publicar los arcos deja una sola, y el front que quiera el
 * cierre hace el mismo recorrido con los mismos datos y llega al mismo sitio.
 *
 * <p>
 * <strong>Solo {@code REQUIRES}.</strong> Un {@code RECOMMENDS} es una
 * sugerencia comercial —anadirla al carrito seria vender de mas sin que nadie
 * lo pidiera— y un {@code EXCLUDES} no arrastra nada. Es el mismo criterio con
 * el que {@code DependencyGraph} decide que arco recorrer y con el que
 * {@code CatalogItemDependencyQueryPort} sirve al configurador.
 *
 * <p>
 * <strong>Sin la columna {@code note}</strong>, y a proposito. Es un campo
 * libre de un CRUD que solo edita {@code SYSTEM}, sin longitud contratada ni
 * garantia de que su texto sea de cara al cliente; la frase que el front tiene
 * que pintar la compone con los dos nombres, que viajan en la misma respuesta.
 * Publicar una nota interna en la portada comercial es un accidente que solo se
 * descubre cuando ya esta publicada.
 *
 * @param itemCode
 *            el articulo que el cliente elige
 * @param requiredItemCode
 *            el que se le va a anadir. Puede no estar en ninguna de las cuatro
 *            listas de la respuesta si no tiene precio en la tarifa vigente:
 *            ver {@link PublicCatalogDto#requirements()}
 */
public record PublicCatalogRequirementRowDto(String itemCode, String requiredItemCode) {
}
