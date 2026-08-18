package com.vetsoftware.app.registration.infrastructure.persistence;

import static com.vetsoftware.app.registration.testsupport.RegistrationMother.TOKEN_HASH;
import static com.vetsoftware.app.registration.testsupport.RegistrationMother.tokenVigente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmailVerificationTokenRepository")
class JpaEmailVerificationTokenRepositoryTest {

    @Mock
    private EmailVerificationTokenJpaRepository jpaRepository;
    @Mock
    private EmailVerificationTokenJpaMapper mapper;
    @InjectMocks
    private JpaEmailVerificationTokenRepository repository;

    @Test
    @DisplayName("save mapea a entidad, persiste y devuelve el dominio reconstruido")
    void save_mapea_persiste_y_reconstruye() {
        EmailVerificationToken dominio = tokenVigente();
        EmailVerificationTokenJpaEntity porGuardar = new EmailVerificationTokenJpaEntity();
        EmailVerificationTokenJpaEntity guardada = new EmailVerificationTokenJpaEntity();
        when(mapper.toJpa(dominio)).thenReturn(porGuardar);
        when(jpaRepository.save(porGuardar)).thenReturn(guardada);
        when(mapper.toDomain(guardada)).thenReturn(dominio);

        EmailVerificationToken resultado = repository.save(dominio);

        assertThat(resultado).isSameAs(dominio);
        verify(jpaRepository).save(porGuardar);
    }

    @Test
    @DisplayName("findByTokenHash devuelve el token cuando existe")
    void find_by_token_hash_existente() {
        EmailVerificationTokenJpaEntity entidad = new EmailVerificationTokenJpaEntity();
        EmailVerificationToken dominio = tokenVigente();
        when(jpaRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(entidad));
        when(mapper.toDomain(entidad)).thenReturn(dominio);

        Optional<EmailVerificationToken> resultado = repository.findByTokenHash(TOKEN_HASH);

        assertThat(resultado).contains(dominio);
    }

    @Test
    @DisplayName("findByTokenHash devuelve vacío cuando no existe")
    void find_by_token_hash_inexistente() {
        when(jpaRepository.findByTokenHash("hash-inexistente")).thenReturn(Optional.empty());

        Optional<EmailVerificationToken> resultado = repository.findByTokenHash("hash-inexistente");

        assertThat(resultado).isEmpty();
    }
}
