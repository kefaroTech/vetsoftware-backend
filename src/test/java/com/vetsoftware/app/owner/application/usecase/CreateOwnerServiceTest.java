package com.vetsoftware.app.owner.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOwnerService")
class CreateOwnerServiceTest {

    @Mock
    private OwnerRepository repository;
    @Mock
    private CityQueryPort cityQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    private CreateOwnerService service;

    private void construirService() {
        service = new CreateOwnerService(repository, cityQueryPort, companyQueryPort);
    }

    @Nested
    @DisplayName("creacion valida")
    class CreacionValida {

        @Test
        @DisplayName("resuelve ciudad y compania y guarda el owner con los datos fiscales explicitos")
        void resuelve_ciudad_y_compania_y_guarda_el_owner() {
            construirService();
            when(cityQueryPort.findById(OwnerMother.CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.BOGOTA));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OwnerDto dto = service.execute(OwnerMother.comandoCrear(TaxRegime.RESPONSABLE_IVA,
                    FiscalResponsibility.GRAN_CONTRIBUYENTE));

            ArgumentCaptor<Owner> capturado = ArgumentCaptor.forClass(Owner.class);
            verify(repository).save(capturado.capture());
            Owner guardado = capturado.getValue();
            assertThat(guardado.getName()).isEqualTo("Ana Ruiz");
            assertThat(guardado.getCity()).isEqualTo(OwnerMother.BOGOTA);
            assertThat(guardado.getCompany()).isEqualTo(OwnerMother.CLINICA);
            assertThat(guardado.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(guardado.getFiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.GRAN_CONTRIBUYENTE);
            assertThat(dto.name()).isEqualTo("Ana Ruiz");
        }

        @Test
        @DisplayName("sin taxRegime explicito, lo infiere con TaxRegime.defaultFor")
        void sin_tax_regime_explicito_lo_infiere() {
            construirService();
            when(cityQueryPort.findById(OwnerMother.CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.BOGOTA));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            CreateOwnerCommand comando = new CreateOwnerCommand("Veterinaria Sur",
                    "contacto@sur.com", "900123456", OwnerDocumentType.NIT, PersonType.JURIDICA,
                    "7", "Veterinaria Sur S.A.S.", "Avenida 3 # 40-50", "6041234567",
                    OwnerMother.CITY_ID, OwnerMother.COMPANY_ID, true, null,
                    FiscalResponsibility.NO_APLICA);

            service.execute(comando);

            ArgumentCaptor<Owner> capturado = ArgumentCaptor.forClass(Owner.class);
            verify(repository).save(capturado.capture());
            // JURIDICA -> RESPONSABLE_IVA por TaxRegime.defaultFor.
            assertThat(capturado.getValue().getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        }

        @Test
        @DisplayName("sin fiscalResponsibility explicita, usa NO_APLICA por defecto")
        void sin_fiscal_responsibility_explicita_usa_no_aplica() {
            construirService();
            when(cityQueryPort.findById(OwnerMother.CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.BOGOTA));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID))
                    .thenReturn(Optional.of(OwnerMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            CreateOwnerCommand comando = new CreateOwnerCommand("Ana Ruiz", "ana@vet.com",
                    "1020304050", OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null,
                    null, "Calle 1 # 2-3", "3001112233", OwnerMother.CITY_ID,
                    OwnerMother.COMPANY_ID, false, TaxRegime.NO_RESPONSABLE_IVA, null);

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
            when(cityQueryPort.findById(OwnerMother.CITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OwnerMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City not found: " + OwnerMother.CITY_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("compania inexistente falla y no guarda")
        void compania_inexistente_falla_y_no_guarda() {
            construirService();
            when(cityQueryPort.findById(OwnerMother.CITY_ID))
                    .thenReturn(Optional.of(OwnerMother.BOGOTA));
            when(companyQueryPort.findById(OwnerMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OwnerMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + OwnerMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }
}
