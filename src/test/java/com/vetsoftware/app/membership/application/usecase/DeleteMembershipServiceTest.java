package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.application.port.out.MembershipSubModuleChildrenQueryPort;
import com.vetsoftware.app.membership.domain.MembershipHasActiveChildrenException;
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
@DisplayName("DeleteMembershipService")
class DeleteMembershipServiceTest {

    @Mock
    private MembershipRepository repository;
    @Mock
    private MembershipSubModuleChildrenQueryPort membershipSubModuleChildrenQueryPort;
    @InjectMocks
    private DeleteMembershipService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina la membresia cuando no tiene submodulos activos")
        void elimina_la_membresia_cuando_no_tiene_submodulos_activos() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));
            when(membershipSubModuleChildrenQueryPort
                    .existsActiveByMembershipId(MembershipMother.MEMBERSHIP_ID)).thenReturn(false);

            service.execute(MembershipMother.MEMBERSHIP_ID);

            verify(repository).delete(MembershipMother.MEMBERSHIP_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza MembershipNotFoundException si no existe y no consulta submodulos")
        void lanza_membership_not_found_si_no_existe() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipMother.MEMBERSHIP_ID))
                    .isInstanceOf(MembershipNotFoundException.class).hasMessageContaining(
                            "Membership not found: " + MembershipMother.MEMBERSHIP_ID);

            verifyNoInteractions(membershipSubModuleChildrenQueryPort);
            verify(repository, never()).delete(MembershipMother.MEMBERSHIP_ID);
        }

        @Test
        @DisplayName("no elimina si la membresia tiene submodulos activos")
        void no_elimina_si_la_membresia_tiene_submodulos_activos() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));
            when(membershipSubModuleChildrenQueryPort
                    .existsActiveByMembershipId(MembershipMother.MEMBERSHIP_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(MembershipMother.MEMBERSHIP_ID))
                    .isInstanceOf(MembershipHasActiveChildrenException.class)
                    .hasMessageContaining("membershipSubModule");

            verify(repository, never()).delete(MembershipMother.MEMBERSHIP_ID);
        }
    }
}
