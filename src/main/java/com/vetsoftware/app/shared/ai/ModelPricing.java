package com.vetsoftware.app.shared.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * &#9940; <strong>La tarifa del modelo, y la UNICA fuente de la que cuelga todo
 * lo que depende de ella.</strong>
 *
 * <p>
 * Hasta hoy el precio eran cuatro {@code private static final} dentro de
 * {@code BedrockProposalGenerator} —las tarifas de Claude Sonnet y dos
 * estimaciones de tokens—, y de ese numero colgaba el resto: el guardian de
 * gasto reserva con el, lo reconcilia con el, el tope diario se traduce en
 * «cuantas llamadas financia» dividiendo por el, y el cupo diario por IP de
 * {@code LoginRateLimitFilter} se deriva de esa division. <strong>Cambiar de
 * modelo —Haiku 4.5 hoy, un DeepSeek manana— dejaba esa aritmetica callada y
 * equivocada</strong>: el sistema seguiria cortando por un numero de llamadas
 * calculado con un precio que ya no existe. Ni falla ni avisa; el tope
 * simplemente deja de significar lo que dice.
 *
 * <p>
 * <strong>Entrada y salida son dos numeros, no uno.</strong> La relacion entre
 * ambas varia mucho entre familias —Sonnet cobra la salida cinco veces mas cara
 * que la entrada; otras la cobran casi igual—, asi que un unico «precio por
 * llamada» configurable volveria a mentir en cuanto cambiara la mezcla de
 * tokens.
 *
 * <p>
 * <strong>Por que esta en el kernel y no en la rodaja del asistente.</strong>
 * Cumple las cuatro condiciones de admision de {@code shared/}: «USD por millon
 * de tokens» no significa nada distinto en {@code aiproposal} que en
 * {@code auth}; es un {@code record} inmutable sin identidad ni persistencia;
 * no nombra ninguna feature; y duplicarlo obliga a escribir dos veces la misma
 * aritmetica transversal —que es <em>exactamente</em> lo que habia: un literal
 * {@code "0.0176"} copiado en {@code LoginRateLimitFilter} y un test
 * comparandolo contra la constante del generador—. Un literal atado por un test
 * sigue siendo una segunda fuente: solo esta vigilada. Y ya se descalibro una
 * vez, con 20 peticiones por IP contra un tope que financiaba 18.
 *
 * <p>
 * <strong>Quien lo construye vive fuera</strong>
 * ({@code aiproposal.infrastructure.ai.ModelPricingConfig}), porque las claves
 * de configuracion son de la rodaja del asistente. El kernel solo aporta el
 * tipo y la aritmetica, sin una linea de Spring.
 *
 * <p>
 * &#9940; <strong>El cero esta prohibido, y no por simetria.</strong> Ya mordio
 * una vez: significaba tres cosas distintas segun donde se leyera, y en el cubo
 * global del filtro significaba «sin limite» —el techo de la plataforma se
 * apagaba justo cuando no habia presupuesto—. Un precio de cero ademas divide
 * por cero al derivar cupos y hace que una invocacion parezca gratis. Aqui es
 * <strong>imposible de construir</strong>, no tolerado: el compact constructor
 * lo rechaza, asi que ninguna capa de mas abajo tiene que defenderse de el.
 *
 * <p>
 * <strong>Esto NO reescribe historia, pero tampoco la conserva.</strong> El
 * coste se calcula al invocar, viaja dentro de {@code ModelUsage} y ahi muere:
 * <strong>{@code ai_proposal_turns} NO tiene columna de coste</strong> —no la
 * crea ninguna migracion, y {@code ProposalTurnWriter} no la escribe—. Cambiar
 * la tarifa mueve lo que se cobre de aqui en adelante y no toca nada de lo
 * anterior, que es lo correcto; pero <strong>lo ya cobrado no quedo registrado
 * por turno</strong>, asi que reconstruir el gasto historico despues de un
 * cambio de tarifa no se puede. Lo unico que queda es el contador diario del
 * guardian de gasto, que es agregado y se reinicia.
 *
 * @param pricedModelId
 *            de que modelo son estas tarifas. Viaja pegado a las cifras a
 *            proposito: un precio sin decir de que modelo es se convierte en
 *            otra cifra huerfana dentro de seis meses
 */
public record ModelPricing(BigDecimal usdPerMillionInputTokens,
        BigDecimal usdPerMillionOutputTokens, int estimatedInputTokens, int estimatedOutputTokens,
        String pricedModelId) {

    /**
     * Tarifas de {@link #MODELO_POR_DEFECTO}, USD por millon de tokens (plan S7.4).
     */
    public static final String DEFECTO_USD_POR_MILLON_ENTRADA = "1";

    /** Tarifa de salida de {@link #MODELO_POR_DEFECTO}. */
    public static final String DEFECTO_USD_POR_MILLON_SALIDA = "5";

    /**
     * El peor caso de S7.2.1, no la media: el cuarto turno acumulativo ronda los
     * 3.800 de entrada, y el tope de gasto se dimensiona con lo que un atacante va
     * a producir a proposito.
     */
    public static final String DEFECTO_TOKENS_ESTIMADOS_ENTRADA = "3800";

    public static final String DEFECTO_TOKENS_ESTIMADOS_SALIDA = "1000";

    /**
     * El modelo al que corresponden las cuatro cifras de arriba, y el mismo defecto
     * que {@code vetsoftware.ai.proposal.model-id}. Con estos valores una
     * invocacion de pago cuesta {@code 0.008800} USD.
     */
    public static final String MODELO_POR_DEFECTO = "us.anthropic.claude-haiku-4-5-20251001-v1:0";

    /**
     * Seis decimales, que es la escala con la que nacio la estimacion. Bajarla
     * redondearia a cero la tarifa de los modelos baratos y volveria a romper la
     * division que reparte los cupos.
     */
    private static final int ESCALA_USD = 6;

    private static final BigDecimal UN_MILLON = new BigDecimal("1000000");

    public ModelPricing {
        usdPerMillionInputTokens = exigirPositiva(usdPerMillionInputTokens,
                "la tarifa de entrada por millon de tokens");
        usdPerMillionOutputTokens = exigirPositiva(usdPerMillionOutputTokens,
                "la tarifa de salida por millon de tokens");
        exigirPositivos(estimatedInputTokens, "los tokens de entrada estimados por invocacion");
        exigirPositivos(estimatedOutputTokens, "los tokens de salida estimados por invocacion");
        if (pricedModelId == null || pricedModelId.isBlank())
            throw new IllegalArgumentException("hay que decir de que modelo son estas tarifas: un"
                    + " precio sin modelo es una cifra huerfana en cuanto pasen unos meses");
    }

    /**
     * Lo que se reserva antes de invocar, y lo que se cobra cuando el modelo no
     * declara su consumo. <strong>Es el numero del que cuelgan el guardian de gasto
     * y el reparto de cupos por IP</strong>: los dos tienen que pedirlo aqui, nunca
     * calcularlo por su cuenta ni copiarlo.
     */
    public BigDecimal usdPerCall() {
        return calcular(estimatedInputTokens, estimatedOutputTokens);
    }

    /**
     * El coste real de una invocacion.
     *
     * <p>
     * <strong>Sin tokens declarados se cobra la estimacion completa, no
     * cero</strong>: un modelo que no informa de su consumo ha consumido igual, y
     * asumir cero es exactamente como se vacia un cupo sin que el contador se
     * mueva.
     */
    public BigDecimal costOf(Integer inputTokens, Integer outputTokens) {
        int entrada = inputTokens == null || inputTokens < 0 ? estimatedInputTokens : inputTokens;
        int salida = outputTokens == null || outputTokens < 0
                ? estimatedOutputTokens
                : outputTokens;
        return calcular(entrada, salida);
    }

    private BigDecimal calcular(int entrada, int salida) {
        return BigDecimal.valueOf(entrada).multiply(usdPerMillionInputTokens)
                .add(BigDecimal.valueOf(salida).multiply(usdPerMillionOutputTokens))
                .divide(UN_MILLON, ESCALA_USD, RoundingMode.HALF_UP);
    }

    private static BigDecimal exigirPositiva(BigDecimal tarifa, String cual) {
        if (tarifa == null || tarifa.signum() <= 0)
            throw new IllegalArgumentException(cual + " tiene que ser mayor que cero: con cero o"
                    + " negativo el guardian de gasto rechaza el 100 % de las reservas y la"
                    + " derivacion del cupo por IP divide por cero. Para apagar el asistente"
                    + " esta vetsoftware.ai.proposal.bedrock.enabled, no el precio");
        return tarifa;
    }

    private static void exigirPositivos(int tokens, String cuales) {
        if (tokens <= 0)
            throw new IllegalArgumentException(cuales + " tienen que ser mayores que cero: una"
                    + " estimacion de cero tokens hace que una invocacion parezca gratis y el"
                    + " tope de gasto financiaria infinitas llamadas");
    }
}
