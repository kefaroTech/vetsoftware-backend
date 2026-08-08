package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWeightRecordService")
class CreateWeightRecordServiceTest {

    @Mock
    private AnimalRepository animalRepository;
    @Mock
    private WeightRecordRepository weightRecordRepository;

    @InjectMocks
    private CreateWeightRecordService service;

    @Captor
    private ArgumentCaptor<WeightRecord> captor;

    private void animalExiste() {
        when(animalRepository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.perroSano()));
    }

    private void guardaDevolviendoLoMismo() {
        when(weightRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateWeightRecordCommand comando(WeightType unidad, LocalDate medidoEl) {
        return new CreateWeightRecordCommand(AnimalMother.ANIMAL_ID, new BigDecimal("12.50"),
                unidad, medidoEl, "control de rutina", AnimalMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("registro creado")
    class RegistroCreado {

        @Test
        @DisplayName("siempre es MANUAL y sin sourceId: este caso de uso no viene de un evento")
        void siempre_es_manual_y_sin_source_id() {
            animalExiste();
            guardaDevolviendoLoMismo();

            service.execute(WeightRecordMother.comandoCrear());

            org.mockito.Mockito.verify(weightRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getSource()).isEqualTo(WeightSource.MANUAL);
            assertThat(captor.getValue().getSourceId()).isNull();
        }

        @Test
        @DisplayName("cuelga del animal y de la empresa DEL ANIMAL, no de la del comando")
        void cuelga_del_animal_y_de_la_empresa_del_animal() {
            animalExiste();
            guardaDevolviendoLoMismo();

            service.execute(WeightRecordMother.comandoCrear());

            org.mockito.Mockito.verify(weightRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getAnimal().id()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(captor.getValue().getAnimal().name()).isEqualTo("Firulais");
            assertThat(captor.getValue().getAnimal().code()).isEqualTo("A-001");
            assertThat(captor.getValue().getCompany()).isEqualTo(AnimalMother.CLINICA);
        }

        @Test
        @DisplayName("propaga valor y nota")
        void propaga_valor_y_nota() {
            animalExiste();
            guardaDevolviendoLoMismo();

            WeightRecordDto dto = service.execute(WeightRecordMother.comandoCrear());

            assertThat(dto.value()).isEqualByComparingTo("12.50");
            assertThat(dto.note()).isEqualTo("control de rutina");
        }
    }

    @Nested
    @DisplayName("valores por defecto del comando")
    class ValoresPorDefecto {

        @Test
        @DisplayName("sin unidad, hereda la unidad preferida del animal")
        void sin_unidad_hereda_la_del_animal() {
            animalExiste();
            guardaDevolviendoLoMismo();

            WeightRecordDto dto = service.execute(comando(null, WeightRecordMother.MEDIDO_EL));

            assertThat(dto.unit()).isEqualTo(WeightType.KILOGRAMS);
        }

        @Test
        @DisplayName("con unidad explicita, manda la del comando aunque difiera de la del animal")
        void con_unidad_explicita_manda_la_del_comando() {
            animalExiste();
            guardaDevolviendoLoMismo();

            WeightRecordDto dto = service
                    .execute(comando(WeightType.GRAMS, WeightRecordMother.MEDIDO_EL));

            assertThat(dto.unit()).isEqualTo(WeightType.GRAMS);
        }

        @Test
        @DisplayName("sin fecha, se registra como medido hoy")
        void sin_fecha_se_registra_como_medido_hoy() {
            animalExiste();
            guardaDevolviendoLoMismo();

            WeightRecordDto dto = service.execute(comando(WeightType.KILOGRAMS, null));

            assertThat(dto.measuredAt()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("con fecha explicita, se respeta")
        void con_fecha_explicita_se_respeta() {
            animalExiste();
            guardaDevolviendoLoMismo();

            LocalDate ayer = LocalDate.now().minusDays(1);

            assertThat(service.execute(comando(WeightType.KILOGRAMS, ayer)).measuredAt())
                    .isEqualTo(ayer);
        }
    }

    @Nested
    @DisplayName("rechazos")
    class Rechazos {

        @Test
        @DisplayName("animal de otra empresa: no se escribe nada")
        void animal_de_otra_empresa() {
            when(animalRepository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID,
                    AnimalMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(WeightRecordMother.comandoCrear()))
                    .isInstanceOf(AnimalNotFoundException.class)
                    .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

            verifyNoInteractions(weightRecordRepository);
        }

        @Test
        @DisplayName("un peso no positivo no llega al repositorio")
        void un_peso_no_positivo_no_llega_al_repositorio() {
            animalExiste();
            CreateWeightRecordCommand comando = new CreateWeightRecordCommand(
                    AnimalMother.ANIMAL_ID, BigDecimal.ZERO, WeightType.KILOGRAMS,
                    WeightRecordMother.MEDIDO_EL, null, AnimalMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be greater than zero");

            verifyNoInteractions(weightRecordRepository);
        }

        @Test
        @DisplayName("una medicion futura la corta el dominio, no el service")
        void una_medicion_futura_la_corta_el_dominio() {
            animalExiste();

            assertThatThrownBy(() -> service
                    .execute(comando(WeightType.KILOGRAMS, LocalDate.now().plusDays(1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("measuredAt cannot be in the future");

            verifyNoInteractions(weightRecordRepository);
        }
    }
}
