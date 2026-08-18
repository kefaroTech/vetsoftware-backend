package com.vetsoftware.app.animalalert.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.application.command.UpdateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAnimalAlertService")
class UpdateAnimalAlertServiceTest {

    @Mock
    private AnimalAlertRepository repository;

    @InjectMocks
    private UpdateAnimalAlertService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("carga la alerta acotando por empresa, la muta y la guarda")
        void carga_la_alerta_acotando_por_empresa_la_muta_y_la_guarda() {
            when(repository.findByIdAndCompanyId(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalAlertMother.alergia()));
            when(repository.save(any(AnimalAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            AnimalAlertDto dto = service.execute(AnimalAlertMother.comandoActualizar());

            assertThat(dto.type()).isEqualTo(AlertType.BEHAVIOR);
            assertThat(dto.description()).isEqualTo("Agresivo con extranos");
            assertThat(dto.severity()).isEqualTo(AlertSeverity.MEDIUM);

            ArgumentCaptor<AnimalAlert> captor = ArgumentCaptor.forClass(AnimalAlert.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(AlertType.BEHAVIOR);
            assertThat(captor.getValue().getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("una alerta que no existe en la empresa no se guarda")
        void una_alerta_de_otra_empresa_no_se_guarda() {
            when(repository.findByIdAndCompanyId(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(Optional.empty());

            UpdateAnimalAlertCommand command = AnimalAlertMother.comandoActualizar();

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(AnimalAlertNotFoundException.class)
                    .hasMessageContaining("AnimalAlert not found: " + AnimalAlertMother.ALERT_ID);

            verify(repository, never()).save(any(AnimalAlert.class));
        }
    }
}
