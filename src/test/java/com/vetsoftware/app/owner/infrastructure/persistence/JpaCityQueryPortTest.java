package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.owner.domain.CityRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCityQueryPort — resolucion del companion VO de city")
class JpaCityQueryPortTest {

    @Mock
    private CityJpaRepository cityJpaRepository;
    @Mock
    private CityJpaEntity cityEntity;

    private JpaCityQueryPort port;

    @Test
    @DisplayName("ciudad existente se resuelve en un CityRef con id y nombre")
    void ciudad_existente_se_resuelve_en_un_city_ref() {
        port = new JpaCityQueryPort(cityJpaRepository);
        when(cityJpaRepository.findById(5L)).thenReturn(Optional.of(cityEntity));
        when(cityEntity.getId()).thenReturn(5L);
        when(cityEntity.getName()).thenReturn("Bogota");

        Optional<CityRef> ref = port.findById(5L);

        assertThat(ref).contains(new CityRef(5L, "Bogota"));
    }

    @Test
    @DisplayName("ciudad inexistente devuelve Optional vacio")
    void ciudad_inexistente_devuelve_optional_vacio() {
        port = new JpaCityQueryPort(cityJpaRepository);
        when(cityJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
