package com.vetsoftware.app.animalalert.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAnimalAlertService")
class DeleteAnimalAlertServiceTest {

    @Mock
    private AnimalAlertRepository repository;

    @InjectMocks
    private DeleteAnimalAlertService service;

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("existiendo en la empresa, borra acotando por empresa")
        void existiendo_en_la_empresa_borra_acotando_por_empresa() {
            when(repository.findByIdAndCompanyId(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.COMPANY_ID))
                    .thenReturn(Optional.of(AnimalAlertMother.alergia()));

            service.execute(AnimalAlertMother.ALERT_ID, AnimalAlertMother.COMPANY_ID);

            verify(repository).delete(AnimalAlertMother.ALERT_ID, AnimalAlertMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("una alerta de otra empresa no existe y no se borra nada")
        void una_alerta_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(AnimalAlertMother.ALERT_ID, AnimalAlertMother.COMPANY_ID))
                    .isInstanceOf(AnimalAlertNotFoundException.class)
                    .hasMessageContaining("AnimalAlert not found: " + AnimalAlertMother.ALERT_ID);

            verify(repository, never()).delete(AnimalAlertMother.ALERT_ID,
                    AnimalAlertMother.COMPANY_ID);
        }
    }
}
