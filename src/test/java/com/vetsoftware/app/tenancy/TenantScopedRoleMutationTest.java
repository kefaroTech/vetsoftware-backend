package com.vetsoftware.app.tenancy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.role.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenCascadePort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.application.usecase.DeleteRoleService;
import com.vetsoftware.app.role.application.usecase.ReactivateRoleService;
import com.vetsoftware.app.role.application.usecase.UpdateRoleService;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantScopedRoleMutationTest {

    private static final Long ROLE_ID = 42L;
    private static final Long COMPANY_ID = 7L;

    @Test
    void updateLooksUpTheRoleInsideTheAuthenticatedCompany() {
        RoleRepository repository = mock(RoleRepository.class);
        CompanyQueryPort companyQueryPort = mock(CompanyQueryPort.class);
        when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID)).thenReturn(Optional.empty());
        var service = new UpdateRoleService(repository, companyQueryPort);

        assertThrows(RoleNotFoundException.class,
            () -> service.execute(new UpdateRoleCommand(ROLE_ID, "Manager", "MANAGER", COMPANY_ID)));

        verify(repository).findByIdAndCompanyId(ROLE_ID, COMPANY_ID);
        verifyNoMoreInteractions(repository, companyQueryPort);
    }

    @Test
    void deleteDoesNotFallBackToAGlobalRoleLookup() {
        RoleRepository repository = mock(RoleRepository.class);
        var service = new DeleteRoleService(
            repository,
            mock(EmployeeRoleChildrenQueryPort.class),
            mock(RolePermissionChildrenCascadePort.class));
        when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> service.execute(ROLE_ID, COMPANY_ID));

        verify(repository).findByIdAndCompanyId(ROLE_ID, COMPANY_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void reactivateUsesACompanyScopedUpdate() {
        RoleRepository repository = mock(RoleRepository.class);
        when(repository.reactivate(ROLE_ID, COMPANY_ID)).thenReturn(0);
        var service = new ReactivateRoleService(repository);

        assertThrows(RoleNotFoundException.class, () -> service.execute(ROLE_ID, COMPANY_ID));

        verify(repository).reactivate(ROLE_ID, COMPANY_ID);
        verifyNoMoreInteractions(repository);
    }
}
