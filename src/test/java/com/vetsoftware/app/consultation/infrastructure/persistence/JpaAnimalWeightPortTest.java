package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de escritura del peso capturado en la consulta, cruzando hacia el
 * agregado {@code WeightRecord} de la feature animal (ver "Cross-feature
 * references" del CLAUDE.md). {@code WeightRecordJpaEntity.of} es codigo real
 * de otra feature, no un doble: exactamente lo que valida que este adaptador
 * arma bien la fila (source = CONSULTATION) y propaga sus invariantes en vez de
 * tragarlas.
 */
@ExtendWith(MockitoExtension.class)
class JpaAnimalWeightPortTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final Long CONSULTATION_ID = 200L;
    private static final LocalDate MEDIDO = LocalDate.of(2026, 3, 1);

    @Mock
    private WeightRecordJpaRepository weightRecordJpaRepository;
    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @InjectMocks
    private JpaAnimalWeightPort port;

    @Nested
    @DisplayName("Registro del peso")
    class Registro {

        @Test
        @DisplayName("resuelve animal y compania por referencia y guarda el peso con origen consulta")
        void resuelve_animal_y_compania_y_guarda_el_peso() {
            AnimalJpaEntity animal = mock(AnimalJpaEntity.class);
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);
            when(animalJpaRepository.getReferenceById(ANIMAL_ID)).thenReturn(animal);
            when(companyJpaRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

            port.recordConsultationWeight(ANIMAL_ID, COMPANY_ID, new BigDecimal("12.500"),
                    "KILOGRAMS", MEDIDO, CONSULTATION_ID);

            ArgumentCaptor<WeightRecordJpaEntity> guardado = ArgumentCaptor
                    .forClass(WeightRecordJpaEntity.class);
            verify(weightRecordJpaRepository).save(guardado.capture());
            WeightRecordJpaEntity entity = guardado.getValue();
            assertThat(entity.getAnimal()).isSameAs(animal);
            assertThat(entity.getCompany()).isSameAs(company);
            assertThat(entity.getValue()).isEqualByComparingTo("12.500");
            assertThat(entity.getUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(entity.getSource()).isEqualTo(WeightSource.CONSULTATION);
            assertThat(entity.getSourceId()).isEqualTo(CONSULTATION_ID);
            assertThat(entity.getMeasuredAt()).isEqualTo(MEDIDO);
        }

        @Test
        @DisplayName("un peso no positivo propaga la invariante del agregado sin guardar nada")
        void un_peso_no_positivo_propaga_la_invariante_sin_guardar() {
            when(animalJpaRepository.getReferenceById(ANIMAL_ID))
                    .thenReturn(mock(AnimalJpaEntity.class));
            when(companyJpaRepository.getReferenceById(COMPANY_ID))
                    .thenReturn(mock(CompanyJpaEntity.class));

            assertThatThrownBy(() -> port.recordConsultationWeight(ANIMAL_ID, COMPANY_ID,
                    BigDecimal.ZERO, "KILOGRAMS", MEDIDO, CONSULTATION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weight value must be greater than zero");

            verify(weightRecordJpaRepository, never()).save(any());
        }
    }
}
