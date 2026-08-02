package com.vetsoftware.app.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.port.in.CreateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.DeleteConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.FindConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.application.port.in.ReactivateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.UpdateConsultationUseCase;
import com.vetsoftware.app.consultation.infrastructure.web.ConsultationController;
import com.vetsoftware.app.consultation.infrastructure.web.request.UpdateConsultationRequest;
import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.port.in.ChangeMyPasswordUseCase;
import com.vetsoftware.app.employee.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.InviteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesByCompanyUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.in.ReactivateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.ResendInvitationUseCase;
import com.vetsoftware.app.employee.application.port.in.SearchEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.employee.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.employee.infrastructure.web.EmployeeController;
import com.vetsoftware.app.employee.infrastructure.web.request.CreateEmployeeRequest;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.FindPermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsByCompanyUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsUseCase;
import com.vetsoftware.app.permission.application.port.in.ReactivatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.infrastructure.web.PermissionController;
import com.vetsoftware.app.permission.infrastructure.web.request.CreatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.request.UpdatePermissionRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TenantRequestCompanyResolutionTest {

  private static final long AUTHENTICATED_COMPANY_ID = 42L;

  @Test
  void employeeCreationUsesAuthenticatedCompany() {
    Authz authz = authenticatedCompany();
    InviteEmployeeUseCase inviteUseCase = mock(InviteEmployeeUseCase.class);
    when(inviteUseCase.execute(any())).thenThrow(new TestFlowStopped());
    EmployeeController controller =
        new EmployeeController(
            inviteUseCase,
            mock(ResendInvitationUseCase.class),
            mock(ChangeMyPasswordUseCase.class),
            mock(UpdateEmployeeUseCase.class),
            mock(FindEmployeeUseCase.class),
            mock(ListEmployeesUseCase.class),
            mock(ListEmployeesByCompanyUseCase.class),
            mock(SearchEmployeesUseCase.class),
            mock(DeleteEmployeeUseCase.class),
            mock(ReactivateEmployeeUseCase.class),
            mock(SuggestEmployeeCodeUseCase.class),
            mock(CheckEmployeeCodeAvailabilityUseCase.class),
            authz,
            mock(AuditLogger.class));
    List<Long> branches = List.of(3L);

    assertThatThrownBy(
            () ->
                controller.create(
                    new CreateEmployeeRequest(
                        "EMP-1",
                        "temporary-password",
                        "Ana",
                        "ana@example.com",
                        List.of(2L),
                        branches)))
        .isInstanceOf(TestFlowStopped.class);

    ArgumentCaptor<InviteEmployeeCommand> command =
        ArgumentCaptor.forClass(InviteEmployeeCommand.class);
    verify(inviteUseCase).execute(command.capture());
    verify(authz).requireAssignableBranches(branches);
    assertThat(command.getValue().companyId()).isEqualTo(AUTHENTICATED_COMPANY_ID);
  }

  @Test
  void consultationUpdateUsesAuthenticatedCompany() {
    Authz authz = authenticatedCompany();
    UpdateConsultationUseCase updateUseCase = mock(UpdateConsultationUseCase.class);
    when(updateUseCase.execute(any())).thenThrow(new TestFlowStopped());
    ConsultationController controller =
        new ConsultationController(
            mock(CreateConsultationUseCase.class),
            updateUseCase,
            mock(FindConsultationUseCase.class),
            mock(ListConsultationsUseCase.class),
            mock(DeleteConsultationUseCase.class),
            mock(ReactivateConsultationUseCase.class),
            authz);

    assertThatThrownBy(
            () ->
                controller.update(
                    8L,
                    new UpdateConsultationRequest(
                        LocalDate.now(),
                        1L,
                        "Anamnesis",
                        null,
                        null,
                        null,
                        9L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
        .isInstanceOf(TestFlowStopped.class);

    ArgumentCaptor<UpdateConsultationCommand> command =
        ArgumentCaptor.forClass(UpdateConsultationCommand.class);
    verify(updateUseCase).execute(command.capture());
    assertThat(command.getValue().companyId()).isEqualTo(AUTHENTICATED_COMPANY_ID);
  }

  @Test
  void permissionCreationUsesAuthenticatedCompanyScope() {
    Authz authz = authenticatedCompany();
    CreatePermissionUseCase createUseCase = mock(CreatePermissionUseCase.class);
    when(createUseCase.execute(any())).thenThrow(new TestFlowStopped());
    PermissionController controller =
        permissionController(createUseCase, mock(UpdatePermissionUseCase.class), authz);

    assertThatThrownBy(
            () -> controller.create(new CreatePermissionRequest("Create", "x.create", 7L)))
        .isInstanceOf(TestFlowStopped.class);

    ArgumentCaptor<CreatePermissionCommand> command =
        ArgumentCaptor.forClass(CreatePermissionCommand.class);
    verify(createUseCase).execute(command.capture());
    assertThat(command.getValue().companyId()).isEqualTo(AUTHENTICATED_COMPANY_ID);
  }

  @Test
  void permissionUpdateUsesAuthenticatedCompanyScope() {
    Authz authz = authenticatedCompany();
    UpdatePermissionUseCase updateUseCase = mock(UpdatePermissionUseCase.class);
    when(updateUseCase.execute(any())).thenThrow(new TestFlowStopped());
    PermissionController controller =
        permissionController(mock(CreatePermissionUseCase.class), updateUseCase, authz);

    assertThatThrownBy(
            () -> controller.update(5L, new UpdatePermissionRequest("Update", "x.update", 7L)))
        .isInstanceOf(TestFlowStopped.class);

    ArgumentCaptor<UpdatePermissionCommand> command =
        ArgumentCaptor.forClass(UpdatePermissionCommand.class);
    verify(updateUseCase).execute(command.capture());
    assertThat(command.getValue().companyId()).isEqualTo(AUTHENTICATED_COMPANY_ID);
  }

  private static Authz authenticatedCompany() {
    Authz authz = mock(Authz.class);
    when(authz.currentCompanyId()).thenReturn(AUTHENTICATED_COMPANY_ID);
    return authz;
  }

  private static PermissionController permissionController(
      CreatePermissionUseCase createUseCase, UpdatePermissionUseCase updateUseCase, Authz authz) {
    return new PermissionController(
        createUseCase,
        updateUseCase,
        mock(FindPermissionUseCase.class),
        mock(ListPermissionsUseCase.class),
        mock(ListPermissionsByCompanyUseCase.class),
        mock(DeletePermissionUseCase.class),
        mock(ReactivatePermissionUseCase.class),
        authz);
  }

  private static final class TestFlowStopped extends RuntimeException {
    private TestFlowStopped() {
      super(null, null, false, false);
    }
  }
}
