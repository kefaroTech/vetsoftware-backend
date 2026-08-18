package com.vetsoftware.app.service.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaServiceCategoryQueryPort (service)")
class JpaServiceCategoryQueryPortTest {

    private static final Long CATEGORY_ID = 20L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private ServiceCategoryJpaRepository serviceCategoryJpaRepository;

    @InjectMocks
    private JpaServiceCategoryQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la categoria encontrada, acotada a la empresa")
        void mapea_la_categoria_encontrada() {
            ServiceCategoryJpaEntity entidad = mock(ServiceCategoryJpaEntity.class);
            when(entidad.getId()).thenReturn(CATEGORY_ID);
            when(entidad.getName()).thenReturn("Consultas");
            when(serviceCategoryJpaRepository.findByIdAndCompany_Id(CATEGORY_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<ServiceCategoryRef> ref = port.findById(CATEGORY_ID, COMPANY_ID);

            assertThat(ref).contains(new ServiceCategoryRef(CATEGORY_ID, "Consultas"));
        }

        @Test
        @DisplayName("devuelve vacio si la categoria no existe o es de otra empresa")
        void devuelve_vacio_si_no_existe_o_es_ajena() {
            when(serviceCategoryJpaRepository.findByIdAndCompany_Id(CATEGORY_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(CATEGORY_ID, COMPANY_ID)).isEmpty();
        }
    }
}
