# Alta de superadministradores por invitación — modelo de datos

**Estado:** especificación normativa. `db-migrations` la convierte en changesets **sin tomar ninguna
decisión de modelado**; `backend-feature` mapea las `@Entity` contra este documento. Si algo no está
escrito aquí, no se improvisa: se pregunta.

**Ámbito:** GLOBAL de plataforma. **Ninguna tabla de este documento lleva `company_id` ni ninguna FK
a `companies`.** No es una omisión: una FK a `companies` en un slice nuevo activa las cuatro reglas
duras BE-COV de ArchUnit sobre la feature entera y rompe el build.

**Verificado el 2026-08-24 contra `feature/alta-superadmin-por-invitacion`:** el último changeset es
`279_seed_company_self_service_permissions.xml`; **el primer número libre es el 280**.

**Decisiones humanas ya tomadas que este documento acata, no discute:**

1. **`system_users` gana una columna `email` nueva** — no se reutiliza `code`, que sigue siendo el
   identificador de login, ni se deja el correo solo en la tabla del token. Desarrollo en §4.3.
2. **El aprobador NO se modela.** Ni tabla, ni rol, ni FK, ni columna: el destinatario del enlace es
   `${VETSOFTWARE_PLATFORM_APPROVER_EMAIL}` en `application.yml`, y la autorización para aprobar es la
   posesión del token más el código de 6 dígitos. Desarrollo en §7.
3. **Tope de 5 intentos y bloqueo terminal permanente** (no temporal), **código hasheado** y comparado
   en tiempo constante. Desarrollo en §5.2 y §6.

---

## 0. El contrato que hay que satisfacer (ya mergeado en el front)

Fuente: `VetSoftwareFront/src/features/platform-access/`, escrito a mano porque el OpenAPI aún no lo
publica (`types/platform-access.types.ts:1-14`).

| Endpoint | Cuerpo / respuesta | Códigos que el front distingue |
|---|---|---|
| `POST /platform/access-request` | `{fullName, email, reason}` → 202 | `404` = formulario **cerrado** (`usePlatformAccess.ts:113`), `400` con `errors[]` |
| `GET /platform/access-request/validate?token=` | → `{fullName, email, reason, requestedAt}` | `404`/`410` = enlace muerto, **indistinguibles a propósito** (`usePlatformAccess.ts:78-81`) |
| `POST /platform/access-request/approve` \| `/reject` | `{token, code}` → 204 | `422` (+ `remainingAttempts` opcional), `429` = bloqueado, `404`/`410` = muerto |
| `GET /platform/invitation/validate?token=` | → `{email}` | `404`/`410` |
| `POST /platform/invitation/accept` | `{token, password}` → 204 | `404`/`410`, `400` con `errors[]` |

Límites que el front ya valida y que **deben existir también en la base** (`SolicitarAccesoView.vue:67-72`):
`fullName` 3..120 · `email` ≤ 150 · `reason` 20..500 · `code` exactamente 6 dígitos
(`usePlatformAccess.ts:30,46-48`).

Y un dato que decide medio modelo: **el mismo código sirve para aprobar y para rechazar**
(`platform-access.types.ts:32-37`). No hay un código por decisión.

---

## 1. Las invariantes, antes que las tablas

| # | Lo que no puede pasar jamás | Dónde queda garantizado |
|---|---|---|
| I1 | Dos filas con el mismo token (solicitud o invitación) | `UNIQUE` sobre `*_token_hash` |
| I2 | Que la base guarde un token utilizable en claro | Solo se persiste SHA-256 hex; el valor plano solo viaja en el correo |
| I3 | Más de `max_attempts` intentos de código sobre una solicitud | `CHECK (verification_attempts <= max_attempts)` + `UPDATE` atómico condicional (§6) |
| I4 | Una solicitud decidida sin fecha de decisión, o al revés | `CHECK` de par nulo (patrón `chk_subscriptions_cancel`, `242_create_subscriptions.xml`) |
| I5 | Una decisión que no sea `APPROVED` o `REJECTED` | `CHECK (decision IN (...))` — **no** `ENUM` de MySQL |
| I6 | Que una solicitud aprobada produzca **dos** superadministradores | `UNIQUE` sobre columna generada `consumed_request_id` (§4.2) |
| I7 | Que un `system_user` proceda de dos invitaciones | `UNIQUE (system_user_id)` en `platform_access_invitations` |
| I8 | Dos superadministradores con el mismo correo | `UNIQUE (email)` en `system_users` (§4.3) |
| I9 | Una invitación consumida sin usuario creado, o al revés | `CHECK` de par nulo |
| I10 | Que se borre la trazabilidad de un superadmin vivo | FK `ON DELETE RESTRICT` + predicado de purga (§8) |
| I11 | Un motivo de menos de 20 caracteres | `CHECK (CHAR_LENGTH(reason) >= 20)` |
| I12 | Que caduque antes de emitirse | `CHECK (expires_at > created_date)` |

**Invariantes que se declaran NO garantizadas por la base, y por qué.** Esto no es una omisión: es la
parte del método que obliga a escribirlo en vez de dejarlo implícito.

- **«Una sola solicitud viva por correo».** No es expresable. Depende de `NOW()` —una solicitud
  caducada debe poder repetirse— y el manual de MySQL 8.4 prohíbe funciones no deterministas en la
  expresión de una columna generada: *«Examples of functions that are nondeterministic and fail this
  definition: `CONNECTION_ID()`, `CURRENT_USER()`, `NOW()`»*
  (<https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html>). Un `UNIQUE` parcial
  sobre `decision IS NULL` bloquearía para siempre a quien pidió acceso, caducó y vuelve a pedirlo.
  **Se resuelve en la aplicación**: si existe una solicitud viva para ese correo, se reenvía la misma
  en vez de crear otra. El abuso del buzón lo acota `LoginRateLimitFilter`, que ya tiene el patrón
  exacto (`RECOVER_CODE_LIMIT`, 3/hora por correo, `LoginRateLimitFilter.java:65-68`).
- **«El `code` del `system_user` que se va a crear está libre».** Es unicidad entre dos tablas y MySQL
  no tiene assertions. Se resuelve con el patrón de la casa (`EmployeeCodeGenerator.generateAvailable`,
  sufijo `-2`, `-3`… con predicado de disponibilidad) y el `UNIQUE (code)` de `system_users` como
  última línea: si dos aceptaciones colisionan, la segunda revienta con violación de unicidad y se
  reintenta.
- **«El correo es sintácticamente válido».** Se queda en Bean Validation. Un `CHECK` con `LIKE` sobre
  correos rechaza direcciones legítimas raras, que es un fallo peor que aceptar una inválida — el mismo
  criterio que ya documenta el front (`SolicitarAccesoView.vue:48-52`).

---

## 2. ¿Una tabla o dos? — **dos**, y la tercera se rechaza

**Dos tablas: `platform_access_requests` y `platform_access_invitations`.**

Los tres argumentos, en orden de peso:

1. **Cardinalidad real: no es 1:1, es 1:0..N.** Una invitación caducada se vuelve a emitir con un token
   nuevo. En una sola tabla eso obliga a reescribir `token_hash` sobre la fila, es decir, a **destruir el
   registro de qué token se envió antes** — o a duplicar la solicitud entera para reemitir. Con dos
   tablas, reemitir es un `INSERT`. Este argumento por sí solo cierra la discusión.
2. **Dos secretos con dos `UNIQUE` distintos y dos destinatarios distintos.** En una sola tabla habría
   `approval_token_hash` e `invitation_token_hash`, cada uno con su índice único y su `NULL` mientras no
   exista, y la pregunta «¿de quién es este token?» dejaría de tener una respuesta única. Además el token
   del aprobador y el del solicitante **no van a la misma persona**: fundirlos en una fila es fundir dos
   credenciales de dos sujetos.
3. **Dependencia funcional.** `token_hash`, `expires_at` y `consumed_at` de la invitación dependen del
   *evento de invitar*, no de la solicitud. Meterlos en la fila de la solicitud es exactamente la
   violación de 3NF que produce el bloque de seis columnas nulas que no significan nada mientras el
   estado es `PENDING`.

**La tercera tabla que se rechaza:** un `platform_approval_tokens` aparte. El token del aprobador es
1:1 con la solicitud, nace en la misma transacción y muere con ella; no se reemite de forma
independiente (un reenvío manda **el mismo** enlace). Vive por tanto como columnas de la solicitud. Si
algún día hay varios aprobadores o reemisión con token nuevo, el camino de expansión es limpio: tabla
nueva, doble escritura, backfill, corte de lectura, `DROP` de las columnas — el guion de los cuatro
pasos de Stripe (<https://stripe.com/blog/online-migrations>). **Se documenta ahora precisamente para
que ese día no haya que inventarlo.**

---

## 3. La máquina de estados — sin columna `status`

**No hay columna `status`, y es deliberado.** El estado debe ser derivable y no contradecible; una
columna `status` junto a `decision`, `decided_at`, `expires_at` y `verification_attempts` es **una quinta
fuente de verdad que puede desincronizarse de las otras cuatro** y que ninguna constraint puede mantener
en línea con el reloj.

Estado de `platform_access_requests`, derivado íntegramente de la fila y del instante de lectura:

```
BLOCKED   ⇐ verification_attempts >= max_attempts        (persistente, gana a todo)
APPROVED  ⇐ decision = 'APPROVED'
REJECTED  ⇐ decision = 'REJECTED'
EXPIRED   ⇐ decision IS NULL AND now > expires_at
PENDING   ⇐ el resto
```

Precedencia obligatoria: **BLOCKED antes que EXPIRED antes que PENDING**. Un `429` tiene que seguir
siendo `429` después de caducar el enlace, o el front vuelve a ofrecer el formulario
(`usePlatformAccess.ts:152-159`, que evalúa `429` antes que `422`).

Estado de `platform_access_invitations`:

```
CONSUMED  ⇐ consumed_at IS NOT NULL
EXPIRED   ⇐ consumed_at IS NULL AND now > expires_at
PENDING   ⇐ el resto
```

Consecuencia directa: **ninguna de las dos tablas tiene una sola columna booleana**. Con eso, la trampa
de `TINYINT(1)` que documenta el proyecto (`preferred_boolean_jdbc_type: TINYINT`, `application.yml:85`;
con display width Connector/J reporta `Types.BIT` y `ddl-auto: validate` tumba el arranque) **no puede
darse aquí**. Tampoco hay `enabled`: estas tablas no son catálogo de negocio sino credencial y bitácora,
igual que `refresh_tokens`, `password_reset_tokens` y `email_verification_tokens`, ninguna de las cuales
lo lleva.

---

## 4. DDL objetivo

Convenciones que se dan por leídas y no se repiten en cada ficha: PK `id BIGINT AUTO_INCREMENT` siempre
· nulabilidad explícita, sin implícitos · toda FK con `ON DELETE RESTRICT ON UPDATE RESTRICT` declarado
· índices `ix_`/`uq_` con nombre explícito · charset y collation **heredados del servidor**
(`utf8mb4_0900_ai_ci`), como las 105 tablas actuales — ningún `CREATE TABLE` del repo los declara.

> **Sobre la collation, un aviso que sí es operativo:** `utf8mb4_0900_ai_ci` es *accent-insensitive* y
> *case-insensitive*. Sobre `email` eso significa que `Jose@x.com`, `jose@x.com` y `josé@x.com` son **la
> misma clave** para `UNIQUE (email)` de `system_users`. Para correos es más restrictivo de lo estricto,
> nunca menos: no abre ningún agujero, pero la aplicación debe saberlo y no asumir que puede haber dos.
> Sobre `*_token_hash` es irrelevante: el hash lo produce siempre `HexFormat.formatHex` en minúsculas
> (`PasswordResetTokens.java:34-42`) y el usuario nunca envía hex, envía el token plano.

### 4.1 `platform_access_requests`

Slice propuesto: `com.vetsoftware.app.platformaccess`.

| Columna | Tipo | Nulabilidad | Por qué exactamente ese tipo |
|---|---|---|---|
| `id` | `BIGINT` `AUTO_INCREMENT` | PK | Clave *clustered* secuencial; la casa nunca usa UUID |
| `full_name` | `VARCHAR(120)` | `NOT NULL` | El máximo que el front valida, no `255` por inercia |
| `email` | `VARCHAR(150)` | `NOT NULL` | Igual que `owners.email` (`030_create_owners.xml:15`) y `suppliers.email`, y el techo del front |
| `reason` | `VARCHAR(500)` | `NOT NULL` | `MOTIVO_MAX = 500`. No `TEXT`: cabe en fila y evita el off-page de InnoDB |
| `approval_token_hash` | `VARCHAR(64)` | `NOT NULL` | SHA-256 en hex = 64 chars exactos. Idéntico a las tres tablas de token vivas |
| `verification_code_hash` | `VARCHAR(255)` | `NOT NULL` | **bcrypt**, no SHA-256 (§5). 255 por coherencia con `system_users.hash_password` |
| `verification_attempts` | `INT` | `NOT NULL` `DEFAULT 0` | Contador del `422`/`429` |
| `max_attempts` | `SMALLINT` | `NOT NULL` `DEFAULT 5` | Política **congelada al emitir** (§6) |
| `expires_at` | `DATETIME(6)` | `NOT NULL` | Caducidad del enlace del aprobador. 72 h propuestas |
| `decision` | `VARCHAR(10)` | `NULL` | `APPROVED` \| `REJECTED`. **Nunca `ENUM` de MySQL**: añadir un valor sería un `ALTER` de tabla |
| `decided_at` | `DATETIME(6)` | `NULL` | Par obligado de `decision` |
| `created_date` | `DATETIME(6)` | `NOT NULL` `DEFAULT CURRENT_TIMESTAMP(6)` | Es el `requestedAt` del contrato |
| `version` | `BIGINT` | `NOT NULL` `DEFAULT 0` | `@Version`. Esta fila **sí** se edita dos veces (§6) |

```sql
-- ⚠️ Referencia. El changeset lo escribe db-migrations con <createTable> + <sql>.
CREATE TABLE platform_access_requests (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    full_name              VARCHAR(120) NOT NULL,
    email                  VARCHAR(150) NOT NULL,
    reason                 VARCHAR(500) NOT NULL,
    approval_token_hash    VARCHAR(64)  NOT NULL,
    verification_code_hash VARCHAR(255) NOT NULL,
    verification_attempts  INT          NOT NULL DEFAULT 0,
    max_attempts           SMALLINT     NOT NULL DEFAULT 5,
    expires_at             DATETIME(6)  NOT NULL,
    decision               VARCHAR(10)  NULL,
    decided_at             DATETIME(6)  NULL,
    created_date           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_platform_access_requests PRIMARY KEY (id),
    CONSTRAINT uq_par_approval_token_hash  UNIQUE (approval_token_hash),
    CONSTRAINT chk_par_decision_values
        CHECK (decision IS NULL OR decision IN ('APPROVED','REJECTED')),
    CONSTRAINT chk_par_decision_pair            -- I4, patrón chk_subscriptions_cancel
        CHECK ((decision IS NULL     AND decided_at IS NULL)
            OR (decision IS NOT NULL AND decided_at IS NOT NULL)),
    CONSTRAINT chk_par_attempts                 -- I3
        CHECK (verification_attempts >= 0 AND verification_attempts <= max_attempts),
    CONSTRAINT chk_par_max_attempts
        CHECK (max_attempts BETWEEN 1 AND 10),
    CONSTRAINT chk_par_expiry                   -- I12
        CHECK (expires_at > created_date),
    CONSTRAINT chk_par_decided_after
        CHECK (decided_at IS NULL OR decided_at >= created_date),
    CONSTRAINT chk_par_reason_min               -- I11
        CHECK (CHAR_LENGTH(reason) >= 20),
    CONSTRAINT chk_par_full_name_min
        CHECK (CHAR_LENGTH(full_name) >= 3)
);
```

**Ningún índice más que el `UNIQUE` del token.** Ni sobre `email`, ni sobre `decision`, ni sobre
`expires_at`. Justificación con el número: esta tabla crece **una fila por persona que pide acceso de
plataforma**, es decir, decenas al año — no miles al día como `refresh_tokens`, que es la razón por la
que `217_add_token_retention_support.xml` sí indexó `expires_at` allí. Un índice de listado aquí paga
escritura y espacio en cada `INSERT` a cambio de ahorrar el escaneo de una tabla de tres cifras de
filas, que InnoDB resuelve en una sola página. Se añadirá el día que exista un endpoint de listado y la
tabla pase de ~10.000 filas, **no antes**.

### 4.2 `platform_access_invitations`

| Columna | Tipo | Nulabilidad | Notas |
|---|---|---|---|
| `id` | `BIGINT` `AUTO_INCREMENT` | PK | |
| `access_request_id` | `BIGINT` | `NOT NULL` | FK → `platform_access_requests(id)` |
| `token_hash` | `VARCHAR(64)` | `NOT NULL` | SHA-256 hex |
| `expires_at` | `DATETIME(6)` | `NOT NULL` | 7 días propuestos |
| `consumed_at` | `DATETIME(6)` | `NULL` | Un solo uso |
| `system_user_id` | `BIGINT` | `NULL` | FK → `system_users(id)`. Se rellena al consumir |
| `consumed_request_id` | `BIGINT` `GENERATED … STORED` | derivada | Emula el índice único parcial (I6) |
| `created_date` | `DATETIME(6)` | `NOT NULL` `DEFAULT CURRENT_TIMESTAMP(6)` | |
| `version` | `BIGINT` | `NOT NULL` `DEFAULT 0` | Ver §9: aquí se propone **exención E3_TOKEN**, no `@Version` |

**`email` NO se duplica aquí.** El `GET /platform/invitation/validate` devuelve `{email}` y lo resuelve
con un `JOIN` por PK a la solicitud: una fila, un acceso al índice *clustered*, coste cero. Duplicar la
columna sería desnormalizar sin una sola medición que lo pida, que es la definición de deuda. La FK
`ON DELETE RESTRICT` garantiza además que la fila padre no puede desaparecer bajo ella.

```sql
CREATE TABLE platform_access_invitations (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    access_request_id   BIGINT      NOT NULL,
    token_hash          VARCHAR(64) NOT NULL,
    expires_at          DATETIME(6) NOT NULL,
    consumed_at         DATETIME(6) NULL,
    system_user_id      BIGINT      NULL,
    created_date        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version             BIGINT      NOT NULL DEFAULT 0,
    -- I6: MySQL no tiene indices unicos parciales. Se emula con una columna generada
    -- que vale NULL fuera de alcance (MySQL admite multiples NULL en un indice unico).
    -- Mismo patron que 226_add_unique_active_appointment_slot, 210 y 206.
    -- La expresion es DETERMINISTA (no usa NOW()), como exige el manual.
    consumed_request_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN consumed_at IS NULL THEN NULL ELSE access_request_id END
    ) STORED,
    CONSTRAINT pk_platform_access_invitations PRIMARY KEY (id),
    CONSTRAINT uq_pai_token_hash       UNIQUE (token_hash),
    CONSTRAINT uq_pai_consumed_request UNIQUE (consumed_request_id),   -- I6
    CONSTRAINT uq_pai_system_user      UNIQUE (system_user_id),        -- I7
    CONSTRAINT chk_pai_consumption_pair                               -- I9
        CHECK ((consumed_at IS NULL     AND system_user_id IS NULL)
            OR (consumed_at IS NOT NULL AND system_user_id IS NOT NULL)),
    CONSTRAINT chk_pai_expiry
        CHECK (expires_at > created_date),
    CONSTRAINT chk_pai_consumed_after
        CHECK (consumed_at IS NULL OR consumed_at >= created_date),
    CONSTRAINT fk_pai_request FOREIGN KEY (access_request_id)
        REFERENCES platform_access_requests (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pai_system_user FOREIGN KEY (system_user_id)
        REFERENCES system_users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);
```

**Por qué `uq_pai_consumed_request` es la constraint más importante del documento.** Sin ella, reemitir
una invitación —que es un `INSERT`, §2— deja dos tokens vivos para la misma solicitud aprobada y **dos
superadministradores** si se usan los dos. Con ella, se pueden emitir las invitaciones que hagan falta y
**como mucho una llega a consumirse**: la segunda choca contra el índice único y aborta la transacción.
Es la única forma de expresar «a lo sumo un usuario por aprobación» sin depender de una lectura previa
en Java, que la concurrencia se come.

**Índices resultantes**, y ni uno más:

| Índice | Columnas | Qué consulta sirve | Estado |
|---|---|---|---|
| `pk_platform_access_invitations` | `(id)` | acceso por PK | propuesto |
| `uq_pai_token_hash` | `(token_hash)` | `GET /invitation/validate`, `POST /invitation/accept` | propuesto |
| `uq_pai_consumed_request` | `(consumed_request_id)` | ninguna — existe por I6 | propuesto |
| `uq_pai_system_user` | `(system_user_id)` | existe por I7 **y** cubre el índice que InnoDB exigiría para `fk_pai_system_user` | propuesto |
| *(automático)* `fk_pai_request` | `(access_request_id)` | «invitaciones de esta solicitud», al reemitir | **lo crea InnoDB solo**; que nadie lo añada a mano ni lo borre como redundante |

Sobre esa última fila, el criterio ya escrito en la casa (`docs/db/suscripciones-tablas.md`, Apéndice C):
InnoDB crea el índice de la columna hija si no existe
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>). Declararlo además a mano
deja dos índices donde bastaba uno y `sys.schema_redundant_indexes` lo marcará en la próxima auditoría.

### 4.3 `system_users` — la columna `email` (decisión humana tomada)

**Hallazgo estructural, y es el de mayor radio de este documento.** `system_users` tiene hoy exactamente
cuatro columnas de negocio: `code VARCHAR(50) UNIQUE`, `hash_password`, `created_date`, más `enabled`,
`version` y `auth_version` (`008_create_system_users.xml`, `068`, `208`, `225`;
`SystemUserJpaEntity.java:8-34`). **No tiene `email` ni `full_name`.** Se comprueba también aguas abajo:
`JpaSystemUserProfileQueryPort.java:19` construye `SystemUserProfile(u.getId(), u.getCode())` — el
`name` que el front recibe en `/auth/me` de un superadministrador **es su `code`**.

Sin `email` en `system_users` el flujo no se puede implementar correctamente:

- no hay forma de responder a «¿ya existe un superadministrador con este correo?» sin inventarse un
  `LIKE` sobre `code`;
- la invariante I8 —«dos superadministradores con el mismo correo»— no tiene dónde vivir;
- el `full_name` que el solicitante escribe y que el aprobador lee se tiraría a la basura al crear el
  usuario, y `/auth/me` seguiría devolviendo un código como nombre.

**`code` NO se reutiliza para el correo.** `code` sigue siendo el identificador de login
(`LoginSystemUserRequest`, `auth.types.ts:37-40`), es `VARCHAR(50)` y ya está poblado en todas las filas
existentes con valores que no son correos. Meter un correo ahí sería el antipatrón de la columna con dos
significados y rompería el login de los superadministradores actuales. Columna nueva.

```sql
-- Paso 1 (INSTANT, sin bloqueo, DML concurrente permitido)
ALTER TABLE system_users
    ADD COLUMN email     VARCHAR(150) NULL,
    ADD COLUMN full_name VARCHAR(120) NULL;

-- Paso 2, changeset aparte (IN PLACE, sin reconstruir, DML concurrente permitido)
ALTER TABLE system_users
    ADD CONSTRAINT uq_system_users_email UNIQUE (email);
```

#### Longitud: `VARCHAR(150)`, y por qué NO `VARCHAR(100)` como `employees`

El esquema **no es coherente consigo mismo** en esto, así que hay que elegir con criterio y no por
mimetismo:

| Tabla | Columna | Tipo | Origen |
|---|---|---|---|
| `employees` | `email` | **`VARCHAR(100)`** | `015_create_employees.xml:21` |
| `owners` | `email` | `VARCHAR(150)` | `030_create_owners.xml:15` |
| `suppliers` | `email` | `VARCHAR(150)` | `197_create_suppliers.xml:18` |

**Se elige `VARCHAR(150)`**, con dos razones que no admiten discusión:

1. **El front ya acepta 150** (`maxLength(form.value.email, 'El correo electrónico', 150)`,
   `SolicitarAccesoView.vue:71`) y `platform_access_requests.email` es `VARCHAR(150)`. Con `VARCHAR(100)`
   en destino, **un correo de 101 a 150 caracteres pasa la solicitud, pasa la aprobación, llega a la
   pantalla de crear contraseña y revienta en el `INSERT` final** — es decir, falla en el único momento
   del flujo en el que ya no hay reintento posible y el usuario ya escribió su contraseña. Copiar un
   valor de una columna a otra más corta es una pérdida de datos esperando su turno.
2. **El 100 de `employees` no es un descuido que haya que imitar: es un límite deliberado y coherente de
   principio a fin**, y comprobado —`CreateEmployeeRequest.java:17` y `UpdateEmployeeRequest.java:10`
   declaran `@Size(max = 100)`, y `Employee.java:36-37,81-82` lo repite como invariante de dominio. Es
   decir, en `employees` el 100 está sostenido por tres capas; aquí no habría ninguna que lo sostenga,
   porque el front ya manda 150. Copiar el número sin copiar el resto de la cadena no es coherencia, es
   cargo cult.

El coste de la diferencia es cero: `VARCHAR` almacena la longitud real, no la declarada, y en un índice
`utf8mb4` la clave llega hasta 600 bytes — muy por debajo del límite de 3.072 bytes de InnoDB con
`DYNAMIC`. No hay ningún argumento de espacio a favor de 100.

#### Índice único: **sí**, y los `NULL` son exactamente lo que se quiere

`UNIQUE (email)` es la única forma de garantizar I8 —«dos superadministradores con el mismo correo»— bajo
concurrencia: dos aceptaciones simultáneas de dos invitaciones al mismo correo pasan las dos el `SELECT`
previo y crean las dos la cuenta. Solo la constraint las separa.

Y el comportamiento con `NULL` **no es un efecto colateral que haya que tolerar: es el mecanismo que hace
posible el despliegue sin downtime.** El manual de MySQL 8.4 es explícito: *«A `UNIQUE` index permits
multiple `NULL` values for columns that can contain `NULL`»*
(<https://dev.mysql.com/doc/refman/8.4/en/create-index.html>), y la tabla 15.2 lo confirma para InnoDB
con índices BTREE. Por tanto:

- las **N filas heredadas** de `system_users` conviven todas con `email IS NULL` bajo el mismo índice
  único, sin excepción, sin backfill y sin inventarse un correo para nadie;
- solo las cuentas creadas por invitación llevan correo, y entre ellas la unicidad **sí** es estricta;
- es el mismo hecho del motor en el que se apoya el patrón de unicidad condicional de `226`, `210` y
  `206`. Aquí no hace falta columna generada porque el «fuera de alcance» ya es `NULL` de forma natural.

Lo que ese `NULL` **no** garantiza, y hay que saberlo: no impide que existan dos cuentas heredadas de la
misma persona. Es información que la base no tiene y no la puede inventar.

**Ambas columnas nacen `NULL`, y eso no es pereza: es expand/contract.**

- Hibernate con `ddl-auto: validate` **no falla por columnas de más en la base**, solo por columnas que
  falten. Los changesets 280 y 281 se pueden desplegar **antes** de que `backend-feature` toque una sola
  `@Entity`. Ese es el paso *expand* de Parallel Change (<https://martinfowler.com/bliki/ParallelChange.html>).
- El *contract* —poner `email NOT NULL`— **no se hace nunca**, o se hace el día que alguien decida qué
  correo tienen los superadministradores heredados. Se deja escrito aquí para que no se intente por
  higiene.

#### De dónde sale el correo con el que se crea la cuenta

**Regla dura, y va en el documento porque es una decisión de datos, no de código:**

> El `system_users.email` de la cuenta nueva se copia de la fila de `platform_access_requests` a la que
> apunta la invitación **que trae el token**. Nunca, bajo ninguna circunstancia, del cuerpo de
> `POST /platform/invitation/accept`.

El cuerpo de esa petición es `{token, password}` y **no lleva correo**
(`platform-access.types.ts:48-52`): si algún día alguien lo añade, este documento dice que se ignora. Un
correo que viaja en el cuerpo permitiría a quien posee una invitación legítima para `a@x.com` crear la
cuenta de `b@x.com`, es decir, elegir la identidad del superadministrador que va a nacer. La cadena que
la base garantiza es `token_hash → platform_access_invitations.access_request_id →
platform_access_requests.email`, toda ella por clave única y con `ON DELETE RESTRICT`: no hay eslabón que
el cliente pueda torcer. Es el mismo motivo por el que §4.2 no duplica `email` en la invitación — un
segundo sitio donde guardarlo es un segundo sitio donde puede acabar siendo otro.

#### ¿Abre esta columna una vía de enumeración de cuentas de plataforma?

**Por sí sola no; el riesgo está en cómo se comporte el endpoint público que la consulta.** El análisis,
punto por punto:

- **`POST /platform/access-request` es el único endpoint anónimo que puede tocar este dato**, cuando el
  backend comprueba «¿ya hay un superadministrador con este correo?». **Debe responder `202` idéntico en
  los tres casos** —correo nuevo, correo de un superadministrador vivo, correo de uno dado de baja— sin
  `ProblemDetail`, sin `409`, sin cabecera distinta y sin cuerpo distinto. El front ya está construido
  sobre esa premisa: solo distingue `404` (formulario cerrado) y `400` (errores de campo)
  (`usePlatformAccess.ts:107-119`). **Un `409` «ese correo ya tiene cuenta» convertiría el formulario en
  un directorio de superadministradores de la plataforma**, que es la lista de objetivos más valiosa del
  sistema.
- **Canal temporal.** Si la rama «ya existe» sale sin enviar correo y la otra hace una llamada a Resend,
  la diferencia de latencia es medible y el oráculo vuelve por la puerta de atrás. Mitigación: **el
  envío de correo ya es asíncrono y best-effort en este repo** (`ResendCodeRecoveryEmailSender`,
  javadoc), así que las dos ramas devuelven al cliente en el mismo punto. Que se mantenga así es
  responsabilidad de `backend-feature`; queda escrito aquí como requisito, no como sugerencia.
- **La collation ayuda, no estorba.** `utf8mb4_0900_ai_ci` hace que `Jose@x.com`, `jose@x.com` y
  `josé@x.com` colisionen bajo `uq_system_users_email`. Un atacante no puede usar mayúsculas ni tildes
  para colar un duplicado ni para sondear variantes: las tres son la misma clave.
- **Ningún endpoint nuevo de «¿está libre este correo?».** No existe, no debe existir, y no hay ninguna
  consulta en §11 que lo requiera. La comprobación Q8 es **interna** y solo se ejecuta dentro del flujo
  de aceptación, es decir, con un token válido en la mano.
- **El error de clave duplicada al aceptar no es un oráculo:** para provocarlo hay que poseer una
  invitación válida, y quien la posee ya conoce el correo — es el suyo.

Resumen: la columna no crea la vía de enumeración; la crearía un endpoint que respondiese distinto según
lo que hay en ella. El modelo permite el comportamiento correcto y el contrato del front ya lo exige.

**`UNIQUE (email)` liso, sin columna generada por `enabled`.** Es deliberado: `system_users` borra en
lógico (`@SQLDelete … SET enabled = false`, `SystemUserJpaEntity.java:10`) y un superadministrador dado
de baja **debe seguir reteniendo su correo**. Si vuelve, el camino correcto es
`SystemUserJpaRepository.reactivate()` —que ya existe, `SystemUserJpaRepository.java:59-66`— no crear un
segundo usuario con la misma identidad. Este es el único sitio del documento donde se rechaza el patrón
de unicidad condicional de la casa, y se rechaza por semántica, no por comodidad.

**Lo que este documento NO resuelve y hay que decidir con producto:** el login de la consola pide
`code` + `password` (`VetSoftwareFront/src/features/auth/types/auth.types.ts:37-40`) y la pantalla de
éxito manda directamente a `Iniciar sesión` (`AceptarInvitacionView.vue:298`) **sin haber enseñado nunca
al usuario su `code`**. `POST /platform/invitation/accept` responde 204 sin cuerpo. Alguien tiene que
comunicarle ese código —lo natural es el correo de bienvenida— o el login debe aceptar el correo. Está
levantado como issue. Impacto en el modelo si se decide lo primero: **ninguno**. Si se decide reservar
el código en la aprobación en vez de generarlo al aceptar, entonces sí hace falta una columna
`assigned_code VARCHAR(50)` en `platform_access_invitations` con su unicidad parcial — por eso queda
escrito aquí y no se descubre a mitad de la implementación.

---

## 5. Los secretos: token de 256 bits y código de 20 bits, tratados distinto

### 5.1 Tokens (`approval_token_hash`, `token_hash`)

**Se reutiliza literalmente el patrón de la casa**, `PasswordResetTokens.java:24-42`: 32 bytes de
`SecureRandom` en Base64 URL-safe sin padding (~43 chars) para el valor plano, SHA-256 en hex (64 chars)
para la columna. **256 bits de entropía**, muy por encima de los 128 exigidos, y `VARCHAR(64)` con
`UNIQUE`.

SHA-256 sin salt es **lo correcto aquí y no una concesión**: el secreto es aleatorio de 256 bits, así que
no existe diccionario ni fuerza bruta que atacar; el hash está para que un volcado de la tabla no
entregue tokens usables (I2). OWASP pide exactamente eso y nada más: *«Generated using a
cryptographically secure random number generator»*, *«Stored in a secure manner»*, *«Invalidated after
they have been used»* (<https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html>).

### 5.2 El código de 6 dígitos — **bcrypt, y aquí sí importa**

Un código de 6 dígitos son **10^6 combinaciones, ~20 bits**. Guardarlo con SHA-256 es prácticamente
guardarlo en claro: recorrer el millón de hashes de un volcado es cuestión de milisegundos. Guardarlo
literalmente en claro convierte cualquier lectura de la tabla en un ascenso a superadministrador.

**Decisión: `verification_code_hash VARCHAR(255)`, bcrypt, con el `PasswordHasher` que el repo ya tiene**
—el mismo que usa `CreateSystemUserService.java:25`—. Con coste 10, forzar el millón de combinaciones
desde un volcado cuesta horas de CPU en vez de milisegundos, y el coste en línea es **un bcrypt por
intento, con un máximo de 5 intentos por solicitud** sobre una tabla de decenas de filas: irrelevante
incluso en la `db.t4g.small` de este proyecto.

Consecuencia de modelado que hay que entender: bcrypt lleva salt, así que **no se puede buscar por el
hash del código**. No hace falta: la fila **siempre** se localiza primero por `approval_token_hash`
(256 bits) y el código se verifica **contra esa fila**. Es decir, para atacar el código por fuerza bruta
hay que poseer ya el token. Esa ordenación es la que hace que 20 bits sean suficientes.

**Comparación en tiempo constante, y la columna la facilita.** `BCrypt.checkpw` (el que hay detrás de
`PasswordHasher`) compara el digest completo sin cortocircuitar en el primer byte distinto, así que la
verificación no filtra por latencia cuántos dígitos se acertaron. Es otra razón por la que **no** se
guarda el código en claro: un `equals` sobre `String` en Java sí termina antes en cuanto encuentra una
diferencia, y con 5 intentos por solicitud y un canal medible eso es información real que se regala. Que
la columna sea un hash bcrypt hace que la comparación correcta sea también la comparación natural.

**Alternativa considerada y descartada:** HMAC-SHA256 con *pepper* fuera de la base. Es más barato que
bcrypt y más fuerte frente a un volcado limpio, pero introduce un secreto nuevo que gestionar, rotar y
documentar en `GESTION_DE_SECRETOS.md`, y este repo no tiene esa pieza. bcrypt reutiliza infraestructura
existente y no añade superficie operativa.

**Aviso de diseño que no es de base de datos pero condiciona su lectura:** si el código de 6 dígitos
viaja **en el mismo correo** que el enlace `?token=`, no es un segundo factor — es el mismo canal, el
mismo mensaje y el mismo lector. OWASP desaconseja explícitamente el PIN por correo y lo describe como
algo que se manda *«through a side-channel such as SMS»*. Con el diseño actual el código protege contra
el clic accidental y contra el enlace reenviado por error, no contra un buzón comprometido. **La barrera
real sigue siendo el token.** Si producto quiere que el código sea de verdad un segundo factor, tiene
que salir por otro canal o en otro momento. Levantado como issue.

---

## 6. Los intentos, el `422` y el `429`

### `max_attempts` en la fila, no en el `application.yml`

La política se **congela al emitir**. Motivo, y es el mismo por el que este repo guarda `expires_at` en
cada tabla de token en vez de calcular `created_date + TTL` con la configuración de hoy: **el límite con
el que se emitió una credencial es una propiedad de esa credencial, no del despliegue actual**. Si
mañana la política baja de 5 a 3, una solicitud bloqueada con 4 intentos seguiría bloqueada, y una
emitida con 5 conservaría sus 5. Sin la columna, cambiar una constante en un `.yml` **desbloquea o
bloquea retroactivamente** credenciales ya emitidas — un control de seguridad que muta hacia atrás.
Coste: 2 bytes por fila en una tabla de decenas de filas.

Con eso, `BLOCKED` es **derivable de la fila sola**: `verification_attempts >= max_attempts`. No hace
falta ninguna bandera `blocked`, que es justo la bandera redundante que hay que evitar.

**El bloqueo es terminal y permanente, no «una hora».** Decisión acordada: agotados los 5 intentos, el
token queda muerto para siempre y solo cabe pedir acceso de nuevo. Eso tiene una consecuencia de modelado
que conviene ver: **no hay columna `blocked_until`, y no la debe haber**. Un bloqueo temporal exigiría un
`DATETIME` que se compara con el reloj, es decir, devolvería el estado a depender del instante de lectura
—justo lo que §3 elimina— y abriría la puerta a que 5 intentos por hora se conviertan en fuerza bruta
lenta sobre 10^6 combinaciones. Con el bloqueo permanente, `verification_attempts >= max_attempts` es un
predicado sobre datos, no sobre el tiempo, y ninguna espera lo revierte.

### El `UPDATE` que hace de verdad el trabajo

Contar intentos con `SELECT` + `if` + `save()` es un *read-then-write*: dos peticiones simultáneas con el
código equivocado leen `attempts = 4`, las dos escriben `5`, y se han gastado 6 intentos. La forma
correcta —y la que `chk_par_attempts` respalda— es un `UPDATE` condicional atómico cuyo `WHERE` **es** la
invariante:

```sql
-- Intento fallido: incrementa SOLO si queda margen. rowcount = 0 ⇒ ya estaba bloqueada ⇒ 429.
UPDATE platform_access_requests
   SET verification_attempts = verification_attempts + 1,
       version               = version + 1
 WHERE id = :id
   AND verification_attempts < max_attempts;

-- Decision: se aplica SOLO si sigue siendo aplicable. rowcount = 0 ⇒ ya decidida,
-- caducada o bloqueada ⇒ 404/410/429 segun el estado que se relea.
UPDATE platform_access_requests
   SET decision   = :decision,          -- 'APPROVED' | 'REJECTED'
       decided_at = :now,
       version    = version + 1
 WHERE id = :id
   AND decision IS NULL
   AND expires_at > :now
   AND verification_attempts < max_attempts;
```

Los dos **mueven `version`** a propósito: es exigencia de la regla dura `UPDATE_MASIVO_MUEVE_LA_VERSION`,
y el motivo está escrito con todas sus letras en el precedente de la casa
(`SystemUserJpaRepository.java:24-45`): sin mover la versión, un `save()` cargado antes del `UPDATE`
reescribe la columna con su valor viejo y su `WHERE version = ?` casa igual. `backend-feature` debe
declararlos con `@Modifying(flushAutomatically = true, clearAutomatically = true)`, como todos los del
repo.

**`remainingAttempts` no necesita columna.** Es `max_attempts - verification_attempts` releído tras el
`UPDATE`. El front ya trata el campo como opcional (`usePlatformAccess.ts:83-94`) y mapea
`remainingAttempts === 0` al mismo estado que el `429`, así que las dos rutas convergen.

---

## 7. El aprobador NO se modela · el interruptor del formulario sí

**Decisión humana tomada, y el modelo la acata: el aprobador no existe como dato.**

- **No hay tabla de aprobadores.** No hay rol, no hay permiso de aprobación, no hay lista.
- **No hay FK desde `platform_access_requests` a `system_users`.** Ni `approver_id`, ni `approved_by`,
  ni nada equivalente.
- **No hay columna `approver_email`.** El destinatario del enlace es un correo fijo de configuración,
  `${VETSOFTWARE_PLATFORM_APPROVER_EMAIL}` en `application.yml` — el mismo estilo que
  `vetsoftware.code-recovery.login-url` (`ResendCodeRecoveryEmailSender.java:56`).
- **La autorización para aprobar es la posesión del token más el código de 6 dígitos.** Eso es todo, y
  por eso no hay sujeto que modelar: no hay un «quién», hay un «quien tenga esto».

Esa decisión es coherente con el resto del sistema —es exactamente el modelo de
`password_reset_tokens`, donde la posesión del token *es* la autorización
(`PasswordResetToken.java:5-10`)— y tiene además la virtud de que el correo que acuña
superadministradores vive en el parameter store y no en una tabla: **un `UPDATE` en la base no basta
para redirigirlo**, y la base es alcanzable por más caminos que la configuración de despliegue.

> **Lo que se pierde, dicho sin adornos.** Sin instantánea del correo del aprobador, la pregunta «¿a
> qué dirección se envió el enlace que creó a este superadministrador?» solo se puede responder mirando
> el historial de `application.yml` y de SSM en la fecha de la aprobación. Si el equipo decide más
> adelante que esa traza hace falta, **volver atrás es barato y no rompe nada**: es un `ADD COLUMN
> approver_email VARCHAR(150) NULL`, que MySQL 8.4 resuelve con `ALGORITHM=INSTANT`, sin reconstruir y
> con DML concurrente. Se deja escrito aquí para que el día que se pida no haya que redecidirlo.

**El interruptor del formulario sí es un dato, y no necesita tabla nueva.** El repo ya tiene almacén
clave-valor global: `system_configurations` (`149_alter_system_configurations_to_key_value.xml`), con
`UNIQUE (property_name)` y sin `company_id` — pensado exactamente para *«distintos ajustes del sistema
(UVT, umbrales, flags) sin nuevas tablas»*.

**`platform.access-request.open` = `'true'|'false'`** → una fila en `system_configurations`. Es la
fuente del `404` = «formulario cerrado» que el front ya sabe pintar (`usePlatformAccess.ts:113`).
Cambiable en caliente, sin despliegue, que es justo lo que se le pide a un interruptor de emergencia.
No lleva `CHECK` sobre `value` porque la columna es genérica y compartida con el UVT y con todo lo
demás: validarla aquí obligaría a un `CHECK` condicionado por `property_name` que habría que tocar en
cada ajuste nuevo. La interpretación del texto es de la aplicación, y ante un valor ilegible **el
formulario se considera cerrado** (fallo seguro).

---

## 8. Retención — son datos personales de gente que quizá nunca fue aprobada

`full_name`, `email` y `reason` son datos personales (Ley 1581 de 2012, Habeas Data — producto
colombiano). Conservarlos indefinidamente para alguien a quien se rechazó no tiene justificación.

**Política propuesta**, ejecutada por el `TokenCleanupJob` que ya existe, con el mismo patrón acotado de
`TokenCleanupRepository` (`DELETE … ORDER BY id LIMIT ?`, para no sostener bloqueos largos):

| Qué | Cuándo se borra | Por qué ese plazo |
|---|---|---|
| Solicitud **rechazada** | 90 días desde `decided_at` | Ventana de disputa («no me llegó», «se rechazó por error») |
| Solicitud **caducada sin decidir** | 90 días desde `expires_at` | Igual |
| Solicitud **bloqueada** | 90 días desde `expires_at` | Es también la evidencia de un posible intento de abuso: no menos de 90 días |
| Invitación **caducada sin consumir** | 30 días desde `expires_at` | No contiene PII propia (§4.2); se va antes |
| Solicitud **aprobada** e invitación **consumida** | **Nunca** | Es la procedencia de un superadministrador vivo. Borrarlas deja una cuenta con privilegio total y sin explicación de por qué existe |

```sql
-- Invitaciones muertas primero: el RESTRICT de fk_pai_request impide el orden inverso.
DELETE FROM platform_access_invitations
 WHERE consumed_at IS NULL AND expires_at < :cutoff30
 ORDER BY id LIMIT :batch;

-- Solicitudes. El NOT EXISTS es cinturon: el RESTRICT ya lo impediria, pero
-- fallar 500 filas de un lote por una es peor que no seleccionarlas.
DELETE r FROM platform_access_requests r
 WHERE (   (r.decision = 'REJECTED' AND r.decided_at < :cutoff90)
        OR (r.decision IS NULL      AND r.expires_at < :cutoff90))
   AND NOT EXISTS (SELECT 1 FROM platform_access_invitations i
                    WHERE i.access_request_id = r.id)
 ORDER BY r.id LIMIT :batch;
```

**Sin índice de purga, y con el número delante.** `217_add_token_retention_support.xml` sí creó
`idx_password_reset_tokens_expires_at` y compañía, porque esas tablas reciben una fila por login, por
registro y por «olvidé mi contraseña». Esta recibe una fila por persona que pide acceso de plataforma.
Con una purga diaria sobre una tabla de tres cifras de filas, el índice cuesta escritura en cada
`INSERT` y ahorra un escaneo de una página. **Se revisa si la tabla supera 10.000 filas**, y se decide
entonces con un `EXPLAIN`, no ahora con una intuición.

`ON DELETE RESTRICT` en las dos FK es lo que convierte I10 en garantía: intentar borrar la solicitud de
un superadministrador vivo **falla con un error**, en vez de dejar la fila huérfana o, peor, borrarla en
cascada. Es la razón por la que ninguna FK de este documento lleva `CASCADE`.

---

## 9. Bloqueo optimista y reglas duras de ArchUnit

`ENTIDADES_CON_BLOQUEO_OPTIMISTA` obliga a que toda `@Entity` lleve `@Version` **o esté exenta por
escrito** en `ENTIDADES_EXENTAS_DE_VERSION` (`HexagonalArchitectureTest.java:509`).

| Entidad | Decisión | Motivo |
|---|---|---|
| `PlatformAccessRequestJpaEntity` | **`@Version`**, columna `version BIGINT NOT NULL DEFAULT 0` | No es un token puro: la fila se reescribe dos veces —contador de intentos y decisión— y hay concurrencia real: aprobar y rechazar pueden llegar a la vez desde dos pestañas |
| `PlatformAccessInvitationJpaEntity` | **Exenta, `E3_TOKEN`** | «token de un solo uso con caducidad corta», la misma redacción con la que ya están exentas `PasswordResetTokenJpaEntity`, `EmailVerificationTokenJpaEntity` y `RefreshTokenJpaEntity` (`HexagonalArchitectureTest.java:591-596`) |

**Por qué la solicitud SÍ se versiona aunque parezca un token.** La tentación de exentarla como `E3_TOKEN`
es fuerte —tiene hash, caducidad y consumo— pero la exención no encajaría: `E3_TOKEN` dice «se emite, se
consume y caduca; **nadie lo edita**», y esta fila se edita hasta seis veces (cinco incrementos del
contador más la decisión), con concurrencia real: aprobar y rechazar pueden llegar a la vez desde dos
pestañas del mismo correo. Escribir «nadie lo edita» al lado de una entidad con un contador sería
exactamente el motivo falso que el javadoc de la lista prohíbe («un motivo que valdría igual para
cualquier otra fila no es un motivo»). Se versiona. La invitación, en cambio, encaja literalmente: se
emite, se consume una vez y caduca.

**La cuenta del javadoc hay que cuadrarla, y es trabajo de `backend-feature`, no de `db-migrations`.** El
javadoc de `ENTIDADES_EXENTAS_DE_VERSION` (`HexagonalArchitectureTest.java:481-490`) lleva un censo que
«cierra al dígito»: hoy dice **128 `@Entity` = 82 versionadas + 46 exentas**. Con estas dos entidades
pasa a **130 = 83 versionadas + 47 exentas**. Si el censo no se actualiza, la prosa que demuestra que la
lista es exhaustiva deja de ser cierta, que es justo lo que el propio javadoc advierte: *«Cualquier
entidad nueva desequilibra la suma»*.

La columna `version` se declara igualmente en el DDL de las **dos** tablas: dejarla puesta en la
invitación cuesta 8 bytes y evita que el día que alguien le ponga `@Version` haya que hacer un `ALTER`
sobre datos. Si `backend-feature` prefiere no declararla en la `@Entity`, no pasa nada: `validate` no
falla por columnas sobrantes.

**`@SQLDelete`: ninguna de las dos entidades lo lleva, y es deliberado.** No hay borrado lógico aquí
(§3: no hay `enabled`); estas filas se borran de verdad en la purga (§8). Como no hay `@SQLDelete`, la
trampa de BE-26 —que al poner `@Version` Hibernate empieza a ligar **dos** parámetros al SQL del
`@SQLDelete`, `id` y `version`, y un `WHERE id = ?` que ayer era correcto se convierte hoy en un borrado
lógico roto en tiempo de ejecución— **no puede darse**. `BORRADO_LOGICO_RESPETA_LA_VERSION` no aplica, y
que siga sin aplicar depende de que nadie añada `enabled` a estas tablas.
`PROYECCION_SIN_LITERAL_BOOLEANO` tampoco aplica: no hay ni un booleano.

### BE-COV: el mecanismo exacto, y las dos líneas de Java que hay que escribir bien

La señal que enciende las cuatro reglas duras de tenencia sobre **toda** la feature es un campo llamado
`companyId` o de tipo `CompanyJpaEntity` en **cualquier** `*JpaEntity` del paquete, seguido
transitivamente hasta cinco saltos. `passwordreset` está bajo BE-COV por una sola línea:
`private Long companyId` en `PasswordResetTokenJpaEntity.java:20`.

**La señal es de Java, no de SQL.** Una FK en la base no enciende nada por sí sola; lo que se inspecciona
son los campos de las clases. De ahí dos instrucciones concretas para `backend-feature`, que valen tanto
como el DDL:

1. **`system_user_id` se mapea como `private Long systemUserId`, NO como
   `@ManyToOne SystemUserJpaEntity`.** La FK en la base queda igual (§4.2). Con la asociación, el paquete
   de la feature pasaría a referenciar una entidad de otra feature y el análisis transitivo tendría cinco
   saltos por delante para buscar `companies`. Hoy `SystemUserJpaEntity` no llega —no tiene ni
   `companyId` ni asociaciones (`SystemUserJpaEntity.java:12-34`)— pero eso es una propiedad de **hoy**:
   basta que alguien le cuelgue una relación mañana para que esta feature se encienda entera sin haberla
   tocado. Con un `Long` no hay arista que seguir.
2. **`access_request_id` igual: `private Long accessRequestId`.** Es además el patrón literal de la casa
   para tablas de token (`PasswordResetTokenJpaEntity` guarda `Long employeeId`, no una asociación).
3. **Ningún campo se llama `companyId` ni de lejos.** No hay ninguno que lo justifique: este flujo no
   tiene empresa.

Sobre el resto de reglas de tenencia (`LISTADOS_SIN_EMPRESA_SOLO_SYSTEM`,
`OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM`, `TENANT_DEFENSA_EN_PROFUNDIDAD`,
`MUTACIONES_SQL_ACOTADAS_POR_EMPRESA`): sin la señal, no se activan. Y aunque se activaran, ninguna
operación de este flujo recibe un id escrito por el cliente —la fila se localiza siempre por hash de
token— ni devuelve varias filas. `passwordreset`, que **sí** está bajo BE-COV, convive con las cuatro
reglas hoy; esta feature no debería llegar siquiera a esa situación.

---

## 10. Coste de cada `ALTER` y cómo se despliega sin parar nada

Verificado contra <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>:

| Operación | Algoritmo | ¿Reconstruye? | ¿DML concurrente? |
|---|---|---|---|
| `ADD COLUMN` a `system_users` | **INSTANT** | No | **Sí** |
| `ADD UNIQUE INDEX` a `system_users` | IN PLACE | No | **Sí** |
| `CREATE TABLE` (las dos nuevas) | — | — | — |
| **`ADD COLUMN … GENERATED … STORED`** | **COPY** | **Sí** | **NO** |

Esa última fila es la que manda en el orden de trabajo: añadir una columna generada `STORED` a una tabla
**con datos** la reconstruye entera y bloquea la escritura mientras dura. Por eso
`consumed_request_id` se declara en el **mismo changeset** que crea `platform_access_invitations`, con la
tabla todavía vacía, donde la copia cuesta cero. Es la misma doctrina que ya está escrita en
`docs/db/suscripciones-tablas.md`: «Las cinco se declaran dentro del `CREATE TABLE` inicial. Ninguna en
un `ALTER` posterior».

**Secuencia expand/contract**, para que ningún paso obligue a parar la aplicación:

1. **280 + 281** (`system_users`): se despliegan con la aplicación en marcha y **sin ningún cambio en
   `src/`**. `validate` ignora columnas de más.
2. **282 + 283**: tablas nuevas. Nadie las lee todavía.
3. **284**: la fila del interruptor, con `open = 'false'`. **El formulario nace cerrado**: el front ya
   sabe pintar el `404`, así que el endpoint puede existir en producción antes de que el flujo esté
   completo, sin exponer nada.
4. `backend-feature` añade las `@Entity` y el resto del slice.
5. Se pone `platform.access-request.open = 'true'` cuando el flujo está probado de punta a punta. **Ese
   `UPDATE` de una fila es el interruptor de encendido y también el de emergencia.**

**Backfill:** ninguno. Las dos tablas nacen vacías y las dos columnas de `system_users` nacen `NULL` a
propósito. No hay un solo `UPDATE` masivo en todo el plan, que es la mejor propiedad que puede tener una
migración.

**Rollback:** real y comprobable. `dropTable` para 282 y 283; `dropIndex` + `dropColumn` para 281 y 280.
Ninguno destruye datos preexistentes, porque ninguno los toca.

---

## 11. Consultas previstas y el índice que las sirve

| # | Consulta | Cuándo | Índice | Plan esperado |
|---|---|---|---|---|
| Q1 | `INSERT INTO platform_access_requests …` | `POST /access-request` | — | — |
| Q2 | `SELECT … WHERE approval_token_hash = ?` | `GET /access-request/validate` | `uq_par_approval_token_hash` | `type: const`, `rows: 1` |
| Q3 | `UPDATE … SET verification_attempts = … WHERE id = ? AND …` | código erróneo | PK | `type: const` |
| Q4 | `UPDATE … SET decision = … WHERE id = ? AND decision IS NULL AND …` | approve / reject | PK | `type: const` |
| Q5 | `INSERT INTO platform_access_invitations …` | tras aprobar | — | — |
| Q6 | `SELECT i.*, r.email, r.full_name … JOIN … WHERE i.token_hash = ?` | `GET /invitation/validate`, `POST /accept` | `uq_pai_token_hash` + PK del padre | `const` + `eq_ref` |
| Q7 | `UPDATE platform_access_invitations SET consumed_at = ?, system_user_id = ? WHERE id = ? AND consumed_at IS NULL` | aceptar | PK | `type: const` |
| Q8 | `SELECT 1 FROM system_users WHERE email = ?` | anti-duplicado al aceptar | `uq_system_users_email` | `type: const` |
| Q9 | `SELECT 1 FROM system_users WHERE code = ?` | generar código libre | `UNIQUE (code)` existente | `type: const` |
| Q10 | los dos `DELETE` de purga (§8) | diario | **ninguno, a propósito** | escaneo de una tabla de tres cifras |

**Toda consulta de este flujo, salvo la purga, es un acceso por clave única a una fila.** No hay ni un
listado, ni un `ORDER BY`, ni una paginación. Por eso este documento no propone ni un índice compuesto:
no hay una sola consulta que lo pudiera usar. El día que exista una bandeja de solicitudes en la consola
—que hoy no existe: el front tiene tres vistas y ninguna lista nada— se diseñará el índice contra esa
consulta real y no contra su anticipación.

---

## 12. Lo que le paso a `db-migrations`

Cinco changesets, en este orden. Los números están libres: **el último del `master` es el 279**,
verificado el 2026-08-24.

### `280_add_identity_to_system_users.xml`
`addColumn` sobre `system_users`: `email VARCHAR(150)` **NULL** y `full_name VARCHAR(120)` **NULL**.
**`VARCHAR(150)`, no `VARCHAR(100)`** aunque `employees.email` sea 100: el motivo, que es una pérdida de
datos en el peor momento del flujo, está en §4.3 y no se resume aquí.
Sin `defaultValue`. Sin `addNotNullConstraint` — es deliberado (§4.3).
`<rollback>`: `dropColumn` de las dos.
**Para el comentario del changeset:** justificar por qué nacen `NULL` (filas heredadas sin correo) y que
el paso *contract* no está planificado.

### `281_add_unique_email_to_system_users.xml`
`addUniqueConstraint tableName="system_users" columnNames="email" constraintName="uq_system_users_email"`.
`preConditions onFail="HALT"` con `sqlCheck expectedResult="0"`:
`SELECT COUNT(*) FROM (SELECT email FROM system_users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1) d`
— trivialmente 0 tras el 280, pero es el guardia que hace el changeset seguro en una base que ya hubiera
recibido correos por otra vía. Mismo patrón que `206` y `226`. El `WHERE email IS NOT NULL` no es
decorativo: sin él, `GROUP BY` agruparía todos los `NULL` juntos y la precondición fallaría con `HALT` en
una base perfectamente sana.
`<rollback>`: `dropUniqueConstraint`.
**Va en changeset aparte del 280 a propósito:** son dos algoritmos distintos (INSTANT vs IN PLACE) y no
deben fundirse en un solo `ALTER`, porque MySQL degrada al algoritmo más caro de la sentencia.

### `282_create_platform_access_requests.xml`
`createTable` con las **13** columnas de §4.1 (tipos exactos, nulabilidad explícita, `created_date` con
`defaultValueComputed="CURRENT_TIMESTAMP(6)"` — **con el `(6)`**: `DATETIME(6)` con
`DEFAULT CURRENT_TIMESTAMP` a secas es un error de sintaxis en MySQL, las precisiones tienen que
coincidir).
`uniqueConstraintName="uq_par_approval_token_hash"` inline sobre `approval_token_hash`.
Después, bloque `<sql><![CDATA[ … ]]></sql>` con los **ocho `CHECK`** de §4.1, con sus nombres literales.
**Sin `createIndex`.** Ninguno. Si parece que falta uno, está justificado en §4.1 y §11.
`<rollback><dropTable tableName="platform_access_requests"/></rollback>`.

### `283_create_platform_access_invitations.xml`
Orden **dentro** del changeset, y el orden importa:
1. `createTable` con las 8 columnas normales de §4.2 (sin `consumed_request_id`).
2. `<sql>` con **un solo** `ALTER TABLE`: `ADD COLUMN consumed_request_id … GENERATED ALWAYS AS (…) STORED`
   + `ADD CONSTRAINT uq_pai_token_hash UNIQUE (token_hash)`
   + `ADD CONSTRAINT uq_pai_consumed_request UNIQUE (consumed_request_id)`
   + `ADD CONSTRAINT uq_pai_system_user UNIQUE (system_user_id)`
   + los tres `CHECK`.
   Con la tabla vacía, la reconstrucción por COPY que exige la columna generada cuesta cero.
3. **Después** de lo anterior, los dos `addForeignKeyConstraint` (`fk_pai_request`,
   `fk_pai_system_user`), los dos con `onDelete="RESTRICT" onUpdate="RESTRICT"` explícitos.
   `uq_pai_system_user` ya existe a estas alturas, así que InnoDB **no** creará un índice propio para
   `fk_pai_system_user`. Para `fk_pai_request` sí lo creará, y está bien: es el único acceso por padre
   que el flujo necesita.
`<rollback><dropTable tableName="platform_access_invitations"/></rollback>`.
**No añadir `email` a esta tabla.** Si aparece en alguna versión del diseño, es un error: §4.2 explica
por qué no se duplica.

### `284_seed_platform_access_switch.xml`
`insert` en `system_configurations`: `property_name = 'platform.access-request.open'`, `value = 'false'`,
`enabled = true`. **`false`, no `true`** (§10, paso 3).
`preConditions onFail="MARK_RAN"` con `sqlCheck expectedResult="0"` sobre
`SELECT COUNT(*) FROM system_configurations WHERE property_name = 'platform.access-request.open'`, para
que sea idempotente.
`<rollback>`: `delete` de esa fila por `property_name`.

### Lo que `db-migrations` NO debe hacer
- No crear ninguna columna `company_id` ni ninguna FK a `companies` en estas dos tablas.
- **No crear ninguna tabla, columna ni FK de aprobador**, ni `approver_id`, ni `approver_email`, ni
  tabla de permisos de aprobación: §7. El aprobador sale de `${VETSOFTWARE_PLATFORM_APPROVER_EMAIL}`.
- No crear tabla para el interruptor del formulario: es una fila en `system_configurations` (§7).
- No añadir `blocked` ni `blocked_until` ni `status`: §3 y §6.
- No añadir `enabled` ni `@SQLDelete`: §3 y §9.
- No usar `type="TINYINT(1)"` en ninguna columna. (No debería surgir: no hay booleanos.)
- No usar `ENUM` de MySQL para `decision`: `VARCHAR(10)` + `CHECK`.
- No añadir índices «por si acaso» sobre `email`, `decision` o `expires_at`: §4.1, §8, §11.
- No poner `email` en `VARCHAR(100)` por parecerse a `employees`: §4.3.

### Lo que sigue después, y no es de `db-migrations`
`backend-feature` cuadra el censo del javadoc de `ENTIDADES_EXENTAS_DE_VERSION`
(128 = 82 + 46 → **130 = 83 + 47**), añade `PlatformAccessInvitationJpaEntity` a la lista con código
`E3_TOKEN`, y mapea `systemUserId` y `accessRequestId` como `Long` planos, nunca como asociaciones (§9).

---

## 13. Lo medido y lo razonado

**Medido (leyendo código y changesets, sin tocar ninguna base):** el inventario de changesets y el
primer número libre (`ls` sobre `migrations/` + cola de `db.changelog-master.xml`); las columnas reales
de `system_users` (`008`, `068`, `208`, `225` + `SystemUserJpaEntity.java`); que `SystemUserProfile` solo
lleva `id` y `code` (`JpaSystemUserProfileQueryPort.java:19`); el patrón de token
(`PasswordResetTokens.java:24-42`, `179_create_password_reset_tokens.xml`); el patrón de unicidad
condicional (`226`, `210`, `206`); el contrato del front (`platform-access.types.ts`,
`platform-access.api.ts`, `usePlatformAccess.ts`, `SolicitarAccesoView.vue:67-72`); las tres longitudes
de `email` del esquema (`015_create_employees.xml:21` = 100, `030_create_owners.xml:15` y
`197_create_suppliers.xml:18` = 150) frente al tope de 150 del front; las exenciones de `@Version` y su
censo de 128 = 82 + 46 (`HexagonalArchitectureTest.java:481-596`); el señalador de BE-COV en
`PasswordResetTokenJpaEntity.java:20` y la ausencia de asociaciones en `SystemUserJpaEntity.java:12-34`;
y el coste de cada `ALTER` con el comportamiento de `UNIQUE` frente a `NULL` (manual de MySQL 8.4).

**NO medido, y por qué:** no se ejecutó ningún `EXPLAIN` ni se consultó `information_schema`. No hacía
falta y no habría aportado: todas las consultas de §11 son accesos por clave única a una fila y las dos
tablas nacen vacías. No se consultó la base de dev (fuera de alcance por defecto) ni la local. El número
real de filas de `system_users` en dev y en prod **está sin verificar**: el razonamiento del §10 asume
«decenas», que es lo que corresponde a un usuario de plataforma, no una medición. Si fueran miles, el
`ADD UNIQUE INDEX` del 281 seguiría siendo IN PLACE con DML concurrente, así que la conclusión no cambia.

**Proyección, etiquetada como tal:** «esta tabla no necesitará índices de listado» sale del supuesto de
que las solicitudes de acceso de plataforma se cuentan por decenas al año. Si el producto abriera el
formulario al público general y el volumen se midiera en miles al día, hay que rehacer §4.1 y §8 — y en
ese escenario la primera pieza que falta no es un índice, es un CAPTCHA.

---

## 14. Fuentes

| URL | Qué sostiene |
|---|---|
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html> | `NOW()` prohibido en columnas generadas ⇒ «una sola solicitud viva por correo» no es expresable; `STORED` sí puede ir en un `UNIQUE` |
| <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html> | `ADD COLUMN` INSTANT; `ADD INDEX` IN PLACE con DML; **columna generada `STORED` = COPY, reconstruye, sin DML** |
| <https://dev.mysql.com/doc/refman/8.4/en/create-index.html> | *«A `UNIQUE` index permits multiple `NULL` values for columns that can contain `NULL`»* ⇒ `uq_system_users_email` convive con todas las filas heredadas sin backfill |
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html> | InnoDB crea solo el índice de la columna hija ⇒ no declarar `fk_pai_request` a mano |
| <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html> | Prefijo por la izquierda — el criterio por el que aquí **no** hace falta ningún índice compuesto |
| <https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html> | Token con CSPRNG, almacenado hasheado, invalidado tras usarse, rate limiting; y el PIN por correo desaconsejado |
| <https://martinfowler.com/bliki/ParallelChange.html> | Expand/contract: columnas `NULL` primero, `@Entity` después |
| <https://stripe.com/blog/online-migrations> | El guion de cuatro pasos, si algún día el token del aprobador se separa a su propia tabla |
| <https://use-the-index-luke.com/> | Criterio de indexación; aquí lo que sostiene es la ausencia de índices, no su presencia |
| `docs/db/suscripciones-tablas.md` (Apéndice C) | Doctrina interna: el orden `createIndex` antes que `addForeignKeyConstraint` |

🤖 Generated with [Claude Code](https://claude.com/claude-code)
