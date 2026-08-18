package com.vetsoftware.app.productcategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNameAlreadyExistsException;
import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductCategoryService")
class CreateProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateProductCategoryService service;

    @org.junit.jupiter.api.BeforeEach
    void crearServicio() {
        service = new CreateProductCategoryService(repository, companyQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve la compania por el puerto y persiste la categoria")
        void resuelve_la_compania_y_persiste_la_categoria() {
            when(companyQueryPort.findById(ProductCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndName(ProductCategoryMother.COMPANY_ID,
                    "Medicamentos")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProductCategoryDto dto = service.execute(ProductCategoryMother.comandoCrear());

            ArgumentCaptor<ProductCategory> guardado = ArgumentCaptor
                    .forClass(ProductCategory.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Medicamentos");
            assertThat(guardado.getValue().getDescription()).isEqualTo("Categoria de medicamentos");
            assertThat(guardado.getValue().getCompany()).isEqualTo(ProductCategoryMother.CLINICA);
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Medicamentos");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca el repositorio si la compania no existe")
        void no_toca_el_repositorio_si_la_compania_no_existe() {
            when(companyQueryPort.findById(ProductCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ProductCategoryMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no persiste si el nombre ya existe en la empresa")
        void no_persiste_si_el_nombre_ya_existe_en_la_empresa() {
            when(companyQueryPort.findById(ProductCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.of(ProductCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndName(ProductCategoryMother.COMPANY_ID,
                    "Medicamentos")).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.comandoCrear()))
                    .isInstanceOf(ProductCategoryNameAlreadyExistsException.class)
                    .hasMessageContaining("Medicamentos");

            verify(repository, org.mockito.Mockito.never()).save(any());
        }
    }
}
