package com.vetsoftware.app.companybillingprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companybillingprofile.application.command.SucceedCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.out.CityQueryPort;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.BillingProfileSuccessionNotAfterCurrentException;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileNotFoundException;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El caso de uso que sustituye al {@code update} que esta feature no tiene.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SucceedCompanyBillingProfileService — cerrar la vigente y abrir la sucesora")
class SucceedCompanyBillingProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 28, 17, 5, 40);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CompanyBillingProfileRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;

    private SucceedCompanyBillingProfileService service;

    @BeforeEach
    void servicio() {
        service = new SucceedCompanyBillingProfileService(repository, cityQueryPort, RELOJ);
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("guarda DOS fichas: primero la vigente cerrada y despues la sucesora")
        void guarda_dos_fichas_primero_la_cerrada() {
            // El orden importa y no por estilo: si la sucesora entrara primero, las dos
            // filas calcularian el mismo current_profile_marker y
            // uq_company_billing_profiles_current pararia la operacion.
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE));

            ArgumentCaptor<CompanyBillingProfile> guardadas = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository, times(2)).save(guardadas.capture());
            List<CompanyBillingProfile> enOrden = guardadas.getAllValues();
            assertThat(enOrden.get(0).isCurrent()).as("la primera es la que se cierra").isFalse();
            assertThat(enOrden.get(0).getValidTo())
                    .isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
            assertThat(enOrden.get(1).isCurrent()).as("la segunda es la que nace vigente").isTrue();
        }

        @Test
        @DisplayName("la cadena queda sin hueco: la sucesora arranca el dia en que se cierra la anterior")
        void la_cadena_queda_sin_hueco() {
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE));

            ArgumentCaptor<CompanyBillingProfile> guardadas = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository, times(2)).save(guardadas.capture());
            assertThat(guardadas.getAllValues().get(0).getValidTo())
                    .isEqualTo(guardadas.getAllValues().get(1).getValidFrom());
        }

        @Test
        @DisplayName("la ficha cerrada conserva sus datos: la factura vieja sigue diciendo a quien se emitio")
        void la_ficha_cerrada_conserva_sus_datos() {
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE));

            ArgumentCaptor<CompanyBillingProfile> guardadas = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository, times(2)).save(guardadas.capture());
            CompanyBillingProfile cerrada = guardadas.getAllValues().get(0);
            assertThat(cerrada.getId()).as("es la fila que ya existia").isEqualTo(7L);
            assertThat(cerrada.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
            assertThat(cerrada.getValidFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
        }

        @Test
        @DisplayName("la sucesora trae los datos NUEVOS y nace sin id")
        void la_sucesora_trae_los_datos_nuevos() {
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE));

            ArgumentCaptor<CompanyBillingProfile> guardadas = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository, times(2)).save(guardadas.capture());
            CompanyBillingProfile sucesora = guardadas.getAllValues().get(1);
            assertThat(sucesora.getId()).isNull();
            assertThat(sucesora.getTaxId()).isEqualTo("901555444");
            assertThat(sucesora.getLegalName()).isEqualTo("Inversiones Pet II SAS");
            assertThat(sucesora.getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("devuelve la SUCESORA y no la ficha que se acaba de cerrar")
        void devuelve_la_sucesora() {
            // Es lo que el front tiene que pintar despues del cambio: la ficha que rige.
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CompanyBillingProfileDto dto = service
                    .execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE));

            assertThat(dto.validTo()).as("la devuelta es la vigente").isNull();
            assertThat(dto.taxId()).isEqualTo("901555444");
            assertThat(dto.validFrom()).isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin ficha vigente no hay nada que suceder: 404 y ni se consulta el municipio")
        void sin_ficha_vigente_no_hay_nada_que_suceder() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE)))
                    .isInstanceOf(CompanyBillingProfileNotFoundException.class)
                    .hasMessageContaining("has no current billing profile");

            verify(repository, never()).save(any());
            verifyNoInteractions(cityQueryPort);
        }

        @Test
        @DisplayName("un municipio inexistente deja la ficha vigente INTACTA y no escribe nada")
        void un_municipio_inexistente_deja_la_vigente_intacta() {
            // El municipio se resuelve ANTES de cerrar: con la transaccion bastaria para
            // que el UPDATE no cuajara, pero asi el resultado no depende del rollback.
            CompanyBillingProfile vigente = vigenteEnLaEmpresa();
            when(cityQueryPort.findById(900L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comandoDeSucesion(CompanyBillingProfileMother.SUCEDE_DESDE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: 900");

            verify(repository, never()).save(any());
            assertThat(vigente.isCurrent()).as("no se cerro").isTrue();
        }

        @Test
        @DisplayName("suceder EL MISMO DIA en que empezo la vigente se rechaza y no escribe nada")
        void suceder_el_mismo_dia_se_rechaza() {
            // La consecuencia del > estricto de chk_company_billing_profiles_validity. No
            // se corre la fecha al dia siguiente: se rechaza diciendo cual es la primera
            // fecha posible.
            CompanyBillingProfile vigente = vigenteEnLaEmpresa();
            municipioResuelto();

            assertThatThrownBy(() -> service
                    .execute(comandoDeSucesion(CompanyBillingProfileMother.RIGE_DESDE)))
                    .isInstanceOf(BillingProfileSuccessionNotAfterCurrentException.class)
                    .hasMessageContaining("the earliest possible date is 2026-01-16");

            verify(repository, never()).save(any());
            assertThat(vigente.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("el dia siguiente si sucede: es la primera fecha representable")
        void el_dia_siguiente_si_sucede() {
            vigenteEnLaEmpresa();
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CompanyBillingProfileDto dto = service
                    .execute(comandoDeSucesion(LocalDate.of(2026, 1, 16)));

            assertThat(dto.validFrom()).isEqualTo(LocalDate.of(2026, 1, 16));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("busca la vigente de la empresa del command, no la de cualquiera")
        void busca_la_vigente_de_la_empresa_del_command() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comandoDeSucesionDeEmpresa(CompanyBillingProfileMother.OTRA_COMPANY_ID,
                            CompanyBillingProfileMother.SUCEDE_DESDE)))
                    .isInstanceOf(CompanyBillingProfileNotFoundException.class);

            verify(repository).findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("la sucesora hereda la empresa del command: la sucesion no cambia de dueño")
        void la_sucesora_hereda_la_empresa_del_command() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.persistida(8L,
                            CompanyBillingProfileMother.OTRA_COMPANY_ID,
                            CompanyBillingProfileMother.RIGE_DESDE, null)));
            municipioResuelto();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSucesionDeEmpresa(CompanyBillingProfileMother.OTRA_COMPANY_ID,
                    CompanyBillingProfileMother.SUCEDE_DESDE));

            ArgumentCaptor<CompanyBillingProfile> guardadas = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository, times(2)).save(guardadas.capture());
            assertThat(guardadas.getAllValues()).extracting(CompanyBillingProfile::getCompanyId)
                    .containsOnly(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }
    }

    private CompanyBillingProfile vigenteEnLaEmpresa() {
        CompanyBillingProfile vigente = CompanyBillingProfileMother.persistida(7L);
        when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(vigente));
        return vigente;
    }

    private void municipioResuelto() {
        when(cityQueryPort.findById(900L))
                .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));
    }

    private static SucceedCompanyBillingProfileCommand comandoDeSucesion(LocalDate effectiveFrom) {
        return comandoDeSucesionDeEmpresa(CompanyBillingProfileMother.COMPANY_ID, effectiveFrom);
    }

    private static SucceedCompanyBillingProfileCommand comandoDeSucesionDeEmpresa(Long companyId,
            LocalDate effectiveFrom) {
        return new SucceedCompanyBillingProfileCommand(PersonKind.LEGAL, TaxIdKind.NIT, "901555444",
                "3", "Inversiones Pet II SAS", null, null, null, null,
                "Carrera 43A # 1-50 torre norte", 900L, "pagos@inversionespet2.com.co",
                TaxRegime.SIMPLE, false, effectiveFrom, companyId);
    }
}
