package com.vetsoftware.app.companytaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.NitVerificationDigit;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import java.util.Optional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyTaxProfileService")
class CreateCompanyTaxProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 28, 17, 5, 40);

    /**
     * El {@code Clock} no es un puerto: se inyecta de verdad y fijo, para que la
     * vigencia con la que nace el perfil sea comprobable.
     */
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CompanyTaxProfileRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private EconomicActivityQueryPort economicActivityQueryPort;

    private CreateCompanyTaxProfileService service;

    @BeforeEach
    void servicio() {
        service = new CreateCompanyTaxProfileService(repository, companyQueryPort,
                economicActivityQueryPort, RELOJ);
    }

    @Captor
    private ArgumentCaptor<CompanyTaxProfile> profileCaptor;

    private void laEmpresaExisteYSinPerfilPrevio() {
        when(companyQueryPort.findById(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyTaxProfileMother.CLINICA));
        when(repository.existsCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(false);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("recalcula el DV del NIT e ignora el que manda el cliente")
        void recalcula_el_dv_del_nit() {
            laEmpresaExisteYSinPerfilPrevio();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.VETERINARIA.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.VETERINARIA));
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilNit());

            service.execute(CompanyTaxProfileMother.comandoCrear());

            verify(repository).save(profileCaptor.capture());
            CompanyTaxProfile guardado = profileCaptor.getValue();
            assertThat(guardado.getCompanyDocumentVerificationDigit())
                    .isEqualTo(NitVerificationDigit.calculate(CompanyTaxProfileMother.NIT))
                    .isNotEqualTo(CompanyTaxProfileMother.DV_ENTRANTE_INCORRECTO);
        }

        @Test
        @DisplayName("resuelve la empresa y la actividad economica por los puertos, no por los ids sueltos")
        void resuelve_las_referencias_por_los_puertos() {
            laEmpresaExisteYSinPerfilPrevio();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.VETERINARIA.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.VETERINARIA));
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilNit());

            service.execute(CompanyTaxProfileMother.comandoCrear());

            verify(repository).save(profileCaptor.capture());
            CompanyTaxProfile guardado = profileCaptor.getValue();
            assertThat(guardado.getCompany()).isEqualTo(CompanyTaxProfileMother.CLINICA);
            assertThat(guardado.getEconomicActivity())
                    .isEqualTo(CompanyTaxProfileMother.VETERINARIA);
        }

        @Test
        @DisplayName("traduce cada codigo de responsabilidad a su VO, en orden")
        void traduce_cada_codigo_de_responsabilidad() {
            laEmpresaExisteYSinPerfilPrevio();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.VETERINARIA.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.VETERINARIA));
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilNit());

            service.execute(CompanyTaxProfileMother.comandoCrear());

            verify(repository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getResponsibilities()).containsExactly(
                    new CompanyTaxProfileResponsibility("O-13"),
                    new CompanyTaxProfileResponsibility("O-15"));
        }

        @Test
        @DisplayName("persona natural: sin DV y sin consultar el puerto de actividad economica")
        void persona_natural_sin_dv_ni_actividad() {
            when(companyQueryPort.findById(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.CLINICA));
            when(repository.existsCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(false);
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilCedula());

            service.execute(CompanyTaxProfileMother.comandoCrearCedula());

            verify(repository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getCompanyDocumentVerificationDigit()).isNull();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("sin actividad economica ni responsabilidades: no consulta el puerto y guarda lista vacia")
        void sin_actividad_ni_responsabilidades() {
            laEmpresaExisteYSinPerfilPrevio();
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilCedula());

            service.execute(CompanyTaxProfileMother.comandoCrearSinActividad());

            verify(repository).save(profileCaptor.capture());
            assertThat(profileCaptor.getValue().getEconomicActivity()).isNull();
            assertThat(profileCaptor.getValue().getResponsibilities()).isEmpty();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("devuelve el DTO del perfil ya persistido")
        void devuelve_el_dto_del_perfil_persistido() {
            laEmpresaExisteYSinPerfilPrevio();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.VETERINARIA.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.VETERINARIA));
            when(repository.save(any())).thenReturn(CompanyTaxProfileMother.perfilNit());

            CompanyTaxProfileDto dto = service.execute(CompanyTaxProfileMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
            assertThat(dto.legalName()).isEqualTo(CompanyTaxProfileMother.RAZON_SOCIAL);
        }
    }

    @Nested
    @DisplayName("fallos que no deben escribir")
    class Fallos {

        @Test
        @DisplayName("empresa inexistente: no consulta existencia ni actividad, no persiste")
        void empresa_inexistente() {
            when(companyQueryPort.findById(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + CompanyTaxProfileMother.COMPANY_ID);

            verifyNoInteractions(repository, economicActivityQueryPort);
        }

        @Test
        @DisplayName("la empresa ya tiene perfil: no consulta actividad ni persiste")
        void la_empresa_ya_tiene_perfil() {
            when(companyQueryPort.findById(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.CLINICA));
            when(repository.existsCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoCrear()))
                    .isInstanceOf(CompanyTaxProfileAlreadyExistsException.class)
                    .hasMessageContaining(
                            "already exists for company: " + CompanyTaxProfileMother.COMPANY_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("actividad economica inexistente: no persiste")
        void actividad_economica_inexistente() {
            laEmpresaExisteYSinPerfilPrevio();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.VETERINARIA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Economic activity not found: "
                            + CompanyTaxProfileMother.VETERINARIA.id());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un NIT invalido interrumpe la creacion antes de persistir")
        void un_nit_invalido_interrumpe_la_creacion() {
            laEmpresaExisteYSinPerfilPrevio();
            CreateCompanyTaxProfileCommand comando = new CreateCompanyTaxProfileCommand(
                    CompanyDocumentType.NIT, "90012A456",
                    CompanyTaxProfileMother.DV_ENTRANTE_INCORRECTO,
                    CompanyTaxProfileMother.RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA,
                    CompanyTaxProfileMother.EMAIL_FISCAL, CompanyTaxProfileMother.NOMBRE_COMERCIAL,
                    null, null, CompanyTaxProfileMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only digits");

            verify(repository, never()).save(any());
        }
    }
}
