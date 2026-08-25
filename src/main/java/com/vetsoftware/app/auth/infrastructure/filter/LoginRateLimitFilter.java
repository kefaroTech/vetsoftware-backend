package com.vetsoftware.app.auth.infrastructure.filter;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
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
    private static final RouteLimit CONFIGURATOR_RESOLVE_LIMIT = new RouteLimit(
            "configurator-resolve-rl:", "/configurator/resolve", 60, Duration.ofMinutes(1),
            "CONFIGURATOR_RESOLVE_RATE_LIMITED", "Too many configurator requests. Try again later.",
            List.of());
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

    /**
     * Campos cuyo valor es un secreto opaco y NO se normaliza a minusculas: dos
     * tokens que solo difieran en mayusculas son tokens distintos, y meterlos en el
     * mismo bucket los cuenta como uno.
     */
    private static final Set<String> OPAQUE_FIELDS = Set.of("refreshToken", "token");

    private final LettuceBasedProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    public LoginRateLimitFilter(LettuceBasedProxyManager<String> loginRateLimitProxyManager,
            ObjectMapper objectMapper, AuditLogger auditLogger) {
        this.proxyManager = loginRateLimitProxyManager;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;
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
            writeRateLimited(response, routeLimit);
            return;
        }

        HttpServletRequest requestForChain = request;
        for (String accountKey : pathAccountKeys(request, routeLimit)) {
            if (!tryConsume(routeLimit, accountKey)) {
                writeRateLimited(response, routeLimit);
                return;
            }
        }

        if (!routeLimit.accountFields().isEmpty()) {
            byte[] body = request.getInputStream().readNBytes(MAX_ACCOUNT_BODY_BYTES + 1);
            if (body.length > MAX_ACCOUNT_BODY_BYTES) {
                writeProblem(response, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_BODY_TOO_LARGE",
                        "Request body is too large for this endpoint.");
                return;
            }

            requestForChain = new CachedBodyRequest(request, body);
            for (String accountKey : bodyAccountKeys(body, routeLimit)) {
                if (!tryConsume(routeLimit, accountKey)) {
                    writeRateLimited(response, routeLimit);
                    return;
                }
            }
        }

        chain.doFilter(requestForChain, response);
    }

    private static RouteLimit routeLimit(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod()))
            return null;
        String uri = request.getServletPath();
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
        if (uri.equals(CONFIGURATOR_RESOLVE_LIMIT.path()))
            return CONFIGURATOR_RESOLVE_LIMIT;
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
        return null;
    }

    private static BucketConfiguration bucketConfiguration(RouteLimit routeLimit) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(routeLimit.maxAttempts())
                        .refillIntervally(routeLimit.maxAttempts(), routeLimit.window()))
                .build();
    }

    private boolean tryConsume(RouteLimit routeLimit, String key) {
        BucketProxy bucket = proxyManager.builder().build(key,
                () -> bucketConfiguration(routeLimit));
        return bucket.tryConsume(1);
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

    private void writeRateLimited(HttpServletResponse response, RouteLimit routeLimit)
            throws IOException {
        auditLogger.rateLimited(routeLimit.code());
        response.setHeader("Retry-After", String.valueOf(routeLimit.window().toSeconds()));
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

    private record RouteLimit(String keyPrefix, String path, int maxAttempts, Duration window,
            String code, String detail, List<String> accountFields) {
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
