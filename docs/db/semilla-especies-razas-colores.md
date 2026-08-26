# Semilla de los catálogos `species`, `breeds` y `animal_colors`

**Especificación para `db-migrations`.** Este documento **no** es un changeset y no autoriza a escribir
uno sin leer antes la §7.4, que contiene una trampa de *collation* capaz de dejar el catálogo a medias
en silencio en los entornos donde el changeset `218` ya corrió.

- **Autor:** agente de modelado de datos (`db-schema`)
- **Fecha:** 2026-08-25
- **Alcance:** tres tablas: `species`, `breeds`, `animal_colors`. No toca `animals` ni ninguna otra.
- **Estado de los catálogos hoy:** **los tres vacíos en una base nueva.** Verificado en §1.

---

## 0. Resumen ejecutable

| Pregunta | Respuesta |
|---|---|
| ¿Cuántas filas se siembran? | **18 especies · 577 razas · 411 colores** = 1006 filas |
| ¿Severidad de no hacerlo? | **Bloqueante.** `animals.specie_id`, `animals.breed_id` y `animals.color_id` son `NOT NULL`: con los catálogos vacíos **no se puede registrar ni un solo animal** (§1.2) |
| ¿El changeset `218` ya siembra colores? | Sí, pero es un **no-op en base nueva**: su `INSERT` está condicionado a `species.name IN ('Canino','Felino')` y `species` nunca se siembra (§1.3) |
| ¿Cuántas razas caninas? | **350**: las 347 filas de la nomenclatura FCI en español (344 razas, el Pastor Belga abierto en sus 4 variedades) + Sabueso Fino Colombiano + American Pit Bull Terrier + Mestizo. Criterio de corte razonado en §3.2 |
| ¿Cómo se llama «sin raza»? | **`Mestizo`**, una fila por especie. `Criollo` **no**, porque ya es el nombre de razas reales en bovino, porcino, ovino y equino (§3.3) |
| ¿Puede repetirse `Negro` en perro y en gato? | **Sí y debe.** El `UNIQUE` es `(specie_id, name)`, no global desde el changeset `218` (§3.4) |
| ¿Tildes? | **Sí, siempre.** La collation (`utf8mb4_0900_ai_ci`, **verificada**) es insensible a acento y a caja, y el buscador del front también pliega acentos: **el acento es presentación, no unicidad** (§3.5) |
| ¿Puede fallar el `INSERT` por dos nombres que solo difieren en tilde o mayúscula? | **No en esta lista.** Comprobado fila a fila con normalización `ai_ci`, en el ámbito real de cada índice: **0 colisiones** (§8.1). Sí colisiona con lo que dejó `218`, y eso se resuelve en §7.4 |
| ¿Longitud máxima real? | **68 caracteres** (`Perro Cobrador de Nueva Escocia (Nova Scotia Duck Tolling Retriever)`). Tope de columna: 100. Validado por script (§8.1) |
| ¿Riesgo abierto antes de sembrar? | Sí, dos: la normalización de acentos frente a lo que dejó `218` (§7.4) y el orden de aparición en el desplegable, que hoy es el orden de `id` (§3.6) |

---

## 1. Punto de partida — qué existe hoy, verificado contra el árbol

Censo sobre `VetSoftware/src/main/resources/db/changelog/migrations/` (277 ficheros).

| Changeset | Qué hace |
|---|---|
| `028_create_species.xml:8-21` | `CREATE TABLE species`: `id BIGINT AI PK`, `name VARCHAR(100) NOT NULL UNIQUE`, `created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `029_create_breeds.xml:8-28` | `CREATE TABLE breeds` + FK inline `fk_breeds_specie → species(id)` + `addUniqueConstraint uq_breeds_specie_name (specie_id, name)` |
| `041_create_animal_colors.xml:8-21` | `CREATE TABLE animal_colors` con `name VARCHAR(100) NOT NULL UNIQUE` **global** |
| `068_add_enabled_to_all_tables.xml:23-27, 55-59, 239-243` | `enabled BOOLEAN NOT NULL DEFAULT TRUE` en las tres tablas |
| `218_scope_animal_colors_by_specie.xml` | Añade `specie_id` a `animal_colors`, tira el `UNIQUE` global de `name`, lo cambia por `uq_animal_colors_specie_name (specie_id, name)` y **siembra un catálogo de capas de perro y gato** |
| `225_add_version_optimistic_lock_wave2.xml:48-77` | `version BIGINT NOT NULL DEFAULT 0` en las tres tablas |

**Ningún changeset inserta una sola fila en `species` ni en `breeds`.** Verificado con
`grep -ril "species\|breeds"` sobre los 277 ficheros: solo aparecen en `028`, `029`, `031`, `068`,
`218` y `225`, y ninguno de ellos contiene un `INSERT` a esas tablas.

### 1.1 Las tres tablas, tal como quedan

```
species        id BIGINT AI PK · name VARCHAR(100) NOT NULL UNIQUE · created_date DATETIME NOT NULL
               version BIGINT NOT NULL DEFAULT 0 · enabled BOOLEAN NOT NULL DEFAULT TRUE
breeds         id · name VARCHAR(100) NOT NULL · specie_id BIGINT NOT NULL FK→species(id)
               created_date · version · enabled · UNIQUE uq_breeds_specie_name (specie_id, name)
animal_colors  id · name VARCHAR(100) NOT NULL · specie_id BIGINT NOT NULL FK→species(id)
               created_date · version · enabled · UNIQUE uq_animal_colors_specie_name (specie_id, name)
```

Las entidades JPA confirman el contrato y añaden dos cosas que la semilla debe respetar:
`@SQLRestriction("enabled = true")` y `@Version`
(`SpecieJpaEntity.java:8-28`, `BreedJpaEntity.java:9-34`, `AnimalColorJpaEntity.java:9-35`).

**Ninguna de las tres tablas lleva `company_id`: son catálogos globales, compartidos por todas las
clínicas.** Eso tiene dos consecuencias que se arrastran por todo este documento: la semilla es única
para todo el SaaS (no hay que replicarla por tenant) y **cualquier fila creada por un tenant la ven
todos los demás** (§9-R2).

### 1.2 Qué se rompe hoy con los catálogos vacíos

`031_create_animals.xml:18-25` y `042_alter_animals_color_to_fk.xml:10-12`:

```xml
<column name="specie_id" type="BIGINT">
    <constraints nullable="false" foreignKeyName="fk_animals_specie" references="species(id)"/>
</column>
<column name="breed_id" type="BIGINT">
    <constraints nullable="false" foreignKeyName="fk_animals_breed" references="breeds(id)"/>
</column>
<column name="color_id" type="BIGINT">
    <constraints nullable="false" foreignKeyName="fk_animals_color" references="animal_colors(id)"/>
</column>
```

Las tres son `NOT NULL`. Con los catálogos vacíos no existe ningún `id` válido que poner: **una clínica
recién dada de alta no puede registrar ni un solo paciente**. Y no puede resolverlo por su cuenta,
porque el CRUD de estos catálogos no está en la app del tenant: `VetSoftwarePublicFront` solo consume
lectura (`features/breeds/api/breeds.api.ts`, `features/animal-colors/composables/useAnimalColorsBySpecie.ts`),
mientras que el mantenimiento vive en la consola de plataforma
(`VetSoftwareFront/src/features/species/views/SpeciesListView.vue`). La clínica **depende de que
alguien de la plataforma teclee las razas una a una**.

**Corolario duro para la semilla: cada especie sembrada necesita al menos una raza y al menos un color,
o esa especie queda inutilizable.** Está validado por script en §8.1.

### 1.3 El changeset `218` es un no-op en base nueva — y esto no es una curiosidad

`218_scope_animal_colors_by_specie.xml:96-128` siembra el catálogo de capas así:

```sql
INSERT IGNORE INTO animal_colors (name, created_date, enabled, specie_id)
SELECT n.name, CURRENT_TIMESTAMP, 1, s.id
  FROM species s CROSS JOIN ( ... ) n
 WHERE s.name IN ('Canino', 'Felino');
```

El `CROSS JOIN` es contra `species`. En una base nueva `species` está vacía, el `WHERE` no encuentra
nada y **el `INSERT` mete cero filas**. Liquibase marca el changeset como ejecutado igualmente y
`218` **no se volverá a ejecutar nunca**, ni siquiera cuando las especies existan.

Esto fija tres condiciones de la especificación:

1. La especie canina se llama exactamente **`Canino`** y la felina exactamente **`Felino`**. No
   `Perro`, no `Perros`, no `Canina`. Cualquier otro nombre deja huérfana la lógica de `218` y la de
   `218_animal_colors_purge_cross_specie`, que la referencia igual.
2. El changeset de colores de esta semilla **debe sembrar también las capas de perro y gato**, no
   darlas por hechas: en base nueva no están.
3. En los entornos donde `218` **sí** corrió con especies presentes (dev), el nuevo changeset debe ser
   idempotente y además **normalizar los acentos** que `218` dejó sin poner. Ver §7.4.

---

## 2. Qué se decide aquí y qué no

Esto es una especificación de **datos**, no de esquema. No se propone ni una sola `ALTER TABLE`:
las tres tablas están bien formadas para lo que hacen y el cambio más caro de todos —el tipo de las
columnas— ya está tomado y es correcto (`BIGINT AUTO_INCREMENT` como PK, `VARCHAR(100)` para el
nombre). Lo que sí se deja anotado, sin proponer cambio en este turno, son tres deudas de esquema
(§9): el índice de FK redundante, la ausencia de un criterio de orden y la ausencia de `company_id`
en un catálogo que el API deja mutar.

---

## 3. Decisiones de modelado, razonadas

### 3.1 Granularidad de especie: por qué `Ave` y `Reptil` son especies, y `Canario` una «raza»

El modelo tiene **exactamente dos niveles**: `species` y, colgando de ella, `breeds` y `animal_colors`.
No hay taxonomía, no hay `parent_id`, no hay orden ni familia. Toda la granularidad tiene que caber en
esos dos niveles, y la regla que la fija es esta:

> **Una fila de `species` es el nivel al que la clínica decide protocolo y dosis, y al que existe un
> catálogo de razas y de capas distinto del de sus vecinos.**

Con esa regla, para los mamíferos domésticos la especie es la especie biológica: un perro y un gato no
comparten ni razas ni nomenclatura de capa, y tampoco un bovino y un bufalino. De ahí las 16 primeras
filas de §4.

Para **aves y reptiles la regla se rompe al revés**: no existe el concepto de «raza» en un canario ni
en una iguana, y sembrar treinta especies biológicas de ave como filas de `species` produciría treinta
desplegables de razas vacíos —y, por §1.2, treinta especies inutilizables, porque `breed_id` es
`NOT NULL`—. Se resuelve con un compromiso **explícito y documentado**:

- `Ave` y `Reptil` son filas de `species` a nivel de **grupo**.
- La tabla `breeds` transporta ahí la **especie zoológica** (`Canario`, `Periquito Australiano`), no
  una raza.

Es una sobrecarga semántica de la columna, y se declara como tal: es el antipatrón que Karwin llama
*Entity-Attribute-Value* en su versión suave —una columna que significa dos cosas según el valor de
otra—. Se acepta a sabiendas porque la alternativa (un modelo taxonómico) es un cambio de esquema que
esta semilla no necesita para desbloquear el registro de pacientes, y se registra como deuda en §9-R6.

**Lo que NO se hace:** desglosar `Ave` en `Ave Ornamental` / `Ave de Corral`. Serían dos filas cuyo
único efecto sería partir en dos un desplegable de 17 entradas.

### 3.2 Cuántas razas caninas: entran todas las de la FCI

La FCI declara ~360 razas a título definitivo. La tentación es curar una lista de «las 60 que se ven en
Colombia». **Se descarta, y por un motivo que no es de gusto:** ninguna fuente publica esa lista. La
página de estadísticas de la FCI para Colombia
(`fci.be/es/statistics/ByNco.aspx?iso=CO`) da agregados de la Asociación Club Canino Colombiano
—cachorros inscritos, camadas, jueces—, **no inscripciones por raza**. Cualquier corte por
«frecuencia en Colombia» sería una opinión disfrazada de dato, y el encargo exige lo contrario.

El criterio que sí es verificable fila a fila es **la nomenclatura FCI en español completa**, y ese es
el que se aplica. Los tres argumentos que lo sostienen:

1. **Coste nulo.** 347 filas en una tabla global, sin `company_id`, ocupan unos pocos cientos
   de kilobytes contando el índice. La instancia es una `db.t4g.small` con 20 GiB; esto no se nota.
   No hay paginación que se degrade: el endpoint `GET /species/{specieId}/breeds` devuelve la lista
   entera y el front filtra en memoria, cosa que `SpeciesListView.vue:20-33` documenta como decisión
   consciente.
2. **El coste de quedarse corto no es simétrico.** Sobra una raza → el administrador de plataforma la
   desactiva en un clic (`@SQLDelete` deja `enabled = 0`). Falta una raza → la clínica **no puede**
   crearla, tiene que abrir un ticket a plataforma, y mientras tanto teclea el paciente con una raza
   equivocada. Ese dato equivocado no se limpia después.
3. **Sin catálogo, el catálogo lo inventa el usuario.** Es el mecanismo por el que estas tablas se
   llenan de `Labrador`, `labrador retriever` y `Lab` como tres filas distintas; y como la tabla es
   global, la basura de una clínica la ven las demás.

**Excepciones documentadas sobre la nomenclatura FCI:**

| Caso | Qué se hace | Por qué |
|---|---|---|
| Pastor Belga (FCI 15) | 4 filas: `(Groenendael)`, `(Tervueren)`, `(Malinois)`, `(Laekenois)` | Son las cuatro variedades del estándar y en Colombia se nombran por la variedad, nunca por la raza. `Malinois` sin más es lo que teclea un veterinario |
| Nombres FCI en español ilegibles en Colombia | Se añade el nombre original entre paréntesis: `Cobrador Dorado (Golden Retriever)`, `Caniche (Poodle)`, `Gran Danés (Deutsche Dogge)`, `Perro Crestado Rodesiano (Rhodesian Ridgeback)`, `Antiguo Perro de Pastor Inglés (Bobtail)`, y los tres cobradores restantes | El buscador del front es una subcadena literal sobre `name` (`coincide` en `@/composables/text`): no hay tabla de sinónimos. Si `Golden Retriever` no está **dentro** de la cadena, nadie lo encuentra |
| Teckel (FCI 148) | Una sola fila, `Teckel (Dachshund)` | Las 9 variedades del estándar (3 tallas × 3 pelos) no cambian el manejo clínico |
| Caniche, Spitz Alemán | Una fila cada uno | Idem: las tallas son variedades del mismo estándar |
| `Perro de Pastor Rumano de los Cárpatos "Carpatin"` | Se siembra sin las comillas dobles | Comillas dentro de un literal SQL dentro de un XML: tres capas de escape para nada |

**Dos razas que no son FCI y entran igual:**

- **`Sabueso Fino Colombiano`** — la FCI le dio reconocimiento **provisional** en febrero de 2026, a
  propuesta de la Asociación Club Canino Colombiano. Es la única raza canina colombiana y omitirla en
  un software veterinario colombiano sería difícil de explicar.
- **`American Pit Bull Terrier`** — **no** está reconocida por la FCI, pero la **Ley 1801 de 2016,
  art. 126** la nombra expresamente entre los ejemplares caninos potencialmente peligrosos. Una
  clínica colombiana necesita poder escribir esa raza tal cual porque de ella cuelgan obligaciones
  legales del tenedor. Las demás razas de esa lista legal ya existen en la nomenclatura FCI:
  American Staffordshire Terrier (286), Bullmastiff (157), Dobermann (143), Dogo Argentino (292),
  Dogo de Burdeos (116), Fila Brasileño (225), Mastín Napolitano (197), Bull Terrier (11),
  Presa Canario (346), Rottweiler (147), Staffordshire Bull Terrier (76) y Tosa (260).
  La ley cita además «Pit Bull Terrier» y «Pit Bull» como variantes del mismo nombre: **se siembra una
  sola fila**, porque tres filas casi idénticas garantizan que el mismo perro se registre de tres
  formas distintas.

Para el resto de especies el criterio de corte es otro y se declara sin adornos: **el organismo que
lleva el libro genealógico en Colombia**. UNAGA y Asocebú para bovinos, AGROSAVIA para las razas
criollas, Porkcolombia para porcinos, Fedequinas para equinos, ARBA para conejos, ICA para las
autorizadas como mascota. Lo que no pude verificar contra una fuente en esta sesión va marcado
`pendiente` en su tabla y agrupado en §9-R7; **no está inventado, está sin verificar, y la diferencia
importa**.

### 3.3 Cómo se llama la que no es raza: `Mestizo`

Es la entrada más usada de todo el catálogo y no puede faltar. Hay tres candidatos y la decisión no es
de estilo:

| Candidato | Veredicto |
|---|---|
| `Criollo` | **Descartado.** En este mismo catálogo `Criollo` ya es el nombre de **razas reales**: `Criollo Colombiano` (equino, Fedequinas), `Criolla` (ovino, AGROSAVIA), y toda la familia de razas criollas bovinas y porcinas. Usarlo también para «sin raza» hace que la palabra signifique dos cosas contrarias en la misma columna, y cualquier informe que agregue por raza mezcla peras con manzanas |
| `Sin raza definida` / `SRD` | **Descartado.** Se lee como el texto de ayuda de un formulario, no como un valor de catálogo. Es además el uso brasileño, no el colombiano |
| **`Mestizo`** | **Elegido.** Es el término de la literatura veterinaria en español para el animal sin pedigrí y de ascendencia mixta, no colisiona con ninguna raza real del catálogo, y es de uso corriente en Colombia junto a «criollo» y «gozque» |

Se siembra **una fila `Mestizo` por especie** (excepto `Ave` y `Reptil`, donde la fila de respaldo se
llama `Otra Ave` y `Otro Reptil` porque ahí «mestizo» no significa nada). Es obligatorio por §1.2:
sin ella, una especie con el catálogo de razas incompleto deja al usuario sin nada que elegir.

**Nota de producto, fuera de alcance de esta semilla:** «criollo» y «gozque» son lo que la gente dice
en Colombia. Como el buscador es literal, quien teclee «criollo» en el desplegable de razas caninas no
encontrará `Mestizo`. Eso se arregla con sinónimos, no con una segunda fila. Queda en §9-R4.

### 3.4 Colisiones de nombre entre especies: son legales y son deseadas

**Confirmado y explícito para que `db-migrations` no lo evite sin motivo:** el `UNIQUE` de las dos
tablas hijas es compuesto, `(specie_id, name)`, no `(name)`:

- `breeds`: `029_create_breeds.xml:25-27`, `uq_breeds_specie_name`.
- `animal_colors`: era `UNIQUE(name)` global en `041`, y el changeset `218_animal_colors_drop_unique_name`
  lo tiró expresamente para poder replicar un mismo color por especie. El comentario de ese changeset
  lo dice con todas las letras: *«El unique global de name impide replicar un mismo color por
  especie»*.

Por lo tanto:

- `Negro` existe **una vez por cada una de las 18 especies**. Es correcto.
- `Chinchilla` es a la vez una **especie** (`species.name = 'Chinchilla'`), una **raza de conejo**
  (ARBA: `American Chinchilla`, `Giant Chinchilla`, `Standard Chinchilla`) y un **patrón de capa
  felina** (`animal_colors.name = 'Chinchilla'`, sembrado por `218`). Las tres coexisten sin
  conflicto: son tablas distintas o especies distintas.
- `Hampshire` es raza porcina y raza ovina. Dos filas, dos `specie_id`. Correcto.
- `Appaloosa` es raza equina **y** capa equina (patrón moteado). Aquí sí hay dos filas con el mismo
  nombre, pero en **tablas distintas** (`breeds` y `animal_colors`): tampoco hay conflicto.
- `Harlequin`/`Arlequín` aparece como raza de conejo (ARBA, en inglés) y como capa canina y felina
  (en español). Tablas distintas.

Lo único que **sí** colisiona es un nombre repetido **dentro de la misma especie y la misma tabla**, y
el script de §8.1 verifica que no hay ninguno —incluida la comparación acento-insensible, que es la
que de verdad aplica—.

### 3.5 Acentos, mayúsculas y collation

**La collation efectiva es `utf8mb4_0900_ai_ci`, y está verificada, no deducida.** Se comprobó en tres
niveles —`@@collation_server`, el default del esquema `vetsoftware` y la propia columna `name`— y
coinciden los tres. Concuerda con lo que dice el árbol: ninguno de los tres `CREATE TABLE` declara
`CHARACTER SET` ni `COLLATE`, ningún otro changeset lo hace (`grep -rn "collation\|character_set\|
utf8mb4"` sobre `db/changelog/` solo encuentra un comentario en `215`) y el *parameter group* de RDS
(`VetSoftwareIaC/modules/database/main.tf:7-46`) fija exactamente cuatro parámetros
—`require_secure_transport`, `general_log`, `slow_query_log`, `long_query_time`— y **ninguno de
charset**. Todo se hereda del servidor, cuyo valor por omisión fija así el manual de MySQL 8.4:

> «By default, these are `utf8mb4` and `utf8mb4_0900_ai_ci`, but they can be set explicitly at server
> startup on the command line or in an option file and changed at runtime.»

**`ai_ci` = accent-insensitive **y** case-insensitive.** Las consecuencias, que son las que importan:

- `'Bulldog Inglés'`, `'Bulldog Ingles'` y `'BULLDOG INGLES'` son **la misma clave** para el índice
  único. Si la lista contuviera dos filas que solo difieren en tilde o en caja, el `INSERT` del seed
  **fallaría por violación de índice único** —y con `INSERT IGNORE`, que es el patrón de la casa,
  fallaría **en silencio**: la segunda fila simplemente no entraría y nadie se enteraría—.
- Lo mismo con `ñ` y `n`: `'Peñón'` = `'Penon'`.
- El ámbito de la colisión **no es el mismo en las tres tablas**: en `breeds` y `animal_colors` el
  `UNIQUE` es `(specie_id, name)`, así que dos especies distintas pueden repetir nombre y la colisión
  solo puede darse **dentro de una misma especie**; en `species` el `UNIQUE` **sí es global** sobre
  `name`.
- El buscador del front pliega acentos a propósito (`SpeciesListView.vue:29-32`: *«con
  `toLowerCase().includes()`, «canino» encontraría «Canino» pero nadie encontraría «Anfibio»
  tecleando rápido una palabra con tilde»*). O sea: **ni la base ni la búsqueda distinguen el
  acento**.

**De ahí sale la convención, y conviene decirlo con precisión: el acento es una decisión de
presentación, no de unicidad.** No cambia qué filas caben ni qué encuentra el usuario; solo cambia lo
que se lee en pantalla. Por eso se pone siempre y bien: no cuesta nada y su ausencia sí se ve.

**La convención, por tanto:**

| Regla | Valor |
|---|---|
| Tildes | **Siempre, ortografía española correcta.** Presentación, no unicidad: ni la collation ni el front las distinguen |
| Capitalización | **Tipo título**: mayúscula inicial en cada palabra significativa; `de`, `del`, `la`, `los`, `y`, `con`, `para`, `sin` en minúscula salvo al principio. `Perro de Pastor de los Abruzos y de la Maremma`, `Blanco y Negro` |
| Precedente | Es lo que ya hace el changeset `218`: `Negro y Fuego`, `Merle Azul`, `Blanco y Naranja`. **No se inventa una convención nueva** |
| Mayúsculas sostenidas | **No.** La FCI publica su nomenclatura en versales; se pasa a tipo título |
| Comillas dobles, punto y coma, tabuladores | **No.** Complican el literal SQL sin aportar |
| Nombre original entre paréntesis | Solo en la lista cerrada de §3.2 |

**Verificación obligatoria antes de sembrar:** el mismo dato que hace que `'Café'` y `'Cafe'` sean la
misma clave hace que el catálogo pueda quedarse a medias sin dar error. Está en §7.4.

### 3.6 El orden en que aparecen: hoy es el orden de `id`

`JpaBreedRepository.findBySpecieId` (`JpaBreedRepository.java:41-44`) llama a
`jpaRepository.findAllBySpecie_Id(specieId)` **sin `Sort`**, y `JpaSpecieRepository.findAll`
(`JpaSpecieRepository.java:29-32`) tampoco ordena. El controlador devuelve la lista tal cual
(`BreedController.java:54-61`). Sin `ORDER BY`, el orden que ve el usuario en el desplegable es el que
decida InnoDB, que en la práctica es el de la clave *clustered*: **el `id`, es decir, el orden de
inserción de la semilla**.

De ahí dos exigencias:

1. **`Mestizo` se inserta primero en cada especie**, para que caiga arriba del desplegable. Es la
   opción más elegida y ahora mismo no hay otra forma de subirla.
2. El resto se inserta **en orden alfabético español** dentro de cada especie, con la excepción de los
   caninos, que van **por grupo FCI y dentro del grupo por sección**, tal como los publica la FCI: es
   el orden de la fuente y hace que el diff del changeset sea revisable contra ella.

Añadir un `ORDER BY name` en el repositorio es trivial y el índice ya existe —`uq_breeds_specie_name
(specie_id, name)` sirve el `ORDER BY` sin `filesort`, por la regla del prefijo por la izquierda del
manual de MySQL—, pero eso es código de `src/` y no lo toca esta especificación. Queda en §9-R3.

### 3.7 Longitud

Tope duro de columna: `VARCHAR(100)`. El dominio lo repite en Java y **lanza** si se pasa
(`Specie.java:13-16`, `Breed.java:16-19`: `"name must be 100 chars or less"`). El nombre más largo de
toda esta semilla mide **68** caracteres. Validado por script, §8.1.

---

## 4. Catálogo `species` — 18 filas

Orden de inserción = orden de la tabla. `Canino` y `Felino` van primeras a propósito: son el 95 % del
volumen de una clínica de pequeños animales y el desplegable no se ordena (§3.6).

| # | `name` | Largo | Por qué está |
|---|---|---|---|
| 1 | `Canino` | 6 | Perros. Nombre exacto exigido por el changeset 218 (`s.name = 'Canino'`). |
| 2 | `Felino` | 6 | Gatos. Nombre exacto exigido por el changeset 218 (`s.name = 'Felino'`). |
| 3 | `Equino` | 6 | Caballos. Censo Pecuario Nacional ICA / Fedequinas. |
| 4 | `Asnal` | 5 | Asnos y burros. Fedequinas registra asnos; el ICA los censa aparte del equino. |
| 5 | `Mular` | 5 | Mulas y machos. Hibrido: no tiene razas; se siembra con raza de respaldo. |
| 6 | `Bovino` | 6 | Ganado vacuno. Censo Pecuario Nacional ICA. |
| 7 | `Bufalino` | 8 | Bufalos de agua. Censo Pecuario Nacional ICA (485.141 cabezas en 2023). |
| 8 | `Porcino` | 7 | Cerdos. Censo Pecuario Nacional ICA / Porkcolombia. |
| 9 | `Ovino` | 5 | Ovejas. Censo Pecuario Nacional ICA (1,8 M cabezas en 2023). |
| 10 | `Caprino` | 7 | Cabras. Censo Pecuario Nacional ICA (1,1 M cabezas en 2023). |
| 11 | `Conejo` | 6 | Leporidae. Autorizado como mascota por la Resolucion ICA 842 de 2010. |
| 12 | `Cobayo` | 6 | Caviidae (cuy, conejillo de Indias). Resolucion ICA 842 de 2010. |
| 13 | `Hámster` | 7 | Cricetidae. Resolucion ICA 842 de 2010. |
| 14 | `Chinchilla` | 10 | Chinchillidae. Resolucion ICA 842 de 2010. |
| 15 | `Jerbo` | 5 | Muridae (gerbil). Resolucion ICA 842 de 2010. |
| 16 | `Hurón` | 5 | Mustelidae. Resolucion ICA 842 de 2010. |
| 17 | `Ave` | 3 | Grupo. Aves ornamentales (Resolucion ICA 1862 de 2008) y aves de corral. |
| 18 | `Reptil` | 6 | Grupo. Tenencia muy restringida en Colombia; se atiende en consulta y rescate. |

**Nombres que NO se usan y por qué:** `Perro`/`Gato` (rompen `218`, §1.3) · `Cuy` (se prefiere
`Cobayo`, que es el término de la Resolución ICA 842 de 2010) · `Roedor` (agrupa cobayo, hámster,
chinchilla y jerbo, que no comparten ni dosis ni manejo) · `Exótico` (no es una especie, es una
categoría comercial) · `Pez` y `Erizo`: ver §9-R7.

---

## 5. Catálogo `breeds` — 577 filas

Recuento por especie:

| Especie | Filas | Fuente principal |
|---|---|---|
| `Canino` | 350 | Nomenclatura FCI en español (10 grupos) + Ley 1801 de 2016 art. 126 + FCI (reconoc. provisional 2026) |
| `Felino` | 54 | FIFe vía ACFEC (miembro colombiano de FIFe), sistema EMS en español + CFA |
| `Equino` | 8 | Fedequinas + UNAGA |
| `Asnal` | 2 | Fedequinas |
| `Mular` | 2 | Fedequinas |
| `Bovino` | 31 | UNAGA + Asocebú + AGROSAVIA (razas criollas y sintéticas colombianas) |
| `Bufalino` | 5 | Asociación Colombiana de Criadores de Búfalos (UNAGA) |
| `Porcino` | 10 | Porkcolombia + literatura sobre las tres razas criollas colombianas |
| `Ovino` | 14 | AGROSAVIA |
| `Caprino` | 9 | AGROSAVIA |
| `Conejo` | 54 | ARBA — recognized breeds |
| `Cobayo` | 8 | ACBA / ARBA |
| `Hámster` | 5 | Especies zoológicas (Resolución ICA 842 de 2010 autoriza el grupo) |
| `Chinchilla` | 3 | Especies zoológicas (Resolución ICA 842 de 2010) |
| `Jerbo` | 2 | Especies zoológicas (Resolución ICA 842 de 2010) |
| `Hurón` | 2 | Resolución ICA 842 de 2010 |
| `Ave` | 17 | Resolución ICA 1862 de 2008 (ornamentales) + Censo Pecuario Nacional ICA (corral) |
| `Reptil` | 1 | — |

### 5.1 `Canino` — 350 filas

Orden de inserción: `Mestizo` primero (§3.6), después los diez grupos FCI en orden, después las dos
razas no-FCI. La columna **FCI** es el número de la nomenclatura y es lo que hace auditable fila a
fila esta lista: se comprueba contra `fci.be/es/nomenclature/<grupo>.html`.

**Fila obligatoria de respaldo (se inserta primero)**

| `name` | Fuente |
|---|---|
| `Mestizo` | Decisión §3.3 |

**Grupo 1 - Perros de pastor y perros boyeros (excepto perros boyeros suizos)** — 46 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Pastor Alemán` | 166 | `Perro de Pastor Bergamasco` | 194 |
| `Australian Kelpie` | 293 | `Perro de Pastor de los Abruzos y de la Maremma` | 201 |
| `Perro de Pastor Belga (Groenendael)` | 15 | `Pastor Holandés` | 223 |
| `Perro de Pastor Belga (Tervueren)` | 15 | `Perro Lobo de Saarloos` | 311 |
| `Perro de Pastor Belga (Malinois)` | 15 | `Schapendoes Neerlandés` | 313 |
| `Perro de Pastor Belga (Laekenois)` | 15 | `Pastor Polaco de Tatra` | 252 |
| `Schipperke` | 83 | `Perro de Pastor Polaco de las Llanuras` | 251 |
| `Perro Lobo Checoslovaco` | 332 | `Perro de Pastor Portugués` | 93 |
| `Perro Pastor Croata` | 277 | `Antiguo Perro de Pastor Inglés (Bobtail)` | 16 |
| `Tchuvatch Eslovaco` | 142 | `Border Collie` | 297 |
| `Perro de Pastor Catalán` | 87 | `Collie Barbudo` | 271 |
| `Perro de Pastor Mallorquín` | 321 | `Collie de Pelo Corto` | 296 |
| `Perro Pastor Australiano` | 342 | `Collie de Pelo Largo` | 156 |
| `Pastor de Beauce` | 44 | `Perro Pastor de Shetland` | 88 |
| `Pastor de Brie` | 113 | `Perro de Pastor Rumano de los Cárpatos` | 350 |
| `Pastor de los Pirineos de Cara Rasa` | 138 | `Perro Pastor Rumano de Mioritza` | 349 |
| `Pastor de Picardía` | 176 | `Perro de Pastor de Rusia Meridional` | 326 |
| `Perro Pastor de los Pirineos de Pelo Largo` | 141 | `Pastor Blanco Suizo` | 347 |
| `Komondor` | 53 | `Boyero Australiano` | 287 |
| `Kuvasz` | 54 | `Boyero de las Ardenas` | 171 |
| `Mudi` | 238 | `Boyero de Flandes` | 191 |
| `Puli` | 55 | `Welsh Corgi (Cardigan)` | 38 |
| `Pumi` | 56 | `Welsh Corgi (Pembroke)` | 39 |

**Grupo 2 - Tipo pinscher y schnauzer, molosoides, tipo montaña y boyeros suizos** — 53 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Affenpinscher` | 186 | `Mastiff` | 264 |
| `Dobermann` | 143 | `Cimarrón Uruguayo` | 353 |
| `Pinscher Alemán` | 184 | `Hovawart` | 190 |
| `Pinscher Miniatura` | 185 | `Leonberger` | 145 |
| `Pinscher Austriaco` | 64 | `Landseer (Tipo Europeo Continental)` | 226 |
| `Perro de Granja Danés y Sueco` | 356 | `Pastor de Bosnia y Herzegovina - Croacia (Tornjak)` | 355 |
| `Schnauzer` | 182 | `Terranova` | 50 |
| `Schnauzer Gigante` | 181 | `Pastor de Karst` | 278 |
| `Schnauzer Miniatura` | 183 | `Mastín del Pirineo` | 92 |
| `Perro Smous Holandés` | 308 | `Mastín Español` | 91 |
| `Terrier Ruso Negro` | 327 | `Perro de Montaña de los Pirineos` | 137 |
| `Boxer` | 144 | `Perro de Pastor Yugoslavo de Charplanina` | 41 |
| `Gran Danés (Deutsche Dogge)` | 235 | `Perro de Montaña del Atlas` | 247 |
| `Rottweiler` | 147 | `Perro de Castro Laboreiro` | 170 |
| `Dogo Argentino` | 292 | `Perro de la Sierra de la Estrela` | 173 |
| `Fila Brasileño` | 225 | `Rafeiro del Alentejo` | 96 |
| `Shar Pei` | 309 | `Perro de Pastor Rumano de Bucovina` | 357 |
| `Broholmer` | 315 | `Perro Pastor de Asia Central` | 335 |
| `Perro Dogo Mallorquín (Ca de Bou)` | 249 | `Perro Pastor del Cáucaso` | 328 |
| `Presa Canario` | 346 | `San Bernardo` | 61 |
| `Dogo de Burdeos` | 116 | `Dogo del Tíbet` | 230 |
| `Cane Corso` | 343 | `Perro Pastor Kangal` | 331 |
| `Mastín Napolitano` | 197 | `Boyero de Montaña Bernés` | 45 |
| `Tosa` | 260 | `Gran Boyero Suizo` | 58 |
| `Fila de San Miguel` | 340 | `Perro Boyero de Appenzell` | 46 |
| `Bulldog` | 149 | `Perro Boyero de Entlebuch` | 47 |
| `Bullmastiff` | 157 |  |  |

**Grupo 3 - Terriers** — 34 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Terrier Cazador Alemán` | 103 | `Cairn Terrier` | 4 |
| `Terrier Brasileño` | 341 | `Dandie Dinmont Terrier` | 168 |
| `Kerry Blue Terrier` | 3 | `Norfolk Terrier` | 272 |
| `Soft Coated Wheaten Terrier Irlandés` | 40 | `Norwich Terrier` | 72 |
| `Terrier Glen de Imaal Irlandés` | 302 | `Scottish Terrier` | 73 |
| `Terrier Irlandés` | 139 | `Sealyham Terrier` | 74 |
| `Airedale Terrier` | 7 | `Skye Terrier` | 75 |
| `Bedlington Terrier` | 9 | `Terrier Jack Russell` | 345 |
| `Border Terrier` | 10 | `West Highland White Terrier` | 85 |
| `Fox Terrier de Pelo Alambre` | 169 | `Terrier Checo` | 246 |
| `Fox Terrier de Pelo Liso` | 12 | `American Staffordshire Terrier` | 286 |
| `Lakeland Terrier` | 70 | `Bull Terrier` | 11 |
| `Manchester Terrier` | 71 | `Bull Terrier Miniatura` | 359 |
| `Parson Russell Terrier` | 339 | `Staffordshire Bull Terrier` | 76 |
| `Welsh Terrier` | 78 | `Terrier Sedoso Australiano` | 236 |
| `Australian Terrier` | 8 | `Terrier de Juguete Inglés Negro y Fuego` | 13 |
| `Terrier Japonés` | 259 | `Yorkshire Terrier` | 86 |

**Grupo 4 - Teckels** — 1 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Teckel (Dachshund)` | 148 |  |  |

**Grupo 5 - Perros tipo spitz y tipo primitivo** — 46 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Perro Esquimal Canadiense` | 211 | `Eurásico` | 291 |
| `Husky Siberiano` | 270 | `Chow Chow` | 205 |
| `Malamute de Alaska` | 243 | `Korea Jindo Dog` | 334 |
| `Perro de Groenlandia` | 274 | `Akita` | 255 |
| `Samoyedo` | 212 | `Akita Americano` | 344 |
| `Perro de Osos de Carelia` | 48 | `Hokkaido` | 261 |
| `Spitz Finlandés` | 49 | `Kai` | 317 |
| `Cazador de Alces Noruego Gris` | 242 | `Kishu` | 318 |
| `Cazador de Alces Noruego Negro` | 268 | `Shiba` | 257 |
| `Lundehund Noruego` | 265 | `Shikoku` | 319 |
| `Laika de Siberia Occidental` | 306 | `Spitz Japonés` | 262 |
| `Laika de Siberia Oriental` | 305 | `Thai Bangkaew Dog` | 358 |
| `Laika Ruso-Europeo` | 304 | `Basenji` | 43 |
| `Perro Cazador de Alces Sueco` | 42 | `Canaan Dog` | 273 |
| `Spitz de Norrbotten` | 276 | `Perro del Faraón` | 248 |
| `Pastor Finlandés de Laponia` | 284 | `Xoloitzcuintle` | 234 |
| `Perro Finlandés de Laponia` | 189 | `Perro sin Pelo del Perú` | 310 |
| `Perro de Pastor Islandés` | 289 | `Podenco Canario` | 329 |
| `Buhund Noruego` | 237 | `Podenco Ibicenco` | 89 |
| `Perro Sueco de Laponia` | 135 | `Cirneco del Etna` | 199 |
| `Spitz de los Visigodos` | 14 | `Podenco Portugués` | 94 |
| `Spitz Alemán` | 97 | `Thai Ridgeback Dog` | 338 |
| `Volpino Italiano` | 195 | `Perro de Taiwán` | 348 |

**Grupo 6 - Tipo sabueso, perros de rastro y razas semejantes** — 70 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Chien de Saint Hubert` | 84 | `Sabueso Anglo-Francés de Tamaño Mediano` | 325 |
| `Rastreador Brasileño` | 275 | `Sabueso Artesiano` | 28 |
| `American Foxhound` | 303 | `Sabueso del Ariege` | 20 |
| `Perro Negro y Fuego para la Caza del Mapache` | 300 | `Sabueso Helénico` | 214 |
| `Billy` | 25 | `Sabueso Húngaro de Transilvania` | 241 |
| `Gascon Saintongeois` | 21 | `Sabueso Italiano de Pelo Duro` | 198 |
| `Gran Grifón Vendeano` | 282 | `Sabueso Italiano de Pelo Raso` | 337 |
| `Gran Sabueso Anglo-Francés Blanco y Naranja` | 324 | `Sabueso de Montaña del Montenegro` | 279 |
| `Gran Sabueso Anglo-Francés Blanco y Negro` | 323 | `Sabueso de Hygen` | 266 |
| `Gran Sabueso Anglo-Francés Tricolor` | 322 | `Sabueso Halden` | 267 |
| `Gran Sabueso Azul de Gascuña` | 22 | `Sabueso Noruego` | 203 |
| `Sabueso Francés Blanco y Naranja` | 316 | `Gonczy Polski` | 354 |
| `Sabueso Francés Blanco y Negro` | 220 | `Harrier` | 295 |
| `Sabueso Francés Tricolor` | 219 | `Sabueso Serbio` | 150 |
| `Sabueso Poitevin` | 24 | `Sabueso Tricolor Serbio` | 229 |
| `Sabueso Polaco` | 52 | `Sabueso de Smaland` | 129 |
| `Foxhound Inglés` | 159 | `Sabueso Hamilton` | 132 |
| `Perro de Nutria` | 294 | `Sabueso Schiller` | 131 |
| `Sabueso Austriaco Negro y Fuego` | 63 | `Sabueso Suizo` | 59 |
| `Sabueso del Tirol` | 68 | `Perro Tejonero de Westfalia` | 100 |
| `Sabueso Estirio de Pelo Áspero` | 62 | `Sabueso Alemán` | 299 |
| `Sabueso Bosnio de Pelo Cerdoso (Barak)` | 155 | `Basset Artesiano de Normandía` | 34 |
| `Sabueso de Istria de Pelo Corto` | 151 | `Basset Azul de Gascuña` | 35 |
| `Sabueso de Istria de Pelo Duro` | 152 | `Basset Leonado de Bretaña` | 36 |
| `Sabueso del Valle de Save` | 154 | `Gran Basset Grifón Vendeano` | 33 |
| `Sabueso Eslovaco` | 244 | `Pequeño Basset Grifón Vendeano` | 67 |
| `Sabueso Español` | 204 | `Basset Hound` | 163 |
| `Sabueso Finlandés` | 51 | `Beagle` | 161 |
| `Beagle-Harrier` | 290 | `Perro Tejonero Sueco` | 130 |
| `Briquet Grifón Vendeano` | 19 | `Sabueso Suizo Pequeño` | 60 |
| `Grifón Azul de Gascuña` | 32 | `Rastreador de Hannover` | 213 |
| `Grifón del Nivernais` | 17 | `Rastreador Montañés de Baviera` | 217 |
| `Grifón Leonado de Bretaña` | 66 | `Dachsbracke de los Alpes` | 254 |
| `Pequeño Sabueso Azul de Gascuña` | 31 | `Dálmata` | 153 |
| `Porcelaine` | 30 | `Perro Crestado Rodesiano (Rhodesian Ridgeback)` | 146 |

**Grupo 7 - Perros de muestra** — 36 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Braco Alemán de Pelo Corto` | 119 | `Deutsch Langhaar` | 117 |
| `Perro de Muestra Alemán de Pelo Cerdoso` | 232 | `Gran Münsterländer` | 118 |
| `Perro de Muestra Alemán de Pelo Duro` | 98 | `Pequeño Münsterländer` | 102 |
| `Pudelpointer` | 216 | `Spaniel Azul de Picardía` | 106 |
| `Weimaraner` | 99 | `Spaniel Bretón` | 95 |
| `Antiguo Perro de Muestra Danés` | 281 | `Spaniel de Pont-Audemer` | 114 |
| `Braco Eslovaco de Pelo Duro` | 320 | `Spaniel Francés` | 175 |
| `Perdiguero de Burgos` | 90 | `Spaniel Picardo` | 108 |
| `Braco de Auvernia` | 180 | `Perdiguero de Drente` | 224 |
| `Braco del Ariege` | 177 | `Perdiguero Frisón` | 222 |
| `Braco del Borbonesado` | 179 | `Grifón de Muestra de Pelo Duro` | 107 |
| `Braco Francés - Tipo Gascuña` | 133 | `Espinone` | 165 |
| `Braco Francés - Tipo Pirineos` | 134 | `Grifón de Muestra Bohemio de Pelo Duro` | 245 |
| `Braco Saint-Germain` | 115 | `Pointer Inglés` | 1 |
| `Braco Húngaro de Pelo Corto` | 57 | `Setter Irlandés Rojo` | 120 |
| `Braco Húngaro de Pelo Duro` | 239 | `Setter Irlandés Rojo y Blanco` | 330 |
| `Braco Italiano` | 202 | `Gordon Setter` | 6 |
| `Perdiguero Portugués` | 187 | `Setter Inglés` | 2 |

**Grupo 8 - Cobradores de caza, levantadores de caza y perros de agua** — 22 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Perro Cobrador de Nueva Escocia (Nova Scotia Duck Tolling Retriever)` | 312 | `Field Spaniel` | 123 |
| `Chesapeake Bay Retriever` | 263 | `Springer Spaniel Galés` | 126 |
| `Cobrador de Pelo Liso (Flat Coated Retriever)` | 121 | `Springer Spaniel Inglés` | 125 |
| `Cobrador de Pelo Rizado (Curly Coated Retriever)` | 110 | `Sussex Spaniel` | 127 |
| `Cobrador Dorado (Golden Retriever)` | 111 | `Perro de Agua Español` | 336 |
| `Labrador Retriever` | 122 | `Perro de Agua Americano` | 301 |
| `Perdiguero Alemán` | 104 | `Perro de Agua Francés` | 105 |
| `Cocker Spaniel Americano` | 167 | `Perro de Agua Irlandés` | 124 |
| `Nederlandse Kooikerhondje` | 314 | `Perro de Agua de Romagna` | 298 |
| `Clumber Spaniel` | 109 | `Perro de Agua Frisón` | 221 |
| `Cocker Spaniel Inglés` | 5 | `Perro de Agua Portugués` | 37 |

**Grupo 9 - Perros de compañía** — 26 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Bichón de Pelo Rizado` | 215 | `Spaniel Tibetano` | 231 |
| `Bichón Habanero` | 250 | `Terrier Tibetano` | 209 |
| `Maltés` | 65 | `Chihuahueño` | 218 |
| `Bolognese` | 196 | `Cavalier King Charles Spaniel` | 136 |
| `Coton de Tulear` | 283 | `King Charles Spaniel` | 128 |
| `Pequeño Perro León` | 233 | `Pekinés` | 207 |
| `Caniche (Poodle)` | 172 | `Spaniel Japonés` | 206 |
| `Griffon Belge` | 81 | `Spaniel Continental Enano de Compañía` | 77 |
| `Griffon Bruxellois` | 80 | `Pequeño Perro Ruso` | 352 |
| `Petit Brabançon` | 82 | `Kromfohrländer` | 192 |
| `Perro Crestado Chino` | 288 | `Pug` | 253 |
| `Lhasa Apso` | 227 | `Boston Terrier` | 140 |
| `Shih Tzu` | 208 | `Bulldog Francés` | 101 |

**Grupo 10 - Lebreles** — 13 filas

| `name` | FCI | `name` | FCI |
|---|---|---|---|
| `Lebrel Afgano` | 228 | `Pequeño Lebrel Italiano` | 200 |
| `Saluki` | 269 | `Azawakh` | 307 |
| `Borzoi - Lebrel Ruso para la Caza` | 193 | `Sloughi` | 188 |
| `Lebrel Irlandés` | 160 | `Lebrel Polaco` | 333 |
| `Lebrel Escocés` | 164 | `Greyhound` | 158 |
| `Galgo Español` | 285 | `Whippet` | 162 |
| `Lebrel Húngaro` | 240 |  |  |

**Razas no reconocidas por la FCI que se siembran igual**

| `name` | Fuente | Por qué |
|---|---|---|
| `Sabueso Fino Colombiano` | FCI — reconocimiento **provisional**, feb. 2026, a propuesta de la Asociación Club Canino Colombiano | Única raza canina colombiana |
| `American Pit Bull Terrier` | **Ley 1801 de 2016, art. 126** | La ley colombiana la nombra como ejemplar potencialmente peligroso; de ahí cuelgan obligaciones del tenedor |

### 5.2 `Felino` — 54 filas

Fuente principal: **ACFEC** (Asociación Colombiana de Felinos, club colombiano miembro de FIFe), que
publica el sistema EMS **en español**. Es la mejor fuente posible para este proyecto: es de FIFe y es
colombiana, así que los nombres son los que se usan aquí. Las tres filas marcadas *CFA* son razas que
FIFe no reconoce y la Cat Fanciers' Association sí.

| `name` | Código EMS / fuente |
|---|---|
| `Mestizo` | Decisión §3.3. Sustituye a los códigos FIFe `HCS`/`HCL` (*house cat*), que no son nombres de raza |
| `Exótico` | FIFe/ACFEC — EMS EXO — categoría 1 |
| `Persa` | FIFe/ACFEC — EMS PER — categoría 1 |
| `Ragdoll` | FIFe/ACFEC — EMS RAG — categoría 1 |
| `Sagrado de Birmania` | FIFe/ACFEC — EMS SBI — categoría 1 |
| `Van Turco` | FIFe/ACFEC — EMS TUV — categoría 1 |
| `American Curl Longhair` | FIFe/ACFEC — EMS ACL — categoría 2 |
| `American Curl Shorthair` | FIFe/ACFEC — EMS ACS — categoría 2 |
| `LaPerm Longhair` | FIFe/ACFEC — EMS LPL — categoría 2 |
| `LaPerm Shorthair` | FIFe/ACFEC — EMS LPS — categoría 2 |
| `Maine Coon` | FIFe/ACFEC — EMS MCO — categoría 2 |
| `Neva Masquerade` | FIFe/ACFEC — EMS NEM — categoría 2 |
| `Gato de los Bosques de Noruega` | FIFe/ACFEC — EMS NFO — categoría 2 |
| `Siberiano` | FIFe/ACFEC — EMS SIB — categoría 2 |
| `Angora Turco` | FIFe/ACFEC — EMS TUA — categoría 2 |
| `Bengalí` | FIFe/ACFEC — EMS BEN — categoría 3 |
| `Británico Longhair` | FIFe/ACFEC — EMS BLH — categoría 3 |
| `Burmilla` | FIFe/ACFEC — EMS BML — categoría 3 |
| `Británico Shorthair` | FIFe/ACFEC — EMS BSH — categoría 3 |
| `Birmano` | FIFe/ACFEC — EMS BUR — categoría 3 |
| `Chartreux` | FIFe/ACFEC — EMS CHA — categoría 3 |
| `Cymric` | FIFe/ACFEC — EMS CYM — categoría 3 |
| `Europeo` | FIFe/ACFEC — EMS EUR — categoría 3 |
| `Kurilean Bobtail Longhair` | FIFe/ACFEC — EMS KBL — categoría 3 |
| `Kurilean Bobtail Shorthair` | FIFe/ACFEC — EMS KBS — categoría 3 |
| `Korat` | FIFe/ACFEC — EMS KOR — categoría 3 |
| `Manx` | FIFe/ACFEC — EMS MAN — categoría 3 |
| `Mau Egipcio` | FIFe/ACFEC — EMS MAU — categoría 3 |
| `Ocicat` | FIFe/ACFEC — EMS OCI — categoría 3 |
| `Singapura` | FIFe/ACFEC — EMS SIN — categoría 3 |
| `Snowshoe` | FIFe/ACFEC — EMS SNO — categoría 3 |
| `Sokoke` | FIFe/ACFEC — EMS SOK — categoría 3 |
| `Selkirk Rex Longhair` | FIFe/ACFEC — EMS SRL — categoría 3 |
| `Selkirk Rex Shorthair` | FIFe/ACFEC — EMS SRS — categoría 3 |
| `Abisinio` | FIFe/ACFEC — EMS ABY — categoría 4 |
| `Balinés` | FIFe/ACFEC — EMS BAL — categoría 4 |
| `Cornish Rex` | FIFe/ACFEC — EMS CRX — categoría 4 |
| `Devon Rex` | FIFe/ACFEC — EMS DRX — categoría 4 |
| `Don Sphynx` | FIFe/ACFEC — EMS DSP — categoría 4 |
| `German Rex` | FIFe/ACFEC — EMS GRX — categoría 4 |
| `Bobtail Japonés` | FIFe/ACFEC — EMS JBS — categoría 4 |
| `Oriental Longhair` | FIFe/ACFEC — EMS OLH — categoría 4 |
| `Oriental Shorthair` | FIFe/ACFEC — EMS OSH — categoría 4 |
| `Peterbald` | FIFe/ACFEC — EMS PEB — categoría 4 |
| `Azul Ruso` | FIFe/ACFEC — EMS RUS — categoría 4 |
| `Siamés` | FIFe/ACFEC — EMS SIA — categoría 4 |
| `Somalí` | FIFe/ACFEC — EMS SOM — categoría 4 |
| `Sphynx` | FIFe/ACFEC — EMS SPH — categoría 4 |
| `Thai` | FIFe/ACFEC — EMS THA — categoría 4 |
| `Bombay` | FIFe/ACFEC — EMS BOM (reconocimiento preliminar) |
| `Lykoi` | FIFe/ACFEC — EMS LYO (reconocimiento preliminar) |
| `American Bobtail` | CFA — recognized breeds |
| `American Shorthair` | CFA — recognized breeds |
| `American Wirehair` | CFA — recognized breeds |

**Razas que NO entran y por qué:** `Scottish Fold`, `Munchkin`, `Savannah`, `Ragamuffin` y
`Highland Fold` se ven en Colombia, pero **no pude verificar** su lista de reconocimiento en esta
sesión: `tica.org` devolvió 403 a la herramienta, `wcf.de/breeds` devolvió 404 y `cfa.org/breeds`
solo entregó las doce primeras razas por orden alfabético. **No se inventan.** Van a §9-R7 con issue.

### 5.3 Resto de especies

**`Equino`** — 8 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Criollo Colombiano` | Fedequinas — libro genealógico oficial del Caballo Criollo Colombiano | ✅ consultada 2026-08-25 |
| `Pura Sangre Inglés` | Asociación Colombiana de Criadores de Caballos P.S.I. (afiliada a UNAGA) | ✅ consultada 2026-08-25 |
| `Cuarto de Milla` | American Quarter Horse Association (AQHA) | ⚠️ **sin verificar** |
| `Árabe` | World Arabian Horse Organization (WAHO) | ⚠️ **sin verificar** |
| `Appaloosa` | Appaloosa Horse Club (ApHC) | ⚠️ **sin verificar** |
| `Percherón` | Société Hippique Percheronne de France | ⚠️ **sin verificar** |
| `Poni Shetland` | Shetland Pony Stud-Book Society | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Asnal`** — 2 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Criollo` | Fedequinas registra asnos junto al Caballo Criollo Colombiano | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Mular`** — 2 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Mula` | Híbrido yegua x asno. Fedequinas regula exposiciones de mulas | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Bovino`** — 31 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Holstein` | UNAGA — asociación fundadora (1954) | ✅ consultada 2026-08-25 |
| `Normando` | UNAGA — asociación fundadora (1954) | ✅ consultada 2026-08-25 |
| `Santa Gertrudis` | UNAGA — asociación fundadora (1954) | ✅ consultada 2026-08-25 |
| `Blanco Orejinegro` | UNAGA fundadora + AGROSAVIA (raza criolla colombiana) | ✅ consultada 2026-08-25 |
| `Ayrshire` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Montbeliarde` | UNAGA — ASOMONTBELIARDE | ✅ consultada 2026-08-25 |
| `Bonsmara` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Beefmaster` | UNAGA — ASOBEEFMASTER | ✅ consultada 2026-08-25 |
| `Hereford` | UNAGA — ASOHEREFORD | ✅ consultada 2026-08-25 |
| `Braford` | UNAGA — ASOHEREFORD | ✅ consultada 2026-08-25 |
| `Simmental` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Simbrah` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Simcebú` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Senepol` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Wagyu` | UNAGA — asociación afiliada | ✅ consultada 2026-08-25 |
| `Brahman` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Gyr` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Guzerá` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Nelore` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Indubrasil` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Sardo Negro` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Sindi` | Asocebú — registro genealógico nacional de razas cebuínas | ✅ consultada 2026-08-25 |
| `Romosinuano` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Costeño con Cuernos` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Chino Santandereano` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Hartón del Valle` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Casanareño` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Sanmartinero` | AGROSAVIA — raza criolla colombiana | ✅ consultada 2026-08-25 |
| `Lucerna` | AGROSAVIA — raza sintética colombiana | ✅ consultada 2026-08-25 |
| `Velásquez` | AGROSAVIA — raza sintética colombiana | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Bufalino`** — 5 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Murrah` | Asociación Colombiana de Criadores de Búfalos (afiliada a UNAGA) | ⚠️ **sin verificar** |
| `Mediterráneo` | Asociación Colombiana de Criadores de Búfalos (afiliada a UNAGA) | ⚠️ **sin verificar** |
| `Jafarabadi` | Asociación Colombiana de Criadores de Búfalos (afiliada a UNAGA) | ⚠️ **sin verificar** |
| `Carabao` | Asociación Colombiana de Criadores de Búfalos (afiliada a UNAGA) | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Porcino`** — 10 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Yorkshire` | Porkcolombia / El Sitio Porcino | ✅ consultada 2026-08-25 |
| `Large White` | Porkcolombia — nombrada junto a Pietrain, Duroc, Landrace y Hampshire | ✅ consultada 2026-08-25 |
| `Landrace` | Porkcolombia / El Sitio Porcino | ✅ consultada 2026-08-25 |
| `Duroc` | Porkcolombia / El Sitio Porcino | ✅ consultada 2026-08-25 |
| `Pietrain` | Porkcolombia / El Sitio Porcino | ✅ consultada 2026-08-25 |
| `Hampshire` | Porkcolombia / El Sitio Porcino | ✅ consultada 2026-08-25 |
| `Zungo` | Raza criolla colombiana (Caribe) — LRRD / CONtexto Ganadero | ✅ consultada 2026-08-25 |
| `San Pedreño` | Raza criolla colombiana (Antioquia) — SciELO / CONtexto Ganadero | ✅ consultada 2026-08-25 |
| `Casco de Mula` | Raza criolla colombiana (Orinoquía) — CONtexto Ganadero | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Ovino`** — 14 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Criolla` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Camuro` | AGROSAVIA — oveja criolla de pelo | ✅ consultada 2026-08-25 |
| `Mora Colombiana` | AGROSAVIA — raza ovina mora colombiana | ✅ consultada 2026-08-25 |
| `Hampshire` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Romney Marsh` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Corriedale` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Cheviot` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Black Face` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Merino Rambouillet` | AGROSAVIA — principales razas ovinas existentes en Colombia | ✅ consultada 2026-08-25 |
| `Katahdin` | Muy difundida en Colombia; registro: Katahdin Hair Sheep International | ⚠️ **sin verificar** |
| `Dorper` | Muy difundida en Colombia; registro: Dorper Sheep Breeders' Society | ⚠️ **sin verificar** |
| `Santa Inés` | Muy difundida en Colombia; origen Brasil | ⚠️ **sin verificar** |
| `Pelibuey` | Muy difundida en Colombia y el Caribe | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Caprino`** — 9 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Guajira` | AGROSAVIA — fenotipo criollo colombiano | ✅ consultada 2026-08-25 |
| `Sabanera` | AGROSAVIA — fenotipo criollo colombiano | ✅ consultada 2026-08-25 |
| `Santandereana` | AGROSAVIA — caracterización racial para declararla raza pura | ✅ consultada 2026-08-25 |
| `Saanen` | Raza lechera internacional presente en Colombia | ⚠️ **sin verificar** |
| `Toggenburg` | Raza lechera internacional presente en Colombia | ⚠️ **sin verificar** |
| `Alpina` | Raza lechera internacional presente en Colombia | ⚠️ **sin verificar** |
| `Nubiana` | Raza internacional presente en Colombia (Anglo-Nubian) | ⚠️ **sin verificar** |
| `Boer` | Raza cárnica internacional presente en Colombia | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Conejo`** — 54 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `American` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `American Chinchilla` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `American Fuzzy Lop` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `American Sable` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Argente Brun` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Belgian Hare` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Beveren` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Blanc de Hotot` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Blue Holicer` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Britannia Petite` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Californian` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Champagne d'Argent` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Checkered Giant` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Cinnamon` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Creme d'Argent` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Czech Frosty` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Dutch` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Dwarf Hotot` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Dwarf Papillon` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `English Angora` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `English Lop` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `English Spot` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Flemish Giant` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Florida White` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `French Angora` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `French Lop` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Giant Angora` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Giant Chinchilla` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Harlequin` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Havana` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Himalayan` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Holland Lop` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Jersey Wooly` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Lilac` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Lionhead` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Mini Californian` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Mini Lop` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Mini Rex` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Mini Satin` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Netherland Dwarf` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `New Zealand` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Palomino` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Polish` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Rex` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Rhinelander` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Satin` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Satin Angora` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Silver` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Silver Fox` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Silver Marten` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Standard Chinchilla` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Tan` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Thrianta` | ARBA — recognized breeds | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Cobayo`** — 8 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `American` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Abyssinian` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Peruvian` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `White Crested` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Teddy` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Coronet` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Silkie` | ACBA / ARBA — una de las 13 razas de cavia reconocidas | ⚠️ parcial |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Hámster`** — 5 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Sirio` | Especie Mesocricetus auratus | ⚠️ **sin verificar** |
| `Ruso` | Especie Phodopus sungorus / campbelli | ⚠️ **sin verificar** |
| `Roborovski` | Especie Phodopus roborovskii | ⚠️ **sin verificar** |
| `Chino` | Especie Cricetulus griseus | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Chinchilla`** — 3 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Chinchilla Lanigera` | Especie Chinchilla lanigera | ⚠️ **sin verificar** |
| `Chinchilla Brevicaudata` | Especie Chinchilla chinchilla | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Jerbo`** — 2 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Jerbo de Mongolia` | Especie Meriones unguiculatus | ⚠️ **sin verificar** |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Hurón`** — 2 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Hurón Doméstico` | Mustela putorius furo — Resolución ICA 842 de 2010 | ✅ consultada 2026-08-25 |
| `Mestizo` | Raza de respaldo (§3.3) | — decisión de diseño |

**`Ave`** — 17 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Canario` | Resolución ICA 1862 de 2008 — ave ornamental autorizada | ✅ consultada 2026-08-25 |
| `Periquito Australiano` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Cacatúa Ninfa` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Diamante Mandarín` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Agapornis` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Loro Cabeza Azul` | Resolución ICA 1862 de 2008 (solo criadero legal) | ✅ consultada 2026-08-25 |
| `Cacatúa` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Jilguero` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Papagayo de Collar` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Paloma Doméstica` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Tórtola Diamante` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Pinzón Cebra` | Resolución ICA 1862 de 2008 | ✅ consultada 2026-08-25 |
| `Gallina` | Ave de corral — Censo Pecuario Nacional ICA | ⚠️ **sin verificar** |
| `Pato` | Ave de corral — Censo Pecuario Nacional ICA | ⚠️ **sin verificar** |
| `Pavo` | Ave de corral — Censo Pecuario Nacional ICA | ⚠️ **sin verificar** |
| `Codorniz` | Ave de corral — Censo Pecuario Nacional ICA | ⚠️ **sin verificar** |
| `Otra Ave` | Entrada de respaldo (§3.3) | — decisión de diseño |

**`Reptil`** — 1 filas

| `name` | Organismo / fuente | Verificación |
|---|---|---|
| `Otro Reptil` | Entrada de respaldo (§3.3). Ver §9-R6: la tenencia de reptiles está prohibida o muy restringida en Colombia | — decisión de diseño |


> Las filas marcadas **⚠️ sin verificar** son razas que existen y son de uso corriente, pero cuya
> fuente no llegué a abrir en esta sesión. `db-migrations` puede sembrarlas —están nombradas
> correctamente— o dejarlas para una segunda tanda; lo que **no** puede es presentarlas como
> verificadas. Están agrupadas en §9-R7.

---

## 6. Catálogo `animal_colors` — 411 filas

La nomenclatura de capa **no es traducible entre especies** y por eso este catálogo cuelga de
`species` desde el changeset `218`. Un caballo no es «café», es **castaño**; un gato no es «atigrado»
a secas, es **atigrado clásico**, **rayado**, **moteado** o **ticked**; y `Merle` solo existe en el
perro. El propio `218` lo dice en su cabecera: *«Merle sólo aplica a caninos y Carey sólo a
felinos»*.

Recuento por especie:

| Especie | Filas | Nomenclatura |
|---|---|---|
| `Canino` | 40 | Vocabulario del changeset `218` con acentos corregidos; términos validables contra la nomenclatura FCI en español |
| `Felino` | 51 | Vocabulario del `218` + **sistema EMS de FIFe en español publicado por ACFEC** |
| `Equino` | 38 | **Capas** en nomenclatura española (Caballipedia); Fedequinas regula aparte las manchas blancas |
| `Asnal` | 20 | Descriptiva genérica (§6.4) |
| `Mular` | 20 | Descriptiva genérica (§6.4) |
| `Bovino` | 20 | Descriptiva genérica (§6.4) |
| `Bufalino` | 20 | Descriptiva genérica (§6.4) |
| `Porcino` | 20 | Descriptiva genérica (§6.4) |
| `Ovino` | 20 | Descriptiva genérica (§6.4) |
| `Caprino` | 20 | Descriptiva genérica (§6.4) |
| `Conejo` | 20 | Descriptiva genérica (§6.4) |
| `Cobayo` | 20 | Descriptiva genérica (§6.4) |
| `Hámster` | 20 | Descriptiva genérica (§6.4) |
| `Chinchilla` | 20 | Descriptiva genérica (§6.4) |
| `Jerbo` | 20 | Descriptiva genérica (§6.4) |
| `Hurón` | 20 | Descriptiva genérica (§6.4) |
| `Ave` | 11 | Descriptiva, reducida |
| `Reptil` | 11 | Descriptiva, reducida |

### 6.1 `Canino` — 40 filas

Son **exactamente** los colores que el changeset `218` ya declara para `Canino` (29 compartidos + 11
propios), con **los acentos puestos**. No se añade ni se quita ninguno: el vocabulario de la casa ya
existe y reinventarlo produciría dos catálogos que dicen lo mismo con distintas palabras.

Varios de estos términos están validados por la propia nomenclatura FCI en español, que los usa dentro
de nombres de raza: `Negro y Fuego` (FCI 13, 63, 300), `Blanco y Naranja` (316, 324),
`Blanco y Negro` (220, 323), `Tricolor` (219, 229, 322), `Leonado` (36, 66), `Azul` (22, 32, 35),
`Gris` (242).

| Colores compartidos con `Felino` (29) |
|---|
| `Negro` · `Café` · `Blanco` · `Gris` · `Crema` · `Beige` · `Dorado` · `Naranja` · `Rojo` · `Chocolate` · `Canela` · `Azul` · `Lila` · `Plateado` · `Marfil` · `Arena` · `Albino` · `Bicolor` · `Tricolor` · `Atigrado` · `Manchado` · `Moteado` · `Agutí` · `Blanco y Negro` · `Blanco y Café` · `Blanco y Gris` · `Blanco y Naranja` · `Multicolor` · `Otro` |

| Solo `Canino` (11) |
|---|
| `Leonado` · `Trigueño` · `Hígado` · `Ruano` · `Arlequín` · `Merle` · `Merle Azul` · `Merle Rojo` · `Sable` · `Negro y Fuego` · `Manto Negro` |

### 6.2 `Felino` — 51 filas

Los 29 compartidos + los 8 que `218` ya declara para felino + **14 tomados del sistema EMS de FIFe en
la traducción al español que publica ACFEC**. El EMS es la nomenclatura felina real: `f` es *tortie
negro*, `g` *tortie azul*, `p` *cervato*, `am` *caramelo*, `em` *albaricoque*, `01` *van*,
`04` *mitted*, `22` *tabby clásico*, `23` *tabby rayas*, `24` *tabby puntos*, `25` *tabby ticking*.

| Bloque | Colores |
|---|---|
| Compartidos con `Canino` (29) | `Negro` · `Café` · `Blanco` · `Gris` · `Crema` · `Beige` · `Dorado` · `Naranja` · `Rojo` · `Chocolate` · `Canela` · `Azul` · `Lila` · `Plateado` · `Marfil` · `Arena` · `Albino` · `Bicolor` · `Tricolor` · `Atigrado` · `Manchado` · `Moteado` · `Agutí` · `Blanco y Negro` · `Blanco y Café` · `Blanco y Gris` · `Blanco y Naranja` · `Multicolor` · `Otro` |
| Ya declarados por `218` para felino (8) | `Carey` · `Calicó` · `Carey Diluido` · `Colorpoint` · `Esmoquin` · `Humo` · `Sombreado` · `Chinchilla` |
| Nuevos, del EMS de FIFe/ACFEC (14) | `Cervato` · `Caramelo` · `Albaricoque` · `Carey Azul` · `Carey Chocolate` · `Carey Lila` · `Carey Canela` · `Carey Cervato` · `Van` · `Mitted` · `Atigrado Clásico` · `Atigrado Rayado` · `Atigrado Moteado` · `Atigrado Ticked` |

> `Calicó` lleva tilde; `218` lo sembró como `Calico`. Bajo `utf8mb4_0900_ai_ci` son la misma clave:
> ver §7.4, es exactamente el caso que hay que normalizar.

### 6.3 `Equino` — 38 filas

Capas en nomenclatura española. Fedequinas, que lleva el libro genealógico del Caballo Criollo
Colombiano, **no publica un catálogo cerrado de capas**: lo que reglamenta son las **manchas blancas**
—«estrellas, luceros, listones y calzados bajos o moderados»—, que son *particularidades*, no capas, y
que este modelo no tiene dónde guardar (`animals` tiene un único `color_id`). Se documenta como
limitación, no se fuerza dentro del catálogo de color.

| Bloque | Capas |
|---|---|
| Básicas y tostadas | `Alazán` · `Alazán Tostado` · `Castaño` · `Castaño Oscuro` · `Castaño Bocifuego` · `Negro` |
| Diluciones | `Negro Ahumado` · `Negro Plata` · `Castaño Plata` · `Bayo` · `Bayo Tostado` · `Palomino` · `Palomino Tostado` · `Isabelo` · `Cervuno` · `Grullo` · `Ratonero` · `Perlino` · `Cremello` · `Crema Ahumado` |
| Tordas y ruanas | `Tordo` · `Tordo Rodado` · `Tordo Mosqueado` · `Tordo Atruchado` · `Tordo Vinoso` · `Tordo Apizarrado` · `Tordo Claro` · `Ruano Negro` · `Ruano Castaño` · `Ruano Alazán` · `Rabicano` |
| Patrones pintos y moteados | `Appaloosa` · `Tobiano` · `Overo` · `Sabino` · `Pío` · `Blanco` · `Otro` |

### 6.4 Las demás especies — vocabulario descriptivo, y se dice que lo es

Para bovino, bufalino, porcino, ovino, caprino, asnal, mular, conejo, cobayo, hámster, chinchilla,
jerbo y hurón **no existe una nomenclatura de capa publicada por un organismo colombiano** comparable
al EMS felino o a las capas equinas. Podría inventarme una con términos ganaderos —`barcino`,
`hosco`, `careto`, `berrendo`— pero no los verifiqué contra ninguna fuente en esta sesión y el encargo
prohíbe exactamente eso. Así que estas 13 especies reciben un **vocabulario descriptivo genérico de 20
términos, declarado como tal**, y la nomenclatura zootécnica queda en §9-R8.

| Genérico (20), para las 13 especies |
|---|
| `Negro` · `Blanco` · `Gris` · `Café` · `Rojo` · `Crema` · `Amarillo` · `Dorado` · `Beige` · `Bayo` · `Atigrado` · `Manchado` · `Moteado` · `Bicolor` · `Tricolor` · `Blanco y Negro` · `Blanco y Café` · `Albino` · `Multicolor` · `Otro` |

| `Ave` y `Reptil` (11) |
|---|
| `Verde` · `Azul` · `Amarillo` · `Rojo` · `Naranja` · `Blanco` · `Negro` · `Gris` · `Café` · `Multicolor` · `Otro` |

---

## 7. Especificación para `db-migrations`

### 7.1 Tres changesets, en este orden

| Orden | Fichero propuesto | Qué hace | Filas |
|---|---|---|---|
| 1 | `28X_seed_species_catalog.xml` | `INSERT` en `species` | 18 |
| 2 | `28X+1_seed_breeds_catalog.xml` | `INSERT` en `breeds`, resolviendo `specie_id` por nombre | 577 |
| 3 | `28X+2_seed_animal_colors_catalog.xml` | `INSERT` en `animal_colors` + normalización de acentos de `218` | 411 |

La numeración correlativa y el registro en `db.changelog-master.xml` son de `db-migrations`; aquí solo
se fija el **orden relativo**, que es obligatorio: 2 y 3 resuelven `specie_id` leyendo `species`.

No los metas en un solo fichero. Son tres unidades con distinto riesgo de rollback: tirar las especies
obliga a tirar antes razas y colores por la FK.

### 7.2 Forma del `INSERT` — se reutiliza el patrón de la casa

`218_seed_animal_colors_catalog` ya fijó el patrón y funciona: `INSERT IGNORE` + `SELECT … CROSS JOIN`
resolviendo `specie_id` por `species.name`. **Se reutiliza, no se inventa otro.**

```sql
-- especies
INSERT IGNORE INTO species (name, created_date, enabled, version)
SELECT n.name, CURRENT_TIMESTAMP, 1, 0
  FROM (          SELECT 'Canino' AS name
        UNION ALL SELECT 'Felino'
        UNION ALL SELECT 'Equino'
        /* … */ ) n;

-- razas: specie_id se resuelve por nombre, nunca por id literal
INSERT IGNORE INTO breeds (name, specie_id, created_date, enabled, version)
SELECT n.name, s.id, CURRENT_TIMESTAMP, 1, 0
  FROM species s
  JOIN (          SELECT 'Mestizo' AS name
        UNION ALL SELECT 'Pastor Alemán'
        /* … */ ) n
 WHERE s.name = 'Canino';
```

Puntos no negociables:

1. **`specie_id` se resuelve por `species.name`, jamás por un id literal.** Los `AUTO_INCREMENT` no
   coinciden entre local, dev y producción, y el `218` ya demostró lo que pasa cuando el `SELECT` no
   encuentra la especie: cero filas y ningún error.
2. **`enabled = 1` y `version = 0` explícitos**, aunque las columnas tengan `DEFAULT`
   (`068_…:23-27, 55-59, 239-243` y `225_…:48-77`). Explícito es lo que hace `218` y lo que hace
   legible el changeset.
3. **`created_date = CURRENT_TIMESTAMP`.** La columna es `DATETIME NOT NULL`.
4. **`INSERT IGNORE`** para que el changeset sea reejecutable sobre entornos parcialmente sembrados.
   Ojo con su lado oscuro: `INSERT IGNORE` degrada a *warning* también los errores de truncamiento y
   de FK. Por eso §8 exige un recuento posterior: si el `INSERT` se comió filas, el conteo lo delata.
5. **Un `<changeSet>` por especie en el fichero de razas.** 577 filas en una sola sentencia son
   irrevisables en un PR y, si falla, falla entero. Por especie, el `checksum` de Liquibase también es
   más estable ante una corrección puntual.
6. **Literales con `<![CDATA[ … ]]>`** cuando aparezcan `<` o `&`, como ya hace `263`.

### 7.3 `preConditions`

Los changesets 2 y 3 **no pueden ejecutarse si las especies no están**, y su modo de fallar es
silencioso (`INSERT … SELECT` de cero filas). Se blinda con el patrón de la casa —`sqlCheck` con
`onFail="HALT"`, igual que `206`, `210` y `226`—:

```xml
<preConditions onFail="HALT">
    <sqlCheck expectedResult="18">
        SELECT COUNT(*) FROM species WHERE enabled = 1
    </sqlCheck>
</preConditions>
```

`HALT` y no `MARK_RAN`: si las especies no están, sembrar razas no es «innecesario», es **imposible**,
y dejarlo pasar reproduce el fallo de `218`.

### 7.4 ⚠️ La trampa: normalizar lo que `218` dejó sin acentos

`218_seed_animal_colors_catalog` sembró `Cafe`, `Higado`, `Aguti`, `Calico` y `Arlequin` **sin
tilde** (y `Trigueño` con eñe, o sea que ni siquiera fue coherente). En los entornos donde ese
changeset corrió con especies presentes, esas filas están ahí.

Bajo `utf8mb4_0900_ai_ci`, `'Café'` y `'Cafe'` son **la misma clave** de
`uq_animal_colors_specie_name`. Por lo tanto, si el nuevo changeset se limita a un `INSERT IGNORE` de
`'Café'`:

- **no** inserta nada,
- **no** da error,
- y el catálogo se queda con `Cafe` para siempre, mientras el documento dice `Café`.

Se resuelve con un `UPDATE` de normalización **antes** del `INSERT`, y con `version = version + 1`
porque estas tablas llevan `@Version` y hay 84 entidades con bloqueo optimista en el proyecto:

```sql
UPDATE animal_colors c
  JOIN species s ON s.id = c.specie_id
   SET c.name = 'Café', c.version = c.version + 1
 WHERE c.name = 'Cafe' AND s.name IN ('Canino','Felino');
-- ídem: 'Higado'→'Hígado', 'Aguti'→'Agutí', 'Calico'→'Calicó', 'Arlequin'→'Arlequín'
```

`WHERE c.name = 'Cafe'` bajo `ai_ci` casa también con `'Café'`, así que el `UPDATE` es idempotente:
la segunda pasada escribe el mismo valor. No hace falta `BINARY` ni `COLLATE utf8mb4_bin`, aunque
usarlo (`WHERE c.name COLLATE utf8mb4_bin = 'Cafe'`) hace el `UPDATE` más preciso y evita tocar filas
ya correctas; **es la opción recomendada**, porque no mover `version` sin necesidad es más limpio.

Este `UPDATE` va **antes** del `INSERT IGNORE` del mismo changeset.

### 7.5 `<rollback>`

Los tres changesets llevan rollback explícito, y **acotado por `NOT EXISTS` contra `animals`**: si
alguien ya registró un paciente, la fila no se puede borrar sin romper la FK.

```xml
<rollback>
    <sql>
        DELETE b FROM breeds b
          JOIN species s ON s.id = b.specie_id
         WHERE s.name = 'Canino'
           AND b.name IN ('Mestizo', 'Pastor Alemán' /* … */)
           AND NOT EXISTS (SELECT 1 FROM animals a WHERE a.breed_id = b.id);
    </sql>
</rollback>
```

`<rollback/>` vacío —lo que hace `218`— **no vale aquí**: `218` podía permitírselo porque su `INSERT`
era condicional; el de esta semilla no lo es. Un rollback vacío convierte «deshacer el despliegue» en
«borrar filas a mano en producción».

El rollback de `species` va **el último** y también con `NOT EXISTS` contra `breeds`,
`animal_colors` y `animals`.

### 7.6 Coste del cambio y `ddl-auto: validate`

- **No hay DDL.** Son `INSERT` y un `UPDATE`: cero `ALTER`, cero reconstrucción de tabla, cero riesgo
  de bloqueo. La discusión de *instant / in place / rebuild* del manual de InnoDB **no aplica**.
- **No hay divergencia de esquema**, así que `ddl-auto: validate` no puede tumbar el arranque por
  esta semilla. Las tres entidades JPA ya existen y no cambian.
- **No hay aislamiento por tenant que revisar**: las tres tablas son globales, sin `company_id`. No
  hay fuga posible entre clínicas por sembrar un catálogo compartido —lo que sí hay es el problema
  inverso, §9-R2—.
- Tiempo de ejecución estimado: 1006 `INSERT` en una `db.t4g.small`, del
  orden de segundos. **No medido.**

---

## 8. Cómo se verifica después

### 8.1 Lo que ya está verificado en esta especificación — `db-migrations` no tiene que repetirlo

**La comprobación de colisiones de índice único ya está hecha sobre la lista completa, y este es el
criterio exacto con el que se hizo**, para que se pueda auditar o reproducir:

1. Cada `name` se **normalizó** con el mismo criterio que aplica `utf8mb4_0900_ai_ci`: paso a
   minúsculas + descomposición Unicode NFD + eliminación de todas las marcas diacríticas (categoría
   `Mn`). Eso pliega `é→e`, `í→i`, `ñ→n` y `Ü→u`, que es lo que hace `ai_ci`.
2. Se buscaron duplicados **en el ámbito real de cada índice**, no sobre la lista global:
   - `breeds`: duplicados **dentro de cada `specie_id`** (`uq_breeds_specie_name`).
   - `animal_colors`: duplicados **dentro de cada `specie_id`** (`uq_animal_colors_specie_name`).
   - `species`: duplicados **sobre toda la tabla**, porque ahí el `UNIQUE` sí es global sobre `name`
     (`028_create_species.xml:12-14`).

| Comprobación | Resultado |
|---|---|
| Colisiones `ai_ci` en `breeds`, por especie | ✅ **0** sobre 577 filas y 18 especies |
| Colisiones `ai_ci` en `animal_colors`, por especie | ✅ **0** sobre 411 filas |
| Colisiones `ai_ci` en `species`, globales | ✅ **0** sobre 18 filas |
| Ningún `name` supera 100 caracteres | ✅ máximo real: **68** (`Perro Cobrador de Nueva Escocia (Nova Scotia Duck Tolling Retriever)`) |
| Toda especie tiene ≥1 raza (`breed_id` es `NOT NULL`) | ✅ mínimo: 1 (`Reptil`) |
| Toda especie tiene ≥1 color (`color_id` es `NOT NULL`) | ✅ mínimo: 11 |
| Toda raza y todo color apuntan a una especie que existe en la lista | ✅ |

Casos que **sí** se revisaron uno a uno por ser los candidatos naturales a colisión, y que quedaron
limpios: `Pastor Alemán` (no hay ningún `Pastor Aleman`), `Bulldog` / `Bulldog Francés`,
`Sabueso Suizo` / `Sabueso Suizo Pequeño`, `Collie de Pelo Corto` / `Collie de Pelo Largo`,
`Atigrado` / `Atigrado Clásico`, `Carey` / `Carey Diluido` / `Carey Azul`, y las cuatro variedades del
Pastor Belga, que se distinguen por el paréntesis. Transliteraciones con y sin guion
(`Shih Tzu` vs `Shih-Tzu`, `Chow Chow` vs `Chow-Chow`) **no colisionan** bajo `ai_ci` —el guion sí es
un carácter distinto—, pero por eso mismo entrarían como dos filas distintas: en la lista aparece
**una sola forma de cada una**.

La única colisión real detectada en todo el trabajo no está dentro de esta lista, sino **entre esta
lista y lo que ya sembró el changeset `218`** (`Café` vs `Cafe`, `Calicó` vs `Calico`, …). Es §7.4 y
es la razón por la que ese `UPDATE` de normalización no es opcional.

### 8.2 Lo que hay que ejecutar tras sembrar

```sql
-- 1. Recuento: si INSERT IGNORE se comió filas, aquí se ve
SELECT (SELECT COUNT(*) FROM species       WHERE enabled = 1) AS especies,   -- esperado 18
       (SELECT COUNT(*) FROM breeds        WHERE enabled = 1) AS razas,      -- esperado 577
       (SELECT COUNT(*) FROM animal_colors WHERE enabled = 1) AS colores;    -- esperado >= 411

-- 2. Ninguna especie inutilizable (breed_id y color_id son NOT NULL)
SELECT s.name,
       (SELECT COUNT(*) FROM breeds        b WHERE b.specie_id = s.id AND b.enabled = 1) AS razas,
       (SELECT COUNT(*) FROM animal_colors c WHERE c.specie_id = s.id AND c.enabled = 1) AS colores
  FROM species s
 WHERE s.enabled = 1
HAVING razas = 0 OR colores = 0;       -- debe devolver 0 filas

-- 3. La normalización de acentos de §7.4 se aplicó de verdad
SELECT name FROM animal_colors
 WHERE name COLLATE utf8mb4_bin IN ('Cafe','Higado','Aguti','Calico','Arlequin');  -- 0 filas

-- 4. Toda especie tiene su fila de respaldo
SELECT s.name FROM species s
 WHERE s.enabled = 1
   AND NOT EXISTS (SELECT 1 FROM breeds b
                    WHERE b.specie_id = s.id AND b.enabled = 1
                      AND b.name IN ('Mestizo','Otra Ave','Otro Reptil'));        -- 0 filas

-- 5. Colisiones ai_ci que se hubieran colado pese al INSERT IGNORE: debe devolver 0 filas
SELECT specie_id, LOWER(CONVERT(name USING utf8mb4)) COLLATE utf8mb4_0900_ai_ci AS clave,
       COUNT(*) AS repes, GROUP_CONCAT(name)
  FROM breeds
 GROUP BY specie_id, clave HAVING repes > 1;

-- 6. Charset y collation por tabla (redundante: ya verificado, se deja como red de seguridad)
SELECT @@character_set_database, @@collation_database;
SELECT table_name, table_collation
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN ('species','breeds','animal_colors');
```

La consulta 6 es redundante: **la collation ya está verificada** en los tres niveles
(`@@collation_server`, default del esquema y columna `name`) y es `utf8mb4_0900_ai_ci`. Se deja porque
un cambio futuro de *parameter group* la volvería a hacer relevante, y porque es gratis.

---

## 9. Riesgos y pendientes

**Todo lo de esta tabla está abierto como issue en `kefaroTech/vetsoftware-backend`.** Si el documento
y GitHub discrepan, manda GitHub.

| Id | Qué queda abierto | Severidad | Issue |
|---|---|---|---|
| **R0** | Los tres catálogos vacíos y las tres FK de `animals` `NOT NULL`: es el motivo de este documento | **bloqueante** | [#569](https://github.com/kefaroTech/vetsoftware-backend/issues/569) |
| **R1** | El changeset `218` siembra cero filas en base nueva y Liquibase no lo reejecuta (§1.3) | **grave** | [#570](https://github.com/kefaroTech/vetsoftware-backend/issues/570) |
| **R2** | `breeds`, `species` y `animal_colors` **no tienen `company_id`** y el API expone `POST /breeds`, `POST /species` y `POST /animal-colors` sin `@PreAuthorize`: la única barrera es `anyRequest().authenticated()` (`SecurityConfig.java:99`). Una clínica que cree una raza la crea **para todo el SaaS**. Las lecturas, además, son rutas públicas (`PublicRoutes.java:65-66`), pero eso es deliberado | **grave** | [#571](https://github.com/kefaroTech/vetsoftware-backend/issues/571) |
| **R3** | Los listados **no ordenan**: el orden del desplegable es el de `id`. `uq_breeds_specie_name (specie_id, name)` ya serviría un `ORDER BY name` sin `filesort` | menor | [#572](https://github.com/kefaroTech/vetsoftware-backend/issues/572) |
| **R4** | El buscador de razas es una subcadena literal sobre `name`: quien teclee «criollo» o «gozque» no encontrará `Mestizo`. Necesita sinónimos, no una segunda fila | menor | [#573](https://github.com/kefaroTech/vetsoftware-backend/issues/573) |
| **R5** | Índice de FK redundante: `029` crea `fk_breeds_specie` sobre `(specie_id)` y a continuación `uq_breeds_specie_name` sobre `(specie_id, name)`, del que el primero es prefijo por la izquierda. Mismo patrón en `animal_colors` tras `218`. **Deducido de los changesets, no medido** | nota | [#574](https://github.com/kefaroTech/vetsoftware-backend/issues/574) |
| **R6** | `breeds` significa dos cosas: raza (mamíferos) y especie zoológica (`Ave`, `Reptil`), §3.1. Incluye la decisión de negocio pendiente sobre si `Reptil` se siembra: su tenencia está prohibida o muy restringida en Colombia | nota | [#575](https://github.com/kefaroTech/vetsoftware-backend/issues/575) |
| **R7** | 36 razas nombradas pero **con la fuente sin abrir**: equinas, bufalinas, ovinas de pelo, caprinas lecheras, hámsteres, jerbo, chinchilla, 6 de las 13 cavias, y las felinas de TICA/WCF (403/404). Más `Pez` y `Erizo` como especies candidatas | menor | [#576](https://github.com/kefaroTech/vetsoftware-backend/issues/576) |
| **R8** | No hay nomenclatura de capa verificada para las 13 especies del §6.4; van con vocabulario descriptivo genérico, declarado como tal | nota | incluido en [#576](https://github.com/kefaroTech/vetsoftware-backend/issues/576) |

---

## 10. Fuentes

Todas consultadas el **2026-08-25** salvo indicación. Las marcadas ⚠️ no respondieron y su contenido
**no** se usó.

| # | Fuente | URL | Qué sostiene |
|---|---|---|---|
| 1 | FCI — Nomenclatura de las razas (español) | https://www.fci.be/es/nomenclature/ | Los 10 grupos y el índice de la nomenclatura |
| 2 | FCI — Grupo 1 | https://www.fci.be/es/nomenclature/1-Perros-de-pastor-y-perros-boyeros-excepto-perros-boyeros-suizos.html | 43 razas con número FCI |
| 3 | FCI — Grupo 2 | https://www.fci.be/es/nomenclature/2-Perros-tipo-pinscher-y-schnauzer-Molosoides-Perros-tipo-montana-y-boyeros-suizos.html | 53 razas |
| 4 | FCI — Grupo 3 | https://www.fci.be/es/nomenclature/3-Terriers.html | 34 razas |
| 5 | FCI — Grupo 4 | https://www.fci.be/es/nomenclature/4-Teckels.html | Teckel (Dachshund), FCI 148, y sus 9 variedades |
| 6 | FCI — Grupo 5 | https://www.fci.be/es/nomenclature/5-Perros-tipo-spitz-y-tipo-primitivo.html | 46 razas |
| 7 | FCI — Grupo 6 | https://www.fci.be/es/nomenclature/6-Perros-Tipo-sabueso-perros-de-rastro-y-razas-semejantes.html | 70 razas |
| 8 | FCI — Grupo 7 | https://www.fci.be/es/nomenclature/7-Perros-de-muestra.html | 36 razas |
| 9 | FCI — Grupo 8 | https://www.fci.be/es/nomenclature/8-Perros-cobradores-de-caza-Perros-levantadores-de-caza-Perros-de-agua.html | 22 razas |
| 10 | FCI — Grupo 9 | https://www.fci.be/es/nomenclature/9-Perros-de-compania.html | 26 razas |
| 11 | FCI — Grupo 10 | https://www.fci.be/es/nomenclature/10-Lebreles.html | 13 razas |
| 12 | FCI — Estadísticas de Colombia | https://www.fci.be/es/statistics/ByNco.aspx?iso=CO | La organización canina nacional es la Asociación Club Canino Colombiano; **no** publica inscripciones por raza (base de §3.2) |
| 13 | El Colombiano — Sabueso Fino Colombiano | https://www.elcolombiano.com/tendencias/el-sabueso-fino-colombiano-ya-es-raza-oficial-la-historia-del-perro-que-nacio-en-el-campo-CB33765997 | Reconocimiento **provisional** de la FCI, feb. 2026, a propuesta de la ACCC |
| 14 | Ley 1801 de 2016, art. 126 (Código Nacional de Seguridad y Convivencia) | https://leyes.co/codigo_nacional_de_policia/126.htm | Lista legal de ejemplares caninos potencialmente peligrosos |
| 15 | ACFEC (club colombiano miembro de FIFe) — Sistema EMS en español | https://www.acfec.co/sistemaems | Nombres de raza felina y **códigos de color EMS en español** |
| 16 | FIFe — Breeds | https://fifeweb.org/cats/breeds/ | Las 47 razas reconocidas + 2 preliminares, con código EMS y categoría |
| 17 | CFA — Breeds | https://cfa.org/breeds/ | American Bobtail, American Shorthair, American Wirehair (lista **parcial**: solo devolvió las 12 primeras) |
| 18 | Fedequinas — Estatutos | https://fedequinas.org/wp-content/uploads/2022/09/ESTATUTO-FEDEQUINAS-REFORMA-27-JULIO-DE-2022.pdf | Ente rector del Caballo Criollo Colombiano y de sus libros genealógicos |
| 19 | Agronegocios — Fedequinas y las manchas | https://www.agronegocios.co/finca/fedequinas-aclaro-los-criterios-de-regulacion-de-manchas-del-caballo-criollo-colombiano-4307146 | Fedequinas reglamenta **manchas** (estrella, lucero, listón, calzado), no un catálogo de capas |
| 20 | Caballipedia — Capas del caballo | https://caballipedia.es/Capas_del_caballo | Nomenclatura española de capas: básicas, diluciones, tordas, ruanas, pintos |
| 21 | UNAGA — Asociaciones afiliadas | https://unaga.org.co/category/asociaciones/ | 27 asociaciones de razas puras con libro genealógico en Colombia |
| 22 | Asocebú Colombia | https://asocebu.com/nosotros/ | Registro genealógico nacional: Brahman, Gyr, Guzerá, Nelore, Indubrasil, Sardo Negro, Sindi |
| 23 | AGROSAVIA — Razas bovinas criollas y colombianas | https://repository.agrosavia.co/items/41a88404-baea-4cfc-8e6d-fb6901165252 | 7 criollas + 2 sintéticas (Lucerna, Velásquez) |
| 24 | AGROSAVIA — Principales razas ovinas existentes en Colombia | https://repository.agrosavia.co/handle/20.500.12324/22454 | Razas ovinas; Camuro y Mora Colombiana |
| 25 | AGROSAVIA — Producción ovino-caprina | https://www.agrosavia.co/media/fcadcodi/accion-corporativa_ovino-caprino-tolima.pdf | Fenotipos criollos caprinos: Guajira, Sabanera, Santandereana |
| 26 | Porkcolombia | https://porkcolombia.co/noticias/pietrain-duroc-y-landrace-algunas-razas-para-entrar-en-el-negocio-de-la-porcicultura/ | Pietrain, Duroc, Landrace, Hampshire, Large White |
| 27 | CONtexto Ganadero — cerdo criollo colombiano | https://www.contextoganadero.com/agricultura/este-es-el-panorama-del-cerdo-criollo-colombiano | Zungo, San Pedreño, Casco de Mula |
| 28 | Resolución ICA 842 de 2010 | https://www.icbf.gov.co/cargues/avance/docs/resolucion_ica_0842_2010.htm | Los **seis** grupos de pequeños mamíferos autorizados como mascota: hurones, conejos, chinchillas, hámsteres, cobayos y jerbos |
| 29 | Infobae — mascotas exóticas legales en Colombia | https://www.infobae.com/colombia/2025/08/14/hurones-peces-guppys-aves-ornamentales-y-los-otros-animales-que-se-pueden-tener-como-compania-y-de-mascotas-exoticas-en-colombia/ | Lista de **aves ornamentales** de la Resolución ICA 1862 de 2008 |
| 30 | ARBA — Recognized breeds | https://arba.net/recognized-breeds/ | 53 razas de conejo |
| 31 | ARBA — Cavies | https://arba.net/13-breeds-of-cavies/ | 13 razas de cavia reconocidas (la página **no** las enumera: de ahí el ⚠️ parcial de §5.3) |
| 32 | Censo Pecuario Nacional ICA | https://www.agronet.gov.co/estadistica/Paginas/home.aspx?cod=124 | Especies pecuarias censadas: bovinos, bufalinos, porcinos, aves, equinos, ovinos, caprinos |
| 33 | MySQL 8.4 — Server Character Set and Collation | https://dev.mysql.com/doc/refman/8.4/en/charset-server.html | «By default, these are `utf8mb4` and `utf8mb4_0900_ai_ci`» |
| 34 | MySQL 8.4 — Multiple-Column Indexes | https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html | Regla del prefijo por la izquierda (§3.6 y §9-R5) |
| 35 | Liquibase — Best practices | https://docs.liquibase.com/concepts/bestpractices.html | Un cambio lógico por `changeSet`; rollback explícito |
| 36 | SQL Antipatterns (Karwin) | https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/ | El nombre del antipatrón de §3.1 |
| 37 ⚠️ | TICA — Browse all breeds | https://tica.org/breeds/browse-all-breeds | **403 Forbidden.** No usado |
| 38 ⚠️ | WCF — Breeds | https://wcf.de/breeds/ | **404.** No usado |
| 39 ⚠️ | FIFe — EMS System (PDF) | https://fifeweb.org/wp-content/uploads/2023/12/ems_system_en.pdf | PDF ilegible por la herramienta. Se usó en su lugar la versión en español de ACFEC (#15) |
| 40 ⚠️ | Revista U. de La Salle — razas ovinas y caprinas en Colombia | https://revistauls.lasalle.edu.co/article/view/2857/2713 | PDF ilegible por la herramienta. No usado |

---

## 11. Qué NO se comprobó

- **Yo no consulté ninguna base de datos viva.** El contenedor local `vetsoftware_mysql` está apagado
  (`docker ps` el 2026-08-25 lista `vetsoftware_backend`, `vetsoftware_public_front`, `otel_collector`,
  `tempo` y `localstack`, pero **no** `vetsoftware_mysql`) y dev no se tocó por decisión del encargo.
  Todo lo del esquema sale de los changesets, de las entidades JPA y del Terraform.
- **Excepción:** la collation **sí** está verificada contra base viva —`utf8mb4_0900_ai_ci` en
  `@@collation_server`, en el default del esquema y en la columna `name`—, verificación aportada por
  el coordinador el 2026-08-25 y coincidente con lo que se deduce del DDL y del *parameter group*
  (§3.5). Es el único dato de este documento que viene de una medición, y no la hice yo.
- **Cuántas filas tienen hoy los catálogos en dev.** Si `218` corrió allí con especies presentes, hay
  colores sin acentos que el §7.4 debe normalizar; si no, están vacíos. **No verificado.**
- **La lista FCI contra el recuento oficial.** La FCI declara ~360 razas a título definitivo; de sus
  páginas por grupo se transcribieron **344 razas** (347 filas contando las 4 variedades del
  Pastor Belga). La diferencia es de razas en reconocimiento provisional y de cómo cuenta la FCI las
  variedades. `db-migrations` debe hacer el diff contra las URLs #2 a #11 antes de escribir el changeset.
- **Rendimiento.** No hay `EXPLAIN` en este documento porque no hay ninguna consulta nueva: la semilla
  no cambia ningún plan.

