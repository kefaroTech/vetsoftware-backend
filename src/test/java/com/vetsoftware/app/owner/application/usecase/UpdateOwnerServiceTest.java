package com.vetsoftware.app.owner.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.out.CityQueryPort;
import com.vetsoftware.app.owner.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El escenario de tenant ajeno (owner que no existe en la compania del comando)
 * ya lo cubre {@link OwnerTenantGuardTest}; esta clase se centra en el
 * comportamiento propio del caso de uso: la actualizacion feliz y las dos
 * resoluciones de referencia que pueden fallar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateOwnerService")
class UpdateOwnerServiceTest {

    @Mock
    private OwnerRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private UpdateOwnerService service;

    private void construirService() {
        service = new UpdateOwnerService(repository, cityQueryPort, companyQueryPort);
    }

    private void ownerExiste() {
        when(repository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                .thenReturn(Optional.of(OwnerMother.personaNatural()));
    }

    @Nested
    @DisplayName("actualizacion valida")
    class ActualizacionValida {

        @Test
        @DisplayName("reemplaza los datos y guarda el owner actualizado")
        void reemplaza_los_datos_y_guarda_el_owner_actualizado() {
            construirService();
            ownerExiste();
            when(cityQueryPort.findById(OwnerMother.OTRA_CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.CALI));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OwnerDto dto = service.execute(OwnerMother.comandoActualizar(TaxRegime.RESPONSABLE_IVA,
                    FiscalResponsibility.AUTORRETENEDOR));

            ArgumentCaptor<Owner> capturado = ArgumentCaptor.forClass(Owner.class);
            verify(repository).save(capturado.capture());
            Owner guardado = capturado.getValue();
            assertThat(guardado.getName()).isEqualTo("Ana Maria Ruiz");
            assertThat(guardado.getCity()).isEqualTo(OwnerMother.CALI);
            assertThat(guardado.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(dto.name()).isEqualTo("Ana Maria Ruiz");
        }

        @Test
        @DisplayName("sin taxRegime explicito, lo infiere con TaxRegime.defaultFor")
        void sin_tax_regime_explicito_lo_infiere() {
            construirService();
            ownerExiste();
            when(cityQueryPort.findById(OwnerMother.OTRA_CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.CALI));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateOwnerCommand comando = new UpdateOwnerCommand(OwnerMother.OWNER_ID,
                    "Ana Maria Ruiz", "anamaria@vet.com", "9988776655",
                    OwnerDocumentType.CEDULA_EXTRANJERIA, PersonType.NATURAL, null, null,
                    "Carrera 9 # 8-7", "3005556677", OwnerMother.OTRA_CITY_ID,
                    OwnerMother.COMPANY_ID, true, null, FiscalResponsibility.NO_APLICA);

            service.execute(comando);

            ArgumentCaptor<Owner> capturado = ArgumentCaptor.forClass(Owner.class);
            verify(repository).save(capturado.capture());
            // CEDULA_EXTRANJERIA + NATURAL -> NO_RESPONSABLE_IVA por TaxRegime.defaultFor.
            assertThat(capturado.getValue().getTaxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
        }

        @Test
        @DisplayName("sin fiscalResponsibility explicita, usa NO_APLICA por defecto")
        void sin_fiscal_responsibility_explicita_usa_no_aplica() {
            construirService();
            ownerExiste();
            when(cityQueryPort.findById(OwnerMother.OTRA_CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.CALI));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateOwnerCommand comando = new UpdateOwnerCommand(OwnerMother.OWNER_ID,
                    "Ana Maria Ruiz", "anamaria@vet.com", "9988776655",
                    OwnerDocumentType.CEDULA_EXTRANJERIA, PersonType.NATURAL, null, null,
                    "Carrera 9 # 8-7", "3005556677", OwnerMother.OTRA_CITY_ID,
                    OwnerMother.COMPANY_ID, true, TaxRegime.NO_RESPONSABLE_IVA, null);

            service.execute(comando);

            ArgumentCaptor<Owner> capturado = ArgumentCaptor.forClass(Owner.class);
            verify(repository).save(capturado.capture());
            assertThat(capturado.getValue().getFiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.NO_APLICA);
        }
    }

    @Nested
    @DisplayName("resolucion de referencias fallida")
    class ResolucionFallida {

        @Test
        @DisplayName("ciudad inexistente falla antes de consultar la compania y sin guardar")
        void ciudad_inexistente_falla_antes_de_consultar_la_compania() {
            construirService();
            ownerExiste();
            when(cityQueryPort.findById(OwnerMother.OTRA_CITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OwnerMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: " + OwnerMother.OTRA_CITY_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("compania inexistente falla y no guarda")
        void compania_inexistente_falla_y_no_guarda() {
            construirService();
            ownerExiste();
            when(cityQueryPort.findById(OwnerMother.OTRA_CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.CALI));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OwnerMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + OwnerMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }
}
