package com.vetsoftware.app.branch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.application.command.UpdateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.application.port.out.CityQueryPort;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * UPDATE de sucursal. Valida scoping por empresa (no encontrar ⇒ excepción), unicidad de código
 * excluyéndose a sí misma, resolución de ciudad, y que la actualización sea atómica (un fallo de
 * precondición no muta el agregado ni escribe).
 */
@ExtendWith(MockitoExtension.class)
class UpdateBranchServiceTest {

  @Mock private BranchRepository repository;
  @Mock private CityQueryPort cityQueryPort;
  @InjectMocks private UpdateBranchService service;

  private final CityRef city = new CityRef(5L, "Bogotá");
  private final CityRef otherCity = new CityRef(7L, "Medellín");
  private final CompanyRef company = new CompanyRef(9L, "Vet SAS", "900123456");
  private final LocalDateTime created = LocalDateTime.of(2020, 1, 1, 10, 0);

  private Branch existing() {
    return new Branch(3L, "Old", "OLD", "addr", "phone", city, company, created, true);
  }

  @Test
  void actualiza_campos_conserva_empresa_y_devuelve_dto() {
    Branch branch = existing();
    when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(branch));
    when(repository.codeExistsForOther(9L, "NEW", 3L)).thenReturn(false);
    when(cityQueryPort.findById(7L)).thenReturn(Optional.of(otherCity));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    BranchDto dto =
        service.execute(new UpdateBranchCommand(3L, "New", "NEW", "addr2", "phone2", 7L, 9L));

    assertThat(branch.getName()).isEqualTo("New");
    assertThat(branch.getCity()).isEqualTo(otherCity);
    assertThat(branch.getCompany()).as("update no debe cambiar la empresa").isEqualTo(company);
    assertThat(branch.getCreatedDate()).isEqualTo(created);
    verify(repository).save(branch);
    assertThat(dto.code()).isEqualTo("NEW");
    assertThat(dto.city().id()).isEqualTo(7L);
  }

  @Test
  void hace_trim_del_codigo() {
    Branch branch = existing();
    when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(branch));
    when(repository.codeExistsForOther(9L, "NEW", 3L)).thenReturn(false);
    when(cityQueryPort.findById(5L)).thenReturn(Optional.of(city));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.execute(new UpdateBranchCommand(3L, "New", "  NEW  ", "a", "p", 5L, 9L));

    verify(repository).codeExistsForOther(9L, "NEW", 3L);
    assertThat(branch.getCode()).isEqualTo("NEW");
  }

  @Test
  void falla_si_no_existe_en_la_empresa() {
    when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.execute(new UpdateBranchCommand(3L, "New", "NEW", "a", "p", 7L, 9L)))
        .isInstanceOf(BranchNotFoundException.class);

    verify(repository, never()).save(any());
    verifyNoInteractions(cityQueryPort);
  }

  @Test
  void rechaza_codigo_en_uso_por_otra_sucursal_y_no_muta_ni_escribe() {
    Branch branch = existing();
    when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(branch));
    when(repository.codeExistsForOther(9L, "DUP", 3L)).thenReturn(true);

    assertThatThrownBy(
            () -> service.execute(new UpdateBranchCommand(3L, "New", "DUP", "a", "p", 7L, 9L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already in use");

    assertThat(branch.getName()).as("no debe mutar ante código duplicado").isEqualTo("Old");
    verify(repository, never()).save(any());
    verifyNoInteractions(cityQueryPort);
  }

  @Test
  void falla_si_la_ciudad_no_existe_y_deja_el_agregado_intacto() {
    Branch branch = existing();
    when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(branch));
    when(repository.codeExistsForOther(9L, "NEW", 3L)).thenReturn(false);
    when(cityQueryPort.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.execute(new UpdateBranchCommand(3L, "New", "NEW", "a", "p", 7L, 9L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("City not found");

    assertThat(branch.getName()).isEqualTo("Old");
    assertThat(branch.getCity()).isEqualTo(city);
    verify(repository, never()).save(any());
  }
}
