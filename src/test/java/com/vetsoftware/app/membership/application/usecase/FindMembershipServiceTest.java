package com.vetsoftware.app.membership.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindMembershipService")
class FindMembershipServiceTest {

    @Mock
    private MembershipRepository repository;
    @InjectMocks
    private FindMembershipService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve la membresia encontrada")
        void devuelve_la_membresia_encontrada() {
            when(repository.findById(MembershipMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipMother.activa()));

            MembershipDto dto = service.findById(MembershipMother.MEMBERSHIP_ID);

            assertThat(dto.id()).isEqualTo(MembershipMother.MEMBERSHIP_ID);
            assertThat(dto.name()).isEqualTo("Plan Oro");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza MembershipNotFoundException si no existe")
        void lanza_membership_not_found_si_no_existe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(MembershipNotFoundException.class)
                    .hasMessageContaining("Membership not found: 999");
        }
    }
}
