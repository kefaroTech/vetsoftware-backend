package com.vetsoftware.app.state.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaEntity;
import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCountryQueryPort — resolucion del companion VO de pais")
class JpaCountryQueryPortTest {

    @Mock
    private CountryJpaRepository countryJpaRepository;
    @InjectMocks
    private JpaCountryQueryPort port;

    @Test
    @DisplayName("mapea la entidad encontrada a su companion VO")
    void mapea_la_entidad_encontrada() {
        // Constructor protegido: se dobla en vez de construirla directamente.
        CountryJpaEntity entity = mock(CountryJpaEntity.class);
        when(entity.getId()).thenReturn(1L);
        when(entity.getName()).thenReturn("Colombia");
        when(countryJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(port.findById(1L)).contains(new CountryRef(1L, "Colombia"));
    }

    @Test
    @DisplayName("devuelve vacio si el pais no existe")
    void vacio_si_no_existe() {
        when(countryJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
