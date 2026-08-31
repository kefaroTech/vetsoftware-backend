package com.vetsoftware.app.catalogitemaihint.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogItemAiHint — la pista se publica y se sucede, nunca se edita")
class CatalogItemAiHintTest {

    private static final Long ARTICULO = 4400L;
    private static final Long FIRMANTE = 990L;

    /**
     * Quien cierra la vigencia. Distinto de {@link #FIRMANTE} a proposito: son dos
     * columnas distintas desde el changeset 393, y con un unico id un
     * {@code supersede} que escribiera el firmante equivocado pasaria en verde.
     */
    private static final Long RETIRADOR = 991L;
    private static final LocalDateTime PUBLICADO_EL = LocalDateTime.of(2026, 3, 1, 12, 0);
    private static final LocalDateTime SUCEDIDO_EL = LocalDateTime.of(2026, 9, 1, 12, 0);

    private static final String COMPLETA = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano", "estetica".

            NO se necesita si el negocio es solo clinico.""";

    @Nested
    @DisplayName("Las tres partes de la convencion")
    class TresPartes {

        @Test
        @DisplayName("una pista con las tres partes se publica")
        void una_pista_completa_se_publica() {
            CatalogItemAiHint pista = CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL);

            assertThat(pista.getHintText()).isEqualTo(COMPLETA);
            assertThat(pista.isCurrent()).isTrue();
            assertThat(pista.getSupersededAt()).isNull();
        }

        /**
         * &#9940; <b>El contraejemplo es la parte que mas trabaja.</b> Sin el bloque de
         * «cuando NO aplica», el modelo mete de todo: propone hospitalizacion a una
         * peluqueria porque el texto del prospecto menciona que los animales se quedan.
         * Que falte no rompe nada visible —la pista se guarda, el prompt se arma, la
         * propuesta sale—, asi que la unica forma de que no se olvide es que el alta lo
         * rechace.
         */
        @Test
        @DisplayName("sin el bloque de cuando NO aplica, no se publica")
        void sin_contraejemplo_no_se_publica() {
            String sinContraejemplo = """
                    Se necesita cuando el negocio ofrece bano y peluqueria.

                    Senales en el texto: "peluqueria", "bano", "estetica".""";

            assertThatThrownBy(() -> CatalogItemAiHint.publish(ARTICULO, 1, sinContraejemplo,
                    FIRMANTE, PUBLICADO_EL, PUBLICADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does NOT apply");
        }

        @Test
        @DisplayName("un parrafo suelto tampoco: no es una pista, es una frase")
        void un_solo_bloque_no_se_publica() {
            assertThatThrownBy(() -> CatalogItemAiHint.publish(ARTICULO, 1,
                    "Se necesita cuando el negocio ofrece peluqueria.", FIRMANTE, PUBLICADO_EL,
                    PUBLICADO_EL)).isInstanceOf(IllegalArgumentException.class);
        }

        /**
         * <b>La regla exige estructura, no vocabulario</b>, y este caso es la razon. La
         * pista de {@code CORE} que siembra el changeset 382 no dice «NO se necesita»
         * —dice «NUNCA lo devuelvas como una linea con motivo»—, y las otras trece usan
         * la otra formula. Un predicado por literal habria rechazado a una de las
         * catorce.
         */
        @Test
        @DisplayName("el tercer bloque vale escrito como sea: NUNCA cuenta igual que NO")
        void el_contraejemplo_no_depende_del_literal() {
            String comoCore = """
                    Va siempre, en todo negocio: es el registro de duenos y mascotas.

                    Senales en el texto: no hay que buscarlas.

                    NUNCA lo devuelvas como una linea con motivo.""";

            assertThatCode(() -> CatalogItemAiHint.publish(ARTICULO, 1, comoCore, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL)).doesNotThrowAnyException();
        }

        /**
         * &#9940; <b>La asimetria deliberada entre {@code publish} y el
         * constructor.</b> El constructor lo ejecuta tambien el mapeador al leer una
         * fila: si exigiera las tres partes, una pista historica que no las cumple
         * reventaria al abrir la pantalla que existe para corregirla. Leer nunca valida
         * contenido editorial.
         */
        @Test
        @DisplayName("leer una pista historica de dos bloques NO revienta: el constructor no juzga")
        void leer_una_pista_incompleta_no_revienta() {
            assertThatCode(() -> new CatalogItemAiHint(1L, ARTICULO, 1,
                    "Un texto viejo de una sola parte.", PUBLICADO_EL, FIRMANTE, null, null,
                    PUBLICADO_EL, 0L)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Suceder")
    class Suceder {

        @Test
        @DisplayName("suceder mueve superseded_at y deja el texto donde estaba")
        void suceder_mueve_solo_la_fecha() {
            CatalogItemAiHint pista = CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL);

            pista.supersede(SUCEDIDO_EL, RETIRADOR);

            assertThat(pista.getSupersededAt()).isEqualTo(SUCEDIDO_EL);
            assertThat(pista.isCurrent()).isFalse();
            assertThat(pista.getHintText()).isEqualTo(COMPLETA);
            assertThat(pista.getHintRevision()).isEqualTo(1);
            assertThat(pista.getPublishedBySystemUserId()).isEqualTo(FIRMANTE);
            assertThat(pista.getSupersededBySystemUserId()).as("y deja la firma de quien cerro")
                    .isEqualTo(RETIRADOR).isNotEqualTo(FIRMANTE);
        }

        /**
         * &#9940; <b>La invariante que el changeset 393 vino a sostener.</b> Cerrar una
         * vigencia sin dejar constancia de quien lo hizo es el defecto exacto que 393
         * corrige: la fila retirada seguia mostrando el firmante de
         * <em>publicacion</em> y quien decidio apagar la pista no constaba en ninguna
         * parte.
         *
         * <p>
         * La columna es nulable en la tabla —por las filas anteriores a 393— pero este
         * camino no la deja vacia: aqui se escribe, no se lee.
         */
        @Test
        @DisplayName("suceder sin decir quien lo hace no se puede: la retirada va firmada")
        void suceder_sin_firmante_no_se_puede() {
            CatalogItemAiHint pista = CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL);

            assertThatThrownBy(() -> pista.supersede(SUCEDIDO_EL, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supersededBySystemUserId is required");
            assertThat(pista.isCurrent()).as("y no cerro nada a medias").isTrue();
            assertThat(pista.getSupersededAt()).isNull();
        }

        /**
         * &#9940; <b>Espejo de {@code chk_catalog_item_ai_hints_superseded_by}.</b> No
         * se puede saber quien retiro algo que sigue vigente. Sin esta comprobacion, el
         * dominio dejaria construir una fila que MySQL rechaza, y el fallo saldria como
         * un 500 de integridad en vez de como un error de programacion en el sitio
         * donde se escribio.
         */
        @Test
        @DisplayName("un firmante de retirada sin fecha de retirada es incoherente")
        void firmante_sin_fecha_es_incoherente() {
            assertThatThrownBy(() -> new CatalogItemAiHint(1L, ARTICULO, 1, COMPLETA, PUBLICADO_EL,
                    FIRMANTE, null, RETIRADOR, PUBLICADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chk_catalog_item_ai_hints_superseded_by");
        }

        /**
         * &#9940; <b>Y el sentido contrario SI se puede leer, que es lo que impide que
         * la correccion rompa el historial.</b> Toda revision sucedida antes del
         * changeset 393 tiene fecha de cierre y no tiene firmante: el actor real nunca
         * se escribio. Si alguien endureciera el constructor a «con fecha, siempre
         * firmante», cada una de esas filas reventaria al mapearse y la pantalla del
         * historial —la que existe justamente para revisarlas— moriria sobre el dato
         * que viene a mostrar. Es la misma asimetria que ya tiene la regla de las tres
         * partes: leer no juzga, escribir si.
         */
        @Test
        @DisplayName("leer una retirada anterior al changeset 393, sin firmante, NO revienta")
        void leer_una_retirada_sin_firmante_no_revienta() {
            assertThatCode(() -> new CatalogItemAiHint(1L, ARTICULO, 1, COMPLETA, PUBLICADO_EL,
                    FIRMANTE, SUCEDIDO_EL, null, PUBLICADO_EL, 0L)).doesNotThrowAnyException();
        }

        /**
         * Cubre el camino que ninguna consulta previa puede ver: dos peticiones que
         * cargan la misma revision vigente. La segunda choca aqui.
         */
        @Test
        @DisplayName("suceder dos veces la misma revision es un conflicto, no un no-op")
        void suceder_dos_veces_es_conflicto() {
            CatalogItemAiHint pista = CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL);
            pista.supersede(SUCEDIDO_EL, RETIRADOR);

            assertThatThrownBy(() -> pista.supersede(SUCEDIDO_EL.plusDays(1), RETIRADOR))
                    .isInstanceOf(CatalogItemAiHintAlreadySupersededException.class);
            assertThat(pista.getSupersededAt()).as("y no movio la fecha original")
                    .isEqualTo(SUCEDIDO_EL);
        }

        /** Espejo de {@code chk_catalog_item_ai_hints_supersede}. */
        @Test
        @DisplayName("no se puede suceder antes de haberse publicado")
        void no_se_sucede_antes_de_publicarse() {
            CatalogItemAiHint pista = CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL);

            assertThatThrownBy(() -> pista.supersede(PUBLICADO_EL.minusDays(1), RETIRADOR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chk_catalog_item_ai_hints_supersede");
        }
    }

    @Nested
    @DisplayName("La huella")
    class Huella {

        @Test
        @DisplayName("es SHA-256 en hexadecimal minuscula y depende del texto exacto")
        void la_huella_depende_del_texto() {
            assertThat(CatalogItemAiHint.hashOf(COMPLETA)).matches("^[0-9a-f]{64}$")
                    .isEqualTo(CatalogItemAiHint.hashOf(COMPLETA))
                    .isNotEqualTo(CatalogItemAiHint.hashOf(COMPLETA + " "));
        }
    }

    @Nested
    @DisplayName("Invariantes estructurales")
    class Invariantes {

        @Test
        @DisplayName("sin firmante no hay fila: published_by_system_user_id es NOT NULL")
        void sin_firmante_no_hay_pista() {
            assertThatThrownBy(() -> CatalogItemAiHint.publish(ARTICULO, 1, COMPLETA, null,
                    PUBLICADO_EL, PUBLICADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("publishedBySystemUserId");
        }

        @Test
        @DisplayName("la revision empieza en 1: chk_catalog_item_ai_hints_revision")
        void la_revision_empieza_en_uno() {
            assertThatThrownBy(() -> CatalogItemAiHint.publish(ARTICULO, 0, COMPLETA, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chk_catalog_item_ai_hints_revision");
        }

        @Test
        @DisplayName("el texto no pasa de 1000 caracteres, que es el ancho de la columna")
        void el_texto_no_pasa_del_ancho_de_la_columna() {
            String largo = "a".repeat(996) + "\n\nb\n\nc";

            assertThatThrownBy(() -> CatalogItemAiHint.publish(ARTICULO, 1, largo, FIRMANTE,
                    PUBLICADO_EL, PUBLICADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1000");
        }
    }
}
