# Propuesta comercial generada por IA — modelo de datos

Especificación para `db-migrations`. **No contiene changesets**: contiene la tabla, la columna, el
tipo, la constraint con su nombre, el índice con su orden de columnas, la `preCondition` y el
`<rollback>` que ese agente tiene que escribir, y el motivo de cada uno.

- **Fecha:** 2026-08-29
- **Motor objetivo:** MySQL 8.4 / InnoDB (RDS dev y prod, `db.t4g.small`, 20 GiB gp3, Single-AZ)
- **Siguiente changeset libre:** **381**. ⚠️ **Corrección: la v1 de este documento decía 380.**
  `380_seed_grooming_requires_services.xml` **ya existe** en `db/changelog/migrations/` y es una
  semilla sin relación con esta feature, así que toda la serie se desplaza un número. El último
  declarado antes de ella era `379_attach_orphan_selfservice_submodules_to_core.xml`.
  **Comprobado sobre el árbol, no deducido**; la tabla de la sección 9 lleva ya la numeración
  buena y el `plan-implementacion-propuesta-ia.md` §5.6 también.
- **Nada de esto se ha ejecutado contra ninguna base de datos.** Todo se decide leyendo
  changesets, entidades, repositorios y las reglas ArchUnit. Ver «Medido / no medido» al final.

---

## 0. Resumen de la decisión

Cinco tablas, en **dos rodajas verticales distintas**, y esa separación es la decisión más
importante del documento:

| Tabla | Rodaja | ¿Alcanza `companies`? | Por qué |
|---|---|---|---|
| `catalog_item_ai_hints` | `catalogitem` (existente) | No | El texto de prompt por artículo, versionado |
| `ai_proposals` | `aiproposal` (nueva) | **No, y no puede** | Cabecera de la propuesta anónima |
| `ai_proposal_turns` | `aiproposal` | **No, y no puede** | Un turno = una llamada al modelo o una edición del cliente |
| `ai_proposal_lines` | `aiproposal` | **No, y no puede** | Un código propuesto/rechazado/añadido, con su motivo |
| `ai_proposal_conversions` | rodaja **con empresa** (`company` o la de registro) | Sí, a propósito | El puente propuesta → empresa cuando el prospecto se registra |

La rodaja `aiproposal` **no puede contener ninguna `@Entity` con un campo `companyId` ni con una
asociación que alcance `CompanyJpaEntity` en cinco saltos**. No es una preferencia de estilo: es
lo que decide si las cuatro reglas duras de BE-COV se encienden sobre toda la feature y rompen el
build. La sección 3 lo demuestra con el código de la condición.

---

## 1. Dónde vive el texto «cuándo se necesita este módulo»

### 1.1 Por qué NO sirve `short_description` ni `long_description`

Las dos existen ya en `catalog_items`
(`229_create_catalog_items.xml:47-48`: `short_description VARCHAR(255)`, `long_description TEXT`)
y las dos están descartadas, por tres razones independientes y cualquiera de ellas basta:

1. **Son copy de cliente, no instrucción de modelo.** Las sirve la landing pública por
   `GET /catalog` y `GET /plans` (`PublicRoutes.BUSINESS`, rutas literales). Un texto de prompt
   dice cosas que el cliente no debe leer («no lo propongas si el prospecto no menciona
   inventario ni ventas»). Meterlo ahí es publicar la lógica de venta.
2. **El acoplamiento va en las dos direcciones y las dos duelen.** Marketing retoca una frase de
   `short_description` → cambia el comportamiento del modelo y **el golden set queda inválido sin
   que nadie lo note**. Al revés: alguien afina el prompt → cambia lo que ve la landing. Y la
   landing del tenant valida el contrato con `MatchesContract`, cuya comprobación
   `UndeclaredFields` falla en cuanto la respuesta trae un campo que el front no declara
   (documentado en `PublicRoutes.java`, comentario de la ruta `/catalog`).
3. **No hay dónde versionar.** Para invalidar un golden set hace falta saber *qué texto exacto*
   produjo *qué propuesta*. `catalog_items` guarda un solo estado y una sola columna `version`, y
   esa `version` es el bloqueo optimista de la fila entera: editar el copy y editar el prompt
   colisionarían entre sí con un 409 que nadie sabe resolver.

### 1.2 Por qué tampoco una columna nueva en `catalog_items`

Misma tabla, mismo `@Version`, mismo problema de historia. Y añade uno propio: el *blast radius*
de `CatalogItemJpaEntity` es **23 llamadores** (`CatalogItemJpaMapper`, `BundleComponentJpaEntity`,
`BundleComponentJpaMapper`, `CatalogItemDependencyJpaEntity` y 3 más, con sus tests) — cada
columna nueva ahí es una fila más en un mapper que ya toca siete clases.

### 1.3 La tabla: `catalog_item_ai_hints`

Historia append-only con **una sola revisión vigente por artículo**, emulando el índice único
parcial con el patrón de la casa (`226`, `210`, `206`, y sobre todo `353_create_legal_document_versions.xml:56-65`,
que es el precedente exacto: texto versionado, editado por la consola de plataforma, con huella
para probar qué se usó).

```sql
CREATE TABLE catalog_item_ai_hints (
  id                          BIGINT       NOT NULL AUTO_INCREMENT,
  catalog_item_id             BIGINT       NOT NULL,
  hint_revision               INT          NOT NULL,
  hint_text                   VARCHAR(1000) NOT NULL,
  published_at                DATETIME(6)  NOT NULL,
  published_by_system_user_id BIGINT       NOT NULL,
  superseded_at               DATETIME(6)  NULL,
  created_date                DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version                     BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

ALTER TABLE catalog_item_ai_hints
  ADD COLUMN hint_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin
      GENERATED ALWAYS AS (SHA2(hint_text, 256)) STORED,
  ADD COLUMN current_hint_marker BIGINT
      GENERATED ALWAYS AS (CASE WHEN superseded_at IS NULL THEN catalog_item_id ELSE NULL END) STORED,
  ADD CONSTRAINT fk_catalog_item_ai_hints_item
      FOREIGN KEY (catalog_item_id) REFERENCES catalog_items (id),
  ADD CONSTRAINT uq_catalog_item_ai_hints_revision UNIQUE (catalog_item_id, hint_revision),
  ADD CONSTRAINT uq_catalog_item_ai_hints_current  UNIQUE (current_hint_marker),
  ADD CONSTRAINT uq_catalog_item_ai_hints_text     UNIQUE (catalog_item_id, hint_hash),
  ADD CONSTRAINT chk_catalog_item_ai_hints_revision  CHECK (hint_revision >= 1),
  ADD CONSTRAINT chk_catalog_item_ai_hints_supersede CHECK (superseded_at IS NULL OR superseded_at >= published_at);
```

Decisiones, una a una:

- **`VARCHAR(1000)`, no `TEXT`.** El hint es una o dos frases operativas por artículo; 1000
  caracteres es holgado y dimensionado por el dominio, no `255` por inercia ni `TEXT` por miedo.
  Y **ampliarlo después es `IN PLACE` con DML concurrente**: 1000 caracteres en utf8mb4 son 4000
  bytes, o sea ya en el tramo de dos bytes de longitud, y el manual dice que el `ALTER` in place
  «only supports increasing `VARCHAR` column size from 0 to 255 bytes, or from 256 bytes to a
  greater size» ([online DDL](https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html)).
  Cualquier `VARCHAR` de **64 caracteres o más** en utf8mb4 ya está del lado bueno de esa
  frontera; por debajo, ampliarlo copia la tabla.
- **`hint_hash` GENERADA, no calculada en Java.** `353` guarda `content_hash` como columna normal
  y por eso necesita **un trigger de inmutabilidad** para que editar `content` no deje la huella
  apuntando a un texto que ya no existe (comentario de `353`). Generada, esa deriva es imposible
  por construcción y el trigger sobra. `SHA2` es una función builtin determinista, que es lo que
  el manual exige de una columna generada
  ([columnas generadas](https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html)).
- **`ascii_bin` en `hint_hash`** y en cualquier columna de huella o token de este documento. Es lo
  que ya hace `353:53-58` (`MODIFY COLUMN content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin`),
  y es lo que pide la incidencia abierta **#633** («Ninguna columna del esquema declara colación:
  las llaves de idempotencia y las referencias de pasarela comparan ignorando mayúsculas y
  acentos»). Un `CHAR(64)` en `utf8mb4_0900_ai_ci` compara `A` y `a` como iguales y reserva 256
  bytes; en `ascii_bin` compara byte a byte y ocupa 64.
- **Las columnas generadas se declaran en el `CREATE TABLE`/en el mismo changeset de creación,
  nunca después.** Añadir una columna generada **`STORED`** a una tabla con datos es
  `Rebuilds Table: Yes` y `Permits Concurrent DML: **No**` (manual, tabla de online DDL). Es
  exactamente la incidencia **#532** ya abierta en el repo. Sobre una tabla que nace vacía cuesta
  cero; el día que haya que añadir una a `ai_proposals` con filas dentro, la opción online es
  **`VIRTUAL`** (`Instant: Yes`, `Permits Concurrent DML: Yes`, e InnoDB admite índices
  secundarios sobre columnas virtuales).
- **Sin `enabled`.** Un texto de prompt publicado no se desactiva: se sucede. Mismo criterio, misma
  frase, que `353` («SIN enabled: un texto legal publicado no se desactiva, se sucede»).
- **Con `version`**: la vigencia se cierra moviendo `superseded_at` sobre la misma fila, y esa
  edición sí es concurrente (dos operadores de consola).
- **`uq_catalog_item_ai_hints_text`** impide republicar el mismo texto bajo el mismo artículo: sin
  ella el histórico se llena de revisiones idénticas y el golden set no puede distinguirlas.

**Cómo se invalida el golden set.** `ai_proposals.catalog_snapshot_hash` guarda el SHA-256 que Java
calcula sobre la lista ordenada de pares `(catalog_items.code, catalog_item_ai_hints.hint_hash)` de
los artículos vigentes. Una propuesta vieja se compara con el corpus de hoy con **una igualdad de
64 bytes**, sin recorrer 52 filas ni almacenar el prompt renderizado.

**Lectura del corpus** (una consulta, 52 filas, sin índice nuevo — `uq_catalog_item_ai_hints_current`
la sirve):

```sql
SELECT ci.code, h.hint_text, h.hint_hash
  FROM catalog_items ci
  JOIN catalog_item_ai_hints h ON h.current_hint_marker = ci.id
 WHERE ci.status = 'ACTIVE' AND ci.enabled = TRUE
 ORDER BY ci.sort_order, ci.id;
```

---

## 2. Las tablas de la propuesta

### 2.1 `ai_proposals` — la cabecera

```sql
CREATE TABLE ai_proposals (
  id                        BIGINT        NOT NULL AUTO_INCREMENT,
  public_token              VARCHAR(43)   NOT NULL,   -- ver nota de colación abajo
  status                    VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  price_list_id             BIGINT        NOT NULL,
  billing_cycle             VARCHAR(20)   NOT NULL,
  catalog_snapshot_hash     CHAR(64)      NOT NULL,   -- ascii_bin
  privacy_notice_version_id BIGINT        NOT NULL,
  idempotency_key           CHAR(36)      NULL,       -- ascii_bin; ver nota de idempotencia
  contact_email             VARCHAR(320)  NULL,
  locale                    VARCHAR(10)   NOT NULL DEFAULT 'es-CO',
  turn_count                INT           NOT NULL DEFAULT 0,
  total_input_tokens        INT           NOT NULL DEFAULT 0,
  total_output_tokens       INT           NOT NULL DEFAULT 0,
  first_seen_at             DATETIME(6)   NOT NULL,
  last_activity_at          DATETIME(6)   NOT NULL,
  expires_at                DATETIME(6)   NOT NULL,
  anonymized_at             DATETIME(6)   NULL,
  created_date              DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  enabled                   BOOLEAN       NOT NULL DEFAULT TRUE,
  version                   BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

ALTER TABLE ai_proposals
  MODIFY COLUMN public_token          VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN catalog_snapshot_hash CHAR(64)    CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  MODIFY COLUMN idempotency_key       CHAR(36)    CHARACTER SET ascii COLLATE ascii_bin NULL,
  ADD COLUMN contact_email_hash BINARY(32)
      GENERATED ALWAYS AS (
        CASE WHEN contact_email IS NULL THEN NULL
             ELSE UNHEX(SHA2(LOWER(contact_email), 256)) END) STORED,
  ADD CONSTRAINT fk_ai_proposals_price_list
      FOREIGN KEY (price_list_id) REFERENCES price_lists (id),
  ADD CONSTRAINT fk_ai_proposals_privacy_notice
      FOREIGN KEY (privacy_notice_version_id) REFERENCES legal_document_versions (id),
  ADD CONSTRAINT uq_ai_proposals_token UNIQUE (public_token),
  ADD CONSTRAINT uq_ai_proposals_idempotency UNIQUE (contact_email_hash, idempotency_key),
  ADD CONSTRAINT chk_ai_proposals_status
      CHECK (status IN ('DRAFT','PROPOSED','ABANDONED','CONVERTED','EXPIRED')),
  ADD CONSTRAINT chk_ai_proposals_cycle
      CHECK (billing_cycle IN ('MONTHLY','ANNUAL')),
  ADD CONSTRAINT chk_ai_proposals_snapshot_hash
      CHECK (catalog_snapshot_hash REGEXP '^[0-9a-f]{64}$'),
  ADD CONSTRAINT chk_ai_proposals_counters
      CHECK (turn_count >= 0 AND total_input_tokens >= 0 AND total_output_tokens >= 0),
  ADD CONSTRAINT chk_ai_proposals_timeline
      CHECK (last_activity_at >= first_seen_at AND expires_at > first_seen_at),
  ADD CONSTRAINT chk_ai_proposals_anonimizado
      CHECK (anonymized_at IS NULL OR contact_email IS NULL);

CREATE INDEX ix_ai_proposals_retencion  ON ai_proposals (anonymized_at, last_activity_at, id);
CREATE INDEX ix_ai_proposals_consola    ON ai_proposals (created_date, id);
CREATE INDEX ix_ai_proposals_email_hash ON ai_proposals (contact_email_hash);
```

**`public_token`, y por qué es la pieza de seguridad de toda la feature.** No hay `company_id`, no
hay JWT, no hay principal: **lo único que separa la propuesta de un prospecto de la de otro es que
la URL sea imposible de adivinar**. Por lo tanto:

- 32 bytes de `SecureRandom` en base64url = **43 caracteres**. `VARCHAR(43)`, no `CHAR(43)`: un
  `CHAR(43)` en utf8mb4 reserva 172 bytes por fila; en `ascii_bin` son 43 + 1 de longitud.
- **La propuesta se direcciona por el token, nunca por el `id`.** Con `{id}` cualquiera cuenta
  desde 1 y se lleva el texto libre de todos los prospectos —con el nombre de la clínica, la
  ciudad y a veces el correo—. Es una fuga de datos personales, no un problema de rendimiento.
  ⚠️ **Y el token NO viaja en un segmento de ruta.** La v1 de este documento escribía
  `/ai-proposals/{token}`; **la ruta vigente es `/assistant/proposal` con el token en `?token=`
  (en los `GET`) o en el cuerpo (en los `POST`/`PUT`)** — plan §4.2, §4.2.1. El motivo es de
  código, no de gusto: `RequestLoggingContextFilter.java:37` mete `request.getRequestURI()` en el
  MDC bajo `http.path` **en toda petición**, esa clave está en `LogFieldPolicy.SCANNED` (`:156`) y
  **ningún patrón de `LogRedactor` casa con 43 caracteres de base64url sueltos**. Con el token en
  la ruta, la única frontera de autorización de la feature se escribe en claro en CloudWatch y en
  Loki. Los tres tokens anónimos que ya existen en el repositorio (`reset-password/validate`,
  `platform/access-request/validate`, `platform/invitation/validate`) son los tres
  `@RequestParam String token`, y es exactamente por esto.
- Efecto lateral útil: `senalaUnaFilaPorId` (`VetSoftwareConditions.java:780-792`) se dispara con un
  parámetro `Long`, no con un `String`. Un puerto que recibe el token como `String` no le parece
  «una operación por id» a la regla.

**`chk_ai_proposals_anonimizado` es la constraint más valiosa de la tabla.** Convierte «el job de
retención borró el correo» de promesa de código en **invariante verificable de la base**: una fila
marcada como anonimizada y con correo dentro no puede existir. Un job a medias no puede mentir. Y
está escrita con las dos ramas en `IS NULL`, así que no cae en la trampa que documenta
`229_create_catalog_items.xml` («MySQL rechaza una fila cuando el CHECK evalúa a FALSE; **si evalúa
a NULL, la acepta**»).

**`privacy_notice_version_id` con FK a `legal_document_versions`** (creada en `353`, sin
`company_id`, así que no contamina nada): es la prueba de qué aviso de privacidad exacto se le
mostró al prospecto cuando dio su texto. Sin esa columna, el cumplimiento de Habeas Data es un
pantallazo del front, no un dato.

⛔ **`consent_store_text` SE RETIRA de esta tabla, y no es una simplificación: era una afirmación
falsa por escrito.** Era un segundo consentimiento —«autorizo que guarden mi texto»— que **ningún
control de la interfaz pone jamás en `TRUE`**: ni el anexo A lo pinta, ni el plan §3 lo recoge.
El resultado en producción habría sido, en el 100 % de las filas, `consent_store_text = FALSE` al
lado de un `input_text` poblado — es decir, **el esquema afirmando por escrito que se guardó un
texto sin autorización para guardarlo**, y afirmándolo en la única columna que un requerimiento de
la SIC miraría. Un campo de cumplimiento que nadie puede poner a verdadero es peor que no tenerlo:
parece un control y es una confesión. El tratamiento del texto lo ampara la autorización única de
la casilla, cuya finalidad declara el propio aviso. Si algún día se quiere granularidad, es **otra
casilla y otra fila de aceptación**, no un booleano huérfano. Ver plan §1.4.

⛔ **Y la aceptación —el hecho de que el prospecto autorizara— NO vive aquí.** Esta tabla registra
**qué texto se le mostró**, que es la mitad de la prueba y no la que exige el artículo 9. La otra
mitad va en **`legal_document_acceptances`**, tabla nueva de la rodaja `legaldocumentversion`
—verificado que `LegalDocumentVersionJpaEntity` no alcanza `CompanyJpaEntity` por ningún campo, así
que no contamina—, con `subject_kind` / `subject_ref` / `accepted_at` / `accepted_ip_hash` /
`revoked_at` y `UNIQUE (subject_kind, subject_ref, legal_document_version_id)`. **El plan §1.4
argumenta por qué es una tabla y no tres columnas aquí**, y el resumen es que hay dos aceptaciones
(privacidad **y** transferencia internacional, que el artículo 26 exige nombrar con su destino),
que se revocan, y que el paso de contratación y el alta de empresa van a necesitar la misma tabla.
⚠️ **`subject_ref` de una propuesta es su `id`, jamás su `public_token`**: copiar el secreto a una
segunda tabla lo saca del control de acceso que lo protege.

⛔ **`idempotency_key` y su único ACOTADO.** La columna existía en el plan y **no en este DDL**, que
el plan cita como especificación completa: la que faltaba es esta, no la del plan. Y el único **no
puede ir sobre `idempotency_key` a secas**: la clave la elige el cliente en una cabecera, así que un
único global convierte «reenvía una clave vista» en **una lectura de la propuesta ajena, con su
texto libre y su correo, respondida con 200** — la misma fuga que `public_token` existe para
impedir, entrando por otra puerta. `uq_ai_proposals_idempotency UNIQUE (contact_email_hash,
idempotency_key)` acota la búsqueda al mismo solicitante; una clave repetida por otro correo
sencillamente no encuentra nada y sigue su curso, que es el comportamiento correcto. **Y el
servicio no responde 409 en ese caso**: un 409 distinguiría «esa clave está usada» de «no lo está»,
que es un oráculo de los de plan §6.5. Ver plan §4.2.2.
`contact_email_hash` es `NULL`-able tras la anonimización y `idempotency_key` también se pone a
`NULL` allí: **MySQL admite múltiples `NULL` en un índice único**, así que el barrido no colisiona.

**Índices, uno a uno:**

| Índice | Consulta que sirve | Justificación |
|---|---|---|
| PK `(id)` | todo | Clustered. `BIGINT AUTO_INCREMENT`, nunca UUID |
| `uq_ai_proposals_token` | `WHERE public_token = ?` | La única lectura del prospecto. Único porque es la clave natural |
| `ix_ai_proposals_retencion (anonymized_at, last_activity_at, id)` | `WHERE anonymized_at IS NULL AND last_activity_at < ? ORDER BY last_activity_at, id LIMIT ?` | Igualdad (`IS NULL`) primero, rango después, columna de orden al final. Sin `id` al final el `ORDER BY` es un `filesort` sobre **todas** las filas que casan, y el `LIMIT` se aplica después del sort: el lote deja de ser un lote |
| `ix_ai_proposals_consola (created_date, id)` | listado SYSTEM de la consola, acotado por fecha | Sirve el `ORDER BY created_date DESC, id DESC` |
| `ix_ai_proposals_email_hash` | borrado a petición del titular | Sin él, una petición de supresión es un `type: ALL` |

**Índices que se rechazan a propósito:** ninguno por `status` ni por `outcome`. El embudo
(«cuántas propuestas convirtieron este mes») se responde con un escaneo de una tabla de decenas de
miles de filas una vez al día; un índice que solo usa un informe nocturno paga escritura en cada
`INSERT` y espacio en el buffer pool de 1,5 GiB. **Se añade cuando haya un plan de ejecución que lo
pida, no antes.**

### 2.2 `ai_proposal_turns` — un turno por llamada al modelo o por edición del cliente

```sql
CREATE TABLE ai_proposal_turns (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  proposal_id       BIGINT        NOT NULL,
  turn_number       INT           NOT NULL,
  turn_type         VARCHAR(20)   NOT NULL,
  status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  input_text        VARCHAR(2000) NULL,
  input_text_chars  INT           NULL,
  model_id          VARCHAR(120)  NULL,
  prompt_version    VARCHAR(20)   NULL,
  input_tokens      INT           NULL,
  output_tokens     INT           NULL,
  latency_ms        INT           NULL,
  stop_reason       VARCHAR(30)   NULL,
  raw_response      JSON          NULL,
  failure_code      VARCHAR(40)   NULL,
  client_request_id VARCHAR(64)   NULL,
  created_date      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completed_at      DATETIME(6)   NULL,
  version           BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

ALTER TABLE ai_proposal_turns
  MODIFY COLUMN client_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
  ADD CONSTRAINT fk_ai_proposal_turns_proposal
      FOREIGN KEY (proposal_id) REFERENCES ai_proposals (id),
  ADD CONSTRAINT uq_ai_proposal_turns_seq UNIQUE (proposal_id, turn_number),
  ADD CONSTRAINT uq_ai_proposal_turns_request UNIQUE (proposal_id, client_request_id),
  ADD CONSTRAINT chk_ai_proposal_turns_type
      CHECK (turn_type IN ('MODEL_INITIAL','MODEL_REFINEMENT','CUSTOMER_EDIT')),
  ADD CONSTRAINT chk_ai_proposal_turns_status
      CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
  ADD CONSTRAINT chk_ai_proposal_turns_number
      CHECK (turn_number >= 1),
  ADD CONSTRAINT chk_ai_proposal_turns_initial_is_first
      CHECK (turn_type <> 'MODEL_INITIAL' OR turn_number = 1),
  ADD CONSTRAINT chk_ai_proposal_turns_model_arc
      CHECK ((turn_type IN ('MODEL_INITIAL','MODEL_REFINEMENT')
              AND model_id IS NOT NULL AND prompt_version IS NOT NULL)
          OR (turn_type = 'CUSTOMER_EDIT'
              AND model_id IS NULL AND prompt_version IS NULL
              AND input_tokens IS NULL AND output_tokens IS NULL
              AND raw_response IS NULL AND stop_reason IS NULL)),
  ADD CONSTRAINT chk_ai_proposal_turns_tokens
      CHECK ((input_tokens  IS NULL OR input_tokens  >= 0)
         AND (output_tokens IS NULL OR output_tokens >= 0)
         AND (latency_ms    IS NULL OR latency_ms    >= 0)),
  ADD CONSTRAINT chk_ai_proposal_turns_closed
      CHECK (status = 'PENDING' OR completed_at IS NOT NULL),
  ADD CONSTRAINT chk_ai_proposal_turns_failure
      CHECK ((status = 'FAILED' AND failure_code IS NOT NULL)
          OR (status <> 'FAILED' AND failure_code IS NULL));
```

**El texto libre y el de refinamiento van aquí, no en la cabecera.** El enunciado pide guardar «el
texto libre original» y «el texto de refinamiento si lo hubo». Dos columnas en la cabecera lo
resuelven hoy y obligan a un `ALTER` el día que alguien refine dos veces. Un turno por entrada lo
resuelve para *n* refinamientos **sin cambio de esquema**, que es diseñar el cambio y no solo el
estado (Fowler/Sadalage, [Evolutionary Database Design](https://martinfowler.com/articles/evodb.html)).
Turno 1 = texto original; turnos 2..n = refinamientos.

**`chk_ai_proposal_turns_initial_is_first` + `uq_ai_proposal_turns_seq` juntas garantizan «como
mucho un `MODEL_INITIAL` por propuesta»**, que es una invariante *entre filas* expresada sin
trigger: el `CHECK` la ancla al número 1 y el `UNIQUE` impide que haya dos números 1.

**`chk_ai_proposal_turns_model_arc`** es el arco exclusivo del estilo de
`chk_catalog_items_trial_policy` (`229`): un turno de modelo tiene `model_id` y `prompt_version`;
una edición manual del cliente no tiene ninguna de las dos ni consume tokens. Sin él, «tokens
consumidos» acaba sumando filas que nunca llamaron a Bedrock y el coste por propuesta miente.

**`status = 'PENDING'` no es decoración: lo obliga una regla de arquitectura.**
`SIN_IO_EXTERNO_EN_TRANSACCION` (`HexagonalArchitectureTest.java:331-334`, «una llamada HTTP retiene
la conexión y los locks hasta el commit») prohíbe el I/O externo dentro de `@Transactional`. La
secuencia obligada es: transacción 1 escribe el turno `PENDING` y **commitea** → se llama a Bedrock
**fuera de transacción** → transacción 2 lo cierra a `SUCCEEDED`/`FAILED`. Por eso todas las
columnas de resultado son `NULL`-ables y por eso existe `FAILED` con `failure_code`: **un turno que
nunca recibió respuesta es un estado normal del sistema, no una anomalía**, y el esquema tiene que
poder representarlo. Con `NOT NULL` en `output_tokens` la feature no arranca.

**`uq_ai_proposal_turns_request` es idempotencia, y aquí cuesta dinero.** Un doble clic en «refinar»
son dos invocaciones de Sonnet facturadas. El `UNIQUE` sobre `(proposal_id, client_request_id)`
convierte el segundo en una violación de índice único que el servicio traduce a «devuelvo el turno
que ya existe». Que MySQL permita **múltiples NULL en un índice único** es justo lo que se quiere:
si el cliente no manda clave de idempotencia, no la pide. Es el mismo comportamiento que
`241_create_quote_answers.xml` documenta como límite conocido de `uq_quote_answers`, usado aquí a
favor en vez de en contra.

**`VARCHAR(2000)` para `input_text`.** Dimensionado por el dominio (un párrafo largo describiendo
una veterinaria), no por inercia. Acota además el coste de tokens y la superficie de inyección de
prompt. Y 2000 caracteres utf8mb4 = 8000 bytes: ya en el tramo de dos bytes de longitud, así que
ampliarlo después es `IN PLACE` con DML concurrente (manual de online DDL, fila
*Extending VARCHAR column size*).

**`raw_response JSON` y no `TEXT`.** Es el tipo que ya usa la casa para payloads
(`213_create_audit_event_outbox.xml:19`, `307_create_company_entitlement_snapshots.xml:42`). Se
valida al insertar, lo que impide guardar una respuesta truncada que parezca buena. **No se
consulta con `->>` ni se indexa**: es evidencia de corta vida (ver retención), y todo lo que hay
que consultar está en `ai_proposal_lines`.

**Lo que NO se guarda, y es la decisión de almacenamiento más importante:** el **prompt renderizado**.
Son 52 artículos por ~300 caracteres = ~16 KB por turno, cinco veces el resto de la fila. Se
reconstruye determinísticamente con `prompt_version` + `catalog_snapshot_hash` + el histórico de
`catalog_item_ai_hints`. Guardarlo multiplicaría por cinco la tabla para no añadir un solo dato.

**Con `version`, no exenta.** El turno se escribe una vez y se actualiza una vez (`PENDING` →
`SUCCEEDED`), así que **`E1_APPEND_ONLY` sería falso** y `EXENCIONES_DE_VERSION_AL_DIA` existe justo
para que el repositorio no afirme por escrito algo que no es. Ocho bytes por fila y un argumento
menos.

### 2.3 `ai_proposal_lines` — un código, un veredicto, un motivo

```sql
CREATE TABLE ai_proposal_lines (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  turn_id         BIGINT        NOT NULL,
  item_code       VARCHAR(50)   NOT NULL,
  catalog_item_id BIGINT        NULL,
  action          VARCHAR(20)   NOT NULL,
  source          VARCHAR(30)   NOT NULL,
  verdict         VARCHAR(30)   NOT NULL,
  quantity        INT           NOT NULL DEFAULT 1,
  unit_amount     DECIMAL(19,2) NULL,
  reason          VARCHAR(500)  NULL,
  reason_redacted_at DATETIME(6) NULL,   -- lo pone la retención; ver 4.1
  sort_order      INT           NOT NULL DEFAULT 0,
  created_date    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  version         BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

ALTER TABLE ai_proposal_lines
  ADD CONSTRAINT fk_ai_proposal_lines_turn
      FOREIGN KEY (turn_id) REFERENCES ai_proposal_turns (id),
  ADD CONSTRAINT fk_ai_proposal_lines_item
      FOREIGN KEY (catalog_item_id) REFERENCES catalog_items (id),
  ADD CONSTRAINT uq_ai_proposal_lines_code UNIQUE (turn_id, item_code),
  ADD CONSTRAINT chk_ai_proposal_lines_action
      CHECK (action IN ('ADDED','REMOVED')),
  ADD CONSTRAINT chk_ai_proposal_lines_source
      CHECK (source IN ('MODEL','MODEL_RECOMMENDED','DEPENDENCY_CLOSURE','CUSTOMER')),
  ADD CONSTRAINT chk_ai_proposal_lines_verdict
      CHECK (verdict IN ('ACCEPTED','UNKNOWN_CODE','NOT_SELLABLE','NOT_SELF_SERVICE','DUPLICATE')),
  ADD CONSTRAINT chk_ai_proposal_lines_resolved
      CHECK (verdict <> 'ACCEPTED' OR catalog_item_id IS NOT NULL),
  ADD CONSTRAINT chk_ai_proposal_lines_quantity
      CHECK (quantity >= 1),
  ADD CONSTRAINT chk_ai_proposal_lines_amount
      CHECK (unit_amount IS NULL OR unit_amount >= 0),
  ADD CONSTRAINT chk_ai_proposal_lines_sort
      CHECK (sort_order >= 0),
  ADD CONSTRAINT chk_ai_proposal_lines_model_reason
      CHECK (source NOT IN ('MODEL','MODEL_RECOMMENDED')
             OR reason IS NOT NULL
             OR reason_redacted_at IS NOT NULL),
  ADD CONSTRAINT chk_ai_proposal_lines_redaccion
      CHECK (reason_redacted_at IS NULL OR reason IS NULL);
```

⛔ **Tres correcciones sobre la v1 de esta tabla, y la primera es la que hacía falsa la
anonimización de toda la feature.**

**(1) `reason` es dato personal, y el `CHECK` impedía borrarlo.** El anexo E convierte en **regla
dura del prompt** que el motivo **cite al cliente** —*«Tiene que citar lo que ÉL dijo, no describir
el módulo»*, con ejemplos como *«Le vendes a crédito a una fundación…»*—, así que esta columna
guarda las palabras del prospecto: su modelo de negocio, sus convenios, a veces la ciudad y el
nombre de la clínica. La política de la sección 4.1 borraba `contact_email`, `input_text` y
`raw_response` **y conservaba expresamente «todas las líneas»**. Resultado: **una fila marcada
`anonymized_at` seguía llevando el texto del prospecto dentro**, y el informe de cumplimiento decía
que estaba limpia. Y el `CHECK` de la v1 (`source <> 'MODEL' OR reason IS NOT NULL`) **impedía el
arreglo obvio**: no se podía anular el motivo sin reescribir el `source`, y reescribir el `source`
destruye la única señal que dice qué propuso el modelo.

**El arreglo es `reason_redacted_at` + la rama nueva del `CHECK`**, y la constraint pasa de decir
«una línea de modelo tiene motivo» a decir **«una línea de modelo o tiene motivo, o consta que se le
borró»** — que es más fuerte, porque distingue *borrado* de *ausente*, y un motivo simplemente
ausente sigue siendo un defecto. El marcador va **en la propia línea y no se lee
`ai_proposals.anonymized_at`** por un motivo del motor: **MySQL no permite que un `CHECK` referencie
otra tabla**, así que un `OR anonymized_at IS NOT NULL` aquí no es expresable. Y sale ganando: la
invariante se comprueba **sin `JOIN`**, que es el mismo argumento por el que
`chk_ai_proposals_anonimizado` es la constraint más valiosa de su tabla. Coste: un `DATETIME(6)`
nulable en la tabla con más filas; en `DYNAMIC` un `NULL` vive en el mapa de nulos y **no ocupa nada
hasta que se usa**. Se rechaza el centinela (`reason = '[anonimizado]'`): es «Fear of the Unknown»
de Karwin, el mismo antipatrón que esta sección rechaza para `catalog_item_id`, y además un valor
mágico que cualquier `COUNT` de motivos contaría como motivo. **La alternativa completa —guardar el
motivo como referencia a hint + plantilla, sin prosa— se evaluó y se rechazó en el plan §5.5.1**:
mata desde el día 1 la señal «¿escribe el modelo motivos que citan al cliente?», que es justo la que
la revisión manual de las primeras 100 propuestas tiene que juzgar.

**(2) `verdict` tenía un vocabulario distinto del que usa el plan.** La v1 declaraba
`('ACCEPTED','UNKNOWN_CODE','NOT_PUBLISHED','DUPLICATE','EXCLUDED_BY_RULE')` y el plan §4.1
declaraba `ACCEPTED | UNKNOWN_CODE | NOT_SELLABLE | NOT_SELF_SERVICE | DUPLICATE`. **Manda el del
plan**, y no por antigüedad: `NOT_SELF_SERVICE` es el veredicto que exige la regla 1 de §2.3 del
plan —«la propuesta solo emite líneas con `selfServiceEligible = true`»—, que es la contención de
DC-1, y **el vocabulario de la v1 no tenía dónde ponerla**. `EXCLUDED_BY_RULE` desaparece porque los
arcos `EXCLUDES` son **0** en el catálogo real (plan §4.3).
⛔ **Y estos cinco valores NO se serializan nunca** (plan §4.2.3): cinco veredictos distinguibles en
una respuesta cuyo texto de entrada escribe el atacante son un oráculo de cinco valores sobre el
catálogo interno. Se persisten porque son la medida de calidad del modelo; salen por HTTP solo las
líneas `ACCEPTED` y, como mucho, un entero sin desglose.

**(3) `source` gana `MODEL_RECOMMENDED`.** El esquema de salida devuelve **dos** listas
—`necesarios` y `recomendados`— y el prompt sesga explícitamente hacia la segunda. El plan §4.4
decide que **`recomendados` no entra al carrito por defecto**, y sin un valor propio de `source`
esa decisión no es representable: las dos poblaciones acabarían con el mismo veredicto y la señal
«¿distingue el modelo lo necesario de lo opcional?» desaparecería de la telemetría. **Un `source`
nuevo en vez de una columna `necessity`**: la pregunta que responde la columna es «¿quién puso esta
línea aquí?», y «el modelo, como opcional» responde a esa misma pregunta; una columna aparte
obligaría a un `CHECK` cruzado para impedir el estado imposible «`CUSTOMER` + recomendado».

**Aquí está la respuesta a «qué propuso el modelo, qué corrigió Java y qué tocó el cliente»: son
cuatro valores de `source` y cinco de `verdict`, no un diff en JSON.**

- `source = 'MODEL'` + `verdict = 'ACCEPTED'` → el modelo acertó.
- `source = 'MODEL_RECOMMENDED'` → el modelo lo puso en `recomendados`. **Se sirve aparte, sin
  marcar y sin sumar al total** (plan §4.4); si el cliente lo acepta, se reescribe como `CUSTOMER`
  en un turno `CUSTOMER_EDIT` y **ahí** se cierran sus `REQUIRES`.
- `source = 'MODEL'` + `verdict = 'UNKNOWN_CODE'` → **el modelo alucinó un código**. Por eso
  `item_code VARCHAR(50) NOT NULL` guarda lo que dijo el modelo *verbatim* y `catalog_item_id` es
  `NULL`-able: **no se puede poner una FK a una fila que no existe**, y esa alucinación es
  precisamente el dato que mide la calidad del modelo. Es el punto exacto donde una FK obligatoria
  habría destruido la información más valiosa de la tabla.
- `source = 'DEPENDENCY_CLOSURE'` → lo añadió Java aplicando `catalog_item_dependencies`
  (`REQUIRES`/`RECOMMENDS`/`EXCLUDES`, `231`).
- `source = 'CUSTOMER'` con `action = 'ADDED'`/`'REMOVED'` → lo tocó el cliente a mano, en un turno
  `CUSTOMER_EDIT`.

**`uq_ai_proposal_lines_code` va sobre `(turn_id, item_code)` y no sobre `(turn_id, catalog_item_id)`.**
Con `catalog_item_id` la constraint no valdría nada para las alucinaciones: MySQL admite múltiples
`NULL` en un índice único, así que el mismo código inventado podría repetirse veinte veces en el
mismo turno. Sobre `item_code`, que es `NOT NULL`, la invariante «un código, una vez por turno» se
cumple siempre. Además es el índice que cubre la FK a `turn_id` por prefijo izquierdo
([índices multicolumna](https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html)), así
que InnoDB **no** crea un segundo índice para la FK.

**`unit_amount DECIMAL(19,2)`, jamás `FLOAT`/`DOUBLE`,** y con la misma precisión que
`catalog_prices.unit_amount` (`234`). Congelar el importe mostrado es lo que hace de esta tabla un
registro de auditoría y no una foto que cambia sola cuando se publica una tarifa nueva.

**Se rechaza `proposal_id` denormalizada en esta tabla.** Es funcionalmente dependiente de `turn_id`
(3NF) y no hay ni una medición que pida el atajo: una propuesta tiene ≤5 turnos. La
desnormalización sin evidencia es deuda, y además añadiría una FK y un índice a la tabla que más
filas va a tener.

~~**Sin `enabled` y sin `version`, exenta con `E1_APPEND_ONLY`**~~ — **superado por la corrección
(1) de arriba.** Con el borrado del motivo en la retención, **una línea sí se toca una vez**, así
que `E1_APPEND_ONLY` sería **falso por escrito** y `EXENCIONES_DE_VERSION_AL_DIA` existe justo para
que el repositorio no afirme algo que no es —el mismo razonamiento que ya obligó a versionar
`ai_proposal_turns` dos secciones más arriba—. **`ai_proposal_lines` lleva `version BIGINT NOT NULL
DEFAULT 0`** y su `UPDATE` masivo de retención lleva `version = version + 1` en el `SET`
(`UPDATE_MASIVO_MUEVE_LA_VERSION`, dura). Ocho bytes por fila: ~9 MB a 100.000 propuestas, sobre
los ~830 MB de la sección 6. **Sigue sin `enabled`**: una línea no se desactiva.

### 2.4 `ai_proposal_conversions` — el puente, y **fuera de la rodaja `aiproposal`**

```sql
CREATE TABLE ai_proposal_conversions (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  proposal_id  BIGINT      NOT NULL,
  company_id   BIGINT      NOT NULL,
  converted_at DATETIME(6) NOT NULL,
  created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id)
);

ALTER TABLE ai_proposal_conversions
  ADD CONSTRAINT fk_ai_proposal_conversions_proposal
      FOREIGN KEY (proposal_id) REFERENCES ai_proposals (id),      -- ON DELETE RESTRICT (defecto)
  ADD CONSTRAINT fk_ai_proposal_conversions_company
      FOREIGN KEY (company_id)  REFERENCES companies (id),         -- ON DELETE RESTRICT (defecto)
  ADD CONSTRAINT uq_ai_proposal_conversions_proposal UNIQUE (proposal_id),
  ADD CONSTRAINT uq_ai_proposal_conversions_company  UNIQUE (company_id);
```

- **`ON DELETE RESTRICT` (el defecto de InnoDB) es una decisión, no una omisión.** Es lo que impide
  que el job de retención borre una propuesta que acabó en cliente: la base rechaza el `DELETE`. La
  protección del registro comercial **no depende del `WHERE` del job**.
- `uq_ai_proposal_conversions_company`: una empresa nace de **una** propuesta. Sin ese único, dos
  propuestas se pueden atribuir el mismo alta y el embudo cuenta doble.
- **`ai_proposals.status = 'CONVERTED'` es un estado denormalizado sin FK**, para que el embudo se
  responda sin salir de la rodaja. La empresa concreta solo se sabe desde el puente.

Por qué el puente vive en otra rodaja, y no es una manía de nomenclatura: sección 3.

---

## 3. El punto delicado: una tabla sin `company_id` en este repositorio

### 3.1 Cómo decide ArchUnit si «estas filas son de alguien»

`VetSoftwareConditions.perteneceAUnaEmpresa` (`VetSoftwareConditions.java:849-880`) recorre en
anchura los campos de una `@Entity` hasta `MAX_SALTOS_DE_ASOCIACION` saltos y devuelve `true` en
cuanto encuentra:

```java
if (COMPANY_ENTITY.equals(tipo.getSimpleName())   // CompanyJpaEntity
        || "companyId".equals(campo.getName())) { // o un campo llamado literalmente companyId
    return true;
}
```

y `laFeatureTieneDatosDeEmpresa` (`VetSoftwareConditions.java:800-812`) lo eleva a **toda la rodaja**:

```java
return paqueteDeLaFeature(clazz).map(feature -> feature.getClassesInPackageTree().stream()
        .filter(c -> c.getSimpleName().endsWith("JpaEntity"))
        .anyMatch(VetSoftwareConditions::perteneceAUnaEmpresa)).orElse(false);
```

**`anyMatch` sobre el árbol de paquetes de la feature.** Una sola entidad con empresa enciende las
reglas sobre **todos** los puertos y **todos** los casos de uso de la rodaja. Es la nota del
proyecto «ArchUnit BE-COV se activa sola», y aquí está el código que la produce.

Dos consecuencias que hay que tener presentes:

1. **El disparador es el mapeo Java, no el esquema.** Una columna `company_id` en la base que se
   mapee como `Long convertedCompanyId` **no** dispara la guarda. Eso es exactamente lo que
   denuncia la incidencia abierta **#398** («el discriminador depende del mapeo, no del esquema»).
   **No lo uses como solución**: sería desactivar por nomenclatura una regla de seguridad. Si la
   fila pertenece a una empresa, tiene que decirlo.
2. Las colecciones inversas **no** propagan: el `getRawType` de un `List<XJpaEntity>` es `List`, que
   no termina en `JpaEntity`. Solo propagan las asociaciones de valor único (`@ManyToOne`,
   `@OneToOne`). Por eso una FK a `catalog_items` es inocua: `catalog_items` no tiene `company_id`
   por ningún camino (`229`), y así lo declara ya `PublishedCatalogItemQueryPort`.

### 3.2 ¿Una FK a `companies` desde la propuesta dispara el problema? **Sí, y estas son las cuatro facturas**

Supongamos `AiProposalJpaEntity` con `@ManyToOne(optional = true) CompanyJpaEntity convertedCompany`.
`laFeatureTieneDatosDeEmpresa` pasa a `true` para toda la rodaja `aiproposal`, y entonces:

| Regla | Qué exige entonces | Qué pasa en esta feature |
|---|---|---|
| `OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM` (`HexagonalArchitectureTest.java:137-142`) | Todo puerto que señale una fila por id y no reciba `companyId` tiene que estar cerrado a `hasRole('SYSTEM')` | Sobrevive **solo si** cada puerto público lleva `@NoAuthorizationRequired` (está en el `.that()`) y cada puerto de consola es `hasRole('SYSTEM')` **a secas** |
| `CARGA_POR_ID_ACOTADA_POR_EMPRESA` (`:206-210`) | El servicio debe llamar a la variante acotada del puerto de salida | Exento por `sinEmpresaDeLaQueTirar` (`VetSoftwareConditions.java:1130-1132`) **solo si** el servicio implementa ≥1 puerto y **todos** son `@NoAuthorizationRequired`, o todos son SYSTEM |
| `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA` (`:264-268`) | Misma exención | Igual |
| `MUTACIONES_SQL_ACOTADAS_POR_EMPRESA` (`:160-164`) | **Toda `@Query` que escribe tiene que nombrar la empresa en su `WHERE`** (`VetSoftwareConditions.java:918-940`; la guarda mira la entidad del propio repositorio, no la rodaja) | **Aquí se rompe y no hay salida limpia.** El `UPDATE` de anonimización por fecha no tiene ninguna empresa que nombrar |

La cuarta es la que decide. El SQL de retención es, necesariamente:

```sql
UPDATE ai_proposals
   SET anonymized_at = ?, contact_email = NULL, version = version + 1
 WHERE anonymized_at IS NULL AND last_activity_at < ?
```

📌 **Léase la tabla con su encabezado delante: es un contrafáctico.** «Aquí se rompe» significa
«se rompe **si se pone la asociación**», que es el supuesto de la primera línea de esta sección.
**Con el diseño que se adopta —cero columnas de empresa en las tres tablas— la cuarta regla no
llega ni a mirar una `@Query`**: su condición (`acotarPorEmpresaElSqlQueEscribe`,
`VetSoftwareConditions.java:962-986`) empieza con
`if (entidad.isEmpty() || !perteneceAUnaEmpresa(entidad.get())) return;`, y esa `entidad` es la del
**propio repositorio**. `AiProposalJpaEntity` no alcanza `CompanyJpaEntity` por ningún camino, así
que el `UPDATE` de anonimización pasa **por exención estructural, no por un rodeo y no por suerte**.
**No hay ningún bloqueo abierto aquí.** Lo que hay es el argumento de por qué la asociación no se
pone — y por qué es esta regla, y no las otras tres, la que decide: **las otras tres se filtran por
`laFeatureTieneDatosDeEmpresa`, que es contagiosa por rodaja; esta se filtra por la entidad del
repositorio, y la entidad del repositorio sería justo la que llevaría la asociación.**

Con la asociación puesta, esa `@Query` **rompe el build**, y las tres salidas son todas malas:
(a) inventar una sobrecarga homónima acotada por empresa que nadie llama, solo para activar
`tieneHermanaAcotada`; (b) escribir el SQL con `JdbcTemplate`, que la regla no ve —el propio
`CLAUDE.md` lo documenta como limitación conocida con **exactamente dos** entradas permitidas
(`JdbcDianJobLeasePort` y `TokenCleanupRepository`)—; (c) renombrar el campo para engañar a la
guarda. Las tres son «desactivar el gate para que pase mi cambio».

### 3.3 La salida: el puente en otra rodaja

**No hay ninguna columna de empresa en `ai_proposals`, `ai_proposal_turns` ni `ai_proposal_lines`.**
El vínculo con la empresa vive en `ai_proposal_conversions`, cuya `@Entity` está en una rodaja que
**ya** tiene datos de empresa (`company`, o la de registro) y donde BE-COV ya está encendida **con
razón**. Su único puerto es un listado de consola cerrado a `hasRole('SYSTEM')`, que es la exención
estructural de las cuatro reglas.

No es un truco de empaquetado: es la modelización correcta. **La propuesta no pertenece a ninguna
empresa**; pertenece a un prospecto anónimo. Lo que pertenece a una empresa es *el hecho de que esa
empresa nació de esa propuesta*, y ese hecho se registra donde ocurre. En protección de datos tiene
además nombre propio: la propuesta queda seudonimizada y **la clave que la reidentifica se guarda
por separado y con otro control de acceso**.

### 3.4 La trampa que no está en el enunciado: `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM`

Esta regla se comporta **distinto** a las cuatro anteriores y es la que va a romper el build primero:

- **No tiene guarda `laFeatureTieneDatosDeEmpresa`.** Le da igual que la rodaja no tenga empresa.
- **No exime `@NoAuthorizationRequired`.** `evaluarElGate` (`VetSoftwareConditions.java:552-566`)
  exige `gate.isPresent() && soloAlcanzablePorSystem(...)`. Un puerto público **no tiene
  `@PreAuthorize` en absoluto** ⇒ `cerrado = false` ⇒ **violación**.
- **Su disparador es sorprendentemente laxo:** salta si un servicio de `..application.usecase..`
  llama a un `find…` de un puerto de salida que devuelve `Collection`/`PageResult` sin filtrar por
  empresa, **y ese puerto de salida declara algún método que sí filtra**, donde «filtra» es
  `transportaCompanyId(m) || m.getName().contains("Company")` (`VetSoftwareConditions.java:571-573`).
  **Basta con que el nombre contenga la palabra `Company`.**

**Regla de diseño que sale de ahí, y es obligatoria:**

> Ningún puerto de salida que invoquen los casos de uso anónimos de `aiproposal` puede declarar un
> método que lleve `companyId` ni cuyo nombre contenga `Company`. Las consultas de consola con
> empresa van en **otra interfaz** (`SystemAiProposalQueryPort`), invocada solo por los servicios
> `hasRole('SYSTEM')`.

Y su gemela, del análisis de `sinAutorizacionDeEmpleado`:

> **Una clase de servicio por puerto.** Un `AiProposalService` que implemente a la vez
> `CreateAiProposalUseCase` (`@NoAuthorizationRequired`) y `ListAiProposalsUseCase`
> (`hasRole('SYSTEM')`) pierde la exención de `CARGA_POR_ID_ACOTADA_POR_EMPRESA` y
> `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA`, porque exigen que **todos** los puertos
> implementados por la clase sean `@NoAuthorizationRequired`.

### 3.5 Lo que sustituye al aislamiento por tenant

Sin `company_id`, ninguna de las reglas de tenancy protege estas filas. La protección es otra y hay
que escribirla entera:

1. **`public_token` opaco de 32 bytes**, único, en la URL. Es la frontera.
2. **`expires_at`**: el enlace caduca (propuesta: 30 días). Un token eterno es una fuga con retardo.
3. **Son cuatro rutas públicas y dos de ellas son `POST`** (`/assistant/proposal` y
   `/assistant/proposal/refine`) ⇒ el test `toda_ruta_publica_post_esta_limitada` (invariante viva
   del repo, citada en `PublicRoutes.java`) **exige** límite por IP en `LoginRateLimitFilter`. Aquí
   además protege el gasto: sin límite, un bucle contra `POST /assistant/proposal` es una factura
   de Bedrock. ⚠️ **Y el `PUT /assistant/proposal/lines` y el `GET` quedan FUERA de esa invariante**,
   porque el filtro solo mira `POST`: el `PUT` es una escritura pública anónima y necesita límite
   propio, lo que exige ampliar el filtro (plan §6.1).
4. **Rutas literales en `PublicRoutes.BUSINESS`**, nunca `/assistant/**`: el mismo prefijo va a
   colgar los endpoints SYSTEM de la consola. Es el razonamiento que ya dejó escrito
   `/configurator` con sus dos rutas exactas.
   ⚠️ **Y ninguna de las cuatro lleva el token en un segmento de ruta** (sección 2.1): con `{token}`
   dentro, `RequestLoggingContextFilter` lo escribe en claro en el MDC y el redactor no lo casa.
5. **`@NoAuthorizationRequired(reason = "…")` en cada puerto público**, con el motivo escrito. Las
   **dos** cosas —ruta y anotación— o el prospecto se come un 401 (falta la ruta) o el puerto queda
   abierto sin llamador posible (falta la anotación).

---

## 4. Retención y datos personales (Ley 1581 / Habeas Data)

El texto libre traerá nombre de la clínica, ciudad, volumen de facturación y, con frecuencia,
nombre y correo del dueño. Lo primero es dato de una persona jurídica; lo segundo **sí es dato
personal** y cae bajo la Ley 1581 de 2012 y el Decreto 1074 de 2015: finalidad declarada,
autorización previa, y conservación solo por el tiempo razonable y necesario.

### 4.1 Política

| Momento | Qué pasa | Columna que lo controla |
|---|---|---|
| Alta | Se registra **qué aviso de privacidad se mostró** (aquí) y **que el prospecto lo aceptó** (en `legal_document_acceptances`, sección 2.1) | `privacy_notice_version_id` |
| **+90 días** sin actividad y sin conversión | **Anonimización**: `contact_email = NULL`, `idempotency_key = NULL`, `input_text = NULL`, `raw_response = NULL` **y `reason = NULL` en todas las líneas**. Se conservan `input_text_chars`, tokens, `model_id`, `prompt_version`, hashes, `item_code`, `source`, `verdict` y **la fila de cada línea** | `anonymized_at`, `reason_redacted_at` |
| **+24 meses** desde la anonimización | **Borrado físico** de `ai_proposal_lines` → `ai_proposal_turns` → `ai_proposals`, por lotes | — |
| Convertida en cliente | Se anonimiza el texto a los 12 meses; la fila **no se borra nunca**: el `RESTRICT` de `fk_ai_proposal_conversions_proposal` lo impide | `status = 'CONVERTED'` |
| Petición del titular | Anonimización inmediata **incluidos los motivos** + borrado de `raw_response`; borrado físico si no hay conversión | `ix_ai_proposals_email_hash` |

⛔ **Se retira la fila «+24 meses con `consent_store_text = TRUE`»**: esa columna no existe (sección
2.1). Un plazo distinto colgado de un booleano que nadie pone a verdadero es un plazo que nunca se
aplica, y peor, es una regla escrita que un auditor leería como vigente.

⛔ **Se corrige «se conservan todas las líneas».** Esa frase era la que derrotaba la anonimización
entera: la línea se conserva —su código, su origen y su veredicto son la señal de calidad y no son
dato personal—, **pero su `reason` no**, porque el prompt obliga al modelo a citar al cliente
(sección 2.3, corrección 1). La fila sobrevive; las palabras del prospecto, no.

### 4.2 Cómo se ejecuta (patrón de la casa, por lotes)

El precedente es `TokenCleanupRepository` (`src/main/java/com/vetsoftware/app/infrastructure/token/TokenCleanupRepository.java:13-30`):
`DELETE … ORDER BY id LIMIT ?` por lotes, **nunca** una transacción única sobre toda la tabla.

```sql
-- Paso 1: marcar el lote (una sola tabla, así que ORDER BY + LIMIT son legales)
UPDATE ai_proposals
   SET anonymized_at = :ahora, contact_email = NULL, version = version + 1
 WHERE anonymized_at IS NULL
   AND last_activity_at < :corte
   AND status <> 'CONVERTED'
 ORDER BY last_activity_at, id
 LIMIT :lote;
```

El paso 1 pone además `idempotency_key = NULL` en el mismo `SET`: la clave la eligió el cliente y
está indexada junto a su correo, así que sobrevivirla no aporta nada y `uq_ai_proposals_idempotency`
admite múltiples `NULL`.

```sql
-- Paso 2: los turnos de esas propuestas, por id, NO con un UPDATE multitabla
UPDATE ai_proposal_turns
   SET input_text = NULL, raw_response = NULL, version = version + 1
 WHERE proposal_id IN (:ids)
   AND (input_text IS NOT NULL OR raw_response IS NOT NULL);
```

```sql
-- Paso 3: los motivos de las líneas de esos turnos. Una sola tabla, y por turn_id
-- porque ai_proposal_lines no lleva proposal_id (3NF, sección 2.3).
UPDATE ai_proposal_lines
   SET reason = NULL, reason_redacted_at = :ahora, version = version + 1
 WHERE turn_id IN (:idsDeTurno)
   AND reason IS NOT NULL;
```

⚠️ **El paso 3 obliga a que el paso 2 devuelva los `turn_id`**, no solo los `proposal_id`. Es un
`SELECT id FROM ai_proposal_turns WHERE proposal_id IN (:ids)` de más por lote, servido por
`fk_ai_proposal_turns_proposal`. **La alternativa —un `UPDATE … JOIN` de dos tablas— no existe
aquí**: es exactamente el caso que el segundo aviso de abajo prohíbe.

Tres avisos que se pagan caros si se ignoran:

- **`version = version + 1` en el `SET` es obligatorio**, no opcional: `UPDATE_MASIVO_MUEVE_LA_VERSION`
  es regla dura y **las tres tablas están versionadas** —`ai_proposal_lines` lo está precisamente
  porque este paso 3 existe, ver sección 2.3—. Y **nunca en el `WHERE`**, que dejaría el `UPDATE`
  actualizando cero filas.
- **El paso 2 no puede ser un `UPDATE` multitabla con `JOIN`**: MySQL no admite `ORDER BY` ni
  `LIMIT` en un `UPDATE` de varias tablas, así que el lote deja de estar acotado y el job se lleva
  la tabla entera en una transacción.
- `ix_ai_proposals_retencion (anonymized_at, last_activity_at, id)` es lo que hace que el paso 1 sea
  un rango acotado y no un `filesort` sobre todas las filas candidatas.

### 4.3 Lo que esta política **no** garantiza, dicho por escrito

- ⛔ **Lo que la v1 no garantizaba y ahora sí, dicho para que no se pierda:** hasta la corrección
  de la sección 2.3, «anonimizada» **no significaba «sin texto del prospecto»**, porque el motivo
  de cada línea llevaba sus palabras y se conservaba a propósito. Una fila con `anonymized_at`
  puesto pasaba el `CHECK`, salía limpia en el informe y contenía la descripción del negocio del
  titular. Es el fallo más caro que ha tenido este documento, y no se veía porque cada pieza
  —el prompt que pide citar, la columna que guarda, la política que conserva las líneas— era
  razonable por separado.
- **La supresión dirigida solo alcanza el campo estructurado.** Si el prospecto escribió su correo
  *dentro* del texto libre y nunca lo puso en `contact_email`, `contact_email_hash` no lo encuentra.
  Ese caso lo cubre la anonimización por tiempo, no la petición del titular. Alternativas
  —búsqueda `LIKE '%…%'` sobre `input_text`, o `FULLTEXT`— se descartan: la primera es un escaneo
  completo, la segunda impide añadir columnas con `ALGORITHM=INSTANT` (el manual excluye las tablas
  con índice `FULLTEXT`).
- **`LOWER()` en `contact_email_hash` depende de la colación de la columna.** Es determinista y por
  tanto legal en una columna generada, pero cambiar la colación de `contact_email` cambiaría el
  hash de las filas nuevas y no el de las viejas. Si alguna vez se toca esa colación, hay que
  recalcular.
- **El plazo de 90/24 meses no está validado por nadie con autoridad legal.** Es una propuesta de
  ingeniería. Queda como issue.

---

## 5. Qué hacer con `configurator_questions` / `configurator_options` / `configurator_effects`

**Recomendación: deprecar. Ni borrar, ni dejar como fallback vivo.**

### 5.1 Por qué no se pueden borrar, aunque se quisiera

`241_create_quote_answers.xml` declara `quote_answers` con **dos FK `NOT NULL`**:

```xml
<column name="question_id" type="BIGINT">
  <constraints nullable="false" foreignKeyName="fk_quote_answers_question"
               references="configurator_questions(id)"/>
</column>
<column name="option_id" type="BIGINT">
  <constraints nullable="true" foreignKeyName="fk_quote_answers_option"
               references="configurator_options(id)"/>
</column>
```

Cada cotización emitida por el cuestionario tiene filas en `quote_answers` apuntando ahí. Borrar las
tres tablas obliga a (a) borrar `quote_answers` —se pierde *por qué* se cotizó cada oferta emitida,
que es el registro comercial— o (b) hacer nulable `question_id` y perder la integridad. **La fase de
*contract* del expand/contract nunca puede completarse aquí**, y ese es el criterio, no la
nostalgia: GitLab lo resume igual —solo se elimina lo que la aplicación lleva ignorando al menos una
versión *y* nada más referencia—.

### 5.2 Por qué tampoco «dejarlas como fallback»

Un fallback vivo es un segundo camino de decisión que nadie prueba y que se desincroniza del
catálogo en silencio. `configurator_effects` ya trae su propia complejidad ordenada por `priority`
(ver el javadoc de `ConfiguratorEffect.java:6-47`, con el síntoma «marcar más servicios produce un
carrito más pequeño»): mantener eso vivo «por si acaso» es pagar el mantenimiento sin recibir el uso.

### 5.3 El plan concreto, y qué **no** se toca

Un changeset ya aplicado **no se edita**: el checksum de `DATABASECHANGELOG` es inmutable y
`235`-`238` y `312` se quedan como están. La deprecación es **aditiva**:

1. **Código (no es tuyo ni mío, es de `backend-feature`):** quitar de `PublicRoutes.BUSINESS` las
   dos rutas `GET /configurator/questionnaire` y `POST /configurator/resolve`, y retirar
   `@NoAuthorizationRequired` de `GetPublicQuestionnaireUseCase` y `ResolveConfiguratorSelectionUseCase`.
   Es reversible en un commit, que es lo que lo hace la fase *expand* correcta.
2. **Los puertos SYSTEM de administración del configurador se conservan.** Borrar superficie de API
   es una rotura de contrato para `admin-web`; se oculta la pantalla en la consola y ya.
3. **Changeset nuevo (`38x`)** con `<setTableRemarks>` sobre las tres tablas:
   `DEPRECADA 2026-08: el asistente de venta se sustituyó por la propuesta generada por IA (ai_proposals). Se conserva por la FK NOT NULL de quote_answers. No sembrar filas nuevas.`
   `<rollback>` = `<setTableRemarks remarks=""/>`. Es metadato: no toca ni una fila.
4. **No se desactivan las filas sembradas por `312`.** Un `UPDATE … SET enabled = FALSE` no aporta
   nada una vez retiradas las rutas y destruye la posibilidad de revertir la feature con un
   `git revert`. Si algún día se decide desactivarlas, ese `UPDATE` **debe** llevar
   `version = version + 1` —las tres tablas tienen columna `version`—, que es la disciplina que el
   propio `308` deja escrita.

---

## 6. Coste de almacenamiento y crecimiento

**Todo lo de esta sección es una estimación calculada sobre el DDL propuesto. No hay ni una fila
medida:** las tablas no existen. Los supuestos están explícitos para que se puedan discutir.

Supuestos: texto libre de ~400 caracteres; `raw_response` de ~2,5 KB; 1,4 turnos por propuesta
(el 40 % refina); 11 líneas por propuesta (≈9 del modelo + 2 del cierre de dependencias, más las
ediciones manuales); InnoDB `DYNAMIC`, factor de ocupación de página ×1,3.

| Tabla | Bytes/fila (datos + índices) | Filas por propuesta |
|---|---|---|
| `ai_proposals` | ~320 datos + ~195 índices = **~515** | 1 |
| `ai_proposal_turns` (fresca) | ~350 + `input_text` ~450 + `raw_response` ~2 500 + ~130 índices = **~3 430** | 1,4 |
| `ai_proposal_turns` (anonimizada) | **~480** | 1,4 |
| `ai_proposal_lines` | ~360 + ~100 índices = **~460** | 11 |

- **Propuesta fresca: ~10,4 KB.** **Propuesta anonimizada: ~6,3 KB** (la dominan las líneas, no el
  texto).
- `raw_response` de 2,5 KB **no** va a página de desbordamiento: en `DYNAMIC` InnoDB solo saca una
  columna fuera de la fila cuando la fila no cabe en media página (~8 KB), y aquí no llega.

| Propuestas | Todo fresco (×1,3) | Todo anonimizado (×1,3) | Régimen realista (90 días frescas ≈10 %) |
|---|---|---|---|
| 1 000 | ~14 MB | ~8 MB | ~14 MB |
| 10 000 | ~135 MB | ~82 MB | ~90 MB |
| 100 000 | ~1,35 GB | ~820 MB | **~830 MB** |

**La conclusión operativa no es el disco, es la memoria.** 830 MB sobre 20 GiB de gp3 es el 4 %:
irrelevante. Pero la instancia es una `db.t4g.small` con **2 GiB de RAM**, o sea un
`innodb_buffer_pool_size` del orden de **1,5 GiB** — y ese es el número que importa: una tabla de
830 MB que alguien escanee entera desaloja el conjunto de trabajo de las 105 tablas restantes y
degrada **toda** la aplicación, no solo esta feature. De ahí las dos reglas de la sección 2.1: el
listado de consola va acotado por fecha y el veredicto se consulta por índice, nunca con
`SELECT *` sobre todo el histórico.

**Cuándo llegan esos números** (proyección, no medida): a 20 propuestas/día son ~7 300/año, o sea
10 000 en ~16 meses y 100 000 en ~13 años. A 140/día, 100 000 en ~2 años.

**Particionar: no, y con el número.** GitHub particionó cuando el problema era una flota de bases
con tablas de miles de millones de filas
([GitHub, 2021](https://github.blog/2021-09-27-partitioning-githubs-relational-databases-scale/));
Figma dejó el sharding para el final después de agotar índices y réplicas
([Figma](https://www.figma.com/blog/how-figmas-databases-team-lived-to-tell-the-scale/)). Aquí
hablamos de **una tabla de 10⁵ filas y menos de 1 GB en una instancia de 20 GiB**. La palanca
correcta, si algún día aprieta, es la que ya está en el diseño: el borrado físico a los 24 meses.

---

## 7. Migración y despliegue

- **Cuatro tablas nuevas + una en otra rodaja. Nada de expand/contract**: crear una tabla no
  bloquea nada.
- **`ai_proposal_conversions` va en un changeset propio**, después de `ai_proposals`: MySQL exige que
  la tabla referenciada exista (es el mismo motivo por el que `237` separa el ciclo entre
  `configurator_questions` y `configurator_options`).
- **`<rollback><dropTable/></rollback>` en cada uno**, en orden inverso a las FK.
- **`preConditions`**: `<not><tableExists/></not>` con `onFail="HALT"` en cada creación; y un
  `sqlCheck` en el changeset de `catalog_item_ai_hints` que compruebe que `catalog_items` tiene
  filas `ACTIVE` (sembrar hints contra un catálogo vacío deja la feature muda y nadie se entera).
- **Siembra de los hints**: un changeset aparte, `INSERT` por `code` (no por `id`), con `hint_revision = 1`
  y `superseded_at = NULL`, y sin `context` — el motivo lo explica `308` entero: los changesets con
  `context="local,e2e"` **nunca** se aplican en dev ni en prod y los entornos divergen sin que nada
  lo detecte.
- **El primer `ALTER` futuro previsible** es añadir una columna a `ai_proposals` (p. ej. un campo de
  scoring). Es `INSTANT` en 8.4 —«The `INSTANT` algorithm can add a column at any position»— salvo
  que se combine con otra acción que no lo sea, y con un tope de 64 versiones de fila antes de que
  haga falta reconstruir. **Un `ALTER` por columna, nunca uno que mezcle acciones.**
- **Backfill**: no hay. Tablas nuevas y vacías.
- **`ddl-auto: validate`**: las columnas `GENERATED … STORED` se mapean en Java con
  `@Column(insertable = false, updatable = false)` o no se mapean; los booleanos son `BOOLEAN` en
  Liquibase con `preferred_boolean_jdbc_type: TINYINT` (`application.yml:85`), **nunca `TINYINT(1)`**.
  Una divergencia aquí no rompe la feature: **tumba el arranque de la aplicación entera**.

---

## 8. Hallazgos, por severidad

> **[bloqueante]** La propuesta se sirve por `id` en la URL — diseño de la ruta pública
> **Criterio:** no hay `company_id` ni principal; ninguna regla de tenancy de este repositorio
> protege estas filas. El único control es la imposibilidad de adivinar el identificador.
> **Impacto:** contando de 1 en adelante, cualquiera se lleva el texto libre de todos los
> prospectos: nombre de la clínica, ciudad, facturación y a veces el correo del dueño. Es una fuga
> de datos personales bajo Ley 1581, no un bug de rendimiento.
> **Arreglo:** `public_token VARCHAR(43) CHARACTER SET ascii COLLATE ascii_bin`, 32 bytes de
> `SecureRandom`, `uq_ai_proposals_token`, `expires_at` a 30 días. **Se verifica** con un test que
> pida la propuesta con `?token=1` y espere 404, y con la ruta literal en `PublicRoutes.BUSINESS`.
> ⚠️ **Y con una segunda prueba que la v1 no pedía:** que el token **no aparezca en el MDC
> `http.path`** de ninguna petición (sección 2.1). Un token inadivinable que se escribe en Loki no
> es inadivinable: es público con 31 días de retención.

> **[bloqueante]** Una asociación a `CompanyJpaEntity` desde la rodaja `aiproposal` enciende las
> cuatro reglas de BE-COV sobre toda la feature — `VetSoftwareConditions.java:800-812` y `:849-880`
> **Criterio:** `laFeatureTieneDatosDeEmpresa` usa `anyMatch` sobre **todas** las `*JpaEntity` del
> árbol de paquetes de la feature; `perteneceAUnaEmpresa` devuelve `true` con un campo de tipo
> `CompanyJpaEntity` **o llamado `companyId`**, hasta cinco saltos de asociación.
> **Impacto:** la que revienta es `MUTACIONES_SQL_ACOTADAS_POR_EMPRESA` (`:160-164`): el `UPDATE` de
> anonimización por fecha no tiene empresa que nombrar y rompe el build, con tres salidas y las tres
> malas (sobrecarga falsa, `JdbcTemplate` fuera del gate, o renombrar el campo). *Blast radius*:
> **las otras tres** alcanzan todos los puertos y casos de uso de la rodaja, porque se filtran por
> `laFeatureTieneDatosDeEmpresa`; **esta cuarta se filtra por la entidad del propio repositorio**
> (`:962-986`) y alcanza `AiProposalJpaRepository`, que es donde vive el `UPDATE`. La distinción
> importa: **sin la asociación, esta regla no aplica y la exención es estructural** (sección 3.2).
> **Arreglo:** cero columnas de empresa en las tres tablas; el vínculo va en
> `ai_proposal_conversions`, en una rodaja que ya tiene empresa, con `ON DELETE RESTRICT`.
> **Se verifica** ejecutando `HexagonalArchitectureTest` y comprobando que las cuatro reglas siguen
> sin nombrar la rodaja.

> **[bloqueante]** `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` **no** exime `@NoAuthorizationRequired` —
> `HexagonalArchitectureTest.java:111-116` + `VetSoftwareConditions.java:552-566`
> **Criterio:** `evaluarElGate` exige `gate.isPresent() && soloAlcanzablePorSystem(...)`; un puerto
> público no tiene `@PreAuthorize`, así que `cerrado = false`. Y el disparador es
> `m.getName().contains("Company")` (`:571-573`): basta la palabra en el nombre de **cualquier**
> método del puerto de salida.
> **Impacto:** un `SystemAiProposalQueryPort.findAllByCompanyId` declarado en la **misma interfaz**
> que usa el caso de uso anónimo rompe el build, y el mensaje de error habla de fugas de tenant, que
> no es lo que pasa. Se pierde media tarde antes de encontrarlo.
> **Arreglo:** interfaces de salida segregadas —una para el camino anónimo, sin una sola mención a
> `Company`; otra para la consola—. **Se verifica** con `grep -c Company` sobre el puerto anónimo:
> tiene que dar 0.

> **[grave]** `SIN_IO_EXTERNO_EN_TRANSACCION` solo mira `RestClient` — `HexagonalArchitectureTest.java:331-334`
> **Criterio:** la regla es `alcanzarUnClienteHttp(RestClient.class)`. El SDK de AWS
> (`BedrockRuntimeClient`) no es `RestClient`: **la llamada al modelo no la vigila nadie**.
> **Impacto:** una invocación a Sonnet dentro de `@Transactional` retiene una conexión del pool de
> **10** durante segundos. Cinco prospectos concurrentes dejan la mitad del pool esperando a
> Bedrock; con el timeout por defecto de un SDK, la aplicación entera se queda sin conexiones.
> **Arreglo:** turno `PENDING` commiteado → llamada fuera de transacción → cierre en una segunda
> transacción; y añadir el tipo del cliente de Bedrock al parámetro de la regla. Issue abierto.

> **[grave]** Un servicio que implemente el puerto anónimo y uno SYSTEM a la vez pierde la exención —
> `VetSoftwareConditions.java:1130-1132`
> **Criterio:** `sinEmpresaDeLaQueTirar` = `soloAlcanzablePorSystem(clase) || sinAutorizacionDeEmpleado(clase)`,
> y la segunda exige que **todos** los puertos que implementa la clase estén anotados.
> **Impacto:** `CARGA_POR_ID_ACOTADA_POR_EMPRESA` y `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA`
> se encienden por una razón que no tiene nada que ver con el cambio que las disparó.
> **Arreglo:** una clase de servicio por puerto en toda la rodaja. **Se verifica** en revisión: ningún
> `implements A, B` donde A y B tengan gates distintos.

> **[grave]** `short_description` / `long_description` no pueden alojar el prompt —
> `229_create_catalog_items.xml:47-48`
> **Criterio:** son copy servido por `GET /catalog` y `GET /plans` a la landing pública; el hint es
> instrucción de modelo. Dos audiencias en una columna es acoplamiento en las dos direcciones.
> **Impacto:** marketing retoca una frase y **el golden set queda inválido sin que nadie lo note**;
> el equipo afina el prompt y cambia lo que ve el cliente, con riesgo de romper `MatchesContract` en
> el front del tenant. Y no hay dónde versionar: `catalog_items` guarda un solo estado.
> **Arreglo:** `catalog_item_ai_hints` versionada, con `hint_hash` generado y marcador de vigencia.

> **[grave]** El prompt renderizado por turno son ~16 KB
> **Criterio:** 52 artículos × ~300 caracteres. Es cinco veces el resto de la fila.
> **Impacto:** a 100 000 propuestas, ~2,2 GB extra que **superan el buffer pool de ~1,5 GiB** de la
> `db.t4g.small` — y el daño no se queda en esta feature: desaloja el conjunto de trabajo de las 105
> tablas. (Proyección sobre el supuesto de 1,4 turnos/propuesta.)
> **Arreglo:** guardar `prompt_version` + `catalog_snapshot_hash` (64 bytes) y reconstruir. Se
> verifica comparando `catalog_snapshot_hash` con el corpus vigente.

> **[grave]** Sin `CHECK`, «anonimizado» es una promesa del job y no un hecho
> **Criterio:** una invariante que solo vive en Java sobrevive hasta el primer fallo a mitad de lote.
> **Impacto:** un job interrumpido deja filas con `anonymized_at` puesto y `contact_email` dentro, y
> el informe de cumplimiento dice que están limpias.
> **Arreglo:** `chk_ai_proposals_anonimizado CHECK (anonymized_at IS NULL OR contact_email IS NULL)`,
> con las dos ramas en `IS NULL` para no caer en la trampa de `NULL` en `CHECK` que documenta `229`.

> **[grave]** La FK del veredicto no puede ser obligatoria
> **Criterio:** el modelo va a devolver códigos que no existen; ese es el dato que mide su calidad.
> **Impacto:** con `catalog_item_id NOT NULL` la única salida es descartar la alucinación en Java, y
> **se pierde exactamente la señal para la que existe la tabla**.
> **Arreglo:** `item_code VARCHAR(50) NOT NULL` (verbatim), `catalog_item_id BIGINT NULL` con FK,
> `verdict` con `UNKNOWN_CODE`, y `chk_ai_proposal_lines_resolved` para que solo `ACCEPTED` exija la
> FK resuelta. El `UNIQUE` va sobre `(turn_id, item_code)`, **no** sobre `catalog_item_id`: MySQL
> admite múltiples `NULL` en un índice único y el mismo código inventado podría repetirse.

> **[menor]** Añadir una columna `GENERATED … STORED` a una tabla con datos reconstruye la tabla y
> **no** permite DML concurrente
> **Criterio:** [manual, online DDL](https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html):
> `STORED` → `Rebuilds Table: Yes`, `Permits Concurrent DML: No`. `VIRTUAL` → `Instant: Yes`,
> `Permits Concurrent DML: Yes`.
> **Impacto:** el patrón de unicidad parcial de `226`/`210`/`206` no es gratis fuera del `CREATE TABLE`.
> Ya está abierto como incidencia **#532**.
> **Arreglo:** declarar todas las generadas en el changeset de creación. Si alguna vez hay que
> añadir una a una tabla poblada, `VIRTUAL` + índice único (InnoDB admite índices secundarios sobre
> columnas virtuales).

> **[menor]** `CHAR(43)`/`CHAR(64)` en utf8mb4 reservan 4 bytes por carácter y comparan sin acentos
> **Criterio:** precedente vivo en `353_create_legal_document_versions.xml:53-58`
> (`MODIFY COLUMN content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin`), e incidencia
> abierta **#633**.
> **Impacto:** 172 bytes por fila donde bastan 44, y —peor— dos tokens que difieren solo en
> mayúsculas se comparan como iguales bajo `utf8mb4_0900_ai_ci`.
> **Arreglo:** `ascii_bin` en `public_token`, `catalog_snapshot_hash`, `hint_hash` y
> `client_request_id`. Ninguna de esas columnas se une por `JOIN` a una columna utf8mb4, así que no
> hay riesgo de divergencia de colación que mate un índice.

> **[nota]** El listado de consola paginará con `OFFSET`
> **Criterio:** `Pages`/`PageResult` (BE-21) están sobre Spring Data, que es `OFFSET`.
> [use-the-index-luke.com/no-offset](https://use-the-index-luke.com/no-offset): el coste crece con el
> número de página porque el motor lee y descarta las filas saltadas.
> **Impacto:** a 100 000 propuestas, la página 500 lee ~100 000 entradas de índice para devolver 200.
> Proyección, no medida.
> **Arreglo hoy:** acotar el listado por rango de fechas por defecto. La paginación por keyset es
> una mejora futura y tocaría el contrato único de paginación, así que no se propone aquí.

---

## 9. Contrato para `db-migrations`

Orden y contenido de los changesets (numeración a partir de **381**, el siguiente libre — **380 ya
está ocupado por `380_seed_grooming_requires_services.xml`**, ver la cabecera de este documento):

| # | Changeset | Contenido | `rollback` |
|---|---|---|---|
| 381 | `create_catalog_item_ai_hints` | Tabla + 2 generadas `STORED` + FK + 3 `UNIQUE` + 2 `CHECK` | `dropTable` |
| 382 | `seed_catalog_item_ai_hints` | `INSERT` por `code`, `hint_revision = 1`, **sin `context`** | `DELETE` por `code` |
| 383 | `create_ai_proposals` | Tabla + `MODIFY` de colación (3 columnas, con `idempotency_key`) + generada `contact_email_hash` + 2 FK + **2 `UNIQUE`** (`public_token` · `contact_email_hash, idempotency_key`) + 6 `CHECK` + 3 índices. **Sin `consent_store_text`** | `dropTable` |
| 384 | `create_ai_proposal_turns` | Tabla + `version` + FK + 2 `UNIQUE` + 8 `CHECK` | `dropTable` |
| 385 | `create_ai_proposal_lines` | Tabla + **`version`** + **`reason_redacted_at`** + 2 FK + `UNIQUE` + **8 `CHECK`** (sección 2.3) | `dropTable` |
| 386 | `create_ai_proposal_conversions` | Tabla + 2 FK (`RESTRICT`) + 2 `UNIQUE` | `dropTable` |
| **387** | **`create_legal_document_acceptances`** | Tabla nueva (sección 2.1) + FK a `legal_document_versions` + `UNIQUE (subject_kind, subject_ref, legal_document_version_id)` + 1 `CHECK` de vocabulario de `subject_kind` | `dropTable` |
| ~~388~~ | ~~`deprecate_configurator_tables`~~ | ⛔ **SUPERADO.** La sección 5 de este documento razona sobre una base **con datos**; la premisa que gobierna `plan-implementacion-propuesta-ia.md` es que **la base está vacía y ningún changeset se ha aplicado nunca**, así que el configurador **se borra de verdad, esquema incluido** —`235`, `236`, `237`, `238`, `241` y `312` salen del repositorio y del `db.changelog-master.xml`—. No hay nada que deprecar porque no hay `quote_answers` con filas que proteger. **La sección 5 se conserva por su razonamiento, que sigue siendo el correcto para una base con datos** | — |

`preConditions` `onFail="HALT"`: `<not><tableExists/></not>` en 381 y 383-387; en 382, un `sqlCheck`
que exija `COUNT(*) > 0` sobre `catalog_items WHERE status = 'ACTIVE'`.

⚠️ **387 no depende de nada de `aiproposal`** y por eso puede adelantarse: el plan lo entrega en su
Fase 1.3b, junto con la ruta pública del aviso legal, **antes** que la rodaja. Si se retrasa, el
front no puede cerrar el consentimiento y la Fase 4.2 se queda bloqueada.

---

## 10. Medido / no medido

**Medido:** nada contra ninguna base de datos. Ni local ni dev: la especificación se decide leyendo
el esquema declarado, que es la fuente de verdad del proyecto, y las tablas propuestas no existen.

**Añadido en la revisión de 2026-08-29 (v3), leído y verificado en el árbol:**
`db/changelog/migrations/` — **`380_seed_grooming_requires_services.xml` existe**, así que el
siguiente libre es el 381 · `RequestLoggingContextFilter.java:37` (`getRequestURI()` al MDC),
`LogFieldPolicy.java:156` (`http.path` en `SCANNED`) y los patrones de `LogRedactor` — **ninguno
casa un base64url de 43 caracteres** · `PasswordResetController.java:44` y
`PlatformAccessController.java:89,108` — **los tres tokens anónimos del repo van por
`@RequestParam`** · `PublicRoutes.java:54-145` — **`/legal-documents/{code}/current` NO está en
`BUSINESS`** · `FindCurrentLegalDocumentUseCase` — exige `@authz.isMyCompany(#companyId)` ·
`LegalDocumentVersionJpaEntity` — **13 campos, ninguno de empresa** ·
`LegalDocumentVersionRepository` — **ningún método con `companyId` ni con `Company` en el nombre** ·
`VetSoftwareConditions.acotarPorEmpresaElSqlQueEscribe:962-986` — **la guarda es la entidad del
repositorio, no la rodaja** · `VetSoftwarePublicFront/src/features/legal/` — la casilla de
consentimiento **existe** y el texto legal **sale del bundle, no del servidor**. Sin acceso a
ninguna base de datos, como todo el documento.

**Leído y verificado en el árbol:** `229`, `231`, `234`, `226`, `213`, `235`, `236`, `238`, `241`,
`308`, `353`; `db.changelog-master.xml`; `HexagonalArchitectureTest.java`
(reglas y javadoc), `VetSoftwareConditions.java` (las condiciones citadas, con línea);
`PublicRoutes.java`; `GetPublicQuestionnaireUseCase`, `ResolveConfiguratorSelectionUseCase`,
`PublishedCatalogItemQueryPort`, `SelfServeQuoteService`, `QuoteJpaEntity`, `TokenCleanupRepository`.

**Razonado, no medido:** todos los tamaños de fila y los totales de la sección 6; el ritmo de
crecimiento; el efecto sobre el buffer pool; y que las reglas ArchUnit se comportarán como dice la
sección 3 —está deducido del código de las condiciones, **no** de una ejecución de
`mvn test -Dtest=HexagonalArchitectureTest`, que no se corrió a propósito—.

**No verificable sin base viva:** la colación efectiva del servidor RDS (el `parameter group` podría
no ser `utf8mb4_0900_ai_ci`) y el tamaño real del buffer pool en la `db.t4g.small`. Se dan como
supuestos, no como hechos.

**Advertencia de entorno vigente:** `docker-compose.yml:79` corre **MySQL 8.0.45** mientras RDS y
Testcontainers van a **8.4**. Cualquier medición hecha en el compose local sobre estas tablas puede
no reproducir lo que hace RDS —8.0 y 8.4 difieren en defaults del optimizador y en DDL—.

## 11. Fuentes

- [Índices multicolumna, regla del prefijo por la izquierda](https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html) — por qué `uq(turn_id, item_code)` cubre la FK de `turn_id`
- [Operaciones DDL online de InnoDB](https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html) — `ADD COLUMN` instant; `STORED` reconstruye y bloquea DML; `VIRTUAL` es instant; los bytes de longitud de `VARCHAR`
- [Columnas generadas](https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html) — solo funciones deterministas; `VIRTUAL` vs `STORED`; índices sobre columnas virtuales
- [Evolutionary Database Design](https://martinfowler.com/articles/evodb.html) y [Parallel Change](https://martinfowler.com/bliki/ParallelChange.html) — por qué el configurador se deprecia y no se borra
- [GitLab, evitar downtime en migraciones](https://docs.gitlab.com/development/database/avoiding_downtime_in_migrations/) — cuándo se puede completar la fase de *contract*
- [use-the-index-luke.com/no-offset](https://use-the-index-luke.com/no-offset) — el coste del `OFFSET` creciente
- [GitHub, particionado](https://github.blog/2021-09-27-partitioning-githubs-relational-databases-scale/) y [Figma](https://www.figma.com/blog/how-figmas-databases-team-lived-to-tell-the-scale/) — la escala a la que particionar empieza a pagar, dos órdenes de magnitud por encima de la nuestra
- Bill Karwin, *SQL Antipatterns* — «Jaywalking» (listas en una columna) es lo que evita
  `ai_proposal_lines`; «Fear of the Unknown» es lo que justifica `catalog_item_id NULL` en vez de un
  centinela

🤖 Generated with [Claude Code](https://claude.com/claude-code)
