package com.vetsoftware.app.configurator.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce respuestas en carrito. Es la pieza donde un error no da una
 * excepción: da una cotización equivocada, firmada por el cliente.
 *
 * <p>
 * <strong>El orden de aplicación es el de {@code priority} ascendente, con
 * desempate por {@code id}, y eso es parte del contrato.</strong> Los efectos
 * no conmutan: un {@code ADD} y un {@code REMOVE} sobre el mismo artículo dan
 * un resultado distinto según cuál corra último, y {@code SET_QUANTITY} pisa lo
 * que hubiera. Sin un orden declarado, la selección dependería del orden de
 * recuperación de la base, que no es determinista — el mismo cuestionario y las
 * mismas respuestas darían dos carritos distintos y nadie sabría cuál es el
 * bueno.
 *
 * <p>
 * <strong>Y ordenar por {@code id} tampoco bastaba, que es lo que hacía hasta
 * hoy.</strong> El {@code id} es el orden en que alguien insertó las filas, no
 * una decisión de nadie: con él, un {@code REMOVE} sembrado antes deshace un
 * {@code ADD} de una pregunta <em>posterior</em>. El caso concreto es el que
 * describe el changeset 238 — un efecto añade Inventario por «vendo productos»
 * y otro lo quita por «soy solo estética»— y quien marca las dos cosas se queda
 * sin Inventario: <em>marcar más servicios produce un carrito más pequeño</em>.
 * La columna existía en el esquema desde 238 y no estaba mapeada, así que la
 * regla que ese changeset declara no la ejecutaba nadie.
 *
 * <p>
 * Lo que este resolvedor <strong>no</strong> hace, a propósito: restar
 * {@code catalog_prices.included_quantity} (regla R15). Lo incluido depende de
 * la lista de precios con la que se cotiza, que es un dato de {@code pricelist}
 * y de la cotización, no del cuestionario. La resta va en el servicio que
 * cotiza; mezclarla aquí ataría el configurador a una tarifa.
 *
 * <p>
 * Puro: sin Spring, sin repositorios, sin reloj. Recibe los efectos ya
 * cargados.
 */
public final class ConfiguratorResolver {

    /**
     * Ascendente por {@code priority}; a igualdad, por {@code id}. El desempate por
     * {@code id} es lo que hace el orden <em>total</em>: la prioridad no es única
     * —dos efectos de la misma pregunta comparten decena a propósito— y sin
     * desempate dos ejecuciones con los mismos datos podrían aplicar el mismo par
     * en orden distinto.
     *
     * <p>
     * {@code nullsLast} sobre el {@code id} cubre al efecto todavía sin persistir,
     * que no tiene clave: va al final de su decena, que es donde tiene que ir un
     * efecto que aún no existía cuando se repartieron las prioridades.
     */
    private static final Comparator<ConfiguratorEffect> EN_ORDEN_DE_APLICACION = Comparator
            .comparingInt(ConfiguratorEffect::getPriority).thenComparing(ConfiguratorEffect::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private ConfiguratorResolver() {
    }

    /**
     * La selección que producen esas respuestas sobre esos efectos.
     *
     * @param effects
     *            los efectos activos del cuestionario; se ordenan aquí dentro por
     *            {@code (priority, id)} para no depender del orden en que lleguen
     * @param answers
     *            lo que respondió el prospecto
     * @return los artículos con cantidad mayor que cero, ordenados por id de
     *         artículo para que dos ejecuciones iguales den la misma lista
     */
    public static List<SelectedItem> resolve(List<ConfiguratorEffect> effects,
            ConfiguratorAnswers answers) {
        if (effects == null || effects.isEmpty()) {
            return List.of();
        }
        ConfiguratorAnswers respuestas = answers == null ? ConfiguratorAnswers.empty() : answers;
        Map<Long, Integer> carrito = new LinkedHashMap<>();

        List<ConfiguratorEffect> enOrden = new ArrayList<>(effects);
        enOrden.sort(EN_ORDEN_DE_APLICACION);

        for (ConfiguratorEffect effect : enOrden) {
            if (!effect.isEnabled() || !seDispara(effect, respuestas)) {
                continue;
            }
            aplicar(effect, respuestas, carrito);
        }

        return carrito.entrySet().stream().filter(entrada -> entrada.getValue() > 0)
                .map(entrada -> new SelectedItem(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(SelectedItem::catalogItemId)).toList();
    }

    /**
     * Un efecto por opción se dispara si esa opción está marcada; uno por pregunta,
     * si esa pregunta numérica trae respuesta. Que solo tenga uno de los dos
     * disparadores lo garantiza la invariante de {@link ConfiguratorEffect}.
     */
    private static boolean seDispara(ConfiguratorEffect effect, ConfiguratorAnswers answers) {
        return effect.isTriggeredByQuestion()
                ? answers.numericAnswers().containsKey(effect.getQuestionId())
                : answers.selectedOptionIds().contains(effect.getOptionId());
    }

    private static void aplicar(ConfiguratorEffect effect, ConfiguratorAnswers answers,
            Map<Long, Integer> carrito) {
        Long item = effect.getCatalogItemId();
        switch (effect.getEffect()) {
            case ADD -> carrito.merge(item, 1, (viejo, uno) -> viejo);
            case REMOVE -> carrito.remove(item);
            case SET_QUANTITY -> carrito.put(item, effect.getQuantity());
            case QUANTITY_FROM_ANSWER -> aplicarCantidadDeLaRespuesta(effect, answers, carrito);
        }
    }

    /**
     * La cantidad es el número que escribió el cliente. Un cero saca el artículo
     * del carrito en vez de dejar una línea de cero unidades: «no quiero terminales
     * extra» y «quiero cero terminales extra» son la misma respuesta, y una línea
     * de cero en una cotización impresa es una pregunta del cliente esperando a
     * pasar.
     *
     * <p>
     * Si el efecto no lo dispara una pregunta no hay número que leer. No puede
     * ocurrir tras la validación de guardado, pero la resolución no es el sitio
     * donde descubrirlo: se ignora el efecto y se sigue cotizando.
     */
    private static void aplicarCantidadDeLaRespuesta(ConfiguratorEffect effect,
            ConfiguratorAnswers answers, Map<Long, Integer> carrito) {
        if (!effect.isTriggeredByQuestion()) {
            return;
        }
        Integer respondido = answers.numericAnswers().get(effect.getQuestionId());
        if (respondido == null || respondido <= 0) {
            carrito.remove(effect.getCatalogItemId());
            return;
        }
        carrito.put(effect.getCatalogItemId(), respondido);
    }
}
