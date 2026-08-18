package com.vetsoftware.app.city.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.domain.StateRef;
import com.vetsoftware.app.city.testsupport.CityMother;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaEntity;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaStateQueryPort — adaptador sobre StateJpaRepository")
class JpaStateQueryPortTest {

    @Mock
    private StateJpaRepository stateJpaRepository;
    @Mock
    private StateJpaEntity stateEntity;

    private JpaStateQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaStateQueryPort(stateJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a su companion VO")
        void mapea_la_entidad_encontrada_a_su_companion_vo() {
            when(stateEntity.getId()).thenReturn(CityMother.ANTIOQUIA.id());
            when(stateEntity.getName()).thenReturn(CityMother.ANTIOQUIA.name());
            when(stateJpaRepository.findById(CityMother.STATE_ID))
                    .thenReturn(Optional.of(stateEntity));

            Optional<StateRef> resultado = port.findById(CityMother.STATE_ID);

            assertThat(resultado).contains(CityMother.ANTIOQUIA);
        }

        @Test
        @DisplayName("un departamento inexistente devuelve vacio")
        void un_departamento_inexistente_devuelve_vacio() {
            when(stateJpaRepository.findById(CityMother.STATE_ID)).thenReturn(Optional.empty());

            Optional<StateRef> resultado = port.findById(CityMother.STATE_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
