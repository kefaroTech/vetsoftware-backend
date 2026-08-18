package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaEntity;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTechnicalKeyQueryPort — clave tecnica de la resolucion activa")
class JpaTechnicalKeyQueryPortTest {

    @Mock
    private NumberingResolutionJpaRepository repository;

    private JpaTechnicalKeyQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaTechnicalKeyQueryPort(repository);
    }

    private static NumberingResolutionJpaEntity conTechnicalKey(String key) throws Exception {
        NumberingResolutionJpaEntity entity = ReflectionEntities
                .newInstance(NumberingResolutionJpaEntity.class);
        entity.setTechnicalKey(key);
        return entity;
    }

    @Test
    @DisplayName("una resolucion activa con clave tecnica la devuelve")
    void resolucion_activa_con_clave_tecnica() throws Exception {
        when(repository.findActive(9L, 7L, "FE_VENTA"))
                .thenReturn(Optional.of(conTechnicalKey("abc123")));

        assertThat(port.findActiveTechnicalKey(9L, 7L, ElectronicDocumentType.FE_VENTA))
                .contains("abc123");
    }

    @Nested
    @DisplayName("ausencias — no hay clave que devolver")
    class Ausencias {

        @Test
        @DisplayName("sin resolucion activa devuelve vacio")
        void sin_resolucion_activa() throws Exception {
            when(repository.findActive(9L, 7L, "NOTA_CREDITO")).thenReturn(Optional.empty());

            assertThat(port.findActiveTechnicalKey(9L, 7L, ElectronicDocumentType.NOTA_CREDITO))
                    .isEmpty();
        }

        @Test
        @DisplayName("una resolucion sin clave tecnica (en blanco) devuelve vacio, como una nota")
        void resolucion_sin_clave_tecnica_devuelve_vacio() throws Exception {
            when(repository.findActive(9L, 7L, "FE_VENTA"))
                    .thenReturn(Optional.of(conTechnicalKey("   ")));

            assertThat(port.findActiveTechnicalKey(9L, 7L, ElectronicDocumentType.FE_VENTA))
                    .isEmpty();
        }
    }
}
