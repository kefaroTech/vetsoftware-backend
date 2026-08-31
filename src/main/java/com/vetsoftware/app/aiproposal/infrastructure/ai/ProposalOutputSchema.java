package com.vetsoftware.app.aiproposal.infrastructure.ai;

import java.util.List;
import java.util.Set;
import software.amazon.awssdk.core.document.Document;

/**
 * El esquema de salida del anexo E §2, en la forma que entiende
 * {@code Converse}: el JSON Schema de la herramienta que el modelo tiene que
 * llamar.
 *
 * <p>
 * ⛔ <strong>Es el MISMO contrato que lee
 * {@code BedrockProposalGenerator.parsear} y el mismo que describe la seccion
 * FORMATO DE SALIDA de {@link ProposalPromptBuilder}, y los tres se escriben
 * por separado.</strong> Esa es la grieta que ya cobro una vez: el parser leia
 * {@code usuarios}, {@code sedes} y {@code cajas} y el prompt no los nombraba
 * en ninguna linea, asi que los tres enteros de capacidad llegaban vacios
 * siempre y la nota de capacidades no se pintaba nunca —sin error, sin log y
 * sin que nada lo delatara—. {@link #CAMPOS} existe para que un test pueda atar
 * los tres lados en vez de confiar en que nadie se despiste.
 *
 * <p>
 * <strong>Los campos son obligatorios TODOS, tambien los tres enteros.</strong>
 * Con {@code strict} el proveedor solo garantiza la forma si el esquema declara
 * {@code required} completo y {@code additionalProperties: false}; y hacerlos
 * obligatorios ademas obliga al modelo a decidir explicitamente, que es mejor
 * que omitirlos. El «no lo se» se expresa con {@code 0}, que es el mismo
 * sentinela que ya usa {@code CapacityHint.desconocido()}.
 *
 * <p>
 * <strong>Los rangos de cordura van en la descripcion, no como
 * {@code minimum}/{@code maximum}.</strong> Los declara {@code CapacityHint}
 * —usuarios 1-500, sedes 1-200, cajas 0-100— y quien los hace cumplir es el, en
 * Java, dentro de nuestro proceso. Meterlos ademas como palabras clave del
 * esquema anadiria una apuesta sobre que subconjunto de JSON Schema acepta el
 * validador estricto de cada proveedor, a cambio de una comprobacion que ya
 * corre de todas formas. En la descripcion el modelo los lee igual y ningun
 * validador puede rechazarlos.
 */
final class ProposalOutputSchema {

    /**
     * El nombre de la herramienta. Viaja dos veces —en la declaracion y en el
     * {@code toolChoice} que la fuerza— y una tercera al leer la respuesta, donde
     * se compara con el nombre del bloque que devuelve el modelo: un bloque de
     * herramienta con otro nombre es un proveedor que no honro la eleccion, y eso
     * tiene su propio desenlace.
     */
    static final String HERRAMIENTA = "proponer_modulos";

    /**
     * Las claves de primer nivel del objeto, exactamente las que lee
     * {@code BedrockProposalGenerator.parsear}. Publica para que el test de
     * contrato pueda compararlas contra el parser y contra el prompt.
     */
    static final Set<String> CAMPOS = Set.of("understood", "out_of_domain", "necesarios",
            "recomendados", "usuarios", "sedes", "cajas");

    /**
     * Las dos claves de cada elemento de {@code necesarios}/{@code recomendados}.
     */
    static final Set<String> CAMPOS_DE_LINEA = Set.of("code", "motivo");

    /**
     * El orden en el que se declaran los obligatorios. Es una lista y no
     * {@link #CAMPOS} porque un {@code Set} no tiene orden estable y el esquema
     * viaja en la peticion: un orden que baila cambia los bytes de la peticion en
     * cada arranque y arruinaria el cacheo de prefijo el dia que se active.
     */
    private static final List<String> CAMPOS_ORDENADOS = List.of("understood", "out_of_domain",
            "necesarios", "recomendados", "usuarios", "sedes", "cajas");

    private ProposalOutputSchema() {
    }

    static String descripcion() {
        return "Entrega la propuesta comercial para este prospecto. Es la UNICA forma de"
                + " responder: no escribas texto fuera de esta llamada.";
    }

    static Document esquema() {
        return Document.mapBuilder().putString("type", "object")
                .putMap("properties", propiedades -> propiedades
                        .putDocument("understood", campo("boolean",
                                "false si el texto es tan corto o tan vago que no puedes decidir"
                                        + " nada. Con false, las dos listas van vacias."))
                        .putDocument("out_of_domain", campo("boolean",
                                "true si el texto NO describe un negocio de cuidado animal. Con"
                                        + " true, las dos listas van vacias."))
                        .putDocument("necesarios", lista(
                                "Modulos sin los cuales no puede operar lo que el describio."))
                        .putDocument("recomendados", lista(
                                "Modulos que le ayudarian pero que no pidio ni se deducen. Ante"
                                        + " la duda, la linea va aqui."))
                        .putDocument("usuarios", campo("integer",
                                "Cuantas personas van a usar el sistema, entre 1 y 500. Pon 0 si"
                                        + " el texto no lo dice; no lo estimes."))
                        .putDocument("sedes", campo("integer",
                                "Cuantas sedes describio, entre 1 y 200. Pon 0 si el texto no lo"
                                        + " dice; no lo estimes."))
                        .putDocument("cajas", campo("integer",
                                "Cuantos puntos de cobro describio, entre 0 y 100. Pon 0 si el"
                                        + " texto no lo dice; no lo estimes.")))
                .putList("required", requeridos(CAMPOS_ORDENADOS))
                .putBoolean("additionalProperties", false).build();
    }

    private static Document campo(String tipo, String descripcion) {
        return Document.mapBuilder().putString("type", tipo).putString("description", descripcion)
                .build();
    }

    /**
     * Un array de objetos <code>{"code": …, "motivo": …}</code>, <strong>no de
     * cadenas</strong>. El motivo viaja pegado a su codigo dentro de cada elemento:
     * en un mapa aparte, un modelo que devolviera las listas y el mapa desalineados
     * dejaria lineas con el motivo de otra.
     */
    private static Document lista(String descripcion) {
        return Document.mapBuilder().putString("type", "array")
                .putString("description", descripcion)
                .putDocument("items", Document.mapBuilder().putString("type", "object")
                        .putMap("properties", propiedades -> propiedades
                                .putDocument("code", campo("string",
                                        "Un codigo tal y como aparece en el CATALOGO del mensaje"
                                                + " de sistema. Si dudas del codigo, no incluyas"
                                                + " la linea."))
                                .putDocument("motivo", campo("string",
                                        "Una sola frase en espanol de Colombia, dirigida al"
                                                + " cliente y de tu, que cite lo que EL dijo."
                                                + " Maximo 140 caracteres. SIN NINGUNA CIFRA, sin"
                                                + " simbolos de moneda, sin URLs y sin promesas.")))
                        .putList("required", requeridos(List.of("code", "motivo")))
                        .putBoolean("additionalProperties", false).build())
                .build();
    }

    private static List<Document> requeridos(List<String> nombres) {
        return nombres.stream().map(Document::fromString).toList();
    }
}
