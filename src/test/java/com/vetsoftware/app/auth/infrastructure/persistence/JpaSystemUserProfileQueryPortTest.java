package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort.SystemUserProfile;
import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaSystemUserProfileQueryPortTest {

    @Mock
    private SystemUserJpaRepository systemUserJpaRepository;
    @InjectMocks
    private JpaSystemUserProfileQueryPort port;

    @Test
    @DisplayName("mapea el código del usuario de sistema visible en /auth/me")
    void mapea_el_codigo_visible_en_me() throws Exception {
        SystemUserJpaEntity entity = ReflectionEntities.newInstance(SystemUserJpaEntity.class);
        entity.setId(2L);
        entity.setCode("ADMIN");
        when(systemUserJpaRepository.findById(2L)).thenReturn(Optional.of(entity));

        assertThat(port.findById(2L)).contains(new SystemUserProfile(2L, "ADMIN"));
    }

    @Test
    @DisplayName("un usuario de sistema inexistente no aparece")
    void usuario_inexistente_no_aparece() {
        when(systemUserJpaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(port.findById(2L)).isEmpty();
    }
}
