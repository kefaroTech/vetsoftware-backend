package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El catalogo mezcla filas generales con las privadas de cada empresa, asi que
 * el adaptador consulta {@code findAvailableById} —general O mia— y no la
 * lectura estricta: acotar a secas dejaria de poder asignar un tipo general,
 * que es el caso normal. Lo que si queda fuera es el tipo privado de otro
 * tenant.
 */
@ExtendWith(MockitoExtension.class)
class JpaDiagnosticImagingTypeQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

    @Mock
    private DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository;
    @InjectMocks
    private JpaDiagnosticImagingTypeQueryPort port;

    private static DiagnosticImagingTypeJpaEntity tipoEncontrado(long id, String name) {
        DiagnosticImagingTypeJpaEntity entity = mock(DiagnosticImagingTypeJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("mapea el tipo de estudio disponible para mi empresa a su companion VO")
    void mapea_el_tipo_encontrado_a_su_companion_vo() {
        DiagnosticImagingTypeJpaEntity tipo = tipoEncontrado(5L, "Radiografia");
        when(diagnosticImagingTypeJpaRepository.findAvailableById(5L, EMPRESA))
                .thenReturn(Optional.of(tipo));

        Optional<DiagnosticImagingTypeRef> ref = port.findAvailableByIdAndCompanyId(5L, EMPRESA);

        assertThat(ref).contains(new DiagnosticImagingTypeRef(5L, "Radiografia"));
    }

    @Test
    @DisplayName("el tipo de estudio privado de otra empresa no esta disponible")
    void el_tipo_privado_de_otra_empresa_no_esta_disponible() {
        when(diagnosticImagingTypeJpaRepository.findAvailableById(5L, OTRA_EMPRESA))
                .thenReturn(Optional.empty());

        assertThat(port.findAvailableByIdAndCompanyId(5L, OTRA_EMPRESA)).isEmpty();
    }
}
