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
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.internado()));

        service.execute(HospitalizationMother.HOSPITALIZATION_ID, HospitalizationMother.COMPANY_ID);

        verify(repository).delete(HospitalizationMother.HOSPITALIZATION_ID);
    }

    @Test
    @DisplayName("id inexistente: excepcion de dominio y ningun borrado")
    void id_inexistente_no_borra_nada() {
        when(repository.findByIdAndCompanyId(404L, HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(404L, HospitalizationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class)
                .hasMessageContaining("Hospitalization not found: 404");

        verify(repository, never()).delete(any());
    }

    /**
     * Antes de BE-COV, {@code execute(Long id)} no recibia companyId y su
     * {@code @PreAuthorize} solo exigia
     * {@code hasAuthority('hospitalization.delete')}: cualquier empleado con ese
     * permiso borraba la hospitalizacion de otra empresa adivinando el id. Ahora la
     * existencia se comprueba acotada por empresa y la fila ajena es indistinguible
     * de una inexistente.
     */
    @Test
    @DisplayName("una hospitalizacion de otra empresa no se borra: 404 y el repositorio no escribe")
    void una_hospitalizacion_de_otra_empresa_no_se_borra() {
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.OTRA_COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class).hasMessageContaining(
                        "Hospitalization not found: " + HospitalizationMother.HOSPITALIZATION_ID);

        verify(repository, never()).delete(any());
    }
}
