package com.vetsoftware.app.pricelist.application.dto;

/**
 * Que articulo trae dentro que paquete, por {@code code} y solo por
 * {@code code}.
 *
 * <p>
 * <strong>Solo los rotulos, y es deliberado.</strong> El detalle del componente
 * —nombre, precio, dias de prueba— ya viaja una vez en
 * {@link PublicCatalogItemRowDto}, y repetirlo aqui obligaria a mantener dos
 * copias del mismo dato en la misma respuesta. Con el rotulo basta para lo
 * unico que esta lista tiene que permitir: que el front sume lo que el cliente
 * eligio y lo compare con el precio del paquete, y que sepa que un paquete y
 * una de sus piezas <strong>no</strong> se compran juntos.
 *
 * <p>
 * Esa segunda lectura es la que importa, porque el servidor la comprueba de
 * verdad: {@code SelfServeQuoteService} rechaza una peticion que mezcle un
 * paquete con un componente suyo. Lo que se publica aqui es el mismo grafo
 * contra el que se rechaza, asi que el front puede evitar el error en vez de
 * provocarlo.
 */
public record PublicCatalogPackComponentRowDto(String packCode, String componentCode) {
}
