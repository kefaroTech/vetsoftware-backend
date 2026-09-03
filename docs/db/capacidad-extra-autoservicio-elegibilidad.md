# Capacidad extra en autoservicio: elegibilidad vs. contenido del paquete

Auditoría de modelo de datos sobre la decisión «hacer de los `EXTRA_*` componentes de los tres
paquetes del changeset 309». Fecha: 2026-08-29. Changelog en 379. Base de datos vacía: ningún
changeset se ha aplicado nunca, así que los ficheros de `db/changelog/` se pueden editar o borrar
y no hay expand/contract ni backfill que diseñar. El código, `api/openapi.json`, los tipos
generados del front, ArchUnit y el suelo de JaCoCo sí están vivos.

No se consultó ninguna base de datos. Todo lo que sigue sale de los changesets y del código.

---

## 1. Qué significa realmente una fila de `bundle_components`

La tabla se declara en `src/main/resources/db/changelog/migrations/232_create_bundle_components.xml`:
`(bundle_item_id, component_item_id, quantity INT NOT NULL DEFAULT 1)`, `uq_bundle_components
(bundle_item_id, component_item_id)`, `chk_bundle_components_quantity CHECK (quantity > 0)`.

El esquema no lo dice; lo dicen los seis consumidores. Cada uno infiere lo mismo:

| Consumidor | `fichero:línea` | Qué infiere de una fila |
|---|---|---|
| `SelfServeCartGuard.rechazarCobroDoble` | `src/main/java/com/vetsoftware/app/quote/application/usecase/SelfServeCartGuard.java:64-68` | Que el componente **ya está pagado dentro del paquete**: si el mismo carrito nombra los dos, lanza `Catalog item X is already included in a bundle in the same request and cannot be charged twice`. |
| `PublishedCatalogItemQueryPort` (contrato del puerto) | `src/main/java/com/vetsoftware/app/quote/application/port/out/PublishedCatalogItemQueryPort.java:57` | «Un paquete se cobra por su propio precio y sus piezas *no* se cobran aparte». Es la frase que fija la semántica. |
| `JpaPublicPlanQueryPort.SQL_COMPONENTS` | `.../pricelist/infrastructure/persistence/JpaPublicPlanQueryPort.java:154-159` | Proyecta `bc.quantity` como las unidades del eje que **el paquete trae dentro** (`GET /plans`). |
| `PublicPlanCapacityDto.included` | `.../pricelist/application/dto/PublicPlanCapacityDto.java:15,36` | Javadoc literal: «las unidades del eje que el paquete trae dentro (`bundle_components.quantity`). **No es** `catalog_prices.included_quantity`». |
| `JpaPublicCatalogQueryPort.SQL_PACK_COMPONENTS` → `PublicCatalogPackResponse.componentCodes` | `.../JpaPublicCatalogQueryPort.java:170`; `.../web/response/PublicCatalogPackResponse.java:32` | Descripción publicada en el OpenAPI: «Rótulos de los artículos que el paquete incluye. **Ninguno de ellos se puede comprar además del paquete**». |
| `JpaSubscriptionItemCompositionPort.freeze` (2.º INSERT) | `.../subscription/infrastructure/persistence/JpaSubscriptionItemCompositionPort.java:57-66` | Al firmar, comprar el `BUNDLE` **entrega** los submódulos de todos sus componentes, congelados en `subscription_item_sub_modules`. Es la entrega física del contenido. |

Y un séptimo, de cobertura y no de precio: `SQL_MISSING_REQUIREMENTS`
(`.../quote/infrastructure/persistence/JpaCatalogQueryPorts.java:425-454`) expande el paquete en
sus componentes para dar por satisfecho un `REQUIRES` — otra lectura de «esto viene dentro».

**Conclusión sobre la pregunta 1:** una fila de `bundle_components` significa exactamente
**«incluido en el precio del paquete», con `quantity` = cuántas unidades incluye**. No hay ninguna
columna que module ese significado: `quantity` no es un cupo comprable, es la cifra que la portada
publica como incluida. No existe la lectura «vendible junto al paquete pero no incluido».

Hay un octavo consumidor, y es el que crea el problema:

| Consumidor | `fichero:línea` | Qué infiere |
|---|---|---|
| `SQL_PUBLISHED_ID_BY_CODE` (el gate) | `.../quote/infrastructure/persistence/JpaCatalogQueryPorts.java:311-333` | Que el componente **es contratable por autoservicio**. |
| `SQL_ITEMS`, columna proyectada `selfServiceEligible` | `.../pricelist/infrastructure/persistence/JpaPublicCatalogQueryPort.java:96-105` | Lo mismo, publicado como bandera en `GET /catalog`. |

Esos dos leen la misma tabla para responder **otra pregunta**. Ahí está la raíz.

---

## 2. Respuesta directa: ¿añadir filas `EXTRA_*` a los tres packs regala la capacidad extra?

**No exactamente «gratis»: hoy la deja *invendible*, la anuncia como incluida, y la regala el día
que exista el aprovisionamiento.** El desglose, camino por camino:

1. **El gate sí se abre.** Con la fila, `EXTRA_USER` pasa `SQL_PUBLISHED_ID_BY_CODE:323-332` y
   `selfServiceEligible` pasa a `true` en `GET /catalog`. Eso es lo único que la decisión logra.
2. **Y el carrito se cierra.** `SelfServeCartGuard.java:64-68` rechaza cualquier cesta que lleve
   `PACK_CLINIC` **y** `EXTRA_USER`, porque `findComponentCodesOfBundles` devolverá `EXTRA_USER`
   como componente. Mensaje: «…cannot be charged twice». **El caso de negocio que se quiere
   habilitar —pack + usuarios extra— es exactamente el que queda prohibido**, y por las dos
   puertas: `SelfServeQuoteService.java:196` y `PreviewQuoteService.java:110`.
3. **Se puede comprar suelto, sin producto debajo.** Una cesta con solo `EXTRA_USER` sí pasa: no
   hay bundle en la cesta y `EXTRA_USER` no tiene ningún arco `REQUIRES` en 309. Se vendería
   «usuario adicional» a alguien que no ha comprado nada.
4. **La portada promete que va incluido.** `GET /plans` publicaría `PACK_CLINIC` con una línea de
   capacidad `EXTRA_USER, included = bc.quantity = 1` (`JpaPublicPlanQueryPort.java:159` →
   `PublicPlanCapacityDto.included`), y `GET /catalog` lo listaría en `componentCodes`, cuyo texto
   de contrato dice «ninguno de ellos se puede comprar además del paquete». Es decir: se publica
   por escrito, en un endpoint anónimo, que cada pack **incluye un usuario adicional gratis**.
5. **Y es gratis de verdad en cuanto se construya el eslabón que falta.** Hoy nadie reacciona a
   `QuoteStatus.ACCEPTED` (javadoc de `SelfServeQuoteService`), así que ninguna compra crea
   `subscription_items`. El día que ese camino exista y expanda el paquete —que es la intención
   declarada en 309:206 y ya implementada para submódulos en
   `JpaSubscriptionItemCompositionPort.java:57-66`—, la unidad extra entrará como contenido del
   pack, sin línea de precio propia. El techo se calcula `included_quantity + quantity`
   (`ContractItemJpaRepository.java:130`, `CapacityGrantLine.java:62`), y esa suma no sabe nada de
   `bundle_components`.

Dicho de otra forma: **la sospecha del solicitante es correcta**. La semántica de la tabla es
«incluido en el precio», la opción elegida no la cambia, y el resultado inmediato ni siquiera es el
regalo: es que el pack + extras deja de poder cotizarse. No hay ninguna columna —ni `quantity` ni
otra— que permita a una fila decir «vendible pero no incluido».

---

## 3. El diagnóstico: una relación haciendo dos trabajos

`bundle_components` responde hoy a dos preguntas que no son la misma:

- **Contenido**: qué entrega el paquete por su precio. Consumido por el guard de doble cobro, la
  composición congelada al firmar, `componentCodes`, `PublicPlanCapacityDto.included` y la
  cobertura de `REQUIRES`.
- **Elegibilidad**: qué puede comprar un anónimo por autoservicio. Consumido por el gate y por la
  bandera `selfServiceEligible`.

Mientras las dos coincidieron (módulos que se venden sueltos y además van en packs) la conflación
no dolía. `EXTRA_*` es el primer caso donde divergen: son **vendibles y no incluidos**, y el modelo
no tiene dónde escribirlo. Es un caso de libro de *SQL Antipatterns*: una columna/relación
sobrecargada con dos significados, cuyo síntoma clásico es que para expresar el significado B hay
que mentir en el significado A.

La regla de modelado que aplica: **la elegibilidad es un atributo del artículo** (depende solo de
`catalog_items.id`, 3FN), no una propiedad derivada de una relación con otro artículo. Escribirla
como relación es lo que obliga a mentir.

---

## 4. Alternativa recomendada

### 4.1 Columna explícita `catalog_items.self_service`

```xml
<!-- en el createTable de 229_create_catalog_items.xml (la BD está vacía: se edita, no se parchea) -->
<column name="self_service" type="BOOLEAN"><constraints nullable="false"/></column>
```

- `type="BOOLEAN"` (Liquibase) + `preferred_boolean_jdbc_type: TINYINT` (`application.yml:85`).
  **Nunca `TINYINT(1)`**: con display width, Connector/J (`tinyInt1isBit=true`) reporta la columna
  como `Types.BIT` y `ddl-auto: validate` tumba el arranque.
- **Sin `defaultValue`**, coherente con la regla de la casa que declara 303 (solo `created_date`,
  `enabled` y `version` llevan valor por defecto). El valor lo escribe la semilla, artículo a
  artículo, que es donde la decisión comercial se lee.
- CHECK, con el mismo estilo y prefijo que los nueve de 229:

```sql
ADD CONSTRAINT chk_catalog_items_self_service
    CHECK (self_service = FALSE OR item_type IN ('MODULE','CAPACITY'))
```

  Deja fuera `ONE_TIME` (implantación, migración: cargos negociados) y `BUNDLE` (ya elegible por
  tipo; marcarlo sería un segundo sitio donde decir lo mismo). **La columna es `NOT NULL`, así que
  el CHECK no puede evaluar a `NULL`** y no cae en la trampa de MySQL de aceptar la fila.

### 4.2 El gate pasa a ser una unión, no una sustitución

`JpaCatalogQueryPorts.SQL_PUBLISHED_ID_BY_CODE:323-332` y su gemelo proyectado
`JpaPublicCatalogQueryPort.SQL_ITEMS:96-105` cambian **a la vez y al mismo predicado** (que sean
idénticos es la invariante que sostiene todo el diseño del puerto):

```sql
AND (ci.item_type = 'BUNDLE'
     OR (ci.item_type IN ('MODULE', 'CAPACITY')
         AND (ci.self_service = TRUE
              OR EXISTS (SELECT 1
                           FROM bundle_components bc
                           JOIN catalog_items b ON b.id = bc.bundle_item_id
                          WHERE bc.component_item_id = ci.id
                            AND bc.enabled = TRUE
                            AND b.enabled = TRUE
                            AND b.item_type = 'BUNDLE'
                            AND b.status = 'ACTIVE'))))
```

Aditivo a propósito: ningún artículo pierde elegibilidad, ningún módulo deja de poder comprarse
suelto, y el conjunto publicado por `GET /catalog` sigue siendo exactamente el que la contratación
acepta. Lo único que entra son los cuatro `EXTRA_*`.

**No es la opción peligrosa.** No se acepta «cualquier `CAPACITY` con precio»: se aceptan
exactamente las filas que un `UPDATE` de semilla marcó una a una. El catálogo interno sigue sin ser
enumerable: un rótulo no marcado devuelve `Optional.empty()`, indistinguible de «no existe».

### 4.3 Semilla: cuatro filas marcadas y dos arcos `REQUIRES` nuevos

En 308 (`INSERT` y `UPDATE` de convergencia), `self_service = TRUE` **solo** para `EXTRA_USER`,
`EXTRA_BRANCH`, `EXTRA_TERMINAL`, `EXTRA_STORAGE`; `FALSE` para los otros 22 artículos, incluidos
`CAPACITY_USER`, `CAPACITY_BRANCH` y `CAPACITY_TERMINAL`, que son contenido y no venta.

Y en 309, dos arcos `REQUIRES` nuevos que cierran el agujero 3 de la sección 2 con la maquinaria
que ya existe (`catalog_item_dependencies`, `relation_type IN ('REQUIRES','RECOMMENDS','EXCLUDES')`,
231:41):

```
EXTRA_USER     REQUIRES CORE            (nuevo)
EXTRA_BRANCH   REQUIRES CORE            (nuevo)
EXTRA_TERMINAL REQUIRES CASH_REGISTER   (ya existe, 309:164)
EXTRA_STORAGE  REQUIRES LAB_IMAGING     (ya existe, 309:176)
```

Con eso, `EXTRA_USER` suelto se rechaza nombrando lo que falta, y `PACK_CLINIC + EXTRA_USER` se
acepta porque la cobertura de `SQL_MISSING_REQUIREMENTS` expande el pack y encuentra `CORE` dentro.
La coherencia por pack sale gratis del grafo de dependencias: `PACK_SPA + EXTRA_STORAGE` seguirá
rechazándose porque `PACK_SPA` no trae `LAB_IMAGING`.

### 4.4 Lo que NO cambia

Cero filas nuevas en `bundle_components`. **El contenido efectivo y el precio de los tres packs
quedan idénticos**: mismos `componentCodes`, mismo `PublicPlanCapacityDto.included`, misma
composición congelada al firmar, mismo importe. `EXTRA_USER` sigue siendo una línea propia, con su
escalera de tramos (310:152-153: 1-8 a 12.000, 9+ a 9.000) aplicada entera por `QuoteLineFreezer`.
Se cobra.

### 4.5 Invariantes: qué queda garantizado y qué no

| Invariante | Cómo queda |
|---|---|
| Un artículo marcado `self_service` es MODULE o CAPACITY | `chk_catalog_items_self_service` (base) |
| Un componente de pack no se puede cobrar además del pack | `SelfServeCartGuard` (código) — **no declarable en la base**, sigue igual que hoy |
| Lo vendible por autoservicio tiene precio de entrada en el ciclo pedido | El `JOIN` del gate, fail-closed. **No es una constraint**: es cross-tabla y depende de la tarifa vigente. Debe cubrirlo un IT del *slice* de `pricelist`/`quote` |
| El gate y `selfServiceEligible` evalúan el mismo predicado | **No declarable**: son dos SQL nativos. Hoy ya es así por convención; el cambio debe tocarlos en el mismo commit |
| Un `EXTRA_*` comprado sube el techo del cliente | **Garantizado desde DC-2** (verificado en código, no en 2026-08-29): ver la sección 6, punto 1, corregido |

### 4.6 Forma del changeset y coste del `ALTER`

Dos formas posibles; recomiendo la primera:

1. **Editar 229 (columna + CHECK) y 308/309 (semilla).** La base está vacía; el estado final queda
   limpio y sin `ALTER` arqueológico. Es lo que la restricción del encargo autoriza expresamente.
2. Si `db-migrations` prefiere no tocar ficheros ya revisados: un changeset 380 con
   `ADD COLUMN self_service TINYINT NOT NULL` + `ADD CONSTRAINT` + `UPDATE` de los cuatro códigos.
   Funciona, pero al llegar 380 la tabla ya tiene las 26 filas de 308 y MySQL rellenaría con el
   default implícito `0` — comportamiento implícito que es justo lo que la casa evita. Si se elige
   esta vía, escribir el `UPDATE` de los 26 códigos explícitamente y no confiar en el relleno.
   Coste del DDL: `ADD COLUMN` es **INSTANT** en InnoDB 8.4 salvo las excepciones que enumera el
   manual de DDL online; irrelevante de todas formas con la tabla vacía.

`<rollback>`: en la forma 1, el rollback es el `dropTable` que 229 ya declara y el `DELETE` que 308
ya declara — no hay nada nuevo que escribir. En la forma 2, `dropColumn` + inversa del `UPDATE`
(que no tiene inversa fiel: dejar constancia en un `<comment>`).

### 4.7 Alternativas descartadas

- **`relation_type` en `bundle_components` (`INCLUDED` / `ADDON`).** Mantiene la conflación,
  obliga a 12 filas nuevas (3 packs × 4 extras) y a añadir `relation_type = 'INCLUDED'` a **seis**
  sentencias SQL. Una que se olvide se convierte en capacidad regalada o en un rechazo falso de
  doble cobro, y el fallo es silencioso en los dos sentidos. Más superficie, peor dirección de
  fallo.
- **Tabla puente `self_service_addons`.** Para una relación «artículo → sí/no» es una tabla de más:
  el atributo depende solo de la PK del artículo. Solo se justificaría si la elegibilidad tuviera
  que ser distinta por pack, y no lo es: la diferencia por pack ya la expresan los arcos `REQUIRES`.
- **Aceptar cualquier `CAPACITY` con precio.** La opción peligrosa del plan: expone el catálogo
  interno al embudo anónimo y reabre el oráculo de enumeración que el puerto existe para cerrar.

---

## 5. Coste

- **Esquema**: 1 columna + 1 CHECK. Sin migración, sin backfill, sin downtime (BD vacía).
- **SQL**: 2 sentencias nativas a tocar en el mismo commit (`JpaCatalogQueryPorts.java:311`,
  `JpaPublicCatalogQueryPort.java:80`).
- **Java** (`backend-feature`, no yo): `ddl-auto: validate` obliga a que la entidad refleje la
  columna. Por analogía con `min_quantity`, el radio es el *slice* `catalogitem`: `CatalogItem`,
  `CatalogItemJpaEntity`, mapper, `Create/UpdateCatalogItemCommand`, `CatalogItemDto`,
  `Create/UpdateCatalogItemService`, `CatalogItemController`, `Create/UpdateCatalogItemRequest`,
  `CatalogItemResponse` — **11 ficheros**, todos en el mismo *slice*. Ningún consumidor de
  `pricelist`, `quote`, `subscription` o `entitlement` necesita el campo salvo las dos SQL de
  arriba.
- **Contrato**: `api/openapi.json` se regenera (nuevo campo en `CatalogItemResponse` y en los dos
  requests, todos bajo `hasRole('SYSTEM')`). `selfServiceEligible` **no cambia de forma**, solo de
  valor: los tipos generados del front no se mueven por eso.
- **Front**: `selfServiceEligible` y `componentCodes` existen en `api.generated.d.ts` de
  `VetSoftwarePublicFront` pero **ningún componente de `src/` los consume todavía** — el radio de
  la UI pública es cero hoy. La consola de plataforma sí necesitaría la casilla nueva en el
  formulario de artículo.
- **Efecto en los packs**: ninguno. Mismo contenido, mismo precio.

---

## 6. Hallazgos colaterales, del mismo bloque y con la misma raíz

Se enumeran porque cualquier plan de monetización de capacidad extra los pisa. **Ninguno se
resuelve aquí**, salvo el punto 1, que quedó **obsoleto y se corrige abajo** (verificado leyendo
código, no reafirmado por confianza en esta misma auditoría — sesión de changesets 404-406,
2026-09-02).

1. **[RESUELTO desde que se escribió este documento — YA NO ES CIERTO] "Comprar capacidad extra
   no subiría el techo de nadie".** Cuando esto se redactó (changelog en 379) no existía el
   eslabón cotización aceptada → suscripción. **Ya existe**: DC-2 lo cierra con
   `AcceptQuoteService.execute` (`quote/application/usecase/AcceptQuoteService.java:68-71`), que
   —en la misma transacción de la aceptación, si la cotización tiene empresa— llama a
   `SubscriptionProvisioningPort.provisionFromAcceptedQuote`. Su único adaptador,
   `AcceptedQuoteSubscriptionProvisioner`
   (`quote/infrastructure/orchestration/AcceptedQuoteSubscriptionProvisioner.java:72-77`), invoca
   `ReplaceSubscriptionFromQuoteUseCase`, y `ReplaceSubscriptionFromQuoteService.execute`
   (`subscription/application/usecase/ReplaceSubscriptionFromQuoteService.java:135-184`) traduce
   **cada línea de la cotización aceptada** —vía `AcceptedQuoteContractLines.from`, que copia
   `quote.items()` campo a campo, sin recalcular nada— en un `SubscriptionItemLineCommand` que
   `createSubscriptionUseCase.execute(...)` persiste como `subscription_items`. Una línea
   `EXTRA_USER` o `EXTRA_BRANCH` en la cotización aceptada entra con su propio
   `capacityUnit`/`quantity`, y `EntitlementCalculator` (línea 335) suma los `ceiling()` de todas
   las líneas del mismo eje (`CapacityGrantLine.ceiling() = included_quantity + quantity`), así
   que **sí sube el techo**. No es un camino nuevo escrito para esta tarea: es DC-2, ya en
   producción, y se aplica igual a una cotización de autoservicio que a una de consola —
   `AcceptQuoteUseCase` no distingue el origen—. **La condición "elegibilidad es necesaria y no
   suficiente" ya no aplica**: con el gate abierto (404-406 + el cambio de SQL en el mismo PR),
   comprar un `EXTRA_*` por autoservicio y aceptarlo entrega la capacidad.
2. **[Grave] Los packs publican una terminal incluida que nadie concede.** `CAPACITY_TERMINAL` es
   componente de los tres packs con `quantity = 1` (309:229-262) y se publica como `included = 1`;
   pero es `structural_minimum = FALSE` (308:246 y ss.), así que no lo aprovisiona el alta inicial, y
   `catalog_item_limits` le fija `limit_quantity = 0` (313:160). Es exactamente el mismo defecto
   que la opción elegida multiplicaría por cuatro.
3. **[Menor] `EXTRA_STORAGE` es incomprable con `PACK_CLINIC`.** Requiere `LAB_IMAGING`, que solo
   trae `PACK_FULL`. Ya está anotado como hallazgo abierto en 309:149-152; con `EXTRA_*` vendible
   deja de ser teórico.
4. **[Nota] El configurador ya calcula unidades de `EXTRA_USER` y `EXTRA_TERMINAL`** (312:265-267)
   que hoy **ningún camino puede contratar**, porque el gate las rechaza. El embudo produce una
   recomendación que la contratación niega: es el síntoma que originó esta decisión.

---

## 7. Especificación para `db-migrations`

1. `229_create_catalog_items.xml`: añadir `<column name="self_service" type="BOOLEAN">` con
   `nullable="false"` y sin valor por defecto, y `chk_catalog_items_self_service` en el bloque
   `<sql>` existente, con el mismo estilo que los nueve CHECK actuales.
2. `308_seed_commercial_catalog_items.xml`: añadir `self_service` a la lista de columnas del
   `INSERT` y al `SET` del `UPDATE` de convergencia, con `TRUE` en los cuatro `EXTRA_*` y `FALSE`
   en los otros 22 códigos, explícito fila a fila.
3. `309_seed_commercial_catalog_relations.xml`: dos arcos `REQUIRES` nuevos en
   `catalog_item_dependencies` — `EXTRA_USER → CORE` y `EXTRA_BRANCH → CORE` —, con el mismo
   `NOT EXISTS` y el mismo `<rollback>` que las filas vecinas.
4. **No tocar `bundle_components`.** Ni una fila.

El cambio de las dos sentencias SQL nativas del gate y de la proyección es de `backend-feature`, no
de este changeset, pero **tiene que viajar en el mismo PR**: con la columna sembrada y el gate sin
tocar, no pasa nada (fail-closed); con el gate tocado y la columna sin sembrar, tampoco. El orden
seguro es datos primero.

## 8. Qué no se comprobó

- **Nada se midió contra ninguna base de datos**: sin `EXPLAIN`, sin cardinalidades, sin
  `information_schema`. No hacía falta: la pregunta es de semántica de modelo y se decide leyendo,
  y el encargo prohíbe expresamente consultar dev.
- No se ejecutó la suite (Maven prohibido en este encargo), así que no está comprobado qué tests
  concretos rompen. Por inspección, los candidatos directos son `SelfServeQuoteDoubleChargeTest`,
  `SelfServeQuoteRequirementsTest`, `SelfServeQuoteServiceTest`, `PreviewQuoteServiceTest` y los IT
  `QuoteCatalogQueryPortsIT`, `PublicCatalogQueryPortIT` y `PublicPlanQueryPortIT`.
- No se revisó la consola de plataforma (`VetSoftwareFront`) para el formulario de artículo del
  catálogo; el radio del front público sí se comprobó y es cero.
