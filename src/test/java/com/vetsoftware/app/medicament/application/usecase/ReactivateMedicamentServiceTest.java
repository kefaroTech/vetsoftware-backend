package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateMedicamentService")
class ReactivateMedicamentServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ReactivateMedicamentService service;

    @Test
    @DisplayName("reactiva y devuelve el medicamento cuando reactivate afecta una fila")
    void reactiva_y_devuelve_el_medicamento() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(
                Optional.of(MedicamentMother.propioDeEmpresa(MedicamentMother.companyRef())));

        MedicamentDto dto = service.execute(1L, COMPANY_ID);

        assertThat(dto.name()).isEqualTo("Suero especial");
    }

    @Test
    @DisplayName("lanza MedicamentNotFoundException cuando reactivate afecta cero filas")
    void lanza_not_found_cuando_no_afecta_filas() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

        verify(repository, never()).findByIdAndCompanyId(any(), any());
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * En la reactivacion no hay lectura previa que valide la propiedad: el servicio
         * decide si existe mirando las filas afectadas, asi que la consulta acotada es
         * la unica barrera. Cero filas para el medicamento de otra empresa es
         * exactamente el comportamiento correcto — un 404 que no revela que el id
         * existe.
         */
        @Test
        @DisplayName("el medicamento de otra empresa no se reactiva y no se relee")
        void el_medicamento_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(1L, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(1L, OTRA_EMPRESA))
                    .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }
}
