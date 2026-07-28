# Convención de nombres de observabilidad

Esta convención aplica a las observaciones Micrometer, los spans de negocio derivados de
ellas y los nombres de trabajos programados de VetSoftware.

## Observaciones y métricas

Los nombres técnicos usan `lowercase.dot.notation`:

```text
dominio[.recurso].operacion
```

El segmento de recurso es opcional. Cada palabra ocupa un segmento separado por punto. No se permiten camelCase, guiones,
guiones bajos, espacios, identificadores ni otros valores de cardinalidad no acotada.

Ejemplos:

| Incorrecto | Correcto |
|---|---|
| `animal_alert.listByAnimal` | `animal.alert.list.by.animal` |
| `cashRegister.openSession` | `cash.register.open.session` |
| `clinicalhistory.list_by_company` | `clinical.history.list.by.company` |
| `supplierInvoice.registerPayment` | `supplier.invoice.register.payment` |

El nombre debe identificar una clase estable de operaciones. Los identificadores de empresa,
usuario, animal, factura o solicitud pertenecen a atributos y nunca al nombre.

## Nombres contextuales de spans

Cuando se define `contextualName`, se usa una frase corta y de baja cardinalidad con el patrón
`verbo objeto`, en minúsculas y separada por espacios:

```text
send email
render pdf
```

El nombre contextual facilita la lectura en Tempo. El nombre técnico de la observación se
conserva como identificador estable para métricas, paneles y alertas.

## Trabajos programados

`job.name` también usa `lowercase.dot.notation`, por ejemplo:

```text
audit.outbox.publish
dian.contingency.retry
```

`ScheduledJobTelemetry` rechaza nombres que no cumplan la convención. La prueba
`ObservationNamingConventionTest` examina todas las anotaciones `@Observed`, exige nombres
únicos y evita regresiones de formato.
