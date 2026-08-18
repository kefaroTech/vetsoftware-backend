package com.vetsoftware.app.vaccination.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaEntity;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaRepository;
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
class JpaVaccinationTypeQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

    @Mock
    private VaccinationTypeJpaRepository vaccinationTypeJpaRepository;
    @InjectMocks
    private JpaVaccinationTypeQueryPort port;

    private static VaccinationTypeJpaEntity tipoEncontrado(long id, String name) {
        VaccinationTypeJpaEntity entity = mock(VaccinationTypeJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("mapea el tipo de vacuna disponible para mi empresa a su companion VO")
    void mapea_el_tipo_encontrado_a_su_companion_vo() {
        VaccinationTypeJpaEntity tipo = tipoEncontrado(1L, "Rabia");
        when(vaccinationTypeJpaRepository.findAvailableById(1L, EMPRESA))
                .thenReturn(Optional.of(tipo));

        Optional<VaccinationTypeRef> ref = port.findAvailableByIdAndCompanyId(1L, EMPRESA);

        assertThat(ref).contains(new VaccinationTypeRef(1L, "Rabia"));
    }

    @Test
    @DisplayName("el tipo de vacuna privado de otra empresa no esta disponible")
    void el_tipo_privado_de_otra_empresa_no_esta_disponible() {
        when(vaccinationTypeJpaRepository.findAvailableById(1L, OTRA_EMPRESA))
                .thenReturn(Optional.empty());

        assertThat(port.findAvailableByIdAndCompanyId(1L, OTRA_EMPRESA)).isEmpty();
    }
}
