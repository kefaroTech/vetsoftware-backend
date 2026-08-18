package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMembershipService")
class UpdateMembershipServiceTest {

    @Mock
    private MembershipRepository repository;
    @InjectMocks
    private UpdateMembershipService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza la membresia encontrada con los datos del command")
        void actualiza_la_membresia_encontrada_con_los_datos_del_command() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MembershipDto dto = service.execute(MembershipMother.comandoActualizar());

            ArgumentCaptor<Membership> guardada = ArgumentCaptor.forClass(Membership.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Plan Platino");
            assertThat(guardada.getValue().getStatus()).isEqualTo(MembershipStatus.DEPRECATED);
            assertThat(guardada.getValue().isMandatory()).isTrue();
            assertThat(dto.name()).isEqualTo("Plan Platino");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza MembershipNotFoundException si la membresia no existe y no guarda")
        void lanza_membership_not_found_si_la_membresia_no_existe() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipMother.comandoActualizar()))
                    .isInstanceOf(MembershipNotFoundException.class).hasMessageContaining(
                            "Membership not found: " + MembershipMother.MEMBERSHIP_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un estado desconocido no guarda, aunque la membresia exista")
        void un_estado_desconocido_no_guarda() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));
            UpdateMembershipCommand command = new UpdateMembershipCommand(
                    MembershipMother.MEMBERSHIP_ID, "Plan Platino", "BOGUS", true);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }
    }
}
