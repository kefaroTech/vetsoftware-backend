package com.vetsoftware.app.companytaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.NitVerificationDigit;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
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
@DisplayName("UpdateCompanyTaxProfileService")
class UpdateCompanyTaxProfileServiceTest {

    @Mock
    private CompanyTaxProfileRepository repository;
    @Mock
    private EconomicActivityQueryPort economicActivityQueryPort;

    @InjectMocks
    private UpdateCompanyTaxProfileService service;

    @Captor
    private ArgumentCaptor<CompanyTaxProfile> profileCaptor;

    private void elPerfilExiste() {
        when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("recalcula el DV del NIT e ignora el que manda el cliente")
        void recalcula_el_dv_del_nit() {
            elPerfilExiste();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.COMERCIO));

            service.execute(CompanyTaxProfileMother.comandoActualizar());

            verify(repository).save(profileCaptor.capture());
            CompanyTaxProfile guardado = profileCaptor.getValue();
            assertThat(guardado.getCompanyDocumentVerificationDigit())
                    .isEqualTo(NitVerificationDigit.calculate("830053800"))
                    .isNotEqualTo(CompanyTaxProfileMother.DV_ENTRANTE_INCORRECTO);
        }

        @Test
        @DisplayName("reemplaza regimen, actividad economica y responsabilidades resueltas por el puerto")
        void reemplaza_regimen_actividad_y_responsabilidades() {
            elPerfilExiste();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.COMERCIO));

            service.execute(CompanyTaxProfileMother.comandoActualizar());

            verify(repository).save(profileCaptor.capture());
            CompanyTaxProfile guardado = profileCaptor.getValue();
            assertThat(guardado.getTaxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(guardado.getEconomicActivity()).isEqualTo(CompanyTaxProfileMother.COMERCIO);
            assertThat(guardado.getResponsibilities())
                    .containsExactly(new CompanyTaxProfileResponsibility("O-15"));
        }

        @Test
        @DisplayName("persona natural: sin DV y sin consultar el puerto de actividad economica")
        void persona_natural_sin_dv_ni_actividad() {
            when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(CompanyTaxProfileMother.comandoActualizarCedula());

            verify(repository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getCompanyDocumentVerificationDigit()).isNull();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("sin actividad economica ni responsabilidades: no consulta el puerto y deja lista vacia")
        void sin_actividad_ni_responsabilidades() {
            elPerfilExiste();

            service.execute(CompanyTaxProfileMother.comandoActualizarSinActividad());

            verify(repository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getEconomicActivity()).isNull();
            assertThat(profileCaptor.getValue().getResponsibilities()).isEmpty();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("devuelve el DTO del perfil ya actualizado")
        void devuelve_el_dto_del_perfil_actualizado() {
            elPerfilExiste();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.COMERCIO));

            CompanyTaxProfileDto dto = service.execute(CompanyTaxProfileMother.comandoActualizar());

            assertThat(dto.legalName()).isEqualTo("Clinica Veterinaria Sur S.A.S.");
        }
    }

    @Nested
    @DisplayName("fallos que no deben escribir")
    class Fallos {

        @Test
        @DisplayName("perfil inexistente: no consulta actividad economica ni persiste")
        void perfil_inexistente() {
            when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoActualizar()))
                    .isInstanceOf(CompanyTaxProfileNotFoundException.class).hasMessageContaining(
                            "not found for company: " + CompanyTaxProfileMother.COMPANY_ID);

            verifyNoInteractions(economicActivityQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actividad economica inexistente: no persiste")
        void actividad_economica_inexistente() {
            when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Economic activity not found: "
                            + CompanyTaxProfileMother.COMERCIO.id());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un NIT invalido interrumpe la actualizacion antes de persistir")
        void un_nit_invalido_interrumpe_la_actualizacion() {
            when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
            UpdateCompanyTaxProfileCommand comando = new UpdateCompanyTaxProfileCommand(
                    CompanyDocumentType.NIT, "83005A800",
                    CompanyTaxProfileMother.DV_ENTRANTE_INCORRECTO,
                    "Clinica Veterinaria Sur S.A.S.", TaxRegime.NO_RESPONSABLE_IVA,
                    "contabilidad@vetsur.com", "Vet Sur", null, null,
                    CompanyTaxProfileMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only digits");

            verify(repository, never()).save(any());
        }
    }
}
