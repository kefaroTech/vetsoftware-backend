# Semilla del catálogo `medicaments` — especificación para `db-migrations`

**Autor:** agente de modelado de datos · **Fecha:** 2026-08-25
**Estado:** especificación lista para escribir el changeset. **Este documento no es un changeset.**
`src/main/resources/db/changelog/` es territorio exclusivo de `db-migrations`.

**Base de conocimiento:** la base de datos oficial de **productos veterinarios registrados del ICA**,
publicada el **3 de agosto de 2026** (9.478 productos, 8.129 con estado `VIGENTE`), descargada y
verificada molécula por molécula para este documento. El detalle de la verificación está en §7.

---

## 0. Resumen ejecutivo

| Cosa | Decisión |
|---|---|
| **Unidad de la fila** | **El principio activo (DCI), o la combinación fija registrada.** Ni marca comercial ni concentración |
| **Filas propuestas** | **160** (154 nuevas + 6 que ya existen y solo reciben `description`) |
| **`company_id` / `general`** | `NULL` / `true` en las 160. Lo exige `Medicament.validate` (`Medicament.java:50-51`) |
| **`description`** | Se rellena en las 160. Grupo terapéutico + indicación general + advertencia de especie o de control. **Nunca una dosis** |
| **Riesgo bloqueante detectado** | `uq_medicaments_name` es **único global**, no `(company_id, name)`. Sembrar 160 filas quema 160 nombres —y todas sus variantes de acento y caja— para todos los tenants. Ver §5 y el **issue #557** |
| **Riesgo grave detectado** | El esquema no tiene principio activo, concentración, vía, ATCvet, control especial ni especie contraindicada. La advertencia «permetrina mata gatos» solo cabe como prosa en `description`. Ver §6 y el **issue #567** |
| **Estado del trabajo** | Esta especificación es lo que cierra el **issue #568** («una clínica nueva solo puede recetar seis moléculas») |

---

## 1. La decisión de modelado: molécula, no marca, no concentración

### 1.1 Qué responde cada tabla

El reparto ya está decidido por el código y la semilla **no debe romperlo**:

- `medicaments` responde **«qué molécula es»**.
- `medicament_prescriptions` responde **«en qué presentación, cuánto y cómo»**:
  `presentation VARCHAR(200)`, `quantity`, `posology VARCHAR(1000)`, `observation`
  (`MedicamentPrescription.java:8-11`, validado en `:47-69`).

Meter la concentración en el catálogo duplica un dato que ya vive en `presentation`: dos fuentes de
verdad para el mismo hecho, y la de la receta es la que el veterinario lee. En el catálogo de
antipatrones de Karwin eso es **Metadata Tribbles** — multiplicar filas por cada variante de un
valor en lugar de modelar el valor. Se rechaza.

### 1.2 Por qué la molécula y no la marca — con el número

La base del ICA lo zanja sin necesidad de opinión:

| Hecho medido en la base del ICA (2026-08-03) | Consecuencia si la fila fuera la marca |
|---|---|
| **1.349 de 9.478 productos (14,2 %) están `CANCELADO`** | Uno de cada siete nombres del catálogo apuntaría a un registro muerto, y el catálogo no tiene forma de enterarse |
| **Ivermectina: 348 productos registrados**; permetrina 123; oxitetraciclina 262 | El catálogo pasaría de 160 filas a varios miles, con `UNIQUE` global (§5) |
| Un mismo producto cambia de titular (p. ej. `003-DB CATOSAL` figura hoy a nombre de ELANCO US INC) | El nombre de marca envejece con las fusiones del sector, no con la clínica |

La DCI no cambia: es un nombre asignado por la OMS y permanente. **La marca es un dato de la
receta** (`presentation`: «Bravecto tabletas masticables 500 mg»), no del catálogo.

### 1.3 Y esto además reduce el daño del `UNIQUE` global

Es el argumento que ata las dos decisiones. Con filas-molécula, una clínica **no necesita** crear su
propia fila «Meloxicam»: la global ya se la sirve `findAllByGeneralTrueOrCompany_Id`
(`MedicamentJpaRepository.java:46`). Lo que la clínica querrá crear en privado son **marcas y
fórmulas magistrales** («Metacam 1,5 mg/mL», «Suspensión magistral de metimazol 5 mg/mL»), que **no
colisionan** con una lista de DCI. Sembrar marcas, en cambio, quemaría exactamente los nombres que
las clínicas quieren para sí.

### 1.4 La excepción que se evaluó y se rechazó: los fluidos

«Cloruro de sodio 0,9 %» y «Dextrosa 5 %» tientan a llevar la concentración porque en la práctica se
nombran así. Se rechaza por consistencia: `presentation` ya recoge «bolsa 500 mL al 0,9 %», y admitir
la excepción abre la puerta a «Meloxicam 1,5 mg/mL» tres meses después. Las filas quedan
**Cloruro de sodio (solución salina)**, **Dextrosa** y **Solución de lactato de Ringer (Hartmann)**
— esta última con el nombre propio de la preparación, que no es una concentración.

### 1.5 Convención de nombres (obligatoria para el changeset)

1. DCI en español, **capitalización de oración**: `Meloxicam`, no `MELOXICAM` ni `meloxicam`.
2. **Sin concentración, sin forma farmacéutica, sin marca, sin especie.**
3. Combinación fija registrada: componentes unidos por ` + `, en el orden en que los nombra el
   registro ICA: `Praziquantel + Pirantel + Febantel`.
4. **Ojo con la collation, que aquí no es un detalle.** La collation efectiva es
   **`utf8mb4_0900_ai_ci`** — **verificado** en tres niveles: `@@collation_server`, el default del
   esquema `vetsoftware` y una columna `name` real. Ninguna migración declara `COLLATE`, así que
   todo lo hereda. Es **accent-insensitive y case-insensitive**: `Acido tranexamico`,
   `Ácido tranexámico` y `ÁCIDO TRANEXÁMICO` son **la misma fila** para `uq_medicaments_name`.
   En nomenclatura farmacológica esto se cuela con una facilidad especial —«Pimobendán»,
   «Ácido tolfenámico», «N-acetilcisteína»—, así que la lista de §3 está comprobada por script y no
   a ojo (§3.17).
5. Máximo 200 caracteres (`Medicament.java:46-47`). El nombre más largo de §3 tiene **40**.
6. **Un solo separador para las combinaciones: ` + `** (espacio, más, espacio). Ni
   `Amoxicilina-clavulánico`, ni `Amoxicilina/clavulánico`, ni `Amoxicilina y clavulánico`. Elegir
   dos convenciones a la vez es lo que produce la fila duplicada que la constraint no detecta
   —porque `Amoxicilina + Clavulánico` y `Amoxicilina-clavulánico` **no** colisionan bajo `ai_ci`:
   son dos filas distintas para la base y el mismo fármaco para el veterinario.

---

## 2. Cómo se marca cada fila en este documento

| Marca | Significado | Dónde acaba |
|---|---|---|
| **[ICA]** | Hay **al menos un producto con registro ICA `VIGENTE`** que contiene esta molécula y declara caninos y/o felinos entre sus especies. Verificado en §7 | Frase final de `description` |
| **[EXT]** | **No hay registro veterinario vigente en Colombia.** Se usa por extrapolación desde medicina humana, bajo criterio y responsabilidad del médico veterinario | Frase final de `description` |
| **[FNE]** | **Medicamento de control especial** en Colombia (Resolución 315 de 2020, Fondo Nacional de Estupefacientes) | Frase en `description` |
| **[!GATO]** | Tóxico o contraindicado en gatos | Frase en `description`, **en mayúsculas** |
| **[!RAZA]** | Riesgo aumentado en razas con mutación `ABCB1`/MDR1 (Collie, Pastor Australiano, Border Collie y cruces) | Frase en `description` |

**Sobre [FNE] — el dato exacto, porque casi todo lo publicado lo simplifica mal.** El Anexo Técnico 3
de la Resolución 315 de 2020 lista como medicamentos de control especial **de uso veterinario**
únicamente tres entradas: **butorfanol tartrato 10 mg/mL**, **ketamina clorhidrato 50 y 100 mg/mL**, y
**ketamina clorhidrato + midazolam clorhidrato (50+2) mg/mL**. El resto de lo que un veterinario
maneja bajo control (tramadol, buprenorfina, fenobarbital, diazepam, pentobarbital, tiletamina +
zolazepam) está sometido a fiscalización por el **Anexo 1** o por el listado de uso humano, y el nivel
de control del tramadol «se mantiene como está estipulado desde la Resolución 1478 de 2006».
En este documento **[FNE]** significa «sustancia sometida a fiscalización, con requisitos
diferenciados de prescripción, custodia y reporte ante el FNE», y así debe leerse en `description`.

---

## 3. El catálogo — 160 filas

`company_id = NULL`, `general = true`, `enabled = true`, `version = 0`,
`created_date = CURRENT_TIMESTAMP` en **todas**. Solo se listan `name` y `description`.

### 3.1 Antiparasitarios internos — 12 filas

| # | `name` | `description` |
|---|---|---|
| 1 | Fenbendazol | Antiparasitario interno (bencimidazol). Nematodos gastrointestinales, *Giardia* y algunos cestodos en perros y gatos. Registrado en Colombia para uso veterinario (ICA). |
| 2 | Albendazol | Antiparasitario interno (bencimidazol). Nematodos y cestodos. Registrado en Colombia para uso veterinario (ICA). |
| 3 | Oxibendazol | Antiparasitario interno (bencimidazol). Nematodos gastrointestinales. Registrado en Colombia para uso veterinario (ICA). |
| 4 | Praziquantel | Antiparasitario interno (cestodicida). *Dipylidium*, *Taenia* y *Echinococcus* en perros y gatos. Registrado en Colombia para uso veterinario (ICA). |
| 5 | Pirantel | Antiparasitario interno. Ascáridos y anquilostomas en perros y gatos, incluidos cachorros. Registrado en Colombia para uso veterinario (ICA). |
| 6 | Praziquantel + Pirantel | Combinación antiparasitaria interna de amplio espectro: cestodos y nematodos en una sola toma. Registrada en Colombia para uso veterinario (ICA). |
| 7 | Praziquantel + Pirantel + Febantel | Combinación antiparasitaria interna de amplio espectro para perros: cestodos, nematodos y *Giardia*. Registrada en Colombia para uso veterinario (ICA). |
| 8 | Ivermectina | Antiparasitario interno y externo (lactona macrocíclica). PRECAUCIÓN EN RAZAS CON MUTACIÓN MDR1/ABCB1 (Collie, Pastor Australiano, Border Collie y cruces): riesgo de neurotoxicidad grave. Registrada en Colombia para uso veterinario (ICA). |
| 9 | Milbemicina oxima | Antiparasitario interno (lactona macrocíclica). Nematodos y prevención de dirofilariosis en perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 10 | Milbemicina oxima + Praziquantel | Combinación antiparasitaria interna: nematodos, prevención de dirofilariosis y cestodos. Registrada en Colombia para uso veterinario (ICA). |
| 11 | Moxidectina | Antiparasitario endectocida (lactona macrocíclica). Nematodos, prevención de dirofilariosis y ácaros. Registrada en Colombia para uso veterinario (ICA). |
| 12 | Emodepsida + Toltrazurilo | Combinación antiparasitaria interna para cachorros: nematodos y coccidios (*Isospora*). Registrada en Colombia para uso veterinario (ICA). |

### 3.2 Antiparasitarios externos — 15 filas

| # | `name` | `description` |
|---|---|---|
| 13 | Fipronil | Antiparasitario externo (fenilpirazol). Pulgas y garrapatas en perros y gatos, uso tópico. Registrado en Colombia para uso veterinario (ICA). |
| 14 | Fipronil + S-metopreno | Antiparasitario externo con regulador del crecimiento de insectos: pulgas adultas, huevos y larvas, y garrapatas. Registrado en Colombia para uso veterinario (ICA). |
| 15 | Imidacloprid | Antiparasitario externo (neonicotinoide). Pulgas en perros y gatos, uso tópico. Registrado en Colombia para uso veterinario (ICA). |
| 16 | Imidacloprid + Flumetrina | Antiparasitario externo de liberación prolongada en collar: pulgas y garrapatas. Registrado en Colombia para uso veterinario (ICA). |
| 17 | Imidacloprid + Moxidectina | Antiparasitario externo e interno tópico: pulgas, ácaros, nematodos y prevención de dirofilariosis. Registrado en Colombia para uso veterinario (ICA). |
| 18 | Fluralaner | Antiparasitario externo sistémico (isoxazolina). Pulgas, garrapatas y ácaros en perros y gatos. Usar con precaución en pacientes con antecedentes de convulsiones. Registrado en Colombia para uso veterinario (ICA). |
| 19 | Afoxolaner | Antiparasitario externo sistémico (isoxazolina). Pulgas y garrapatas en perros. Usar con precaución en pacientes con antecedentes de convulsiones. Registrado en Colombia para uso veterinario (ICA). |
| 20 | Sarolaner | Antiparasitario externo sistémico (isoxazolina). Pulgas, garrapatas y ácaros de la sarna en perros. Usar con precaución en pacientes con antecedentes de convulsiones. Registrado en Colombia para uso veterinario (ICA). |
| 21 | Lotilaner | Antiparasitario externo sistémico (isoxazolina). Pulgas y garrapatas en perros y gatos. Usar con precaución en pacientes con antecedentes de convulsiones. Registrado en Colombia para uso veterinario (ICA). |
| 22 | Selamectina | Antiparasitario endectocida tópico (lactona macrocíclica). Pulgas, ácaros de oído, sarna y prevención de dirofilariosis en perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 23 | Permetrina | Antiparasitario externo (piretroide). Pulgas, garrapatas y repelencia de flebótomos en PERROS. TÓXICA EN GATOS: no aplicar en felinos ni permitir su contacto con perros recién tratados; puede causar temblores, convulsiones y muerte. Registrada en Colombia para uso veterinario (ICA). |
| 24 | Deltametrina | Antiparasitario externo (piretroide) en collar para PERROS: garrapatas y repelencia de flebótomos. TÓXICA EN GATOS: no usar en felinos. Registrada en Colombia para uso veterinario (ICA). |
| 25 | Amitraz | Antiparasitario externo (formamidina). Demodicosis y garrapatas en PERROS. NO USAR EN GATOS ni en equinos. Antídoto del efecto alfa-2: atipamezol o yohimbina. Registrado en Colombia para uso veterinario (ICA). |
| 26 | Spinosad | Antiparasitario externo sistémico oral (espinosina). Pulgas en perros y gatos. Registrado en Colombia para uso veterinario (ICA). |
| 27 | Piriproxifeno | Regulador del crecimiento de insectos. Control ambiental y sobre el animal de huevos y larvas de pulga. Registrado en Colombia para uso veterinario (ICA). |

### 3.3 Antibióticos y antimicrobianos sistémicos — 21 filas

| # | `name` | `description` |
|---|---|---|
| 28 | Amoxicilina | Antibiótico betalactámico (aminopenicilina). Infecciones sensibles de piel, vía urinaria y tejidos blandos. Registrada en Colombia para uso veterinario (ICA). |
| 29 | Amoxicilina + Clavulánico | Antibiótico betalactámico con inhibidor de betalactamasas. Infecciones de piel, tejidos blandos, vía urinaria y cavidad oral. Registrada en Colombia para uso veterinario (ICA). |
| 30 | Ampicilina | Antibiótico betalactámico (aminopenicilina), presentación inyectable. Infecciones sistémicas sensibles. Registrada en Colombia para uso veterinario (ICA). |
| 31 | Penicilina G benzatínica + procaínica | Antibiótico betalactámico de acción prolongada. Infecciones por grampositivos sensibles. Registrada en Colombia para uso veterinario (ICA). |
| 32 | Cefalexina | Antibiótico cefalosporínico de primera generación. Piodermas y infecciones de piel y tejidos blandos en perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 33 | Cefadroxilo | Antibiótico cefalosporínico de primera generación, vía oral. Infecciones de piel y tejidos blandos. Registrado en Colombia para uso veterinario (ICA). |
| 34 | Cefovecina | Antibiótico cefalosporínico de tercera generación, inyectable de acción prolongada, para perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 35 | Enrofloxacina | Antibiótico quinolona de amplio espectro. RIESGO DE DEGENERACIÓN RETINIANA Y CEGUERA EN GATOS al superar la dosis recomendada. Evitar en animales en crecimiento por daño del cartílago articular. Registrada en Colombia para uso veterinario (ICA). |
| 36 | Marbofloxacina | Antibiótico quinolona de amplio espectro para perros y gatos. Evitar en animales en crecimiento por daño del cartílago articular. Registrada en Colombia para uso veterinario (ICA). |
| 37 | Ciprofloxacina | Antibiótico quinolona. Infecciones sensibles; absorción oral variable en perros. Registrada en Colombia para uso veterinario (ICA). |
| 38 | Doxiciclina | Antibiótico tetraciclina. Hemoparásitos (*Ehrlichia*, *Anaplasma*), leptospirosis y afecciones respiratorias. Registrada en Colombia para uso veterinario (ICA). |
| 39 | Oxitetraciclina | Antibiótico tetraciclina de amplio espectro. Registrada en Colombia para uso veterinario (ICA). |
| 40 | Azitromicina | Antibiótico macrólido (azálida). Infecciones respiratorias, de piel y por microorganismos intracelulares. Registrada en Colombia para uso veterinario (ICA). |
| 41 | Eritromicina | Antibiótico macrólido. Alternativa en pacientes alérgicos a betalactámicos; también como procinético gástrico. Registrada en Colombia para uso veterinario (ICA). |
| 42 | Tilosina | Antibiótico macrólido. Enteropatías crónicas y colitis en perros. Registrada en Colombia para uso veterinario (ICA). |
| 43 | Metronidazol | Antimicrobiano nitroimidazólico y antiprotozoario. Anaerobios, giardiosis y enteropatías. La sobredosis o el uso prolongado producen neurotoxicidad vestibular. Registrado en Colombia para uso veterinario (ICA). |
| 44 | Clindamicina | Antibiótico lincosamida. Infecciones de cavidad oral, hueso, piel y toxoplasmosis. Registrada en Colombia para uso veterinario (ICA). |
| 45 | Lincomicina | Antibiótico lincosamida. Infecciones por grampositivos sensibles. Registrada en Colombia para uso veterinario (ICA). |
| 46 | Trimetoprim + Sulfadiazina | Sulfa potenciada de amplio espectro. Riesgo de queratoconjuntivitis seca, discrasias sanguíneas y reacciones idiosincráticas en tratamientos prolongados. Registrada en Colombia para uso veterinario (ICA). |
| 47 | Gentamicina | Antibiótico aminoglucósido. Gramnegativos; nefrotóxica y ototóxica, vigilar función renal e hidratación. Registrada en Colombia para uso veterinario (ICA). |
| 48 | Florfenicol | Antibiótico fenicol. Uso tópico ótico y sistémico según presentación. Registrado en Colombia para uso veterinario (ICA). |

### 3.4 AINEs de uso veterinario — 12 filas

> **Advertencia transversal que debe quedar en el producto, no solo aquí:** los AINEs de uso humano
> **no** entran en este catálogo. Ver §4, lista de exclusión.

| # | `name` | `description` |
|---|---|---|
| 49 | Meloxicam | AINE de uso veterinario (oxicam, COX-2 preferente). Dolor e inflamación aguda y crónica en perros y gatos. Contraindicado con deshidratación, hipotensión, enfermedad renal o gastrointestinal activa, y en combinación con corticoides. Registrado en Colombia para uso veterinario (ICA). |
| 50 | Carprofeno | AINE de uso veterinario (propiónico). Dolor osteoarticular y perioperatorio en perros. Vigilar función hepática y renal; no combinar con corticoides. Registrado en Colombia para uso veterinario (ICA). |
| 51 | Firocoxib | AINE de uso veterinario (coxib, COX-2 selectivo). Osteoartritis y dolor perioperatorio en perros. Registrado en Colombia para uso veterinario (ICA). |
| 52 | Robenacoxib | AINE de uso veterinario (coxib, COX-2 selectivo) para perros y gatos. Dolor e inflamación musculoesquelética y perioperatoria. Registrado en Colombia para uso veterinario (ICA). |
| 53 | Mavacoxib | AINE de uso veterinario (coxib) de acción prolongada para perros con osteoartritis. Su vida media larga obliga a vigilar los efectos adversos durante semanas. Registrado en Colombia para uso veterinario (ICA). |
| 54 | Grapiprant | Antagonista del receptor EP4 de la prostaglandina E2, analgésico y antiinflamatorio no COX-inhibidor, para osteoartritis en perros. Registrado en Colombia para uso veterinario (ICA). |
| 55 | Ketoprofeno | AINE de uso veterinario (propiónico). Dolor agudo e inflamación; uso de corta duración por su efecto gastrolesivo. Registrado en Colombia para uso veterinario (ICA). |
| 56 | Ácido tolfenámico | AINE de uso veterinario (fenamato). Dolor e inflamación aguda en perros y gatos. Registrado en Colombia para uso veterinario (ICA). |
| 57 | Flunixino meglumina | AINE de uso veterinario. Dolor visceral, endotoxemia y procesos inflamatorios agudos. Uso restringido y de corta duración en pequeños animales por gastrolesividad. Registrado en Colombia para uso veterinario (ICA). |
| 58 | Dipirona | Analgésico, antipirético y espasmolítico (metamizol). Uso puntual; vigilar discrasias sanguíneas. Registrada en Colombia para uso veterinario (ICA). |
| 59 | Fenilbutazona | AINE de uso veterinario, principalmente equino. Alta gastrolesividad y riesgo de discrasias sanguíneas; uso desaconsejado en perros y gatos habiendo alternativas más seguras. Registrada en Colombia para uso veterinario (ICA). |
| 60 | Tepoxalina | AINE de uso veterinario con inhibición dual COX/LOX para osteoartritis en perros. Registrada en Colombia para uso veterinario (ICA). |

### 3.5 Analgésicos, sedantes y anestésicos — 21 filas

| # | `name` | `description` |
|---|---|---|
| 61 | Tramadol | Analgésico opioide atípico. Dolor agudo y crónico en perros y gatos. MEDICAMENTO DE CONTROL ESPECIAL en Colombia: prescripción, custodia y reporte diferenciados ante el Fondo Nacional de Estupefacientes. Registrado en Colombia para uso veterinario (ICA). |
| 62 | Buprenorfina | Analgésico opioide agonista parcial, de elección en dolor moderado en gatos. MEDICAMENTO DE CONTROL ESPECIAL ante el Fondo Nacional de Estupefacientes. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 63 | Butorfanol | Analgésico y sedante opioide agonista-antagonista. Sedación, analgesia visceral y antitusígeno. MEDICAMENTO DE CONTROL ESPECIAL DE USO VETERINARIO en Colombia (Resolución 315 de 2020, Anexo 3). Registrado en Colombia para uso veterinario (ICA). |
| 64 | Ketamina | Anestésico disociativo. Inducción y anestesia en perros y gatos, siempre combinada con sedantes. MEDICAMENTO DE CONTROL ESPECIAL DE USO VETERINARIO en Colombia (Resolución 315 de 2020, Anexo 3). Registrada en Colombia para uso veterinario (ICA). |
| 65 | Ketamina + Midazolam | Combinación anestésica disociativa con benzodiacepina para inducción en perros y gatos. MEDICAMENTO DE CONTROL ESPECIAL DE USO VETERINARIO en Colombia (Resolución 315 de 2020, Anexo 3). Registrada en Colombia para uso veterinario (ICA). |
| 66 | Tiletamina + Zolazepam | Combinación anestésica disociativa con benzodiacepina para perros y gatos. SUSTANCIA SOMETIDA A FISCALIZACIÓN ante el Fondo Nacional de Estupefacientes. Registrada en Colombia para uso veterinario (ICA). |
| 67 | Midazolam | Benzodiacepina de acción corta. Sedación, premedicación y control de convulsiones. SUSTANCIA SOMETIDA A FISCALIZACIÓN ante el Fondo Nacional de Estupefacientes. En Colombia solo tiene registro veterinario en combinación fija con ketamina; sola se usa por extrapolación humana. |
| 68 | Diazepam | Benzodiacepina. Control de convulsiones, sedación y estimulación del apetito en gatos. SUSTANCIA SOMETIDA A FISCALIZACIÓN ante el Fondo Nacional de Estupefacientes. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 69 | Propofol | Anestésico intravenoso de acción ultracorta para inducción y mantenimiento. Produce apnea si se inyecta rápido; exige control de la vía aérea. Registrado en Colombia para uso veterinario (ICA). |
| 70 | Isoflurano | Anestésico inhalatorio halogenado para mantenimiento anestésico. Requiere vaporizador calibrado y monitorización. Registrado en Colombia para uso veterinario (ICA). |
| 71 | Sevoflurano | Anestésico inhalatorio halogenado de inducción y recuperación rápidas. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 72 | Lidocaína | Anestésico local tipo amida y antiarrítmico de clase IB. Bloqueos locorregionales e infusión analgésica. USAR CON ESPECIAL PRECAUCIÓN EN GATOS por su menor margen de seguridad. Registrada en Colombia para uso veterinario (ICA). |
| 73 | Bupivacaína | Anestésico local tipo amida de larga duración para bloqueos locorregionales. Nunca por vía intravenosa: cardiotoxicidad. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 74 | Dexmedetomidina | Sedante y analgésico agonista alfa-2 para perros y gatos. Produce bradicardia y vasoconstricción; reversible con atipamezol. Registrada en Colombia para uso veterinario (ICA). |
| 75 | Medetomidina | Sedante y analgésico agonista alfa-2. Reversible con atipamezol. Registrada en Colombia para uso veterinario (ICA). |
| 76 | Xilacina | Sedante, analgésico y miorrelajante agonista alfa-2. Emética en gatos; produce bradicardia y depresión respiratoria. Reversible con yohimbina o atipamezol. Registrada en Colombia para uso veterinario (ICA). |
| 77 | Acepromacina | Tranquilizante fenotiazínico. Sedación y premedicación; NO es analgésico. Produce hipotensión; usar con precaución en braquicéfalos y en pacientes con antecedentes de convulsiones. Registrada en Colombia para uso veterinario (ICA). |
| 78 | Atipamezol | Antagonista alfa-2 selectivo. Reversión de la sedación por dexmedetomidina, medetomidina o xilacina. Registrado en Colombia para uso veterinario (ICA). |
| 79 | Yohimbina | Antagonista alfa-2. Reversión de la sedación por xilacina y del efecto del amitraz. Registrada en Colombia para uso veterinario (ICA). |
| 80 | Atropina | Anticolinérgico. Bradicardia, premedicación anestésica y antídoto en intoxicación por organofosforados. Registrada en Colombia para uso veterinario (ICA). |
| 81 | Gabapentina | Analgésico para dolor neuropático y ansiolítico previo a consulta, especialmente en gatos. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.6 Corticoides — 8 filas

| # | `name` | `description` |
|---|---|---|
| 82 | Prednisolona | Corticoide de acción intermedia. Antiinflamatorio e inmunosupresor; es la forma de elección en gatos por no requerir conversión hepática. No combinar con AINEs. Registrada en Colombia para uso veterinario (ICA). |
| 83 | Prednisona | Corticoide de acción intermedia, profármaco de la prednisolona. Antiinflamatorio e inmunosupresor en perros. No combinar con AINEs. Registrada en Colombia para uso veterinario (ICA). |
| 84 | Dexametasona | Corticoide de acción prolongada y alta potencia antiinflamatoria. Procesos inflamatorios, alérgicos y de urgencia. No combinar con AINEs. Registrada en Colombia para uso veterinario (ICA). |
| 85 | Betametasona | Corticoide de acción prolongada. Procesos inflamatorios y alérgicos; también en formulaciones tópicas y óticas. Registrada en Colombia para uso veterinario (ICA). |
| 86 | Triamcinolona | Corticoide de acción intermedia-prolongada. Dermatopatías inflamatorias y alérgicas. Registrada en Colombia para uso veterinario (ICA). |
| 87 | Hidrocortisona | Corticoide de acción corta. Formulaciones tópicas, óticas y oftálmicas; terapia de reemplazo en hipoadrenocorticismo. Registrada en Colombia para uso veterinario (ICA). |
| 88 | Aceponato de hidrocortisona | Corticoide tópico de acción local (diéster) para dermatitis atópica en perros, con baja absorción sistémica. Registrado en Colombia para uso veterinario (ICA). |
| 89 | Metilprednisolona | Corticoide de acción intermedia, antiinflamatorio e inmunosupresor. Sin registro veterinario vigente en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.7 Digestivos y hepatoprotectores — 13 filas

| # | `name` | `description` |
|---|---|---|
| 90 | Maropitant | Antiemético antagonista del receptor NK-1 para perros y gatos. Vómito agudo, cinetosis y control del vómito perioperatorio. Registrado en Colombia para uso veterinario (ICA). |
| 91 | Metoclopramida | Antiemético central y procinético gastrointestinal. Contraindicado ante obstrucción o perforación digestiva. Registrada en Colombia para uso veterinario (ICA). |
| 92 | Ondansetrón | Antiemético antagonista de receptores 5-HT3, útil en vómito refractario y quimioterapia. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 93 | Omeprazol | Inhibidor de la bomba de protones. Úlcera gastroduodenal, esofagitis por reflujo y gastroprotección. Registrado en Colombia para uso veterinario (ICA). |
| 94 | Ranitidina | Antagonista de receptores H2 de la histamina, antisecretor gástrico. Registrada en Colombia para uso veterinario (ICA); en medicina humana el INVIMA ordenó su retiro del mercado en 2020 por nitrosaminas (NDMA). |
| 95 | Famotidina | Antagonista de receptores H2 de la histamina, antisecretor gástrico. Registrada en Colombia para uso veterinario (ICA). |
| 96 | Sucralfato | Protector de la mucosa gástrica; forma una barrera sobre la lesión ulcerada. Separar su administración de otros fármacos orales porque reduce su absorción. Registrado en Colombia para uso veterinario (ICA). |
| 97 | Silimarina | Hepatoprotector derivado del cardo mariano, antioxidante hepático. Registrada en Colombia para uso veterinario (ICA). |
| 98 | Ademetionina | Hepatoprotector (S-adenosilmetionina). Soporte antioxidante en hepatopatías crónicas. Registrada en Colombia para uso veterinario (ICA). |
| 99 | Lactulosa | Laxante osmótico y reductor de la absorción de amoníaco en encefalopatía hepática. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 100 | Ácido ursodesoxicólico | Colerético y hepatoprotector en colestasis y hepatopatías crónicas. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 101 | Enzimas pancreáticas | Suplemento enzimático de reemplazo en insuficiencia pancreática exocrina. Sin registro veterinario en Colombia como medicamento: uso por extrapolación humana o como suplemento, bajo criterio del médico veterinario. |
| 102 | Apomorfina | Emético de acción central para inducir el vómito en intoxicaciones recientes en perros. No usar en gatos. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.8 Cardiología y nefrología — 9 filas

| # | `name` | `description` |
|---|---|---|
| 103 | Pimobendán | Inodilatador (sensibilizante al calcio e inhibidor de PDE3). Insuficiencia cardíaca congestiva por enfermedad valvular mitral o cardiomiopatía dilatada en perros. Registrado en Colombia para uso veterinario (ICA). |
| 104 | Furosemida | Diurético de asa. Edema pulmonar e insuficiencia cardíaca congestiva. Vigilar deshidratación, función renal y potasio. Registrada en Colombia para uso veterinario (ICA). |
| 105 | Torasemida | Diurético de asa de acción más prolongada que la furosemida, en insuficiencia cardíaca congestiva refractaria. Registrada en Colombia para uso veterinario (ICA). |
| 106 | Benazepril | Inhibidor de la ECA. Insuficiencia cardíaca y proteinuria en enfermedad renal crónica. Registrado en Colombia para uso veterinario (ICA). |
| 107 | Enalapril | Inhibidor de la ECA. Insuficiencia cardíaca e hipertensión. Vigilar función renal y potasio. Registrado en Colombia para uso veterinario (ICA). |
| 108 | Espironolactona | Diurético ahorrador de potasio y antagonista de la aldosterona, coadyuvante en insuficiencia cardíaca. Registrada en Colombia para uso veterinario (ICA). |
| 109 | Amlodipino | Bloqueante de los canales de calcio. Hipertensión arterial sistémica, especialmente en gatos con enfermedad renal crónica. Registrado en Colombia para uso veterinario (ICA). |
| 110 | Digoxina | Glucósido digitálico. Control de la frecuencia en fibrilación auricular. Margen terapéutico estrecho: exige monitorización. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 111 | Atenolol | Betabloqueante cardioselectivo. Cardiomiopatía hipertrófica felina y taquiarritmias. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.9 Endocrinos — 6 filas

| # | `name` | `description` |
|---|---|---|
| 112 | Insulina porcina zinc | Insulina de origen porcino de acción intermedia para el tratamiento de la diabetes mellitus en perros y gatos. Conservación en frío y homogeneización suave antes de cada uso. Registrada en Colombia para uso veterinario (ICA). |
| 113 | Insulina glargina | Análogo de insulina de acción prolongada, usado en el manejo de la diabetes mellitus felina. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 114 | Levotiroxina sódica | Hormona tiroidea de reemplazo en el hipotiroidismo canino. Requiere control periódico de niveles séricos. Registrada en Colombia para uso veterinario (ICA). |
| 115 | Metimazol | Antitiroideo para el hipertiroidismo felino. Vigilar hemograma, función hepática y reacciones cutáneas. Sin registro veterinario en Colombia: uso por extrapolación humana o preparación magistral, bajo criterio del médico veterinario. |
| 116 | Trilostano | Inhibidor de la 3-beta-hidroxiesteroide deshidrogenasa para el hiperadrenocorticismo (síndrome de Cushing) canino. Exige monitorización de cortisol y electrolitos. Sin registro veterinario en Colombia: uso por extrapolación o importación, bajo criterio del médico veterinario. |
| 117 | Desmopresina | Análogo de la vasopresina para la diabetes insípida central. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.10 Dermatología y antifúngicos — 9 filas

| # | `name` | `description` |
|---|---|---|
| 118 | Oclacitinib | Inhibidor selectivo de Janus quinasa (JAK1). Control del prurito y de la dermatitis atópica en perros. No usar en perros con infección grave, neoplasia ni menores de la edad autorizada. Registrado en Colombia para uso veterinario (ICA). |
| 119 | Lokivetmab | Anticuerpo monoclonal caninizado anti-IL-31. Control del prurito por dermatitis atópica en perros. Registrado en Colombia para uso veterinario (ICA). |
| 120 | Ciclosporina | Inmunomodulador inhibidor de la calcineurina. Dermatitis atópica y enfermedades inmunomediadas en perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 121 | Itraconazol | Antifúngico triazólico sistémico. Dermatofitosis y micosis sistémicas en perros y gatos. Vigilar función hepática. Registrado en Colombia para uso veterinario (ICA). |
| 122 | Ketoconazol | Antifúngico imidazólico, sistémico y tópico. Dermatofitosis y malasseziosis. Hepatotóxico; interacciona con múltiples fármacos. Registrado en Colombia para uso veterinario (ICA). |
| 123 | Terbinafina | Antifúngico alilamina, sistémico y tópico. Dermatofitosis en perros y gatos. Registrada en Colombia para uso veterinario (ICA). |
| 124 | Griseofulvina | Antifúngico sistémico para dermatofitosis. TERATOGÉNICA: no usar en hembras gestantes. Registrada en Colombia para uso veterinario (ICA). |
| 125 | Miconazol | Antifúngico imidazólico tópico. Dermatofitosis y malasseziosis, con frecuencia asociado a clorhexidina. Registrado en Colombia para uso veterinario (ICA). |
| 126 | Clorhexidina | Antiséptico de amplio espectro para piel y mucosas. Piodermas, antisepsia quirúrgica e higiene oral. Registrada en Colombia para uso veterinario (ICA). |

### 3.11 Antihistamínicos — 5 filas

| # | `name` | `description` |
|---|---|---|
| 127 | Clorfenamina | Antihistamínico H1 de primera generación. Reacciones alérgicas y prurito; produce sedación. Registrada en Colombia para uso veterinario (ICA). |
| 128 | Difenhidramina | Antihistamínico H1 de primera generación. Reacciones alérgicas agudas y cinetosis; produce sedación. Registrada en Colombia para uso veterinario (ICA). |
| 129 | Cetirizina | Antihistamínico H1 de segunda generación, con menor efecto sedante. Prurito alérgico. Registrada en Colombia para uso veterinario (ICA). |
| 130 | Hidroxicina | Antihistamínico H1 de primera generación con efecto ansiolítico, usado en dermatopatías alérgicas. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 131 | Ciproheptadina | Antihistamínico H1 y antiserotoninérgico, usado como estimulante del apetito en gatos. Registrada en Colombia para uso veterinario (ICA). |

### 3.12 Oftálmicos y óticos — 6 filas

| # | `name` | `description` |
|---|---|---|
| 132 | Tobramicina | Antibiótico aminoglucósido de uso oftálmico tópico. Conjuntivitis y úlceras corneales infectadas. Registrada en Colombia para uso veterinario (ICA). |
| 133 | Dorzolamida | Inhibidor tópico de la anhidrasa carbónica. Reducción de la presión intraocular en glaucoma. Registrada en Colombia para uso veterinario (ICA). |
| 134 | Ácido hialurónico | Lubricante ocular y protector corneal en queratoconjuntivitis seca y lesiones de superficie. Registrado en Colombia para uso veterinario (ICA). |
| 135 | Latanoprost | Análogo de prostaglandina tópico, hipotensor ocular en glaucoma canino. Contraindicado en uveítis y en glaucoma felino. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 136 | Tropicamida | Midriático y ciclopléjico tópico de acción corta para exploración del fondo de ojo. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 137 | Florfenicol + Terbinafina + Betametasona | Combinación ótica de antibiótico, antifúngico y corticoide para otitis externa en perros. Descartar rotura timpánica antes de aplicar. Registrada en Colombia para uso veterinario (ICA). |

### 3.13 Fluidoterapia y electrolitos — 7 filas

| # | `name` | `description` |
|---|---|---|
| 138 | Solución de lactato de Ringer (Hartmann) | Cristaloide isotónico balanceado. Fluidoterapia de reposición, deshidratación y soporte perioperatorio. Registrada en Colombia para uso veterinario (ICA). |
| 139 | Cloruro de sodio (solución salina) | Cristaloide isotónico. Fluidoterapia de reposición, hiponatremia y dilución de fármacos. Registrado en Colombia para uso veterinario (ICA). |
| 140 | Dextrosa | Solución de glucosa para hipoglucemia y aporte calórico, y como aditivo de fluidoterapia. Registrada en Colombia para uso veterinario (ICA). |
| 141 | Cloruro de potasio | Electrolito para la corrección de la hipopotasemia, siempre diluido en fluidoterapia. NUNCA administrar en bolo intravenoso: es letal. Registrado en Colombia para uso veterinario (ICA). |
| 142 | Gluconato de calcio | Electrolito para hipocalcemia y eclampsia puerperal, y como cardioprotector en hiperpotasemia. Administración intravenosa lenta con monitorización cardíaca. Registrado en Colombia para uso veterinario (ICA). |
| 143 | Bicarbonato de sodio | Alcalinizante para acidosis metabólica grave documentada por gasometría. Registrado en Colombia para uso veterinario (ICA). |
| 144 | Manitol | Diurético osmótico para hipertensión intracraneal y edema cerebral. Contraindicado en pacientes deshidratados o con hemorragia intracraneal activa. Sin registro veterinario específico verificado en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |

### 3.14 Urgencias, hemostasia y antídotos — 9 filas

| # | `name` | `description` |
|---|---|---|
| 145 | Adrenalina | Simpaticomimético de urgencia. Parada cardiorrespiratoria y anafilaxia. Registrada en Colombia para uso veterinario (ICA). |
| 146 | Fitomenadiona | Vitamina K1. Antídoto de la intoxicación por rodenticidas anticoagulantes y de las coagulopatías por déficit de vitamina K. Registrada en Colombia para uso veterinario (ICA). |
| 147 | Ácido tranexámico | Antifibrinolítico para el control de hemorragias. Sin registro veterinario en Colombia: uso por extrapolación humana bajo criterio del médico veterinario. |
| 148 | Carbón activado | Adsorbente gastrointestinal para el manejo de intoxicaciones por vía oral. Registrado en Colombia para uso veterinario (ICA). |
| 149 | N-acetilcisteína | Mucolítico y precursor del glutatión; antídoto de la intoxicación por paracetamol, especialmente en gatos. Registrada en Colombia para uso veterinario (ICA). |
| 150 | Doxapram | Estimulante respiratorio central para depresión respiratoria y reanimación neonatal. Registrado en Colombia para uso veterinario (ICA). |
| 151 | Oxitocina | Hormona uterotónica. Inercia uterina y estimulación de la bajada de la leche. Contraindicada ante obstrucción del canal del parto. Registrada en Colombia para uso veterinario (ICA). |
| 152 | Sulfato de magnesio | Electrolito y antiarrítmico en hipomagnesemia y arritmias refractarias. Registrado en Colombia para uso veterinario (ICA). |
| 153 | Tiamina | Vitamina B1. Deficiencia de tiamina, especialmente en gatos, con signos neurológicos. Registrada en Colombia para uso veterinario (ICA). |

### 3.15 Antisépticos y tópicos de herida — 5 filas

| # | `name` | `description` |
|---|---|---|
| 154 | Neomicina | Antibiótico aminoglucósido de uso tópico en piel, oído y ojo. Registrada en Colombia para uso veterinario (ICA). |
| 155 | Polimixina B | Antibiótico polipeptídico de uso tópico frente a gramnegativos, en formulaciones óticas y oftálmicas. Registrada en Colombia para uso veterinario (ICA). |
| 156 | Clotrimazol | Antifúngico imidazólico de uso tópico y ótico. Dermatofitosis, malasseziosis y otitis por levaduras. Registrado en Colombia para uso veterinario (ICA). |
| 157 | Sulfadiazina de plata | Antibacteriano tópico de elección en quemaduras y heridas extensas. Registrada en Colombia para uso veterinario (ICA). |
| 158 | Toltrazurilo | Antiprotozoario triazinona para coccidiosis (*Isospora*) en cachorros. Registrado en Colombia para uso veterinario (ICA). |

### 3.16 Eutanásicos — 2 filas

> Sección de máxima sensibilidad. Ambas moléculas están sometidas a fiscalización ante el FNE y su
> uso está restringido al médico veterinario.

| # | `name` | `description` |
|---|---|---|
| 159 | Pentobarbital sódico | Barbitúrico de acción ultracorta empleado en eutanasia y como anticonvulsivante. SUSTANCIA SOMETIDA A FISCALIZACIÓN ante el Fondo Nacional de Estupefacientes; administración exclusiva por médico veterinario. Registrado en Colombia para uso veterinario (ICA). |
| 160 | Pentobarbital sódico + Difenilhidantoína | Solución eutanásica para perros y gatos. SUSTANCIA SOMETIDA A FISCALIZACIÓN ante el Fondo Nacional de Estupefacientes; administración exclusiva por médico veterinario. Registrada en Colombia para uso veterinario (ICA). |

**Total: 160 filas** — de las cuales **154 nuevas** y 6 preexistentes (§8.2).

### 3.17 Comprobación de la lista — hecha por script, no a ojo

`db-migrations` **no tiene que repetir esto**. Se ejecutó sobre las 160 filas extraídas de las
tablas de §3.1 a §3.16 de este mismo fichero, no sobre una copia.

**Criterio de normalización aplicado**, que es el que emula `utf8mb4_0900_ai_ci`:
descomposición Unicode `NFD`, descarte de todo carácter de categoría `Mn` (las marcas diacríticas) y
paso a minúsculas. Es decir, `key("Pimobendán") == key("PIMOBENDAN") == key("pimobendan")`.

| Comprobación | Resultado |
|---|---|
| Filas parseadas y numeración contigua 1..160 | ✅ **160**, sin huecos ni repeticiones |
| `name` > 200 caracteres (`Medicament.java:46-47`) | **ninguno**. Máximo real: **40** (`Florfenicol + Terbinafina + Betametasona`) |
| `description` > 500 caracteres (`Medicament.java:48-49`) | **ninguna**. Máxima real: **283** (fila 23, permetrina) |
| **Duplicados bajo la clave normalizada `ai_ci`** | **ninguno** |
| Nombres que contengan un dígito | **ninguno** — ninguna fila lleva concentración |
| Nombres que contengan `%`, `mg`, `mL` o `g` | **ninguno** — ninguna fila lleva unidad |
| Separador de combinaciones | **` + ` en las 14 combinaciones**, sin excepción. Ningún `-`, `/` o `,` como separador (el único `-` del catálogo es intrasilábico: `N-acetilcisteína`, `S-metopreno`) |
| Descripciones con patrón de dosis (`\d+\s*(mg\|mL\|g\|mcg\|UI\|kg)`, `mg/kg`) | **ninguna** |
| Las 6 filas ya sembradas por el changeset `173` | **6 de 6** contempladas, con su nombre exacto |

**Un caso evaluado y aceptado a propósito:** diez nombres son prefijo de otro
(`Praziquantel` ⊂ `Praziquantel + Pirantel` ⊂ `Praziquantel + Pirantel + Febantel`;
`Amoxicilina` ⊂ `Amoxicilina + Clavulánico`; `Ketamina` ⊂ `Ketamina + Midazolam`…). **No es una
colisión** —son cadenas distintas y la constraint las admite— y es deliberado: la molécula sola y la
combinación fija registrada son productos distintos que el veterinario receta en escenarios
distintos. Se deja constancia para que nadie lo «arregle» fusionándolos.

---

## 4. Lista de exclusión — lo que NO se siembra, y por qué

Esto es parte del entregable, no un apéndice. Un catálogo clínico se define tanto por lo que ofrece
como por lo que se niega a ofrecer.

| Molécula | Por qué NO entra |
|---|---|
| **Paracetamol / acetaminofén** | **Contraindicado en gatos.** El felino tiene «una actividad reducida de la glucoroniltransferasa»; produce metahemoglobinemia y necrosis hepática. Ofrecerlo en un desplegable a un clic de una receta felina es un riesgo que el esquema actual **no puede** mitigar (no hay campo de especie contraindicada — issue #567) |
| **Ibuprofeno** | «Ya no se recomienda el uso de ibuprofeno en perros, ya que puede provocar úlceras y perforaciones gástricas»; los gatos lo toleran aún peor, «con aproximadamente la mitad de la dosis requerida para causarla en los perros» |
| **Ácido acetilsalicílico, naproxeno, diclofenaco, nimesulida** | Mismo razonamiento: AINEs humanos con margen terapéutico estrecho o nulo en pequeños animales, habiendo cinco AINEs veterinarios registrados en §3.4 |
| **Xilitol, cualquier presentación edulcorada de uso humano** | Hipoglucemiante y hepatotóxico en perros |
| **Nitroscanato, dinotefuran, metaflumizona, prednicarbato, heparina, clopidogrel, insulina glargina biosimilar** | Buscados uno a uno en la base del ICA: **cero registros**. Los cuatro primeros ni siquiera figuran; los dos de coagulación se quedan fuera por no poder verificarse. *(Insulina glargina sí entra como fila 113, marcada `[EXT]`, por ser estándar de manejo en el gato diabético.)* |
| **Cloranfenicol, rifampicina, amikacina** | Cero registros veterinarios vigentes en Colombia y perfil de riesgo (aplasia medular, resistencia crítica, nefro/ototoxicidad) que desaconseja ponerlos por defecto en el catálogo de toda clínica que se dé de alta |

Si una clínica necesita alguna de estas, **puede crearla como medicamento propio**
(`general = false`, su `company_id`). La semilla global es el mínimo seguro, no el techo.

---

## 5. Problema 1 — `uq_medicaments_name` es único GLOBAL

**Evidencia:**
`173_create_medicaments.xml:17-19` declara
`<constraints nullable="false" unique="true" uniqueConstraintName="uq_medicaments_name"/>`, y
`MedicamentJpaEntity.java:18` lo repite con `@Column(nullable = false, length = 200, unique = true)`.
No hay ninguna constraint `(company_id, name)` en el árbol de migraciones.

**Qué se rompe, exactamente.** Hay tres efectos y conviene no confundirlos:

1. **La semilla no impide a la clínica usar el medicamento.** `findAllByGeneralTrueOrCompany_Id`
   (`MedicamentJpaRepository.java:46`) le devuelve las 160 globales más las suyas. La semilla
   *habilita*, no bloquea el uso.
2. **La semilla sí impide a la clínica crear una fila propia con ese nombre.**
   `CreateMedicamentService.execute` (`CreateMedicamentService.java:26-34`) **no comprueba
   duplicados**: llama a `repository.save` directo. El choque lo produce la base, y
   `GlobalExceptionHandler:1768-1798` lo convierte en **409 `DATA_INTEGRITY_VIOLATION`
   «Database constraint violation»** — la rama de *constraint sin mapeo de negocio*, con un
   `log.warn` de «Unmapped data integrity violation». El usuario ve un error genérico que no dice
   «ese nombre ya existe».
3. **El problema de verdad no lo causa la semilla: es entre tenants.** Si la Clínica A crea
   «Metacam», la Clínica B **no puede crearlo nunca**, y ni siquiera puede verlo para entender por
   qué. Y como `@SQLDelete` hace borrado lógico (`MedicamentJpaEntity.java:11`), la fila
   deshabilitada **sigue ocupando el índice único**: exactamente el defecto ya reportado para los
   tipos de spa en el **issue #482** y para `quotes.client_request_id` en el **#427**
   (*«El índice único de client_request_id en quotes es global y deja bloqueado permanentemente al
   segundo tenant que reutilice una llave»*, cerrado).

**Cuántas filas es prudente sembrar: 160, y la decisión de la §1 es lo que lo hace prudente.**
El coste de la semilla es «160 nombres de DCI quemados para todos». Ese coste es **aceptable y casi
inocuo** porque una clínica no tiene motivo para querer una fila privada llamada `Meloxicam` a secas
—ya la tiene—, y porque bajo `utf8mb4_0900_ai_ci` esos 160 nombres son justo las variantes que
ninguna clínica querría en privado. Si las filas fueran marcas, el coste sería «miles de nombres
comerciales quemados», y ahí sí la semilla se comería el espacio de nombres del cliente. **Sembrar
marcas es lo que había que evitar, y se evitó.**

**No obstante, la constraint sigue mal puesta** y hay que arreglarla con independencia de la semilla.
Se registró **como comentario en el issue #557** —abierto por otro agente de esta tanda para
`vaccination_types.name`, que es exactamente el mismo defecto en otra tabla— en vez de abrir un
issue gemelo. Ahí está la evidencia de `medicaments`, el DDL propuesto y las tres dimensiones que
este catálogo añade: es el más largo del sistema, la collation `ai_ci` le pega más fuerte que a
ningún otro por la nomenclatura farmacológica, y `CreateMedicamentService.java:26-34` tampoco
pre-comprueba el nombre.

La forma correcta de arreglarlo, y es la que ya usa la casa: un **índice único parcial emulado**
con columna generada, el patrón de los changesets `206`, `210` y `226`
(`GENERATED ALWAYS AS (…) STORED` que vale `NULL` fuera de alcance, protegido por una `preCondition`
`sqlCheck` con `onFail="HALT"`). MySQL no tiene índices parciales ni filtrados; la columna generada
es la única forma. Esbozo, **para que lo escriba `db-migrations`, no para copiar y pegar**:

```
-- La unicidad real es: nombre único DENTRO del ámbito (plataforma o empresa) y solo entre lo vigente.
ALTER TABLE medicaments
  ADD COLUMN uq_scope VARCHAR(220)
      GENERATED ALWAYS AS (
          CASE WHEN enabled = 1
               THEN CONCAT(COALESCE(company_id, 0), ':', name)
          END
      ) STORED;
CREATE UNIQUE INDEX uq_medicaments_scope_name ON medicaments (uq_scope);
DROP INDEX uq_medicaments_name ON medicaments;   -- solo tras validar que no hay colisiones
```

Coste del `ALTER`: **añadir una columna generada `STORED` reconstruye la tabla** (no es `INSTANT`)
según la tabla de operaciones DDL online de InnoDB; con la tabla en el orden de las centenas de filas
es irrelevante, pero debe declararse en el changeset. **Esto no es parte de la semilla** y debe ir en
su propio changeset, antes o después, nunca mezclado.

---

## 6. Problema 2 — el esquema no tiene campos clínicos

**Evidencia:** la tabla completa es `id, name, description, company_id, general, created_date,
version, enabled` (`173_create_medicaments.xml:12-33` + `225_add_version_optimistic_lock_wave2.xml:558`).
No hay principio activo separado del nombre, ni concentración, ni vía, ni forma, ni código **ATCvet**,
ni marca de **control especial**, ni **especie contraindicada**, ni número de **registro ICA**.

**Qué limita, con el caso concreto.** Las filas 23, 24 y 25 de §3.2 (permetrina, deltametrina,
amitraz) llevan la advertencia felina **como prosa dentro de `description`**. Eso significa que:

- el front **no puede** poner un aviso bloqueante ni un color cuando el paciente de la consulta es un
  gato: tendría que hacer *string matching* sobre texto libre en español;
- el sistema **no puede** exigir el flujo de receta oficial del FNE para las filas de control
  especial (63, 64, 65, 66, 159, 160), porque «control especial» no es un dato consultable;
- el catálogo **no puede** avisar de que un registro ICA fue cancelado (el 14,2 % de la base lo
  está), porque no guarda el registro;
- agrupar por grupo terapéutico en la interfaz exige volver a parsear `description`.

**Aviso importante para quien vaya a modelar esto:** la columna `TIPO DE MEDICAMENTOS ICA` de la base
oficial **no sirve** como grupo terapéutico. Verificado fila a fila: `CERENIA` (maropitant) figura
como *«Homeopáticos»*, `APOQUEL` (oclacitinib) como *«Antibióticos»*, `TRAMADOL COMPRIMIDOS BROUWER`
como *«Cosméticos»* y `ZOLETIL 50` como *«Antihelmínticos»*. La agrupación de §3 es propia y está
hecha por criterio farmacológico, no copiada del ICA. Si algún día se importa la base del ICA, esa
columna hay que descartarla.

**No propongo el cambio de esquema en este documento** —es una decisión de producto, no de semilla—,
pero queda abierto como **issue #567**, con el modelo mínimo sugerido: `atcvet_code VARCHAR(10)`,
`controlled_substance BOOLEAN NOT NULL DEFAULT FALSE`, `ica_registration VARCHAR(20)` y una tabla
hija `medicament_species_warnings (medicament_id, species, severity, note)`.

---

## 7. Verificación — qué se comprobó y contra qué

**Método.** Se descargó la base oficial **PRODUCTOS VETERINARIOS REGISTRADOS (publicada el 3 de
agosto de 2026)** desde el portal del ICA, se convirtió a CSV y se buscó cada molécula por las
columnas `PRODUCTO` y `ACTIVOS`, con normalización de acentos y mayúsculas, filtrando por
`ESTADO ICA = VIGENTE` y comprobando que `ESPECIES (DESTINO)` incluyera caninos o felinos.

- **9.478** productos en el archivo · **8.129 VIGENTE** · **1.349 CANCELADO**.
- Columnas del archivo: `REG. ICA No`, `NOMBRE DEL TITULAR`, `ESTADO ICA`, `PRODUCTO`, `ACTIVOS`,
  `CANTIDADES`, `INDICACIONES`, `PRECAUCIONES`, `LABORATORIO PRODUCTOR`, `PAIS ORIGEN`,
  `NOMBRE DEL IMPORTADOR`, `ENVASES O EMPAQUES`, `ESPECIES (DESTINO)`, `TIEMPO DE RETIRO`,
  `TIPO DE MEDICAMENTOS ICA`.

**Muestra del rastro de verificación** (registro ICA vigente concreto que respalda la marca `[ICA]`;
la tabla completa se puede regenerar con el método descrito):

| Molécula | Productos vigentes con caninos/felinos | Ejemplo verificado |
|---|---|---|
| Fluralaner | 20 | `9561-MV` BRAVECTO |
| Afoxolaner | 3 | `9566-MV` NEXGARD |
| Sarolaner | 8 | `10233-MV` SIMPARICA |
| Lotilaner | 12 | `9871-MV` CREDELIO |
| Selamectina | 9 | `5368-DB` REVOLUTION 6% |
| Permetrina | 47 | `3202-DB` PUL-NOC |
| Cefovecina | 1 | `7916-MV` CONVENIA (ZOETIS INC) |
| Marbofloxacina | 16 | `9275-MV` ZENIQUIN |
| Clindamicina | 5 | `7738-MV` CLINDAVET |
| Meloxicam | 54 | `6226-MV` DOLOMEX |
| Carprofeno | 23 | `5269-DB` RIMADYL |
| Firocoxib | 2 | `9987-MV` PREVICOX |
| Robenacoxib | 3 | `10596-MV` ONSIOR |
| Grapiprant | 3 | `10560-MV` GALLIPRANT |
| Tramadol | 3 | `10001-MV` / `10002-MV` TRAMADOL BROUWER |
| Butorfanol | 1 (+1 equino) | `7706-MV` BUTORMIN |
| Ketamina | 9 | `3204-DB` KETAMINA 50 · `7346-MV` KETAMID (con midazolam) |
| Tiletamina + Zolazepam | 2 | `5065-DB` ZOLETIL 50 (VIRBAC) |
| Propofol | 1 | `10090-MV` PROPOFOL RICHMOND |
| Isoflurano | 1 | `11164-MV` ISOFLURANO (PISA) |
| Dexmedetomidina | 3 | `11715-MV` SLEEPET 0,5 |
| Atipamezol | 1 | `11659-MV` REVERSIK VET |
| Maropitant | 2 | `8946-MV` CERENIA · `11428-MV` MAROPITANT (ERMA) |
| Ranitidina | 5 | `9764-MV` RANIDIN V (BUSSIE) |
| Pimobendán | 10 | `10281-MV` VETMEDIN (BOEHRINGER) |
| Torasemida | 3 | `10879-MV` TORACARD |
| Amlodipino | 1 | `10427-MV` AMLODIPINO 5 MG |
| Insulina porcina zinc | 1 | `8699-MV` CANINSULIN (INTERVET) |
| Levotiroxina | 11 | `9348-MV` THYRO-TABS |
| Oclacitinib | 2 | `9677-MV` APOQUEL · `11157-MV` APOQUEL masticable |
| Lokivetmab | 1 | `10356-BV` CYTOPOINT |
| Itraconazol | 4 | `10826-MV` ITRACONAZOL (PROVET) |
| Aceponato de hidrocortisona | 4 | `8165-MV` CORTAVANCE |
| Dorzolamida | 2 | `7686-MV` DORZOLAMIDA 2% |
| Florfenicol + Terbinafina + Betametasona | 2 | `9762-MV` OSURNIA |
| Lactato de Ringer | 5 | `2139-DB` LACTATO DE RINGER (HARTMANN) CORPAUL |
| Pentobarbital | 5 | `3342-DB` EUTHANEX (INVET) · `10240-MV` EUTANÁSICO BROUWER |

**Moléculas verificadas con resultado CERO registros vigentes → marcadas `[EXT]`:**
buprenorfina, bupivacaína, sevoflurano, diazepam, fenobarbital, ondansetrón, metilprednisolona
(el único registro está `CANCELADO`), metimazol/tiamazol, trilostano, desmopresina, digoxina,
atenolol, gabapentina, hidroxicina, latanoprost, tropicamida, lactulosa, ácido ursodesoxicólico,
pancreatina, ácido tranexámico, apomorfina, insulina glargina, mirtazapina, capromorelina,
amikacina, cloranfenicol, rifampicina, nitroscanato, dinotefuran, metaflumizona, prednicarbato,
heparina, clopidogrel.

---

## 8. Especificación del changeset — para `db-migrations`

`db-migrations` es quien escribe esto. Aquí va lo que necesita y **por qué**.

### 8.1 Numeración y ubicación
- Último changeset del árbol: **`284_seed_platform_access_switch.xml`**. El siguiente libre es
  **`285`**. Nombre propuesto: `285_seed_global_medicament_catalog.xml`, declarado en
  `db.changelog-master.xml` (que también es territorio exclusivo de `db-migrations`).

### 8.2 El estado de partida — esto es lo que hace fallar el despliegue si se ignora
La tabla **no está vacía**. `173_create_medicaments.xml` ya sembró **6 filas globales**
(líneas 37-72) con estos nombres exactos:

```
Amoxicilina + Clavulánico   Meloxicam   Metronidazol   Omeprazol   Sucralfato   Maropitant
```

y, además, `173d_backfill_medicament_prescriptions` (líneas 82-90) inserta como **globales** todos
los nombres distintos que ya se hubieran recetado en cada entorno:

```sql
INSERT INTO medicaments (name, general, enabled, created_date)
SELECT DISTINCT mp.name, true, true, NOW() FROM medicament_prescriptions mp ...
```

Es decir: **el contenido actual de la tabla depende del entorno y no es conocible desde el árbol.**
Dev y producción pueden tener nombres que no están en ningún changeset.

**Consecuencia obligatoria:** el changeset **no puede** usar `<insert>` de Liquibase, que no admite
condición. Debe ser `<sql>` idempotente:

```sql
INSERT INTO medicaments (name, description, company_id, general, created_date, version, enabled)
SELECT t.name, t.description, NULL, TRUE, NOW(), 0, TRUE
  FROM (SELECT 'Fenbendazol' AS name, 'Antiparasitario interno …' AS description
        UNION ALL SELECT 'Albendazol', '…'
        …) AS t
 WHERE NOT EXISTS (SELECT 1 FROM medicaments m WHERE m.name = t.name);
```

Es el mismo estilo `SELECT … UNION ALL` que ya usan `258_seed_technical_catalog.xml:51` y
`275_seed_clinical_care_permissions.xml:110-114`, así que no introduce un patrón nuevo.

**Las 6 filas preexistentes** se adoptan con su nombre actual (por eso §3.3 dice
`Amoxicilina + Clavulánico` y no «Amoxicilina + Ácido clavulánico»: el nombre ya existe y renombrarlo
rompería cualquier `medicament_prescriptions` que lo referencie por FK). Solo se les rellena la
descripción, y **solo si está vacía**, para no pisar lo que un `SYSTEM` haya editado:

```sql
UPDATE medicaments SET description = '…', version = version + 1
 WHERE name = 'Meloxicam' AND general = TRUE AND description IS NULL;
```

El `version = version + 1` **no es opcional**: la entidad tiene `@Version`
(`MedicamentJpaEntity.java:34-36`) y la regla dura de ArchUnit **`UPDATE_MASIVO_MUEVE_LA_VERSION`**
(BE-26) exige que todo `UPDATE` masivo mueva la versión. Sin eso, una instancia con la fila cargada
en memoria reescribe la descripción en silencio.

### 8.3 Valores columna a columna

| Columna | Valor | Por qué |
|---|---|---|
| `name` | Tabla §3 | ≤200 (`Medicament.java:46-47`); máx. real 48 |
| `description` | Tabla §3 | ≤500 (`Medicament.java:48-49`); máx. real 362 |
| `company_id` | `NULL` | `Medicament.validate` lanza `general medicament cannot have company` si `general && company != null` (`Medicament.java:50-51`) |
| `general` | `TRUE` | ídem. `general = false` sin empresa también lanza (`:52-53`) |
| `created_date` | `NOW()` | `NOT NULL` sin default en el modelo de dominio |
| `version` | `0` | `NOT NULL` (`225_add_version_optimistic_lock_wave2.xml:558`). **No dejar que lo ponga la base**: la fila debe nacer con versión conocida |
| `enabled` | `TRUE` | `@SQLRestriction("enabled = true")` la oculta si no |

**`version` a `0`, no a `NULL`:** aquí se cruzan dos reglas duras, `ENTIDADES_CON_BLOQUEO_OPTIMISTA`
y `BORRADO_LOGICO_RESPETA_LA_VERSION`, y el `@SQLDelete` de la entidad es
`… WHERE id = ? AND version = ?` (`MedicamentJpaEntity.java:11`): con `version` nula, el borrado
lógico de esa fila no casaría nunca.

### 8.4 `preConditions`

Siguiendo el patrón de la casa (`206`, `210`, `226`), con `onFail="HALT"`:

1. `tableExists tableName="medicaments"`.
2. `columnExists` de `version` — si el `225` no ha corrido, el `INSERT` con `version` falla en frío.
3. `sqlCheck expectedResult="0"` sobre
   `SELECT COUNT(*) FROM medicaments WHERE general = TRUE AND company_id IS NOT NULL`,
   como red de seguridad del invariante de dominio antes de añadir 154 filas más.

### 8.5 `<rollback>`

Explícito y acotado a lo que este changeset creó. **No vale `DELETE FROM medicaments`:**

```sql
DELETE FROM medicaments
 WHERE general = TRUE
   AND company_id IS NULL
   AND name IN ('Fenbendazol', 'Albendazol', …)   -- las 154 nuevas, no las 6 previas
   AND NOT EXISTS (SELECT 1 FROM medicament_prescriptions mp
                    WHERE mp.medicament_id = medicaments.id);
```

La cláusula `NOT EXISTS` es imprescindible: `medicament_prescriptions.medicament_id` es
`NOT NULL` con FK a `medicaments` (`173e`, líneas 92-98). Si alguien recetó con una fila sembrada, el
rollback **debe** dejarla y no romper la receta de un paciente. El rollback de las 6 descripciones es
`UPDATE … SET description = NULL, version = version + 1 WHERE name IN (…)`.

### 8.6 Migración y coste operativo

- **No hay `ALTER`**: es solo DML. No aplica *expand/contract*, no hay reconstrucción de tabla, no
  hay ventana de indisponibilidad.
- **154 `INSERT`** en una transacción es perfectamente asumible; no hace falta *backfill* por lotes
  (ese criterio aplica a `UPDATE` sobre tablas grandes, no a esto).
- **Impacto en lectura:** `findAllAvailableForCompany` devuelve una `List` **sin paginar**
  (`JpaMedicamentRepository.java:63-66`). Tras la semilla, cada carga del selector de medicamentos
  traerá ≈160 filas con su descripción: del orden de **60 KB de JSON**. Es asumible y **no** es
  motivo para recortar el catálogo, pero es el techo: si el catálogo global creciera a millares
  —justo lo que pasaría si las filas fueran marcas— ese endpoint habría que paginarlo.
- **Índices:** ninguno nuevo. `uq_medicaments_name` ya sirve la búsqueda por nombre y la FK
  `fk_medicaments_company` crea su índice en `company_id` automáticamente en InnoDB. Con 160 filas,
  cualquier índice adicional cuesta más de lo que ahorra. **Si algún día se pagina el listado por
  empresa, el índice correcto sería `(company_id, general, enabled, name)`** —igualdad primero, orden
  al final—, no antes.

### 8.7 Lo que este documento NO decide y `db-migrations` no debe inventar

- El cambio de `uq_medicaments_name` a unicidad por ámbito (§5) va en **otro** changeset, después de
  que se resuelva el issue #557. **No mezclar DDL con esta semilla.**
- Los campos clínicos nuevos (§6) son decisión de producto pendiente del issue #567.

---

## 9. Fuentes

Todas consultadas y verificadas vivas el **2026-08-25**.

**Autoridad regulatoria colombiana**

| Fuente | Qué sostiene | URL |
|---|---|---|
| ICA — Grupo de Registro de Medicamentos y Biológicos de uso veterinario | Que el ICA, y no el INVIMA, «registra, inspecciona, vigila y controla los medicamentos y biológicos veterinarios». Marco: Resolución 62542 de 2020 (registro), 62770 de 2020 (homeopáticos), 1056 de 1996 (control técnico de insumos pecuarios) | https://www.ica.gov.co/areas/pecuaria/servicios/regulacion-y-control-de-medicamentos-veterinarios |
| **ICA — Base de datos de productos veterinarios registrados, publicada 2026-08-03** | **La fuente primaria de este documento.** 9.478 productos con registro, titular, activos, especies y estado | https://www.ica.gov.co/areas/pecuaria/servicios/regulacion-y-control-de-medicamentos-veterinarios/listados-vigentes |
| ICA — Tablero de analítica de productos veterinarios registrados | Consulta interactiva de la misma base | *(enlace directo omitido: su parámetro `r=` es un token base64 que los escáneres de secretos marcan como clave. El tablero se abre desde la página del ICA de la fila anterior, en «Consulte aquí el tablero de productos registrados».)* |
| **Resolución 315 de 2020 (MinSalud) — Anexo Técnico 3** | Que los medicamentos de control especial **de uso veterinario** son exactamente butorfanol 10 mg/mL, ketamina 50 y 100 mg/mL, y ketamina + midazolam (50+2) mg/mL. Anexo 1: 396 sustancias fiscalizadas; Anexo 2: 11 de monopolio del Estado | https://normograma.invima.gov.co/compilacion/docs/resolucion_minsaludps_0315_2020.htm |
| Fondo Nacional de Estupefacientes — ABECÉ de medicamentos de control especial | Autoridad y obligaciones de custodia y reporte. *(El PDF no se pudo extraer como texto; el contenido se sostuvo con la Resolución 315 y su compilación en el normograma del INVIMA.)* | https://fne.minsalud.gov.co/ |
| Resolución 1478 de 2006 (MinSalud) | Que el nivel de control del tramadol se mantiene según lo estipulado desde esta resolución | https://faolex.fao.org/docs/pdf/col74701.pdf |
| INVIMA — Alerta sanitaria, retiro de ranitidina por NDMA (2020) | La nota de la fila 94 | https://www.invima.gov.co/biblioteca/alerta-sanitaria-retiro-ranitidina-ndma-2020 |

**Referencia farmacológica y toxicológica**

| Fuente | Qué sostiene | URL |
|---|---|---|
| Manual de Veterinaria MSD/Merck — Toxicosis por analgésicos de uso humano | Paracetamol en gatos: «una actividad reducida de la glucoroniltransferasa» → metahemoglobinemia. Ibuprofeno: «Ya no se recomienda el uso de ibuprofeno en perros»; gatos afectados «con aproximadamente la mitad de la dosis» | https://www.msdvetmanual.com/es/toxicolog%C3%ADa/toxicosis-por-analg%C3%A9sicos-de-uso-humano/toxicosis-en-animales-por-analg%C3%A9sicos-de-uso-humano |
| Manual de Veterinaria Merck — AINEs en animales | Perfil y contraindicaciones de los AINEs veterinarios de §3.4 | https://www.merckvetmanual.com/es-us/farmacolog%C3%ADa/inflamaci%C3%B3n/f%C3%A1rmacos-antiinflamatorios-no-esteroideos-en-animales |
| AEMPS — Nota informativa sobre reacciones adversas en gatos a permetrina | Sustento regulatorio de la advertencia felina de las filas 23 y 24 | https://www.aemps.gob.es/informa/notasinformativas/medicamentosveterinarios-1/seguridad-2/2004/ni-permetrina/ |
| Parasitipedia — Ficha toxicológica de permetrina | «los gatos no toleran la permetrina»; DL50 aguda en gatos 100 mg/kg; mecanismo neurotóxico piretroide | https://parasitipedia.net/index.php?option=com_content&view=article&id=427&Itemid=2487 |
| FDA — Hoja informativa sobre isoxazolinas (Bravecto, Credelio, NexGard, Simparica) | La advertencia de eventos neurológicos de las filas 18-21 | https://www.fda.gov/animal-veterinary/animal-health-literacy/hoja-informativa-para-duenos-de-mascotas-y-veterinarios-acerca-de-los-posibles-eventos-adversos |
| MSD Salud Animal Colombia — Bravecto | Comercialización en Colombia; «aprobado por el ICA, FDA y EMA» | https://www.msd-salud-animal.com.co/productos/bravecto/ |
| APROVET — Asociación Nacional de Laboratorios de Productos Veterinarios | El vademécum veterinario de circulación en Colombia (VadeAprovet); única asociación del país que agrupa a los productores | https://aprovet.com/ |
| INVET Colombia — Euthanex | Producto eutanásico con **registro ICA 3342-DB**, pentobarbital sódico + difenilhidantoína, caninos y felinos | https://invetcolombia.com.co/producto/euthanex/ |

**Criterio de modelado**

| Fuente | Qué sostiene | URL |
|---|---|---|
| MySQL 8.4 — Generated columns | El patrón de unicidad condicional de §5 (los changesets 206/210/226 de la casa) | https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html |
| MySQL 8.4 — InnoDB online DDL operations | Que añadir una columna generada `STORED` reconstruye la tabla | https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html |
| MySQL 8.4 — Multiple-column indexes | La regla del prefijo por la izquierda que respalda `(company_id, general, enabled, name)` en §8.6 | https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html |
| Bill Karwin, *SQL Antipatterns* — «Metadata Tribbles» | Por qué la concentración no va como fila del catálogo (§1.1) | https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/ |
| Citus — Multi-tenant data modeling | «El tenant va primero en la clave»; sustento de la unicidad por ámbito de §5 | https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html |
| Liquibase — Best practices | Changesets idempotentes y con `rollback` explícito (§8.2, §8.5) | https://docs.liquibase.com/concepts/bestpractices.html |

---

## 10. Qué NO se comprobó

Se dice explícitamente para que nadie lo dé por bueno:

1. **Yo no consulté ninguna base de datos viva**, ni local ni de dev. Todo el estado del esquema de
   este documento sale de leer los changesets y las entidades. **La única excepción es la
   collation**, que sí está verificada contra un servidor —`@@collation_server`, el default del
   esquema `vetsoftware` y una columna `name` real, los tres dan `utf8mb4_0900_ai_ci`—, pero **la
   verificación la aportó otro agente de esta misma tanda, no yo**. Se toma como dato firme y se usa
   en §1.5 y §3.17; si alguien necesita reconfirmarlo, es `SELECT @@collation_database;` más
   `information_schema.columns` sobre `medicaments.name`.
2. **No se ejecutó ningún `EXPLAIN`.** El apartado de índices de §8.6 es razonamiento sobre 160
   filas, no medición. Con esa cardinalidad ningún plan es interesante.
3. **No se verificó el registro ICA de cada marca comercial**, solo el de cada **molécula**. Es lo
   coherente con la decisión de §1: el catálogo no guarda marcas.
4. **No se comprobó la fecha de vencimiento de cada registro ICA.** El archivo trae `ESTADO ICA`
   (`VIGENTE`/`CANCELADO`) pero no fecha de expiración; un registro vigente hoy puede caducar. Sin
   campo `ica_registration` en el esquema (§6), el sistema no tiene forma de detectarlo. La semilla
   es una foto del **2026-08-03**.
5. **El PDF del ABECÉ del FNE no se pudo extraer como texto** (llegó como binario). Las marcas
   `[FNE]` se sostienen en la Resolución 315 de 2020 compilada en el normograma del INVIMA, que es
   la fuente normativa y es mejor.
6. **No se levantó la aplicación ni se ejecutó `mvn verify`.** Las reglas ArchUnit citadas
   (`UPDATE_MASIVO_MUEVE_LA_VERSION`, `ENTIDADES_CON_BLOQUEO_OPTIMISTA`,
   `BORRADO_LOGICO_RESPETA_LA_VERSION`) se aplicaron como criterio de diseño, no se comprobaron
   ejecutándolas.
7. **La revisión clínica del texto de las 160 descripciones no la ha hecho un médico veterinario.**
   Cada indicación está respaldada por las fuentes de §9, pero **este documento debe pasar por
   revisión de un MV colegiado antes de que el changeset entre a producción**, porque lo que se
   siembra acaba en la receta de un paciente real.
