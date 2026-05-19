-- Seed: granular update/delete permissions for the 6 clinical procedure modules.
--
-- Run this once after deploying the @PreAuthorize changes that introduce
-- `<recurso>.update` and `<recurso>.delete` authorities on the Update/Delete
-- use cases of deworming, diagnosticimaging, hospitalization, laboratoryTest,
-- surgery, and vaccination.
--
-- The script is idempotent — re-running it skips rows already present.
--
-- After running, hit POST /publish-admin-permissions (or click "Publicar
-- permisos admin" from the front sidebar) so every existing Company gets
-- the new permissions linked to its ADMIN role.
--
-- IMPORTANT: this script assumes the existing `sub_modules.code` for each
-- module matches the value in @code below. If your codes use a different
-- case or naming, adjust the WHERE clauses accordingly.

START TRANSACTION;

-- 1) base_permissions: one row per (module x action) — uses the same code
-- prefix already in production for the *.create permissions.
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar desparasitación', 'deworming.update', sm.id FROM sub_modules sm WHERE sm.code = 'deworming'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'deworming.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar desparasitación', 'deworming.delete', sm.id FROM sub_modules sm WHERE sm.code = 'deworming'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'deworming.delete');

INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar imagen diagnóstica', 'diagnosticimaging.update', sm.id FROM sub_modules sm WHERE sm.code = 'diagnosticimaging'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'diagnosticimaging.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar imagen diagnóstica', 'diagnosticimaging.delete', sm.id FROM sub_modules sm WHERE sm.code = 'diagnosticimaging'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'diagnosticimaging.delete');

INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar hospitalización', 'hospitalization.update', sm.id FROM sub_modules sm WHERE sm.code = 'hospitalization'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'hospitalization.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar hospitalización', 'hospitalization.delete', sm.id FROM sub_modules sm WHERE sm.code = 'hospitalization'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'hospitalization.delete');

INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar examen de laboratorio', 'laboratoryTest.update', sm.id FROM sub_modules sm WHERE sm.code = 'laboratoryTest'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'laboratoryTest.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar examen de laboratorio', 'laboratoryTest.delete', sm.id FROM sub_modules sm WHERE sm.code = 'laboratoryTest'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'laboratoryTest.delete');

INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar cirugía', 'surgery.update', sm.id FROM sub_modules sm WHERE sm.code = 'surgery'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'surgery.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar cirugía', 'surgery.delete', sm.id FROM sub_modules sm WHERE sm.code = 'surgery'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'surgery.delete');

INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Editar vacunación', 'vaccination.update', sm.id FROM sub_modules sm WHERE sm.code = 'vaccination'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'vaccination.update');
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Eliminar vacunación', 'vaccination.delete', sm.id FROM sub_modules sm WHERE sm.code = 'vaccination'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'vaccination.delete');

-- 2) base_role_permissions: link each new base_permission to the ADMIN base_role.
-- Adjust the role code below if your ADMIN base_role uses a different code.
INSERT INTO base_role_permissions (base_role_id, base_permission_id)
SELECT br.id, bp.id
FROM base_roles br
JOIN base_permissions bp ON bp.code IN (
  'deworming.update', 'deworming.delete',
  'diagnosticimaging.update', 'diagnosticimaging.delete',
  'hospitalization.update', 'hospitalization.delete',
  'laboratoryTest.update', 'laboratoryTest.delete',
  'surgery.update', 'surgery.delete',
  'vaccination.update', 'vaccination.delete'
)
WHERE br.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM base_role_permissions brp
    WHERE brp.base_role_id = br.id AND brp.base_permission_id = bp.id
  );

COMMIT;
