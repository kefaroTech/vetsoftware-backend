package com.vetsoftware.app.city.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.testsupport.CityMother;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * {@code StateJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es portador de datos, y mockearla no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CityJpaMapper")
class CityJpaMapperTest {

    private final CityJpaMapper mapper = new CityJpaMapper();

    @Mock
    private StateJpaEntity stateEntity;

    private CityJpaEntity entidadCompleta() {
        CityJpaEntity entity = new CityJpaEntity();
        entity.setId(CityMother.CITY_ID);
        entity.setName("Medellin");
        entity.setDaneCode("05001");
        entity.setCreatedDate(CityMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            City ciudad = CityMother.activa();

            CityJpaEntity entity = mapper.toJpa(ciudad, stateEntity);

            assertThat(entity.getId()).isEqualTo(CityMother.CITY_ID);
            assertThat(entity.getName()).isEqualTo("Medellin");
            assertThat(entity.getDaneCode()).isEqualTo("05001");
            assertThat(entity.getCreatedDate()).isEqualTo(CityMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha el departamento en su slot")
        void engancha_el_departamento_en_su_slot() {
            CityJpaEntity entity = mapper.toJpa(CityMother.activa(), stateEntity);

            assertThat(entity.getState()).isSameAs(stateEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con ref precargado — camino de escritura")
    class ToDomainConRef {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getState(), Hibernate lanzaria un SELECT extra por save.
            City ciudad = mapper.toDomain(entidadCompleta(), CityMother.ANTIOQUIA);

            assertThat(ciudad.getId()).isEqualTo(CityMother.CITY_ID);
            assertThat(ciudad.getName()).isEqualTo("Medellin");
            assertThat(ciudad.getState()).isEqualTo(CityMother.ANTIOQUIA);
            assertThat(ciudad.getDaneCode()).isEqualTo("05001");
            assertThat(ciudad.getCreatedDate()).isEqualTo(CityMother.CREADO);
            assertThat(ciudad.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            City original = CityMother.activa();

            CityJpaEntity entity = mapper.toJpa(original, stateEntity);
            City vuelta = mapper.toDomain(entity, original.getState());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el companion VO desde la asociacion hidratada")
        void construye_el_companion_vo_desde_la_asociacion() {
            when(stateEntity.getId()).thenReturn(CityMother.ANTIOQUIA.id());
            when(stateEntity.getName()).thenReturn(CityMother.ANTIOQUIA.name());
            CityJpaEntity entity = entidadCompleta();
            entity.setState(stateEntity);

            City ciudad = mapper.toDomain(entity);

            assertThat(ciudad.getState()).isEqualTo(CityMother.ANTIOQUIA);
        }
    }
}
