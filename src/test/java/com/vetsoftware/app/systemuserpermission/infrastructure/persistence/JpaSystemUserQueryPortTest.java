package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSystemUserQueryPort")
class JpaSystemUserQueryPortTest {

    @Mock
    private SystemUserJpaRepository systemUserJpaRepository;
    @Mock
    private SystemUserJpaEntity entity;

    private JpaSystemUserQueryPort port;

    @BeforeEach
    void setUp() {
        port = new JpaSystemUserQueryPort(systemUserJpaRepository);
    }

    @Test
    @DisplayName("mapea la entidad encontrada a su ref")
    void mapea_la_entidad_encontrada_a_su_ref() {
        when(entity.getId()).thenReturn(5L);
        when(entity.getCode()).thenReturn("admin-api");
        when(systemUserJpaRepository.findById(5L)).thenReturn(Optional.of(entity));

        Optional<SystemUserRef> ref = port.findById(5L);

        assertThat(ref).contains(new SystemUserRef(5L, "admin-api"));
    }

    @Test
    @DisplayName("id inexistente devuelve vacio")
    void id_inexistente_devuelve_vacio() {
        when(systemUserJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(port.findById(999L)).isEmpty();
    }
}
