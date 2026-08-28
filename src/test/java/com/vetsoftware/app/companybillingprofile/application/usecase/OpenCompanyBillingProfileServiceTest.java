package com.vetsoftware.app.companybillingprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companybillingprofile.application.command.OpenCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.companybillingprofile.application.port.out.CityQueryPort;
import com.vetsoftware.app.companybillingprofile.application.port.out.CompanyBillingProfileRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfileAlreadyOpenException;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenCompanyBillingProfileService — abrir la primera ficha")
class OpenCompanyBillingProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 12, 9, 30, 15);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CompanyBillingProfileRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;

    private OpenCompanyBillingProfileService service;

    @BeforeEach
    void servicio() {
        service = new OpenCompanyBillingProfileService(repository, cityQueryPort, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la ficha vigente con el municipio resuelto por el puerto")
        void persiste_la_ficha_vigente_con_el_municipio_resuelto() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(900L))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSociedad());

            ArgumentCaptor<CompanyBillingProfile> guardada = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(ficha -> {
                assertThat(ficha.getCity()).isEqualTo(CompanyBillingProfileMother.MEDELLIN);
                assertThat(ficha.getValidTo()).as("nace vigente").isNull();
                assertThat(ficha.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
                assertThat(ficha.getLegalName())
                        .isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
            });
        }

        @Test
        @DisplayName("sella la creacion con el reloj inyectado y no con la hora del sistema")
        void sella_la_creacion_con_el_reloj_inyectado() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(900L))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeSociedad());

            ArgumentCaptor<CompanyBillingProfile> guardada = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("devuelve el DTO de lo guardado, con el municipio dentro")
        void devuelve_el_dto_de_lo_guardado() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(900L))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));
            when(repository.save(any())).thenReturn(CompanyBillingProfileMother.persistida(42L));

            CompanyBillingProfileDto dto = service.execute(comandoDeSociedad());

            assertThat(dto.id()).isEqualTo(42L);
            assertThat(dto.city().name()).isEqualTo("Medellin");
            assertThat(dto.validTo()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una empresa que ya tiene ficha vigente no puede abrir otra, y no se toca el catalogo")
        void una_empresa_con_ficha_vigente_no_puede_abrir_otra() {
            // El caso real es el boton pulsado dos veces. Se contesta un conflicto con
            // la sucesion como salida, no un 500 con el Duplicate entry de
            // uq_company_billing_profiles_current.
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.persistida(7L)));

            assertThatThrownBy(() -> service.execute(comandoDeSociedad()))
                    .isInstanceOf(CompanyBillingProfileAlreadyOpenException.class)
                    .hasMessageContaining("already has a current billing profile");

            verify(repository, never()).save(any());
            verifyNoInteractions(cityQueryPort);
        }

        @Test
        @DisplayName("un municipio inexistente no escribe nada y nombra el id en el mensaje")
        void un_municipio_inexistente_no_escribe_nada() {
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConMunicipio(77L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: 77");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un cuerpo que pasa el binder pero viola el CHECK de nombres muere en el dominio, sin escribir")
        void un_cuerpo_que_viola_el_check_de_nombres_no_escribe() {
            // El binder no sabe que LEGAL exige razon social: esa regla es del agregado.
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(900L))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));

            assertThatThrownBy(() -> service.execute(comandoSinRazonSocial()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalName is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la ficha se abre para la empresa del command, no para ninguna otra")
        void la_ficha_se_abre_para_la_empresa_del_command() {
            // El companyId del command lo inyecta el controller desde el principal: aqui
            // se congela que el service lo traslada tal cual al agregado y no lo deriva
            // de ningun otro dato.
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(cityQueryPort.findById(900L))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.MEDELLIN));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDeEmpresa(CompanyBillingProfileMother.OTRA_COMPANY_ID));

            ArgumentCaptor<CompanyBillingProfile> guardada = ArgumentCaptor
                    .forClass(CompanyBillingProfile.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCompanyId())
                    .isEqualTo(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("la existencia de ficha vigente se consulta acotada por la empresa del command")
        void la_existencia_se_consulta_acotada_por_empresa() {
            // Sin el companyId en la consulta, la ficha vigente de otra clinica bloquearia
            // el alta de esta.
            when(repository.findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID))
                    .thenReturn(Optional.of(CompanyBillingProfileMother.persistida(7L)));

            assertThatThrownBy(() -> service
                    .execute(comandoDeEmpresa(CompanyBillingProfileMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(CompanyBillingProfileAlreadyOpenException.class);

            verify(repository).findCurrentByCompanyId(CompanyBillingProfileMother.OTRA_COMPANY_ID);
        }
    }

    private static OpenCompanyBillingProfileCommand comandoDeSociedad() {
        return comandoDeEmpresa(CompanyBillingProfileMother.COMPANY_ID);
    }

    private static OpenCompanyBillingProfileCommand comandoDeEmpresa(Long companyId) {
        return new OpenCompanyBillingProfileCommand(PersonKind.LEGAL, TaxIdKind.NIT,
                CompanyBillingProfileMother.NIT, CompanyBillingProfileMother.DIGITO_VERIFICACION,
                CompanyBillingProfileMother.RAZON_SOCIAL, null, null, null, null,
                CompanyBillingProfileMother.DIRECCION, 900L, CompanyBillingProfileMother.CORREO,
                TaxRegime.COMMON, true, CompanyBillingProfileMother.RIGE_DESDE, companyId);
    }

    private static OpenCompanyBillingProfileCommand comandoConMunicipio(Long cityId) {
        return new OpenCompanyBillingProfileCommand(PersonKind.LEGAL, TaxIdKind.NIT,
                CompanyBillingProfileMother.NIT, CompanyBillingProfileMother.DIGITO_VERIFICACION,
                CompanyBillingProfileMother.RAZON_SOCIAL, null, null, null, null,
                CompanyBillingProfileMother.DIRECCION, cityId, CompanyBillingProfileMother.CORREO,
                TaxRegime.COMMON, true, CompanyBillingProfileMother.RIGE_DESDE,
                CompanyBillingProfileMother.COMPANY_ID);
    }

    private static OpenCompanyBillingProfileCommand comandoSinRazonSocial() {
        return new OpenCompanyBillingProfileCommand(PersonKind.LEGAL, TaxIdKind.NIT,
                CompanyBillingProfileMother.NIT, null, null, null, null, null, null,
                CompanyBillingProfileMother.DIRECCION, 900L, CompanyBillingProfileMother.CORREO,
                TaxRegime.COMMON, true, CompanyBillingProfileMother.RIGE_DESDE,
                CompanyBillingProfileMother.COMPANY_ID);
    }
}
