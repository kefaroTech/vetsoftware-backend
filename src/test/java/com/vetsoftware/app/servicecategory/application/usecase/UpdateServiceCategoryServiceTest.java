package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
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
@DisplayName("UpdateServiceCategoryService")
class UpdateServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateServiceCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateServiceCategoryService(repository, companyQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("resuelve la compania, valida el nombre y actualiza la categoria")
        void resuelve_la_compania_valida_el_nombre_y_actualiza() {
            ServiceCategory existente = ServiceCategoryMother.activa();
            UpdateServiceCategoryCommand command = ServiceCategoryMother.comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(command.companyId()))
                    .thenReturn(Optional.of(ServiceCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(),
                    command.id())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceCategoryDto dto = service.execute(command);

            ArgumentCaptor<ServiceCategory> guardado = ArgumentCaptor
                    .forClass(ServiceCategory.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getName()).isEqualTo("Cirugias");
            assertThat(guardado.getValue().getDescription()).isEqualTo("Categoria de cirugias");
            assertThat(guardado.getValue().getUpdatedBy())
                    .isEqualTo(ServiceCategoryMother.EMPLOYEE_ID);
            assertThat(guardado.getValue().getVersion()).isEqualTo(3L);
            assertThat(dto.name()).isEqualTo("Cirugias");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca la compania ni persiste si la categoria no existe")
        void no_toca_la_compania_ni_persiste_si_la_categoria_no_existe() {
            UpdateServiceCategoryCommand command = ServiceCategoryMother.comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ServiceCategoryNotFoundException.class)
                    .hasMessageContaining("ServiceCategory not found: " + command.id());

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si la compania no existe")
        void no_persiste_si_la_compania_no_existe() {
            ServiceCategory existente = ServiceCategoryMother.activa();
            UpdateServiceCategoryCommand command = ServiceCategoryMother.comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(command.companyId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + command.companyId());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si el nombre ya existe en otra categoria de la empresa")
        void no_persiste_si_el_nombre_ya_existe_en_otra_categoria() {
            ServiceCategory existente = ServiceCategoryMother.activa();
            UpdateServiceCategoryCommand command = ServiceCategoryMother.comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(command.companyId()))
                    .thenReturn(Optional.of(ServiceCategoryMother.CLINICA));
            when(repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(),
                    command.id())).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ServiceCategoryNameAlreadyExistsException.class)
                    .hasMessageContaining(command.name());

            verify(repository, never()).save(any());
        }
    }
}
