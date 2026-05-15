package com.vetsoftware.app.publishadminpermissions.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermissionsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBaseRoleQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyCatalogQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.MembershipSubModuleIdsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.PermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.RolePermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.UpsertedPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublishAdminPermissionsServiceTest {

    private static final long ADMIN_BASE_ROLE_ID = 99L;

    @Test
    void publishes_new_basepermission_to_admin_of_company_with_matching_membership() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(10L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(1, result.companiesProcessed());
        assertEquals(1, result.companiesUpdated());
        assertEquals(1, result.permissionsCreated());
        assertEquals(1, result.rolePermissionsCreated());
        assertEquals(Set.of("100|vac.delete"), s.permissionStore.keySet());
        assertEquals(Set.of("300|100|vac.delete"), s.rolePermissionLinks);
    }

    @Test
    void does_not_publish_basepermission_when_submodule_not_in_company_membership() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(99L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(1, result.companiesProcessed());
        assertEquals(0, result.companiesUpdated());
        assertEquals(0, result.permissionsCreated());
        assertEquals(0, result.rolePermissionsCreated());
    }

    @Test
    void is_idempotent_when_run_twice() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(10L));

        PublishAdminPermissionsService service = newService(s);
        service.execute();
        PublishAdminPermissionsDto second = service.execute();

        assertEquals(1, second.companiesProcessed());
        assertEquals(0, second.companiesUpdated());
        assertEquals(0, second.permissionsCreated());
        assertEquals(0, second.rolePermissionsCreated());
    }

    @Test
    void throws_when_admin_base_role_not_configured() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = null;

        assertThrows(IllegalStateException.class, () -> newService(s).execute());
    }

    @Test
    void returns_zero_when_no_basepermissions_linked_to_admin() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(10L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(1, result.companiesProcessed());
        assertEquals(0, result.companiesUpdated());
        assertEquals(0, result.permissionsCreated());
        assertEquals(0, result.rolePermissionsCreated());
    }

    @Test
    void publishes_to_multiple_companies_with_same_membership() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.adminBasePermissions.add(new AdminBasePermission(2L, "vac.create", "Crear vacuna", 10L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.companies.add(new CompanyAdminContext(101L, 200L, 301L));
        s.membershipSubModules.put(200L, Set.of(10L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(2, result.companiesProcessed());
        assertEquals(2, result.companiesUpdated());
        assertEquals(4, result.permissionsCreated());
        assertEquals(4, result.rolePermissionsCreated());
    }

    @Test
    void publishes_only_applicable_permission_when_some_match_membership() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.adminBasePermissions.add(new AdminBasePermission(2L, "surg.create", "Crear cirugia", 20L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(10L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(1, result.companiesProcessed());
        assertEquals(1, result.companiesUpdated());
        assertEquals(1, result.permissionsCreated());
        assertEquals(1, result.rolePermissionsCreated());
    }

    @Test
    void links_existing_permission_without_creating_when_permission_already_exists() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));
        s.companies.add(new CompanyAdminContext(100L, 200L, 300L));
        s.membershipSubModules.put(200L, Set.of(10L));
        s.permissionStore.put("100|vac.delete", 5000L);

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(1, result.companiesProcessed());
        assertEquals(1, result.companiesUpdated());
        assertEquals(0, result.permissionsCreated());
        assertEquals(1, result.rolePermissionsCreated());
    }

    @Test
    void returns_zero_updated_when_no_companies_exist() {
        Stubs s = new Stubs();
        s.adminBaseRoleId = ADMIN_BASE_ROLE_ID;
        s.adminBasePermissions.add(new AdminBasePermission(1L, "vac.delete", "Borrar vacuna", 10L));

        PublishAdminPermissionsDto result = newService(s).execute();

        assertEquals(0, result.companiesProcessed());
        assertEquals(0, result.companiesUpdated());
        assertEquals(0, result.permissionsCreated());
        assertEquals(0, result.rolePermissionsCreated());
    }

    private PublishAdminPermissionsService newService(Stubs s) {
        return new PublishAdminPermissionsService(
            s.adminBaseRoleQueryPort(),
            s.adminBasePermissionsQueryPort(),
            s.companyCatalogQueryPort(),
            s.membershipSubModuleIdsQueryPort(),
            s.permissionUpsertPort(),
            s.rolePermissionUpsertPort()
        );
    }

    private static final class Stubs {
        Long adminBaseRoleId = null;
        final List<AdminBasePermission> adminBasePermissions = new ArrayList<>();
        final List<CompanyAdminContext> companies = new ArrayList<>();
        final Map<Long, Set<Long>> membershipSubModules = new HashMap<>();
        final Map<String, Long> permissionStore = new HashMap<>();
        long nextPermissionId = 1000L;
        final Set<String> rolePermissionLinks = new HashSet<>();

        AdminBaseRoleQueryPort adminBaseRoleQueryPort() {
            return () -> Optional.ofNullable(adminBaseRoleId);
        }

        AdminBasePermissionsQueryPort adminBasePermissionsQueryPort() {
            return baseRoleId -> List.copyOf(adminBasePermissions);
        }

        CompanyCatalogQueryPort companyCatalogQueryPort() {
            return () -> List.copyOf(companies);
        }

        MembershipSubModuleIdsQueryPort membershipSubModuleIdsQueryPort() {
            return ids -> {
                Map<Long, Set<Long>> filtered = new HashMap<>();
                for (Long id : ids) {
                    if (membershipSubModules.containsKey(id)) {
                        filtered.put(id, new HashSet<>(membershipSubModules.get(id)));
                    }
                }
                return filtered;
            };
        }

        PermissionUpsertPort permissionUpsertPort() {
            return (companyId, template) -> {
                String key = companyId + "|" + template.code();
                Long existing = permissionStore.get(key);
                if (existing != null) {
                    return new UpsertedPermission(existing, false);
                }
                long id = nextPermissionId++;
                permissionStore.put(key, id);
                return new UpsertedPermission(id, true);
            };
        }

        RolePermissionUpsertPort rolePermissionUpsertPort() {
            return (roleId, permissionId) -> {
                String key = roleId + "|" + permissionCodeFor(permissionId);
                if (rolePermissionLinks.contains(key)) return false;
                rolePermissionLinks.add(key);
                return true;
            };
        }

        private String permissionCodeFor(Long permissionId) {
            return permissionStore.entrySet().stream()
                .filter(e -> e.getValue().equals(permissionId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("unknown|" + permissionId);
        }
    }
}
