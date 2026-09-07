# Pasarela Wompi — auditoría contable (ronda 2) — especificación de esquema

**Para `db-migrations`.** Esta ronda auditó `subscription_payments`, `billing_document_
applications`, `gateway_settlements`, `gateway_webhook_events`, `payment_refunds`, `payment_
reversal_requests`, `payment_attempts`, `customer_credit_entries` y `shedlock` contra el criterio
de un contador de plataforma, tras los changesets `411`-`414` y los cambios de código descritos en
el encargo (idempotencia del cierre de contratación, aplicación de pagos `PENDING`, prorrateo por
`customercredit`, documento del primer periodo emitido en la firma).

El informe completo de hallazgos —35+ casos, `DAT2-nn`— vive en el scratchpad de la sesión, no en
este repositorio (regla del agente: no se escriben informes de auditoría en `docs/`, solo
especificaciones de esquema). Este fichero cubre **el único hallazgo de esta ronda que exige un
cambio de esquema** para que `db-migrations` lo escriba si el negocio lo confirma. El resto de
hallazgos (columnas sin mapear, guardas de aplicación, listados sin filtro) son de dominio o de
aplicación y van como issues de `vetsoftware-backend` (#779-#784), no de esquema.

---

## 1. Pendiente de confirmación de negocio — NO implementar sin esa confirmación

### 1.1 `gateway_settlements`: ¿retiene VetSoftware algo sobre la comisión de Wompi?

**DAT2-08.** `gateway_settlements` (changeset `326`) modela `gross_amount`, `fee_amount`,
`fee_tax_amount` (el IVA de la comisión) y `gmf_amount` (el 4×1000 de la salida), pero no tiene
ninguna columna de **retención en la fuente**. El encargo de esta ronda pregunta explícitamente
si el extracto de Wompi trae una retención junto a la comisión, el IVA y el neto.

Verificado contra el código: no hay ninguna columna, constante ni comentario en el repositorio que
hable de una retención sobre la comisión de Wompi. Y verificado contra el criterio contable
colombiano en general (no específico de Wompi, que no pude consultar en vivo): la retención en la
fuente la practica **quien paga una factura de servicios**, no quien la recibe. Si Wompi factura
su comisión a VetSoftware y VetSoftware la paga, es **VetSoftware quien debería practicar la
retención** al pagar — no Wompi quien la descuenta del lote. Eso significaría que la retención,
si existe, **no es un dato que Wompi reporte en la liquidación**, sino un asiento que VetSoftware
genera en su propia contabilidad al registrar la factura del proveedor
(`provider_invoice_ref`/`provider_tax_id`, ya modelados en `326`) — es decir, no es una columna
de `gateway_settlements` sino de un módulo de cuentas por pagar a proveedores, que está fuera del
alcance de esta feature.

**No propongo DDL para esto todavía.** Antes de escribir un changeset:

1. Confirmar con contabilidad si Wompi opera como agente autorretenedor (caso en el que la
   retención sí podría venir practicada en su propio extracto/factura) o si la retención la debe
   practicar VetSoftware al pagar la factura del proveedor (caso en el que no toca a este esquema).
2. Si el primer caso se confirma, la especificación sería: dos columnas nulables en
   `gateway_settlements`, con el mismo patrón bicondicional que `chk_gateway_settlements_provider_
   invoice` (326:92-94) — van juntas o ninguna, porque llegan con la factura del proveedor, no con
   la liquidación:

   ```sql
   ALTER TABLE gateway_settlements
       ADD COLUMN retention_amount DECIMAL(19,2) NULL,
       ADD COLUMN retention_certificate_ref VARCHAR(60)
           CHARACTER SET ascii COLLATE ascii_bin NULL,
       ADD CONSTRAINT chk_gateway_settlements_retention
           CHECK ((retention_amount IS NULL AND retention_certificate_ref IS NULL)
                  OR (retention_amount IS NOT NULL AND retention_certificate_ref IS NOT NULL
                      AND retention_amount >= 0 AND retention_amount < fee_amount)),
       MODIFY COLUMN retention_certificate_ref VARCHAR(60)
           CHARACTER SET ascii COLLATE ascii_bin NULL;
   ```

   `chk_gateway_settlements_net` (326:88-89) **no cambia**: la retención no altera lo que cae al
   banco (`net_amount = gross_amount - fee_amount - fee_tax_amount - gmf_amount`) porque quien la
   practica es VetSoftware sobre su propia factura de proveedor, no Wompi sobre el lote.

**Manual de MySQL 8.4** (verificado): añadir dos columnas `NULL` sin `DEFAULT` no trivial y sin
`STORED` es *Instant* en InnoDB Online DDL — el `ALTER` de arriba no bloquea escritura ni
reconstruye la tabla (`ADD COLUMN` sin más que un valor `NULL` por defecto está en la lista de
operaciones *instant* desde 8.0.12). https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html

---

## 2. Verificado — sin cambio de esquema

Para que quien lea esto no reabra lo ya decidido:

- **Los índices que un contador necesitaría ya existen.** `ix_subscription_payments_company_
  status (company_id, status, received_at)` (252) serviría un listado "pagos `CONFIRMED` de esta
  empresa entre dos fechas" en cuanto el caso de uso acepte esos parámetros — hoy no los acepta
  (issue #782). `ix_subscription_payments_settlement (gateway, settlement_reference)` (252)
  serviría el cruce con `gateway_settlements` en cuanto la entidad JPA mapee esa columna — hoy no
  la mapea (issue #779). **No hace falta ningún índice nuevo**: el esquema ya anticipó estas dos
  consultas: el problema es de código, no de DDL.
- **La moneda ya está cerrada a `COP`** (`chk_subscription_payments_currency`, 252): un cambio de
  moneda está bloqueado en la base, no solo en la aplicación. Verificado, sin acción.
- **La doble emisión del documento del primer periodo está resuelta**: `IssueSubscriptionPeriod
  DocumentUseCase` es el mismo camino de código para el primer periodo (`ChargeContractFirst
  PeriodService`) y para el ciclo recurrente, y `DuplicateBillingCycleException` hace que un
  segundo intento sobre el mismo periodo devuelva el documento existente
  (`IssueSubscriptionPeriodDocumentService.java:89-92,101-112`) en vez de numerar dos veces.
  Verificado, sin acción de esquema.
- **El patrón de unicidad condicional de `226`/`210`/`206` no hace falta aquí**: ningún hallazgo
  de esta ronda pide una unicidad "solo para lo vigente" nueva. `customer_credit_entries` (323) ya
  lo usa para `origin_marker` y no se toca.

---

## 3. Fuera de esquema — issues de `vetsoftware-backend`

Registrados en GitHub, no en este fichero (son dominio/aplicación, no DDL):

- #779 — `subscription_payments`: cinco columnas de conciliación nunca mapeadas ni escritas.
- #780 — Anular un documento con pago `CONFIRMED` aplicado solo deja un `WARN`.
- #781 — Un pago puede confirmarse (`SYSTEM`) sin `gatewayReference`.
- #782 — Sin filtro de estado/fecha para el cierre de mes.
- #783 — `received_at` es la hora de reserva, no la de la transacción real.
- #784 — El reembolso por retracto no valida plazo.
