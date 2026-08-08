package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.WeightRecordNotFoundException;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindLatestWeightRecordService")
class FindLatestWeightRecordServiceTest {

    @Mock
    private WeightRecordRepository repository;

    @InjectMocks
    private FindLatestWeightRecordService service;

    @Test
    @DisplayName("devuelve el ultimo registro de la serie")
    void devuelve_el_ultimo_registro_de_la_serie() {
        when(repository.findLatestByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID)).thenReturn(Optional.of(WeightRecordMother.manual()));

        WeightRecordDto dto = service.findLatest(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(WeightRecordMother.RECORD_ID);
        assertThat(dto.value()).isEqualByComparingTo("12.50");
        assertThat(dto.measuredAt()).isEqualTo(WeightRecordMother.MEDIDO_EL);
    }

    @Test
    @DisplayName("consulta acotando por empresa")
    void consulta_acotando_por_empresa() {
        when(repository.findLatestByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID)).thenReturn(Optional.of(WeightRecordMother.manual()));

        service.findLatest(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        verify(repository).findLatestByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID);
    }

    @Test
    @DisplayName("un animal sin pesos lanza WeightRecordNotFoundException")
    void un_animal_sin_pesos_lanza_not_found() {
        when(repository.findLatestByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.findLatest(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .isInstanceOf(WeightRecordNotFoundException.class);
    }

    @Test
    @DisplayName("DEFECTO FIJADO: el mensaje de error reporta el id del ANIMAL como si fuera "
            + "el del registro de peso")
    void defecto_fijado_el_mensaje_reporta_el_id_del_animal() {
        // FindLatestWeightRecordService construye
        // WeightRecordNotFoundException(animalId),
        // y esa excepcion formatea "WeightRecord not found: {id}". Para el animal 100
        // sin
        // pesos, el cliente recibe "WeightRecord not found: 100" — un id de registro
        // que
        // no existe y que ademas puede coincidir con el de OTRO registro real.
        //
        // Este test fija el comportamiento actual para que el cambio sea deliberado.
        // Al corregirlo (mensaje propio del tipo "no weight records for animal 100"),
        // este test debe actualizarse, no borrarse.
        when(repository.findLatestByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.findLatest(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .hasMessageContaining("WeightRecord not found: " + AnimalMother.ANIMAL_ID);
    }
}
