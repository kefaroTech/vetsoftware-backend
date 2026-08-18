package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindServiceCategoryService")
class FindServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repository;

    private FindServiceCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new FindServiceCategoryService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve la categoria de la empresa del contexto")
        void devuelve_la_categoria_de_la_empresa_del_contexto() {
            ServiceCategory categoria = ServiceCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.of(categoria));

            ServiceCategoryDto dto = service.findById(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(dto.name()).isEqualTo("Consultas");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("lanza no encontrada si no existe en la empresa")
        void lanza_no_encontrada_si_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID))
                    .isInstanceOf(ServiceCategoryNotFoundException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceCategoryMother.CATEGORY_ID);
        }
    }
}
