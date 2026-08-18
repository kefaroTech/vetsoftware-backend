package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDefaultMembershipProvider")
class JpaDefaultMembershipProviderTest {

    @Mock
    private MembershipJpaRepository membershipJpaRepository;
    @InjectMocks
    private JpaDefaultMembershipProvider provider;

    @Test
    @DisplayName("devuelve el id de la membresía marcada como obligatoria")
    void devuelve_el_id_de_la_membresia_obligatoria() {
        MembershipJpaEntity membresia = mock(MembershipJpaEntity.class);
        when(membresia.getId()).thenReturn(1L);
        when(membershipJpaRepository.findFirstByMandatoryTrue()).thenReturn(Optional.of(membresia));

        assertThat(provider.getDefaultMembershipId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("sin membresía obligatoria configurada falla explícitamente en vez de devolver null")
    void sin_membresia_obligatoria_falla_explicitamente() {
        when(membershipJpaRepository.findFirstByMandatoryTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getDefaultMembershipId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No mandatory membership found");
    }
}
