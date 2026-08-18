package com.vetsoftware.app.companysettings.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.out.CompanySettingRepository;
import com.vetsoftware.app.companysettings.domain.CompanySetting;
import com.vetsoftware.app.companysettings.testsupport.CompanySettingMother;
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
@DisplayName("SetCompanySettingService")
class SetCompanySettingServiceTest {

    @Mock
    private CompanySettingRepository repository;

    @InjectMocks
    private SetCompanySettingService service;

    @Nested
    @DisplayName("ajuste ya existente")
    class AjusteExistente {

        @Test
        @DisplayName("actualiza el value del ajuste encontrado en vez de crear uno nuevo")
        void actualiza_el_value_del_ajuste_encontrado() {
            CompanySetting existente = CompanySettingMother.ajusteExistente();
            when(repository.find(CompanySettingMother.COMPANY_ID,
                    CompanySettingMother.PROPERTY_NAME)).thenReturn(Optional.of(existente));
            when(repository.save(existente)).thenReturn(existente);

            CompanySettingDto resultado = service.set(new SetCompanySettingCommand(
                    CompanySettingMother.COMPANY_ID, CompanySettingMother.PROPERTY_NAME, "false"));

            ArgumentCaptor<CompanySetting> guardado = ArgumentCaptor.forClass(CompanySetting.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getValue()).isEqualTo("false");
            assertThat(guardado.getValue().getId()).isEqualTo(CompanySettingMother.SETTING_ID);
            assertThat(resultado.propertyName()).isEqualTo(CompanySettingMother.PROPERTY_NAME);
            assertThat(resultado.value()).isEqualTo("false");
        }

        @Test
        @DisplayName("un value invalido rechaza la actualizacion sin guardar")
        void un_value_invalido_rechaza_la_actualizacion_sin_guardar() {
            CompanySetting existente = CompanySettingMother.ajusteExistente();
            when(repository.find(CompanySettingMother.COMPANY_ID,
                    CompanySettingMother.PROPERTY_NAME)).thenReturn(Optional.of(existente));

            assertThatThrownBy(
                    () -> service.set(new SetCompanySettingCommand(CompanySettingMother.COMPANY_ID,
                            CompanySettingMother.PROPERTY_NAME, "x".repeat(256))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be 255 chars or less");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("ajuste inexistente")
    class AjusteInexistente {

        @Test
        @DisplayName("crea un ajuste nuevo cuando no habia fila previa")
        void crea_un_ajuste_nuevo() {
            when(repository.find(CompanySettingMother.COMPANY_ID, "nueva.propiedad"))
                    .thenReturn(Optional.empty());
            when(repository.save(any(CompanySetting.class))).thenAnswer(inv -> inv.getArgument(0));

            CompanySettingDto resultado = service.set(new SetCompanySettingCommand(
                    CompanySettingMother.COMPANY_ID, "nueva.propiedad", "10"));

            ArgumentCaptor<CompanySetting> guardado = ArgumentCaptor.forClass(CompanySetting.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(guardado.getValue().getCompanyId())
                    .isEqualTo(CompanySettingMother.COMPANY_ID);
            assertThat(guardado.getValue().getPropertyName()).isEqualTo("nueva.propiedad");
            assertThat(resultado.value()).isEqualTo("10");
        }
    }
}
