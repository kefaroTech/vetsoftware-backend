package com.vetsoftware.app.aiproposal.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * El motor determinista. <strong>Puro</strong>: sin Spring, sin repositorios y
 * sin reloj.
 *
 * <p>
 * La IA aparece en un solo punto de toda la feature -leer al cliente-, y lo que
 * devuelve son <em>candidatos</em>, no decisiones. Todo lo que pasa a partir de
 * aqui es codigo auditable: que codigos son validos, que dependencias hay que
 * cerrar y que se puede cotizar.
 *
 * <p>
 * <strong>El orden es contrato</strong> (plan S4.4):
 *
 * <ol>
 * <li>Descartar codigos inexistentes ({@code UNKNOWN_CODE}), no publicados
 * ({@code NOT_SELLABLE}) y no contratables por autoservicio
 * ({@code NOT_SELF_SERVICE}).</li>
 * <li>Deduplicar ({@code DUPLICATE}).</li>
 * <li>Anadir el articulo {@code is_core}, siempre.</li>
 * <li>Cerrar {@code REQUIRES} en BFS con conjunto de visitados: la cadena
 * entera, no un salto.</li>
 * <li><strong>Nunca</strong> auto-anadir {@code RECOMMENDS}: es un upsell
 * disfrazado de requisito tecnico. Aqui es estructural -{@link SellableCatalog}
 * no guarda ese arco-.</li>
 * <li>Incluir {@code CAPACITY_TERMINAL} si entra {@code CASH_REGISTER}.</li>
 * </ol>
 *
 * <p>
 * ⛔ <strong>{@code recomendados} NO entra en el carrito. Nunca.</strong> Sus
 * lineas pasan los pasos 1 y 2 igual -un recomendado inexistente o no
 * contratable se descarta igual- pero <strong>no disparan el cierre de
 * {@code REQUIRES}</strong>: cerrar dependencias de algo que nadie pidio es
 * como un carrito de 6 lineas se convierte en uno de 10. Fundirlas en el
 * carrito es la misma decision que S1.5 prohibe, aplicada dos veces: el modelo
 * va sesgado a recomendar de mas, y la interfaz las presentaria ya elegidas.
 */
public final class ProposalCart {

    /**
     * Motivo de reserva. {@code chk_ai_proposal_lines_model_reason} exige motivo en
     * toda linea de modelo, tambien en las rechazadas, y el modelo puede no haber
     * escrito uno. Es un texto fijo: <strong>no hace eco del codigo
     * recibido</strong>, que seria justo el canal lateral que S6.5 cierra.
     */
    public static final String MOTIVO_AUSENTE = "Sin motivo declarado por el asistente.";

    /**
     * Los dos codigos de la regla 3 de S2.3. {@code CAPACITY_TERMINAL} es
     * componente de los tres paquetes, asi que quien contrata {@code CASH_REGISTER}
     * sin esa linea se queda con techo cero de terminales y <strong>no puede abrir
     * la primera caja</strong>.
     */
    private static final String CASH_REGISTER = "CASH_REGISTER";

    private static final String CAPACITY_TERMINAL = "CAPACITY_TERMINAL";

    private ProposalCart() {
    }

    /**
     * @param necesarios
     *            los codigos que el modelo declaro necesarios
     * @param recomendados
     *            los que ofrecio como opcionales; salen aparte y sin sumar
     * @param motivos
     *            motivo en prosa por codigo, ya saneado por quien llama
     * @param catalog
     *            la foto del catalogo con la que se decide todo
     */
    public static CartResult build(List<String> necesarios, List<String> recomendados,
            Map<String, String> motivos, SellableCatalog catalog) {
        return build(necesarios, recomendados, motivos, catalog, LineSource.MODEL);
    }

    /**
     * La misma maquina, declarando <strong>quien</strong> puso las lineas de la
     * primera lista.
     *
     * <p>
     * Existe por el turno {@code CUSTOMER_EDIT}: alli el carrito lo decide el
     * cliente, no el modelo, y persistirlo con {@code source = MODEL} afirmaria por
     * escrito que lo propuso el asistente —que es justo la senal con la que se mide
     * si el modelo sirve—. Un turno de edicion registra <strong>el carrito tal como
     * lo dejo el cliente</strong>; la autoria del modelo sigue viva, intacta, en
     * las lineas de su propio turno, que no se borran nunca.
     *
     * <p>
     * El cierre de {@code REQUIRES} conserva su {@code DEPENDENCY_CLOSURE} en los
     * dos casos: lo arrastro el grafo, no la persona.
     */
    public static CartResult build(List<String> necesarios, List<String> recomendados,
            Map<String, String> motivos, SellableCatalog catalog, LineSource origen) {
        if (origen == null || origen == LineSource.DEPENDENCY_CLOSURE)
            throw new IllegalArgumentException("the seed source must be MODEL or CUSTOMER");
        if (catalog == null)
            throw new IllegalArgumentException("catalog is required");
        List<String> pedidos = necesarios == null ? List.of() : necesarios;
        List<String> sugeridos = recomendados == null ? List.of() : recomendados;
        Map<String, String> prosa = motivos == null ? Map.of() : motivos;

        List<CartLine> lineas = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        Set<String> enCarrito = new LinkedHashSet<>();

        // Pasos 1 y 2 sobre lo necesario: lo aceptado es la semilla del carrito.
        for (String code : pedidos) {
            evaluar(code, origen, prosa.get(code), catalog, vistos, lineas)
                    .ifPresent(item -> enCarrito.add(item.code()));
        }

        // Pasos 1 y 2 sobre lo recomendado: mismo filtro, y hasta aqui llegan.
        //
        // ⛔ Un codigo que YA se evaluo como necesario no se vuelve a evaluar aqui.
        // Si se hiciera, evaluar() escribiria una segunda linea con el mismo codigo
        // y veredicto DUPLICATE, y las dos filas chocarian contra
        // uq_ai_proposal_lines_code al cerrar el turno: 500, transaccion revertida y
        // turno PENDING huerfano con la llamada al modelo ya pagada. El validador
        // deduplica las dos listas antes de llegar aqui, pero el refinamiento vuelve
        // a mezclarlas -fusionar() anade lo que el cliente puso a mano, que puede
        // ser justo lo que el modelo ahora recomienda-, asi que la guarda tiene que
        // estar TAMBIEN en el motor: es el unico punto por el que pasan los tres
        // llamantes.
        for (String code : sugeridos) {
            if (code == null || vistos.contains(code))
                continue;
            evaluar(code, LineSource.MODEL_RECOMMENDED, prosa.get(code), catalog, vistos, lineas);
        }

        // Paso 3: el nucleo entra siempre, lo pidiera el modelo o no.
        // La guarda mira `vistos` y no `enCarrito`, igual que las dos de cerrar(...)
        // -ver su javadoc-. Aqui es defensiva y hoy no alcanzable: el filtro
        // esCotizable de la linea de arriba ya descarta al nucleo que evaluar
        // habria rechazado, asi que las dos guardas coinciden. Se escribe con el
        // mismo predicado para que las tres digan lo mismo y no haya que razonar
        // cual de ellas era la buena el dia que el filtro cambie.
        catalog.core().filter(SellableItem::esCotizable).ifPresent(nucleo -> {
            if (!vistos.contains(nucleo.code()))
                anadirPorCierre(nucleo.code(), catalog, vistos, enCarrito, lineas);
        });

        // Pasos 4 y 6: el cierre y la regla del terminal, en el mismo recorrido.
        cerrar(catalog, vistos, enCarrito, lineas);

        return new CartResult(lineas, monedaDe(lineas, catalog));
    }

    /**
     * Un paso de validacion. Devuelve el articulo <strong>solo</strong> si la linea
     * quedo aceptada y por tanto entra al carrito; en cualquier otro caso deja la
     * linea rechazada escrita y devuelve vacio.
     */
    private static Optional<SellableItem> evaluar(String code, LineSource source, String motivo,
            SellableCatalog catalog, Set<String> vistos, List<CartLine> lineas) {
        if (code == null || code.isBlank())
            return Optional.empty();
        String texto = source.exigeMotivo() ? motivoDe(motivo) : motivo;
        if (!vistos.add(code)) {
            lineas.add(rechazo(code, source, LineVerdict.DUPLICATE, texto, catalog, lineas.size()));
            return Optional.empty();
        }
        Optional<SellableItem> encontrado = catalog.find(code);
        if (encontrado.isEmpty()) {
            lineas.add(
                    rechazo(code, source, LineVerdict.UNKNOWN_CODE, texto, catalog, lineas.size()));
            return Optional.empty();
        }
        SellableItem item = encontrado.get();
        if (!item.active()) {
            lineas.add(
                    rechazo(code, source, LineVerdict.NOT_SELLABLE, texto, catalog, lineas.size()));
            return Optional.empty();
        }
        if (!item.selfServiceEligible()) {
            lineas.add(rechazo(code, source, LineVerdict.NOT_SELF_SERVICE, texto, catalog,
                    lineas.size()));
            return Optional.empty();
        }
        lineas.add(aceptada(item, source, texto, lineas.size()));
        return Optional.of(item);
    }

    /**
     * El cierre de {@code REQUIRES} en anchura, con conjunto de visitados para que
     * un ciclo no lo cuelgue. <strong>La regla del terminal viaja dentro del mismo
     * recorrido</strong>, y no como un paso posterior: {@code CASH_REGISTER} puede
     * entrar tanto porque lo pidio el modelo como porque lo arrastro otra
     * dependencia, y un paso al final solo cubriria el primer caso.
     *
     * <p>
     * &#9940; <strong>Las guardas de «esto ya esta» miran {@code vistos}, nunca
     * {@code enCarrito}</strong>, y la diferencia entre los dos conjuntos era un
     * 500. {@code enCarrito} son los codigos <em>aceptados</em>; {@code vistos} son
     * los <em>evaluados</em>, aceptados o no. Un requisito que se evaluo y salio
     * rechazado -no existe, no esta publicado, no es autoservicio- esta en
     * {@code vistos} y no en {@code enCarrito}: con la guarda vieja se volvia a
     * evaluar, y {@link #evaluar} escribia entonces una segunda linea con el mismo
     * codigo y veredicto {@code DUPLICATE}. Dos filas con el mismo
     * {@code (turn_id, item_code)} chocan contra {@code uq_ai_proposal_lines_code},
     * el {@code saveLines} revienta, la transaccion revierte y el turno se queda
     * {@code PENDING} para siempre con la llamada al modelo ya pagada.
     *
     * <p>
     * <strong>Es alcanzable hoy y sin modelo</strong>, a diferencia del duplicado
     * entre las dos listas del borrador: basta con que un articulo requerido —o el
     * propio nucleo— este marcado {@code active = false} o
     * {@code self_service_eligible = false} en el catalogo publicado, que es una
     * edicion normal de negocio y no una averia.
     */
    private static void cerrar(SellableCatalog catalog, Set<String> vistos, Set<String> enCarrito,
            List<CartLine> lineas) {
        Deque<String> frontera = new ArrayDeque<>(enCarrito);
        Set<String> expandidos = new LinkedHashSet<>();
        while (!frontera.isEmpty()) {
            String actual = frontera.poll();
            if (!expandidos.add(actual))
                continue;
            for (String requerido : catalog.requiredBy(actual)) {
                if (vistos.contains(requerido))
                    continue;
                if (anadirPorCierre(requerido, catalog, vistos, enCarrito, lineas))
                    frontera.add(requerido);
            }
            if (CASH_REGISTER.equals(actual) && !vistos.contains(CAPACITY_TERMINAL)
                    && anadirPorCierre(CAPACITY_TERMINAL, catalog, vistos, enCarrito, lineas)) {
                frontera.add(CAPACITY_TERMINAL);
            }
        }
    }

    /**
     * Anade una linea de cierre. Un requisito que no se puede cotizar deja su linea
     * rechazada escrita -es telemetria de la misma calidad que el resto- y no entra
     * al carrito.
     */
    private static boolean anadirPorCierre(String code, SellableCatalog catalog, Set<String> vistos,
            Set<String> enCarrito, List<CartLine> lineas) {
        return evaluar(code, LineSource.DEPENDENCY_CLOSURE, null, catalog, vistos, lineas)
                .map(item -> enCarrito.add(item.code())).orElse(false);
    }

    private static CartLine aceptada(SellableItem item, LineSource source, String motivo,
            int orden) {
        return new CartLine(item.code(), item.name(), item.shortDescription(), item.kind(), source,
                LineVerdict.ACCEPTED, 1, item.unitAmount(), item.taxRate(), item.trialDays(),
                item.currency(), motivo, orden);
    }

    /**
     * Una linea rechazada conserva el codigo <strong>verbatim</strong>: la
     * alucinacion del modelo es precisamente el dato que mide su calidad. No lleva
     * precio -no se cotiza- y por eso tampoco divisa.
     */
    private static CartLine rechazo(String code, LineSource source, LineVerdict verdict,
            String motivo, SellableCatalog catalog, int orden) {
        SellableItem item = catalog.find(code).orElse(null);
        return new CartLine(code, item == null ? code : item.name(),
                item == null ? null : item.shortDescription(), item == null ? null : item.kind(),
                source, verdict, 1, null, null, 0, null, motivo, orden);
    }

    private static String motivoDe(String motivo) {
        return motivo == null || motivo.isBlank() ? MOTIVO_AUSENTE : motivo;
    }

    /**
     * La divisa del carrito sale de lo que se va a cobrar. Con el carrito vacio cae
     * en la del nucleo, que existe siempre en el catalogo; si tampoco lo hubiera,
     * en la primera del catalogo. Un {@code CartResult} sin divisa no es
     * construible a proposito: es el defecto que dejo 52 de 53 DTO de dinero mudos.
     */
    private static String monedaDe(List<CartLine> lineas, SellableCatalog catalog) {
        return lineas.stream().filter(l -> l.verdict().esAceptado()).map(CartLine::currency)
                .filter(java.util.Objects::nonNull).findFirst()
                .or(() -> catalog.core().map(SellableItem::currency))
                .or(() -> catalog.items().values().stream().map(SellableItem::currency).findFirst())
                .orElseThrow(() -> new IllegalArgumentException(
                        "an empty catalog cannot price a proposal"));
    }
}
