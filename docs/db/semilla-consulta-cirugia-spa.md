# Especificación de datos semilla — `consultation_types`, `surgery_types`, `spa_types`

- **Fecha:** 2026-08-25
- **Autor:** agente de modelo de datos (auditoría y especificación; **no** escribe changesets)
- **Destinatario:** `db-migrations` — este documento es el insumo con el que se escriben los
  changesets `285`, `286` y `287`. Nada de lo que hay aquí toca `src/` ni `db/changelog/`.
- **Estado hoy:** los tres catálogos **se despliegan vacíos**. Verificado: los únicos changesets
  que los tocan son DDL (`032`, `043`, `051`, más `057`, `068` y `225`), y ninguno inserta filas.

---

## 0. El esquema real, verificado contra el árbol

Los tres **no** son iguales, y esa asimetría manda sobre todo lo demás.

| Tabla | Columnas | `company_id` | `general` | Evidencia |
|---|---|---|---|---|
| `consultation_types` | `id`, `name VARCHAR(100) NOT NULL UNIQUE`, `description VARCHAR(500) NOT NULL`, `created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`, `enabled BOOLEAN NOT NULL DEFAULT TRUE`, `version BIGINT NOT NULL DEFAULT 0` | no existe | no existe | `032_create_consultation_types.xml:8-21`, `068_add_enabled_to_all_tables.xml:78-83`, `225_add_version_optimistic_lock_wave2.xml:102-111` |
| `spa_types` | idénticas a las anteriores | no existe | no existe | `043_create_spa_types.xml:8-21`, `068:222-227`, `225:612-621` |
| `surgery_types` | las anteriores **más** `company_id BIGINT NULL` (FK `fk_surgery_types_company` → `companies(id)`) y `general BOOLEAN NOT NULL DEFAULT FALSE` | sí, nullable | sí | `051_create_surgery_types.xml:8-21`, `057_alter_surgery_types_add_company_and_general.xml:7-40`, `068:270-275`, `225:469-478` |

Tres consecuencias que fijan el formato de la semilla:

1. **`description` es `NOT NULL` en las tres.** Ninguna fila puede ir sin descripción, y la
   descripción no es decorativa: es lo único que distingue dos nombres parecidos en el
   desplegable de la agenda. Todas las de este documento están escritas para el recepcionista
   que agenda, no para el veterinario que ya sabe qué es.
2. **`name` tiene tope duro de 100 caracteres** en la columna **y** en el dominio
   (`SurgeryType.java:46-47`, `ConsultationType.java:38-39`). Ninguna fila propuesta lo roza: el
   nombre más largo de las 140 mide 49 caracteres (§6).
3. **En `surgery_types` el dominio impone un XOR** entre `general` y `company`
   (`surgerytype/domain/SurgeryType.java:50-53`): `general = true` **exige** `company = null`, y
   `general = false` **exige** una empresa. Toda fila semilla va, por tanto,
   `company_id = NULL, general = TRUE`. La otra combinación es inválida para el dominio:
   `ddl-auto: validate` no la ve, pero el primer `findAll` que la mapee lanza
   `IllegalArgumentException`.

### 0.1. `consultation_types` y `spa_types` son globales puros: la clínica no puede añadir nada

Esto **sube el listón de completitud** de la semilla, y conviene que quede escrito en el propio
documento porque no se deduce del DDL:

- Las cinco operaciones de escritura y la lectura por id de los dos catálogos están cerradas a
  `hasRole('SYSTEM')` **a secas**:
  `consultationtype/application/port/in/CreateConsultationTypeUseCase.java:8`,
  `UpdateConsultationTypeUseCase.java:8`, `DeleteConsultationTypeUseCase.java:6`,
  `ReactivateConsultationTypeUseCase.java:19`, `FindConsultationTypeUseCase.java:7`; e idénticos
  en `spatype/application/port/in/`.
- La única puerta abierta al tenant es el listado, marcado
  `@NoAuthorizationRequired(reason = "Catálogo maestro global de solo lectura…")`
  (`ListConsultationTypesUseCase.java:7`, `ListSpaTypesUseCase.java:7`).

Traducción: **lo que se siembre aquí es todo lo que una clínica va a tener**. Si un tipo de
consulta o un servicio de spa no está en esta lista, el tenant no puede crearlo — ni por API ni
por pantalla. Solo la plataforma puede añadirlo, y cuando lo añada lo verán **todas** las clínicas
a la vez. Por eso las dos listas se cierran con una fila de escape (`Otro motivo de consulta`,
`Otro servicio de estética`) y por eso son más largas de lo que sería prudente en un catálogo que
el tenant pudiera extender.

`surgery_types` es distinto: el tenant **sí** puede crear tipos propios (`general = false`,
`company_id` = su empresa) y `GET /surgery-types/available` mezcla globales y propios
(`ListAvailableSurgeryTypesService.java:20-23` → `findAllByGeneralTrueOrCompany_Id`).

---

## 1. Decisiones de modelado, razonadas

### 1.1. Granularidad de `surgery_types`: un procedimiento = una fila, nunca desglosada por especie ni por peso

**Decisión:** «Ovariohisterectomía» es **una** fila. No hay «Ovariohisterectomía felino»,
«Ovariohisterectomía canino 10-25 kg» ni «Entropión (por ojo)».

**Por qué:**

- La especie y el peso **no son atributos del tipo de cirugía**: son atributos del paciente y ya
  viven en la fila de `surgeries` a través de su `animal_id`. Repetirlos en el catálogo duplica
  un dato que ya está y que puede contradecirlo.
- El desglose por tramo es una **dimensión de precio**, no clínica. En el tarifario del Hospital
  Veterinario de la Universidad Nacional, «Ovariectomía / ovariohisterectomía electiva» es **un**
  procedimiento con cuatro precios por peso, no cuatro procedimientos. El precio pertenece al
  catálogo comercial y a la lista de precios, no a `surgery_types`.
- Multiplicar por tramos es el antipatrón que Bill Karwin llama **Metadata Tribbles** (*SQL
  Antipatterns*): partir filas o tablas por el **valor** de un atributo. Aquí el coste es doble,
  porque `name` es `UNIQUE` global: cada tramo **quema un nombre más** del espacio compartido por
  todas las clínicas.
- Usabilidad al agendar: 81 procedimientos se recorren en un desplegable; 320 no. Un catálogo que
  nadie encuentra se sortea escribiendo el nombre en el campo libre, y entonces deja de servir
  para reportar.

**Lo mismo aplica a la lateralidad** («por ojo», «por lado», «bilateral») y a la **dificultad**
(«grado 1…6» del tarifario UNAL): son modificadores de facturación, no tipos clínicos.

### 1.2. El tope de 100 caracteres: cómo se acorta sin perder precisión

Regla aplicada, en este orden:

1. **El nombre lleva el término técnico**, en español y en la forma que usa el gremio
   (`Ovariohisterectomía`, `Estafilectomía`, `Ostectomía de cabeza y cuello femoral`).
2. **Las siglas consagradas van entre paréntesis en el nombre** solo cuando son el nombre real
   con el que se pide el procedimiento — `(TPLO)`, `(TTA)`, `(GDV)` — porque quitarlas obligaría
   al usuario a leer la descripción para encontrar lo que ya sabía buscar.
3. **Todo lo demás se cae del nombre y se recupera en la descripción**: el nombre completo del
   procedimiento, el sinónimo coloquial («ojo de cereza», «castración», «esterilización»), la vía
   de abordaje y la indicación. La descripción tiene 500 caracteres y sobra sitio.
4. **Nunca se abrevia truncando.** No hay `Ablación total del cond. aud. ext.`: si no cabe, se
   reformula — `Ablación total del conducto auditivo` (37 caracteres) con «externo, con osteotomía
   de la bulla timpánica (TECA-BO)» en la descripción.

### 1.3. El `UNIQUE` global de `surgery_types.name`: cuántas filas es prudente

`name VARCHAR(100) NOT NULL UNIQUE` en `surgery_types` **no está acotado por empresa**
(`051_create_surgery_types.xml:12-14`). Es decir: **cada nombre sembrado como fila global queda
quemado para todas las clínicas**. Si sembramos `Castración`, ninguna empresa podrá crear un tipo
propio con ese nombre, y lo que recibirá al intentarlo es un error de integridad sin traducir
—mismo defecto de familia que el issue #482 documenta para `spa_types`—.

**La regla que se sigue aquí es la parte del trabajo que evita el daño:**

- **Se siembra la nomenclatura técnica, nunca la coloquial.** `Orquiectomía` sí; `Castración` no.
  `Ovariohisterectomía` sí; `Esterilización` no. Así el nombre coloquial —justo el que una
  clínica pequeña querría para su propio catálogo— **queda libre**.
- **Nombres deliberadamente NO sembrados**, reservados al tenant: `Castración`,
  `Esterilización`, `Esterilización canina`, `Esterilización felina`, `Limpieza dental`,
  `Cirugía general`, `Cirugía de urgencia`, `Cirugía menor`, `Cirugía mayor`,
  `Extracción de tumor`, `OVH`, `OVE`.
- **81 filas es la cifra propuesta**, y es conservadora a propósito: la lista de procedimientos de
  residencia del ACVS pasa de 400 entradas. Se siembra lo que una clínica de pequeños animales
  agenda de verdad en Colombia, no el temario de una especialidad.

### 1.4. Nada de `ENUM`, nada de `code`: el catálogo ya es la tabla

No se propone añadir un `code` ni un `ENUM`. El esquema no lo tiene, añadir una columna a tres
tablas por comodidad del `INSERT` es cambiar el modelo para servir a la migración, y el patrón
idempotente por `name` (§5) resuelve el problema sin tocar el DDL. Si algún día hiciera falta un
`code` estable para integraciones, ese cambio va por expand/contract y **en su propio changeset**,
no en el de la semilla.

### 1.5. La fila de escape, y su coste

`consultation_types` y `spa_types` cierran con `Otro motivo de consulta` / `Otro servicio de
estética`. Es una concesión consciente y reversible:

- **A favor:** el tenant no puede crear filas. Sin escape, un motivo no previsto no se registra en
  absoluto y la clínica acaba usando el tipo que menos le miente, que corrompe el reporte más que
  una fila «Otro» honesta. Las entidades que consumen el catálogo llevan campos libres donde cabe
  el detalle: `Spa.reason`, `Spa.details` y `Spa.observations`, los tres obligatorios y de 2000
  caracteres (`spa/domain/Spa.java:76-87`).
- **En contra:** un cajón de sastre siempre se llena. Si «Otro» supera el 5 % de los registros,
  esa es la señal medida de que falta una fila y hay que sembrarla.
- **Reversible:** se da de baja con `enabled = false` sin borrar historia.

En `surgery_types` la fila `Otro procedimiento quirúrgico` se siembra por otro motivo: hoy
**ninguna ruta de la API puede crear un tipo global** (§7, hallazgo B-2), así que sin ella un
procedimiento no previsto solo existe si cada clínica se lo crea privado, una por una.

### 1.6. La collation es `utf8mb4_0900_ai_ci`: el `UNIQUE` ignora acentos y mayúsculas

**Dato verificado contra el servidor** (`@@collation_server`, el default del esquema y una columna
`name` real): la collation efectiva de las tres tablas es **`utf8mb4_0900_ai_ci`**, heredada del
servidor porque **ningún `createTable` del árbol declara `CHARACTER SET` ni `COLLATE`**. `ai_ci` es
**a**ccent-**i**nsensitive y **c**ase-**i**nsensitive.

Para la semilla eso significa dos cosas, y las dos son operativas:

1. **Dos filas que solo difieran en acento o en caja son la MISMA fila para el índice único.**
   `Ovariohisterectomía`, `Ovariohisterectomia` y `OVARIOHISTERECTOMIA` colisionan entre sí. Si la
   lista contuviera dos variantes, el `INSERT` del changeset **fallaría entero**, no solo esa fila.
   El riesgo es real justo aquí: al acortar nombres quirúrgicos largos es fácil generar dos formas
   que se solapen, y en el bloque de spa los nueve nombres que empiezan por «Baño» y los seis que
   empiezan por «Corte» son terreno abonado.
   **Comprobado**: se normalizaron las 140 filas a minúsculas y sin diacríticos y **no hay ni una
   colisión** dentro de ninguna de las tres tablas, ni contra los doce nombres reservados de §1.3.
   Los dos pares más cercanos difieren en una palabra completa, no en un acento:
   `Consulta de medicina felina` / `Consulta de medicina interna`, y
   `Reducción de prolapso uterino` / `Reducción de prolapso vaginal`.
2. **La reserva de nombres coloquiales de §1.3 vale más de lo que parecía… y el bloqueo de los
   sembrados también.** Sembrar `Orquiectomía` no le quita a la clínica solo esa cadena: le quita
   `orquiectomia`, `ORQUIECTOMIA` y toda variante de acento y caja. Es un argumento adicional a
   favor de sembrar pocas filas y técnicas, y una **dimensión nueva del hallazgo G-1** que hay que
   contar cuando se decida el arreglo del índice.

**Instrucción para `db-migrations`:** no añadas `COLLATE` a los `INSERT` ni a las tablas. Cambiar la
collation de una columna en MySQL 8.4 reconstruye la tabla y sus índices, y una divergencia de
collation entre dos columnas unidas por un `JOIN` —por ejemplo `surgery_types.name` contra
cualquier proyección— mata el uso del índice. La semilla se escribe con el default heredado, que es
el mismo que ya tienen todas las demás tablas.

---

## 2. `consultation_types` — 27 filas

Todas globales por construcción: la tabla no tiene `company_id` ni `general`.

| # | `name` | `description` |
|---|---|---|
| 1 | Consulta general | Primera atención clínica del paciente por un motivo nuevo: anamnesis, examen físico completo, orientación diagnóstica y plan de manejo inicial. Es la consulta de entrada cuando el caso no es un control ni una urgencia. |
| 2 | Control médico | Consulta de seguimiento de un paciente ya valorado: revisión de la evolución, lectura de resultados pendientes, ajuste del tratamiento y decisión de continuidad o alta. |
| 3 | Control posquirúrgico | Revisión del paciente operado: estado de la herida, retiro de puntos, control del dolor y del vendaje, y autorización del alta quirúrgica. |
| 4 | Consulta de urgencias | Atención de un paciente cuyo estado exige valoración inmediata. Incluye la clasificación por triaje al ingreso y la estabilización antes de cualquier otro procedimiento. |
| 5 | Consulta prioritaria | Atención sin cita previa de un caso que no compromete la vida pero no admite esperar a la agenda programada del día siguiente. |
| 6 | Valoración prequirúrgica | Evaluación del riesgo anestésico y quirúrgico antes de un procedimiento programado: examen físico dirigido, clasificación del estado físico, revisión de los exámenes prequirúrgicos y consentimiento informado. |
| 7 | Consulta de medicina preventiva | Chequeo del paciente sano según su etapa de vida: examen físico, plan de vacunación y desparasitación, control de peso y condición corporal, y actualización del carné. |
| 8 | Consulta pediátrica | Valoración del cachorro o gatito: desarrollo, plan sanitario inicial, pauta de alimentación, socialización y detección de defectos congénitos. |
| 9 | Consulta geriátrica | Valoración del paciente en su última etapa de vida, recomendada al menos dos veces al año: detección temprana de enfermedad crónica, evaluación del dolor, la movilidad, el deterioro cognitivo y la calidad de vida. |
| 10 | Consulta de dermatología | Consulta especializada en piel, manto y oído externo: prurito, alopecia, otitis, alergias, enfermedad autoinmune y pruebas dermatológicas complementarias. |
| 11 | Consulta de cardiología | Consulta especializada del aparato cardiovascular: soplos, arritmias, insuficiencia cardiaca, valoración del riesgo cardiaco previo a cirugía y seguimiento ecocardiográfico. |
| 12 | Consulta de oftalmología | Consulta especializada del ojo y sus anexos: úlceras corneales, glaucoma, cataratas, uveítis y alteraciones palpebrales, con las pruebas oftalmológicas específicas. |
| 13 | Consulta de ortopedia y traumatología | Consulta especializada del aparato locomotor: cojeras, fracturas, luxaciones, displasias, ruptura de ligamento cruzado craneal y planificación de la cirugía ortopédica. |
| 14 | Consulta de neurología | Consulta especializada del sistema nervioso: convulsiones, paresias y parálisis, enfermedad discal, síndrome vestibular y alteraciones de conducta de origen orgánico. |
| 15 | Consulta de oncología | Consulta especializada en tumores: estadificación, plan terapéutico, seguimiento del paciente oncológico y control de la quimioterapia. |
| 16 | Consulta de medicina interna | Consulta especializada en enfermedad sistémica: endocrinología, aparato digestivo, riñón, hígado y enfermedades infecciosas, con enfoque en el caso complejo o sin diagnóstico. |
| 17 | Consulta de medicina felina | Consulta especializada en el gato, con manejo de bajo estrés y enfoque en la patología propia de la especie: enfermedad renal crónica, hipertiroidismo y enfermedad del tracto urinario inferior. |
| 18 | Consulta de reproducción | Consulta especializada en reproducción: control del ciclo, citología vaginal, planificación de la monta o la inseminación, seguimiento de la gestación y valoración del reproductor. |
| 19 | Consulta de etología | Consulta de medicina del comportamiento: prevención, diagnóstico y tratamiento de problemas de conducta como agresividad, ansiedad por separación, miedos y eliminación inadecuada. |
| 20 | Consulta de nutrición | Valoración nutricional del paciente y recomendación dietética específica: condición corporal, masa muscular, cálculo de requerimientos y dieta terapéutica para una enfermedad concreta. |
| 21 | Consulta de rehabilitación y fisioterapia | Valoración y plan de rehabilitación física: recuperación posquirúrgica u ortopédica, manejo del dolor crónico y control del peso y del acondicionamiento. |
| 22 | Consulta de paciente remitido | Atención de un paciente enviado por otro médico veterinario para valoración especializada, segunda opinión o procedimiento no disponible en el centro remitente. |
| 23 | Certificado de salud y viaje | Examen clínico para expedir el certificado de salud exigido para el viaje del animal, con verificación de vacunación, desparasitación e identificación por microchip previa al trámite sanitario ante el ICA. |
| 24 | Eutanasia y acompañamiento | Consulta de decisión y realización de la eutanasia: valoración de la calidad de vida, consentimiento informado, sedación previa, procedimiento y acompañamiento a la familia. |
| 25 | Teleconsulta | Atención clínica prestada a distancia por medios de telecomunicación, sobre un paciente con relación veterinario-cliente-paciente ya establecida. No sustituye al examen físico cuando este es necesario. |
| 26 | Consulta a domicilio | Atención clínica prestada en el domicilio del propietario, con el alcance limitado por el equipo disponible fuera del centro. |
| 27 | Otro motivo de consulta | Motivo de consulta no contemplado en el catálogo. El detalle se registra en el texto de la consulta. Su uso frecuente es la señal de que falta un tipo por sembrar. |

**Lo que se dejó fuera a propósito:** vacunación, desparasitación, hospitalización, laboratorio,
imagen diagnóstica, spa, guardería y cirugía. No son tipos de consulta: son **submódulos propios**
del producto, ya sembrados como capacidades en `258_seed_technical_catalog.xml:38-49`. Meterlos
aquí duplicaría el concepto en dos catálogos y haría imposible reportar por uno solo.

---

## 3. `surgery_types` — 81 filas

**Todas las filas van `company_id = NULL` y `general = TRUE`.** Es la única combinación que el
dominio acepta para una fila de plataforma (`SurgeryType.java:50-53`) y la que `057` ya usó para
el backfill de las filas preexistentes (`057:35-40`).

### 3.1. Reproductiva (9)

| # | `name` | `description` |
|---|---|---|
| 1 | Ovariohisterectomía | Extirpación de ovarios y útero. Es la esterilización electiva de la hembra, comúnmente llamada «castración» o «esterilización». Vía laparotomía por la línea media ventral. |
| 2 | Ovariectomía | Extirpación de los ovarios conservando el útero. Alternativa a la ovariohisterectomía en la hembra joven y sana, con menor abordaje. |
| 3 | Orquiectomía | Extirpación de ambos testículos. Es la castración del macho, electiva o terapéutica, por abordaje prescrotal en el perro y escrotal en el gato. |
| 4 | Orquiectomía de testículo criptórquido | Extirpación del testículo no descendido, alojado en el canal inguinal o en la cavidad abdominal. Exige abordaje inguinal o laparotomía, y no es equiparable a la orquiectomía corriente. |
| 5 | Cesárea | Extracción quirúrgica de los fetos por histerotomía ante distocia o por indicación programada. Incluye la reanimación de los neonatos. |
| 6 | Ovariohisterectomía por piómetra | Ovariohisterectomía de urgencia por infección uterina. No es la cirugía electiva: el paciente llega comprometido y exige estabilización previa y manejo posoperatorio distinto. |
| 7 | Reducción de prolapso vaginal | Corrección del prolapso o la hiperplasia del suelo vaginal, con resección del tejido protruido cuando está desvitalizado. |
| 8 | Reducción de prolapso uterino | Reposición o resección del útero prolapsado tras el parto. Procedimiento de urgencia. |
| 9 | Vasectomía | Sección y ligadura de los conductos deferentes. Esteriliza al macho conservando la función testicular endocrina. |

### 3.2. Tejidos blandos — abdomen y aparato digestivo (12)

| # | `name` | `description` |
|---|---|---|
| 10 | Laparotomía exploratoria | Apertura de la cavidad abdominal para exploración y diagnóstico, con toma de biopsias, sin un procedimiento adicional planeado de antemano. |
| 11 | Gastrotomía | Apertura del estómago para extraer un cuerpo extraño o tomar biopsia de pared, con cierre en dos planos. |
| 12 | Gastropexia preventiva | Fijación del estómago a la pared abdominal derecha para prevenir la recidiva o la aparición del vólvulo gástrico en razas predispuestas. |
| 13 | Corrección de dilatación y vólvulo gástrico (GDV) | Urgencia quirúrgica: descompresión y desrotación del estómago, valoración de la viabilidad de la pared y del bazo, y gastropexia en el mismo acto. |
| 14 | Enterotomía | Apertura del intestino para extraer un cuerpo extraño o tomar biopsia, conservando el segmento intestinal. |
| 15 | Enterectomía con anastomosis | Resección del segmento intestinal inviable y anastomosis término-terminal. Indicada en obstrucción con necrosis, invaginación o masa intestinal. |
| 16 | Esplenectomía | Extirpación total o parcial del bazo por masa esplénica, torsión o traumatismo con hemorragia. |
| 17 | Lobectomía hepática | Resección total o parcial de un lóbulo hepático por masa, absceso o traumatismo. |
| 18 | Colecistectomía | Extirpación de la vesícula biliar, con exploración y permeabilización de la vía biliar. Indicada en mucocele y colecistitis. |
| 19 | Ligadura de shunt portosistémico | Cierre progresivo del vaso anómalo que desvía la sangre portal, con anillo ameroide, banda de celofán o sutura. |
| 20 | Adrenalectomía | Extirpación de la glándula suprarrenal por masa funcional o no funcional, con valoración de la invasión de la vena cava. |
| 21 | Corrección de prolapso rectal | Reducción y fijación del recto prolapsado, por vía perineal o por laparotomía con colopexia según la gravedad y la recidiva. |

### 3.3. Aparato urinario (4)

| # | `name` | `description` |
|---|---|---|
| 22 | Cistotomía | Apertura de la vejiga para extraer urolitos, tomar biopsia o resecar una masa de la pared. |
| 23 | Uretrostomía | Creación de una abertura uretral permanente (perineal, prescrotal, escrotal o prepúbica) ante obstrucción uretral recurrente o irresoluble. |
| 24 | Nefrotomía | Apertura del riñón para extraer un cálculo de la pelvis renal, conservando el órgano. |
| 25 | Nefrectomía | Extirpación de un riñón por masa, hidronefrosis o traumatismo, con función renal contralateral comprobada. |

### 3.4. Pared abdominal, piel y reconstructiva (10)

| # | `name` | `description` |
|---|---|---|
| 26 | Herniorrafia umbilical | Cierre del anillo umbilical persistente. Con frecuencia se resuelve en el mismo acto de la esterilización electiva. |
| 27 | Herniorrafia inguinal | Reducción del contenido herniado y cierre del anillo inguinal, uni o bilateral. |
| 28 | Herniorrafia perineal | Reconstrucción del diafragma pélvico, con transposición del músculo obturador interno cuando hace falta. Frecuente en el macho entero de edad avanzada. |
| 29 | Herniorrafia diafragmática | Reparación de la rotura del diafragma, casi siempre traumática, con reexpansión pulmonar y drenaje torácico. |
| 30 | Herniorrafia de pared abdominal | Cierre de la eventración o de la hernia de pared traumática o incisional, con malla si el defecto lo exige. |
| 31 | Resección de masa cutánea o subcutánea | Extirpación de una masa de piel o tejido subcutáneo con margen quirúrgico, y remisión de la pieza a histopatología. |
| 32 | Mastectomía | Extirpación de la cadena mamaria, regional o radical, por tumor mamario. Puede acompañarse de ovariohisterectomía en el mismo acto. |
| 33 | Saculectomía anal | Extirpación de los sacos anales por absceso recurrente, impactación crónica o neoplasia. |
| 34 | Debridación y sutura de heridas | Limpieza, desbridamiento del tejido desvitalizado y cierre de una herida traumática o por mordedura, con drenaje si procede. |
| 35 | Colgajo cutáneo o injerto de piel | Reconstrucción de un defecto cutáneo extenso mediante colgajo de avance, rotación, transposición o patrón axial, o injerto libre de piel. |

### 3.5. Vía aérea, cuello y tórax (7)

| # | `name` | `description` |
|---|---|---|
| 36 | Estafilectomía | Resección del paladar blando elongado, componente principal de la corrección del síndrome braquicefálico. |
| 37 | Corrección de narinas estenóticas | Rinoplastia o alarplastia que amplía el orificio nasal en el paciente braquicéfalo. Suele hacerse junto con la estafilectomía. |
| 38 | Lateralización aritenoidea | Fijación del cartílago aritenoides en abducción para tratar la parálisis laríngea. |
| 39 | Traqueostomía | Apertura de la tráquea para asegurar la vía aérea, temporal o permanente, en obstrucción de vía aérea alta. |
| 40 | Toracotomía exploratoria | Apertura de la cavidad torácica —intercostal o por esternotomía media— para exploración, biopsia o resección de una masa intratorácica. |
| 41 | Lobectomía pulmonar | Resección total o parcial de un lóbulo pulmonar por masa, torsión, absceso o traumatismo. |
| 42 | Ligadura de ducto arterioso persistente | Cierre quirúrgico del conducto arterioso persistente por toracotomía. Cardiopatía congénita corregible del cachorro. |

### 3.6. Oído (3)

| # | `name` | `description` |
|---|---|---|
| 43 | Manejo quirúrgico de otohematoma | Drenaje y fijación del pabellón auricular para tratar el hematoma auricular y evitar la deformidad en coliflor. |
| 44 | Ablación total del conducto auditivo | Ablación total del conducto auditivo externo con osteotomía de la bulla timpánica (TECA-BO), en otitis crónica de fin de vía o neoplasia del conducto. |
| 45 | Resección lateral del conducto auditivo | Resección de la pared lateral del conducto auditivo vertical (técnica de Zepp) para mejorar el drenaje y la ventilación del oído. |

### 3.7. Ortopedia (15)

| # | `name` | `description` |
|---|---|---|
| 46 | Osteosíntesis con placa | Reducción abierta y fijación interna de una fractura con placa y tornillos, sola o combinada con clavo intramedular. |
| 47 | Osteosíntesis con clavo intramedular | Fijación de una fractura de hueso largo con clavo intramedular o clavo encerrojado, con o sin cerclaje. |
| 48 | Osteosíntesis con fijador externo | Estabilización de una fractura con fijador externo lineal, híbrido o circular. Indicada en fractura abierta o con pérdida de sustancia. |
| 49 | Osteotomía niveladora de la meseta tibial (TPLO) | Nivelación de la meseta tibial para neutralizar la subluxación craneal de la tibia tras la ruptura del ligamento cruzado craneal. |
| 50 | Avance de la tuberosidad tibial (TTA) | Avance de la tuberosidad tibial para neutralizar la inestabilidad por ruptura del ligamento cruzado craneal. Alternativa a la TPLO. |
| 51 | Estabilización extracapsular de ligamento cruzado | Estabilización de la rodilla mediante sutura fabelo-tibial o retinacular, fuera de la articulación. Es la técnica tradicional para el paciente de bajo peso. |
| 52 | Corrección de luxación rotuliana | Corrección de la luxación de rótula con trocleoplastia, transposición de la tuberosidad tibial y ajuste de los tejidos blandos, según el grado. |
| 53 | Ostectomía de cabeza y cuello femoral | Resección de la cabeza y el cuello del fémur para crear una seudoartrosis indolora en displasia de cadera, luxación irreducible o necrosis de la cabeza femoral. |
| 54 | Reducción abierta de luxación articular | Reducción quirúrgica y estabilización de una luxación de cadera, codo, hombro o tarso que no se mantiene tras la reducción cerrada. |
| 55 | Artrodesis | Fusión quirúrgica de una articulación —carpo, tarso, hombro, codo o rodilla—, total o parcial, ante inestabilidad o artrosis irreparable. |
| 56 | Reemplazo total de cadera | Sustitución protésica de la articulación coxofemoral en displasia avanzada o artrosis incapacitante. |
| 57 | Osteotomía correctiva | Corrección quirúrgica de una deformidad angular o rotacional de un hueso largo, estabilizada con placa, clavo encerrojado o fijador externo. |
| 58 | Amputación de miembro | Amputación de un miembro anterior o posterior por traumatismo irreparable, neoplasia o dolor incontrolable, por desarticulación o con escapulectomía. |
| 59 | Extracción de material de osteosíntesis | Retiro de placas, tornillos, clavos o fijadores una vez consolidada la fractura, o antes por intolerancia o infección del implante. |
| 60 | Artrotomía exploratoria | Apertura quirúrgica de una articulación para exploración, biopsia sinovial o extracción de un fragmento osteocondral. |

### 3.8. Neurocirugía (2)

| # | `name` | `description` |
|---|---|---|
| 61 | Hemilaminectomía | Descompresión de la médula espinal por abordaje lateral, con o sin fenestración del disco, en enfermedad discal toracolumbar. |
| 62 | Craneotomía | Apertura del cráneo para biopsia o resección de una masa intracraneal, o para descompresión tras traumatismo. |

### 3.9. Odontología y cirugía oral (5)

| # | `name` | `description` |
|---|---|---|
| 63 | Profilaxis dental | Limpieza dental profesional bajo anestesia general: raspado supragingival y subgingival, pulido y sondaje periodontal completo con registro por diente. |
| 64 | Extracción dental simple | Extracción de una pieza dental sin colgajo ni ostectomía, en diente de raíz única o con movilidad avanzada. |
| 65 | Extracción dental quirúrgica | Extracción con elevación de colgajo mucogingival, ostectomía y seccionamiento de raíces, con cierre del alveolo. |
| 66 | Tratamiento periodontal | Tratamiento de la enfermedad periodontal más allá de la limpieza: alisado radicular, curetaje, colgajo periodontal, gingivectomía o aplicación local de antisépticos. |
| 67 | Mandibulectomía o maxilectomía parcial | Resección parcial de la mandíbula o del maxilar por neoplasia oral o fractura irreparable, con reconstrucción de tejidos blandos. |

### 3.10. Oftálmica (8)

| # | `name` | `description` |
|---|---|---|
| 68 | Corrección de entropión | Blefaroplastia que corrige la inversión del párpado y el roce de las pestañas contra la córnea. |
| 69 | Corrección de ectropión | Blefaroplastia que corrige la eversión del párpado y la exposición conjuntival crónica. |
| 70 | Reposición de la glándula del tercer párpado | Reposición quirúrgica de la glándula del tercer párpado prolapsada, conocida como «ojo de cereza», por técnica de bolsillo o de anclaje. Nunca por extirpación. |
| 71 | Queratotomía en rejilla | Desbridamiento del epitelio corneal no adherido y queratotomía en rejilla o puntiforme para tratar la úlcera corneal superficial crónica indolente. |
| 72 | Recubrimiento conjuntival | Injerto o colgajo de conjuntiva que cubre y nutre una úlcera corneal profunda, un descemetocele o una perforación. |
| 73 | Enucleación | Extirpación del globo ocular con cierre palpebral definitivo, ante ojo ciego doloroso, glaucoma terminal o neoplasia intraocular. |
| 74 | Facoemulsificación de catarata | Extracción del cristalino cataratoso por facoemulsificación, con implante de lente intraocular. |
| 75 | Resección de masa palpebral | Extirpación de un tumor del párpado con margen, y blefaroplastia reconstructiva cuando el defecto supera un tercio del borde palpebral. |

### 3.11. Mínimamente invasiva (5)

| # | `name` | `description` |
|---|---|---|
| 76 | Ovariectomía laparoscópica | Extirpación de los ovarios por laparoscopia, con menor dolor posoperatorio y recuperación más rápida que la vía abierta. |
| 77 | Gastropexia laparoscópica | Fijación preventiva del estómago a la pared abdominal por vía laparoscópica o asistida por laparoscopia. |
| 78 | Laparoscopia exploratoria | Exploración de la cavidad abdominal por laparoscopia, con toma de biopsias de hígado, riñón, páncreas o intestino. |
| 79 | Endoscopia intervencionista | Procedimiento terapéutico por endoscopia flexible o rígida: extracción de cuerpo extraño digestivo, dilatación esofágica o colocación de sonda de alimentación. |
| 80 | Artroscopia | Exploración articular por artroscopia, con extracción de fragmentos, liberación meniscal o desbridamiento en hombro, codo, rodilla o tarso. |

### 3.12. Escape (1)

| # | `name` | `description` |
|---|---|---|
| 81 | Otro procedimiento quirúrgico | Procedimiento no contemplado en el catálogo global. El detalle se registra en el texto de la cirugía. Su uso frecuente indica que falta un tipo por sembrar, o que la clínica debería crear el suyo propio. |

---

## 4. `spa_types` — 32 filas

Todas globales por construcción: la tabla no tiene `company_id` ni `general`. La clínica **no
puede añadir servicios propios**, así que la lista cubre la oferta habitual de una peluquería y
spa profesional en Colombia, con la nomenclatura del gremio.

### 4.1. Baños (8)

| # | `name` | `description` |
|---|---|---|
| 1 | Baño y secado | Baño completo con champú acorde al tipo de manto, aclarado y secado profesional con soplador y cepillado. Es el servicio base de la peluquería. |
| 2 | Baño medicado | Baño con champú medicado prescrito o recomendado por el veterinario, con tiempo de contacto controlado, para dermatitis, seborrea, control bacteriano o fúngico y prurito. |
| 3 | Baño antipulgas | Baño con producto insecticida para eliminar las pulgas presentes en el manto. No sustituye al antiparasitario externo de uso periódico. |
| 4 | Baño hidratante de avena | Baño con champú y mascarilla a base de avena para piel sensible o reseca, con efecto calmante sobre el prurito leve. |
| 5 | Baño realce de color | Baño con champú específico que realza el color del manto —blanco, negro, dorado o marrón— sin teñirlo. |
| 6 | Baño en seco | Limpieza sin agua con producto en espuma o polvo, para gatos, pacientes que no toleran el baño o animales con vendaje o herida reciente. |
| 7 | Baño de ozono | Baño en tina con microburbujas de ozono generadas por ozonizador, para limpieza profunda, control del olor, desinfección de la piel y apoyo a los tratamientos dermatológicos. |
| 8 | Primer baño de cachorro | Primer baño del cachorro, con manejo suave y progresivo para que asocie el proceso a una experiencia positiva. Sujeto a que el plan de vacunación lo permita. |

### 4.2. Corte y arreglo (9)

| # | `name` | `description` |
|---|---|---|
| 9 | Corte higiénico | Recorte del pelo de las zonas genital, perianal, axilar y de las almohadillas plantares. Es un servicio de higiene, no de estética. |
| 10 | Corte a máquina | Rebaje uniforme de todo el manto a máquina, con el número de peine acordado. Conocido como corte de verano. |
| 11 | Corte de raza | Arreglo según el patrón propio de la raza, con tijera y máquina, respetando las proporciones del estándar. |
| 12 | Corte tipo schnauzer | Patrón de schnauzer: lomo y flancos apurados a máquina, faldón, cejas y barba conservados y perfilados con tijera. |
| 13 | Corte tipo poodle | Patrón de poodle en cualquiera de sus variantes de salón —cachorro, holandés, continental o león—, con acabado a tijera. |
| 14 | Corte tipo cachorro | Recorte de todo el manto a una longitud uniforme y corta, con acabado redondeado en cabeza y patas. Apto para cualquier raza de pelo largo. |
| 15 | Arreglo de patas | Perfilado del pelo de las patas y de entre las almohadillas, con limpieza del pelo interdigital. |
| 16 | Pulida de corte | Repaso y perfilado de un corte anterior para prolongar su duración, sin rehacer el patrón completo. |
| 17 | Stripping | Arrancado del pelo muerto a navaja o a mano en razas de pelo duro, como los terrier, para renovar el manto y conservar su textura y color. |

### 4.3. Cuidado del manto (3)

| # | `name` | `description` |
|---|---|---|
| 18 | Deslanado | Retiro del subpelo muerto con herramienta específica para rebajar el volumen del manto y reducir la caída de pelo, conservando la capa de cobertura. |
| 19 | Cepillado y desenredado | Cepillado completo con retiro de nudos superficiales, previo o independiente del baño. |
| 20 | Rescate de manto | Desenredo profundo de un manto apelmazado, nudo a nudo y con producto desenredante, como alternativa a rapar al animal. |

### 4.4. Higiene (5)

| # | `name` | `description` |
|---|---|---|
| 21 | Corte y limado de uñas | Corte de las uñas hasta el límite del lecho vascular y limado del borde para evitar enganches y arañazos. |
| 22 | Limpieza de oídos | Limpieza del pabellón y del conducto auditivo externo con solución ótica, y retiro del pelo del conducto en las razas que lo requieren. |
| 23 | Cepillado dental estético | Cepillado de los dientes con pasta dental para animales, con fin higiénico y de control del mal aliento. No sustituye a la profilaxis dental bajo anestesia. |
| 24 | Drenaje de glándulas anales | Vaciado manual de los sacos anales cuando el animal lo requiere. Si hay dolor, inflamación o secreción anómala, el caso pasa a valoración veterinaria. |
| 25 | Hidratación de nariz y almohadillas | Aplicación de bálsamo hidratante en la trufa y en las almohadillas plantares resecas o agrietadas. |

### 4.5. Tratamientos y paquetes (5)

| # | `name` | `description` |
|---|---|---|
| 26 | Mascarilla capilar hidratante | Aplicación de mascarilla acondicionadora tras el baño para hidratar, dar brillo y facilitar el peinado del manto. |
| 27 | Tratamiento antiseborreico | Baño y tratamiento tópico con producto queratolítico para el manto graso o con exceso de descamación. |
| 28 | Masaje relajante y aromaterapia | Masaje corporal con aromaterapia para reducir el estrés durante la sesión de estética, en pacientes nerviosos o de edad avanzada. |
| 29 | Colorimetría | Aplicación de tinte apto para mascotas en cola, orejas o puntas, con fines exclusivamente estéticos. |
| 30 | Paquete spa completo | Sesión completa que reúne baño, mascarilla, masaje, corte higiénico, deslanado, corte de uñas, limpieza de oídos y perfumado final. |

### 4.6. Modalidad y escape (2)

| # | `name` | `description` |
|---|---|---|
| 31 | Baño a domicilio | Baño y arreglo realizados en el domicilio del propietario o en unidad móvil, con el alcance limitado por el equipo disponible fuera del local. |
| 32 | Otro servicio de estética | Servicio de estética no contemplado en el catálogo. El detalle se registra en el texto del servicio. Su uso frecuente es la señal de que falta un tipo por sembrar. |

### 4.7. Lo que se investigó y NO se siembra, con el motivo

- **Spa de barro / arcilla.** No se encontró ninguna peluquería profesional en Colombia que lo
  ofrezca con ese nombre. Lo que sí existe y sí se siembra es la **mascarilla capilar** (fila 26),
  que es el servicio real detrás de esa idea. Si mañana aparece la evidencia, se añade entonces.
- **Baño de ozono**: sí se siembra (fila 7). Está documentado como servicio de peluquería y hay
  establecimientos en Bogotá y Medellín que lo anuncian. Se describe como servicio **estético**, no
  como ozonoterapia médica, que es otra cosa y no pertenece a este catálogo.
- **Guardería, adiestramiento y transporte.** No son estética: guardería es un submódulo propio
  (`DAYCARE`, `258_seed_technical_catalog.xml:47`) y el resto no existe en el producto.

---

## 5. Especificación para `db-migrations`

### 5.1. Numeración y orden

Último changeset incluido en `db.changelog-master.xml`: **284**. Los tres nuevos son los
siguientes libres, y **no dependen entre sí** (tres tablas disjuntas):

| Changeset | Fichero propuesto | Filas |
|---|---|---|
| 285 | `285_seed_consultation_types.xml` | 27 |
| 286 | `286_seed_surgery_types.xml` | 81 |
| 287 | `287_seed_spa_types.xml` | 32 |

Se declaran uno a uno en `db.changelog-master.xml`, como el resto.

### 5.2. Forma del `INSERT` — idempotente, columnas explícitas, nunca `INSERT IGNORE`

El patrón de la casa para sembrar es `<sql>` con `INSERT … SELECT … WHERE NOT EXISTS`, como en
`258_seed_technical_catalog.xml:32-40`. Se reutiliza tal cual. Tres reglas que **no** son
negociables:

1. **`INSERT IGNORE` está prohibido** por el CLAUDE.md: convierte el error en aviso, la fila no
   entra y nadie se entera. Ya engañó tres veces el 2026-08-17.
2. **Todas las columnas `NOT NULL` van explícitas en el `INSERT`**, aunque tengan `DEFAULT`:
   `name`, `description`, `enabled`, `version`, `created_date` y, en `surgery_types`, además
   `company_id` y `general`. La razón es la misma: el fallo por `NOT NULL` omitido aparece más
   tarde y disfrazado.
3. **La idempotencia se ancla en `name`**, que es la clave natural única de las tres tablas.

Esqueleto para `surgery_types` (el de las otras dos es el mismo sin `company_id` ni `general`):

```sql
INSERT INTO surgery_types (name, description, company_id, general, enabled, version, created_date)
SELECT seed.name, seed.description, NULL, TRUE, TRUE, 0, CURRENT_TIMESTAMP
FROM (
    SELECT 'Ovariohisterectomía' AS name,
           'Extirpación de ovarios y útero. …' AS description
    UNION ALL SELECT 'Ovariectomía', 'Extirpación de los ovarios conservando el útero. …'
    -- … 79 filas más
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM surgery_types st WHERE st.name = seed.name);
```

- `version = 0` y no `NULL`: la columna es `NOT NULL` (`225:469-478`) y el mapeo `@Version` de
  Hibernate espera un entero desde la primera lectura.
- `enabled = TRUE` explícito, coherente con `@SQLRestriction("enabled = true")`.
- **Ojo con el `WHERE NOT EXISTS` y el borrado lógico**: si una fila con ese nombre existe pero
  está `enabled = false`, el `INSERT` no la vuelve a meter **y tampoco la reactiva**. Es el
  comportamiento correcto para una semilla (no debe deshacer una decisión de la plataforma), pero
  hay que decirlo en el comentario del changeset para que nadie lo lea como un fallo.

### 5.3. `preConditions`

Una por changeset, con `onFail="HALT"`, siguiendo el patrón de `226`/`210`/`206`:

```xml
<preConditions onFail="HALT">
    <tableExists tableName="surgery_types"/>
    <columnExists tableName="surgery_types" columnName="general"/>
</preConditions>
```

`HALT` y no `MARK_RAN`: si la tabla o la columna no están, el árbol de migraciones no es el que
esta semilla supone, y seguir adelante sembraría en un esquema desconocido.

### 5.4. `<rollback>`

Explícito y acotado por nombre, nunca `DELETE FROM tabla` a secas — hay que poder revertir la
semilla sin llevarse por delante lo que la plataforma haya creado después:

```xml
<rollback>
    <sql>DELETE FROM surgery_types
          WHERE general = TRUE AND company_id IS NULL AND name IN ('Ovariohisterectomía', …);</sql>
</rollback>
```

**Advertencia que debe ir en el comentario del changeset:** el rollback fallará con error de clave
foránea si alguna cirugía ya referencia el tipo (`surgeries.surgery_type_id`). Eso es correcto y
deseable: significa que hay historia clínica colgando y que la salida no es borrar sino
`enabled = false`.

### 5.5. Coste del cambio y despliegue sin downtime

- **No hay `ALTER`.** Son tres `INSERT` de 27, 81 y 32 filas: ni *instant*, ni *in place*, ni
  reconstrucción de tabla. No aplica la tabla de DDL online de InnoDB porque no hay DDL.
- **Volumen:** 140 filas en total, unos 60 KB contando índices. Irrelevante frente a los 20 GiB de
  gp3 de la instancia. No hay razón de rendimiento para trocear el `INSERT`, ni de lejos el
  umbral en el que un backfill por lotes empieza a tener sentido.
- **Expand/contract:** la semilla es puro *expand*. Añadir filas a un catálogo no rompe a ningún
  cliente desplegado; el front que ya está en producción simplemente empieza a ver más opciones en
  su desplegable. No hace falta coordinar despliegue de backend y front.
- **Índices:** no se propone ninguno nuevo. Las tres tablas se leen enteras (`findAll`) o por `id`,
  el `UNIQUE` de `name` ya sirve la búsqueda por nombre del `WHERE NOT EXISTS`, y con 140 filas
  cualquier índice extra costaría más escritura de la que ahorra en lectura. En `surgery_types`,
  `findAllByGeneralTrueOrCompany_Id` es un `OR` sobre 81 filas globales más las privadas de una
  empresa: a esta escala el escaneo es más barato que cualquier índice. **Se revisa cuando alguna
  empresa pase de unos pocos cientos de tipos propios, no antes.**

### 5.6. Cómo se verifica después de aplicar

```sql
SELECT 'consultation_types' t, COUNT(*) FROM consultation_types WHERE enabled = TRUE
UNION ALL SELECT 'spa_types', COUNT(*) FROM spa_types WHERE enabled = TRUE
UNION ALL SELECT 'surgery_types (global)', COUNT(*) FROM surgery_types
          WHERE enabled = TRUE AND general = TRUE AND company_id IS NULL;
-- esperado: 27 / 32 / 81

-- ninguna fila global puede tener empresa (invariante XOR del dominio)
SELECT COUNT(*) FROM surgery_types WHERE general = TRUE AND company_id IS NOT NULL;  -- 0

-- ningún nombre supera el tope de la columna ni queda sin descripción
SELECT COUNT(*) FROM surgery_types WHERE CHAR_LENGTH(name) > 100 OR description IS NULL;  -- 0
```

---

## 6. Invariantes: regla de negocio → constraint que la garantiza

| Invariante | Dónde vive hoy | Estado |
|---|---|---|
| Dos tipos de consulta no pueden llamarse igual | `consultation_types.name UNIQUE` (`032:12-14`) | **Garantizada en base** |
| Dos tipos de spa no pueden llamarse igual | `spa_types.name UNIQUE` (`043:12-14`) | **Garantizada en base** |
| Dos tipos de cirugía no pueden llamarse igual | `surgery_types.name UNIQUE` (`051:12-14`) | **Garantizada en base**, pero **globalmente**, no por empresa — ver hallazgo G-1 |
| Ningún tipo puede quedarse sin descripción | `description NOT NULL` en las tres | **Garantizada en base** para las tres; el dominio la exige en `ConsultationType.java:40-41`, **pero no en `SurgeryType.java:48-49`** — ver hallazgo G-2 |
| Un tipo global de cirugía no pertenece a ninguna empresa, y uno privado sí | Solo en Java: `SurgeryType.java:50-53` | **NO garantizada en base.** No hay `CHECK` — ver hallazgo G-3 |
| Un tipo dado de baja no desaparece: se conserva para la historia | `enabled BOOLEAN` + `@SQLDelete`/`@SQLRestriction` | **Garantizada**, con el efecto colateral del issue #482 |
| Una fila de catálogo no se pisa entre dos ediciones concurrentes | `version BIGINT NOT NULL` + `@Version` | **Garantizada** |

**Verificación de los nombres propuestos** (medida sobre las tablas de este documento con un
script, no sobre la base de datos): **140 filas** — 27 + 81 + 32 —, ningún `name` duplicado dentro
de su tabla, ninguno de los doce nombres reservados de §1.3 sembrado por error, cero descripciones
vacías, longitud máxima de `name` **49** caracteres
(`Corrección de dilatación y vólvulo gástrico (GDV)`) y longitud máxima de `description` **218**
caracteres. Todo holgadamente dentro de `VARCHAR(100)` y `VARCHAR(500)`.

**Y la comprobación que de verdad decide si el `INSERT` entra**: normalizando los 140 nombres a
minúsculas y sin diacríticos —lo que hace `utf8mb4_0900_ai_ci`, §1.6— **no hay ninguna colisión**
dentro de ninguna de las tres tablas ni contra los nombres reservados. Sin esa comprobación, la
verificación por longitud y por igualdad exacta no habría bastado: el índice único no compara las
cadenas como las compara Java.

---

## 7. Hallazgos

> **[Bloqueante] B-1 — Los tres catálogos se despliegan vacíos, y dos de ellos la clínica no los puede llenar**
> `032_create_consultation_types.xml`, `043_create_spa_types.xml`, `051_create_surgery_types.xml`
> — solo DDL, cero `INSERT` en los 227 changesets.
> **Criterio:** `CreateConsultationTypeUseCase.java:8` y `CreateSpaTypeUseCase.java:8` están
> cerrados a `hasRole('SYSTEM')`; el listado del tenant es de solo lectura
> (`ListConsultationTypesUseCase.java:7`).
> **Impacto:** una clínica recién dada de alta abre la agenda de consulta y de spa y encuentra un
> desplegable vacío, sin ninguna acción disponible para arreglarlo. No es un catálogo incompleto:
> es una feature que no arranca. Alcanza a los tres slices `consultation`, `spa` y `surgery`.
> **Arreglo:** los changesets 285/286/287 de §5. **Verificación:** las tres consultas de §5.6.

> **[Bloqueante] B-2 — Ninguna ruta de la API puede crear un tipo de cirugía global: `general = true` es inalcanzable**
> `SurgeryTypeController.java:50-53` inyecta siempre `authz.currentCompanyId()` en el command.
> **Criterio:** `Authz.currentCompanyId()` (`Authz.java:48-55`) devuelve la empresa del empleado,
> la empresa de plataforma para un `SystemUserContext`, y **lanza `AccessDeniedException`** para
> un `SystemContext`. Con empresa no nula, `SurgeryType.validate` rechaza `general = true`
> (`SurgeryType.java:50-51`); sin empresa, el controller ya falló antes de llegar al dominio.
> **Impacto:** el campo `general` de `CreateSurgeryTypeRequest.java` es inservible: **no existe
> ningún actor que pueda crear un tipo global por HTTP**. Todo tipo global tiene que nacer de una
> migración, para siempre. Añadir un procedimiento nuevo al catálogo de plataforma exige un
> despliegue.
> **Arreglo:** un caso de uso hermano para SYSTEM que no derive la empresa del contexto
> (`currentCompanyIdOrNull`, que ya existe y ya se usa en el `delete` de la línea 81), o quitar el
> campo `general` del request si la decisión es que los globales solo vengan por migración. Es
> territorio de `backend-feature`. **Verificación:** un `@WebMvcTest` que cree un tipo global.

> **[Grave] G-1 — El `UNIQUE` de `surgery_types.name` es global: sembrar un nombre se lo quita a todas las clínicas para siempre** — `051_create_surgery_types.xml:12-14`
> **Criterio:** doctrina de tenencia con discriminador: en un esquema multi-tenant una clave
> natural casi nunca es única globalmente, es única **por empresa** (`UNIQUE (company_id, name)`).
> Aquí la tabla mezcla filas globales (`company_id IS NULL`) y privadas bajo un único índice.
> **Impacto:** dos escenarios reales. (a) La clínica A crea «Cirugía de rodilla»; la clínica B
> recibe un error de integridad sin traducir al intentar el mismo nombre, y no puede ver por qué,
> porque la fila de A no le aparece en `/surgery-types/available`. (b) Cada fila semilla quema un
> nombre del espacio común. Con 81 filas el daño está acotado **solo porque** la semilla usa
> nomenclatura técnica y reserva la coloquial (§1.3).
> **Dimensión que agrava el impacto y que conviene contar al decidir el arreglo:** la collation es
> `utf8mb4_0900_ai_ci` (§1.6), así que el nombre quemado no es una cadena sino **una clase de
> equivalencia**. Sembrar `Orquiectomía` le quita a toda clínica `orquiectomia`, `ORQUIECTOMIA` y
> cualquier variante de acento o caja; y el error que recibe el usuario es el mismo de integridad
> sin traducir, ahora sobre un nombre que **no se parece visualmente** al que ya existe.
> **Arreglo:** MySQL 8.4 no tiene índice único parcial, así que el patrón de la casa es la columna
> generada `STORED` que vale `NULL` fuera de alcance (changesets `226`, `210`, `206`), aplicada
> aquí como `UNIQUE (company_key, name)` con
> `company_key = COALESCE(company_id, 0)` — o `UNIQUE (name)` solo sobre las globales y
> `UNIQUE (company_id, name)` sobre las privadas. **No es prerrequisito de la semilla**, pero sí de
> la primera clínica que choque. Va como issue propio.

> **[Grave] G-2 — `SurgeryType` acepta una descripción nula que la columna rechaza: el 500 llega desde la base, no desde la validación** — `surgerytype/domain/SurgeryType.java:48-49` frente a `051_create_surgery_types.xml:15-17`
> **Criterio:** una invariante que solo vive en un lado se rompe por el otro. `ConsultationType`
> **sí** la exige (`ConsultationType.java:40-41`: `description is required`); `SurgeryType` solo
> comprueba la longitud, y `CreateSurgeryTypeRequest.java` declara `@Size(max = 500)` sin
> `@NotBlank`.
> **Impacto:** un `POST /surgery-types` sin descripción pasa la validación de Spring, pasa el
> dominio y muere en el `INSERT` con `DataIntegrityViolationException`. El cliente recibe un error
> genérico en vez del error de campo que el front sabe pintar. Es el mismo defecto de forma que
> describe #135 para `@Valid`.
> **Arreglo:** `@NotBlank` en el request y la comprobación de nulo en `SurgeryType.validate`, igual
> que en `ConsultationType`. Territorio de `backend-feature`. *(El issue hermano sobre `description`
> mencionado dentro de #482 cubre el ángulo de `spa_types`; este es el de `surgery_types`.)*

> **[Grave] G-3 — El XOR entre `general` y `company_id` solo existe en Java: la base acepta las cuatro combinaciones** — `057_alter_surgery_types_add_company_and_general.xml:7-32`
> **Criterio:** la concurrencia y el SQL directo se comen toda validación que solo esté en la
> aplicación. La regla es `general = TRUE ⇔ company_id IS NULL` (`SurgeryType.java:50-53`) y la
> base no la conoce: no hay `CHECK`.
> **Impacto:** una fila `general = TRUE, company_id = 7` es aceptada por MySQL y **revienta al
> leerla**, porque el constructor de `SurgeryType` lanza `IllegalArgumentException` al mapear. Un
> listado entero cae por una sola fila mal insertada, y el punto de entrada natural para esa fila
> es exactamente una semilla escrita a mano. Peor: la fila `general = FALSE, company_id = NULL`
> también pasa, y esa la tuvo el sistema de verdad —`057:33-40` es el backfill que la corrigió—.
> **Arreglo:** `ALTER TABLE surgery_types ADD CONSTRAINT ck_surgery_types_general_xor CHECK
> ((general = TRUE AND company_id IS NULL) OR (general = FALSE AND company_id IS NOT NULL));`.
> En MySQL 8.4 añadir un `CHECK` es `ALGORITHM=COPY` —reconstruye la tabla—, pero sobre 81 filas
> es instantáneo, y hacerlo **antes** de que la tabla crezca es exactamente el argumento. Va como
> issue propio, con el `<rollback>` siendo un `DROP CHECK`. **Verificación:** el `SELECT COUNT(*)`
> de §5.6 debe dar 0 antes de intentar el `ALTER`.

> **[Grave] G-4 — Ninguna empresa puede añadir un tipo de consulta ni un servicio de spa que no hayamos previsto** — `consultationtype/application/port/in/*.java`, `spatype/application/port/in/*.java`
> **Criterio:** modelo de tenencia. `consultation_types` y `spa_types` son catálogos maestros
> globales puros —sin `company_id`— y toda su escritura está cerrada a `hasRole('SYSTEM')`;
> `surgery_types`, `diagnostic_imaging_types` y `laboratory_test_types` sí resolvieron el mismo
> problema con el par `company_id` + `general`.
> **Impacto:** una clínica con una especialidad propia («Consulta de exóticos», «Baño de gatos sin
> agua a domicilio») no puede registrarla, y el único camino es pedirle a la plataforma que la
> añada **para todos los tenants**. A medida que entren clínicas, o el catálogo global se llena de
> filas que solo le sirven a una —y le ensucia el desplegable a las demás—, o el tenant deja de
> usar el catálogo. Es la misma decisión de modelado que `surgery_types` ya tomó al revés.
> **Arreglo:** replicar el patrón de `057` en las dos tablas (`company_id BIGINT NULL` FK +
> `general BOOLEAN NOT NULL DEFAULT FALSE`, backfill `general = TRUE` para lo existente) y añadir
> el `listAvailable(companyId)` hermano. **No bloquea la semilla**: al contrario, la semilla es lo
> que compra tiempo. Va como issue propio.

> **[Menor] M-1 — Sin semilla, la fila de escape no existe y el usuario no tiene dónde registrar lo no previsto**
> **Criterio:** decisión de §1.5, no un defecto del código.
> **Impacto:** mientras B-1 siga abierto no hay nada que registrar, así que este punto solo cuenta
> **después** de sembrar: si «Otro» pasa del 5 % de los registros de una tabla, falta una fila.
> **Arreglo:** medición periódica, no cambio de esquema.
> **Verificación:** `SELECT ct.name, COUNT(*) FROM consultations c JOIN consultation_types ct ON
> ct.id = c.consultation_type_id GROUP BY 1 ORDER BY 2 DESC;`

> **[Nota] N-1 — El compose local corre MySQL 8.0.45 y RDS corre 8.4**
> `docker-compose.yml:79` frente a la familia `mysql8.4` de RDS y `mysql:8.4` de Testcontainers.
> **Impacto:** para esta semilla es inocuo —son `INSERT` sin sintaxis dependiente de versión—,
> pero cualquier medición de plan hecha en el compose local no representa a RDS. Se anota aquí
> porque es el entorno donde `db-migrations` probará el changeset.

---

## 8. Fuentes

Todas consultadas el **2026-08-25**. Las marcadas como PDF se leyeron extrayendo el texto del
documento, no la página que lo enlaza.

| Fuente | URL | Qué sostiene |
|---|---|---|
| ACVS — Small Animal Surgical Procedures List (agosto 2026), PDF, 15 páginas | https://www.acvs.org/wp-content/uploads/2025/08/ACVS_Small_Animal_Surgical_Procedures_List_for_Website.pdf | La existencia y el nombre canónico de los procedimientos del §3: `Tibial plateau leveling osteotomy` (TPLO), `Tibial tuberosity advancement` (TTA), `Trochleoplasty`, `Femoral head and neck ostectomy`, `Total ear canal ablation/bulla osteotomy`, `Staphylectomy`, `Arytenoid lateralization`, `Ovariohysterectomy for pyometra`, `Cesarean section`, `Abdominal cryptorchid orchiectomy`, `Urethrostomy (perineal, prescrotal, scrotal, prepubic, transpelvic)`, `Gastropexy`, `Intestinal resection and anastomosis`, `Splenectomy`, `Partial liver lobectomy`, `Portosystemic shunt ligation`, `Anal sacculectomy`, `Radical/Regional mastectomy`, `Enucleation`, `Hemilaminectomy`, y toda la sección de laparoscopia/toracoscopia/artroscopia |
| ACVS — página de requisitos de residencia (índice del PDF anterior) | https://www.acvs.org/certification/residency-requirements/surgical-procedures-lists/ | Que la lista es el estándar de procedimientos del colegio de cirujanos y su fecha de actualización |
| Hospital Veterinario, Facultad de Medicina Veterinaria y de Zootecnia, **Universidad Nacional de Colombia** — lista de precios de la Clínica de Pequeños Animales, PDF, 10 páginas | https://medicinaveterinariaydezootecnia.bogota.unal.edu.co/media/attachments/2022/05/23/lista-de-precios-cpa.pdf | **La nomenclatura en español y en uso en Colombia.** Sección C1 (Clínica externa) sostiene los tipos de consulta 1-5 y 22-24: «Consulta programada y carné por primera vez», «Consulta especializada (cardiología, homeopatía, ortopedia)», «Consulta oncológica», «Consulta de emergencia», «Consulta prioritaria», «Control médico», «Control posquirúrgico», «Consulta paciente remitido por un Médico Veterinario para Especialista», «Certificación de salud y vacunación», sección C4 «Eutanasia». Secciones C8.1-C8.5 sostienen los nombres quirúrgicos en español: «Ovariectomía / ovariohisterectomía electiva», «Orquiectomía», «Cesárea», «Piómetra», «Prolapso vaginal», «Gastrotomía», «Enterotomía», «Enteroanastomósis», «Esplenectomía», «Cistotomía», «Uretrotomía – uretrostomía», «Herniorrafía (umbilical, inguinal, perineal, diafragmática)», «Mastectomia», «Dilatación / vólvulus gástrico», «Gastropexia», «Ablación total de conducto auditivo externo», «Otohematoma», «Ablación del paladar blando elongado (estafilectomía)», «Corrección de narinas estenóticas», «Lateralización del cartílago aritenoides (parálisis laríngea)», «Osteosíntesis», «Fijación externa», «Osteotomías correctivas», «Reducción luxación rotula», «Amputación de cabeza femoral», «Artrodesis», «Reemplazo total de cadera», «Ruptura de ligamento cruzado craneal (técnica tradicional extracapsular)» y «(TTA - avance de la tuberosidad de la tibia)», «Laminectomía», «Craneotomía», «Profilaxis dental», «Extracción de dientes», «Entropión», «Ectropión», «Prolapso glándula de Harder ó protrusión del tercer párpado», «Flap conjuntival», «Cataratas (facoemulsificación)», «Artroscopia». **Y también sostiene §1.1**: los precios se desglosan por peso dentro de un mismo procedimiento |
| AVDC — nomenclatura oficial de odontología veterinaria | https://avdc.org/avdc-nomenclature/ | Las filas 63 y 66: «professional dental cleaning (PRO): scaling (supragingival and subgingival plaque and calculus removal) and polishing of the teeth with power/hand instrumentation performed by a trained veterinary health care provider **under general anesthesia**», y que «periodontal therapy» es una categoría que suma alisado radicular, curetaje, colgajos, gingivectomía y antisépticos locales |
| AAHA — 2019 Canine Life Stage Guidelines y 2023 Senior Care Guidelines | https://www.aaha.org/wp-content/uploads/globalassets/02-guidelines/canine-life-stage-2019/2019-aaha-canine-life-stage-guidelines-final.pdf · https://www.aaha.org/wp-content/uploads/globalassets/02-guidelines/2023-aaha-senior-care-guidelines-for-dogs-and-cats/resources/2023-aaha-senior-care-guidelines-for-dogs-and-cats.pdf | Las filas 7, 8 y 9 de consulta: la definición de «senior» como el último 25 % de la esperanza de vida y la recomendación de **examen al menos dos veces al año**, y el contenido del chequeo preventivo por etapa (entorno, comportamiento, nutrición, parásitos, vacunación, odontología, zoonosis, seguridad y reproducción) |
| WSAVA — Nutritional Assessment Guidelines (la nutrición como «quinta constante vital») | https://wsava.org/wp-content/uploads/2020/01/WSAVA-Nutrition-Assessment-Guidelines-2011-JSAP.pdf | La fila 20 (Consulta de nutrición): que la valoración nutricional y una recomendación dietética específica son parte del estándar de atención, con condición corporal y masa muscular como ejes |
| ICA — ingreso y salida de animales de compañía (perros y gatos) | https://www.ica.gov.co/importacion-y-exportacion/otros-procedimientos/requisitos-para-importar-mascotas.aspx | La fila 23 (Certificado de salud y viaje): que el trámite exige **certificado de salud expedido por un médico veterinario en los 3 días previos al embarque**, tratamiento antiparasitario interno y externo en los 60 días previos e identificación por microchip de 15 dígitos, y que el CIS lo emite el ICA. Es lo que hace de esta consulta un tipo propio y no un chequeo cualquiera |
| Centro Veterinario de Comportamiento Animal (Bogotá) | https://www.comportamientoanimal.com.co/servicios/consulta-de-etologia-clinica-para-perros-y-gatos/ | La fila 19: que «etología clínica, también llamada medicina del comportamiento», es una consulta que se ofrece como tal en Colombia, y que la practican veterinarios especializados |
| AniCura — consideraciones clínicas de las úlceras recurrentes (SCCED) · ConsultaVet | https://www.anicura.es/referencia-veterinaria/noticias/consideraciones-clinicas-de-las-ulceras-recurrentes-scced-en-el-perro-y-en-el-gato/ · https://www.consultavet.org/articulo-tratamiento-de-ulceras-superficiales-cronicas-efectividad-del-desbridamiento-epitelial-fresado-corneal-y-queratotomia-en-rejilla-1611 | Las filas 71 y 72: que el tratamiento quirúrgico de la úlcera indolente es el **desbridamiento epitelial** más **queratotomía en rejilla o puntiforme**, y que el recubrimiento conjuntival cubre y nutre la úlcera profunda |
| Bettasspa (Medellín) — precios del servicio | https://bettasspa.com.co/precios-del-servicio | Las filas 1, 6, 9, 15, 16, 18, 20, 21, 22, 29 de spa, con el nombre del gremio tal cual: «Baño y secado», «Corte y limado de uñas», «Limpieza de oídos», «Corte higiénico», «Pulida de corte», «Arreglo de patas», «Deslanado», «Rescate de manto» («soltar cada nudo con un excelente desenredante»), «Colorimetría», «Baño en seco» para gatos |
| Dogs & Love (Bogotá) — spa y peluquería canina | https://www.dogsandlove.co/spa-peluqueria-canina | Las filas 2, 3, 8, 19, 23, 25, 26, 28: «Peluquería por raza/corte higiénico y/o deslanado», «Corte y limado de uñas», «Hidratación de nariz y huellitas», «Limpieza de oídos», «Cepillado de dientes», «Aromaterapia», «Baño Antipulgas», «Masaje capilar con mascarilla frutal», «Desenredo y/o deslanado», «Baño Medicado», «primer baño para cachorros» |
| Don Bigotes Vet (Colombia) — peluquería canina | https://www.donbigotes.co/servicios/peluqueria-canina/ | Las filas 2, 3, 4, 5: «Baño Estándar», «Baño Especializado» (realce de color: blanco, negro, dorado o marrón), «Baño Medicado» (champú de avena para piel sensible), «Baño Insecticida», y que limado de uñas, limpieza de oídos, cepillado dental, deslanado e hidratación de trufa y almohadillas van incluidos en el servicio |
| Nubika — el baño de ozono para perros · Rebeca Cuevas — spa canino de ozono | https://nubika.es/noticias/bano-ozono-perros/ · https://rebecacuevas.com/ozonoterapia-y-spa-canino/ | La fila 7: que el baño de ozono es un servicio real de peluquería profesional, hecho con microburbujas generadas por ozonizador en la tina, y qué se le atribuye (limpieza profunda, control del olor, desinfección de la piel) |
| Style and Dog — el grooming en perros y gatos · Peluquería Vivancos — técnicas de peluquería canina | https://www.styleanddog.com/noticias/el-grooming-en-perros-y-gatos · https://peluqueriacaninavivancos.com/tecnicas-de-peluqueria-canina/ | Las filas 10, 11, 17: el vocabulario del gremio — «clipperwork» es el esquilado a máquina al ras, «stripping» el arrancado a navaja en razas de pelo duro «con el fin de renovar el pelo», «trimming» el recorte y ajuste del manto |
| Inteligencia Canina — grooming por raza · ISED — cortes especiales para cada raza | https://www.inteligenciacanina.com/blog/grooming-de-las-7-razas-de-perros-mas-populares-corte-cuidados-y-mas · https://www.ised.es/articulo/veterinaria/el-arte-de-la-peluqueria-canina-cortes-especiales-para-cada-raza/ | Las filas 12, 13, 14: que los cortes con nombre propio por raza existen y cuáles son —continental y león en poodle, corte de cachorro «recortar todo el pelaje a una longitud uniforme», patrón de terrier/schnauzer con lomo apurado y faldón, cejas y barba conservados |
| MySQL 8.4 — operaciones DDL online de InnoDB | https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html | §5.5 (que la semilla no es DDL y no aplica) y el coste `ALGORITHM=COPY` del `ADD CHECK` del hallazgo G-3 |
| MySQL 8.4 — columnas generadas | https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html | El arreglo propuesto en G-1: es el patrón que el repo ya usa en `226`, `210` y `206` para emular el índice único parcial que MySQL no tiene |
| Bill Karwin, *SQL Antipatterns* — Metadata Tribbles | https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/ | §1.1: por qué no se desglosa `surgery_types` por especie ni por tramo de peso |
| Citus — multi-tenant con discriminador de tenant | https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html | G-1 y G-4: la doctrina de que la clave natural de un sistema multi-tenant es única **por tenant**, no globalmente |

**Fuentes que no se pudieron leer y por qué:** `dev.mysql.com` devuelve 403 a `curl` (se leyó con
`WebFetch`, que sí funciona); los dos PDF de ACVS y de la Universidad Nacional no son legibles por
`WebFetch` —devuelve el binario— y se extrajeron con `pypdf`; `depelos.co` devolvió 403 y no se
usó; la página de telemedicina de la AVMA devolvió cuerpo vacío, así que **la fila 25
(Teleconsulta) se apoya solo en la práctica del sector y queda pendiente de respaldo con la
definición formal de la AVMA o del COMVEZCOL**.

---

## 9. Qué NO se comprobó

- **No se consultó ninguna base de datos**, ni la local ni la de dev. Todo lo de este documento se
  verificó leyendo changesets, entidades, dominios y puertos. Los conteos de §5.6 son la
  verificación **posterior**, para quien aplique el changeset.
- **No se comprobó si dev o prod ya tienen filas** en estas tablas creadas a mano. El `WHERE NOT
  EXISTS` de §5.2 lo hace irrelevante para la corrección, pero significa que el conteo esperado
  (27/32/81) puede quedarse corto si alguien sembró antes por fuera de Liquibase.
- **No se revisó el front.** Si `VetSoftwarePublicFront` tiene alguna lista de tipos hardcodeada
  que haya que retirar cuando el catálogo se llene, este documento no lo sabe.
- **No se ejecutó ningún test ni ningún build.** No hacía falta: la especificación no toca código.

---

## 10. Trazabilidad — dónde vive cada hallazgo

| Hallazgo | Issue | Estado |
|---|---|---|
| B-1 — los tres catálogos vacíos | [#564](https://github.com/kefaroTech/vetsoftware-backend/issues/564) | abierto, con esta especificación como insumo |
| B-2 — `general = true` inalcanzable por la API | [#565](https://github.com/kefaroTech/vetsoftware-backend/issues/565) | abierto |
| G-4 — `consultation_types` y `spa_types` sin `company_id` | [#566](https://github.com/kefaroTech/vetsoftware-backend/issues/566) | abierto |
| G-1 — `UNIQUE` global de `name` | [#556](https://github.com/kefaroTech/vetsoftware-backend/issues/556) | **ya abierto** por otro agente; cubre `surgery_types` entre sus cuatro tablas. Comentado con la dimensión de la semilla y de la collation, no duplicado. Gemelo por tabla: [#557](https://github.com/kefaroTech/vetsoftware-backend/issues/557) |
| G-3 — XOR `general`/`company_id` sin `CHECK` | [#560](https://github.com/kefaroTech/vetsoftware-backend/issues/560) | **ya abierto** para `vaccination_types`. Comentado con el caso idéntico de `surgery_types` y su DDL, no duplicado |
| G-2 — `description` `NOT NULL` sin validación en el dominio | [#483](https://github.com/kefaroTech/vetsoftware-backend/issues/483) | **ya abierto** para `spa_types`. Comentado con el caso idéntico de `surgery_types`, no duplicado |
| Borrado lógico que quema el nombre en `spa_types` | [#482](https://github.com/kefaroTech/vetsoftware-backend/issues/482) | ya abierto; aplica igual a los tres catálogos |
| N-1 — compose local en MySQL 8.0.45 frente a RDS 8.4 | — | anotado aquí; inocuo para esta semilla |
