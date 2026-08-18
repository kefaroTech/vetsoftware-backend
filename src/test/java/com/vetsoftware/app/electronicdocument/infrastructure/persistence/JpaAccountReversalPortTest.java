package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAccountReversalPort — marca la cuenta reversada al validar la nota credito")
class JpaAccountReversalPortTest {

    @Mock
    private OpenAccountJpaRepository openAccountJpaRepository;

    private JpaAccountReversalPort port;

    @BeforeEach
    void montar() {
        port = new JpaAccountReversalPort(openAccountJpaRepository);
    }

    @Test
    @DisplayName("una cuenta no reversada se marca reversada y se persiste")
    void cuenta_no_reversada_se_marca_y_persiste() throws Exception {
        OpenAccountJpaEntity account = ReflectionEntities.newInstance(OpenAccountJpaEntity.class);
        account.setReversed(false);
        when(openAccountJpaRepository.findById(100L)).thenReturn(Optional.of(account));

        port.markReversed(100L);

        ArgumentCaptor<OpenAccountJpaEntity> captor = ArgumentCaptor
                .forClass(OpenAccountJpaEntity.class);
        verify(openAccountJpaRepository).save(captor.capture());
        assertThat(captor.getValue().isReversed()).isTrue();
    }

    @Nested
    @DisplayName("idempotencia y ausencias — no hace nada")
    class NoHaceNada {

        @Test
        @DisplayName("una cuenta ya reversada no se vuelve a guardar")
        void cuenta_ya_reversada_no_se_guarda_otra_vez() throws Exception {
            OpenAccountJpaEntity account = ReflectionEntities
                    .newInstance(OpenAccountJpaEntity.class);
            account.setReversed(true);
            when(openAccountJpaRepository.findById(101L)).thenReturn(Optional.of(account));

            port.markReversed(101L);

            verify(openAccountJpaRepository, never()).save(any());
        }

        @Test
        @DisplayName("una cuenta inexistente no falla y no escribe nada")
        void cuenta_inexistente_no_falla() {
            when(openAccountJpaRepository.findById(102L)).thenReturn(Optional.empty());

            port.markReversed(102L);

            verify(openAccountJpaRepository, never()).save(any());
        }
    }
}
