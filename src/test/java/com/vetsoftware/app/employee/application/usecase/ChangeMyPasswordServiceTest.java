package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.ChangeMyPasswordCommand;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cambio de la propia contraseña (primer login forzado). El retorno indica si este cambio fue
 * además la aceptación de la invitación, dato que el borde usa para auditar. Se fija que la nueva
 * contraseña se hashea y que la sesión en curso sobrevive por diseño.
 */
@ExtendWith(MockitoExtension.class)
class ChangeMyPasswordServiceTest {

  @Mock private EmployeeRepository repository;
  @Mock private PasswordHasher passwordHasher;
  @InjectMocks private ChangeMyPasswordService service;

  private static Employee employee(boolean mustChangePassword) {
    return new Employee(
        55L,
        "VV-MARIANA",
        "$2a$10$old",
        "Mariana Rojas",
        "mariana@vetrina.co",
        new CompanyRef(9L, "Vetrina", "900123456"),
        LocalDateTime.now(),
        true,
        true,
        mustChangePassword,
        EmployeeStatus.INVITED,
        3L);
  }

  @Test
  void hashea_la_nueva_contrasena_antes_de_persistirla() {
    when(repository.findById(55L)).thenReturn(Optional.of(employee(true)));
    when(passwordHasher.hash("NuevaClave123*")).thenReturn("$2a$10$new");

    service.execute(new ChangeMyPasswordCommand(55L, "NuevaClave123*"));

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getHashPassword()).isEqualTo("$2a$10$new");
  }

  @Test
  void devuelve_true_cuando_el_cambio_acepta_la_invitacion() {
    when(repository.findById(55L)).thenReturn(Optional.of(employee(true)));
    when(passwordHasher.hash("NuevaClave123*")).thenReturn("$2a$10$new");

    boolean acceptedInvitation =
        service.execute(new ChangeMyPasswordCommand(55L, "NuevaClave123*"));

    assertThat(acceptedInvitation).isTrue();
  }

  @Test
  void devuelve_false_en_un_cambio_voluntario_posterior() {
    when(repository.findById(55L)).thenReturn(Optional.of(employee(false)));
    when(passwordHasher.hash("OtraClave123*")).thenReturn("$2a$10$new");

    boolean acceptedInvitation = service.execute(new ChangeMyPasswordCommand(55L, "OtraClave123*"));

    assertThat(acceptedInvitation).isFalse();
  }

  @Test
  void limpia_la_obligacion_de_cambiar_la_contrasena() {
    when(repository.findById(55L)).thenReturn(Optional.of(employee(true)));
    when(passwordHasher.hash("NuevaClave123*")).thenReturn("$2a$10$new");

    service.execute(new ChangeMyPasswordCommand(55L, "NuevaClave123*"));

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().isMustChangePassword()).isFalse();
  }

  @Test
  void no_invalida_la_sesion_en_curso() {
    // Diseño explícito: el empleado pasa directo al panel sin re-login, así que authVersion no
    // cambia.
    when(repository.findById(55L)).thenReturn(Optional.of(employee(true)));
    when(passwordHasher.hash("NuevaClave123*")).thenReturn("$2a$10$new");

    service.execute(new ChangeMyPasswordCommand(55L, "NuevaClave123*"));

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getAuthVersion()).isEqualTo(3L);
  }

  @Test
  void un_empleado_inexistente_no_puede_cambiar_contrasena() {
    when(repository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute(new ChangeMyPasswordCommand(404L, "x")))
        .isInstanceOf(EmployeeNotFoundException.class);

    verify(repository, never()).save(any());
  }

  @Test
  void una_contrasena_vacia_no_llega_a_persistirse() {
    when(repository.findById(55L)).thenReturn(Optional.of(employee(true)));
    when(passwordHasher.hash("   ")).thenReturn("   ");

    assertThatThrownBy(() -> service.execute(new ChangeMyPasswordCommand(55L, "   ")))
        .isInstanceOf(IllegalArgumentException.class);

    verify(repository, never()).save(any());
  }
}
