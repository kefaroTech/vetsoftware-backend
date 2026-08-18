package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMembershipService")
class CreateMembershipServiceTest {

    @Mock
    private MembershipRepository repository;
    @InjectMocks
    private CreateMembershipService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la membresia con el estado resuelto del command")
        void persiste_la_membresia_con_el_estado_resuelto_del_command() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MembershipDto dto = service.execute(MembershipMother.comandoCrear());

            ArgumentCaptor<Membership> guardada = ArgumentCaptor.forClass(Membership.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Plan Oro");
            assertThat(guardada.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(guardada.getValue().isMandatory()).isFalse();
            assertThat(guardada.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Plan Oro");
        }

        @Test
        @DisplayName("el estado del command se normaliza a mayusculas")
        void el_estado_del_command_se_normaliza_a_mayusculas() {
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new CreateMembershipCommand("Plan Oro", "active", false));

            ArgumentCaptor<Membership> guardada = ArgumentCaptor.forClass(Membership.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("un estado desconocido no toca el repositorio")
        void un_estado_desconocido_no_toca_el_repositorio() {
            CreateMembershipCommand command = new CreateMembershipCommand("Plan Oro", "BOGUS",
                    false);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un nombre en blanco no toca el repositorio")
        void un_nombre_en_blanco_no_toca_el_repositorio() {
            CreateMembershipCommand command = new CreateMembershipCommand("   ", "ACTIVE", false);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verifyNoInteractions(repository);
        }
    }
}
