package com.vetsoftware.app.medicamentprescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateMedicamentPrescriptionService")
class ReactivateMedicamentPrescriptionServiceTest {

    private static final Long ID = MedicamentPrescriptionMother.ID;
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicamentPrescriptionRepository repository;

    @InjectMocks
    private ReactivateMedicamentPrescriptionService service;

    @Test
    @DisplayName("reactiva y devuelve la linea ya habilitada")
    void reactiva_y_devuelve_la_linea_ya_habilitada() {
        when(repository.reactivate(ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ID, COMPANY_ID))
                .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));

        MedicamentPrescriptionDto dto = service.execute(ID, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(ID, COMPANY_ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(ID, COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(ID, COMPANY_ID))
                .isInstanceOf(MedicamentPrescriptionNotFoundException.class)
                .hasMessageContaining(String.valueOf(ID));

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("si la linea desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_la_linea_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ID, COMPANY_ID))
                .isInstanceOf(MedicamentPrescriptionNotFoundException.class);
    }

    @Test
    @DisplayName("sin empresa (camino SYSTEM) reactiva y relee sin acotar")
    void sin_empresa_reactiva_sin_acotar() {
        when(repository.reactivate(ID)).thenReturn(1);
        when(repository.findById(ID))
                .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));

        assertThat(service.execute(ID, null).id()).isEqualTo(ID);

        verify(repository, never()).reactivate(anyLong(), anyLong());
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * La linea de receta no tiene {@code company_id}: su empresa es la de la receta
         * padre y el filtro viaja por un EXISTS. En la reactivacion no hay lectura
         * previa que valide la propiedad, asi que ese EXISTS es la unica barrera — la
         * correccion de {@code create} no habia llegado hasta aqui.
         */
        @Test
        @DisplayName("la linea de la receta de otra empresa no se reactiva ni se relee")
        void la_linea_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(ID, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, OTRA_EMPRESA))
                    .isInstanceOf(MedicamentPrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
            verify(repository, never()).findById(anyLong());
            verify(repository, never()).save(any());
        }
    }
}
