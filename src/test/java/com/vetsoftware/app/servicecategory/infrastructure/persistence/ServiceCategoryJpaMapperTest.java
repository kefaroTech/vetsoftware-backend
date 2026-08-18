package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
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
 * {@code CompanyJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es portador de datos, y mockearla no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCategoryJpaMapper")
class ServiceCategoryJpaMapperTest {

    private final ServiceCategoryJpaMapper mapper = new ServiceCategoryJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private ServiceCategoryJpaEntity entidadCompleta() {
        ServiceCategoryJpaEntity entity = new ServiceCategoryJpaEntity();
        entity.setId(ServiceCategoryMother.CATEGORY_ID);
        entity.setName("Consultas");
        entity.setDescription("Categoria de consultas");
        entity.setCreatedDate(ServiceCategoryMother.CREADO);
        entity.setUpdatedDate(null);
        entity.setUpdatedBy(null);
        entity.setVersion(0L);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            ServiceCategory categoria = ServiceCategoryMother.activa();

            ServiceCategoryJpaEntity entity = mapper.toJpa(categoria, companyEntity);

            assertThat(entity.getId()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(entity.getName()).isEqualTo("Consultas");
            assertThat(entity.getDescription()).isEqualTo("Categoria de consultas");
            assertThat(entity.getCreatedDate()).isEqualTo(ServiceCategoryMother.CREADO);
            assertThat(entity.getUpdatedDate()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
            assertThat(entity.getVersion()).isZero();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha la compania en su slot")
        void engancha_la_compania_en_su_slot() {
            ServiceCategoryJpaEntity entity = mapper.toJpa(ServiceCategoryMother.activa(),
                    companyEntity);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con ref precargado — camino de escritura")
    class ToDomainConRef {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getCompany(), Hibernate lanzaria un SELECT extra por save.
            ServiceCategory categoria = mapper.toDomain(entidadCompleta(),
                    ServiceCategoryMother.CLINICA);

            assertThat(categoria.getId()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(categoria.getName()).isEqualTo("Consultas");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de consultas");
            assertThat(categoria.getCompany()).isEqualTo(ServiceCategoryMother.CLINICA);
            assertThat(categoria.getCreatedDate()).isEqualTo(ServiceCategoryMother.CREADO);
            assertThat(categoria.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            ServiceCategory original = ServiceCategoryMother.activa();

            ServiceCategoryJpaEntity entity = mapper.toJpa(original, companyEntity);
            ServiceCategory vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el companion VO desde la asociacion hidratada")
        void construye_el_companion_vo_desde_la_asociacion() {
            when(companyEntity.getId()).thenReturn(ServiceCategoryMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(ServiceCategoryMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(ServiceCategoryMother.CLINICA.identifier());
            ServiceCategoryJpaEntity entity = entidadCompleta();
            entity.setCompany(companyEntity);

            ServiceCategory categoria = mapper.toDomain(entity);

            assertThat(categoria.getCompany()).isEqualTo(ServiceCategoryMother.CLINICA);
        }

        // No hay caso "sin compania hidratada": tanto la columna JPA (nullable =
        // false) como el constructor de ServiceCategory (company is required)
        // prohiben una categoria sin empresa.
    }
}
