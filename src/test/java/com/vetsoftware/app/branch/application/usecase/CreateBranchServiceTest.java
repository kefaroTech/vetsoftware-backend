package com.vetsoftware.app.branch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.application.port.out.CityQueryPort;
import com.vetsoftware.app.branch.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.branch.application.port.out.FullCoverageBranchAssignmentPort;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CREATE de sucursal. Valida que se resuelva ciudad+empresa, se aplique unicidad de código (con trim),
 * se persista una sucursal ACTIVA con los datos correctos, y que ningún fallo de precondición deje escribir.
 */
@ExtendWith(MockitoExtension.class)
class CreateBranchServiceTest {

    @Mock private BranchRepository repository;
    @Mock private CityQueryPort cityQueryPort;
    @Mock private CompanyQueryPort companyQueryPort;
    @Mock private FullCoverageBranchAssignmentPort fullCoverageAssignmentPort;
    @InjectMocks private CreateBranchService service;

    private final CityRef city = new CityRef(5L, "Bogotá");
    private final CompanyRef company = new CompanyRef(9L, "Vet SAS", "900123456");

    private static Branch withId(Long id, Branch b) {
        return new Branch(id, b.getName(), b.getCode(), b.getAddress(), b.getPhone(),
            b.getCity(), b.getCompany(), b.getCreatedDate(), b.isActive());
    }

    @Test
    void crea_sucursal_activa_y_devuelve_dto_persistido() {
        when(repository.codeExists(9L, "NORTE")).thenReturn(false);
        when(cityQueryPort.findById(5L)).thenReturn(Optional.of(city));
        when(companyQueryPort.findById(9L)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenAnswer(inv -> withId(1L, inv.getArgument(0)));

        BranchDto dto = service.execute(
            new CreateBranchCommand("Sede Norte", "NORTE", "Cra 1", "3001112233", 5L, 9L));

        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(repository).save(captor.capture());
        Branch saved = captor.getValue();
        assertThat(saved.getId()).as("una creación no debe traer id").isNull();
        assertThat(saved.getName()).isEqualTo("Sede Norte");
        assertThat(saved.getCode()).isEqualTo("NORTE");
        assertThat(saved.getAddress()).isEqualTo("Cra 1");
        assertThat(saved.getPhone()).isEqualTo("3001112233");
        assertThat(saved.getCity()).isEqualTo(city);
        assertThat(saved.getCompany()).isEqualTo(company);
        assertThat(saved.isActive()).isTrue();

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.code()).isEqualTo("NORTE");
        assertThat(dto.city().id()).isEqualTo(5L);
        assertThat(dto.company().id()).isEqualTo(9L);
        assertThat(dto.company().identifier()).isEqualTo("900123456");
        assertThat(dto.active()).isTrue();

        // La sede recién creada se auto-asigna a los empleados "con todas las sedes" de la empresa.
        verify(fullCoverageAssignmentPort).assignNewBranchToFullCoverageEmployees(9L, 1L);
    }

    @Test
    void hace_trim_del_codigo_para_unicidad_y_persistencia() {
        when(repository.codeExists(9L, "NORTE")).thenReturn(false);
        when(cityQueryPort.findById(5L)).thenReturn(Optional.of(city));
        when(companyQueryPort.findById(9L)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenAnswer(inv -> withId(1L, inv.getArgument(0)));

        service.execute(new CreateBranchCommand("Sede", "  NORTE  ", null, null, 5L, 9L));

        // el chequeo de unicidad y lo persistido deben usar el código ya recortado.
        verify(repository).codeExists(9L, "NORTE");
        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("NORTE");
    }

    @Test
    void rechaza_codigo_duplicado_y_no_escribe_ni_consulta_dependencias() {
        when(repository.codeExists(9L, "NORTE")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                new CreateBranchCommand("Sede", "NORTE", null, null, 5L, 9L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already in use");

        verify(repository, never()).save(any());
        verifyNoInteractions(cityQueryPort, companyQueryPort);
    }

    @Test
    void falla_si_la_ciudad_no_existe_y_no_escribe() {
        when(repository.codeExists(9L, "NORTE")).thenReturn(false);
        when(cityQueryPort.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new CreateBranchCommand("Sede", "NORTE", null, null, 5L, 9L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("City not found");

        verify(repository, never()).save(any());
    }

    @Test
    void falla_si_la_empresa_no_existe_y_no_escribe() {
        when(repository.codeExists(9L, "NORTE")).thenReturn(false);
        when(cityQueryPort.findById(5L)).thenReturn(Optional.of(city));
        when(companyQueryPort.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new CreateBranchCommand("Sede", "NORTE", null, null, 5L, 9L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Company not found");

        verify(repository, never()).save(any());
    }
}
