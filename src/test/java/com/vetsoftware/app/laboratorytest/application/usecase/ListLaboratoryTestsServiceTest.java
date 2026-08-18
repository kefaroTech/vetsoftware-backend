package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code ListLaboratoryTestsUseCase} esta cerrado a {@code hasRole('SYSTEM')} a
 * secas (BE-29): {@code repository.findAll()} no filtra por empresa, asi que
 * ningun tenant puede llegar aqui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListLaboratoryTestsService")
class ListLaboratoryTestsServiceTest {

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private ListLaboratoryTestsService service;

    @Test
    @DisplayName("devuelve el dto de cada muestra del repositorio")
    void devuelve_el_dto_de_cada_muestra() {
        when(repository.findAll()).thenReturn(
                List.of(LaboratoryTestMother.pendienteDeToma(), LaboratoryTestMother.validada()));

        List<LaboratoryTestDto> resultado = service.listAll();

        assertThat(resultado).extracting(LaboratoryTestDto::id)
                .containsExactly(LaboratoryTestMother.ID, LaboratoryTestMother.ID);
    }

    @Test
    @DisplayName("sin muestras devuelve una lista vacia, no null")
    void sin_muestras_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
