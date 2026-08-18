package com.vetsoftware.app.membership.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMembershipSubModuleChildrenQueryPort")
class JpaMembershipSubModuleChildrenQueryPortTest {

    @Mock
    private MembershipSubModuleJpaRepository jpaRepository;
    @InjectMocks
    private JpaMembershipSubModuleChildrenQueryPort port;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("delega en existsByMembership_Id cuando hay submodulos activos")
        void delega_cuando_hay_submodulos_activos() {
            when(jpaRepository.existsByMembership_Id(100L)).thenReturn(true);

            assertThat(port.existsActiveByMembershipId(100L)).isTrue();
        }

        @Test
        @DisplayName("delega en existsByMembership_Id cuando no hay submodulos activos")
        void delega_cuando_no_hay_submodulos_activos() {
            when(jpaRepository.existsByMembership_Id(100L)).thenReturn(false);

            assertThat(port.existsActiveByMembershipId(100L)).isFalse();
        }
    }
}
