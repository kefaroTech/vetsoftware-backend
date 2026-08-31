package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.vetsoftware.app.shared.ai.ModelPricing;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

/**
 * Cablea {@link BedrockModelInvoker}, y solo cuando el despliegue lo enciende.
 *
 * <p>
 * ⛔ <strong>{@link #ACTIVO} es la unica condicion, y su negacion exacta es lo
 * que deja vivo a {@link ModelAccessNotEnabledInvoker}.</strong> Las dos
 * anotaciones comparten literalmente esta constante para que no puedan ser
 * falsas a la vez: si lo fueran no habria ningun bean de {@link ModelInvoker},
 * el contexto de Spring no levantaria y con el se caerian las 93 rodajas de
 * integracion del repositorio —que es exactamente el desastre que
 * {@link ModelAccessNotEnabledInvoker} existe para evitar—. Escritas por
 * separado las dos expresiones, ese acoplamiento seria invisible y se romperia
 * en la primera edicion descuidada.
 *
 * <p>
 * ⛔ <strong>El interruptor NO es {@code model-id}, y esa separacion es
 * deliberada.</strong> {@code vetsoftware.ai.proposal.model-id} siempre trae
 * valor —lo publica el contenedor y ademas tiene defecto en
 * {@code application.yml}—, asi que condicionar sobre el encenderia Bedrock en
 * local y en cada rodaja de integracion, que es justo donde no hay credenciales
 * ni region que resolver. El interruptor es
 * {@code vetsoftware.ai.proposal.bedrock.enabled}, apagado por defecto:
 * <strong>encender es un acto explicito del despliegue</strong> y apagarlo es
 * el kill switch de S10.4, que devuelve el sistema al camino determinista sin
 * tocar codigo.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression(BedrockInvokerConfig.ACTIVO)
public class BedrockInvokerConfig {

    /**
     * El despliegue encendio la invocacion real.
     *
     * <p>
     * <strong>Se compara como cadena, no se evalua como booleano.</strong> Un
     * {@code @ConditionalOnExpression("${...:false}")} deja que SpEL parsee el
     * valor crudo: un {@code yes} o un {@code 1} no son expresiones booleanas
     * validas y revientan el arranque <em>en las dos</em> condiciones a la vez.
     * Comparando contra {@code true} la funcion es total —lo que no sea
     * {@code true} es {@code false}— y el par no puede dejar el puerto sin bean
     * pase lo que pase.
     */
    static final String ACTIVO = "'${vetsoftware.ai.proposal.bedrock.enabled:false}'.trim()"
            + ".equalsIgnoreCase('true')";

    /**
     * ⛔ <strong>Es la MISMA clave que escribe la etiqueta de auditoria del turno, y
     * eso ya no es una coincidencia: es el contrato.</strong>
     * {@code AI_PROPOSAL_MODEL_ID} se publica al contenedor derivada de la misma
     * variable que compone el ARN del permiso, asi que la cadena que llega aqui es
     * literalmente aquella para la que hay permiso concedido, en la region desde la
     * que se invoca. Un test de contrato de Terraform lo ata: publicar el modelo
     * base pone el gate en rojo.
     *
     * <p>
     * ⛔ <strong>Se pasa VERBATIM.</strong> Ni se reconstruye, ni se le quita el
     * prefijo regional, ni se normaliza. Ese prefijo —{@code us.}— es lo que hace
     * que la llamada la atienda un perfil de inferencia de las regiones que el
     * texto de consentimiento del prospecto nombra al declarar la transferencia
     * internacional. Normalizarlo aqui romperia el respaldo legal de ese texto sin
     * que ninguna prueba de este repositorio se enterara.
     */
    private final String modelId;

    /**
     * Vacia deja resolver la region a la cadena por defecto del SDK
     * ({@code AWS_REGION} del contenedor), que es como esta hoy. Se declara
     * igualmente porque la region de Bedrock no tiene por que ser la del resto del
     * despliegue, y porque el perfil de inferencia y la region tienen que
     * corresponderse: el permiso se concedio para esa pareja.
     */
    private final String region;

    /**
     * Techo de tokens de salida. Por encima de los 1.000 que estima
     * {@code BedrockProposalGenerator.TOKENS_ESTIMADOS_SALIDA}: esa cifra
     * dimensiona la <em>reserva</em>, y cortar la respuesta justo en la estimacion
     * convertiria cada respuesta algo mas larga de lo normal en un
     * {@code stop_reason=max_tokens} con un JSON truncado que no parsea —una
     * llamada cobrada y tirada—.
     */
    private final int maxOutputTokens;

    /**
     * ⛔ <strong>El corte duro de la llamada, y hace falta ponerlo porque el SDK no
     * trae ninguno.</strong> El {@code apiCallTimeout} por defecto del AWS SDK v2
     * es <em>ilimitado</em>: sin esta linea, un modelo colgado retiene un hilo de
     * peticion de Tomcat hasta que se rinda el socket subyacente, en un endpoint
     * <strong>publico y anonimo</strong>. El prospecto se queda mirando una
     * pantalla en blanco en vez de recibir la propuesta determinista, que es la
     * respuesta correcta cuando el modelo no contesta.
     *
     * <p>
     * Tiene defecto aqui, asi que <strong>la propiedad se lee siempre</strong> y no
     * es una variable publicada que nadie mira. Publicar
     * {@code AI_PROPOSAL_BEDROCK_TIMEOUT} solo hace falta para moverla por entorno.
     */
    private final Duration timeout;

    /**
     * ⛔ <strong>Como se le exige al modelo que la salida sea JSON, y es una
     * propiedad justamente porque no puede ser una decision de codigo.</strong> El
     * uso de herramientas es una capacidad <em>por modelo</em>, no de
     * {@code Converse}: el dia que se cambie de familia —y esta previsto que pase—
     * el mecanismo mas fuerte puede no existir. Con esto, cambiar de modelo y bajar
     * un escalon es una linea de {@code application.yml} y no un despliegue de
     * codigo. Ver {@link StructuredOutputMode}.
     *
     * <p>
     * Se resuelve <strong>al construir la configuracion</strong>, no en la primera
     * invocacion: un valor mal escrito tumba el arranque con el valor dentro del
     * mensaje, que es donde alguien lo ve. Y solo se lee con Bedrock encendido, asi
     * que un dedazo no puede tumbar un despliegue que no usa el modelo.
     */
    private final StructuredOutputMode structuredOutput;

    /**
     * &#9940; <strong>Escribe en el log el prompt entero y la respuesta entera, y
     * por eso nace apagado.</strong> El prompt lleva el texto libre que escribio el
     * prospecto: encender esto convierte un log operativo en un almacen de datos
     * personales bajo la Ley 1581, con su propia retencion y su propia superficie
     * de acceso. Es para depurar la conversacion con el modelo —ver que se manda,
     * ver que contesta— no para dejarlo puesto.
     *
     * <p>
     * <strong>Es independiente de {@code bedrock.enabled} a proposito.</strong>
     * Aquel decide si se invoca; este, si se cuenta lo que se invoco. Atarlos
     * dejaria sin depuracion justo al encender la invocacion real, que es cuando
     * hace falta.
     *
     * <p>
     * Donde acaba lo escrito lo decide {@code logback-spring.xml} y no esta clase:
     * en local el logger {@code AI_PAYLOAD} va sin redactar a consola, y en dev y
     * prod no se declara, asi que cae en la raiz y pasa por el redactor.
     */
    private final boolean logPayloads;

    public BedrockInvokerConfig(
            @Value("${vetsoftware.ai.proposal.model-id:" + ModelPricing.MODELO_POR_DEFECTO
                    + "}") String modelId,
            @Value("${vetsoftware.ai.proposal.bedrock.region:}") String region,
            @Value("${vetsoftware.ai.proposal.bedrock.max-output-tokens:1500}") int maxOutputTokens,
            @Value("${vetsoftware.ai.proposal.bedrock.timeout:45s}") Duration timeout,
            @Value("${vetsoftware.ai.proposal.bedrock.structured-output:TOOL_STRICT}") String structuredOutput,
            @Value("${vetsoftware.ai.proposal.bedrock.log-payloads:false}") boolean logPayloads) {
        this.modelId = modelId;
        this.region = region;
        this.maxOutputTokens = maxOutputTokens;
        this.timeout = timeout;
        this.structuredOutput = StructuredOutputMode.of(structuredOutput);
        this.logPayloads = logPayloads;
    }

    /**
     * <strong>Sin reintentos.</strong> Cada intento se cobra y el guardian del
     * gasto reserva una vez por invocacion: con la politica por defecto —tres
     * intentos— una llamada podria costar el triple de lo reservado sin que el
     * contador se entere, y el «un intento, un span» de
     * {@code BedrockProposalGenerator} contaria uno donde hubo tres.
     *
     * <p>
     * Credenciales por {@code DefaultCredentialsProvider}, igual que
     * {@code S3Config}: en ECS resuelve el rol de tarea del contenedor y no hay
     * ninguna clave que guardar ni rotar. Es la mitad del motivo por el que este
     * adaptador va contra Bedrock y no contra la API directa de Anthropic.
     *
     * <p>
     * Invocacion <strong>sin streaming</strong> ({@code Converse}, no
     * {@code ConverseStream}): el puerto devuelve el cuerpo entero en
     * {@code rawJson} y no tiene costura de flujo, asi que pedir la variante con
     * streaming solo ampliaria la superficie de permisos sin que nadie la use.
     */
    @Bean
    BedrockRuntimeClient bedrockRuntimeClient() {
        BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .overrideConfiguration(ClientOverrideConfiguration.builder().apiCallTimeout(timeout)
                        .apiCallAttemptTimeout(timeout).retryStrategy(AwsRetryStrategy.doNotRetry())
                        .build());
        if (!region.isBlank())
            builder.region(Region.of(region));
        return builder.build();
    }

    /**
     * {@code @Primary} ademas de la condicion, y no por duplicar la defensa: la
     * condicion decide <em>si existe</em> y esto decide <em>quien gana</em> el dia
     * que entre un tercer invocador —un doble de contrato, un segundo proveedor—.
     * Sin el, ese dia el fallo no seria un error de arranque sino una inyeccion
     * ambigua resuelta por nombre de campo.
     *
     * <p>
     * El {@code trim()} recorta espacios de la variable de entorno, no el
     * contenido: el identificador entra intacto, prefijo regional incluido.
     */
    @Bean
    @Primary
    ModelInvoker bedrockModelInvoker(BedrockRuntimeClient cliente) {
        return new BedrockModelInvoker(cliente, modelId.trim(), maxOutputTokens, structuredOutput,
                logPayloads);
    }
}
