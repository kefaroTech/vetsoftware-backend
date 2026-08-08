package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteWeightRecordService")
class DeleteWeightRecordServiceTest {

    @Mock
    private WeightRecordRepository repository;

    @InjectMocks
    private DeleteWeightRecordService service;

    @Test
    @DisplayName("borra tras comprobar que el registro es de ese animal y de esa empresa")
    void borra_tras_comprobar_animal_y_empresa() {
        when(repository.findByIdAndAnimalIdAndCompanyId(WeightRecordMother.RECORD_ID,
                AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(WeightRecordMother.manual()));

        service.execute(WeightRecordMother.RECORD_ID, AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID);

        // La lectura valida las tres claves; el borrado solo necesita (id, companyId)
        // porque la pertenencia al animal ya quedo comprobada.
        verify(repository).findByIdAndAnimalIdAndCompanyId(WeightRecordMother.RECORD_ID,
                AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
        verify(repository).delete(WeightRecordMother.RECORD_ID, AnimalMother.COMPANY_ID);
    }

    @Test
    @DisplayName("un registro que no existe no se borra")
    void un_registro_que_no_existe_no_se_borra() {
        when(repository.findByIdAndAnimalIdAndCompanyId(WeightRecordMother.RECORD_ID,
                AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(WeightRecordMother.RECORD_ID,
                AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .isInstanceOf(WeightRecordNotFoundException.class)
                .hasMessageContaining("WeightRecord not found: " + WeightRecordMother.RECORD_ID);

        verify(repository, never()).delete(anyLong(), anyLong());
    }

    @Test
    @DisplayName("un registro de otro animal no se borra aunque el id exista")
    void un_registro_de_otro_animal_no_se_borra() {
        // Sin el animalId en la lectura, un DELETE /animals/999/weight-records/500
        // borraria el registro 500 aunque pertenezca al animal 100.
        long otroAnimal = 999L;
        when(repository.findByIdAndAnimalIdAndCompanyId(WeightRecordMother.RECORD_ID, otroAnimal,
                AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(WeightRecordMother.RECORD_ID, otroAnimal,
                AnimalMother.COMPANY_ID)).isInstanceOf(WeightRecordNotFoundException.class);

        verify(repository, never()).delete(anyLong(), anyLong());
    }
}
