-- =====================================================================================
-- Reinicio del kardex y de las ventas POS  ·  BE-01
-- =====================================================================================
--
-- QUE HACE
--   Vacia el inventario (6 tablas) y borra los documentos POS directos con todo lo que
--   cuelga de ellos. Se toma la decision de NO reconciliar el historico y empezar de
--   cero, porque la linea fiscal no guarda product_id y reconstruir que se vendio en
--   cada documento antiguo seria adivinar. Ver BE-01 en la auditoria.
--
-- QUE NO HACE
--   No se ejecuta solo. No es un changeset de Liquibase a proposito: un borrado de
--   datos no debe dispararse en un despliegue. Lo corres tu, en el entorno que decidas.
--
-- ==========================  LEELO ANTES DE EJECUTAR  =================================
--
--   Borrar un documento aqui NO lo borra en la DIAN. Si alguno se transmitio y quedo
--   VALIDADO, la DIAN conserva su copia y tu te quedas sin el respaldo local: sin XML,
--   sin CUFE y sin representacion grafica para responder a una revision.
--
--   El PASO 0 cuenta exactamente eso. Si devuelve algo distinto de cero en
--   "validados_en_dian", PARA y decide a conciencia; no es un dato de prueba.
--
--   Esto no tiene rollback. Haz un dump antes:
--     mysqldump -h <host> -u <user> -p --single-transaction --set-gtid-purged=OFF \
--       vetsoftware > respaldo-antes-del-reset.sql
--
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- PASO 0 — Radiografia. Solo lectura: ejecuta esto primero y lee el resultado.
-- -------------------------------------------------------------------------------------
SELECT 'documentos POS directos'        AS concepto, COUNT(*) AS filas FROM electronic_documents WHERE open_account_id IS NULL
UNION ALL SELECT '  de ellos, VALIDADOS EN DIAN', COUNT(*) FROM electronic_documents WHERE open_account_id IS NULL AND dian_status = 'VALIDADO'
UNION ALL SELECT '  de ellos, con transmision',   COUNT(*) FROM electronic_documents d WHERE d.open_account_id IS NULL
             AND EXISTS (SELECT 1 FROM electronic_document_transmissions t WHERE t.electronic_document_id = d.id)
UNION ALL SELECT 'notas que referencian un POS',  COUNT(*) FROM electronic_documents n WHERE n.referenced_cufe IS NOT NULL
             AND n.referenced_cufe IN (SELECT cufe FROM electronic_documents WHERE open_account_id IS NULL AND cufe IS NOT NULL)
UNION ALL SELECT 'movimientos de kardex',         COUNT(*) FROM stock_movement
UNION ALL SELECT 'lotes',                         COUNT(*) FROM stock_lot
UNION ALL SELECT 'saldos',                        COUNT(*) FROM stock_balance
UNION ALL SELECT 'tomas fisicas',                 COUNT(*) FROM inventory_count
UNION ALL SELECT 'movimientos de caja por POS',   COUNT(*) FROM cash_movement WHERE reference_type = 'POS_DOCUMENT';

-- -------------------------------------------------------------------------------------
-- PASO 1 — El borrado. Todo dentro de una transaccion: o entra entero o no entra nada.
--
-- El orden va de la hoja a la raiz. Las tablas de inventario no tienen FK entre si
-- (el vinculo con la venta es logico: reference_type + reference_id), pero las de
-- documento si, y borrar el padre antes reventaria.
-- -------------------------------------------------------------------------------------
START TRANSACTION;

-- 1.1 Kardex completo. Se vacia entero, no solo lo de POS: los saldos y los lotes son
--     acumulados de TODAS las fuentes (compras, ajustes, traslados, consumo clinico), y
--     borrar solo las salidas de POS dejaria los saldos cuadrando contra un historial
--     que ya no existe. Empezar de cero significa empezar de cero.
DELETE FROM sale_inventory_allocations;
DELETE FROM inventory_count_line;
DELETE FROM inventory_count;
DELETE FROM stock_movement;
DELETE FROM stock_lot;
DELETE FROM stock_balance;

-- 1.2 Caja: los movimientos generados por esas ventas. Si se dejan, el arqueo sigue
--     sumando cobros cuyo documento ya no existe y ninguna sesion vuelve a cuadrar.
--     Las sesiones de caja NO se borran: son del cajero, no de la venta.
DELETE FROM cash_movement WHERE reference_type = 'POS_DOCUMENT';

-- 1.3 Notas credito/debito que referencian un documento POS. Van ANTES que el documento
--     al que apuntan: una nota huerfana es un documento fiscal que corrige algo que no
--     existe.
DELETE FROM electronic_document_payments
 WHERE electronic_document_id IN (
       SELECT id FROM (SELECT n.id FROM electronic_documents n
                        WHERE n.referenced_cufe IS NOT NULL
                          AND n.referenced_cufe IN (SELECT cufe FROM electronic_documents
                                                    WHERE open_account_id IS NULL AND cufe IS NOT NULL)) AS x);
DELETE FROM electronic_document_lines
 WHERE electronic_document_id IN (
       SELECT id FROM (SELECT n.id FROM electronic_documents n
                        WHERE n.referenced_cufe IS NOT NULL
                          AND n.referenced_cufe IN (SELECT cufe FROM electronic_documents
                                                    WHERE open_account_id IS NULL AND cufe IS NOT NULL)) AS x);
DELETE FROM electronic_document_transmissions
 WHERE electronic_document_id IN (
       SELECT id FROM (SELECT n.id FROM electronic_documents n
                        WHERE n.referenced_cufe IS NOT NULL
                          AND n.referenced_cufe IN (SELECT cufe FROM electronic_documents
                                                    WHERE open_account_id IS NULL AND cufe IS NOT NULL)) AS x);
DELETE FROM electronic_documents
 WHERE referenced_cufe IS NOT NULL
   AND referenced_cufe IN (SELECT cufe FROM (SELECT cufe FROM electronic_documents
                                             WHERE open_account_id IS NULL AND cufe IS NOT NULL) AS y);

-- 1.4 Los documentos POS directos. open_account_id IS NULL es lo que distingue la venta
--     de mostrador de la facturacion de una cuenta abierta, que NO se toca.
--
--     NO se toca numbering_resolutions a proposito. El consecutivo fiscal NO se
--     reinicia: un numero de resolucion ya usado no puede volver a emitirse aunque el
--     documento desaparezca de esta base, porque la DIAN si lo tiene. El contador sigue
--     donde estaba y la proxima venta continua desde ahi. Si alguna vez se reinicia a
--     mano, la primera factura nueva choca con un consecutivo ya reportado.
DELETE FROM electronic_document_payments
 WHERE electronic_document_id IN (SELECT id FROM (SELECT id FROM electronic_documents WHERE open_account_id IS NULL) AS x);
DELETE FROM electronic_document_lines
 WHERE electronic_document_id IN (SELECT id FROM (SELECT id FROM electronic_documents WHERE open_account_id IS NULL) AS x);
DELETE FROM electronic_document_transmissions
 WHERE electronic_document_id IN (SELECT id FROM (SELECT id FROM electronic_documents WHERE open_account_id IS NULL) AS x);
DELETE FROM electronic_documents WHERE open_account_id IS NULL;

COMMIT;

-- -------------------------------------------------------------------------------------
-- PASO 2 — Comprobacion. Todo debe dar cero salvo lo que se conserva a proposito.
-- -------------------------------------------------------------------------------------
SELECT 'kardex (debe ser 0)'                   AS concepto, COUNT(*) AS filas FROM stock_movement
UNION ALL SELECT 'lotes (0)',                  COUNT(*) FROM stock_lot
UNION ALL SELECT 'saldos (0)',                 COUNT(*) FROM stock_balance
UNION ALL SELECT 'documentos POS (0)',         COUNT(*) FROM electronic_documents WHERE open_account_id IS NULL
UNION ALL SELECT 'caja por POS (0)',           COUNT(*) FROM cash_movement WHERE reference_type = 'POS_DOCUMENT'
UNION ALL SELECT 'lineas huerfanas (0)',       COUNT(*) FROM electronic_document_lines l
             WHERE NOT EXISTS (SELECT 1 FROM electronic_documents d WHERE d.id = l.electronic_document_id)
UNION ALL SELECT 'CONSERVADO: docs de cuenta', COUNT(*) FROM electronic_documents WHERE open_account_id IS NOT NULL
UNION ALL SELECT 'CONSERVADO: sesiones caja',  COUNT(*) FROM cash_session
UNION ALL SELECT 'CONSERVADO: productos',      COUNT(*) FROM products;

-- -------------------------------------------------------------------------------------
-- PASO 3 — Que hacer despues, para que el inventario vuelva a ser real
--
--   El stock queda en CERO, no en "desconocido". Mientras no cargues existencias, toda
--   venta de mostrador saldra con stock negativo: el POS no se frena por falta de stock
--   (allowNegative=true), asi que vendera igual y el kardex quedara en numeros rojos.
--
--   Carga las existencias por la aplicacion, no con INSERTs a mano. Dos caminos, los dos
--   dejan asiento trazable:
--     a) Toma fisica (POST /inventory/counts): cuentas lo que hay y el sistema emite los
--        ADJUSTMENT_IN con la sesion de conteo como referencia. Es el camino auditable.
--     b) Compra / entrada de mercancia (GOODS_RECEIPT): si vas a recargar con las
--        facturas de compra reales, entra por ahi y ademas queda el costo por lote,
--        que es lo que despues sostiene el COGS y el margen.
--
--   No insertes filas en stock_balance directamente: el saldo es un derivado del ledger
--   y dejarlo desalineado reproduce el problema que este script viene a cerrar.
-- -------------------------------------------------------------------------------------
