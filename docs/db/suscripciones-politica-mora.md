# Política de mora y cobranza — PROPUESTA PENDIENTE DE APROBACIÓN

> ⚠️ **Estado: PROPUESTA. Nada de este documento está implementado ni aprobado.**
> Los únicos números que hoy corren en producción son los que están en la tabla
> `platform_billing_config` y en `subscriptions.grace_days`, y la sección
> «[Lo que el motor hace hoy](#1-lo-que-el-motor-hace-hoy-medido-no-supuesto)»
> describe exactamente lo que hacen. Todo lo demás es una propuesta para que el
> usuario apruebe, cambie o rechace número a número.
>
> Redactado el 2026-08-24 tras auditar el slice `dunning`, que se escribió fuera
> del alcance declarado en `suscripciones-modelo.md` (ese documento pone la
> aritmética de la mora explícitamente fuera de alcance) y que **nunca se había
> revisado ni ejercitado contra datos reales**. Contexto en la incidencia #397.

---

## 0. La restricción que no se negocia (R18)

**No existe, ni debe implementarse, un estado de corte total de acceso.** El
máximo grado de restricción es «solo lectura»: el cliente moroso consulta e
imprime toda su información —incluida la historia clínica— y no puede crear ni
modificar.

Esto no es una preferencia de diseño: dejar a una clínica sin acceso a su propia
historia clínica es un riesgo legal real y una reclamación garantizada. Cualquier
escalón que este documento proponga se lee **por debajo de esa línea**, y un
escalón que la cruce está mal propuesto aunque sea eficaz cobrando.

**Vocabulario prohibido en toda la consola y en todo mensaje al usuario:**
*bloquear · suspender el acceso · cortar · desactivar la cuenta · inhabilitar*.
Lo que sí se dice: *pasa a solo lectura*, *conserva la consulta y la impresión*.

---

## 1. Lo que el motor hace hoy (medido, no supuesto)

El slice `dunning` tiene tres piezas y **funcionan**, con dos huecos grandes.

| Pieza | Archivo | Qué hace |
|---|---|---|
| Evaluador | `dunning/application/usecase/DunningEvaluationService.java` | Decide el estado de un contrato a partir de la factura vencida más antigua con saldo |
| Barrido | `dunning/application/usecase/DunningEvaluationWorker.java` | Reclama un lote de facturas vencidas de todos los tenants con `FOR UPDATE SKIP LOCKED` |
| Job | `dunning/infrastructure/scheduling/DunningEvaluationJob.java` | Encadena lotes por cursor bajo principal `ROLE_SYSTEM` |

### 1.1 Las tres transiciones que existen

```
ACTIVE | TRIALING   ──(hay factura vencida)──────────────►  PAST_DUE      + GRACE_STARTED
PAST_DUE            ──(daysOverdue > grace_days)─────────►  READ_ONLY     + READ_ONLY_APPLIED
PAST_DUE | READ_ONLY──(ya no hay vencidas con saldo)─────►  ACTIVE        + REACTIVATED
CANCELLED | EXPIRED ──────────────────────────────────────►  (no se toca: terminal)
```

`daysOverdue` se cuenta **desde `due_date` de la factura hasta hoy**, y la
consulta que la selecciona exige `due_date < :today`, así que el primer día de
mora posible es `daysOverdue = 1`.

### 1.2 Los dos huecos

- **No se envía ni se registra ningún aviso.** `DunningEventType.REMINDER_SENT`
  y el enum `DunningChannel` (EMAIL, SMS, WHATSAPP, PHONE, IN_APP) existen, pero
  **el motor nunca los emite**: el único camino que escribe un `REMINDER_SENT`
  es un `POST /dunning-events` manual, cerrado a `hasRole('SYSTEM')`. Es decir:
  hoy el motor **restringe sin haber avisado**, y el expediente de cobranza
  —cuya única razón de existir es *demostrar que se avisó antes de restringir*—
  queda vacío justo en el caso en que haría falta.
- **No existe la declaración de incobrable.** `DunningEventType.WRITTEN_OFF`
  está en el enum y ningún código lo escribe.

---

## 2. De dónde sale hoy cada número, que no es de donde parece

Esta es la parte que hay que leer antes de aprobar ningún valor.

| Valor | Dónde está declarado | **De dónde lo lee el motor** |
|---|---|---|
| Días de gracia | `platform_billing_config.default_grace_days` = **5** | ❌ **No lo lee.** Lee `subscriptions.grace_days` de cada contrato |
| Día de emisión | `platform_billing_config.invoice_day_of_month` = **1** | No lo usa el motor de mora |
| Plazo de pago | `platform_billing_config.default_payment_term_days` = **0** | No lo usa el motor de mora |
| Umbral de aviso | — | **No existe** |
| Umbral de incobrable | — | **No existe** |

**El defecto que esto produce.** Que el motor lea la gracia del contrato y no de
la configuración global es *correcto* —`suscripciones-tablas.md` dice
explícitamente «por contrato, no por código: a un cliente grande se le pueden dar
15»—. El problema es **con qué valor nace ese contrato**, y hay dos caminos que
no coinciden:

- **Alta desde el registro** (`CreateInitialSubscriptionService`): lee
  `default_grace_days` de `platform_billing_config` vía
  `PlatformCatalogTemplateJpaRepository` → el contrato nace con **5**. Correcto.
- **Alta directa por API** (`CreateSubscriptionService.java:93`):
  `command.graceDays() == null ? 0 : command.graceDays()`. El campo `graceDays`
  del `CreateSubscriptionRequest` es opcional, y la columna
  `subscriptions.grace_days` tiene `DEFAULT 0`. → el contrato nace con **0**.

Un contrato nacido por el segundo camino **no tiene gracia ninguna**: la primera
factura vencida da `daysOverdue = 1 > 0` y el motor emite `GRACE_STARTED` y
`READ_ONLY_APPLIED` **en el mismo instante**, con un mensaje que dice
literalmente *«gracia de 0 dias agotada»*. Registrado como incidencia aparte.

---

## 3. La propuesta

### 3.1 Primero: separar el plazo de pago de la gracia

Hoy `default_payment_term_days = 0`, así que la factura del ciclo **vence el
mismo día en que se emite**. Eso convierte los 5 días de gracia en la *única*
ventana que tiene el cliente para pagar, y mezcla dos cosas que son distintas:

- El **plazo de pago** es comercial, va impreso en la factura y el cliente lo
  conoce. Consumirlo no es morosidad.
- La **gracia** es operativa y el cliente no la ve: es el colchón para el desfase
  entre *pagar* y *que el pago quede registrado*. En este producto ese desfase es
  real, porque `settled_amount` sube cuando alguien concilia el pago, no cuando
  el dinero sale de la cuenta del cliente.

| Parámetro | Hoy | **Propuesto** | Por qué |
|---|---|---|---|
| `default_payment_term_days` | 0 | **8** | Emisión el día 1 → vence el día 9. Cubre el primer fin de semana entero y deja ~5 días hábiles de plazo real |
| `default_grace_days` | 5 | **5 (se mantiene)** | Ver 3.2 |

### 3.2 ¿Tienen sentido 5 días de gracia? Sí — pero solo si dejan de ser el plazo de pago

**Sí, con la separación de 3.1.** El razonamiento:

- **Qué tiene que cubrir.** No el plazo de pago (eso son los 8 días), sino el
  desfase de conciliación. Una transferencia o un PSE hecho un viernes por la
  tarde se concilia el lunes o el martes. Cinco días naturales desde el
  vencimiento cubren un fin de semana completo más un día hábil de margen.
- **Qué pasa si se queda corto.** El peor fallo posible de todo este sistema no
  es que un moroso siga escribiendo un día de más: es que **un cliente que sí
  pagó pase a solo lectura**. Con la gracia como único colchón (situación de
  hoy), un festivo colombiano en lunes más el fin de semana se come 3 de los 5
  días, y el cliente que pagó el viernes entra en solo lectura el jueves. Ese
  cliente llama enfadado y tiene razón.
- **Qué pasa si se pasa.** Con un ciclo mensual, una gracia de 30 días significa
  que la factura del mes siguiente vence antes de que la primera restrinja nada:
  el cliente acumula dos meses de deuda sin haber notado una sola señal, y la
  conversación de cobro empieza cuando la cifra ya duele y la relación ya está
  rota. Cobrar tarde es cobrar peor.
- **Dónde queda el punto de restricción.** Emisión día 1 → vence día 9 → gracia
  hasta el día 14 → **solo lectura el día 15**. Media vuelta de ciclo antes de la
  factura siguiente, y dos fines de semana enteros dentro del margen.

**Si el usuario prefiere no tocar `default_payment_term_days`**, entonces 5 días
es **demasiado poco** y la recomendación cambia a **`default_grace_days = 13`**,
que reproduce el mismo día 15 con un solo parámetro. Es peor idea: mete el plazo
comercial dentro de un campo que el cliente no ve y que se llama «cortesía».

### 3.3 Los escalones

Días contados **desde `due_date`**, que es lo que el motor ya mide. `D+0` es el
día del vencimiento.

| Momento | Evento a registrar | Estado del contrato | Qué cambia para el cliente |
|---|---|---|---|
| **D−3** | `REMINDER_SENT` (canal `EMAIL` + `IN_APP`) | `ACTIVE` — sin cambio | Nada. Aviso de cortesía antes de vencer |
| **D+1** | `GRACE_STARTED` + `REMINDER_SENT` | `PAST_DUE` | **Nada.** Debe y sigue trabajando. Aviso visible en la app |
| **D+3** | `REMINDER_SENT` (2.º) | `PAST_DUE` | Nada. El aviso sube de tono, no de restricción |
| **D+6** (`> grace_days`) | `READ_ONLY_APPLIED` | `READ_ONLY` | **Deja de poder crear y modificar. Conserva la consulta y la impresión de todo, incluida la historia clínica.** Este es el máximo, y no hay nada después |
| **D+90** | `WRITTEN_OFF` | `READ_ONLY` — **sin cambio** | **Nada.** Ver 3.4 |

**Los tres avisos antes de la restricción no son cortesía: son la prueba.** El
expediente existe para demostrar que se avisó, y un `READ_ONLY_APPLIED` sin
ningún `REMINDER_SENT` delante es exactamente el expediente que no prueba nada.

### 3.4 Incobrable: es un hito contable, no un escalón de acceso

**`WRITTEN_OFF` no cambia el estado del contrato ni quita un solo permiso.** Es
la anotación de que la plataforma deja de esperar ese dinero, para que la
contabilidad pueda provisionar la deuda. El cliente sigue exactamente igual que
el día anterior: en solo lectura, con toda su información disponible.

- **Umbral propuesto: 90 días desde `due_date`.** Es el corte contable habitual
  de cartera vencida y coincide con el trimestre, que es la periodicidad a la que
  esta decisión se revisa de verdad.
- **No es automático.** El motor puede *proponer* la lista de candidatos; quien
  firma el `WRITTEN_OFF` es una persona desde la consola. Dar una deuda por
  perdida sin que nadie mire es cómo se pierde un cliente que solo había cambiado
  de contacto de facturación.
- **Nunca cancela el contrato.** Cancelar es una decisión comercial separada
  (`CancelSubscriptionUseCase`), y aun cancelado el acceso de lectura se conserva
  —`ContractStatus.maxAccessLevel()` devuelve `READ_ONLY` también para
  `CANCELLED` y `EXPIRED`—.

### 3.5 Qué NO se propone, a propósito

- ❌ Ningún estado nuevo entre `READ_ONLY` y la nada. Añadirlo es violar R18.
- ❌ Ninguna degradación por módulos como palanca de cobro. El único caso en que
  un submódulo llega a `NONE` es el técnico —un submódulo que no sabe funcionar
  en solo lectura se oculta en vez de enseñar botones que rechazan el guardado—
  y eso se decide por capacidad del módulo, nunca por deuda.
- ❌ Ningún recargo por mora calculado por el motor. El dinero es *append-only*:
  un interés se cobra emitiendo otro documento, no editando el existente.

---

## 4. Dónde ajusta el usuario cada número

| Número | Pantalla | Ruta |
|---|---|---|
| `default_grace_days` («Días de cortesía») | Consola de plataforma → **Sistema › Facturación de plataforma** | `/configuracion/facturacion` |
| `default_payment_term_days` («Plazo de pago») | La misma | `/configuracion/facturacion` |
| `invoice_day_of_month` («Día de emisión de los cobros») | La misma | `/configuracion/facturacion` |
| `grace_days` **de un contrato concreto** | Consola de plataforma → **Contratos** → ficha del contrato | `subscriptions-admin` |
| Umbrales de aviso (D−3, D+1, D+3) | **No existen todavía**: harían falta columnas nuevas en `platform_billing_config` | — |
| Umbral de incobrable (D+90) | **No existe todavía**: ídem | — |

La pantalla es de plataforma y su autorización es `hasRole('SYSTEM')` a secas:
la política de mora la fija quien opera VetSoftware, no el tenant.

`invoice_day_of_month` está acotado a **1–28** por
`chk_platform_billing_config_invoice_day`, para que no haya un día 30 que en
febrero no existe.

---

## 5. Qué hay que construir para que esta política sea real

En orden de dependencia:

1. **Que el alta directa por API herede `default_grace_days`** en vez de caer a
   0. Sin esto, cualquier número que se apruebe aquí no llega a los contratos
   creados por ese camino.
2. **Columnas de umbral en `platform_billing_config`**: días de preaviso, días
   entre recordatorios, días hasta incobrable. Hoy no hay dónde guardarlos, así
   que estarían quemados en el código — el defecto que esta auditoría vino a
   buscar.
3. **Que el motor emita `REMINDER_SENT`** con su canal, e integrarlo con el envío
   real. Mientras esto no exista, el escalón de solo lectura llega sin aviso
   registrado.
4. **Una hora fija para el barrido.** Hoy el job usa `fixedDelay` de 24 h desde
   el arranque, así que corre a una hora distinta después de cada despliegue.
   Una política que habla de «D+3» necesita que el día sea el mismo día para
   todos.
5. **La declaración de incobrable**, como acción de consola con su evento.

---

## 6. Cómo aprobar o cambiar esto

Los números que necesitan una decisión explícita son cinco:

| # | Parámetro | Propuesto | Alternativa si se rechaza |
|---|---|---|---|
| 1 | `default_payment_term_days` | 8 | 0 (hoy) → obliga a subir la gracia a 13 |
| 2 | `default_grace_days` | 5 (se mantiene) | 13 si no se toca el #1 |
| 3 | Preaviso | D−3 | Quitarlo: se pierde el aviso previo al vencimiento |
| 4 | Recordatorios en mora | D+1 y D+3 | Uno solo en D+2 |
| 5 | Incobrable | D+90, manual | D+120, o solo bajo decisión sin umbral |

Cambiar cualquiera de ellos **no requiere tocar código** una vez construido el
punto 2 de la sección 5: son filas de `platform_billing_config` editables desde
`/configuracion/facturacion`.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
