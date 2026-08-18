package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("JpaLaboratoryTestTypeQueryPort — adaptador sobre LaboratoryTestTypeJpaRepository")
class JpaLaboratoryTestTypeQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

    @Mock
    private LaboratoryTestTypeJpaRepository testTypeJpaRepository;
    @Mock
    private LaboratoryTestTypeJpaEntity testTypeEntity;
    @InjectMocks
    private JpaLaboratoryTestTypeQueryPort port;

    @Nested
    @DisplayName("disponibilidad por empresa")
    class Disponibilidad {

        @Test
        @DisplayName("mapea el tipo de examen disponible para mi empresa a su companion VO")
        void mapea_el_tipo_de_examen_encontrado_a_su_companion_vo() {
            LaboratoryTestTypeRef hemograma = LaboratoryTestMother.HEMOGRAMA;
            when(testTypeEntity.getId()).thenReturn(hemograma.id());
            when(testTypeEntity.getName()).thenReturn(hemograma.name());
            when(testTypeJpaRepository.findAvailableById(hemograma.id(), EMPRESA))
                    .thenReturn(Optional.of(testTypeEntity));

            Optional<LaboratoryTestTypeRef> resultado = port
                    .findAvailableByIdAndCompanyId(hemograma.id(), EMPRESA);

            assertThat(resultado).contains(hemograma);
        }

        @Test
        @DisplayName("el tipo de examen privado de otra empresa no esta disponible")
        void el_tipo_privado_de_otra_empresa_no_esta_disponible() {
            LaboratoryTestTypeRef hemograma = LaboratoryTestMother.HEMOGRAMA;
            when(testTypeJpaRepository.findAvailableById(hemograma.id(), OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThat(port.findAvailableByIdAndCompanyId(hemograma.id(), OTRA_EMPRESA)).isEmpty();
        }
    }
}
