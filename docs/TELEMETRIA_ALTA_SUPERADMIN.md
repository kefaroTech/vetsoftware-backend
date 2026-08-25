# Telemetría y trazabilidad del alta de superadministradores por invitación

Contrato de observabilidad del flujo de `/platform/access-request` → `/platform/invitation/accept`.
Lo escribe el especialista de observabilidad **antes** de que exista el código, para que
`backend-feature` lo implemente con este documento delante. Todo lo que aquí se declara es
verificable: cada sección termina en un test o en un fichero concreto.

Ámbito: los seis endpoints acordados en la incidencia
[#360](https://github.com/kefaroTech/vetsoftware-backend/issues/360), cinco de ellos **sin JWT**, y
cuyo desenlace es la creación de una cuenta con control total de la plataforma
(incidencia [#466](https://github.com/kefaroTech/vetsoftware-backend/issues/466)).

| # | Método | Ruta | Público |
|---|---|---|---|
| 1 | POST | `/platform/access-request` | sí |
| 2 | GET | `/platform/access-request/validate?token=` | sí |
| 3 | POST | `/platform/access-request/approve` | sí |
| 4 | POST | `/platform/access-request/reject` | sí |
| 5 | GET | `/platform/invitation/validate?token=` | sí |
| 6 | POST | `/platform/invitation/accept` | sí |

> Si alguno de los seis termina siendo autenticado (p. ej. un listado de solicitudes para la
> consola), su telemetría es la de un CRUD normal y **no** necesita nada de este documento salvo el
> identificador de correlación de §3.

## 0. Las tres decisiones que no son cosméticas

Si solo se lee una sección, que sea esta. Ordenadas por lo que de verdad mueve la aguja:

1. **El rastro durable del alta no puede ser un log.** `AuditLogger` escribe a un canal cuyo único
   destino es Loki (su propio javadoc lo dice: «mutable y de retención acotada […] no constituye
   evidencia inalterable»), y en el plan Free esa retención se mide en días. «Se creó un
   superadministrador el 14 de marzo, aprobado por X, a partir de la solicitud Y» tiene que poder
   demostrarse dentro de doce meses (ISO/IEC 27001 A.8.15, PCI DSS v4.0 10.5.1, NIST SP 800-53
   AU-11). **La fila persistida es la evidencia; el evento de auditoría es la señal operativa.** El
   modelo debe conservar, en la propia base: id de la solicitud, quién aprobó, cuándo, desde qué IP,
   y el `system_users.id` resultante. Sin eso, el resto de este documento es decoración.
2. **El identificador de correlación de negocio es obligatorio y hay que declararlo en cuatro
   sitios** (§3). Olvidar uno solo de los cuatro no rompe nada visiblemente: deja huecos silenciosos
   en la investigación. Es el fallo más probable de esta implementación.
3. **La severidad no vigila nada.** Ninguna denegación de este flujo se alerta por nivel de log. Se
   alerta por métrica, y **una sola** (§6). Todo lo demás se consulta cuando ya hay una pregunta.

---

## 1. Eventos de auditoría

### 1.1 Cómo se emiten

Métodos nuevos en `com.vetsoftware.app.infrastructure.audit.AuditLogger`, con la misma forma que los
existentes: `audit.atX().addKeyValue(...).log("plantilla {}", valor)`. **No** se crea un logger
paralelo ni una clase de auditoría propia del feature: el canal `AUDIT` es único y es lo que hace
que las consultas de Grafana funcionen sin conocer el feature.

`event` y `reason` son **vocabulario cerrado en snake_case**, extendiendo el que ya existe
(`token_missing`, `token_expired`, `token_invalid`, `session_replaced`, `email_not_verified`). Se
**reutilizan** `token_expired` y `token_invalid` con su significado literal en vez de inventar
`approval_token_expired`: `event` ya desambigua el canal, y un vocabulario paralelo impide preguntar
«cuántos rechazos por token caducado hubo hoy» a través de todo el sistema. Prosa en `reason` rompe
cualquier agrupación en Grafana y está prohibida.

`outcome` usa los tres valores ya vivos: `SUCCESS`, `DENIED`, `FAILURE`.

### 1.2 La tabla

Campos que **ya viajan solos** en cada evento por el MDC y por tanto no se repiten:
`client.ip`, `user_agent.original`, `http.method`, `http.path`, `trace_id`, `span_id`.

| # | Hecho | `event` | `reason` | Nivel | `outcome` | Campos propios | Qué se redacta |
|---|---|---|---|---|---|---|---|
| 1 | Solicitud recibida | `system_user_requested` | — | INFO | `SUCCESS` | `system.user.request.id`, `email.domain` | correo local, nombre, motivo |
| 2 | Solicitud sobre formulario cerrado | `system_user_request_denied` | `form_closed` | INFO | `DENIED` | `email.domain` | ídem + **el motivo del cierre no sale en la respuesta HTTP** (#360) |
| 3 | Solicitud duplicada del mismo correo | `system_user_request_denied` | `duplicate_request` | INFO | `DENIED` | `system.user.request.id` (la existente), `email.domain` | ídem |
| 4 | Token de aprobación inválido | `system_user_approval_denied` | `token_invalid` | INFO | `DENIED` | — | **el token, entero** |
| 5 | Token de aprobación caducado | `system_user_approval_denied` | `token_expired` | INFO | `DENIED` | `system.user.request.id` | el token |
| 6 | Token de aprobación ya usado | `system_user_approval_denied` | `token_consumed` | **WARN** | `DENIED` | `system.user.request.id`, `seconds_since_consumption` | el token |
| 7 | Código de 6 dígitos incorrecto | `system_user_approval_denied` | `code_mismatch` | INFO | `DENIED` | `system.user.request.id`, `attempts.remaining` | **el código, entero** |
| 8 | Intentos agotados → bloqueo | `system_user_approval_locked` | `attempts_exhausted` | **WARN** | `DENIED` | `system.user.request.id` | el código |
| 9 | Aprobación | `system_user_request_approved` | — | INFO | `SUCCESS` | `system.user.request.id` | el token, el código |
| 10 | Rechazo | `system_user_request_rejected` | — | INFO | `SUCCESS` | `system.user.request.id` | el token |
| 11 | Invitación enviada | `system_user_invited` | — | INFO | `SUCCESS` | `system.user.request.id`, `email.domain` | el token de invitación, el enlace completo |
| 12 | El correo de invitación no salió | `system_user_invitation_undelivered` | `email_failed` | **ERROR** | `FAILURE` | `system.user.request.id`, `email.domain` | ídem |
| 13 | Invitación aceptada (solo si la cuenta se crea después) | `system_user_invitation_accepted` | — | INFO | `SUCCESS` | `system.user.request.id` | contraseña, hash, token |
| 14 | **Superadministrador creado** | `system_user_provisioned` | — | INFO | `SUCCESS` | `system.user.request.id`, `actor.systemUserId` | contraseña, hash |
| 15 | Invitación caducada sin usar (barrido) | `system_user_invitation_expired` | `invitation_expired` | INFO | `DENIED` | `system.user.request.id` | — |
| 16 | Aceptación rechazada | `system_user_invitation_denied` | `token_invalid` · `token_expired` · `token_consumed` · `email_already_provisioned` | INFO | `DENIED` | `system.user.request.id` (nulo si el token no existe) | el token, el correo, la contraseña |
| 17 | El correo de bienvenida no salió | `system_user_welcome_undelivered` | `email_failed` | **ERROR** | `FAILURE` | `system.user.request.id`, `email.domain` | el código de usuario, la contraseña |

**Los dos últimos existen porque la respuesta calla.** Los cuatro rechazos de aceptar salen por el mismo 404 indistinguible —deliberado, y no negociable: cualquier diferencia sería un oráculo—, así que el evento es el único sitio donde el hecho puede existir. `email_already_provisioned` es el que de verdad faltaba: para provocarlo hay que poseer una invitación válida, y que alguien la presente contra una identidad que ya tiene superadministrador merece poder verse después. El 17 es el gemelo del 12 y **el segundo ERROR del flujo**: el login de las cuentas de sistema es por `code`, ese correo es el único canal por el que su dueño lo conoce, y perderlo deja una cuenta con control total en la que nadie puede entrar. Se arreglan de formas distintas —el 12 exige reemitir la invitación, el 17 basta con leer el `code` de `system_users` y comunicarlo—, y por eso son dos eventos y no uno.

### 1.3 Por qué cada nivel

El criterio es **quién debe actuar**, no cuán grave suena el hecho — el mismo que `AuditLogger`
argumenta ya para `unauthenticated`.

- **#4, #5, #7 en INFO, no en WARN.** Son endpoints públicos: cualquier anónimo puede provocar un
  token inválido o un código incorrecto a voluntad, tantas veces como el rate limit permita. El
  sistema funcionó exactamente como debía y no hay nada que un operador tenga que hacer con *una*
  ocurrencia. A nivel WARN esta población entierra los WARN que sí piden revisión humana
  (`refresh_token_reuse_detected`, `rate_limited`, y los dos de aquí abajo). **Lo que importa es la
  tasa, y la tasa es una métrica (§4), no un nivel.**
- **#6 en WARN.** Un token de aprobación de un solo uso que se vuelve a presentar no es un error de
  tecleo: o el enlace se filtró, o alguien está reproduciendo un correo. Es la misma semántica que
  `refresh_token_reuse_detected`, que el repo ya trata como WARN porque describe un ataque en curso.
  `seconds_since_consumption` es lo que separa el doble clic del aprobador (segundos) de la
  reproducción (horas o días) — copiado a propósito de `seconds_since_revocation`.
- **#8 en WARN.** Dos poblaciones a la vez, y las dos piden mirar: alguien está probando códigos, y
  además un aprobador legítimo acaba de quedarse fuera y va a pedir ayuda. Es la misma familia que
  `rate_limited`, ya en WARN.
- **#12 en ERROR, y es el único.** El envío es `@Async` fire-and-forget (`ResendEmailClient`, cuyo
  `CompletableFuture` **nunca** se completa excepcionalmente) y **no hay reintento ni outbox**:
  `EmailDispatchOutcome.FAILED` significa que el correo se perdió definitivamente, aunque el HTTP
  haya respondido 200. Nadie ni nada lo recupera sin que una persona reenvíe la invitación → fallo
  terminal por el paso 2 del árbol de decisión. **El arreglo de fondo no es el nivel de log: es que
  no hay reintento.** Mientras no lo haya, este ERROR necesita runbook («reenviar la invitación
  desde …») o no es un ERROR. Ver el precedente de la incidencia #85, que es exactamente este mismo
  defecto en las facturas.
  - `EmailDispatchOutcome.SKIPPED` (correo deshabilitado, modo normal de dev) **no** emite este
    evento ni cuenta como fallo. Contarlo llenaría de falsos positivos toda alerta de tasa.
  - Leer el desenlace obliga a componer sobre el `CompletableFuture`; un `try/catch` alrededor de la
    llamada es código muerto para el 100 % de los fallos reales.
- **#14 en INFO, deliberadamente.** Es un hecho normal de un flujo que funcionó. Subirlo a WARN para
  «que destaque» es usar la severidad como resaltador, que es justo lo que satura el canal. Su
  visibilidad viene del contador y de la **única alerta** de §6, no del nivel.

### 1.4 La regla que decide entre #13 y #14

Los dos eventos describen instantes distintos **solo si el diseño los separa**. Si aceptar la
invitación y crear la cuenta ocurren en la misma transacción, **se emite únicamente #14**: dos
eventos para un mismo hecho rompen «un evento por hecho» y duplican el conteo.

El criterio para saber cuál es el hecho que hay que poder demostrar:
**el instante en que la cuenta puede autenticarse.** Si el diseño crea la fila al aprobar y solo fija
la credencial al aceptar, entonces los eventos son `system_user_provisioned` (fila creada, todavía no
puede entrar) y `system_user_activated` (credencial fijada, ya entra), y **la alerta de §6 vigila el
segundo**.

### 1.5 Higiene, no negociable

- Plantilla constante y parámetros `{}`; nunca concatenación.
- El `Throwable` como último argumento, nunca `e.getMessage()`.
- Nada de «entrando en…»/«saliendo de…»: para eso están los spans.
- **Ni un solo `log-and-throw`.** Se registra donde se maneja el fallo, no en cada capa que lo
  propaga.
- Ningún valor del cuerpo de la petición entra en el mensaje formateado. El JSON estructurado
  neutraliza la inyección de líneas por CRLF (ASVS V7.3.1), pero el mensaje sigue siendo texto.

---

## 2. Lo que no se escribe nunca

### 2.1 Prohibido absoluto

| Dato | Regla | Por qué, más allá de «es un secreto» |
|---|---|---|
| Token de aprobación en claro | **Nunca**, en ningún nivel, ni en `DEBUG` | Es la credencial que aprueba la creación de un superadministrador. ASVS V7.1.1. |
| Token de invitación en claro | **Nunca** | Ídem: quien lo tenga se convierte en superadministrador. |
| El enlace del correo | **Nunca** | Lleva el token dentro. Es la fuga concreta que ya se corrigió en `ResendPasswordResetEmailSender` y `ResendVerificationEmailSender`. |
| Código de 6 dígitos | **Nunca** | Ver 2.2: **no hay red de seguridad para este**. |
| Contraseña elegida al aceptar | **Nunca** | — |
| Hash del token o del código | **Nunca** | El hash **es** el verificador. Para un código de 6 dígitos hay 10⁶ preimágenes: publicar el hash es publicar el código. Y publicar el verificador en Loki lo mueve a un almacén que lee mucha más gente que la base de datos. |

**Para local existe una y solo una salida:** `DevEmailPreview`, que escribe al logger
`DEV_EMAIL_PREVIEW`, declarado solo en el perfil `!prod & !dev`, con `additivity="false"` y un único
appender de consola — no existe ruta desde ahí hasta Loki. Si el flujo necesita ver el token o el
código para poder probarse sin buzón, **va por ahí y por ningún otro sitio**.
`LogbackRedactionConfigTest` verifica que sigue siendo el único canal crudo.

### 2.2 El código de 6 dígitos no tiene red de seguridad

`LogRedactor` suprime corridas de dígitos **de 10 o más**. El umbral es deliberado: por debajo se
solapa con importes e ids de entidad, que son la mayoría de los números que se registran. **Un
código de 6 dígitos no casa con ninguna regla y saldría entero.**

No se debe bajar el umbral: mutilaría todos los ids y todos los importes del sistema. La respuesta
correcta es no registrarlo, y **la garantía es un test con valor señuelo**, no la buena intención:

```java
// El código emitido se fija a un valor único e improbable; se ejercita el flujo entero
// y se afirma que no aparece en NINGUNA línea capturada del appender.
```

Lo mismo para el token: aunque su forma casaría con la regla de JWT solo si lo fuera, un token
opaco aleatorio **no casa con nada**.

### 2.3 Correo y nombre del solicitante — la decisión

Son datos personales de alguien que quizá **nunca fue aprobado**: un solicitante rechazado no es
usuario del producto y no consintió nada.

**Decisión: el correo no sale al log, ni siquiera enmascarado. El nombre y el motivo no salen nunca.
Sale el dominio del correo, y nada más.**

- **Nombre y motivo: fuera.** No responden ninguna pregunta operativa. Toda investigación empieza por
  `system.user.request.id` y sigue en la base de datos, que es donde esos datos tienen dueño, control
  de acceso y política de retención. Minimización: ISO/IEC 27001 A.8.15 exige que lo registrado sea
  proporcionado al propósito; ASVS V7.1.1 prohíbe registrar datos personales que no sean necesarios.
- **Correo completo: fuera, aunque el redactor lo enmascare a `***@dominio`.** Confiar en el
  enmascarado para un dato que se sabe personal es exactamente la confusión que dejó viva la fuga de
  la incidencia #81 durante meses: «redactado centralmente» no equivale a «seguro», y la capa de
  texto libre es de mejor esfuerzo. Si no debe salir, no se emite.
- **`email.domain`: sí, y solo el dominio.** Responde una pregunta concreta que ninguna otra señal
  responde: *¿estas cuarenta solicitudes vienen de cuarenta dominios desechables distintos o son tres
  personas de la misma empresa?* Es la diferencia entre «abuso» y «el equipo de un cliente». El
  dominio, por sí solo, no identifica a una persona. Se declara **`SCANNED`**, no `VERBATIM`: un
  dominio es texto semi-libre y si alguien mete el correo entero por error, el enmascarado todavía lo
  ataja.
- **`client.ip` ya viaja en el MDC** para toda request y cubre el «desde dónde» que exige NIST SP
  800-53 AU-3 / PCI DSS 10.2.

### 2.4 El token viaja en la query string: hay que probar que no se escapa

Los endpoints 2 y 5 llevan `?token=`. Consecuencias que no se arreglan con redacción:

- `RequestLoggingContextFilter` usa `request.getRequestURI()`, que **no** incluye la query string, así
  que `http.path` está a salvo. Comprobado en
  `src/main/java/com/vetsoftware/app/infrastructure/web/RequestLoggingContextFilter.java:37`.
- **No comprobado y obligatorio de comprobar:** que ningún atributo del span SERVER (`url.full`,
  `http.url`) ni ninguna traza de acceso del contenedor lleve la query string. La instrumentación de
  Spring puede publicar la URL como clave de alta cardinalidad.
- Fuera de nuestro proceso, un token en la URL acaba en el historial del navegador, en la cabecera
  `Referer` de cualquier recurso de terceros de esa página y en los logs de acceso del balanceador.
  **Nada de eso lo controla este repo.**

Entregable exigido: un test de contra-prueba con **token señuelo** que ejercite los dos `GET` y
afirme que el valor no aparece ni en los logs capturados ni en los atributos del span. Si aparece, la
corrección es mover el token al cuerpo de un `POST`; bajar la retención o confiar en el redactor no lo
es. Independientemente del resultado, el token debe ser de un solo uso y de vida corta, que es lo que
acota el daño de las rutas que no controlamos.

---

## 3. Correlación: cómo se atan tres peticiones separadas por horas

`trace_id` **no sobrevive**, y no debe: W3C Trace Context identifica *una* operación distribuida, no
un proceso de negocio con un humano dentro. Estirar una traza a lo largo de horas rompe toda métrica
de latencia derivada de ella.

### 3.1 El identificador

**`system.user.request.id`** — el id de la solicitud de alta, generado por el servidor en el paso 1,
persistido, y resoluble desde cualquier token (`hash(token) → fila de la solicitud`).

- **Forma:** el `Long` de la clave primaria, igual que `company.id` y `employee.id`. Es la forma que
  el sistema garantiza, así que puede ir `VERBATIM` sin que el enmascarado lo mutile. No sale nunca
  al cliente (el cliente maneja tokens), así que no hace falta que sea opaco.
- **Nombre:** `system.user.*` porque el recurso que este flujo termina creando es un `system_user`, y
  eso lo deja agrupado con las ocho observaciones `system.user.*` que ya existen. En Loki, tras
  `| json`, el campo se consulta como **`system_user_request_id`** (los puntos pasan a guiones bajos).

### 3.2 Los cuatro sitios donde hay que declararlo — los cuatro

Falta uno cualquiera y la correlación se rompe **en silencio**: sin error, sin alerta, solo huecos.

| # | Dónde | Qué exactamente | Qué pasa si falta |
|---|---|---|---|
| 1 | `MdcKeys` | `public static final String SYSTEM_USER_REQUEST_ID = "system.user.request.id";` | No hay fuente única; aparecen literales sueltos. |
| 2 | `LogFieldPolicy.VERBATIM` | añadir la constante | **El valor sale como `***` en todos los logs.** Es la incidencia [#153](https://github.com/kefaroTech/vetsoftware-backend/issues/153), abierta precisamente porque nada lo detecta. |
| 3 | `RequestLoggingContextFilter.clearApplicationContext()` | `MDC.remove(...)` en las dos llamadas (entrada y `finally`) | La clave sobrevive en un hilo del pool y **etiqueta la petición del siguiente usuario con el id de otro**. |
| 4 | `AsyncConfig.contextPropagatingTaskDecorator()` | añadir la constante a la lista explícita de `Slf4jThreadLocalAccessor` | El envío del correo corre en `emailTaskExecutor` y **el id no cruza el salto de hilo**: el evento #12 —el ERROR de correo perdido— sale sin saber de qué solicitud habla. Sin ningún síntoma visible. |

El punto 4 es el más fácil de pasar por alto: `ContextPropagatingTaskDecorator` sí propaga la
observación y la traza, pero el MDC viaja por una **lista explícita de claves**
(`ACTOR_TYPE`, `ACTOR_EMPLOYEE_ID`, `ACTOR_COMPANY_ID`, `ACTOR_SYSTEM_USER_ID`, `HTTP_METHOD`,
`HTTP_PATH`) y lo que no esté en esa lista se queda en el hilo de la request.

### 3.3 Cómo se pone y se quita

En la **capa de aplicación**, no en un filtro: el id solo se conoce después de resolver el token, que
es trabajo de dominio.

```
MDC.put(MdcKeys.SYSTEM_USER_REQUEST_ID, String.valueOf(requestId));
try { ... } finally { MDC.remove(MdcKeys.SYSTEM_USER_REQUEST_ID); }
```

`trace_id`/`span_id` **no se tocan jamás**: son propiedad exclusiva de Micrometer Tracing.

### 3.4 También como atributo de span

En cada observación del flujo:

```
observation.highCardinalityKeyValue("system.user.request.id", String.valueOf(requestId));
```

**`highCardinalityKeyValue`, nunca `lowCardinalityKeyValue`.** En Micrometer, lo *low* va a métricas
**y** a trazas; lo *high* solo a trazas. Un id como etiqueta de métrica es una serie por solicitud —
el fallo más caro del catálogo.

### 3.5 Lo que esto compra a las 3 de la mañana

```
Tempo:  { .system.user.request.id = "4271" }        → las 3 trazas del flujo
Loki:   {service_name="vetsoftware"} | json | system_user_request_id="4271"
        → las 15 líneas del flujo entero, ordenadas, incluida la del correo perdido
```

Y dentro de la traza del paso 1, el span `email.send` cuelga de la petición porque el contexto sí
cruza el `@Async`: se ve en la misma traza cuánto tardó Resend y si falló.

---

## 4. Métricas

### 4.1 Dónde viven

Bajo el prefijo `vetsoftware.business.`, declaradas en `BusinessMetricNames`, emitidas desde
`MicrometerBusinessMetrics` a través de un puerto de salida del feature. **No** se instancian
`Counter` sueltos en el servicio.

El motivo no es estético: solo dentro de ese prefijo actúa `BusinessMetricCardinalityFilter`, que
(a) deniega cualquier etiqueta o valor no declarado en su lista blanca, (b) cuenta el descarte en
`vetsoftware.observability.metrics.denied` y (c) lo registra en ERROR con instrucciones. Fuera del
prefijo no hay ninguna barrera: un identificador como etiqueta se publicaría sin más.

> El prefijo dice «business» y esto es administración de plataforma. Es el precio de heredar la
> lista blanca, y se paga a gusto. La alternativa —un prefijo `vetsoftware.security.*` propio— exige
> extender el filtro a un segundo prefijo y duplicar su catálogo por reflexión; si el equipo lo
> prefiere, es un cambio aparte y no debe hacerse dentro de este feature.

### 4.2 Las cuatro métricas

| Nombre | Tipo | Etiquetas | Valores permitidos | Series |
|---|---|---|---|---|
| `vetsoftware.business.system.user.requests` | Counter | `result` | `success`, `duplicate_ignored`, `form_closed` | 3 |
| `vetsoftware.business.system.user.approvals` | Counter | `result` | `approved`, `rejected`, `token_invalid`, `token_expired`, `token_consumed`, `code_mismatch`, `attempts_exhausted` | 7 |
| `vetsoftware.business.system.user.invitations` | Counter | `result` | `sent`, `failed`, `skipped`, `accepted`, `expired`, `token_invalid`, `token_consumed`, `email_already_provisioned` | 8 |
| `vetsoftware.business.system.user.provisioned` | Counter | **ninguna** | — | 1 |

**Presupuesto de cardinalidad: 3 + 7 + 8 + 1 = 19 series nuevas**, más 4 series de
`vetsoftware_observability_metrics_denied_total{metric=…}` que el filtro pre-registra por reflexión al
añadir los cuatro nombres al catálogo. **Total: 23 series.** Constante: ninguna de las tres etiquetas
crece con el tráfico ni con el número de tenants.

Notas de implementación que evitan nombres que nadie adivina al escribir la alerta:

- `requests` / `approvals` / `invitations`: `baseUnit` igual al sufijo plural del nombre. El
  exportador de Prometheus no lo duplica → `vetsoftware_business_system_user_requests_total`.
- `provisioned`: **sin `baseUnit`.** El nombre no termina en un sustantivo de unidad, así que
  declarar una produciría `..._provisioned_admins_total`. Sin ella queda
  `vetsoftware_business_system_user_provisioned_total`, que es lo que dice la alerta.
- **`provisioned` se pre-registra a cero en el constructor** (`Counter.builder(...).register(registry)`,
  no solo `MeterProvider`). Sin etiquetas no hay excusa para diferirlo, y es lo que hace que
  `increase(...) > 0` funcione desde el primer scrape en lugar de depender de que la serie nazca justo
  durante el incidente. Es el mismo argumento con el que el filtro pre-registra sus contadores de
  descarte. Comprobado hoy contra Grafana Cloud: `vetsoftware_security_tokens_rejected_total` **no
  existe** en el tenant porque nunca se ha incrementado — exactamente el fallo que esto evita.
- Todos los éxitos (`success`, `approved`, `accepted`, `provisioned`) se publican con
  `AfterCommitMetricRecorder.recordAfterCommit(...)`. Publicar un alta de superadministrador que luego
  hace rollback es contar una cuenta que no existe. Las denegaciones se publican de inmediato: no hay
  transacción que esperar.

### 4.3 Valores nuevos que hay que añadir a `BusinessMetricCardinalityFilter.ALLOWED_VALUES`

En la entrada `result`, que ya existe. Ya declarados: `rejected`, `duplicate_ignored`, `success`,
`failed`, `pending`. **Faltan y hay que añadir:** `form_closed`, `approved`, `token_invalid`,
`token_expired`, `token_consumed`, `code_mismatch`, `attempts_exhausted`, `sent`, `skipped`,
`accepted`, `expired`, `email_already_provisioned`.

`token_invalid` y `token_consumed` los emiten **dos** medidores distintos —`approvals` y
`invitations`— con el mismo significado a los dos lados del flujo. Es reutilización deliberada del
vocabulario, igual que `rejected` o `failed`: abrir `invitation_token_invalid` impediría preguntar
«cuántos rechazos por token muerto hubo hoy» de una sola vez. `email_already_provisioned` sí es
nuevo, porque el concepto no existía en ninguna otra parte del sistema.

Si se olvida uno, el medidor **entero** queda denegado: el panel muestra un hueco indistinguible de
«no hubo actividad», y el único aviso es un ERROR en el log más el contador de descartes — que hoy
**no tiene alerta ni panel**, incidencia
[#173](https://github.com/kefaroTech/vetsoftware-backend/issues/173).

### 4.4 Etiquetas prohibidas

`email`, `email.domain`, `system.user.request.id`, `token`, `token.hash`, `client.ip`,
`user_agent.original`, `name`, `http.path`, cualquier id.

Van a **atributos de span** (que aguantan la alta cardinalidad) o a **campos de log**. Si alguna se
emite, el filtro deniega el medidor completo y la métrica queda ciega — no ruidosa, **ciega**.
La respuesta correcta nunca es añadirla a la lista blanca.

### 4.5 Métricas que se proponen y se rechazan

- **Timer «solicitud → superadministrador creado».** Mide una espera humana de horas o días, no el
  sistema. Ningún valor accionable a las 3 de la mañana. Coste puro.
- **Un contador aparte para el bloqueo.** Ya es `result="attempts_exhausted"` en `approvals`.
- **Un timer por endpoint.** Ya lo da `http_server_requests_seconds` con `uri` plantillado (las seis
  rutas no tienen variables de path, así que la cardinalidad es 6), y `@Observed` publica además un
  timer por caso de uso.

### 4.6 El barrido de invitaciones caducadas

Es un `@Scheduled` y va con `ScheduledJobTelemetry`, no con métricas propias:

```
job.name = "system.user.invitation.expiry"
```

`lowercase.dot.notation`, como `security.tokens.cleanup`. `ScheduledJobTelemetry` rechaza en tiempo de
ejecución cualquier nombre que no cumpla. Devuelve `Outcome.from(intentadas, fallidas)`, que ya
distingue `no_work` (corrió y no había nada que caducar) de `failure` — la dimensión de resultado que
separa «no hubo trabajo» de «se rompió». Cada invitación caducada incrementa además
`invitations{result="expired"}`.

---

## 5. Nombres de `@Observed`

Familia `system.user.*`, coherente con las ocho observaciones ya existentes de ese dominio. Todas
cumplen `^[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)+$` y son únicas, que es lo que exige
`ObservationNamingConventionTest`.

| Endpoint | Caso de uso | `@Observed(name = …)` |
|---|---|---|
| POST `/platform/access-request` | `RequestSystemUserAccessService` | `system.user.request.create` |
| GET `/platform/access-request/validate` | `ValidateSystemUserApprovalTokenService` | `system.user.request.validate.token` |
| POST `/platform/access-request/approve` | `ApproveSystemUserRequestService` | `system.user.request.approve` |
| POST `/platform/access-request/reject` | `RejectSystemUserRequestService` | `system.user.request.reject` |
| GET `/platform/invitation/validate` | `ValidateSystemUserInvitationTokenService` | `system.user.invitation.validate.token` |
| POST `/platform/invitation/accept` | `AcceptSystemUserInvitationService` | `system.user.invitation.accept` |
| (si se añade) reenvío de aprobación | `ResendSystemUserApprovalService` | `system.user.request.resend.approval` |
| (si se añade, #360) disponibilidad | `CheckSystemUserRequestAvailabilityService` | `system.user.request.check.availability` |

`system.user.request.validate.token` calca a propósito la forma de `password.reset.validate.token`, que
es el flujo público más parecido que ya existe.

**`contextualName` solo si aporta**, y entonces `verbo objeto` en minúsculas separado por espacios
(p. ej. `approve access request`). Es opcional; el nombre técnico es el identificador estable para
paneles y alertas.

### 5.1 Estado del span

- Los seis endpoints producen spans **SERVER**. Un 4xx (token inválido, código incorrecto, formulario
  cerrado) deja el status en **`Unset`**: el cliente se equivocó, el servidor funcionó. Solo un 5xx va
  a `Error`.
- Si se captura una excepción y se decide marcar el span, hay que hacer **las dos llamadas**: registrar
  el evento `exception` **y** fijar el status. Registrar la excepción no fija el status, y olvidarlo
  deja el span verde con la excepción dentro.
- **Coherencia de veredicto:** si el log dice `INFO`/`DENIED`, el span no puede decir `Error`. Una de
  las dos señales estaría mintiendo justo cuando más caro es perder tiempo.

---

## 6. La alerta. Una.

```yaml
- alert: VetSoftwareSystemUserProvisioned
  expr: sum(increase(vetsoftware_business_system_user_provisioned_total[5m])) > 0
  for: 0m
  labels:
    severity: critical
    domain: security
    service: vetsoftware
  annotations:
    summary: "Se creó una cuenta con control total de la plataforma"
    description: >-
      Un superadministrador fue provisionado en los últimos cinco minutos. Confirmar contra el
      registro de aprobación (system.user.request.id en Loki y en la tabla de solicitudes) que la
      creación corresponde a una aprobación legítima. Si no la hay, la plataforma está comprometida.
    runbook: "<URL absoluta de VetSoftwareIaC/docs/ALERTAMIENTO_OPERATIVO.md#vetsoftwaresystemuserprovisioned>"
```

**Por qué esta y no otra.** Es el único hecho irreversible del flujo: una cuenta con control total
sobre todos los tenants. Ocurre un puñado de veces al año, así que una alerta por cada ocurrencia no
genera fatiga; y quien la atiende la cierra en treinta segundos si era legítima. Si no lo era, se
detecta el compromiso total en cinco minutos en lugar de en la siguiente revisión trimestral.

**Por qué no la fuerza bruta sobre el código.** Un pico de `code_mismatch` merece un ticket, no un
despertar: mientras el bloqueo por intentos y el rate limit funcionen, no pasa nada; y su **único
desenlace exitoso posible es exactamente esta alerta disparándose**. Vigilarla por separado duplica el
ruido sin adelantar el descubrimiento.

**Sobre qué se evalúa, y esto importa tanto como la expresión.** La alerta va a
`VetSoftwareIaC/observability/grafana-managed/vetsoftware-cloud-additions-managed.yml`, en formato
Grafana-managed, leyendo el datasource `grafanacloud-prom`, y enrutada por `severity: critical` al
contact point `vetsoftware-critical`. **No** va a `docker/prometheus-business-alerts.yml`: ese fichero
está declarado explícitamente sin gemelo Cloud y **solo se evalúa en el stack local**, así que una
alerta puesta ahí no despierta a nadie. El fichero nuevo debe listarse en `PROVISIONING_FILES` de los
dos workflows de sync, y el ancla del runbook debe existir en
`VetSoftwareIaC/docs/ALERTAMIENTO_OPERATIVO.md`.

**Ese cambio es de `VetSoftwareIaC` y lo ejecuta quien tenga ese repo**, no este feature. Aquí se
declara el contrato; allí se despliega.

Dos precondiciones sin las cuales la alerta no dispara:

1. La serie tiene que existir antes del incidente → pre-registro a cero (§4.2).
2. El prefijo `vetsoftware.business.` tiene que llegar al tenant. **Verificado hoy**:
   `vetsoftware_business_dian_transmissions_total` está presente en `grafanacloud-prom`, así que la
   ruta funciona.

Complemento opcional, no obligatorio: `absent(vetsoftware_business_system_user_provisioned_total)`
como aviso de que la serie desapareció. Se menciona por completitud; con una sola señal que despierte
a alguien, esta no es esa.

---

## 7. Qué hay que ejecutar antes de dar el feature por cerrado

```bash
# Convención de nombres y gobierno del logback
mvn test -Dtest='ObservationNamingConventionTest,LogbackRedaction*'

# Redacción y contra-prueba de auditoría: los campos nuevos NO deben salir como ***
mvn test -Dtest='LogRedactor*,LogRedaction*,AuditFieldsSurvive*'

# Instrumentación y propagación a través del @Async del correo
mvn test -Dtest='*Observability*,*Observation*,ScheduledJobTelemetryTest,AsyncObservedIntegrationTest,*BusinessMetric*'
```

Tests nuevos que este contrato exige y que hoy no existen:

| Test | Qué fija | Sin él |
|---|---|---|
| Contra-prueba de señuelo del **código de 6 dígitos** | que el código no aparece en ninguna línea | §2.2: no hay ninguna regla del redactor que lo ataje |
| Contra-prueba de señuelo del **token** en los dos `GET` | que la query string no llega a logs ni a atributos de span | §2.4 |
| `AuditFieldsSurviveRedactionTest` extendido con `system.user.request.id`, `email.domain`, `attempts.remaining` | que los campos nuevos no salen `***` | incidencia #153: nada lo detecta |
| Propagación de `system.user.request.id` a través de `emailTaskExecutor` | que el evento del correo perdido sabe de qué solicitud habla | §3.2 punto 4, sin síntoma visible |
| El valor `result` de cada métrica está en `ALLOWED_VALUES` | que ninguna serie nace ciega | §4.3 |

---

## 8. Documentos relacionados

- `docs/POLITICA_REDACCION_LOGS.md` — allowlist, redactor, `DevEmailPreview`.
- `docs/CONVENCION_NOMBRES_OBSERVABILIDAD.md` — nombres de observaciones, spans y jobs.
- `docs/ALERTAS_STACK_LOCAL.md` — alertas del plano local (donde esta alerta **no** va).
- `docs/SEGURIDAD_OBSERVABILIDAD.md`, `docs/RESILIENCIA_OBSERVABILIDAD.md`.
- `VetSoftwareIaC/observability/grafana-managed/README.md` — por qué todo el alerting vive allí.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
