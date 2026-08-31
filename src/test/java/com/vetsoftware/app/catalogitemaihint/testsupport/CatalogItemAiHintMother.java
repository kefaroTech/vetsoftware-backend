package com.vetsoftware.app.catalogitemaihint.testsupport;

import com.vetsoftware.app.catalogitemaihint.application.command.PublishCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.command.RetireCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Object mother de la feature {@code catalogitemaihint}.
 *
 * <p>
 * <strong>Ningun test construye un {@link CatalogItemAiHint} por su
 * cuenta.</strong> El agregado es append-only y su constructor lo comparten el
 * caso de uso y el mapeador, asi que su firma se mueve cada vez que la tabla
 * gana una columna. Concentrar aqui las cuatro llamadas al constructor —y el
 * command— hace que ese movimiento sea una edicion mecanica en un archivo, y no
 * una reescritura repartida por cinco clases de test.
 *
 * <p>
 * Todos los instantes son constantes: no se llama a {@code now()} en ninguna
 * parte de esta jerarquia, y el reloj de los servicios lo fija
 * {@link #relojFijo()}.
 */
public final class CatalogItemAiHintMother {

    public static final Long ARTICULO_ID = 4400L;
    public static final Long OTRO_ARTICULO_ID = 4401L;
    public static final Long TERCER_ARTICULO_ID = 4402L;
    public static final Long FIRMANTE_ID = 990L;

    /**
     * &#9940; <strong>Distinto de {@link #FIRMANTE_ID} a proposito.</strong> Quien
     * publica un texto y quien decide apagarlo casi nunca son la misma persona, y
     * las dos firmas viven en columnas distintas desde el changeset 393. Con un
     * unico id compartido, un servicio que escribiera el firmante equivocado en
     * {@code superseded_by_system_user_id} pasaria todos los casos en verde.
     */
    public static final Long RETIRADOR_ID = 991L;

    /**
     * Lo que devuelve {@link #relojFijo()}: el instante de la escritura bajo
     * prueba.
     */
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 1, 12, 0);

    /** Cuando se publico lo que ya estaba en la tabla antes del caso de uso. */
    public static final LocalDateTime PUBLICADA_EN = LocalDateTime.of(2026, 3, 1, 12, 0);

    /** Cuando se cerro la vigencia de una revision ya historica. */
    public static final LocalDateTime REEMPLAZADA_EN = LocalDateTime.of(2026, 5, 1, 12, 0);

    /**
     * Tres bloques con el cierre que usan trece de las catorce pistas del changeset
     * 382.
     */
    public static final String TRES_PARTES = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano", "spa".

            NO se necesita si el negocio es solo clinico.""";

    /**
     * Tres bloques con el cierre de la decimocuarta, {@code CORE}. La convencion
     * exige <b>estructura y no vocabulario</b>: un predicado por literal habria
     * rechazado esta.
     */
    public static final String TRES_PARTES_OTRO_CIERRE = """
            El modulo central: agenda, historia clinica y facturacion.

            Senales en el texto: cualquier veterinaria en funcionamiento.

            NUNCA lo devuelvas suelto: ya va incluido en toda propuesta.""";

    /** Le falta el contraejemplo, que es el bloque que mas trabaja. */
    public static final String DOS_PARTES = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano".""";

    private CatalogItemAiHintMother() {
    }

    /**
     * Reloj detenido en {@link #AHORA}: ningun servicio bajo prueba lee la hora
     * real.
     */
    public static Clock relojFijo() {
        return Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    /**
     * Un texto de tres bloques distinto por revision, para que el historial se
     * pueda distinguir revision a revision en las aserciones.
     */
    public static String texto(int revision) {
        return "Se necesita cuando el negocio ofrece el modulo (revision " + revision + ").\n\n"
                + "Senales en el texto: \"peluqueria\", \"bano\".\n\n"
                + "NO se necesita si el negocio es solo clinico.";
    }

    /** La revision que rige hoy: {@code supersededAt} nulo. */
    public static CatalogItemAiHint vigente() {
        return vigente(7001L, ARTICULO_ID, 1, TRES_PARTES);
    }

    /**
     * La revision que rige hoy para ese articulo. Sin fecha de cierre y por tanto
     * <b>sin firmante de retirada</b>: nadie ha retirado lo que sigue vigente, que
     * es lo que impone {@code chk_catalog_item_ai_hints_superseded_by} y repite el
     * constructor del dominio.
     */
    public static CatalogItemAiHint vigente(Long id, Long catalogItemId, int revision,
            String texto) {
        return new CatalogItemAiHint(id, catalogItemId, revision, texto, PUBLICADA_EN, FIRMANTE_ID,
                null, null, PUBLICADA_EN, 0L);
    }

    /**
     * Una revision ya cerrada: sigue en el historial con su texto, el firmante que
     * la publico y —desde el changeset 393— el que la cerro.
     */
    public static CatalogItemAiHint reemplazada(Long id, Long catalogItemId, int revision,
            String texto) {
        return new CatalogItemAiHint(id, catalogItemId, revision, texto, PUBLICADA_EN, FIRMANTE_ID,
                REEMPLAZADA_EN, RETIRADOR_ID, PUBLICADA_EN, 0L);
    }

    /**
     * &#9940; <strong>La fila que el changeset 393 no pudo firmar.</strong> Una
     * revision sucedida <em>antes</em> de que existiera
     * {@code superseded_by_system_user_id}: tiene fecha de cierre y no tiene
     * firmante, porque el actor real nunca se escribio y no hay forma de
     * reconstruirlo.
     *
     * <p>
     * Existe para fijar que esa combinacion <b>se puede construir</b>. Si alguien
     * endureciera el constructor a «con fecha, siempre firmante», cada una de estas
     * filas historicas reventaria al leerse y la pantalla del historial —la que
     * existe justamente para revisarlas— moriria sobre el dato que viene a mostrar.
     */
    public static CatalogItemAiHint reemplazadaSinFirma(Long id, Long catalogItemId, int revision,
            String texto) {
        return new CatalogItemAiHint(id, catalogItemId, revision, texto, PUBLICADA_EN, FIRMANTE_ID,
                REEMPLAZADA_EN, null, PUBLICADA_EN, 0L);
    }

    public static PublishCatalogItemAiHintCommand comandoDePublicacion(String texto) {
        return new PublishCatalogItemAiHintCommand(ARTICULO_ID, texto, FIRMANTE_ID);
    }

    /**
     * El command de retirada. El firmante es {@link #RETIRADOR_ID} y no
     * {@link #FIRMANTE_ID} para que un servicio que escribiera el actor equivocado
     * se ponga rojo.
     */
    public static RetireCatalogItemAiHintCommand comandoDeRetirada() {
        return new RetireCatalogItemAiHintCommand(ARTICULO_ID, RETIRADOR_ID);
    }

    public static CatalogItemRef ref() {
        return ref(ARTICULO_ID, "GROOMING", "Estetica");
    }

    public static CatalogItemRef ref(Long catalogItemId, String code, String name) {
        return new CatalogItemRef(catalogItemId, code, name);
    }
}
