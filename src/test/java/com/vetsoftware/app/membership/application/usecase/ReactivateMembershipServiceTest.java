package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.testsupport.MembershipMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateMembershipService")
class ReactivateMembershipServiceTest {

    @Mock
    private MembershipRepository repository;
    @InjectMocks
    private ReactivateMembershipService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la membresia releida")
        void reactiva_y_devuelve_la_membresia_releida() {
            when(repository.reactivate(MembershipMother.MEMBERSHIP_ID)).thenReturn(1);
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));

            MembershipDto dto = service.execute(MembershipMother.MEMBERSHIP_ID);

            assertThat(dto.id()).isEqualTo(MembershipMother.MEMBERSHIP_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza MembershipNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_membership_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(MembershipMother.MEMBERSHIP_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(MembershipMother.MEMBERSHIP_ID))
                    .isInstanceOf(MembershipNotFoundException.class).hasMessageContaining(
                            "Membership not found: " + MembershipMother.MEMBERSHIP_ID);

            verify(repository, never()).findById(MembershipMother.MEMBERSHIP_ID);
        }

        @Test
        @DisplayName("lanza MembershipNotFoundException si reactivo una fila pero la relectura no la encuentra")
        void lanza_membership_not_found_si_la_relectura_no_encuentra_la_fila() {
            when(repository.reactivate(MembershipMother.MEMBERSHIP_ID)).thenReturn(1);
            when(repository.findById(MembershipMother.MEMBERSHIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipMother.MEMBERSHIP_ID))
                    .isInstanceOf(MembershipNotFoundException.class);
        }
    }
}
