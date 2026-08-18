package com.vetsoftware.app.state.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCityChildrenQueryPort — municipios activos para el guardado de borrado")
class JpaCityChildrenQueryPortTest {

    @Mock
    private CityJpaRepository jpaRepository;
    @InjectMocks
    private JpaCityChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByState_Id")
    void delega_en_exists_by_state_id() {
        when(jpaRepository.existsByState_Id(7L)).thenReturn(true);

        assertThat(port.existsActiveByStateId(7L)).isTrue();
    }

    @Test
    @DisplayName("devuelve false cuando no hay municipios bajo el departamento")
    void false_sin_municipios() {
        when(jpaRepository.existsByState_Id(7L)).thenReturn(false);

        assertThat(port.existsActiveByStateId(7L)).isFalse();
    }
}
