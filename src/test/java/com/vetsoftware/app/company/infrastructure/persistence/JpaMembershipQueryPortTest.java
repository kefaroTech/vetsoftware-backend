package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.company.testsupport.ReflectionEntities;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMembershipQueryPort (company) — arma el MembershipRef desde MembershipJpaEntity")
class JpaMembershipQueryPortTest {

    @Mock
    private MembershipJpaRepository membershipJpaRepository;

    private JpaMembershipQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaMembershipQueryPort(membershipJpaRepository);
    }

    @Test
    @DisplayName("membresia existente: mapea id, nombre y estado")
    void membresia_existente_mapea_id_nombre_y_estado() throws ReflectiveOperationException {
        MembershipJpaEntity entity = ReflectionEntities.newInstance(MembershipJpaEntity.class);
        entity.setId(21L);
        entity.setName("Premium");
        entity.setStatus("ACTIVE");
        when(membershipJpaRepository.findById(21L)).thenReturn(Optional.of(entity));

        Optional<MembershipRef> result = port.findById(21L);

        assertThat(result).contains(new MembershipRef(21L, "Premium", "ACTIVE"));
    }

    @Test
    @DisplayName("membresia inexistente devuelve vacio")
    void membresia_inexistente_devuelve_vacio() {
        when(membershipJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
