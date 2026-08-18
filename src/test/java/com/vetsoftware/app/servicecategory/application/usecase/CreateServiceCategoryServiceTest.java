package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateServiceCategoryService")
class CreateServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateServiceCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateServiceCategoryService(repository, companyQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve la compania por el puerto y persiste la categoria")
        void resuelve_la_compania_y_persiste_la_categoria() {
            when(companyQueryPort.findById(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndName(ServiceCategoryMother.COMPANY_ID, "Consultas"))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceCategoryDto dto = service.execute(ServiceCategoryMother.comandoCrear());

            ArgumentCaptor<ServiceCategory> guardado = ArgumentCaptor
                    .forClass(ServiceCategory.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Consultas");
            assertThat(guardado.getValue().getDescription()).isEqualTo("Categoria de consultas");
            assertThat(guardado.getValue().getCompany()).isEqualTo(ServiceCategoryMother.CLINICA);
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Consultas");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca el repositorio si la compania no existe")
        void no_toca_el_repositorio_si_la_compania_no_existe() {
            when(companyQueryPort.findById(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ServiceCategoryMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no persiste si el nombre ya existe en la empresa")
        void no_persiste_si_el_nombre_ya_existe_en_la_empresa() {
            when(companyQueryPort.findById(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndName(ServiceCategoryMother.COMPANY_ID, "Consultas"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.comandoCrear()))
                    .isInstanceOf(ServiceCategoryNameAlreadyExistsException.class)
                    .hasMessageContaining("Consultas");

            verify(repository, never()).save(any());
        }
    }
}
