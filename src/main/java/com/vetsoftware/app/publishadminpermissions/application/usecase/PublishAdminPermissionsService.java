package com.vetsoftware.app.publishadminpermissions.application.usecase;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;
import com.vetsoftware.app.publishadminpermissions.application.port.in.PublishAdminPermissionsUseCase;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermissionsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBaseRoleQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyCatalogQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyGrantedSubModuleIdsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.PermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.RolePermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.UpsertedPermission;
import com.vetsoftware.app.publishadminpermissions.domain.PublishAdminPermissionsResult;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "publish.admin.permissions.publish")
@Service
public class PublishAdminPermissionsService implements PublishAdminPermissionsUseCase {

    private final AdminBaseRoleQueryPort adminBaseRoleQueryPort;
    private final AdminBasePermissionsQueryPort adminBasePermissionsQueryPort;
    private final CompanyCatalogQueryPort companyCatalogQueryPort;
    private final CompanyGrantedSubModuleIdsQueryPort companyGrantedSubModuleIdsQueryPort;
    private final PermissionUpsertPort permissionUpsertPort;
    private final RolePermissionUpsertPort rolePermissionUpsertPort;

    public PublishAdminPermissionsService(AdminBaseRoleQueryPort adminBaseRoleQueryPort,
            AdminBasePermissionsQueryPort adminBasePermissionsQueryPort,
            CompanyCatalogQueryPort companyCatalogQueryPort,
            CompanyGrantedSubModuleIdsQueryPort companyGrantedSubModuleIdsQueryPort,
            PermissionUpsertPort permissionUpsertPort,
            RolePermissionUpsertPort rolePermissionUpsertPort) {
        this.adminBaseRoleQueryPort = adminBaseRoleQueryPort;
        this.adminBasePermissionsQueryPort = adminBasePermissionsQueryPort;
        this.companyCatalogQueryPort = companyCatalogQueryPort;
        this.companyGrantedSubModuleIdsQueryPort = companyGrantedSubModuleIdsQueryPort;
        this.permissionUpsertPort = permissionUpsertPort;
        this.rolePermissionUpsertPort = rolePermissionUpsertPort;
    }

    @Override
    @Transactional
    public PublishAdminPermissionsDto execute() {
        Long adminBaseRoleId = adminBaseRoleQueryPort.findAdminBaseRoleId()
                .orElseThrow(() -> new IllegalStateException("BaseRole 'ADMIN' not configured"));

        List<AdminBasePermission> adminBasePermissions = adminBasePermissionsQueryPort
                .findByAdminBaseRoleId(adminBaseRoleId);

        List<CompanyAdminContext> companies = companyCatalogQueryPort.findAllWithAdminRole();
        if (adminBasePermissions.isEmpty() || companies.isEmpty()) {
            return PublishAdminPermissionsDto
                    .from(new PublishAdminPermissionsResult(companies.size(), 0, 0, 0));
        }

        Set<Long> companyIds = companies.stream().map(CompanyAdminContext::companyId)
                .collect(Collectors.toSet());
        Map<Long, Set<Long>> subModulesByCompany = companyGrantedSubModuleIdsQueryPort
                .findGrantedSubModuleIdsByCompanyIds(companyIds);

        int companiesUpdated = 0;
        int permissionsCreated = 0;
        int rolePermissionsCreated = 0;

        for (CompanyAdminContext ctx : companies) {
            Set<Long> allowedSubModules = subModulesByCompany.getOrDefault(ctx.companyId(),
                    Set.of());
            boolean changedHere = false;
            for (AdminBasePermission tpl : adminBasePermissions) {
                if (!allowedSubModules.contains(tpl.subModuleId()))
                    continue;
                UpsertedPermission p = permissionUpsertPort.upsert(ctx.companyId(), tpl);
                if (p.created())
                    permissionsCreated++;
                if (rolePermissionUpsertPort.linkIfAbsent(ctx.adminRoleId(), p.id())) {
                    rolePermissionsCreated++;
                    changedHere = true;
                }
            }
            if (changedHere)
                companiesUpdated++;
        }

        return PublishAdminPermissionsDto.from(new PublishAdminPermissionsResult(companies.size(),
                companiesUpdated, permissionsCreated, rolePermissionsCreated));
    }
}
