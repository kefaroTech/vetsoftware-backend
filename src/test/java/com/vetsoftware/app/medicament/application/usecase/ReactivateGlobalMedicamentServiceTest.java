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

/**
 * Reactivacion de un global pausado.
 *
 * <p>
 * Va por {@code reactivateGlobal(id)} y no por {@code reactivate(id, null)}
 * porque el segundo no reactivaria NADA: su {@code WHERE} es
 * {@code company_id = :companyId} y {@code company_id = NULL} no casa nunca en
 * SQL. Ese reparto entre las dos consultas es de infraestructura y quien lo
 * prueba de verdad es {@code MedicamentPersistenceIT} contra MySQL; lo que este
 * unitario fija es el contrato de arriba: el numero de filas afectadas ES la
 * comprobacion de existencia, no hay lectura previa que valide nada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateGlobalMedicamentService")
class ReactivateGlobalMedicamentServiceTest {

    private static final Long ID = MedicamentMother.MEDICAMENT_ID;

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ReactivateGlobalMedicamentService service;

    @Test
    @DisplayName("reactiva y devuelve el medicamento global recuperado")
    void reactiva_y_devuelve_el_global() {
        when(repository.reactivateGlobal(ID)).thenReturn(1);
        when(repository.findById(ID)).thenReturn(Optional.of(MedicamentMother.activoGeneral()));

        MedicamentDto dto = service.execute(ID);

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.general()).isTrue();
        assertThat(dto.company()).isNull();
    }

    @Nested
    @DisplayName("Validaciones — cero filas es un 404")
    class Validaciones {

        /**
         * No hay lectura previa: si el UPDATE no alcanza ninguna fila, el caso de uso
         * ni siquiera intenta releer. Que {@code findById} no se llame es lo que
         * distingue «no existe» de «existe pero no se pudo tocar».
         */
        @Test
        @DisplayName("si el UPDATE no afecta filas lanza 404 sin releer nada")
        void filas_cero_es_un_404() {
            when(repository.reactivateGlobal(ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findById(any());
        }

        /**
         * El {@code filter(Medicament::isGeneral)} de la relectura si es defensa en
         * profundidad —el {@code company_id IS NULL} del UPDATE ya acoto—, y se prueba
         * igualmente: si alguien cambiara la consulta nativa, esto es lo que impediria
         * devolver el DTO de la fila de un tenant como si fuera del catalogo global.
         */
        @Test
        @DisplayName("si la fila reactivada no fuera general, no se devuelve como global")
        void una_fila_no_general_no_se_devuelve() {
            when(repository.reactivateGlobal(ID)).thenReturn(1);
            when(repository.findById(ID))
                    .thenReturn(Optional.of(MedicamentMother.activoDeEmpresa()));

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));
        }
    }
}
