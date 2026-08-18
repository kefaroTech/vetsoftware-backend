package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.NewRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.StoredRefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Envuelve {@code RefreshTokenJpaRepository}, el Spring Data del propio
 * paquete: la entidad se construye real, sin necesidad de dobles.
 */
@ExtendWith(MockitoExtension.class)
class JpaRefreshTokenRepositoryTest {

    @Mock
    private RefreshTokenJpaRepository jpaRepository;
    @InjectMocks
    private JpaRefreshTokenRepository repository;

    @Nested
    @DisplayName("save")
    class Guardar {

        @Test
        @DisplayName("guarda una fila nueva, no revocada, con los datos del token")
        void guarda_una_fila_nueva_no_revocada() {
            LocalDateTime expira = LocalDateTime.now().plusDays(30);

            repository.save(new NewRefreshToken("hash", 7L, "EMPLOYEE", 5L, expira));

            ArgumentCaptor<RefreshTokenJpaEntity> captor = ArgumentCaptor
                    .forClass(RefreshTokenJpaEntity.class);
            verify(jpaRepository).save(captor.capture());
            RefreshTokenJpaEntity saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo("hash");
            assertThat(saved.getSubjectId()).isEqualTo(7L);
            assertThat(saved.getSubjectType()).isEqualTo("EMPLOYEE");
            assertThat(saved.getAuthVersion()).isEqualTo(5L);
            assertThat(saved.getExpiresAt()).isEqualTo(expira);
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findByHash")
    class BuscarPorHash {

        @Test
        @DisplayName("mapea una fila almacenada, revocada o no")
        void mapea_una_fila_almacenada() {
            RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
            entity.setId(11L);
            entity.setTokenHash("hash");
            entity.setSubjectId(7L);
            entity.setSubjectType("EMPLOYEE");
            entity.setAuthVersion(5L);
            LocalDateTime expira = LocalDateTime.now().plusHours(1);
            entity.setExpiresAt(expira);
            entity.setRevoked(true);
            LocalDateTime revocado = LocalDateTime.now().minusMinutes(5);
            entity.setRevokedAt(revocado);
            when(jpaRepository.findByTokenHash("hash")).thenReturn(Optional.of(entity));

            Optional<StoredRefreshToken> result = repository.findByHash("hash");

            assertThat(result).contains(
                    new StoredRefreshToken(11L, 7L, "EMPLOYEE", 5L, expira, true, revocado));
        }

        @Test
        @DisplayName("un hash desconocido no devuelve nada")
        void hash_desconocido_no_devuelve_nada() {
            when(jpaRepository.findByTokenHash("otro")).thenReturn(Optional.empty());

            assertThat(repository.findByHash("otro")).isEmpty();
        }
    }

    @Test
    @DisplayName("revokeById delega en el UPDATE del repositorio")
    void revoke_by_id_delega_en_el_update() {
        repository.revokeById(11L);

        verify(jpaRepository).revokeById(11L);
    }

    @Test
    @DisplayName("revokeAllForSubject delega en el UPDATE masivo del repositorio")
    void revoke_all_for_subject_delega_en_el_update() {
        repository.revokeAllForSubject(7L, "EMPLOYEE");

        verify(jpaRepository).revokeAllForSubject(7L, "EMPLOYEE");
    }
}
