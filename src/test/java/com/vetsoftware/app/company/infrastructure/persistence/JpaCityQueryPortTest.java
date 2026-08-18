package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.testsupport.ReflectionEntities;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCityQueryPort (company) — arma el CityRef desde CityJpaEntity")
class JpaCityQueryPortTest {

    @Mock
    private CityJpaRepository cityJpaRepository;

    private JpaCityQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaCityQueryPort(cityJpaRepository);
    }

    @Test
    @DisplayName("ciudad existente: mapea id y nombre")
    void ciudad_existente_mapea_id_y_nombre() throws ReflectiveOperationException {
        CityJpaEntity entity = ReflectionEntities.newInstance(CityJpaEntity.class);
        entity.setId(11L);
        entity.setName("Bogota");
        when(cityJpaRepository.findById(11L)).thenReturn(Optional.of(entity));

        Optional<CityRef> result = port.findById(11L);

        assertThat(result).contains(new CityRef(11L, "Bogota"));
    }

    @Test
    @DisplayName("ciudad inexistente devuelve vacio")
    void ciudad_inexistente_devuelve_vacio() {
        when(cityJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
