package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.vetsoftware.app.shared.ai.ModelPricing;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Construye la tarifa del modelo desde configuracion, que es la mitad que hacia
 * falta para poder cambiar de modelo sin romper nada mas.
 *
 * <p>
 * <strong>Las claves son de esta rodaja; el tipo es del kernel.</strong> El
 * precio lo declara el asistente —que es quien invoca y quien paga— y lo
 * consumen dos: {@code BedrockProposalGenerator}, que cobra, y
 * {@code LoginRateLimitFilter}, que reparte cupos con lo que cuesta cobrar. El
 * segundo vive en otra rodaja y no puede importar de esta, asi que lo que cruza
 * es {@link ModelPricing}, un {@code record} de {@code shared}. Cruza el valor,
 * no la feature.
 *
 * <p>
 * &#9940; <strong>Un precio invalido impide arrancar.</strong> El compact
 * constructor de {@link ModelPricing} rechaza el cero y los negativos, y esta
 * clase no lo atrapa a proposito: un {@code IllegalArgumentException} aqui
 * tumba el contexto y se ve en el primer despliegue. La alternativa —caer al
 * defecto en silencio— es como se llega a un tope que no significa lo que dice.
 */
@Configuration
public class ModelPricingConfig {

    private static final Logger log = LoggerFactory.getLogger(ModelPricingConfig.class);

    /**
     * &#9940; <strong>El acoplamiento que queda vivo, dicho en voz alta.</strong>
     * Cambiar {@code model-id} sin mover las tarifas es justo el fallo que este
     * refactor persigue, y es el unico que no se puede impedir desde aqui: son dos
     * claves independientes, y la infraestructura publica una de ellas
     * ({@code AI_PROPOSAL_MODEL_ID}) sin publicar las otras —decision deliberada,
     * para no crear una tercera fuente que descalibre el reparto de cupos—.
     *
     * <p>
     * <strong>Avisa, no revienta.</strong> Prod recibe el identificador del perfil
     * de inferencia desde Terraform, asi que un arranque fatal convertiria un
     * cambio de version del perfil en una caida de produccion. Un WARN que nombra
     * los dos identificadores basta para que el desajuste se vea; lo que no bastaba
     * era el silencio de antes.
     */
    @Bean
    ModelPricing modelPricing(
            @Value("${vetsoftware.ai.proposal.pricing.usd-per-million-input-tokens:"
                    + ModelPricing.DEFECTO_USD_POR_MILLON_ENTRADA
                    + "}") BigDecimal usdPorMillonEntrada,
            @Value("${vetsoftware.ai.proposal.pricing.usd-per-million-output-tokens:"
                    + ModelPricing.DEFECTO_USD_POR_MILLON_SALIDA
                    + "}") BigDecimal usdPorMillonSalida,
            @Value("${vetsoftware.ai.proposal.pricing.estimated-input-tokens:"
                    + ModelPricing.DEFECTO_TOKENS_ESTIMADOS_ENTRADA + "}") int tokensEntrada,
            @Value("${vetsoftware.ai.proposal.pricing.estimated-output-tokens:"
                    + ModelPricing.DEFECTO_TOKENS_ESTIMADOS_SALIDA + "}") int tokensSalida,
            @Value("${vetsoftware.ai.proposal.pricing.priced-model-id:"
                    + ModelPricing.MODELO_POR_DEFECTO + "}") String modeloTarifado,
            @Value("${vetsoftware.ai.proposal.model-id:" + ModelPricing.MODELO_POR_DEFECTO
                    + "}") String modeloInvocado) {
        ModelPricing tarifa = new ModelPricing(usdPorMillonEntrada, usdPorMillonSalida,
                tokensEntrada, tokensSalida, modeloTarifado);
        if (!tarifa.pricedModelId().equals(modeloInvocado)) {
            log.warn("Las tarifas configuradas dicen ser de '{}' pero el modelo que se invoca es"
                    + " '{}'. El tope de gasto y el cupo diario por IP se derivan de esas tarifas,"
                    + " asi que hasta que coincidan el asistente corta por un numero de llamadas"
                    + " calculado con un precio que no es el que se paga. Se ajustan en"
                    + " vetsoftware.ai.proposal.pricing.*", tarifa.pricedModelId(), modeloInvocado);
        }
        log.info("Tarifa del modelo '{}': {} USD/M de entrada y {} USD/M de salida; con {} tokens"
                + " de entrada y {} de salida estimados, una invocacion de pago cuesta {} USD",
                tarifa.pricedModelId(), tarifa.usdPerMillionInputTokens(),
                tarifa.usdPerMillionOutputTokens(), tarifa.estimatedInputTokens(),
                tarifa.estimatedOutputTokens(), tarifa.usdPerCall());
        return tarifa;
    }
}
