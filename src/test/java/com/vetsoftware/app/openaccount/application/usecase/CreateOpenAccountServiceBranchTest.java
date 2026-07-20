package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.BranchQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Resolución de sede al abrir cuenta (multi-sucursal). Reglas: sede solicitada debe ser de la empresa y estar
 * ACTIVA (inactiva ⇒ error distinto de inexistente); sin branchId, la sede ACTIVA por defecto. Además la
 * invariante del get-or-create: una cuenta OPEN se reutiliza únicamente dentro de la misma sede; el mismo dueño
 * puede mantener cuentas independientes en sedes diferentes.
 */
@ExtendWith(MockitoExtension.class)
class CreateOpenAccountServiceBranchTest {

    @Mock private OpenAccountRepository repository;
    @Mock private OwnerQueryPort ownerQueryPort;
    @Mock private CompanyQueryPort companyQueryPort;
    @Mock private EmployeeQueryPort employeeQueryPort;
    @Mock private BranchQueryPort branchQueryPort;
    @InjectMocks private CreateOpenAccountService service;

    private static final long COMPANY = 9L;
    private final OwnerRef owner = new OwnerRef(2L, "Juan", "CC123");
    private final CompanyRef company = new CompanyRef(COMPANY, "Vet SAS", "900123456");
    private final EmployeeRef createdBy = new EmployeeRef(4L, "Dra. Vet");
    private final BranchRef requested = new BranchRef(11L, "Sede Norte", "NORTE");
    private final BranchRef principal = new BranchRef(1L, "Principal", "PRINCIPAL");

    private static PageResult<OpenAccount> page(OpenAccount... accounts) {
        return new PageResult<>(List.of(accounts), 0, 50, accounts.length, accounts.length == 0 ? 0 : 1);
    }

    private void stubNewAccountLookups() {
        when(ownerQueryPort.findById(2L)).thenReturn(Optional.of(owner));
        when(companyQueryPort.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeQueryPort.findById(4L)).thenReturn(Optional.of(createdBy));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crea_con_la_sede_solicitada_cuando_es_valida_y_activa() {
        when(repository.search(any())).thenReturn(page());
        stubNewAccountLookups();
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(requested));

        OpenAccountDto dto = service.execute(new CreateOpenAccountCommand(2L, 11L, COMPANY, 4L));

        ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
        verify(repository).save(captor.capture());
        OpenAccount saved = captor.getValue();
        assertThat(saved.getBranch()).isEqualTo(requested);
        assertThat(saved.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
        assertThat(dto.branch().id()).isEqualTo(11L);
        ArgumentCaptor<SearchOpenAccountsCommand> searchCaptor =
            ArgumentCaptor.forClass(SearchOpenAccountsCommand.class);
        verify(repository).search(searchCaptor.capture());
        assertThat(searchCaptor.getValue().branchId()).isEqualTo(11L);
        verify(branchQueryPort, never()).findDefaultActiveByCompanyId(any());
    }

    @Test
    void crea_con_la_sede_activa_por_defecto_cuando_no_viene_branchId() {
        when(repository.search(any())).thenReturn(page());
        stubNewAccountLookups();
        when(branchQueryPort.findDefaultActiveByCompanyId(COMPANY)).thenReturn(Optional.of(principal));

        OpenAccountDto dto = service.execute(new CreateOpenAccountCommand(2L, null, COMPANY, 4L));

        ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBranch()).isEqualTo(principal);
        assertThat(dto.branch().code()).isEqualTo("PRINCIPAL");
        verify(branchQueryPort, never()).findActiveByIdAndCompanyId(any(), any());
    }

    @Test
    void rechaza_una_sede_solicitada_INACTIVA_con_error_distinto_y_no_escribe() {
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());
        when(branchQueryPort.existsByIdAndCompanyId(11L, COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new CreateOpenAccountCommand(2L, 11L, COMPANY, 4L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Branch is not active")
            .hasMessageContaining("11");

        verify(repository, never()).save(any());
        verifyNoInteractions(repository, ownerQueryPort, companyQueryPort, employeeQueryPort);
    }

    @Test
    void rechaza_una_sede_inexistente_o_ajena_con_error_not_found_y_no_escribe() {
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());
        when(branchQueryPort.existsByIdAndCompanyId(11L, COMPANY)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new CreateOpenAccountCommand(2L, 11L, COMPANY, 4L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Branch not found");

        verify(repository, never()).save(any());
        verifyNoInteractions(repository, ownerQueryPort, companyQueryPort, employeeQueryPort);
    }

    @Test
    void falla_si_la_empresa_no_tiene_ninguna_sede_activa_y_no_escribe() {
        when(branchQueryPort.findDefaultActiveByCompanyId(COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CreateOpenAccountCommand(2L, null, COMPANY, 4L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Company has no active branch");

        verify(repository, never()).save(any());
        verifyNoInteractions(repository, ownerQueryPort, companyQueryPort, employeeQueryPort);
    }

    @Test
    void reutiliza_la_cuenta_open_existente_en_la_misma_sede() {
        OpenAccount existing = OpenAccount.create(owner, company, requested, createdBy);
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(requested));
        when(repository.search(any())).thenReturn(page(existing));

        OpenAccountDto dto = service.execute(new CreateOpenAccountCommand(2L, 11L, COMPANY, 4L));

        assertThat(dto.branch().code()).isEqualTo("NORTE");
        verify(repository, never()).save(any());
        verifyNoInteractions(ownerQueryPort, companyQueryPort, employeeQueryPort);
    }

    @Test
    void crea_una_nueva_open_si_la_unica_cuenta_previa_esta_cerrada() {
        OpenAccount closed = OpenAccount.create(owner, company, principal, createdBy);
        closed.changeStatus(OpenAccountStatus.CLOSE, createdBy, null); // saldo 0 ⇒ cierre válido
        when(repository.search(any())).thenReturn(page(closed));
        stubNewAccountLookups();
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(requested));

        service.execute(new CreateOpenAccountCommand(2L, 11L, COMPANY, 4L));

        ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
        verify(repository).save(captor.capture());
        OpenAccount saved = captor.getValue();
        assertThat(saved.getStatus()).as("una cuenta cerrada no se reutiliza").isEqualTo(OpenAccountStatus.OPEN);
        assertThat(saved.getBranch()).isEqualTo(requested);
    }
}
