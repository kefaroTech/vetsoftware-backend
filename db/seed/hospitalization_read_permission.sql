-- Seed: granular read permission for the hospitalization ward module.
--
-- Run this once after deploying the @PreAuthorize change that introduces the
-- `hospitalization.read` authority on ListHospitalizationsUseCase (used by the
-- new hospitalization ward board in the frontend).
--
-- The script is idempotent — re-running it skips rows already present.
--
-- After running, hit POST /publish-admin-permissions (or click "Publicar
-- permisos admin" from the front sidebar) so every existing Company gets
-- the new permission linked to its ADMIN role.
--
-- IMPORTANT: this script assumes the existing `sub_modules.code` for the
-- hospitalization module is 'hospitalization' (same prefix as
-- hospitalization.create/update/delete). Adjust the WHERE clause if your
-- code uses a different case or naming.

START TRANSACTION;

-- 1) base_permissions: one row for hospitalization.read.
INSERT INTO base_permissions (name, code, sub_module_id)
SELECT 'Ver hospitalizaciones', 'hospitalization.read', sm.id FROM sub_modules sm WHERE sm.code = 'hospitalization'
  AND NOT EXISTS (SELECT 1 FROM base_permissions WHERE code = 'hospitalization.read');

-- 2) base_role_permissions: link the new base_permission to the ADMIN base_role.
INSERT INTO base_role_permissions (base_role_id, base_permission_id)
SELECT br.id, bp.id
FROM base_roles br
JOIN base_permissions bp ON bp.code = 'hospitalization.read'
WHERE br.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM base_role_permissions brp
    WHERE brp.base_role_id = br.id AND brp.base_permission_id = bp.id
  );

COMMIT;
