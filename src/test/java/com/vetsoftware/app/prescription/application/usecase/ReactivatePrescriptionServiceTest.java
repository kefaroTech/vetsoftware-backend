package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivatePrescriptionService")
class ReactivatePrescriptionServiceTest {

    private static final Long ID = PrescriptionMother.PRESCRIPTION_ID;
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private PrescriptionRepository repository;

    @InjectMocks
    private ReactivatePrescriptionService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva y devuelve el DTO recien leido")
        void reactiva_y_devuelve_el_dto() {
            when(repository.reactivate(ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));

            PrescriptionDto dto = service.execute(ID, COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) reactiva y relee sin acotar")
        void sin_empresa_reactiva_sin_acotar() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(PrescriptionMother.persistida()));

            assertThat(service.execute(ID, null).id()).isEqualTo(ID);

            verify(repository, never()).reactivate(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("receta inexistente")
    class Inexistente {

        @Test
        @DisplayName("reactivate() en cero lanza not-found sin leer de nuevo")
        void reactivate_en_cero_lanza_not_found() {
            when(repository.reactivate(ID, COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, COMPANY_ID))
                    .isInstanceOf(PrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El resto de la feature ya pasaba la empresa; solo la reactivacion quedo fuera
         * del patron, y es justo donde mas duele: no hay lectura previa que valide la
         * propiedad, el servicio decide si existe mirando las filas afectadas, asi que
         * el {@code AND company_id} del UPDATE es la unica barrera.
         */
        @Test
        @DisplayName("la receta de otra empresa no se resucita ni se relee")
        void la_receta_de_otra_empresa_no_se_resucita() {
            when(repository.reactivate(ID, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, OTRA_EMPRESA))
                    .isInstanceOf(PrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }
}
