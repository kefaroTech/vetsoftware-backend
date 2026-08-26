# Semilla del catálogo `vaccination_types`

**Especificación para `db-migrations`.** Este documento **no** es un changeset y no autoriza a escribir
uno sin leer antes la §6, que contiene un defecto de esquema abierto cuyo alcance crece con cada fila
que se siembre.

- **Autor:** agente de modelado de datos (`db-schema`)
- **Fecha:** 2026-08-25
- **Alcance:** tabla `vaccination_types` (una tabla, un catálogo). No toca `vaccinations`.
- **Estado del catálogo hoy:** **vacío**. Solo hay DDL. Verificado en §1.

---

## 0. Resumen ejecutable

| Pregunta | Respuesta |
|---|---|
| ¿Cuántas filas se proponen sembrar **ahora**? | **12** (7 obligatorias + 5 recomendadas). Mínimo defendible: **7** |
| ¿Cuántas quedan documentadas para después? | **21** más, ya investigadas y redactadas (§4), + **6** descartadas a propósito (§5) |
| ¿Marcas comerciales? | **No.** El catálogo es de **biológicos**, no de productos. Razonado en §3 |
| ¿`company_id` / `general` de las filas semilla? | `company_id = NULL`, `general = TRUE`. Obligatorio y **no tiene default correcto** (§7.2) |
| ¿Bloqueante antes de sembrar? | Sí: `name` es `UNIQUE` **global**, no `(company_id, name)`. §6 |

---

## 1. Punto de partida — qué existe hoy, verificado contra el árbol

Censo hecho sobre `VetSoftware/src/main/resources/db/changelog/migrations/` (276 ficheros XML).
Changesets que tocan la tabla, en orden:

| Changeset | Qué hace |
|---|---|
| `034_create_vaccination_types.xml:8-22` | `CREATE TABLE`: `id`, `name VARCHAR(100) NOT NULL UNIQUE`, `description VARCHAR(500) NOT NULL`, `created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `055_alter_vaccination_types_add_company_and_general.xml:8-21` | `company_id BIGINT NULL` + FK `fk_vaccination_types_company → companies(id)` |
| `055_…:23-32` | `general BOOLEAN NOT NULL DEFAULT FALSE` |
| `055_…:34-43` | Backfill: `general = TRUE` donde `general = FALSE AND company_id IS NULL` |
| `068_add_enabled_to_all_tables.xml:310-316` | `enabled BOOLEAN NOT NULL DEFAULT TRUE` |
| `225_add_version_optimistic_lock_wave2.xml:513-523` | `version BIGINT NOT NULL DEFAULT 0` |

**Ningún `<insert>` en ningún changeset toca `vaccination_types`.** Los únicos ficheros del árbol con
`<insert>` o `loadData` son `148`, `173`, `212`, `215`, `255` y `284`. El catálogo se despliega vacío.

### 1.1 Qué se rompe con el catálogo vacío

`035_create_vaccinations.xml:15-18`:

```xml
<column name="vaccination_type_id" type="BIGINT">
    <constraints nullable="false" foreignKeyName="fk_vaccinations_vaccination_type"
                 references="vaccination_types(id)"/>
</column>
```

`vaccination_type_id` es **`NOT NULL`**. Con el catálogo vacío y sin filas propias, una clínica recién
dada de alta **no puede registrar ni una sola vacunación**: no hay ningún id válido que poner en el FK.
Esto no es una carencia de comodidad, es una funcionalidad clínica inalcanzable hasta que alguien cree
tipos a mano, uno por uno, en cada tenant.

### 1.2 Reglas del dominio que la semilla debe respetar

`VaccinationType.java:42-54` valida en el constructor:

```java
if (general && company != null)  throw new IllegalArgumentException("general type cannot have company");
if (!general && company == null) throw new IllegalArgumentException("non-general type requires company");
```

Es un **XOR**: o `general = TRUE` con `company_id IS NULL`, o `general = FALSE` con `company_id` puesto.
La validación vive **solo en Java** y el constructor se ejecuta al **mapear cada fila leída**
(`JpaVaccinationTypeRepository.java:35`, `:40`, `:45`, `:55` → `mapper::toDomain`). Consecuencia
directa y poco intuitiva: **una fila semilla mal formada no falla al insertarse, falla al leerse**, y
revienta el listado entero de tipos de vacuna de todos los tenants, no solo esa fila. Ver §7.2.

### 1.3 Cómo lee cada tenant este catálogo

`VaccinationTypeJpaRepository.java:33-34`:

```java
List<VaccinationTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);
```

Cada empresa ve **las filas globales más las suyas**. El aislamiento entre clínicas está correcto en
las lecturas y las escrituras (`findByIdAndCompany_Id` es el finder estricto de los caminos de
escritura, documentado en `VaccinationTypeJpaRepository.java:36-44`, y `DeleteVaccinationTypeService`
lo usa para impedir que un tenant borre una fila general). **No hay fuga de tenant en este slice.**
El problema no es de aislamiento de lectura: es del **espacio de nombres**, §6.

---

## 2. Las invariantes de este catálogo

Se escriben antes que las filas, porque una fila mal elegida se corrige y una invariante mal puesta se
paga durante años.

| # | Invariante de negocio | Dónde vive hoy | Veredicto |
|---|---|---|---|
| I1 | Un tipo de vacuna es global **o** de una empresa, nunca las dos ni ninguna | Solo en Java (`VaccinationType.java:50-53`) | **No garantizada en base.** Un `INSERT` directo (incluido un changeset) puede crear la fila inválida. Debería ser un `CHECK` (§7.5) |
| I2 | Dos tipos activos de la misma empresa no pueden llamarse igual | `UNIQUE (name)` global — **más estricto de lo que la invariante pide, y por eso rompe** | **Mal modelada.** §6 |
| I3 | Un tipo dado de baja no debe seguir ocupando el nombre | Nada | **No garantizada.** Mismo defecto de familia que #482, #432 y #433 |
| I4 | La descripción nunca es vacía | `NOT NULL` en base + `VaccinationType` no valida vacío | Parcial: `NOT NULL` impide `NULL`, no impide `''`. Un `CHECK (description <> '')` lo cerraría; es menor |
| I5 | No se puede borrar un tipo con vacunaciones activas | `DeleteVaccinationTypeService.java:36-38` + FK `fk_vaccinations_vaccination_type` | **Garantizada.** El FK es la red bajo el check de Java |

Para la semilla, la que manda es **I1**: si el changeset la incumple, no rompe una fila, rompe el
listado de todos los tenants (§1.2).

---

## 3. La decisión de modelado: «tipo de vacuna» ≠ «producto comercial»

Es la decisión más cara de revertir de todo el documento, porque afecta al **contenido** de una columna
con datos dentro, y eso no tiene arreglo barato.

### 3.1 La decisión

**El catálogo lleva el biológico —la valencia y los antígenos que contiene—, nunca la marca.**
`Nobivac`, `Vanguard`, `Defensor`, `Eurican`, `Purevax`, `Recombitek`, `Bioquiral`, `Vecol` y cualquier
otro nombre de fabricante **quedan fuera** de `vaccination_types`.

### 3.2 Por qué, con el código delante

1. **El esquema ya guarda el producto en otro sitio, y no aquí.** `035_create_vaccinations.xml:19-21`
   declara `lot VARCHAR(100) NOT NULL` en la tabla `vaccinations`, y `166_add_route_and_site_to_vaccinations.xml`
   añadió vía y sitio de aplicación. Es decir: **la identidad comercial del frasco concreto —lote, vía,
   sitio— ya se registra por acto clínico, que es donde tiene sentido**. Duplicar la marca en el
   catálogo sería normalizar mal: el mismo dato en dos sitios con dos ciclos de vida distintos.
2. **La marca caduca; el biológico no.** El registro ICA de un producto se vence, se cede, se cancela o
   el laboratorio retira el portafolio del país. La *rabia inactivada* seguirá siendo rabia inactivada.
   Un catálogo de marcas obliga a un `UPDATE` de datos maestros cada vez que cambia un portafolio
   comercial; un catálogo de biológicos no se toca casi nunca.
3. **El `UNIQUE` global multiplica el coste del error.** Con marcas, el catálogo tendría cientos de
   filas en vez de decenas, y cada una **confisca un nombre a todos los tenants para siempre** (§6).
   Sembrar marcas convierte un defecto acotado en un problema estructural.
4. **Una historia clínica con marca envejece peor que con biológico.** Dentro de diez años, «Antirrábica»
   seguirá diciendo qué se aplicó; «Defensor 3» exige saber qué contenía ese producto en 2026 y en qué
   país. El registro clínico debe ser legible sin un catálogo comercial externo.
5. **Precedente de la casa.** `212_create_petshop_catalog.xml:8-27` siembra `unit_measure_catalog` con
   el código estándar (`94 / Unidad`), no con una nomenclatura de proveedor, y deja escrito en el
   comentario que el conjunto completo se sincroniza después con la fuente autoritativa. Mismo criterio.

### 3.3 Lo que sí se admite en el nombre, y lo que no

| Admitido | Ejemplo | Por qué |
|---|---|---|
| Especie | `Antirrábica canina y felina` | Es información de dominio: cambia la indicación y a veces el biológico |
| Valencia / antígenos | `Polivalente canina DHPP: moquillo, hepatitis, parvovirus y parainfluenza` | Es lo que de verdad identifica la vacuna |
| Cepa cuando la fija una norma | `Encefalitis equina venezolana (EEV)`, cepa TC-83 en la descripción | La Resolución ICA 6646 de 2017 art. 4 impone la cepa; forma parte del biológico oficial |
| **No** admitido | `Nobivac DHPPi`, `Defensor 3`, `Vanguard Plus 5 L4` | Marca |
| **No** admitido como nombre canónico | `Séxtuple`, `Óctuple`, `Décuple` | Ver §3.4 |

### 3.4 El caso «séxtuple»: por qué la valencia coloquial va en la descripción y no en el nombre

`Quíntuple`, `séxtuple`, `óctuple` y `décuple` **no son nomenclatura técnica: son etiquetas de
mercadeo** y cada laboratorio cuenta distinto. Un producto DHPPi + 2 serovares de *Leptospira* se
vende como «séxtuple» en un portafolio y como «óctuple» en otro que cuenta cada serovar por separado.
Si el nombre canónico es «Séxtuple canina», dos clínicas registrarán cosas distintas bajo la misma
fila y la historia clínica deja de ser comparable.

**Decisión:** el `name` enumera los antígenos en español; el término coloquial va en la `description`,
que es texto libre y **no** identidad. Así el vet colombiano reconoce la fila («…se conoce como
séxtuple») sin que el sistema adopte una nomenclatura ambigua como clave.

---

## 4. El catálogo

Formato de cada fila: `name` (≤100) y `description` (≤500, `NOT NULL`). Todas con
`company_id = NULL`, `general = TRUE`, `enabled = TRUE`, `version = 0`.

Columna **Nivel**:

- **N1** — sembrar en el changeset de esta especificación. Sin ellas hay funcionalidad clínica rota (§1.1).
- **N2** — sembrar en el mismo changeset; ancladas en norma colombiana y de nombre nacional estable.
- **N3** — investigadas y redactadas, **no** sembrar todavía: esperar a que `name` deje de ser
  `UNIQUE` global (§6). Su nombre está fijado aquí para que nadie lo reinvente.

### 4.1 Caninos

| Nivel | `name` | `description` | Clasificación de guía |
|---|---|---|---|
| **N1** | `Antirrábica canina y felina` | Virus de la rabia inactivado, aplicable a perros y gatos. Previene la rabia, encefalitis de curso mortal y zoonosis transmisible al ser humano. Vacuna núcleo (core) para ambas especies según WSAVA y AAHA/AAFP, y exigida en Colombia dentro del control oficial de zoonosis (Ley 9 de 1979 y Decreto 2257 de 1986). | Core WSAVA / obligatoria CO |
| **N1** | `Polivalente canina DHPP: moquillo, hepatitis, parvovirus y parainfluenza` | Vacuna combinada para perros. Previene el moquillo (virus del distemper canino), la hepatitis infecciosa canina (adenovirus canino, formulada con cepa CAV-2), la parvovirosis (parvovirus canino tipo 2) y la parainfluenza canina. Reúne las tres vacunas núcleo (core) del perro según WSAVA. En el mercado colombiano se conoce como quíntuple. | Core WSAVA (CDV, CAV, CPV-2) |
| **N1** | `Polivalente canina DHPP-L: moquillo, hepatitis, parvovirus, parainfluenza y leptospirosis` | Vacuna combinada para perros. Previene el moquillo, la hepatitis infecciosa canina (adenovirus canino), la parvovirosis, la parainfluenza canina y la leptospirosis (serovares de Leptospira interrogans). Las cuatro primeras valencias son las mismas de la polivalente DHPP; la fracción de Leptospira es no núcleo (non-core) y se indica por riesgo de exposición. En el mercado colombiano se conoce como séxtuple. | Core + non-core WSAVA |
| **N1** | `Leptospirosis canina` | Bacterina para perros contra serovares de Leptospira interrogans (canicola, icterohaemorrhagiae, grippotyphosa y pomona, según el producto). Previene la leptospirosis canina, de curso hepático y renal, y zoonosis de importancia en Colombia. Vacuna no núcleo (non-core) según WSAVA: se indica por riesgo de exposición a agua estancada, roedores o inundaciones. | Non-core WSAVA |
| **N1** | `Tos de las perreras (Bordetella bronchiseptica)` | Vacuna para perros contra Bordetella bronchiseptica, sola o combinada con parainfluenza canina, en presentación intranasal, oral o inyectable. Previene la traqueobronquitis infecciosa canina, conocida como tos de las perreras. Vacuna no núcleo (non-core): se indica en perros con guardería, peluquería, criadero, albergue o exposiciones. | Non-core WSAVA |
| N3 | `Influenza canina (H3N8 y H3N2)` | Vacuna para perros contra los virus de la influenza canina H3N8 y H3N2. Previene la influenza canina, de curso respiratorio agudo. Vacuna no núcleo (non-core), indicada solo donde la enfermedad esté presente y en perros con alta vida social. No se ha verificado que exista un producto con registro ICA vigente en Colombia. | Non-core WSAVA |

### 4.2 Felinos

| Nivel | `name` | `description` | Clasificación de guía |
|---|---|---|---|
| **N1** | `Trivalente felina FVRCP: panleucopenia, rinotraqueítis y calicivirus` | Vacuna combinada para gatos. Previene la panleucopenia felina (parvovirus felino), la rinotraqueítis viral felina (herpesvirus felino tipo 1) y la calicivirosis felina (calicivirus felino). Son las tres vacunas núcleo (core) del gato según WSAVA y las guías AAHA/AAFP. En el mercado colombiano se conoce como triple felina. | Core WSAVA y AAHA/AAFP |
| **N1** | `Leucemia felina (FeLV)` | Vacuna para gatos contra el virus de la leucemia felina, retrovirus que causa inmunosupresión, anemia y linfoma. Núcleo (core) en gatos menores de un año según AAHA/AAFP y WSAVA; en el gato adulto es no núcleo (non-core) y se indica con acceso al exterior o convivencia con gatos de estado desconocido. Exige prueba previa de FeLV. | Core <1 año / non-core adulto |
| N3 | `Polivalente felina FVRCP + Chlamydia felis` | Vacuna combinada para gatos. Añade a la trivalente felina (panleucopenia, rinotraqueítis y calicivirosis) la fracción contra Chlamydia felis, causa de conjuntivitis crónica. La fracción de Chlamydia es no núcleo (non-core) y se indica en poblaciones con conjuntivitis confirmada, sobre todo en colonias y refugios. En el mercado se conoce como cuádruple felina. | Core + non-core |
| N3 | `Bordetelosis felina (Bordetella bronchiseptica)` | Vacuna intranasal para gatos contra Bordetella bronchiseptica. Previene la enfermedad respiratoria alta por bordetelosis felina. Vacuna no núcleo (non-core) de uso muy limitado: se reserva a poblaciones de refugio o criadero con enfermedad respiratoria confirmada, no a gatos de hogar. | Non-core |

La antirrábica del gato **no es una fila aparte**: el biológico es el mismo virus rabia inactivado y los
productos registrados suelen amparar ambas especies. Está en la fila `Antirrábica canina y felina`
(§4.1). Es un ejemplo directo de la decisión de §3: se modela el biológico, no la presentación.

### 4.3 Equinos

| Nivel | `name` | `description` | Clasificación |
|---|---|---|---|
| **N2** | `Toxoide tetánico equino` | Toxoide de Clostridium tetani para equinos. Previene el tétanos, enfermedad neurológica de letalidad muy alta en el caballo, que se adquiere por heridas punzantes, por el ombligo del potro y por la castración. Vacuna núcleo (core) de la AAEP: todo équido debe recibirla con independencia de su uso, su valor o su ubicación. | Core AAEP |
| **N2** | `Encefalitis equina venezolana (EEV)` | Vacuna de virus vivo atenuado, cepa TC-83, para caballares, mulares y asnales. Previene la encefalitis equina venezolana, de curso nervioso y mortal, y zoonosis. En Colombia es enfermedad de control oficial y su aplicación es obligatoria por debajo de los 1.500 m s. n. m. y para todo équido que asista a eventos de concentración animal (Resolución ICA 6646 de 2017). | Obligatoria CO |
| N3 | `Encefalitis equina del Este y del Oeste (EEE/WEE)` | Vacuna inactivada para equinos contra los virus de la encefalomielitis equina del Este y del Oeste. Previene encefalitis de curso nervioso transmitidas por mosquitos, con letalidad muy alta en el caso del Este. Vacunas núcleo (core) de la AAEP. En Colombia se han atendido brotes de encefalitis equina del Este; suele aplicarse en presentación combinada con la venezolana. | Core AAEP |
| N3 | `Influenza equina` | Vacuna para equinos contra el virus de la influenza equina (subtipo H3N8). Previene la influenza equina, de curso respiratorio y alta difusión en poblaciones agrupadas. Vacuna basada en riesgo (non-core) de la AAEP: se indica en caballos de deporte, de trabajo, de transporte frecuente o que asisten a concentraciones. | Non-core AAEP |
| N3 | `Rinoneumonitis equina (herpesvirus equino tipos 1 y 4)` | Vacuna para equinos contra los herpesvirus equinos tipo 1 y tipo 4. Previene la rinoneumonitis equina, de curso respiratorio, y el aborto por herpesvirus tipo 1 en la yegua gestante. Vacuna basada en riesgo (non-core) de la AAEP: prioritaria en yeguas de cría y en caballos jóvenes agrupados. | Non-core AAEP |
| N3 | `Antirrábica equina` | Virus de la rabia inactivado para equinos. Previene la rabia equina, que en Colombia se presenta sobre todo como rabia de origen silvestre transmitida por el murciélago hematófago Desmodus rotundus. Vacuna núcleo (core) de la AAEP y de interés en salud pública por ser zoonosis mortal. | Core AAEP |

### 4.4 Bovinos

| Nivel | `name` | `description` | Clasificación |
|---|---|---|---|
| **N2** | `Fiebre aftosa bovina` | Vacuna oleosa inactivada contra el virus de la fiebre aftosa, para bovinos y bufalinos. Previene la fiebre aftosa, enfermedad vesicular de difusión muy rápida y alto impacto económico. En Colombia la vacunación es obligatoria y se ejecuta en dos ciclos anuales coordinados por el ICA; es la condición que sostiene el estatus de país libre de fiebre aftosa con vacunación. | Obligatoria CO |
| **N2** | `Brucelosis bovina` | Vacuna de Brucella abortus, cepa 19 o cepa RB51, para bovinos. Previene la brucelosis bovina, causa de aborto y de pérdida reproductiva, y zoonosis de riesgo profesional. En Colombia su aplicación es obligatoria en las terneras dentro de la franja de edad definida por el ICA y se ejecuta junto con los ciclos oficiales de vacunación. | Obligatoria CO |
| N3 | `Rabia bovina de origen silvestre` | Virus de la rabia inactivado para bovinos y bufalinos. Previene la rabia de origen silvestre o rabia paresiante, transmitida por el murciélago hematófago Desmodus rotundus, de curso nervioso y siempre mortal. En Colombia se aplica en las zonas de riesgo dentro de los ciclos oficiales de vacunación del ICA. | Obligatoria en zona de riesgo |
| N3 | `Clostridiales bovinos: carbón sintomático, edema maligno y septicemia hemorrágica` | Bacterina para bovinos contra Clostridium chauvoei, Clostridium septicum y Pasteurella multocida. Previene el carbón sintomático, el edema maligno y la septicemia hemorrágica bovina, cuadros de curso agudo y mortalidad alta en animales jóvenes en pastoreo. En el mercado colombiano se conoce como triple bovina. | Electiva, uso extendido |
| N3 | `Carbón bacteridiano (ántrax, cepa Sterne)` | Vacuna viva de Bacillus anthracis, cepa Sterne 34F2, para bovinos y otros rumiantes. Previene el carbón bacteridiano o ántrax, de curso sobreagudo y zoonosis grave. Se aplica en predios y zonas con antecedente de la enfermedad, bajo criterio de la autoridad sanitaria. | Electiva por zona |
| N3 | `Complejo respiratorio bovino: IBR, DVB, PI3 y VRSB` | Vacuna combinada para bovinos contra rinotraqueítis infecciosa bovina (herpesvirus bovino tipo 1), diarrea viral bovina, parainfluenza tipo 3 y virus respiratorio sincitial bovino. Previene el complejo respiratorio bovino y, en el caso de la diarrea viral, la pérdida reproductiva y el ternero persistentemente infectado. | Electiva |
| N3 | `Leptospirosis bovina` | Bacterina para bovinos contra serovares de Leptospira (hardjo, pomona, canicola, icterohaemorrhagiae y grippotyphosa, según el producto). Previene la leptospirosis bovina, causa de aborto, infertilidad y caída de producción láctea, y zoonosis de riesgo profesional. | Electiva |

### 4.5 Aves

| Nivel | `name` | `description` | Clasificación |
|---|---|---|---|
| **N2** | `Enfermedad de Newcastle` | Vacuna para aves contra el virus de la enfermedad de Newcastle (paramixovirus aviar tipo 1), en presentación viva y en oleosa inactivada. Previene la enfermedad de Newcastle, de curso respiratorio, nervioso y digestivo. En Colombia es enfermedad de control oficial del ICA; el esquema depende del tipo de ave y del sistema de producción, y la vacunación no es universalmente obligatoria para toda ave. | Control oficial CO |
| N3 | `Bronquitis infecciosa aviar` | Vacuna para aves contra el coronavirus de la bronquitis infecciosa aviar. Previene la bronquitis infecciosa, de curso respiratorio, con daño renal y con caída y deformidad del huevo en ponedoras. El serotipo del producto debe corresponder al que circula en la zona. | Electiva |
| N3 | `Enfermedad de Gumboro (bursitis infecciosa aviar)` | Vacuna para aves contra el virus de la bursitis infecciosa aviar. Previene la enfermedad de Gumboro, que destruye la bolsa de Fabricio en el pollo joven y provoca inmunosupresión, con lo que hace fracasar las demás vacunaciones. | Electiva |
| N3 | `Enfermedad de Marek` | Vacuna para aves contra el herpesvirus de la enfermedad de Marek. Previene la enfermedad de Marek, linfoma de las aves con parálisis. Se aplica en la incubadora, al día de edad o en el huevo embrionado, nunca en el lote adulto. | Electiva |
| N3 | `Viruela aviar` | Vacuna viva para aves contra el virus de la viruela aviar. Previene la viruela aviar en sus formas cutánea y diftérica. Se aplica por punción en la membrana alar y es de uso frecuente en aves de traspatio, ornamentales y de riña. | Electiva |

### 4.6 Otras especies

| Nivel | `name` | `description` | Clasificación |
|---|---|---|---|
| N3 | `Peste porcina clásica` | Vacuna de virus vivo modificado de peste porcina clásica, cepa China adaptada en cultivo celular, para porcinos. Previene la peste porcina clásica, de curso hemorrágico y mortalidad muy alta. En Colombia la vacunación es obligatoria en las zonas definidas por el ICA y solo se admiten vacunas con registro ICA vigente. | Obligatoria por zona CO |
| N3 | `Triple porcina: parvovirosis, leptospirosis y erisipela` | Vacuna combinada para porcinos reproductores contra parvovirus porcino, serovares de Leptospira y Erysipelothrix rhusiopathiae. Previene la falla reproductiva por parvovirosis y leptospirosis y la erisipela porcina. Se aplica en cerdas y verracos de cría. | Electiva |
| N3 | `Neumonía enzoótica porcina (Mycoplasma hyopneumoniae)` | Bacterina para porcinos contra Mycoplasma hyopneumoniae. Previene la neumonía enzoótica porcina, de curso crónico, que deteriora la conversión alimenticia y abre la puerta al complejo respiratorio porcino. Se aplica en lechones. | Electiva |
| N3 | `Circovirus porcino tipo 2 (PCV2)` | Vacuna para porcinos contra el circovirus porcino tipo 2. Previene las enfermedades asociadas al circovirus, con desmedro, adelgazamiento progresivo y mortalidad en la etapa de levante. Se aplica en lechones. | Electiva |
| N3 | `Clostridiales de ovinos y caprinos (enterotoxemia)` | Bacterina toxoide para ovinos y caprinos contra clostridios, sobre todo Clostridium perfringens tipos C y D y Clostridium tetani. Previene la enterotoxemia y el tétanos, de curso sobreagudo en corderos y cabritos bien alimentados. | Electiva |

---

## 5. Lo que se deja fuera **a propósito**

Un catálogo clínico se juzga tanto por lo que no tiene. Cada exclusión, con su motivo:

| No incluida | Motivo | Fuente del criterio |
|---|---|---|
| Coronavirus canino (CCoV) | Clasificada como **no recomendada** por el VGG de la WSAVA: la enfermedad clínica atribuible es marginal y la vacuna no protege del cuadro relevante | WSAVA VGG |
| Giardiasis canina | **No recomendada** por el VGG; no previene la infección ni la eliminación de quistes | WSAVA VGG |
| Peritonitis infecciosa felina (PIF) | **No recomendada** por el VGG y por AAHA/AAFP | WSAVA VGG, AAHA/AAFP |
| Inmunodeficiencia felina (FIV) | **No recomendada** en la mayoría de mercados; interfiere con el diagnóstico serológico posterior, que es su daño principal | WSAVA VGG |
| Borreliosis de Lyme (Borrelia burgdorferi) | La enfermedad **no es endémica en Colombia**; sembrarla invita a registrar una vacunación que aquí no tiene indicación | AAHA (uso geográfico) |
| Influenza aviar | La vacunación está sujeta a política nacional y a autorización específica del ICA; **no se pudo verificar** el estado vigente. Sembrarla podría inducir a registrar una aplicación no permitida | Pendiente de verificar |
| Leishmaniosis canina | No se pudo verificar que exista producto con **registro ICA vigente** en Colombia | Pendiente de verificar |
| Mixomatosis y enfermedad hemorrágica del conejo | No se pudo verificar registro ICA vigente | Pendiente de verificar |

**Las tres últimas no son un «no»: son un «no verificado».** Para confirmarlas hay que descargar la
*Base de datos de medicamentos* del ICA (§9) y buscar el biológico. No lo hice: el listado se publica
como archivo descargable y esta especificación se cerró leyendo código y fuentes normativas.

---

## 6. El defecto de esquema que condiciona cuántas filas sembrar

> **[BLOQUEANTE para el volumen de la semilla]** `vaccination_types.name` tiene un `UNIQUE` **global**,
> no acotado por empresa ni por borrado lógico — `034_create_vaccination_types.xml:12-14`,
> `VaccinationTypeJpaEntity.java:18-19`
>
> **Criterio:** *SQL Antipatterns* — clave única cuyo alcance no coincide con el alcance de la
> invariante. Doctrina multi-tenant con discriminador: **el tenant va delante en toda clave**
> (Citus, *Multi-tenant applications*). Precedente **de la propia casa**:
> `151_product_unique_name_and_audit.xml`, `152`, `153` y `154` ya resolvieron exactamente esto para
> productos, categorías de producto, categorías de servicio e impuestos. Precedentes en issues:
> **#427** (cerrado, `client_request_id` único global bloqueaba al segundo tenant) y **#482** (abierto,
> `spa_types`, que explícitamente deja `vaccination_types` como sospecha sin comprobar).
>
> **Impacto — el escenario concreto:** el día que se siembre `Antirrábica canina y felina` como fila
> global, **ninguna de las N clínicas de la plataforma podrá volver a crear jamás un tipo con ese
> nombre**. Y no recibe una explicación: `CreateVaccinationTypeService.java:26-34` inserta sin ninguna
> guarda previa de nombre —a diferencia de producto, impuesto, categoría de servicio y proveedor, que
> sí tienen su `…NameAlreadyExistsException`—, así que el choque llega a
> `GlobalExceptionHandler.handleDataIntegrity` (`:1768`), no encuentra mapeo en `mapConstraint`
> (`:1804` en adelante: hay mapeo para citas, cuentas abiertas, configurator, listas de precios y
> productos; **ninguno para `vaccination_types`**) y sale por `:1796-1797` como
> **`409` con `detail` = «Database constraint violation»**, en inglés y sin nombrar el campo. Además
> deja un `log.warn` de integridad no mapeada que, según el comentario del propio handler
> (`:1786-1795`), está reservado para «la cola rara».
> Segunda cara del mismo defecto: como el `UNIQUE` tampoco excluye las filas dadas de baja, **dar de
> baja un tipo quema su nombre para siempre** — es el defecto de #482 confirmado aquí.
> **Blast radius:** una columna, pero la leen los 6 casos de uso del slice
> (`Create`/`Update`/`Find`/`List`/`ListAvailable`/`Delete`), el adaptador
> `JpaVaccinationTypeRepository` y el front del tenant.
>
> **Arreglo propuesto** — el patrón de la casa de `153_service_category_unique_name_and_audit.xml:12-27`,
> con la vuelta de tuerca que exige tener `company_id` **nullable**: `UNIQUE (company_id, active_name)`
> **no serviría**, porque MySQL considera distintos todos los `NULL` en un índice único y las filas
> globales no quedarían deduplicadas entre sí. Hace falta un discriminador de dueño sin `NULL`:
>
> ```sql
> ALTER TABLE vaccination_types
>     ADD COLUMN owner_id BIGINT
>         GENERATED ALWAYS AS (COALESCE(company_id, 0)) STORED,
>     ADD COLUMN active_name VARCHAR(100)
>         GENERATED ALWAYS AS (CASE WHEN enabled = TRUE THEN name ELSE NULL END) STORED,
>     ADD CONSTRAINT uq_vaccination_types_owner_active_name UNIQUE (owner_id, active_name),
>     DROP INDEX `name`;
> ```
>
> `owner_id = 0` identifica al «dueño plataforma». `active_name` vale `NULL` en las filas dadas de baja
> y MySQL admite múltiples `NULL`, con lo que la baja deja de quemar el nombre. Es el mismo mecanismo
> de columna generada `STORED` que ya usan los changesets `151`–`154`, `206`, `210` y `226`.
>
> **Cómo se verifica:** `SHOW CREATE TABLE vaccination_types` debe mostrar el índice nombrado; y dos
> `INSERT` con el mismo `name` y `company_id` distinto deben pasar los dos, mientras que dos con el
> mismo `company_id` deben fallar el segundo.

### 6.1 Detalles que se arrastran

- **El índice único de hoy no tiene nombre explícito.** `034` usa `unique="true"` en línea, sin
  `constraintName`. MySQL lo nombra a partir de la columna (`name`), lo que hace ilegible el log de
  integridad (`constraint=vaccination_types.name`) e impide referenciarlo con seguridad en un
  `<rollback>`. **No verificado contra base viva** (§10).
- **Collation.** Ningún `CREATE TABLE` del repositorio declara charset ni collation, así que la columna
  hereda la del servidor, que en MySQL 8.4 por defecto es `utf8mb4_0900_ai_ci`: **insensible a
  mayúsculas y a acentos**. Si se confirma, `Antirrábica`, `antirrabica` y `ANTIRRABICA` son la misma
  clave. Para la semilla es más bien bueno —evita casi-duplicados—, pero **empeora el defecto de §6**:
  la clínica que teclee «Antirrabica» sin tilde también recibe el 409 opaco. **No verificado** (§10).

### 6.2 Cuántas filas es prudente sembrar, a la luz de esto

Cada fila global **confisca un nombre a todos los tenants presentes y futuros**. Con eso sobre la mesa:

| Escenario | Filas | Veredicto |
|---|---|---|
| Sembrar las 33 investigadas | 33 | **No.** Confisca 33 nombres, casi todos de especies que la mayoría de clínicas de compañía no atiende. Coste alto, beneficio marginal |
| Sembrar 12 (N1 + N2) | **12** | **Recomendado.** Desbloquea la funcionalidad clínica rota de §1.1 y cubre lo obligatorio por norma colombiana, con nombres de estándar nacional que a nadie le conviene reescribir |
| Sembrar 7 (solo N1) | 7 | **Alternativa conservadora defendible** si se prefiere no tocar el espacio de nombres hasta arreglar §6. Deja fuera aftosa, brucelosis, EEV, tétanos equino y Newcastle |
| Sembrar 0 y esperar al arreglo | 0 | **No.** Mantiene rota la funcionalidad clínica descrita en §1.1, que es un daño presente y medible frente a un riesgo futuro |

**Recomendación: 12 filas ahora; las 21 de nivel N3 después de que `uq_vaccination_types_owner_active_name`
esté en producción.** Y las 12 se eligen, además, por un criterio que reduce el daño: son nombres que
**ninguna clínica tiene motivo para querer para sí**, porque nombran biológicos nacionales o de guía
internacional, no productos ni servicios propios de una clínica concreta.

---

## 7. Especificación del changeset para `db-migrations`

**Fichero:** el siguiente número correlativo libre, con nombre `NNN_seed_vaccination_types.xml`.
Declararlo en `db.changelog-master.xml` en el orden que corresponda. La numeración es tuya, no mía.

### 7.1 Un solo `changeSet`, idempotente

Patrón de `284_seed_platform_access_switch.xml:22-27`: precondición `sqlCheck` con
`onFail="MARK_RAN"` —no `HALT`—, para que un reintento sobre una base donde las filas ya existan no
rompa el despliegue:

```xml
<preConditions onFail="MARK_RAN">
    <sqlCheck expectedResult="0">
        SELECT COUNT(*) FROM vaccination_types WHERE general = TRUE
    </sqlCheck>
</preConditions>
```

Se cuenta por `general = TRUE` y no por la lista de nombres a propósito: si alguien ya sembró filas
globales por otra vía, este changeset **no debe pisarlas**, y `MARK_RAN` es exactamente eso.

### 7.2 Las cuatro columnas que hay que poner **explícitamente**, y por qué

| Columna | Valor | Si se omite |
|---|---|---|
| `general` | **`TRUE`, sin excepción** | El `DEFAULT` de la columna es **`FALSE`** (`055_…:23-32`). Una fila con `general = FALSE` y `company_id IS NULL` incumple el XOR de `VaccinationType.java:50-53` y **lanza `IllegalArgumentException` al mapear la fila leída**, no al insertarla: el `GET` de tipos de vacuna devuelve **500 para todos los tenants**, no solo para el que la creó. Es el peor fallo posible de esta semilla |
| `company_id` | **omitir** (queda `NULL`) | Poner cualquier id rompería el XOR por el otro lado y ataría un catálogo de plataforma a una clínica |
| `enabled` | `TRUE` (hay `DEFAULT TRUE`, ponerlo es explícito y barato) | Correcto por defecto |
| `version` | `0` (hay `DEFAULT 0`) | Correcto por defecto. **No inventar otro valor**: la entidad tiene `@Version` (`VaccinationTypeJpaEntity.java:34-36`) |
| `created_date` | **omitir** | `DEFAULT CURRENT_TIMESTAMP`. Es correcto, y así la fecha es la del despliegue real |

Forma de cada `<insert>`:

```xml
<insert tableName="vaccination_types">
    <column name="name"        value="Antirrábica canina y felina"/>
    <column name="description" value="Virus de la rabia inactivado, …"/>
    <column name="general"     valueBoolean="true"/>
    <column name="enabled"     valueBoolean="true"/>
    <column name="version"     valueNumeric="0"/>
</insert>
```

**Booleanos con `valueBoolean`, nunca `value="1"`**: la columna es `BOOLEAN` y el proyecto fija
`preferred_boolean_jdbc_type: TINYINT` (`application.yml:85`); mezclar literales numéricos con
`ddl-auto: validate` es pedir un fallo de arranque.

### 7.3 Encoding

Los nombres llevan tildes (`Antirrábica`, `rinotraqueítis`, `tetánico`, `quíntuple`). El XML debe
declarar `<?xml version="1.0" encoding="UTF-8"?>` —como todos los del árbol— y **guardarse en UTF-8
sin BOM**. Una tilde mal codificada aquí acaba impresa en un carné de vacunación.

### 7.4 `<rollback>`

```xml
<rollback>
    <delete tableName="vaccination_types">
        <where>general = TRUE AND name IN (…las 12…)</where>
    </delete>
</rollback>
```

**Advertencia que hay que dejar escrita en el comentario del changeset:** este rollback **falla** —y
debe fallar— si alguna clínica ya registró una vacunación contra uno de estos tipos, porque
`fk_vaccinations_vaccination_type` (`035_…:15-18`) lo impide. Eso no es un defecto del rollback: es el
FK protegiendo historia clínica. Si hiciera falta retirar un tipo ya usado, el camino es el **borrado
lógico** (`enabled = 0`), no el `DELETE`.

### 7.5 Mejora opcional del mismo changeset (recomendada, no bloqueante)

Bajar la invariante **I1** a la base, que hoy solo vive en Java:

```sql
ALTER TABLE vaccination_types
    ADD CONSTRAINT ck_vaccination_types_owner_xor
    CHECK ((general = TRUE AND company_id IS NULL) OR (general = FALSE AND company_id IS NOT NULL));
```

MySQL 8.4 aplica `CHECK` de verdad. Coste del `ALTER`: se valida contra las filas existentes, así que
**hay que ejecutarlo antes de los `INSERT` o en un changeset propio**, y solo si la tabla ya cumple.
Con la tabla vacía —que es el caso hoy— es gratis. Si se añade, ponerle una `preCondition` `sqlCheck`
que confirme `SELECT COUNT(*) … WHERE (general = TRUE AND company_id IS NOT NULL) OR (general = FALSE
AND company_id IS NULL)` = 0, con `onFail="HALT"`, igual que hace `226`.

### 7.6 Coste del DDL

| Operación | Clasificación InnoDB | Nota |
|---|---|---|
| 12 `INSERT` | DML, no DDL | Irrelevante: 12 filas |
| `ADD CONSTRAINT … CHECK` | *In place*, permite DML concurrente | Valida la tabla existente; con la tabla vacía es instantáneo |
| `ADD COLUMN … GENERATED … STORED` (§6) | **Reconstruye la tabla** (*copy*/*inplace* con rebuild) — **no es `INSTANT`** | Con este volumen es irrelevante, pero hay que decirlo: una columna generada `STORED` no entra por la vía `INSTANT` |

Fuente: manual de MySQL 8.4, *InnoDB Online DDL Operations*.

### 7.7 Expand/contract — cómo se añade una columna a esta tabla dentro de un año

Por si aparece `species`, `route` o `frequency_months` en el catálogo, que es lo previsible:

1. **Expand:** `ADD COLUMN … NULL` (es *instant* en InnoDB si no lleva `GENERATED … STORED` ni cambia
   el formato de fila). Nunca `NOT NULL` con default en el primer paso si la aplicación vieja aún
   escribe.
2. **Backfill por lotes**, nunca un `UPDATE` de una transacción sobre toda la tabla — irrelevante con
   33 filas, pero la regla se escribe para que no se rompa cuando cada tenant tenga las suyas.
3. **Doble lectura**, y solo cuando el 100 % de las filas esté rellenado, `MODIFY … NOT NULL`.
4. **Contract.**

Referencia: Fowler, *Parallel Change*; GitLab, *Avoiding downtime in migrations*.

### 7.8 Índices

**No se propone ningún índice nuevo.** Justificación con el número delante: la tabla tendrá **12 filas
globales** más las propias de cada tenant, y la única consulta de listado es
`findAllByGeneralTrueOrCompany_Id` (`VaccinationTypeJpaRepository.java:33-34`), que devuelve la tabla
casi entera por diseño. Con este volumen, cualquier índice adicional cuesta escritura y no ahorra
nada; el FK `company_id` ya tiene su índice automático de InnoDB. **Si algún día el catálogo por tenant
crece a miles de filas, el índice a proponer sería `(company_id, general, enabled)`, y no antes.**
El `uq_vaccination_types_owner_active_name` de §6 es una constraint de integridad, no una optimización
—aunque de paso sirva a las lecturas por `owner_id`—.

---

## 8. Bloque para copiar

`name` y `description` de las 12 filas de nivel N1 y N2, separadas por tabulador. Verificado
programáticamente: ningún `name` supera 100 caracteres, ninguna `description` supera 500, ninguna está
vacía.

```tsv
Antirrábica canina y felina	Virus de la rabia inactivado, aplicable a perros y gatos. Previene la rabia, encefalitis de curso mortal y zoonosis transmisible al ser humano. Vacuna núcleo (core) para ambas especies según WSAVA y AAHA/AAFP, y exigida en Colombia dentro del control oficial de zoonosis (Ley 9 de 1979 y Decreto 2257 de 1986).
Polivalente canina DHPP: moquillo, hepatitis, parvovirus y parainfluenza	Vacuna combinada para perros. Previene el moquillo (virus del distemper canino), la hepatitis infecciosa canina (adenovirus canino, formulada con cepa CAV-2), la parvovirosis (parvovirus canino tipo 2) y la parainfluenza canina. Reúne las tres vacunas núcleo (core) del perro según WSAVA. En el mercado colombiano se conoce como quíntuple.
Polivalente canina DHPP-L: moquillo, hepatitis, parvovirus, parainfluenza y leptospirosis	Vacuna combinada para perros. Previene el moquillo, la hepatitis infecciosa canina (adenovirus canino), la parvovirosis, la parainfluenza canina y la leptospirosis (serovares de Leptospira interrogans). Las cuatro primeras valencias son las mismas de la polivalente DHPP; la fracción de Leptospira es no núcleo (non-core) y se indica por riesgo de exposición. En el mercado colombiano se conoce como séxtuple.
Leptospirosis canina	Bacterina para perros contra serovares de Leptospira interrogans (canicola, icterohaemorrhagiae, grippotyphosa y pomona, según el producto). Previene la leptospirosis canina, de curso hepático y renal, y zoonosis de importancia en Colombia. Vacuna no núcleo (non-core) según WSAVA: se indica por riesgo de exposición a agua estancada, roedores o inundaciones.
Tos de las perreras (Bordetella bronchiseptica)	Vacuna para perros contra Bordetella bronchiseptica, sola o combinada con parainfluenza canina, en presentación intranasal, oral o inyectable. Previene la traqueobronquitis infecciosa canina, conocida como tos de las perreras. Vacuna no núcleo (non-core): se indica en perros con guardería, peluquería, criadero, albergue o exposiciones.
Trivalente felina FVRCP: panleucopenia, rinotraqueítis y calicivirus	Vacuna combinada para gatos. Previene la panleucopenia felina (parvovirus felino), la rinotraqueítis viral felina (herpesvirus felino tipo 1) y la calicivirosis felina (calicivirus felino). Son las tres vacunas núcleo (core) del gato según WSAVA y las guías AAHA/AAFP. En el mercado colombiano se conoce como triple felina.
Leucemia felina (FeLV)	Vacuna para gatos contra el virus de la leucemia felina, retrovirus que causa inmunosupresión, anemia y linfoma. Núcleo (core) en gatos menores de un año según AAHA/AAFP y WSAVA; en el gato adulto es no núcleo (non-core) y se indica con acceso al exterior o convivencia con gatos de estado desconocido. Exige prueba previa de FeLV.
Toxoide tetánico equino	Toxoide de Clostridium tetani para equinos. Previene el tétanos, enfermedad neurológica de letalidad muy alta en el caballo, que se adquiere por heridas punzantes, por el ombligo del potro y por la castración. Vacuna núcleo (core) de la AAEP: todo équido debe recibirla con independencia de su uso, su valor o su ubicación.
Encefalitis equina venezolana (EEV)	Vacuna de virus vivo atenuado, cepa TC-83, para caballares, mulares y asnales. Previene la encefalitis equina venezolana, de curso nervioso y mortal, y zoonosis. En Colombia es enfermedad de control oficial y su aplicación es obligatoria por debajo de los 1.500 m s. n. m. y para todo équido que asista a eventos de concentración animal (Resolución ICA 6646 de 2017).
Fiebre aftosa bovina	Vacuna oleosa inactivada contra el virus de la fiebre aftosa, para bovinos y bufalinos. Previene la fiebre aftosa, enfermedad vesicular de difusión muy rápida y alto impacto económico. En Colombia la vacunación es obligatoria y se ejecuta en dos ciclos anuales coordinados por el ICA; es la condición que sostiene el estatus de país libre de fiebre aftosa con vacunación.
Brucelosis bovina	Vacuna de Brucella abortus, cepa 19 o cepa RB51, para bovinos. Previene la brucelosis bovina, causa de aborto y de pérdida reproductiva, y zoonosis de riesgo profesional. En Colombia su aplicación es obligatoria en las terneras dentro de la franja de edad definida por el ICA y se ejecuta junto con los ciclos oficiales de vacunación.
Enfermedad de Newcastle	Vacuna para aves contra el virus de la enfermedad de Newcastle (paramixovirus aviar tipo 1), en presentación viva y en oleosa inactivada. Previene la enfermedad de Newcastle, de curso respiratorio, nervioso y digestivo. En Colombia es enfermedad de control oficial del ICA; el esquema depende del tipo de ave y del sistema de producción, y la vacunación no es universalmente obligatoria para toda ave.
```

---

## 9. Fuentes

Consultadas el **2026-08-25**. La columna «Acceso» dice la verdad sobre cómo se obtuvo, porque varias
sedes devuelven `403` a la herramienta de descarga y **eso cambia la fuerza de la cita**.

| # | Fuente | URL | Acceso | Qué sostiene |
|---|---|---|---|---|
| F1 | WSAVA, *Guidelines for the vaccination of dogs and cats* (VGG, 2024), JSAP, DOI 10.1111/jsap.13718 | https://wsava.org/global-guidelines/vaccination-guidelines/ | **403** a la descarga directa; contenido obtenido por búsqueda con cita textual | Clasificación *core* / *non-core* / *not recommended* de perro y gato; «core vaccines … all dogs and cats, regardless of geographic location, should receive throughout their lives»; parainfluenza pasa a *core* en refugios; FeLV *core* en gatos <1 año donde sea prevalente |
| F2 | AAHA, *2022 Canine Vaccination Guidelines* | https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/ | **403**; contenido por búsqueda | Core: rabia, moquillo, parvovirus, adenovirus-2. Non-core: *Bordetella*, *Leptospira*, *Borrelia*, influenza canina |
| F3 | AAHA/AAFP, *2020 Feline Vaccination Guidelines* | https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/ | **403**; contenido por búsqueda | Core felino: FHV-1, FCV, FPV, rabia y FeLV en <1 año. Non-core: FeLV adulto, *Chlamydia felis*, *Bordetella* |
| F4 | AAEP, *Equine Vaccination Guidelines* | https://aaep.org/guidelines-resources/vaccination-guidelines/ | **Descargada íntegra** | Core equino: tétanos, EEE/WEE, virus del Nilo Occidental y rabia. Riesgo: ántrax, botulismo, herpesvirus, influenza, arteritis, leptospirosis, fiebre del Potomac, rotavirus, mordedura de serpiente, papera equina y EEV |
| F5 | ICA, Registro de medicamentos y biológicos de uso veterinario | https://www.ica.gov.co/areas/pecuaria/servicios/regulacion-y-control-de-medicamentos-veterinarios.aspx | Por búsqueda | El ICA es la autoridad que **registra** todo biológico veterinario que se fabrique, importe o comercialice en Colombia |
| F6 | ICA, Listados vigentes | https://www.ica.gov.co/areas/pecuaria/servicios/regulacion-y-control-de-medicamentos-veterinarios/listados-vigentes | **Descargada** | Publica la «Base de datos de medicamentos», actualizada al **3 de agosto de 2026**, y un tablero de analítica de productos registrados. **Es la fuente para cerrar los tres «no verificado» de la §5** |
| F7 | Resolución ICA **6646 de 2017**, Encefalitis Equina Venezolana | https://normograma.invima.gov.co/compilacion/docs/resolucion_ica_6646_2017.htm | **Descargada íntegra** | Art. 1: medidas sanitarias de EEV. Art. 3: vacunación **obligatoria** en caballares, mulares y asnales «en las áreas por debajo o iguales a los 1.500 m.s.n.m.» y en eventos de concentración. Art. 3 par. 1: desde las 2 semanas en potros, repetición cada 2 años. Art. 4: vacuna oficial **virus vivo atenuado cepa TC-83**, controlada por el LANIP |
| F8 | ICA, vacunación obligatoria contra peste porcina clásica (Resolución 22077) | https://www.ica.gov.co/noticias/ica-establecio-vacunacion-obligatoria-contra-ppc.aspx | **Descargada** | Obligatoria en los departamentos y municipios listados; esquema por ciclo productivo; solo vacunas con registro ICA, «virus vivo modificado … con Cepa China, adaptada en cultivos celulares» |
| F9 | ICA, Newcastle | https://www.ica.gov.co/areas/pecuaria/servicios/enfermedades-animales/newcastle-1.aspx | **Descargada** | Se controla con bioseguridad **y** vacunación; el esquema depende del tipo de ave. **Corrige** lo que afirman fuentes secundarias: **no** es vacunación universalmente obligatoria. Resoluciones 3650, 3651 y 3652 de 2014, y 103751 de 2021 |
| F10 | ICA, ciclos de vacunación 2025 (aftosa, brucelosis, rabia de origen silvestre) | https://www.ica.gov.co/noticias/ganaderos-a-vacunar-ciclo-vacunacion-2025 | Por búsqueda | Dos ciclos anuales de carácter **obligatorio**; brucelosis en terneras entre 3 y 9 meses; rabia de origen silvestre en zonas de riesgo; sostiene el estatus de país libre de aftosa con vacunación |
| F11 | Secretaría Distrital de Salud, Marco legal vigente en zoonosis | https://www.saludcapital.gov.co/CZOO/Paginas/MarcoLegalVigente.aspx | **Descargada** | **Ley 9 de 1979** (medidas sanitarias), **Decreto 2257 de 1986** (investigación, prevención y control de zoonosis), Ley 84 de 1989, Ley 746 de 2002, Resolución 0240 de 2014 |
| F12 | MinSalud, *Lineamiento para el manejo de biológico antirrábico de perros y gatos* | https://www.minsalud.gov.co/sites/rid/Lists/BibliotecaDigital/RIDE/VS/PP/SA/lineamiento-manejo-biologico-antirrabico-perros-gatos.pdf | **PDF sin capa de texto legible** | Sería la fuente del esquema oficial colombiano (edad de primera dosis y periodicidad). **Queda pendiente**; por eso ninguna `description` de este documento afirma edades ni intervalos |

### 9.1 Respaldo de cada fila

| Fila | Fuentes |
|---|---|
| Antirrábica canina y felina | F1, F2, F3, F11 |
| Polivalente canina DHPP | F1, F2 |
| Polivalente canina DHPP-L | F1, F2 |
| Leptospirosis canina | F1, F2 |
| Tos de las perreras (*Bordetella*) | F1, F2 |
| Trivalente felina FVRCP | F1, F3 |
| Leucemia felina (FeLV) | F1, F3 |
| Toxoide tetánico equino | F4 |
| Encefalitis equina venezolana | **F7** |
| Encefalitis equina del Este y del Oeste | F4 |
| Influenza equina, rinoneumonitis, antirrábica equina | F4 |
| Fiebre aftosa bovina | F10 |
| Brucelosis bovina | F10 |
| Rabia bovina de origen silvestre | F10 |
| Enfermedad de Newcastle | **F9** |
| Peste porcina clásica | **F8** |
| Resto de bovinos, aves y otras especies | Uso veterinario establecido y biológicos con registro en Colombia según F5/F6, **sin verificación producto a producto** — ver §10 |

**Un aviso sobre las fuentes de internet en general y sobre este catálogo en particular:** las fuentes
secundarias en español repiten con mucha frecuencia que «la Resolución ICA 2640 de 2007 hace obligatoria
la antirrábica en animales de compañía». **No se pudo verificar y este documento no lo afirma.** La
rabia en perros y gatos es materia de **salud pública** —Ley 9 de 1979 y Decreto 2257 de 1986,
F11—, mientras que el ICA registra biológicos y dirige los programas de sanidad **pecuaria**. La
`description` de la fila antirrábica cita solo lo verificado.

---

## 10. Qué **no** se comprobó, y por qué

Separado a propósito de lo verificado.

| Sin comprobar | Por qué |
|---|---|
| El nombre real del índice único en MySQL y su collation efectiva | El contenedor `vetsoftware_mysql` **no está levantado** (`docker ps`, 2026-08-25) y el encargo prohíbe levantar servicios y consultar la base de dev. Todo lo de §6.1 es razonamiento sobre el DDL declarado, no medición |
| El plan de ejecución de `findAllByGeneralTrueOrCompany_Id` | Misma razón. Con 12 filas el plan es irrelevante; se anota por rigor, no por sospecha |
| Registro ICA vigente producto a producto | Exige descargar y cruzar la base de datos de medicamentos del ICA (F6). Afecta a influenza canina, leishmaniosis canina, influenza aviar y vacunas de conejo, que por eso son N3 o quedan excluidas (§5) |
| El esquema oficial colombiano de la antirrábica (edad, refuerzo) | El PDF de MinSalud (F12) no tiene capa de texto legible. Ninguna `description` afirma edades ni intervalos |
| Si el front del tenant ofrece algún camino para recuperar un tipo dado de baja | Fuera del alcance de datos; es el mismo hueco que #482 dejó anotado para `spa_types` |
| Si los otros catálogos maestros (`consultation_types`, `surgery_types`, `diagnostic_imaging_types`, `laboratory_test_types`) tienen el mismo `UNIQUE` global | **#482 lo dejó como sospecha y sigue sin barrer.** Aquí se confirma solo para `vaccination_types` |

---

## 11. Qué debe pasar después de esto — con su issue

Los cuatro hallazgos de este documento están registrados en `kefaroTech/vetsoftware-backend`:

| Issue | Qué es | Quién lo cierra |
|---|---|---|
| **#558** | El catálogo se despliega vacío y `vaccination_type_id` es `NOT NULL`: ninguna clínica nueva puede registrar una vacunación | `db-migrations`, con **este documento** como especificación |
| **#557** | El índice único de `name` es global: la primera clínica que cree un tipo se lo quita a todas las demás (y la baja quema el nombre) | `db-migrations` |
| **#560** | El XOR `general` / `company_id` solo vive en Java y se evalúa **al leer**: una fila mal insertada rompe el listado de todos los tenants | `db-migrations` |
| **#559** | El alta no comprueba que el nombre esté libre y su constraint no está mapeada: 409 en inglés sin campo | `backend-feature` |

Orden recomendado:

1. **#560 primero**, mientras la tabla esté vacía: el `CHECK` de §7.5 cuesta cero hoy y protege al propio changeset de semilla del error más caro que puede cometer.
2. **#558**: `NNN_seed_vaccination_types.xml` con las 12 filas de §8 y las reglas de §7.
3. **#557** en changeset aparte (`uq_vaccination_types_owner_active_name`, §6).
4. **#559** después de #557, porque `mapConstraint` traduce por **nombre** de constraint y debe escribirse contra el definitivo.
5. Cuando #557 esté en producción, sembrar las 21 filas N3 en una segunda tanda.
