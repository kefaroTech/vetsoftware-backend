package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeBranchAssigner;
import com.vetsoftware.app.employee.application.port.out.EmployeeCapacityPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.employee.application.port.out.EmployeePasswordHasherPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Alta de staff por un admin: es el camino por el que nace una credencial de
 * acceso al sistema. Se fija que la contraseña se hashea antes de persistir,
 * que el empleado no puede quedar sin sede (quedaría bloqueado de todo recurso
 * scopeado) y que el correo con la clave temporal se envía después de que el
 * alta ya cuadró.
 */
@ExtendWith(MockitoExtension.class)
class InviteEmployeeServiceTest {

    private static final Long COMPANY = 9L;

    @Mock
    private EmployeeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private EmployeePasswordHasherPort passwordHasher;
    @Mock
    private EmployeeRoleAssigner roleAssigner;
    @Mock
    private EmployeeBranchAssigner branchAssigner;
    @Mock
    private EmployeeInvitationEmailSender invitationEmailSender;
    @Mock
    private EmployeeCapacityPort employeeCapacityPort;
    @InjectMocks
    private InviteEmployeeService service;

    private static CompanyRef company() {
        return new CompanyRef(COMPANY, "Veterinaria Vetrina", "900123456");
    }

    private static InviteEmployeeCommand command(List<Long> roleIds, List<Long> branchIds) {
        return new InviteEmployeeCommand("VV-MARIANA", "Temporal123*", "Mariana Rojas",
                "mariana@vetrina.co", COMPANY, roleIds, branchIds);
    }

    private static Employee persisted() {
        return new Employee(55L, "VV-MARIANA", "$2a$10$hashed", "Mariana Rojas",
                "mariana@vetrina.co", company(), LocalDateTime.now(), null, true, true, true,
                EmployeeStatus.INVITED, 0L);
    }

    @BeforeEach
    void setUp() {
        lenient().when(companyQueryPort.findById(COMPANY)).thenReturn(Optional.of(company()));
        lenient().when(passwordHasher.hash("Temporal123*")).thenReturn("$2a$10$hashed");
        lenient().when(repository.save(any())).thenReturn(persisted());
        lenient().when(roleAssigner.assign(anyLong(), anyLong(), anyLong()))
                .thenReturn("Veterinario");
    }

    @Nested
    class AltaCorrecta {

        @Test
        void persiste_el_empleado_con_la_contrasena_hasheada_nunca_en_claro() {
            service.execute(command(List.of(3L), List.of(7L)));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHashPassword()).isEqualTo("$2a$10$hashed");
            assertThat(captor.getValue().getHashPassword()).isNotEqualTo("Temporal123*");
            verify(passwordHasher).hash("Temporal123*");
            verify(employeeCapacityPort).reserve(COMPANY);
        }

        @Test
        void el_staff_invitado_nace_verificado_y_obligado_a_cambiar_la_clave() {
            service.execute(command(List.of(3L), List.of(7L)));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isEmailVerified()).isTrue();
            assertThat(captor.getValue().isMustChangePassword()).isTrue();
            assertThat(captor.getValue().getStatus()).isEqualTo(EmployeeStatus.INVITED);
        }

        /**
         * La empresa del alta viaja con cada asignación: es con lo que
         * {@code employeerole} acota la resolución del empleado y del rol. Sin ella un
         * admin podía colgarle a su staff un rol de otro tenant.
         */
        @Test
        void asigna_todos_los_roles_recibidos_con_la_empresa_del_alta() {
            service.execute(command(List.of(3L, 4L), List.of(7L)));

            verify(roleAssigner).assign(55L, COMPANY, 3L);
            verify(roleAssigner).assign(55L, COMPANY, 4L);
        }

        @Test
        void asigna_las_sedes_dentro_de_la_misma_transaccion_del_alta() {
            service.execute(command(List.of(3L), List.of(7L, 8L)));

            verify(branchAssigner).assign(55L, COMPANY, List.of(7L, 8L));
        }

        @Test
        void envia_la_invitacion_con_la_clave_temporal_en_claro_solo_al_correo() {
            when(roleAssigner.assign(55L, COMPANY, 3L)).thenReturn("Veterinario");
            when(roleAssigner.assign(55L, COMPANY, 4L)).thenReturn("Cajero");

            service.execute(command(List.of(3L, 4L), List.of(7L)));

            verify(invitationEmailSender).send("mariana@vetrina.co", "Mariana Rojas",
                    "Veterinaria Vetrina", "VV-MARIANA", "Temporal123*", "Veterinario, Cajero");
        }

        @Test
        void guarda_antes_de_asignar_roles_sedes_y_de_enviar_el_correo() {
            // El INSERT valida la unicidad del código: si el correo saliera primero, un
            // código duplicado
            // dejaría una invitación enviada para una cuenta que nunca existió.
            service.execute(command(List.of(3L), List.of(7L)));

            InOrder order = inOrder(employeeCapacityPort, repository, roleAssigner, branchAssigner,
                    invitationEmailSender);
            order.verify(employeeCapacityPort).reserve(COMPANY);
            order.verify(repository).save(any());
            order.verify(roleAssigner).assign(55L, COMPANY, 3L);
            order.verify(branchAssigner).assign(55L, COMPANY, List.of(7L));
            order.verify(invitationEmailSender).send(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString());
        }

        @Test
        void devuelve_el_empleado_creado_con_su_empresa() {
            EmployeeDto dto = service.execute(command(List.of(3L), List.of(7L)));

            assertThat(dto.id()).isEqualTo(55L);
            assertThat(dto.employeeCode()).isEqualTo("VV-MARIANA");
            assertThat(dto.company().id()).isEqualTo(COMPANY);
        }
    }

    @Nested
    class Rechazos {

        @Test
        void un_empleado_no_puede_crearse_sin_sede() {
            assertThatThrownBy(() -> service.execute(command(List.of(3L), List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one branch is required");

            verify(repository, never()).save(any());
            verify(invitationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString());
        }

        @Test
        void sedes_nulas_se_tratan_como_ausentes() {
            assertThatThrownBy(() -> service.execute(command(List.of(3L), null)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void una_empresa_inexistente_aborta_el_alta_antes_de_hashear() {
            when(companyQueryPort.findById(COMPANY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command(List.of(3L), List.of(7L))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found");

            verify(passwordHasher, never()).hash(anyString());
            verify(repository, never()).save(any());
        }

        @Test
        void si_falla_la_asignacion_de_sede_no_se_envia_la_invitacion() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException("branch of another company"))
                    .when(branchAssigner).assign(anyLong(), anyLong(), any());

            assertThatThrownBy(() -> service.execute(command(List.of(3L), List.of(99L))))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(invitationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString());
        }

        @Test
        void el_limite_user_aborta_antes_de_persistir_roles_sedes_y_correo() {
            doThrow(new CompanyCapacityLimitExceededException(COMPANY, "USER", 5, 5, 1))
                    .when(employeeCapacityPort).reserve(COMPANY);

            assertThatThrownBy(() -> service.execute(command(List.of(3L), List.of(7L))))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class)
                    .hasMessageContaining("USER");

            verify(repository, never()).save(any());
            verify(roleAssigner, never()).assign(anyLong(), anyLong(), anyLong());
            verify(branchAssigner, never()).assign(anyLong(), anyLong(), any());
            verify(invitationEmailSender, never()).send(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString());
        }
    }
}
