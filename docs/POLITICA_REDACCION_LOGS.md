# Política automática de redacción de logs

Cierra **OBS-019** de `proporsal/REPORTE_TRAZABILIDAD_TELEMETRIA_BACKEND.md`. Establece un
mecanismo central que impide que tokens, credenciales, datos personales, tributarios, de pago o
payloads clínicos salgan del proceso hacia archivos, la consola o Loki.

## 1. Principio

Todo evento de log pasa por un único punto antes de salir del proceso. Ese punto es un **appender
decorador** que envuelve a los appenders reales, así que la redacción no depende de que quien añade
un destino se acuerde de aplicarla: **un appender que no se envuelve no recibe eventos**.

```
logger.info(...) ──▶ RedactingAppender ──▶ CONSOLE
                          │             └─▶ OTEL ──▶ Collector ──▶ Loki
                          │
                     LogRedactor
```

| Clase | Responsabilidad |
|---|---|
| `LogRedactor` | Motor puro de redacción. Sin Logback ni Spring, por eso es verificable. |
| `LogFieldPolicy` | Allowlist de campos estructurados (MDC y `KeyValuePair`). |
| `RedactingAppender` | Decorador que redacta y reenvía a los appenders anidados. |
| `RedactedLoggingEvent` | Vista redactada del evento: mensaje, MDC, pares clave-valor y excepción. |
| `RedactedThrowable` | Copia de la excepción con los mensajes de toda su cadena ya redactados. |

Se usa un decorador y no un `Filter`/`TurboFilter` porque los filtros de Logback solo deciden si un
evento pasa o no — **no pueden transformarlo**.

## 2. Las dos superficies y sus dos garantías

La fuerza de la garantía es distinta en cada una, y conviene no confundirlas.

### 2.1 Campos estructurados — allowlist (garantía cerrada)

Las claves de MDC y de `addKeyValue(...)` se gobiernan por allowlist en `LogFieldPolicy`. **Lo que no
está declarado sale como `***`.** Un campo nuevo es opaco por defecto: no hay forma de introducir un
campo sensible por descuido.

Dos niveles:

| Nivel | Comportamiento | Para qué |
|---|---|---|
| `VERBATIM` | El valor sale intacto, sin enmascarado de texto. | Identificadores **cuya forma la fija el sistema**: `traceId`, `actor.type`, `actor.id`, `actor.employeeId`, `actor.companyId`, `client.ip`, `http.status`, `company.id`, `company.identifier`… |
| `SCANNED` | El valor se permite pero pasa por el enmascarado de texto. | Texto libre o semi-libre donde puede colarse algo (`http.path`, `user_agent.original`, `company.name`) **y todo identificador que teclea un humano**: `actor.identifier`, `employee.identifier`. |

`VERBATIM` existe porque el enmascarado dañaría estos valores: el NIT del tenant
(`company.identifier`) tiene 10 dígitos y la regla de documentos personales lo suprimiría, cegando
la auditoría precisamente en el campo con el que se investiga un incidente.

**El criterio es la forma, no la confidencialidad** (incidencia #216). `actor.identifier` y
`employee.identifier` estuvieron en `VERBATIM` porque «un código de acceso de empleado no es un
secreto», y la premisa la rompe el propio producto: en el auto-registro el código de acceso **es** el
correo del dueño (`RegisterUserService.register` hace `String employeeCode =
command.employeeEmail().trim()`), así que cada alta de veterinaria y cada login publicaban el correo
en claro hasta Loki. Un valor que escribe un humano no tiene forma garantizada y no puede ir en
`VERBATIM`, por muy «no secreto» que sea el concepto que representa.

Escanearlas **no ciega la investigación**: el enmascarado es por patrones, y `EMP0042` u `OWNER01` no
casan con ninguno, así que salen enteros. Un correo sale como `***@clinica.com` —se conserva el
dominio— y un código que sea una cédula queda suprimido, que es lo que le corresponde a un documento
personal. La decisión está fijada por `LogFieldPolicyTest` y por
`AuditFieldsSurviveRedactionTest.el_correo_usado_como_codigo_de_acceso_no_sale_en_claro`.

### 2.2 Texto libre — patrones (defensa en profundidad, de mejor esfuerzo)

El mensaje formateado y los mensajes de excepción no admiten allowlist, así que se aplican reglas de
detección. Cubren las formas de alta confianza:

| Regla | Ejemplo de entrada | Salida |
|---|---|---|
| Credenciales en URL | `jdbc:mysql://root:s3cr3t@db:3306/vet` | `jdbc:mysql://***:***@db:3306/vet` |
| JWT | `token rechazado: eyJhbGci….…` | `token rechazado: ***` |
| Esquema HTTP | `Authorization: Bearer eyJ…` | `Authorization: *** ***` |
| Tarjeta (Luhn) | `pago con 4111 1111 1111 1111` | `pago con ***1111` |
| Clave sensible | `password=hunter2`, `{"clave":"x"}` | `password=***`, `{"clave":"***"}` |
| Correo | `senuelo@gmail.com` | `***@gmail.com` |
| Teléfono internacional | `+57 320 555 7788` | `***` |
| Documento aislado | `documento 1032456789` | `documento ***` |

Se conserva **la clave visible y el valor suprimido** (`password=***`, no `***`): quien lea el log
sabe qué campo se ocultó sin conocer su contenido. En los correos se conserva el dominio, que no es
dato personal y sirve para diagnosticar entregas.

**Lo que esta capa no puede prometer:** prosa clínica sin clave asociada. `LogRedactor` detecta
`diagnostico=Insuficiencia renal`, pero no una frase suelta. Para eso la regla sigue siendo **no
registrar entidades de dominio ni payloads completos** — registra ids y deja que la investigación
vaya a la base de datos.

### 2.3 Decisiones de diseño que no conviene revertir

- **El orden de las reglas importa.** Las de forma (JWT, `Bearer`, Luhn) corren *antes* que la de
  clave-valor. `Authorization: Bearer eyJ…` tiene un valor con espacios: si la regla de clave-valor
  corriera primero, cortaría en el espacio y solo enmascararía la palabra `Bearer`, **dejando el
  token intacto**. Hay una prueba de regresión para esto.
- **Los lookarounds excluyen letras, no solo dígitos.** Sin eso, un hash hex de 64 caracteres tiene
  ~50 % de probabilidad de contener 10 dígitos seguidos por azar, y los checkpoints de la cadena de
  auditoría saldrían mutilados.
- **El umbral de dígitos aislados es 10.** Por debajo se solapa con importes e ids de entidad, que
  son la mayoría de los números que se registran. Los documentos más cortos quedan cubiertos por la
  regla de clave-valor cuando vienen con su clave.
- **Se redacta el mensaje ya formateado, no argumento a argumento.** En `log.info("password={}",
  secreto)` el argumento aislado es texto anodino; el secreto solo existe al unir plantilla y
  argumento. Por eso `RedactedLoggingEvent.getArgumentArray()` devuelve `null` y `getMessage()`
  devuelve el texto ya redactado: un encoder que reformatee obtiene el mismo texto y no puede
  reconstruir una versión limpia.
- **Las excepciones se clonan, no se envuelven.** El appender de OpenTelemetry solo reconoce la clase
  concreta `ThrowableProxy` y saca de ella el `Throwable` real; un `IThrowableProxy` propio haría que
  OTel **descartara la excepción completa**, perdiendo tipo, mensaje y stacktrace en Loki. Se
  construye entonces un `Throwable` real con el mensaje redactado, conservando stacktrace, causas y
  suprimidas. El `toString()` sobrescrito preserva el tipo original en consola y en
  `exception.stacktrace`; solo el atributo `exception.type` pasa a ser `RedactedThrowable`, y
  **únicamente cuando la redacción cambió algo** — una excepción limpia se reenvía sin tocar.
- **Si no hubo cambios, se reenvía el evento original.** Ni copia del evento, ni del mapa MDC, ni de
  la lista de pares. El caso normal no paga nada.

## 3. Cómo añadir un campo nuevo

1. Si es una clave de MDC, decláralas en `MdcKeys` **y** en `LogFieldPolicy`.
2. Si es un `addKeyValue(...)` de un evento `AUDIT`, decláralo en `LogFieldPolicy`.
3. Elige el nivel: `VERBATIM` solo si la forma del valor es conocida y acotada; `SCANNED` si es texto
   libre.
4. Corre `AuditFieldsSurviveRedactionTest`. Si el campo queda en `***`, falta declararlo.

Si el campo es sensible, la respuesta correcta **no** es añadirlo a la allowlist: es no registrarlo.

### Y una quinta regla que el bloque de dinero de suscripciones hizo obligatoria

5. **Si el valor puede tener diez dígitos o más, no lo interpoles en el texto del mensaje.**

El redactor escanea el mensaje ya formateado, y su regla `LONG_DIGIT_RUN` suprime toda corrida de
diez dígitos o más por parecerse a una cédula. Una factura anual de 500 clínicas pasa de diez
dígitos con facilidad, así que un `log("… amount={}", amount)` sale como `***` **en el mensaje**
mientras el campo estructurado `amount`, declarado `VERBATIM`, sale entero: **dos verdades distintas
en el mismo evento**, y la que primero ve un humano es la mutilada.

Los catorce eventos de dinero de `AuditLogger` dejan por eso los importes fuera del texto y solo en
campos. Además de correcto, es la práctica que este documento ya predicaba por otro motivo: el
mensaje es una constante y lo variable va en campos, que es lo que permite agrupar por plantilla en
Loki.

### Los campos del dinero de suscripciones (#607)

Veinticuatro claves nuevas, todas `VERBATIM` salvo una. Son ids numéricos, enums de dominio en
mayúsculas, importes decimales y cantidades enteras: su **forma** la garantiza el sistema, que es lo
que `VERBATIM` significa aquí —no «no es confidencial»—. Someterlos al enmascarado no sería neutro
sino destructivo, por lo que acaba de decirse sobre `LONG_DIGIT_RUN`.

La excepción es **`change.reason`**, y merece la pena entender por qué no se llama `reason`.
`reason` es `VERBATIM` porque en todo el resto del canal `AUDIT` es vocabulario cerrado en
`snake_case` —`token_expired`, `form_closed`, `email_not_verified`—. El motivo de un cambio de
contrato lo **teclea una persona**: puede traer el correo del cliente, una cédula, o un CRLF que
fabrique una segunda línea de auditoría falsa (ASVS V7.3.1). Metido en `reason` habría publicado
texto de usuario sin tocar **y** habría vuelto inagrupable un tag que hoy se filtra en Grafana.
Escaneado, un motivo normal —«downgrade solicitado por el cliente»— sale entero y un dato personal
sale enmascarado.

**Lo que no está declarado y no debe estarlo**: la descripción libre de un cargo, el motivo tecleado
al anular una cuenta de cobro y la referencia de la factura externa. Los dos primeros son texto que
elige un humano y el tercero lo elige un tercero; esta lista no es el sitio para decidir qué se puede
leer de ellos. El sitio es no emitirlos.

## 4. La única excepción, acotada por construcción

`DevEmailPreview` imprime el enlace o los códigos que un correo habría llevado cuando el envío está
deshabilitado (`vetsoftware.email.enabled=false`), para poder continuar un flujo de verificación o de
restablecimiento sin buzón. Es **el único canal sin redacción**, y no depende de la confianza:

- Escribe al logger `DEV_EMAIL_PREVIEW`, declarado **solo** en el perfil `!prod & !dev`.
- Con `additivity="false"` y un único appender de consola: **no existe ruta desde ese logger hasta el
  appender de OpenTelemetry**, así que su contenido no puede llegar a Loki.
- Fuera de local el logger no está declarado y sus eventos caen en la raíz, que sí está redactada.

`LogbackRedactionConfigTest` verifica que sigue siendo el único canal crudo.

## 5. Pruebas

Todas usan **valores señuelo**: cadenas únicas e improbables que, si aparecen en la salida, prueban
una fuga.

| Suite | Qué verifica |
|---|---|
| `LogRedactorTest` (30) | Cada regla de forma y de clave; la allowlist; idempotencia; y **lo que no debe tocarse** (mensajes operativos, hashes hex, IPs, timestamps). |
| `LogRedactionPipelineTest` (10) | Extremo a extremo sobre un pipeline de Logback real: el señuelo se inyecta por mensaje, MDC, pares clave-valor y cadena de excepciones, y se afirma que no sobrevive en ninguna. |
| `LogbackRedactionConfigTest` (4) | Gobierno del XML: todo appender de `<root>` es un `RedactingAppender`, en todos los perfiles. |
| `AuditFieldsSurviveRedactionTest` (3) | Contra-prueba: ningún campo que la auditoría emite a propósito sale enmascarado. Una allowlist demasiado estrecha no rompe nada visiblemente, solo ciega la auditoría en silencio. |

```bash
mvn test -Dtest='LogRedactor*,LogRedaction*,LogbackRedaction*,AuditFieldsSurvive*'
```

## 6. Fugas concretas corregidas

| Sitio | Antes | Ahora |
|---|---|---|
| `ResendPasswordResetEmailSender` | Registraba el enlace con el **token de restablecimiento en claro** y el correo del destinatario. | `DevEmailPreview` (canal local, sin salida a Loki). |
| `ResendVerificationEmailSender` | Igual, con el **token de verificación**. | Igual. |
| `ResendCodeRecoveryEmailSender` | Registraba el correo y el listado de códigos de acceso. | Igual. |
| `ResendEmailClient` | Registra el correo del destinatario en 5 sitios. | La parte local se enmascara (`***@gmail.com`); el dominio se conserva para diagnóstico. |
| `GlobalExceptionHandler` | ~30 `log.warn("…: {}", ex.getMessage())` y el stacktrace de errores 500. | Mensajes y cadena de excepciones redactados centralmente. |
| `GlobalExceptionHandler.handleDataIntegrity` | `log.warn("Data integrity violation: {}", ex.getMessage())` volcaba el mensaje de Hibernate entero —la sentencia y el valor duplicado: documento, correo o nombre del propietario— en **cada alta duplicada** (#81). | Se registran solo `constraint=` y `type=`. El throwable completo queda para la rama de constraint sin mapear, la única que necesita diagnóstico, y ahí sí pasa por `RedactedThrowable`. |
| `GlobalExceptionHandler.handleExceptionInternal` (4xx) | El detalle salía de `ex.getMessage()`, y el de `MethodArgumentNotValidException` lleva los **valores rechazados** del formulario dentro. | `clientErrorDetail(ex)`: de las excepciones de binding y de deserialización se registra el tipo y los **nombres de campo** (esquema, no dato); el resto de mensajes de Spring MVC son constantes del framework y se conservan. |

## 7. Alcance y limitaciones conocidas

- **No cubre lo que ya salió.** La política aplica a eventos nuevos; los logs históricos en Loki no
  se reescriben.
- **El texto libre es de mejor esfuerzo, y «redactado centralmente» no equivale a «seguro».** Las
  reglas de texto reconocen formas (JWT, `Bearer`, correo, tarjeta con Luhn, corridas de 10+
  dígitos, clave-valor sensible); **no** reconocen nombres propios ni prosa. Por eso la regla de
  fondo sigue siendo **no registrar entidades ni mensajes de driver**, y no «déjalo, ya lo redacta
  el appender»: fue exactamente esa confusión la que dejó viva la fuga de #81 durante meses. El
  residuo conocido —el valor entre comillas de un `Duplicate entry` cuando es un nombre propio—
  está abierto en #170.
- **No cubre otros destinos de datos.** Solo logs. Las respuestas HTTP y los correos tienen sus
  propias reglas.
- **La auditoría depende hoy por completo de esta política.** Desde que se retiró el outbox, el
  canal `AUDIT` es el único destino de los eventos de auditoría: no hay una copia en base de datos
  que quede al margen del enmascarado. Una allowlist demasiado estrecha ya no ciega solo una vista,
  ciega el registro entero — de ahí que `AuditFieldsSurviveRedactionTest` sea contra-prueba
  obligatoria y no un extra.
- **Coste.** Un barrido de caracteres por evento para decidir qué reglas pueden casar, y solo las
  necesarias se ejecutan. Los mensajes operativos típicos no disparan ninguna.
- **Prosa clínica sin clave** no se detecta (§2.2).

## 8. Documentos relacionados

- `docs/CONVENCION_NOMBRES_OBSERVABILIDAD.md` — nombres de observaciones, spans y jobs.
- `docs/OBSERVABILIDAD_PROD_GRAFANA_S3.md` — pipeline de exportación a Grafana Cloud y S3.
