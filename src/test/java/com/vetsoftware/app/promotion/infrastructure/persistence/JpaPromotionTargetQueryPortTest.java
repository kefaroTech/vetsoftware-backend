package com.vetsoftware.app.promotion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaRepository;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPromotionTargetQueryPort — existencia del item objetivo de una promocion")
class JpaPromotionTargetQueryPortTest {

    private static final Long ITEM_ID = 3L;
    private static final Long COMPANY_ID = 5L;

    @Mock
    private ProductJpaRepository productJpaRepository;
    @Mock
    private ServiceJpaRepository serviceJpaRepository;
    @Mock
    private ProductCategoryJpaRepository productCategoryJpaRepository;
    @Mock
    private ServiceCategoryJpaRepository serviceCategoryJpaRepository;

    private JpaPromotionTargetQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaPromotionTargetQueryPort(productJpaRepository, serviceJpaRepository,
                productCategoryJpaRepository, serviceCategoryJpaRepository);
    }

    @Nested
    @DisplayName("guardas de entrada nula")
    class GuardasDeEntradaNula {

        @Test
        @DisplayName("type null es falso sin tocar ningun repositorio")
        void type_null_es_falso_sin_tocar_nada() {
            boolean resultado = port.exists(null, ITEM_ID, COMPANY_ID);

            assertThat(resultado).isFalse();
            verifyNoInteractions(productJpaRepository, serviceJpaRepository,
                    productCategoryJpaRepository, serviceCategoryJpaRepository);
        }

        @Test
        @DisplayName("itemId null es falso sin tocar ningun repositorio")
        void item_id_null_es_falso_sin_tocar_nada() {
            boolean resultado = port.exists(ApplicationType.PRODUCT, null, COMPANY_ID);

            assertThat(resultado).isFalse();
            verifyNoInteractions(productJpaRepository, serviceJpaRepository,
                    productCategoryJpaRepository, serviceCategoryJpaRepository);
        }

        @Test
        @DisplayName("companyId null es falso sin tocar ningun repositorio")
        void company_id_null_es_falso_sin_tocar_nada() {
            boolean resultado = port.exists(ApplicationType.PRODUCT, ITEM_ID, null);

            assertThat(resultado).isFalse();
            verifyNoInteractions(productJpaRepository, serviceJpaRepository,
                    productCategoryJpaRepository, serviceCategoryJpaRepository);
        }
    }

    @Nested
    @DisplayName("PRODUCT")
    class Product {

        @Test
        @DisplayName("delega en ProductJpaRepository y devuelve true si existe")
        void delega_y_devuelve_true() {
            when(productJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(true);

            assertThat(port.exists(ApplicationType.PRODUCT, ITEM_ID, COMPANY_ID)).isTrue();
            verifyNoInteractions(serviceJpaRepository, productCategoryJpaRepository,
                    serviceCategoryJpaRepository);
        }

        @Test
        @DisplayName("delega en ProductJpaRepository y devuelve false si no existe")
        void delega_y_devuelve_false() {
            when(productJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(false);

            assertThat(port.exists(ApplicationType.PRODUCT, ITEM_ID, COMPANY_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("SERVICE")
    class Service {

        @Test
        @DisplayName("delega en ServiceJpaRepository y devuelve true si existe")
        void delega_y_devuelve_true() {
            when(serviceJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(true);

            assertThat(port.exists(ApplicationType.SERVICE, ITEM_ID, COMPANY_ID)).isTrue();
            verifyNoInteractions(productJpaRepository, productCategoryJpaRepository,
                    serviceCategoryJpaRepository);
        }

        @Test
        @DisplayName("delega en ServiceJpaRepository y devuelve false si no existe")
        void delega_y_devuelve_false() {
            when(serviceJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(false);

            assertThat(port.exists(ApplicationType.SERVICE, ITEM_ID, COMPANY_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("CATEGORY — la categoria puede ser de producto o de servicio")
    class Category {

        @Test
        @DisplayName("una categoria de producto responde true sin consultar la de servicio")
        void categoria_de_producto_responde_true_por_cortocircuito() {
            when(productCategoryJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(true);

            assertThat(port.exists(ApplicationType.CATEGORY, ITEM_ID, COMPANY_ID)).isTrue();

            // El OR de Java es perezoso: si la primera rama ya dio true, la segunda
            // consulta ni se dispara.
            verifyNoInteractions(serviceCategoryJpaRepository);
            verifyNoInteractions(productJpaRepository, serviceJpaRepository);
        }

        @Test
        @DisplayName("sin categoria de producto, una categoria de servicio tambien responde true")
        void categoria_de_servicio_responde_true_cuando_falla_la_de_producto() {
            when(productCategoryJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(false);
            when(serviceCategoryJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(true);

            assertThat(port.exists(ApplicationType.CATEGORY, ITEM_ID, COMPANY_ID)).isTrue();
        }

        @Test
        @DisplayName("sin categoria de producto ni de servicio responde false")
        void ninguna_categoria_responde_false() {
            when(productCategoryJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(false);
            when(serviceCategoryJpaRepository.existsByIdAndCompany_Id(ITEM_ID, COMPANY_ID))
                    .thenReturn(false);

            assertThat(port.exists(ApplicationType.CATEGORY, ITEM_ID, COMPANY_ID)).isFalse();
        }
    }
}
