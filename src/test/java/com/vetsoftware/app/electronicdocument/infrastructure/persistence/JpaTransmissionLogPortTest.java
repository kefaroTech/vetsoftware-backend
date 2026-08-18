package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
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
@DisplayName("JpaTransmissionLogPort — bitacora de intentos de transmision")
class JpaTransmissionLogPortTest {

    @Mock
    private ElectronicDocumentTransmissionJpaRepository jpaRepository;

    private JpaTransmissionLogPort port;

    @BeforeEach
    void montar() {
        port = new JpaTransmissionLogPort(jpaRepository);
    }

    private static ElectronicDocumentTransmissionJpaEntity conProviderKey(Long documentId,
            String key) throws Exception {
        ElectronicDocumentTransmissionJpaEntity entity = new ElectronicDocumentTransmissionJpaEntity();
        entity.setElectronicDocumentId(documentId);
        entity.setProviderDocumentKey(key);
        return entity;
    }

    @Nested
    @DisplayName("record — calcula el numero de intento y trunca el mensaje de error")
    class Registro {

        @Test
        @DisplayName("el primer intento se numera 1")
        void primer_intento_se_numera_uno() {
            when(jpaRepository.countByElectronicDocumentId(55L)).thenReturn(0);

            port.record(55L, "MATIAS", 200, "KEY-1", TransmissionResult.ACCEPTED, null);

            ArgumentCaptor<ElectronicDocumentTransmissionJpaEntity> captor = ArgumentCaptor
                    .forClass(ElectronicDocumentTransmissionJpaEntity.class);
            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getAttempt()).isEqualTo(1);
            assertThat(captor.getValue().getProvider()).isEqualTo("MATIAS");
        }

        @Test
        @DisplayName("un reintento continua la numeracion")
        void reintento_continua_la_numeracion() {
            when(jpaRepository.countByElectronicDocumentId(55L)).thenReturn(2);

            port.record(55L, "MATIAS", 502, null, TransmissionResult.ERROR, "timeout");

            ArgumentCaptor<ElectronicDocumentTransmissionJpaEntity> captor = ArgumentCaptor
                    .forClass(ElectronicDocumentTransmissionJpaEntity.class);
            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getAttempt()).isEqualTo(3);
        }

        @Test
        @DisplayName("un mensaje de error mayor a 2000 caracteres se trunca")
        void mensaje_de_error_largo_se_trunca() {
            when(jpaRepository.countByElectronicDocumentId(55L)).thenReturn(0);
            String mensajeLargo = "x".repeat(2500);

            port.record(55L, "MATIAS", 500, null, TransmissionResult.ERROR, mensajeLargo);

            ArgumentCaptor<ElectronicDocumentTransmissionJpaEntity> captor = ArgumentCaptor
                    .forClass(ElectronicDocumentTransmissionJpaEntity.class);
            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getErrorMessage()).hasSize(2000);
        }

        @Test
        @DisplayName("sin mensaje de error, se guarda null")
        void sin_mensaje_de_error_se_guarda_null() {
            when(jpaRepository.countByElectronicDocumentId(55L)).thenReturn(0);

            port.record(55L, "MATIAS", 200, "KEY-1", TransmissionResult.ACCEPTED, null);

            ArgumentCaptor<ElectronicDocumentTransmissionJpaEntity> captor = ArgumentCaptor
                    .forClass(ElectronicDocumentTransmissionJpaEntity.class);
            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getErrorMessage()).isNull();
        }
    }

    @Test
    @DisplayName("findDocumentIdByProviderKey resuelve el documento por la clave del proveedor")
    void find_document_id_by_provider_key() throws Exception {
        when(jpaRepository.findFirstByProviderDocumentKeyOrderByIdDesc("KEY-9"))
                .thenReturn(Optional.of(conProviderKey(77L, "KEY-9")));

        assertThat(port.findDocumentIdByProviderKey("KEY-9")).contains(77L);
    }

    @Test
    @DisplayName("countAttempts delega directamente en el repositorio")
    void count_attempts_delega_en_el_repositorio() {
        when(jpaRepository.countByElectronicDocumentId(55L)).thenReturn(3);

        assertThat(port.countAttempts(55L)).isEqualTo(3);
    }

    @Test
    @DisplayName("findLatestProviderKey devuelve la ultima clave de proveedor registrada")
    void find_latest_provider_key() throws Exception {
        when(jpaRepository
                .findFirstByElectronicDocumentIdAndProviderDocumentKeyNotNullOrderByIdDesc(55L))
                .thenReturn(Optional.of(conProviderKey(55L, "KEY-LATEST")));

        assertThat(port.findLatestProviderKey(55L)).contains("KEY-LATEST");
    }
}
