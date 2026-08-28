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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
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
@DisplayName("UpdateCompanyTaxProfileService")
class UpdateCompanyTaxProfileServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 28, 17, 5, 40);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CompanyTaxProfileRepository repository;
    @Mock
    private EconomicActivityQueryPort economicActivityQueryPort;

    private UpdateCompanyTaxProfileService service;

    @Captor
    private ArgumentCaptor<CompanyTaxProfile> profileCaptor;
    @Captor
    private ArgumentCaptor<CompanyTaxProfile> cerradoCaptor;

    @BeforeEach
    void servicio() {
        service = new UpdateCompanyTaxProfileService(repository, economicActivityQueryPort, RELOJ);
    }

    private void elPerfilExiste() {
        when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
        when(repository.close(any())).thenReturn(1);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Lo unico que se INSERTA es el sucesor: la vigente se cierra con
     * {@code close}, que escribe una sola columna. Comprobar las dos cosas por
     * separado es lo que distingue una sucesion de la reescritura en sitio que este
     * servicio dejo de hacer.
     */
    private CompanyTaxProfile sucesorGuardado() {
        verify(repository).close(cerradoCaptor.capture());
        assertThat(cerradoCaptor.getValue().isCurrent()).as("la vigente queda cerrada").isFalse();
        verify(repository).save(profileCaptor.capture());
        return profileCaptor.getValue();
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

            CompanyTaxProfile guardado = sucesorGuardado();
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

            CompanyTaxProfile guardado = sucesorGuardado();
            assertThat(guardado.getTaxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(guardado.getEconomicActivity()).isEqualTo(CompanyTaxProfileMother.COMERCIO);
            assertThat(guardado.getResponsibilities())
                    .containsExactly(new CompanyTaxProfileResponsibility("O-15"));
        }

        @Test
        @DisplayName("persona natural: sin DV y sin consultar el puerto de actividad economica")
        void persona_natural_sin_dv_ni_actividad() {
            elPerfilExiste();

            service.execute(CompanyTaxProfileMother.comandoActualizarCedula());

            assertThat(sucesorGuardado().getCompanyDocumentVerificationDigit()).isNull();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("sin actividad economica ni responsabilidades: no consulta el puerto y deja lista vacia")
        void sin_actividad_ni_responsabilidades() {
            elPerfilExiste();

            service.execute(CompanyTaxProfileMother.comandoActualizarSinActividad());

            CompanyTaxProfile sucesor = sucesorGuardado();
            assertThat(sucesor.getEconomicActivity()).isNull();
            assertThat(sucesor.getResponsibilities()).isEmpty();
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("cierra la vigente y abre la sucesora el mismo dia del reloj inyectado")
        void cierra_la_vigente_y_abre_la_sucesora_el_dia_del_reloj() {
            elPerfilExiste();
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.COMERCIO));

            service.execute(CompanyTaxProfileMother.comandoActualizar());

            LocalDate hoy = AHORA.toLocalDate();
            verify(repository).close(cerradoCaptor.capture());
            assertThat(cerradoCaptor.getValue().getValidTo()).as("la vigente se cierra hoy")
                    .isEqualTo(hoy);
            verify(repository).save(profileCaptor.capture());
            CompanyTaxProfile sucesora = profileCaptor.getValue();
            assertThat(sucesora.getValidFrom()).as("la sucesora rige desde hoy").isEqualTo(hoy);
            assertThat(sucesora.isCurrent()).isTrue();
            assertThat(sucesora.getId()).as("la sucesora es fila nueva").isNull();
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
            when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
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
            when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
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
        @DisplayName("si el cierre no afecta ninguna fila no se inserta la sucesora")
        void si_el_cierre_no_afecta_ninguna_fila_no_se_inserta_la_sucesora() {
            // Cero filas = otra sucesion gano la carrera. Seguir dejaria DOS fichas
            // vigentes y quien lo parara seria uq_company_tax_profiles_current.
            when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));
            when(repository.close(any())).thenReturn(0);
            when(economicActivityQueryPort.findById(CompanyTaxProfileMother.COMERCIO.id()))
                    .thenReturn(Optional.of(CompanyTaxProfileMother.COMERCIO));

            assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.comandoActualizar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("otra sucesion se adelanto");

            verify(repository, never()).save(any());
        }

        /**
         * <b>Comportamiento visible NUEVO, y por eso se sujeta con una prueba.</b> Un
         * PUT del perfil fiscal <em>dos veces el mismo dia</em> responde 400 desde el
         * changeset 364: la ficha vigente rige desde hoy y
         * {@code chk_company_tax_profiles_validity} exige {@code valid_to > valid_from}
         * estricto, asi que la sucesion intradia no es representable.
         *
         * <p>
         * <b>No se adelanta al dia siguiente por cuenta propia</b>, y eso es la
         * decision, no un descuido: esa fecha es la que decide con que identidad fiscal
         * se emitio un documento del intervalo, e inventarla seria elegir en silencio
         * entre dos respuestas defendibles. El 400 lo produce el
         * {@code GlobalExceptionHandler}, que ya traduce
         * {@code IllegalArgumentException} a {@code INVALID_INPUT}.
         */
        @Test
        @DisplayName("cambiar el perfil dos veces el mismo dia se rechaza en vez de adelantar la"
                + " fecha, y no escribe nada")
        void cambiar_el_perfil_dos_veces_el_mismo_dia_se_rechaza() {
            when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(
                    Optional.of(CompanyTaxProfileMother.perfilVigenteDesde(AHORA.toLocalDate())));

            assertThatThrownBy(
                    () -> service.execute(CompanyTaxProfileMother.comandoActualizarSinActividad()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede empezar antes del 2026-03-29");

            // Ni cierra ni inserta: la ficha vigente sigue siendo la que era. Es la
            // mitad del valor del caso -un rechazo que hubiera cerrado la vigente
            // dejaria a la empresa sin identidad fiscal para facturar-.
            verify(repository, never()).close(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(economicActivityQueryPort);
        }

        @Test
        @DisplayName("un NIT invalido interrumpe la actualizacion antes de persistir")
        void un_nit_invalido_interrumpe_la_actualizacion() {
            when(repository.findCurrentByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
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
