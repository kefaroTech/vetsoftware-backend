package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository.AuthSystemUser;
import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaAuthSystemUserRepositoryTest {

    @Mock
    private SystemUserJpaRepository systemUserJpaRepository;
    @InjectMocks
    private JpaAuthSystemUserRepository repository;

    private static SystemUserJpaEntity entidad(Long id, long authVersion)
            throws ReflectiveOperationException {
        SystemUserJpaEntity entity = ReflectionEntities.newInstance(SystemUserJpaEntity.class);
        entity.setId(id);
        entity.setAuthVersion(authVersion);
        return entity;
    }

    @Nested
    @DisplayName("findActiveById")
    class BuscarActivo {

        @Test
        @DisplayName("mapea la fila con su versión de autenticación")
        void mapea_la_fila() throws Exception {
            when(systemUserJpaRepository.findById(2L)).thenReturn(Optional.of(entidad(2L, 9L)));

            assertThat(repository.findActiveById(2L)).contains(new AuthSystemUser(2L, 9L));
        }

        @Test
        @DisplayName("un usuario inexistente o desactivado no aparece")
        void usuario_inexistente_no_aparece() {
            when(systemUserJpaRepository.findById(2L)).thenReturn(Optional.empty());

            assertThat(repository.findActiveById(2L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("rotateAuthVersion")
    class RotarVersion {

        @Test
        @DisplayName("sube la versión en uno, la persiste y devuelve la ya rotada")
        void sube_la_version_y_la_persiste() throws Exception {
            SystemUserJpaEntity entity = entidad(2L, 8L);
            when(systemUserJpaRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(entity));

            var result = repository.rotateAuthVersion(2L);

            assertThat(result).contains(new AuthSystemUser(2L, 9L));
            assertThat(entity.getAuthVersion()).isEqualTo(9L);
            verify(systemUserJpaRepository).saveAndFlush(entity);
        }

        @Test
        @DisplayName("sin fila bajo lock, no rota ni persiste nada")
        void sin_fila_no_rota_nada() {
            when(systemUserJpaRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

            assertThat(repository.rotateAuthVersion(2L)).isEmpty();
            verify(systemUserJpaRepository, never())
                    .saveAndFlush(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    @DisplayName("bumpAuthVersion delega en el UPDATE nativo del repositorio")
    void bump_auth_version_delega_en_el_update_nativo() {
        repository.bumpAuthVersion(2L);

        verify(systemUserJpaRepository).bumpAuthVersion(2L);
    }
}
