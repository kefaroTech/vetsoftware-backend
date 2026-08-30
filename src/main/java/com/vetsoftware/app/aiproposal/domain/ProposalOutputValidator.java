package com.vetsoftware.app.aiproposal.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convierte lo que dijo el modelo en algo que el motor determinista puede
 * procesar sin creerselo.
 *
 * <p>
 * ⛔ <strong>Aqui vive el argumento de seguridad de la feature, y el plan lo
 * tenia incompleto.</strong> S7.3 afirmaba que "el modelo hace exactamente una
 * cosa —elegir codigos— y todo lo de aguas abajo es determinista". Falso: el
 * modelo controla ademas el motivo en prosa libre, tres enteros de capacidad y
 * dos booleanos que deciden que pantalla ve el prospecto. Los cuatro se tratan
 * como entrada de un atacante, porque el texto que los produce lo escribe
 * cualquiera.
 *
 * <p>
 * <strong>Lo que este validador hace, en orden:</strong>
 *
 * <ol>
 * <li><strong>Acota el tamano.</strong> Un modelo atascado puede devolver
 * quinientos codigos; cada uno seria una fila en {@code ai_proposal_lines} y
 * una linea de prosa que sanear.</li>
 * <li><strong>Descarta lo que ni siquiera puede ser una linea</strong>: codigo
 * nulo, en blanco o de mas de 50 caracteres —{@code CartLine} y
 * {@code item_code VARCHAR(50)} lo rechazarian, y una excepcion aqui
 * convertiria una alucinacion en un 500—.</li>
 * <li><strong>NO intersecta con el catalogo.</strong> Eso lo hace
 * {@link ProposalCart}, y tiene que hacerlo el: filtrar aqui borraria las
 * lineas {@code UNKNOWN_CODE} que son la senal con la que se mide la calidad
 * del modelo. La interseccion existe —es el paso 1 del motor— pero produce un
 * veredicto en vez de un silencio.</li>
 * <li><strong>Impone la coherencia de los dos booleanos.</strong> El servidor
 * decide, no el modelo: si dijo que no entendio o que esta fuera de dominio, el
 * borrador sale sin lineas <em>aunque</em> las hubiera mandado. Confiar en que
 * un modelo mantenga coherente su propia salida es confiar en el atacante que
 * escribio su entrada.</li>
 * <li><strong>Sanea todos los motivos</strong> con las nueve reglas, incluida
 * la novena, que necesita el turno entero.</li>
 * <li><strong>Acota las tres capacidades</strong>, que ademas nunca llegan a
 * ser una linea cotizada ({@link CapacityHint}).</li>
 * </ol>
 */
public final class ProposalOutputValidator {

    /**
     * Techo de codigos por lista. Es holgado respecto al catalogo real —26
     * articulos, 13 vendibles a mano— a proposito: no es un filtro de calidad, es
     * el limite que impide que una salida desbocada se convierta en trabajo
     * proporcional a lo que el modelo quiera escribir.
     */
    private static final int MAX_CODES = 40;

    private static final int MAX_CODE_CHARS = 50;

    private ProposalOutputValidator() {
    }

    public static ProposalDraft validate(ModelProposalPayload payload, SellableCatalog catalog) {
        if (payload == null)
            return ProposalDraft.sinLineas(false, false);

        // La contradiccion se DESCARTA y se CUENTA (plan S8.2.1): no se sirve ni una
        // linea, pero que el modelo se contradiga es una senal de calidad, y tirarla
        // en silencio es apagarla.
        if (!payload.understood() || payload.outOfDomain())
            return ProposalDraft.sinLineas(payload.understood(), payload.outOfDomain(),
                    contradichos(payload));

        // Deduplicar aqui NO es simetria estetica con contradichos(): es lo que
        // impide un 500. Ver el javadoc de deduplicar(...).
        List<String> necesarios = acotar(deduplicar(payload.necessaryCodes(), Set.of()));
        List<String> recomendados = acotar(
                deduplicar(payload.recommendedCodes(), Set.copyOf(necesarios)));
        if (necesarios.isEmpty() && recomendados.isEmpty())
            return ProposalDraft.sinLineas(true, false);

        return new ProposalDraft(true, false, necesarios, recomendados,
                sanear(payload, necesarios, recomendados, catalog),
                new CapacityHint(valor(payload.staff()), valor(payload.branches()),
                        valor(payload.terminals())),
                0);
    }

    /**
     * Los codigos con los que el modelo se contradijo, <strong>nombrados y no solo
     * contados</strong>. El caso de uso los persiste como lineas con veredicto
     * {@code NOT_SELLABLE} (plan S8.2.1), que es donde esta senal de calidad se
     * puede consultar despues; un entero suelto no tiene donde vivir en el esquema
     * y se perderia al cerrar el turno.
     *
     * <p>
     * Pasan por el mismo {@link #acotar(List)} que las listas buenas —y ademas se
     * deduplican—, porque {@code ai_proposal_lines} tiene
     * {@code uq_ai_proposal_lines_code} sobre {@code (turn_id, item_code)} y
     * {@code item_code} es {@code VARCHAR(50) NOT NULL}: un codigo repetido o de
     * sesenta caracteres reventaria la escritura del turno entero.
     */
    private static List<String> contradichos(ModelProposalPayload payload) {
        List<String> todos = new ArrayList<>(payload.necessaryCodes());
        todos.addAll(payload.recommendedCodes());
        return acotar(todos.stream().distinct().toList());
    }

    /**
     * &#9940; <strong>Un codigo repetido entre las dos listas revienta el turno
     * entero con un 500, y la llamada ya esta pagada.</strong>
     *
     * <p>
     * {@code ai_proposal_lines} lleva {@code uq_ai_proposal_lines_code} sobre
     * {@code (turn_id, item_code)}. {@code ProposalCart.evaluar} escribe una linea
     * por codigo <em>incluidos los rechazos</em>, y el rechazo por duplicado
     * conserva el codigo verbatim -a proposito: la alucinacion es la senal-, asi
     * que un {@code CORE} en {@code necessaryCodes} y otro en
     * {@code recommendedCodes} produce dos filas con el mismo
     * {@code (turn_id, item_code)}. El {@code saveLines} falla, TX2 revierte, el
     * turno se queda {@code PENDING} para siempre y el prospecto recibe un 500 por
     * una propuesta que ya se cobro al modelo.
     *
     * <p>
     * {@link #contradichos(ModelProposalPayload)} ya lo hacia y su javadoc explica
     * exactamente este motivo; lo que faltaba era aplicarlo tambien al camino
     * bueno, que es el que corre siempre. <strong>Hoy no es alcanzable</strong>
     * porque {@code ModelAccessNotEnabledInvoker} deja todo borrador sin lineas: se
     * arma sola el dia que se habilite Bedrock, que es lo que la hace mas urgente y
     * no menos.
     *
     * <p>
     * <strong>Lo que se pierde</strong>: la linea {@code DUPLICATE} que el motor
     * habria escrito como telemetria de calidad del modelo. Es un intercambio
     * deliberado —una senal de calidad contra un 500 y una llamada de pago tirada—
     * y ademas la senal no desaparece del todo: el codigo sigue en el carrito con
     * su veredicto real.
     *
     * @param codigos
     *            la lista tal como la mando el modelo
     * @param yaEmitidos
     *            lo que ya salio en la lista anterior del mismo turno
     */
    private static List<String> deduplicar(List<String> codigos, Set<String> yaEmitidos) {
        Set<String> vistos = new LinkedHashSet<>(yaEmitidos);
        List<String> unicos = new ArrayList<>();
        for (String codigo : codigos) {
            if (vistos.add(codigo))
                unicos.add(codigo);
        }
        return unicos;
    }

    private static List<String> acotar(List<String> codigos) {
        List<String> validos = new ArrayList<>();
        for (String codigo : codigos) {
            if (validos.size() >= MAX_CODES)
                break;
            if (codigo != null && !codigo.isBlank() && codigo.length() <= MAX_CODE_CHARS)
                validos.add(codigo);
        }
        return validos;
    }

    /**
     * Se sanean los motivos <strong>de los codigos que quedaron</strong> y no todos
     * los que mando el modelo: un motivo huerfano —prosa para un codigo que se
     * descarto— no se persiste, no se sirve y no tiene por que gastar una regla.
     *
     * <p>
     * El {@code short_description} del fallback sale del catalogo por codigo. Un
     * codigo alucinado no tiene ninguno, y entonces el fallback es el texto fijo de
     * {@link ProposalCart#MOTIVO_AUSENTE}: <strong>no hace eco del codigo
     * recibido</strong>, que seria exactamente el canal lateral que S6.5 cierra.
     */
    private static Map<String, SanitizedReason> sanear(ModelProposalPayload payload,
            List<String> necesarios, List<String> recomendados, SellableCatalog catalog) {
        Map<String, String> crudos = new LinkedHashMap<>();
        for (String codigo : necesarios)
            crudos.put(codigo, payload.reasons().get(codigo));
        for (String codigo : recomendados)
            crudos.putIfAbsent(codigo, payload.reasons().get(codigo));

        Map<String, String> deterministas = new LinkedHashMap<>();
        if (catalog != null)
            crudos.keySet().forEach(codigo -> catalog.find(codigo)
                    .ifPresent(item -> deterministas.put(codigo, item.shortDescription())));

        return ProposalReasonSanitizer.sanitizeTurn(crudos, deterministas);
    }

    private static int valor(Integer entero) {
        return entero == null ? 0 : entero;
    }
}
