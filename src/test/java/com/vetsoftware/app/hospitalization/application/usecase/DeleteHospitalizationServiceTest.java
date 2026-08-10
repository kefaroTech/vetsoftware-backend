package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteHospitalizationService")
class DeleteHospitalizationServiceTest {

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private DeleteHospitalizationService service;

    @Test
    @DisplayName("borra la hospitalizacion que existe")
    void borra_la_hospitalizacion_que_existe() {
        when(repository.findById(HospitalizationMother.HOSPITALIZATION_ID))
                .thenReturn(Optional.of(HospitalizationMother.internado()));

        service.execute(HospitalizationMother.HOSPITALIZATION_ID);

        verify(repository).delete(HospitalizationMother.HOSPITALIZATION_ID);
    }

    @Test
    @DisplayName("id inexistente: excepcion de dominio y ningun borrado")
    void id_inexistente_no_borra_nada() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(404L))
                .isInstanceOf(HospitalizationNotFoundException.class)
                .hasMessageContaining("Hospitalization not found: 404");

        verify(repository, never()).delete(any());
    }
}
