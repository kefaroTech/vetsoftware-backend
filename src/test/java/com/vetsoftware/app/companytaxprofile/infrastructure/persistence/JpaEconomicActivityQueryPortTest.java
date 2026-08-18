package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.testsupport.ReflectionEntities;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEconomicActivityQueryPort — arma el EconomicActivityRef desde EconomicActivityJpaEntity")
class JpaEconomicActivityQueryPortTest {

    @Mock
    private EconomicActivityJpaRepository economicActivityJpaRepository;

    private JpaEconomicActivityQueryPort port;

    @Test
    @DisplayName("actividad existente: mapea id, codigo y nombre")
    void actividad_existente_mapea_id_codigo_y_nombre() throws Exception {
        port = new JpaEconomicActivityQueryPort(economicActivityJpaRepository);
        EconomicActivityJpaEntity entity = ReflectionEntities
                .newInstance(EconomicActivityJpaEntity.class);
        entity.setId(5L);
        entity.setCode("7500");
        entity.setName("Actividades veterinarias");
        when(economicActivityJpaRepository.findById(5L)).thenReturn(Optional.of(entity));

        Optional<EconomicActivityRef> result = port.findById(5L);

        assertThat(result)
                .contains(new EconomicActivityRef(5L, "7500", "Actividades veterinarias"));
    }

    @Test
    @DisplayName("actividad inexistente devuelve vacio")
    void actividad_inexistente_devuelve_vacio() {
        port = new JpaEconomicActivityQueryPort(economicActivityJpaRepository);
        when(economicActivityJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
