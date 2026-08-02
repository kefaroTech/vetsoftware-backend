# Integridad del registro de auditoría

**Fecha:** 07/28/2026
**Hallazgo que cierra:** OBS-020 del reporte de trazabilidad
**Implementación:** `infrastructure/audit/chain/`, migración `215_add_audit_chain_integrity.xml`

> **Retención y obligaciones legales.** Los plazos de conservación de este documento (365 días de
> Object Lock, 7 días de outbox) son valores de ingeniería, **no una determinación legal**. Las
> obligaciones aplicables a registros de auditoría con datos de historia clínica animal y datos
> personales de propietarios deben ser confirmadas por asesoría legal antes de considerarlas
> definitivas. Este documento no constituye asesoría legal.

## 1. Corrección del enunciado del hallazgo

El hallazgo original decía que «`audit.log` se almacena localmente y el propio host o proceso con
permisos puede modificarlo». **Ese archivo no existe.** `logback-spring.xml` no declara ningún
appender de archivo; su comentario es explícito: «No se crea ningún archivo local». Los eventos de
auditoría salen por dos rutas, ninguna de ellas un archivo en disco:

| Ruta | Destino | Propósito |
|---|---|---|
| Logger `AUDIT` → appender OTEL | Collector → Loki | Consulta operativa, no evidencia |
| `AuditEventStore` → tabla `audit_event_outbox` → Firehose | S3 con Object Lock | **Evidencia** |

Lo que sí era cierto es el fondo del hallazgo: **no había mecanismo de integridad**. El resto de este
documento describe el que se implementó.

## 2. Qué protege cada control

| Control | Protege contra | **No** protege contra |
|---|---|---|
| S3 Object Lock, modo COMPLIANCE | Modificar o borrar un objeto ya entregado, incluso con credenciales de root | Que un evento nunca llegue a entregarse |
| Versionado + bloqueo de acceso público + política TLS-only | Sobrescritura, exposición accidental, tránsito en claro | Manipulación previa a la entrega |
| Rol de Firehose con solo `PutObject` | Que el remitente borre lo ya escrito | Manipulación en la base de datos |
| **Cadena de hash en la outbox** | **Suprimir, alterar o reordenar eventos en MySQL** | Suprimir un evento antes de que se secuencie |
| **Checkpoints anclados en WORM** | Recalcular toda la cadena tras manipular un evento | — |
| Verificación periódica + alerta | Que una rotura pase inadvertida | — |

El punto clave: **Object Lock y la cadena resuelven amenazas distintas.** Object Lock demuestra que
un objeto no cambió *después* de entregarse. No dice nada sobre lo que pasó antes, y la tabla de
MySQL es escribible por cualquiera con acceso a la base. La cadena cubre precisamente ese tramo.

## 3. La cadena de hash

Cada evento almacena:

| Columna | Contenido |
|---|---|
| `payload_hash` | `SHA-256` del payload serializado, fijado en el `INSERT` |
| `chain_sequence` | Posición monótona, asignada por el secuenciador |
| `previous_hash` | `chain_hash` del eslabón anterior |
| `chain_hash` | `SHA-256(previous_hash + ":" + sequence + ":" + payload_hash)` |

El primer eslabón usa `GENESIS_HASH` (64 ceros) como `previous_hash`.

Encadenar así hace que **cualquier** manipulación invalide todos los eslabones posteriores:

- **Alterar un payload** → `payload_hash` deja de coincidir con el contenido.
- **Alterar payload y su hash** → `chain_hash` deja de coincidir con el recálculo.
- **Recalcular también el eslabón** → el eslabón *siguiente* sigue apuntando al valor viejo, y su
  `previous_hash` ya no coincide.
- **Eliminar un evento** → queda un hueco en la secuencia.
- **Recalcular la cadena completa** → deja de coincidir con el último checkpoint anclado en el
  bucket inmutable, que no se puede reescribir.

Ese último punto es el que convierte la cadena en evidencia y no en una simple suma de comprobación.
Sin el ancla, quien tenga acceso total a la base puede recalcular todo y quedar consistente.

## 4. Por qué el hash no se calcula en la transacción de negocio

Una cadena necesita un orden total, y un orden total necesita serializar a los escritores. Hacerlo
en el `INSERT` obligaría a que **toda transacción de negocio que muta algo** tomara un bloqueo sobre
una fila común y lo retuviera hasta terminar. Eso convierte el registro de auditoría en el cuello de
botella de la aplicación entera y multiplica el riesgo de deadlock.

El trabajo se parte en dos:

- **En el `INSERT` (dentro de la transacción de negocio):** solo `payload_hash`. Es un cálculo local,
  sin bloqueos ni contención. Ya deja el contenido sellado.
- **En el secuenciador (transacción propia y corta, dentro del publicador):** asigna posición y
  eslabón. Bloquea la fila única de `audit_chain_head` con `SELECT ... FOR UPDATE` durante
  milisegundos, y solo compite con otras réplicas del publicador. **Este bloqueo nunca se toma dentro
  de una transacción de negocio.**

Solo se publica lo ya secuenciado, de modo que el registro que llega al archivo lleva su prueba de
integridad en un bloque `integrity`.

## 5. Riesgo residual

**Ventana de supresión antes del secuenciado.** Un evento eliminado de `audit_event_outbox` entre su
inserción y el ciclo del secuenciador (por defecto 5 s) no deja hueco en la cadena, porque nunca
recibió posición. Es el límite del diseño y es una decisión consciente: cerrarlo del todo exige
serializar los escritores en la transacción de negocio, con el coste descrito arriba.

Mitigaciones de esa ventana:

- El `id` es `AUTO_INCREMENT`: una supresión deja un salto observable, aunque los rollbacks también
  producen saltos, así que la señal es indiciaria y no prueba.
- Reducir `verify-interval` y `publish-interval` acorta la ventana proporcionalmente.
- El control efectivo es **no conceder `DELETE` sobre esa tabla** a la cuenta de aplicación
  (sección 7).

**Alcance de la verificación local.** La depuración elimina eventos ya publicados y anclados después
de la retención de la outbox (7 días por defecto). La verificación en base de datos cubre por tanto
la ventana retenida; lo anterior solo es auditable contra el archivo inmutable. La depuración **no
puede rebasar la marca de checkpoint**, así que nunca borra un eslabón sin anclar.

**El logger `AUDIT` no está encadenado.** Su copia en Loki es para consulta operativa. La evidencia
es la del bucket WORM. No deben tratarse como equivalentes.

## 6. Verificación y señales

`AuditChainVerificationJob` recorre **toda la ventana retenida en cada pasada**, por lotes para
acotar la memoria.

La pasada completa es deliberada y no una ineficiencia. Un verificador incremental que solo mirase
los eslabones nuevos sería inútil: el ataque probable no es alterar un evento futuro sino uno
*pasado*, y un cursor que nunca vuelve atrás jamás lo vería. Esta implementación empezó siendo
incremental y se cambió al comprobar que, efectivamente, no detectaba la alteración de un registro ya
verificado. El mando para regular el coste es `verify-interval`; recortar el alcance no es una opción.

| Métrica | Significado |
|---|---|
| `audit_chain_broken` | `1` roto, `0` intacto, `-1` sin verificar todavía |
| `audit_chain_verified_sequence` | Última posición verificada correctamente |
| `audit_chain_failure_sequence` | Posición de la divergencia, `0` si está intacta |
| `audit_chain_length` | Longitud total de la cadena emitida |
| `audit_chain_checkpoint_sequence` | Hasta dónde está anclada en WORM |
| `audit_chain_unsequenced` | Eventos insertados sin posición todavía |

El valor inicial `-1` es deliberado: un verificador que no arranca no debe verse igual que una cadena
sana. Lo cubre `VetSoftwareAuditChainNotVerified`.

Alertas en `docker/prometheus-platform-alerts.yml`, grupo `vetsoftware-audit-integrity`, con runbooks
en `docs/ALERTAMIENTO_OPERATIVO.md`.

## 7. Endurecimiento pendiente, fuera del código

Estos controles no se pueden implementar en la aplicación y quedan como trabajo de operación:

1. **Grants de MySQL.** La cuenta de la aplicación necesita `INSERT` y `UPDATE` sobre
   `audit_event_outbox`, pero **no necesita `DELETE`**. La depuración debería ejecutarse con una
   cuenta distinta, o moverse a un procedimiento almacenado con `SQL SECURITY DEFINER`. Hoy la
   aplicación usa una sola cuenta para todo, así que conserva `DELETE`: es el riesgo residual más
   accionable que queda.
2. **Acceso de lectura al bucket de auditoría.** Debe restringirse a los roles que realmente
   investigan incidentes, con acceso registrado en CloudTrail.
3. **Validar `audit_retention_days`** contra las obligaciones legales aplicables. El módulo exige un
   mínimo de 365 días y COMPLIANCE no permite acortarlo después: **elegir un valor demasiado alto es
   tan irreversible como elegirlo demasiado bajo**, porque el objeto no se puede borrar ni pagando.
4. **Ensayar la restauración.** Nunca se ha probado reconstruir la cadena desde los objetos de S3.
   Un archivo del que no se sabe leer no es un respaldo.

## 8. Cómo investigar una alerta de cadena rota

1. **No tocar la base de datos.** Cualquier escritura sobre la tabla destruye evidencia.
2. Leer `audit_chain_failure_sequence` y `audit_chain_verified_sequence` para acotar el tramo.
3. Descargar del bucket WORM el último checkpoint **anterior** a la divergencia y comparar su
   `chain.hash` con el `chain_hash` de esa posición en la base.
   - **Coinciden** → la manipulación es posterior al checkpoint; el archivo conserva la versión
     buena de los eventos anteriores.
   - **No coinciden** → la manipulación es anterior; ampliar el análisis a checkpoints previos.
4. Reconstruir la secuencia real desde los objetos del archivo, que sí son inmutables.
5. Tratarlo como incidente de seguridad: una cadena rota implica acceso de escritura a la base de
   datos de producción.

## 9. Referencias

- [S3 Object Lock](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html)
- [Modos de retención: governance y compliance](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock-overview.html)
- [NIST SP 800-92: gestión de registros de seguridad](https://csrc.nist.gov/pubs/sp/800/92/final)
