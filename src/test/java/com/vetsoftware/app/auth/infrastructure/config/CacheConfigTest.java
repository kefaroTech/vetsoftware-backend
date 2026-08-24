package com.vetsoftware.app.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Censo de caches, levantado del codigo y no de una lista escrita a mano.
 *
 * <p>
 * <b>Que invariante sujeta.</b> Un {@code @Cacheable} cuyo nombre no aparezca
 * en {@link CacheConfig#TIPOS_POR_CACHE} cae en la configuracion por defecto,
 * que serializa sin informacion de tipo. Para cualquier valor que no sea un
 * escalar -una coleccion, un {@code record}, un mapa- eso significa que lo que
 * vuelve de Redis NO es del tipo que el metodo declara: una coleccion vuelve
 * como {@code ArrayList} y un objeto como {@code LinkedHashMap}, y el
 * {@code checkcast} del proxy lanza {@code ClassCastException}. Es la
 * incidencia #464, y su rasgo peligroso es que solo se manifiesta en la SEGUNDA
 * llamada, asi que ni el arranque ni una prueba de una sola invocacion la ven.
 *
 * <p>
 * Por eso el censo se lee del arbol de fuentes en vez de compararse contra una
 * constante: una lista que hay que acordarse de actualizar es exactamente lo
 * que fallo aqui.
 *
 * <p>
 * <b>Limitacion conocida</b>: reconoce el nombre del cache solo cuando es un
 * literal escrito dentro de la anotacion. Un {@code @Cacheable(value =
 * CONSTANTE)} o un {@code value = {"a", "b"}} se le escapan.
 */
@DisplayName("Censo de caches declarados en src/main")
class CacheConfigTest {

    private static final Path FUENTES = Path.of("src", "main", "java");

    private static final List<String> ANOTACIONES = List.of("@Cacheable", "@CacheEvict",
            "@CachePut");

    private static final List<String> ATRIBUTOS_DE_NOMBRE = List.of("value", "cacheNames");

    private static final char COMILLA = '"';

    private static Set<String> nombresDeCacheEnElCodigo() {
        assertThat(FUENTES).as("el test se ejecuta desde la raiz del modulo").isDirectory();
        Set<String> nombres = new TreeSet<>();
        try (Stream<Path> ficheros = Files.walk(FUENTES)) {
            ficheros.filter(fichero -> fichero.toString().endsWith(".java"))
                    .forEach(fichero -> acumularFichero(leer(fichero), nombres));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return nombres;
    }

    private static void acumularFichero(String fuente, Set<String> nombres) {
        for (String anotacion : ANOTACIONES) {
            int desde = 0;
            int encontrada = fuente.indexOf(anotacion, desde);
            while (encontrada >= 0) {
                int finNombre = encontrada + anotacion.length();
                desde = finNombre;
                int abre = fuente.indexOf('(', finNombre);
                int cierra = fuente.indexOf(')', finNombre);
                // Solo cuenta como anotacion si abre parentesis acto seguido: asi
                // se descartan las menciones en javadoc.
                if (abre >= 0 && cierra > abre && fuente.substring(finNombre, abre).isBlank()) {
                    acumularCuerpo(fuente.substring(abre + 1, cierra), nombres);
                }
                encontrada = fuente.indexOf(anotacion, desde);
            }
        }
    }

    private static void acumularCuerpo(String cuerpo, Set<String> nombres) {
        for (String fragmento : cuerpo.split(",")) {
            String limpio = fragmento.trim();
            int igual = limpio.indexOf('=');
            String valor = igual < 0 ? limpio : limpio.substring(igual + 1).trim();
            if (igual >= 0 && !ATRIBUTOS_DE_NOMBRE.contains(limpio.substring(0, igual).trim())) {
                continue;
            }
            if (valor.length() > 1 && valor.charAt(0) == COMILLA
                    && valor.charAt(valor.length() - 1) == COMILLA) {
                nombres.add(valor.substring(1, valor.length() - 1));
            }
        }
    }

    private static String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nested
    @DisplayName("Todo cache tiene su tipo declarado")
    class TodoCacheTieneTipo {

        @Test
        @DisplayName("el censo del codigo no esta vacio")
        void el_censo_no_esta_vacio() {
            assertThat(nombresDeCacheEnElCodigo())
                    .as("si esto sale vacio es que el barrido dejo de casar, no que no haya "
                            + "caches: el test estaria en verde sin comprobar nada")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("cada cache de src/main figura en CacheConfig.TIPOS_POR_CACHE")
        void cada_cache_del_codigo_tiene_serializador_tipado() {
            assertThat(nombresDeCacheEnElCodigo())
                    .as("un cache sin tipo declarado serializa sin informacion de tipo y "
                            + "vuelve de Redis con otro tipo: ver la incidencia #464")
                    .isSubsetOf(CacheConfig.TIPOS_POR_CACHE.keySet());
        }

        @Test
        @DisplayName("no sobran entradas en TIPOS_POR_CACHE")
        void no_hay_entradas_podridas() {
            assertThat(CacheConfig.TIPOS_POR_CACHE.keySet())
                    .as("una entrada que ya no corresponde a ninguna anotacion de cache "
                            + "ensena a no leer la lista")
                    .isSubsetOf(nombresDeCacheEnElCodigo());
        }
    }
}
