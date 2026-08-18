package com.vetsoftware.app.productcategory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
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
@DisplayName("ProductCategoryJpaMapper")
class ProductCategoryJpaMapperTest {

    private final ProductCategoryJpaMapper mapper = new ProductCategoryJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private ProductCategoryJpaEntity entidadCompleta() {
        ProductCategoryJpaEntity entity = new ProductCategoryJpaEntity();
        entity.setId(ProductCategoryMother.CATEGORY_ID);
        entity.setName("Medicamentos");
        entity.setDescription("Categoria de medicamentos");
        entity.setCreatedDate(ProductCategoryMother.CREADO);
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
            ProductCategory categoria = ProductCategoryMother.activa();

            ProductCategoryJpaEntity entity = mapper.toJpa(categoria, companyEntity);

            assertThat(entity.getId()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(entity.getName()).isEqualTo("Medicamentos");
            assertThat(entity.getDescription()).isEqualTo("Categoria de medicamentos");
            assertThat(entity.getCreatedDate()).isEqualTo(ProductCategoryMother.CREADO);
            assertThat(entity.getUpdatedDate()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
            assertThat(entity.getVersion()).isZero();
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha la compania en su slot")
        void engancha_la_compania_en_su_slot() {
            ProductCategoryJpaEntity entity = mapper.toJpa(ProductCategoryMother.activa(),
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
            ProductCategory categoria = mapper.toDomain(entidadCompleta(),
                    ProductCategoryMother.CLINICA);

            assertThat(categoria.getId()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(categoria.getName()).isEqualTo("Medicamentos");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de medicamentos");
            assertThat(categoria.getCompany()).isEqualTo(ProductCategoryMother.CLINICA);
            assertThat(categoria.getCreatedDate()).isEqualTo(ProductCategoryMother.CREADO);
            assertThat(categoria.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            ProductCategory original = ProductCategoryMother.activa();

            ProductCategoryJpaEntity entity = mapper.toJpa(original, companyEntity);
            ProductCategory vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el companion VO desde la asociacion hidratada")
        void construye_el_companion_vo_desde_la_asociacion() {
            when(companyEntity.getId()).thenReturn(ProductCategoryMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(ProductCategoryMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(ProductCategoryMother.CLINICA.identifier());
            ProductCategoryJpaEntity entity = entidadCompleta();
            entity.setCompany(companyEntity);

            ProductCategory categoria = mapper.toDomain(entity);

            assertThat(categoria.getCompany()).isEqualTo(ProductCategoryMother.CLINICA);
        }

        // No hay caso "sin compania hidratada": tanto la columna JPA (nullable =
        // false) como el constructor de ProductCategory (company is required)
        // prohiben una categoria sin empresa. La rama `c == null` del ternario en
        // toDomain(entity) es defensiva y hoy inalcanzable — ver HUECOS del informe.
    }
}
