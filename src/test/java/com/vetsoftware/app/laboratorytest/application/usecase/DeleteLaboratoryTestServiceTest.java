package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteLaboratoryTestService")
class DeleteLaboratoryTestServiceTest {

    private static final Long ID = LaboratoryTestMother.ID;

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private DeleteLaboratoryTestService service;

    @Test
    @DisplayName("borra la muestra existente por su id")
    void borra_la_muestra_existente() {
        when(repository.findById(ID))
                .thenReturn(Optional.of(LaboratoryTestMother.pendienteDeToma()));

        service.execute(ID);

        verify(repository).delete(ID);
    }

    @Test
    @DisplayName("una muestra inexistente no dispara ningun borrado")
    void una_muestra_inexistente_no_dispara_borrado() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ID))
                .isInstanceOf(LaboratoryTestNotFoundException.class)
                .hasMessageContaining("LaboratoryTest not found: 42");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("borrar una muestra ya deshabilitada sigue siendo valido: el borrado es logico")
    void borrar_una_muestra_ya_deshabilitada_sigue_siendo_valido() {
        when(repository.findById(ID)).thenReturn(Optional.of(LaboratoryTestMother.deshabilitada()));

        assertThatCode(() -> service.execute(ID)).doesNotThrowAnyException();

        verify(repository).delete(ID);
    }
}
