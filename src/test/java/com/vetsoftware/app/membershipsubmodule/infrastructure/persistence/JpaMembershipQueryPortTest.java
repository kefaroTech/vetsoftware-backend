package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMembershipQueryPort — adaptador sobre MembershipJpaRepository")
class JpaMembershipQueryPortTest {

    @Mock
    private MembershipJpaRepository membershipJpaRepository;
    @Mock
    private MembershipJpaEntity membershipEntity;

    private JpaMembershipQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaMembershipQueryPort(membershipJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a su companion VO")
        void mapea_la_entidad_encontrada_a_su_companion_vo() {
            when(membershipEntity.getId()).thenReturn(MembershipSubModuleMother.MEMBERSHIP_ID);
            when(membershipEntity.getName())
                    .thenReturn(MembershipSubModuleMother.PLAN_PREMIUM.name());
            when(membershipJpaRepository.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membershipEntity));

            Optional<MembershipRef> resultado = port
                    .findById(MembershipSubModuleMother.MEMBERSHIP_ID);

            assertThat(resultado).contains(MembershipSubModuleMother.PLAN_PREMIUM);
        }

        @Test
        @DisplayName("una membresia inexistente devuelve vacio")
        void una_membresia_inexistente_devuelve_vacio() {
            when(membershipJpaRepository.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.empty());

            Optional<MembershipRef> resultado = port
                    .findById(MembershipSubModuleMother.MEMBERSHIP_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
