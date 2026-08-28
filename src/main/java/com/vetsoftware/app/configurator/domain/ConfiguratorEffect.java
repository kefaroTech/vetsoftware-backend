package com.vetsoftware.app.configurator.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * El corazón del configurador: traduce una respuesta en un artículo del
 * carrito.
 *
 * <p>
 * <strong>Se dispara por opción o por pregunta, nunca por las dos.</strong> Por
 * opción cuando la respuesta es una elección; por pregunta cuando la pregunta
 * es numérica y el número <em>es</em> la respuesta. Con los dos disparadores
 * rellenos el efecto se ejecutaría dos veces y el artículo entraría duplicado
 * en el carrito — de ahí {@code chk_configurator_effects_trigger} en la base y
 * la misma invariante repetida aquí, que es la que da un 400 con mensaje en vez
 * de un 500 traduciendo una violación de constraint.
 *
 * <p>
 * <strong>{@code priority} decide el orden de aplicación, y no es
 * cosmética.</strong> Los efectos no conmutan. Si un efecto añade Inventario
 * por «vendo productos» y otro lo quita por «soy solo estética», quien marque
 * <em>las dos</em> cosas se queda sin Inventario o no según cuál corra último,
 * y el síntoma es el peor posible para un configurador: <em>marcar más
 * servicios produce un carrito más pequeño</em>. Nadie lo lee como un error de
 * datos; se lee como que el producto no funciona.
 *
 * <p>
 * <strong>La regla</strong>: ascendente por {@code priority}; a igualdad, por
 * {@code id}. Es regla de código —una columna no puede ordenar nada por sí
 * sola—, y hasta hoy no lo era: la columna existía en el esquema desde el
 * changeset 238 y <em>no estaba mapeada en Java</em>, así que el orden real era
 * el de creación. La invariante que la sostiene —ningún {@code REMOVE} con
 * prioridad mayor que un {@code ADD} del mismo artículo disparado por una
 * pregunta posterior— <em>no cabe en un {@code CHECK}</em>, porque es una
 * comparación entre filas de tablas distintas: la siembra la cumple por
 * construcción reservando una decena por pregunta (P1 en 10-19, P2 en 20-29,
 * …), de modo que cada pregunta corrige a las anteriores y ninguna puede
 * corregir a las siguientes.
 *
 * <p>
 * <strong>Que el valor por defecto sea cero también es la decisión.</strong> Un
 * efecto sin prioridad declarada tiene que caer <em>antes</em> que cualquiera
 * de los sembrados —que empiezan en 10—, no en medio: si cayera en medio,
 * añadir una fila a mano desde la consola se colaría entre dos decenas y
 * desharía una corrección.
 */
public class ConfiguratorEffect {

    /**
     * Espejo de {@code chk_configurator_effects_priority}. El rango se acota a algo
     * legible por un humano a proposito: la columna es un orden relativo, no un
     * identificador, y permitir dos mil millones invita a usarla como hueco
     * infinito en vez de reordenar las decenas.
     */
    public static final int MIN_PRIORITY = 0;

    /** Ver {@link #MIN_PRIORITY}. */
    public static final int MAX_PRIORITY = 9999;

    /**
     * Con que prioridad nace un efecto que no la declara. Es el {@code DEFAULT 0}
     * de la columna, repetido aqui para que el dominio no dependa de que la base
     * rellene el hueco.
     */
    private static final int DEFAULT_PRIORITY = MIN_PRIORITY;

    private final Long id;
    private final Long optionId;
    private final Long questionId;
    private Long catalogItemId;
    private EffectType effect;
    private Integer quantity;

    /**
     * Orden de aplicacion, ascendente. Ver el javadoc de la clase: sin el, dos
     * efectos que no conmutan se aplican en el orden en que alguien inserto las
     * filas.
     */
    private int priority;

    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public ConfiguratorEffect(Long id, Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity, int priority, LocalDateTime createdDate,
            Long version, boolean enabled) {
        validate(optionId, questionId, catalogItemId, effect, quantity);
        validatePriority(priority);
        this.id = id;
        this.optionId = optionId;
        this.questionId = questionId;
        this.catalogItemId = catalogItemId;
        this.effect = effect;
        this.quantity = quantity;
        this.priority = priority;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    /**
     * Un efecto recien creado nace con la prioridad por defecto: el alta no elige
     * su sitio en el orden, lo elige despues quien reordena. Es deliberado —
     * repartir prioridades es una decision sobre el <em>conjunto</em> de efectos, y
     * dejarla en el formulario de alta produce colisiones que nadie revisa.
     */
    public static ConfiguratorEffect create(Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity, Clock clock) {
        return new ConfiguratorEffect(null, optionId, questionId, catalogItemId, effect, quantity,
                DEFAULT_PRIORITY, LocalDateTime.now(clock), null, true);
    }

    /**
     * Mueve el efecto de sitio en el orden de aplicacion. <strong>Es la unica forma
     * de reordenar</strong>: la alternativa que habia hasta hoy era borrar el
     * efecto y volver a crearlo, lo que le cambia el {@code id} —y con el, el
     * desempate— y reordena de paso todo lo demas.
     */
    public void reprioritize(int priority) {
        validatePriority(priority);
        this.priority = priority;
    }

    /**
     * El disparador no se edita. Cambiar de opción a pregunta —o de una opción a
     * otra— es otro efecto distinto, y editarlo en sitio deja las dos claves únicas
     * de la tabla vigilando pares que ya no existen.
     */
    public void update(Long catalogItemId, EffectType effect, Integer quantity) {
        validate(this.optionId, this.questionId, catalogItemId, effect, quantity);
        this.catalogItemId = catalogItemId;
        this.effect = effect;
        this.quantity = quantity;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    /** {@code true} si lo dispara el número respondido a una pregunta. */
    public boolean isTriggeredByQuestion() {
        return questionId != null;
    }

    private static void validate(Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity) {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (effect == null)
            throw new IllegalArgumentException("effect is required");
        boolean byOption = optionId != null;
        boolean byQuestion = questionId != null;
        if (byOption == byQuestion)
            throw new IllegalArgumentException(
                    "exactly one trigger is required: either optionId or questionId");
        if (effect == EffectType.SET_QUANTITY) {
            if (quantity == null || quantity <= 0)
                throw new IllegalArgumentException(
                        "SET_QUANTITY requires a quantity greater than 0");
        } else if (quantity != null) {
            throw new IllegalArgumentException("quantity is only allowed for SET_QUANTITY");
        }
    }

    /**
     * Espejo literal de {@code chk_configurator_effects_priority}. Comprobarlo aqui
     * convierte un {@code Check constraint 'chk_configurator_effects_priority' is
     * violated} —que no dice ni que columna ni que valor— en un 400 que nombra el
     * campo y el rango.
     */
    private static void validatePriority(int priority) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY)
            throw new IllegalArgumentException(
                    "priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY);
    }

    public Long getId() {
        return id;
    }

    public Long getOptionId() {
        return optionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public EffectType getEffect() {
        return effect;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public int getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
