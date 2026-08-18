package com.vetsoftware.app.country.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.infrastructure.persistence.StateJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaStateChildrenQueryPort")
class JpaStateChildrenQueryPortTest {

    private static final Long COUNTRY_ID = 700L;

    @Mock
    private StateJpaRepository jpaRepository;

    @InjectMocks
    private JpaStateChildrenQueryPort port;

    @Nested
    @DisplayName("existsActiveByCountryId")
    class ExistsActiveByCountryId {

        @Test
        @DisplayName("hay hijos activos si algun departamento activo referencia el pais")
        void hay_hijos_activos() {
            when(jpaRepository.existsByCountry_Id(COUNTRY_ID)).thenReturn(true);

            assertThat(port.existsActiveByCountryId(COUNTRY_ID)).isTrue();
        }

        @Test
        @DisplayName("no hay hijos activos si ningun departamento referencia el pais")
        void no_hay_hijos_activos() {
            when(jpaRepository.existsByCountry_Id(COUNTRY_ID)).thenReturn(false);

            assertThat(port.existsActiveByCountryId(COUNTRY_ID)).isFalse();
        }
    }
}
