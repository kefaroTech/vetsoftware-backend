package com.vetsoftware.app.auth.infrastructure.filter;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.shared.ai.ModelPricing;
import com.vetsoftware.app.shared.ai.PaidInvocationMark;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Rate limiting distribuido por IP y credencial para rutas publicas sensibles.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ACCOUNT_BODY_BYTES = 16 * 1024;

    /** La ventana larga. Es un dia rodante, no de medianoche a medianoche. */
    private static final Duration UN_DIA = Duration.ofDays(1);

    private static final RouteLimit LOGIN_LIMIT = new RouteLimit("login-rl:", "/auth/login", 5,
            Duration.ofMinutes(1), "LOGIN_RATE_LIMITED",
            "Too many login attempts. Try again later.", List.of("employeeCode", "code"));
    private static final RouteLimit REGISTER_LIMIT = new RouteLimit("register-rl:", "/register", 3,
            Duration.ofHours(1), "REGISTER_RATE_LIMITED",
            "Too many registration attempts. Try again later.",
            List.of("employeeEmail", "companyIdentifier"));
    private static final RouteLimit REFRESH_LIMIT = new RouteLimit("refresh-rl:", "/auth/refresh",
            30, Duration.ofMinutes(1), "REFRESH_RATE_LIMITED",
            "Too many token refresh attempts. Try again later.", List.of("refreshToken"));
    private static final RouteLimit FORGOT_PASSWORD_LIMIT = new RouteLimit("forgot-password-rl:",
            "/auth/forgot-password", 3, Duration.ofHours(1), "FORGOT_PASSWORD_RATE_LIMITED",
            "Too many password reset attempts. Try again later.", List.of("employeeCode"));
    private static final RouteLimit DIAN_WEBHOOK_LIMIT = new RouteLimit("dian-webhook-rl:",
            "/dian/webhooks", 120, Duration.ofMinutes(1), "DIAN_WEBHOOK_RATE_LIMITED",
            "Too many webhook requests. Try again later.", List.of());
    // Mismo limite que /auth/forgot-password: las dos rutas disparan un correo, asi
    // que el recurso que hay que proteger es el mismo y el abuso tambien.
    private static final RouteLimit RECOVER_CODE_LIMIT = new RouteLimit("recover-code-rl:",
            "/auth/recover-code", 3, Duration.ofHours(1), "RECOVER_CODE_RATE_LIMITED",
            "Too many code recovery attempts. Try again later.", List.of("email"));
    // Consume un token de un solo uso: sin limite, el token se puede adivinar a
    // fuerza bruta. 10/h deja margen a que la nueva contrasena falle la politica
    // varias veces seguidas.
    private static final RouteLimit RESET_PASSWORD_LIMIT = new RouteLimit("reset-password-rl:",
            "/auth/reset-password", 10, Duration.ofHours(1), "RESET_PASSWORD_RATE_LIMITED",
            "Too many password reset attempts. Try again later.", List.of("token"));
    private static final RouteLimit VERIFY_EMAIL_LIMIT = new RouteLimit("verify-email-rl:",
            "/register/verify", 10, Duration.ofHours(1), "VERIFY_EMAIL_RATE_LIMITED",
            "Too many verification attempts. Try again later.", List.of("token"));
    // El unico POST anonimo que no es un flujo de credenciales: resuelve el
    // cuestionario del asistente de venta para un prospecto que todavia no tiene
    // cuenta. Se limita solo por IP —el cuerpo son ids de opcion y numeros, no hay
    // ninguna cuenta que contar— y con holgura, porque una sesion del asistente
    // reevalua el carrito a cada respuesta: 60/min deja pasar de sobra a una
    // persona
    // y acota lo que cuesta el endpoint, que lee el cuestionario entero dos veces
    // por
    // llamada.
    // La calculadora publica de precio: es una lectura anonima, cara en CPU pero
    // sin correo ni token que proteger, asi que
    // el limite es de higiene -evitar que alguien la use de bomba de consultas- y
    // no
    // de fuerza bruta. Sin clave de cuerpo: no hay ningun campo que identifique a
    // quien pregunta, solo su IP.
    private static final RouteLimit QUOTE_PREVIEW_LIMIT = new RouteLimit("quote-preview-rl:",
            "/quotes/preview", 60, Duration.ofMinutes(1), "QUOTE_PREVIEW_RATE_LIMITED",
            "Too many price preview requests. Try again later.", List.of());
    // Alta de superadministradores de plataforma (#360). Los cuatro POST son
    // anonimos y
    // su desenlace es una cuenta con control total sobre la plataforma, asi que
    // aqui el
    // limite no es higiene: es lo unico que separa un token de 32 bytes de un
    // ataque por
    // fuerza bruta sostenido.
    //
    // 3/h y por "email", igual que /register y /auth/forgot-password: el endpoint
    // dispara
    // un correo hacia un tercero (el aprobador), que es el recurso que hay que
    // proteger.
    private static final RouteLimit PLATFORM_ACCESS_REQUEST_LIMIT = new RouteLimit(
            "platform-access-request-rl:", "/platform/access-request", 3, Duration.ofHours(1),
            "PLATFORM_ACCESS_REQUEST_RATE_LIMITED", "Too many access requests. Try again later.",
            List.of("email"));
    // 10/h y por "token", mismo argumento que /auth/reset-password: consumen un
    // token de
    // un solo uso y hay que dejar margen a que el codigo de 6 digitos se teclee mal
    // varias veces antes de agotar los 5 intentos del propio flujo.
    //
    // El "code" NO va en accountFields a proposito: es el secreto que este limite
    // existe
    // para proteger, y contarlo por bucket lo escribiria como parte de una clave de
    // Redis. El token ya identifica la solicitud, asi que el bucket por cuenta no
    // pierde
    // precision.
    private static final RouteLimit PLATFORM_ACCESS_APPROVE_LIMIT = new RouteLimit(
            "platform-access-approve-rl:", "/platform/access-request/approve", 10,
            Duration.ofHours(1), "PLATFORM_ACCESS_APPROVE_RATE_LIMITED",
            "Too many approval attempts. Try again later.", List.of("token"));
    private static final RouteLimit PLATFORM_ACCESS_REJECT_LIMIT = new RouteLimit(
            "platform-access-reject-rl:", "/platform/access-request/reject", 10,
            Duration.ofHours(1), "PLATFORM_ACCESS_REJECT_RATE_LIMITED",
            "Too many rejection attempts. Try again later.", List.of("token"));
    private static final RouteLimit PLATFORM_INVITATION_ACCEPT_LIMIT = new RouteLimit(
            "platform-invitation-accept-rl:", "/platform/invitation/accept", 10,
            Duration.ofHours(1), "PLATFORM_INVITATION_ACCEPT_RATE_LIMITED",
            "Too many invitation attempts. Try again later.", List.of("token"));

    // El asistente comercial (propuesta generada por IA). Los cuatro endpoints son
    // anonimos y DOS de ellos cuestan dinero por peticion: el POST inicial y el
    // POST de refinamiento invocan los dos un modelo de pago, con la misma reserva
    // y el mismo coste. Copiar los 60/min de otro endpoint publico serian 86.400
    // invocaciones de pago al dia por IP.
    //
    // La ventana horaria (5/h y 10/h) corta la rafaga y se elige a mano. La DIARIA
    // -que corta el goteo, la forma barata de vaciar el presupuesto sin disparar
    // ninguna alarma de rafaga- ya NO se elige a mano: se DERIVA del tope de gasto.
    // El argumento entero esta en limitesDePago(...).
    //
    // Por eso las dos rutas de pago son campos de instancia y no constantes (mas
    // abajo, con el resto del estado del filtro): su cupo diario depende de
    // configuracion.
    //
    // ⛔ Un PUT anonimo, es decir una escritura publica sin sesion. La invariante
    // toda_ruta_publica_post_esta_limitada solo recorre los POST, asi que este
    // limite no lo exige ningun gate: si desaparece, nada se pone rojo. Se declara
    // igualmente porque no llamar al modelo no lo hace gratis -reescribe el carrito
    // entero y cierra el grafo de dependencias en cada llamada-.
    private static final RouteLimit AI_PROPOSAL_LINES_LIMIT = new RouteLimit(
            "ai-proposal-lines-rl:", "/assistant/proposal/lines", 30, Duration.ofHours(1),
            "AI_PROPOSAL_LINES_RATE_LIMITED", "Too many proposal edits. Try again later.",
            List.of());
    // Ídem para el GET: sirve la propuesta entera a quien tenga el token, y sin
    // limite es una lectura repetible sin coste para quien la haga.
    private static final RouteLimit AI_PROPOSAL_READ_LIMIT = new RouteLimit("ai-proposal-read-rl:",
            "/assistant/proposal", 60, Duration.ofHours(1), "AI_PROPOSAL_READ_RATE_LIMITED",
            "Too many proposal reads. Try again later.", List.of());

    /**
     * Campos cuyo valor es un secreto opaco y NO se normaliza a minusculas: dos
     * tokens que solo difieran en mayusculas son tokens distintos, y meterlos en el
     * mismo bucket los cuenta como uno.
     */
    private static final Set<String> OPAQUE_FIELDS = Set.of("refreshToken", "token");

    /**
     * Llamadas de pago que consume una sesion completa del asistente: la inicial
     * mas los tres refinamientos. Es el mismo numero que
     * {@code ProposalReader.MAX_TURNOS_DE_MODELO}, que vive en otra rodaja y no se
     * puede importar desde aqui; {@code LoginRateLimitFilterTest} ata los dos.
     */
    static final int LLAMADAS_DE_PAGO_POR_SESION = 4;

    /**
     * Cuantos origenes distintos tienen que confabularse para vaciar el presupuesto
     * de un dia. Con 1, una sola IP se lo lleva entero -que es lo que pasaba-; con
     * un numero muy alto, el cupo por IP cae por debajo de una sesion y el
     * asistente deja de servir para lo que existe.
     */
    static final int ORIGENES_PARA_VACIAR_EL_DIA = 4;

    /**
     * Cuanto por encima del presupuesto del dia esta el cubo global de peticiones.
     * No sustituye al tope de gasto -aquel cuenta dinero, este cuenta peticiones- y
     * esta deliberadamente por encima de el: si mordiera antes, seria el limite
     * efectivo y el control del dinero no llegaria a ejercerse nunca.
     */
    static final int FACTOR_DEL_CUBO_GLOBAL = 25;

    /** El cupo diario por correo del POST inicial: un prospecto, tres intentos. */
    static final int CUPO_DIARIO_POR_CORREO = 3;

    /**
     * &#9940; <b>El defecto del tope de gasto, escrito aqui para poder atarlo.</b>
     * Tiene que ser el mismo que declara {@code ValkeyDailySpendGuard}; el test lo
     * comprueba. Si los dos se separan, este filtro calibra su limite contra un
     * presupuesto que no es el que se aplica.
     */
    static final String DEFECTO_TOPE_DE_GASTO_DIARIO_USD = "0.33";

    /**
     * &#9940; <b>La clave del cubo diario global NO lleva el prefijo de la
     * ruta.</b> Con {@code routeLimit.keyPrefix() + "day:global"}, el cubo llamado
     * "global" era en realidad uno por ruta: la propuesta inicial y el
     * refinamiento, que gastan del mismo presupuesto, contaban por separado y el
     * techo efectivo de la plataforma era el doble del declarado. El presupuesto es
     * uno, asi que el contador tiene que ser uno.
     */
    static final String CLAVE_DIARIA_GLOBAL_DE_PAGO = "ai-paid-rl:day:global";

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final LettuceBasedProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    private final RouteLimit aiProposalLimit;

    private final RouteLimit aiProposalRefineLimit;

    /** Cuantas invocaciones de pago financia el tope de gasto del dia. */
    private final int llamadasQueFinanciaElTope;

    /**
     * &#9940; <b>El coste por llamada NO se declara aqui: se pide.</b> Hasta hoy
     * era un literal {@code "0.0176"} en esta clase, copiado de la constante de
     * {@code BedrockProposalGenerator} y atado a ella por un test. Un literal atado
     * por un test sigue siendo una segunda fuente —solo esta vigilada—, y sobre
     * todo obligaba a que el precio fuese una constante compilada: el dia que se
     * cambiara de modelo, este filtro repartiria cupos contra un precio que ya no
     * existe. {@link ModelPricing} es ahora la unica fuente, y se la piden los dos
     * consumidores: el que cobra y el que reparte.
     */
    public LoginRateLimitFilter(LettuceBasedProxyManager<String> loginRateLimitProxyManager,
            ObjectMapper objectMapper, AuditLogger auditLogger,
            @Value("${vetsoftware.ai.proposal.daily-spend-cap-usd:"
                    + DEFECTO_TOPE_DE_GASTO_DIARIO_USD + "}") BigDecimal topeDeGastoDiarioUsd,
            ModelPricing modelPricing) {
        this.proxyManager = loginRateLimitProxyManager;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
        this.llamadasQueFinanciaElTope = llamadasQueFinanciaElTope(topeDeGastoDiarioUsd,
                modelPricing.usdPerCall());
        LimitesDePago limites = limitesDePago(this.llamadasQueFinanciaElTope);
        this.aiProposalLimit = new RouteLimit("ai-proposal-rl:", "/assistant/proposal", 5,
                Duration.ofHours(1), "AI_PROPOSAL_RATE_LIMITED",
                "Too many proposal requests. Try again later.", List.of("email"),
                new DailyLimit(limites.porIpInicial(), CUPO_DIARIO_POR_CORREO, limites.global()));
        // Sin clave de cuerpo, y a proposito: el unico campo que identifica aqui es
        // el token, y contarlo por bucket lo escribiria como parte de una clave de
        // Redis. El token ya esta acotado por su propio tope de tres refinamientos.
        //
        // ⛔ Lo que SI le faltaba es el cubo diario, y su ausencia no se veia: sin
        // DailyLimit, RouteLimit.daily queda a null, dailyGlobal() devuelve 0 y
        // consumirDiario cortocircuita a true. Es decir, la ruta que paga modelo
        // exactamente igual que la inicial no contaba NADA contra el presupuesto
        // del dia, y el codigo que lo dejaba pasar se lee como una guarda correcta.
        this.aiProposalRefineLimit = new RouteLimit("ai-proposal-refine-rl:",
                "/assistant/proposal/refine", 10, Duration.ofHours(1),
                "AI_PROPOSAL_REFINE_RATE_LIMITED", "Too many refinement requests. Try again later.",
                List.of(), new DailyLimit(limites.porIpRefinamiento(), 0, limites.global()));
    }

    /**
     * &#9940; <b>El limite de peticiones se DERIVA del limite de dinero, y esa es
     * toda la correccion.</b> Los dos numeros se elegian por separado y quedaron
     * calibrados al reves: 0,0176 USD por llamada contra un tope de 0,33 USD son
     * <b>dieciocho</b> invocaciones al dia para toda la plataforma, y el cupo por
     * IP declaraba <b>veinte</b>. Una sola IP, sin agotar su propio limite, vaciaba
     * el presupuesto de todos los prospectos del dia; y los dos numeros se leian
     * bien por separado, que es lo que hacia invisible el defecto.
     *
     * <p>
     * <b>Lo que se reparte son llamadas de pago, no peticiones.</b> Las dos rutas
     * que invocan al modelo -la propuesta inicial y el refinamiento- salen del
     * mismo presupuesto, asi que sus cupos se reparten juntos y la suma de los dos
     * <b>nunca supera lo que el tope financia</b>. Esa es la invariante y es lo que
     * comprueba el test: no "el numero es 20" -eso volveria a fijar el sintoma-
     * sino la relacion entre los dos.
     *
     * <p>
     * <b>El suelo de una sesion.</b> Un tope tan bajo que no quepa ni una sesion
     * completa por origen es una configuracion de juguete: quien corta ahi es el
     * guardian de gasto, que es fail-closed, no este filtro. Se deja el minimo util
     * y se avisa, en vez de dejar cupos a cero — que en {@link #consumirDiario}
     * significan «sin limite» y serian justo lo contrario de lo que se quiere.
     */
    private LimitesDePago limitesDePago(int financiadas) {
        int porIp = financiadas / ORIGENES_PARA_VACIAR_EL_DIA;
        if (porIp < LLAMADAS_DE_PAGO_POR_SESION) {
            log.warn("El tope de gasto diario del asistente solo financia {} invocaciones, menos"
                    + " de una sesion para cada uno de los {} origenes que este limite supone."
                    + " Se deja el minimo de {} llamadas por IP; con este tope quien corta de"
                    + " verdad es el guardian de gasto, no el limite de peticiones", financiadas,
                    ORIGENES_PARA_VACIAR_EL_DIA, LLAMADAS_DE_PAGO_POR_SESION);
            porIp = LLAMADAS_DE_PAGO_POR_SESION;
        }
        int porIpInicial = porIp / LLAMADAS_DE_PAGO_POR_SESION;
        // El global se calcula sobre el MAYOR de los dos, y no sobre `financiadas` a
        // secas, por el mismo motivo que el suelo de arriba: con un tope de cero
        // -clave mal escrita, variable de entorno vacia- `financiadas * 25` seria
        // cero, y cero en consumirDiario significa «sin limite». El cubo que existe
        // para ser el techo de la plataforma se apagaria justo cuando no hay
        // presupuesto, que es cuando mas falta hace.
        return new LimitesDePago(porIpInicial, porIp - porIpInicial,
                Math.max(financiadas, porIp) * FACTOR_DEL_CUBO_GLOBAL);
    }

    private static int llamadasQueFinanciaElTope(BigDecimal tope, BigDecimal porLlamada) {
        if (tope == null || porLlamada == null || porLlamada.signum() <= 0 || tope.signum() <= 0)
            return 0;
        return tope.divide(porLlamada, 0, RoundingMode.DOWN).intValue();
    }

    /** El reparto del presupuesto del dia entre las dos rutas que pagan. */
    private record LimitesDePago(int porIpInicial, int porIpRefinamiento, int global) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return routeLimit(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        RouteLimit routeLimit = routeLimit(request);
        if (routeLimit == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!tryConsume(routeLimit, ipKey(request, routeLimit))) {
            writeRateLimited(response, routeLimit, routeLimit.window());
            return;
        }
        // La ventana diaria por IP va inmediatamente detras de la horaria: las dos
        // cuentan la misma peticion y ninguna sustituye a la otra.
        if (!consumirDiario(routeLimit, routeLimit.dailyPerIp(), ipKey(request, routeLimit))) {
            writeRateLimited(response, routeLimit, UN_DIA);
            return;
        }

        HttpServletRequest requestForChain = request;
        for (String accountKey : pathAccountKeys(request, routeLimit)) {
            if (!tryConsume(routeLimit, accountKey)) {
                writeRateLimited(response, routeLimit, routeLimit.window());
                return;
            }
        }

        // Identificable = el cuerpo trae los campos con los que esta ruta cuenta por
        // cuenta. Una ruta que no declara ninguno lo es por vacuidad. Ver
        // cobraPresupuestoDiario.
        boolean identificable = true;
        // Se guardan porque hay que poder DESHACER lo que se consumio: el cupo
        // diario por cuenta se devuelve sobre la misma clave que lo gasto, y
        // recalcularla despues de la cadena significaria volver a leer un cuerpo
        // que ya se consumio.
        List<String> cuentasQueGastaronCupoDiario = List.of();
        if (leeElCuerpo(routeLimit)) {
            byte[] body = request.getInputStream().readNBytes(MAX_ACCOUNT_BODY_BYTES + 1);
            if (body.length > MAX_ACCOUNT_BODY_BYTES) {
                writeProblem(response, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_BODY_TOO_LARGE",
                        "Request body is too large for this endpoint.");
                return;
            }

            requestForChain = new CachedBodyRequest(request, body);
            List<String> accountKeys = bodyAccountKeys(body, routeLimit);
            identificable = routeLimit.accountFields().isEmpty() || !accountKeys.isEmpty();
            cuentasQueGastaronCupoDiario = accountKeys;
            for (String accountKey : accountKeys) {
                if (!tryConsume(routeLimit, accountKey)) {
                    writeRateLimited(response, routeLimit, routeLimit.window());
                    return;
                }
                if (!consumirDiario(routeLimit, routeLimit.dailyPerAccount(), accountKey)) {
                    writeRateLimited(response, routeLimit, UN_DIA);
                    return;
                }
            }
        }

        // El cubo global va EL ULTIMO, y ese orden es la decision: solo consume
        // quien ya paso sus propios limites, asi que un abusador no puede agotar el
        // cupo de la plataforma con peticiones que de todas formas iban a rechazarse.
        boolean cobraPresupuesto = cobraPresupuestoDiario(routeLimit, identificable);
        if (cobraPresupuesto
                && !tryConsume(CLAVE_DIARIA_GLOBAL_DE_PAGO, routeLimit.dailyGlobal(), UN_DIA)) {
            writeRateLimited(response, routeLimit, UN_DIA);
            return;
        }

        chain.doFilter(requestForChain, response);
        // &#9940; Aqui, y solo aqui, este filtro cumple lo que su javadoc afirma
        // desde el primer dia: que lo que reparte son llamadas de pago y no
        // peticiones. Antes de la cadena eso era indecidible -no se sabe si habra
        // invocacion hasta que la hay-, asi que el cupo se cobraba siempre y no se
        // devolvia nunca. Al volver, el desenlace ya se conoce.
        if (PaidInvocationMark.constaQueNoHuboInvocacion(request)) {
            devolverCuposDelDia(routeLimit, request, cuentasQueGastaronCupoDiario,
                    cobraPresupuesto);
            return;
        }
        // Llegar aqui con cobraPresupuesto significa que el token se consumio: la
        // rama de arriba se lleva el caso contrario.
        if (cobraPresupuesto)
            devolverPresupuestoSiNoPasoLaValidacion(routeLimit, response);
    }

    /**
     * &#9940; <b>Se devuelven los cupos DIARIOS y ninguno mas.</b> La ventana
     * horaria de la ruta -5/h en la propuesta inicial, 10/h en el refinamiento- es
     * antiinundacion: existe para que nadie haga trabajar al servidor a ritmo de
     * rafaga, y ese trabajo lo hubo igual -se leyo el catalogo, se abrio un turno,
     * se escribio en la base-. Un cubo antiinundacion que solo contara lo que acabo
     * costando dinero no protegeria de nada, porque la forma barata de tumbar esto
     * es justamente la peticion que se degrada en milisegundos. Los diarios son
     * otra cosa: reparten el presupuesto de un dia, se derivan del tope de gasto y
     * su unidad es la invocacion de pago.
     *
     * <p>
     * <b>Se devuelve sobre las mismas claves que se gastaron</b>, construidas por
     * {@link #claveDiaria} en los dos sentidos. Recalcularlas por separado es la
     * forma en que este tipo de arreglo se rompe en silencio: se abona un cubo que
     * nadie consumio y el que si se gasto se queda bajo.
     *
     * <p>
     * <b>El global solo si de verdad se cobro.</b> Una peticion no identificable
     * -sin el campo de cuenta que la ruta declara- ni siquiera lo toca, y abonarle
     * un token subiria el techo de la plataforma por encima del presupuesto.
     */
    private void devolverCuposDelDia(RouteLimit routeLimit, HttpServletRequest request,
            List<String> cuentas, boolean cobroElPresupuesto) {
        if (routeLimit.dailyPerIp() > 0)
            devolverUnToken(claveDiaria(routeLimit, ipKey(request, routeLimit)),
                    routeLimit.dailyPerIp(), "el cupo diario por IP");
        if (routeLimit.dailyPerAccount() > 0)
            for (String cuenta : cuentas)
                devolverUnToken(claveDiaria(routeLimit, cuenta), routeLimit.dailyPerAccount(),
                        "el cupo diario por cuenta");
        if (cobroElPresupuesto)
            devolverUnToken(CLAVE_DIARIA_GLOBAL_DE_PAGO, routeLimit.dailyGlobal(),
                    "el presupuesto diario de la plataforma");
    }

    /**
     * <b>Y nunca lanza.</b> Un fallo de Valkey devolviendo un token no puede
     * convertir en un 500 la respuesta que el usuario ya tiene: la cadena termino y
     * el estado esta escrito. El precio de tragarselo es que el cupo del dia queda
     * mas bajo de lo real, que es el lado seguro del error y por eso se avisa.
     */
    private void devolverUnToken(String clave, int cupo, String cual) {
        try {
            proxyManager.builder().build(clave, () -> bucketConfiguration(cupo, UN_DIA))
                    .addTokens(1);
        } catch (RuntimeException fallo) {
            log.warn(
                    "No se pudo devolver {} de una peticion que no invoco al modelo; queda mas"
                            + " bajo de lo real hasta que rote el dia: {}",
                    cual, fallo.getMessage());
        }
    }

    /**
     * &#9940; <b>Tambien se lee el cuerpo de la ruta que no cuenta por cuenta.</b>
     * La lectura acotada a {@value #MAX_ACCOUNT_BODY_BYTES} bytes era un efecto
     * colateral de necesitar el JSON para sacar la clave de cuenta, asi que
     * {@code PUT /assistant/proposal/lines} -una escritura publica y anonima, sin
     * ningun {@code accountField}- se quedaba <b>sin ninguna cota de tamano</b>: el
     * unico techo era el del contenedor. Aqui se le pone la misma red que a las
     * demas.
     *
     * <p>
     * El webhook de la DIAN queda fuera a proposito: su cuerpo lo escribe un
     * tercero de confianza y no hay motivo para creer que cabe en 16 KB. Cortarlo
     * convertiria un documento grande en un 413 en produccion, que es exactamente
     * el tipo de regresion que se paga a las tres de la manana.
     */
    private static boolean leeElCuerpo(RouteLimit routeLimit) {
        return !routeLimit.accountFields().isEmpty() || routeLimit == AI_PROPOSAL_LINES_LIMIT;
    }

    /**
     * Si esta peticion tiene que pagar del presupuesto del dia de toda la
     * plataforma.
     *
     * <p>
     * &#9940; <b>Una peticion que no puede pasar {@code @Valid} no gasta
     * presupuesto.</b> Este filtro corre <em>antes</em> que el binder, asi que un
     * {@code POST /assistant/proposal} sin {@code email} consumia el cubo global y
     * solo despues se rechazaba con un 400: peticiones invalidas, gratis para quien
     * las manda, quemando el cupo de la plataforma. Si la ruta declara campos de
     * cuenta y el cuerpo no trae ninguno, la peticion no es identificable y no
     * puede ser valida, asi que ni se le cobra.
     */
    private static boolean cobraPresupuestoDiario(RouteLimit routeLimit, boolean identificable) {
        return routeLimit.dailyGlobal() > 0 && identificable;
    }

    /**
     * &#9940; <b>La red que cubre lo que la guarda de identificabilidad no ve.</b>
     * Aquella solo sabe de los campos que la ruta cuenta por cuenta; el
     * refinamiento no declara ninguno a proposito -su unico identificador es el
     * token, y meterlo en una clave de Redis seria publicarlo-, asi que un cuerpo
     * con el token mal formado o el texto demasiado corto pasaba igual y se cobraba
     * igual. Un 400 aguas abajo significa que el cuerpo ni siquiera llego al caso
     * de uso: no hubo llamada al modelo, no hubo gasto, y el token vuelve al cubo.
     *
     * <p>
     * <b>Solo 400.</b> Un 404, un 409 o un 500 son peticiones que si llegaron a
     * ejecutarse -y en las rutas de pago, posiblemente despues de invocar al
     * modelo-, asi que devolver ahi el token seria regalar el gasto que si ocurrio.
     *
     * <p>
     * <b>Y nunca lanza.</b> Un fallo de Valkey devolviendo un token no puede
     * convertir el 400 del usuario en un 500.
     */
    private void devolverPresupuestoSiNoPasoLaValidacion(RouteLimit routeLimit,
            HttpServletResponse response) {
        if (response.getStatus() != HttpStatus.BAD_REQUEST.value())
            return;
        devolverUnToken(CLAVE_DIARIA_GLOBAL_DE_PAGO, routeLimit.dailyGlobal(),
                "el presupuesto diario de la plataforma");
    }

    private RouteLimit routeLimit(HttpServletRequest request) {
        String uri = request.getServletPath();
        // Las dos ramas que NO son POST, y van antes del filtro por metodo porque
        // ese filtro es justo lo que las dejaba fuera. El PUT del asistente es una
        // escritura publica anonima y su GET sirve la propuesta entera: los dos
        // necesitan cupo propio, y ninguna invariante automatica lo exige -la que
        // hay solo recorre los POST publicos-.
        if ("PUT".equalsIgnoreCase(request.getMethod()))
            return uri.equals(AI_PROPOSAL_LINES_LIMIT.path()) ? AI_PROPOSAL_LINES_LIMIT : null;
        // equals y no startsWith: /assistant/proposal es tambien el path del POST
        // inicial, y con startsWith el GET caeria en /refine y en /lines.
        if ("GET".equalsIgnoreCase(request.getMethod()))
            return uri.equals(AI_PROPOSAL_READ_LIMIT.path()) ? AI_PROPOSAL_READ_LIMIT : null;
        if (!"POST".equalsIgnoreCase(request.getMethod()))
            return null;
        if (uri.equals(REFRESH_LIMIT.path()))
            return REFRESH_LIMIT;
        if (uri.startsWith(LOGIN_LIMIT.path() + "/"))
            return LOGIN_LIMIT;
        if (uri.equals(REGISTER_LIMIT.path()))
            return REGISTER_LIMIT;
        if (uri.equals(FORGOT_PASSWORD_LIMIT.path()))
            return FORGOT_PASSWORD_LIMIT;
        if (uri.equals(RECOVER_CODE_LIMIT.path()))
            return RECOVER_CODE_LIMIT;
        // equals, no startsWith: /auth/reset-password/validate es otra ruta (y es GET,
        // que aqui ya se descarto arriba).
        if (uri.equals(RESET_PASSWORD_LIMIT.path()))
            return RESET_PASSWORD_LIMIT;
        if (uri.equals(VERIFY_EMAIL_LIMIT.path()))
            return VERIFY_EMAIL_LIMIT;
        // equals y no startsWith: /quotes es el prefijo del embudo comercial entero,
        // que
        // es territorio autenticado. Solo /quotes/preview es publico.
        if (uri.equals(QUOTE_PREVIEW_LIMIT.path()))
            return QUOTE_PREVIEW_LIMIT;
        // equals y no startsWith: /platform/access-request es el prefijo textual de
        // /approve, de /reject y del GET /validate. Con startsWith los tres caerian en
        // el
        // bucket de la solicitud (3/h), y ademas /approve consumiria el cupo que
        // protege
        // al endpoint que manda correo.
        if (uri.equals(PLATFORM_ACCESS_REQUEST_LIMIT.path()))
            return PLATFORM_ACCESS_REQUEST_LIMIT;
        if (uri.equals(PLATFORM_ACCESS_APPROVE_LIMIT.path()))
            return PLATFORM_ACCESS_APPROVE_LIMIT;
        if (uri.equals(PLATFORM_ACCESS_REJECT_LIMIT.path()))
            return PLATFORM_ACCESS_REJECT_LIMIT;
        if (uri.equals(PLATFORM_INVITATION_ACCEPT_LIMIT.path()))
            return PLATFORM_INVITATION_ACCEPT_LIMIT;
        if (uri.startsWith(DIAN_WEBHOOK_LIMIT.path() + "/"))
            return DIAN_WEBHOOK_LIMIT;
        // equals en los dos: /assistant/proposal es el prefijo textual de /refine y
        // de /lines. Con startsWith, el refinamiento consumiria el cupo de 5/h que
        // protege la invocacion inicial de pago, y lo agotaria desde fuera.
        if (uri.equals(aiProposalRefineLimit.path()))
            return aiProposalRefineLimit;
        if (uri.equals(aiProposalLimit.path()))
            return aiProposalLimit;
        return null;
    }

    /**
     * Solo para {@code LoginRateLimitFilterTest}: la invariante que hay que
     * comprobar no es "el numero es N" sino que la suma de lo que las rutas de pago
     * permiten a una IP no supere lo que el tope de gasto financia.
     */
    int cupoDiarioPorIpDeLasRutasDePago() {
        return aiProposalLimit.dailyPerIp() + aiProposalRefineLimit.dailyPerIp();
    }

    int llamadasDePagoQueFinanciaElTope() {
        return llamadasQueFinanciaElTope;
    }

    /**
     * Solo para el test. Las dos rutas de pago comparten cubo, asi que tienen que
     * declarar el mismo numero; el metodo lo comprueba en vez de devolver el de una
     * de ellas y confiar.
     */
    int cupoDiarioGlobal() {
        if (aiProposalLimit.dailyGlobal() != aiProposalRefineLimit.dailyGlobal())
            throw new IllegalStateException("las dos rutas de pago comparten cubo global y tienen"
                    + " que declarar el mismo cupo; bucket4j configuraria el cubo con el de la"
                    + " primera peticion que llegara y el otro dejaria de existir en silencio");
        return aiProposalLimit.dailyGlobal();
    }

    private static BucketConfiguration bucketConfiguration(int maxAttempts, Duration window) {
        return BucketConfiguration.builder()
                .addLimit(
                        limit -> limit.capacity(maxAttempts).refillIntervally(maxAttempts, window))
                .build();
    }

    private boolean tryConsume(RouteLimit routeLimit, String key) {
        return tryConsume(key, routeLimit.maxAttempts(), routeLimit.window());
    }

    private boolean tryConsume(String key, int maxAttempts, Duration window) {
        BucketProxy bucket = proxyManager.builder().build(key,
                () -> bucketConfiguration(maxAttempts, window));
        return bucket.tryConsume(1);
    }

    /**
     * Un cubo de ventana diaria, o via libre si esta ruta no declara ninguna.
     *
     * <p>
     * <b>La clave lleva su propio prefijo {@code day:}</b> y por tanto es distinta
     * de la del cubo horario. Reutilizarla haria que las dos ventanas compartieran
     * cubo: la que se configurara primero ganaria y la otra dejaria de existir, en
     * silencio y de forma dependiente del orden de llegada de las peticiones.
     */
    private boolean consumirDiario(RouteLimit routeLimit, int cupo, String claveBase) {
        if (cupo <= 0)
            return true;
        return tryConsume(claveDiaria(routeLimit, claveBase), cupo, UN_DIA);
    }

    /**
     * La clave del cubo diario de una ruta. Existe como metodo, y no como
     * concatenacion repetida, porque ahora hay dos sentidos -consumir y devolver- y
     * una diferencia de un caracter entre ellos no rompe nada de forma visible:
     * abona un cubo que no existia y deja sin devolver el que si se gasto.
     */
    private static String claveDiaria(RouteLimit routeLimit, String claveBase) {
        return routeLimit.keyPrefix() + "day:" + claveBase;
    }

    /**
     * {@code getRemoteAddr()} devuelve aqui la IP <b>del cliente</b>, no la del
     * balanceador: {@code server.forward-headers-strategy=native} activa el
     * {@code RemoteIpValve} de Tomcat, que reescribe la IP remota desde
     * {@code X-Forwarded-For} confiando <b>solo</b> en proxies de rangos privados.
     * Un cliente externo no puede falsear la cabecera para escaparse del limite, y
     * detras del balanceador el limite no se aplica a todos los clientes a la vez.
     *
     * <p>
     * Por eso aqui NO se parsea {@code X-Forwarded-For} a mano: hacerlo duplicaria
     * —y casi con seguridad debilitaria— la logica de proxies de confianza que ya
     * aplica el contenedor. {@code ServerForwardHeadersConfigTest} fija esa
     * configuracion para que no desaparezca en silencio.
     */
    private static String ipKey(HttpServletRequest request, RouteLimit routeLimit) {
        return routeLimit.keyPrefix() + "ip:" + request.getRemoteAddr();
    }

    private static List<String> pathAccountKeys(HttpServletRequest request, RouteLimit routeLimit) {
        if (routeLimit != DIAN_WEBHOOK_LIMIT)
            return List.of();
        String provider = request.getServletPath().substring(routeLimit.path().length() + 1).trim();
        if (provider.isEmpty() || provider.contains("/"))
            return List.of();
        return List.of(accountKey(routeLimit, "provider", provider));
    }

    private List<String> bodyAccountKeys(byte[] body, RouteLimit routeLimit) {
        if (body.length == 0)
            return List.of();
        try {
            JsonNode root = objectMapper.readTree(body);
            List<String> keys = new ArrayList<>(routeLimit.accountFields().size());
            for (String field : routeLimit.accountFields()) {
                JsonNode valueNode = root.get(field);
                if (valueNode == null || !valueNode.isString())
                    continue;
                String value = valueNode.asText().trim();
                if (value.isEmpty())
                    continue;
                if (!OPAQUE_FIELDS.contains(field))
                    value = value.toLowerCase(Locale.ROOT);
                keys.add(accountKey(routeLimit, field, value));
            }
            return keys;
        } catch (RuntimeException ignored) {
            // El controller conserva la responsabilidad de reportar JSON invalido; el
            // limite por IP ya se
            // consumio.
            return List.of();
        }
    }

    private static String accountKey(RouteLimit routeLimit, String field, String value) {
        return routeLimit.keyPrefix() + "account:" + sha256(field + ':' + value);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * <b>El {@code Retry-After} lleva la ventana QUE RECHAZO</b>, no siempre la
     * horaria. Un rechazo de la ventana diaria que anunciara una hora invitaria a
     * reintentar cincuenta veces y a agotar el cubo horario encima del diario,
     * castigando justo al usuario legitimo que hace caso a la cabecera.
     */
    private void writeRateLimited(HttpServletResponse response, RouteLimit routeLimit,
            Duration ventana) throws IOException {
        auditLogger.rateLimited(routeLimit.code());
        response.setHeader("Retry-After", String.valueOf(ventana.toSeconds()));
        writeProblem(response, HttpStatus.TOO_MANY_REQUESTS, routeLimit.code(),
                routeLimit.detail());
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String code,
            String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("type", "about:blank", "title", status.getReasonPhrase(), "status",
                        status.value(), "code", code, "detail", detail));
    }

    /**
     * &#9940; <b>La segunda ventana de una ruta.</b> {@link RouteLimit} nacio con
     * una sola -una capacidad y un periodo- y eso basta mientras el unico riesgo
     * sea la rafaga. No basta cuando cada peticion cuesta dinero: ahi el goteo
     * paciente es tan caro como la rafaga y ninguna ventana horaria lo ve.
     *
     * <p>
     * Los tres cupos son de poblaciones distintas y por eso no se pueden fundir en
     * uno: <b>{@code perIp}</b> acota a un origen, <b>{@code perAccount}</b> acota
     * al identificador del cuerpo y <b>{@code global}</b> es el techo de toda la
     * plataforma para el caso que los otros dos no pueden ver: muchos origenes
     * distintos, cada uno dentro de su cupo.
     *
     * <p>
     * &#9940; <b>{@code perAccount} NO detiene una botnet, y este javadoc afirmaba
     * que si.</b> La clave sale de una cadena del cuerpo <em>que elige quien
     * llama</em> y que nadie ha verificado: cambiar un caracter del correo da un
     * cubo nuevo, y omitir el campo no consumia ningun cubo de cuenta en absoluto.
     * Contra un atacante no vale nada. Lo que si hace, y por lo que se conserva, es
     * <b>frenar el doble clic y el reintento honrado</b>: el mismo prospecto
     * pulsando tres veces «generar» comparte cubo consigo mismo, que es un caso
     * real y frecuente. Quien de verdad tiene que parar el abuso distribuido es
     * {@code global} —el presupuesto del dia—, no esto.
     *
     * <p>
     * <b>Cero desactiva el cupo</b>, que es lo que declara toda ruta que no pasa un
     * {@code DailyLimit}: una ventana diaria sobre {@code /auth/login} no protege
     * de nada que la horaria no proteja ya, y multiplicaria por dos las claves en
     * Valkey.
     *
     * <p>
     * <b>El global ya no es un literal</b>: sale de multiplicar por
     * {@link #FACTOR_DEL_CUBO_GLOBAL} lo que el tope de gasto financia, asi que se
     * mueve solo cuando se mueve el presupuesto. Empieza a morder cuando muchos
     * origenes distintos agotan su cuota el mismo dia, que es exactamente la forma
     * de una botnet y exactamente lo que los otros dos cubos no distinguen de un
     * buen dia de trafico. No sustituye al tope de gasto -aquel cuenta dinero, este
     * cuenta peticiones- y esta deliberadamente por encima de el: si mordiera
     * antes, seria el limite efectivo y el control del dinero no llegaria a
     * ejercerse nunca.
     *
     * <p>
     * <b>Su cubo lo comparten TODAS las rutas que pagan</b>, con una unica clave
     * fuera del prefijo de ruta ({@link #CLAVE_DIARIA_GLOBAL_DE_PAGO}). Por eso las
     * dos tienen que declarar el mismo {@code global}: si declararan numeros
     * distintos, bucket4j configuraria el cubo con el de la primera peticion que
     * llegara y el otro dejaria de existir en silencio.
     */
    private record DailyLimit(int perIp, int perAccount, int global) {
    }

    private record RouteLimit(String keyPrefix, String path, int maxAttempts, Duration window,
            String code, String detail, List<String> accountFields, DailyLimit daily) {

        /** Las rutas que solo necesitan la ventana corta, que son casi todas. */
        RouteLimit(String keyPrefix, String path, int maxAttempts, Duration window, String code,
                String detail, List<String> accountFields) {
            this(keyPrefix, path, maxAttempts, window, code, detail, accountFields, null);
        }

        int dailyPerIp() {
            return daily == null ? 0 : daily.perIp();
        }

        int dailyPerAccount() {
            return daily == null ? 0 : daily.perAccount();
        }

        int dailyGlobal() {
            return daily == null ? 0 : daily.global();
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // La copia en memoria siempre esta disponible de forma sincrona.
                }

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
