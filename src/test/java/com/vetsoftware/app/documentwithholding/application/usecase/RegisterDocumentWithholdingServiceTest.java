package com.vetsoftware.app.documentwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.documentwithholding.application.command.RegisterDocumentWithholdingCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.application.port.out.MunicipalityValidationPort;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que este servicio hace y lo que deliberadamente <b>no</b> hace.
 *
 * <p>
 * Hace dos cosas: comprobar que las claves foraneas apuntan a algo que existe y
 * es de la misma empresa, y sellar la fecha de creacion con el reloj inyectado.
 * Todo lo demas —la tarifa, el periodo, el municipio segun el tipo— lo decide
 * el constructor de {@code DocumentWithholding}, y por eso aqui solo se
 * comprueba que el servicio deja que el dominio hable, en el orden correcto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterDocumentWithholdingService — las FK se comprueban, las invariantes no")
class RegisterDocumentWithholdingServiceTest {

    /**
     * Fijo para que {@code createdDate} sea afirmable sin depender del reloj de la
     * maquina. Distinto de {@code practicedOn} a proposito: si el servicio cruzara
     * las dos fechas, la asercion cae.
     */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-07T08:45:00Z"),
            ZoneOffset.UTC);

    @Mock
    private DocumentWithholdingRepository repository;
    @Mock
    private BillingDocumentValidationPort billingDocumentValidationPort;
    @Mock
    private MunicipalityValidationPort municipalityValidationPort;

    private RegisterDocumentWithholdingService service;

    @BeforeEach
    void servicio() {
        service = new RegisterDocumentWithholdingService(repository, billingDocumentValidationPort,
                municipalityValidationPort, RELOJ);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda la retencion sin respaldo y sellada con el reloj inyectado")
        void guarda_la_retencion_sin_respaldo_y_sellada_con_el_reloj() {
            laFacturaExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            DocumentWithholdingDto devuelta = service
                    .execute(DocumentWithholdingMother.comandoDeRenta());

            ArgumentCaptor<DocumentWithholding> guardada = ArgumentCaptor
                    .forClass(DocumentWithholding.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(retencion -> {
                assertThat(retencion.getId()).isNull();
                assertThat(retencion.getCompanyId()).isEqualTo(DocumentWithholdingMother.EMPRESA);
                assertThat(retencion.getBillingDocumentId())
                        .isEqualTo(DocumentWithholdingMother.FACTURA);
                assertThat(retencion.getType()).isEqualTo(WithholdingType.INCOME_TAX);
                assertThat(retencion.getTaxableBase()).isEqualByComparingTo("1234567.89");
                assertThat(retencion.getRatePercent()).isEqualByComparingTo("2.500000");
                assertThat(retencion.getAmount()).isEqualByComparingTo("30864.20");
                assertThat(retencion.getFiscalYear()).isEqualTo(2026);
                assertThat(retencion.getFiscalPeriodKey()).isEqualTo("2026-A");
                assertThat(retencion.getPracticedOn())
                        .isEqualTo(DocumentWithholdingMother.PRACTICADA_EL);
                // La marca de creacion sale del Clock, no de now().
                assertThat(retencion.getCreatedDate())
                        .isEqualTo(LocalDateTime.of(2026, 3, 7, 8, 45, 0));
                // Nace sin certificado: es lo que la deja en la bandeja de reclamacion.
                assertThat(retencion.getCertificateId()).isNull();
            });
            assertThat(devuelta.certificateId()).isNull();
            assertThat(devuelta.ratePercent()).isEqualByComparingTo("2.500000");
        }

        @Test
        @DisplayName("una tarifa por mil llega al repositorio con sus seis decimales intactos")
        void una_tarifa_por_mil_llega_intacta_al_repositorio() {
            laFacturaExiste();
            elMunicipioExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(DocumentWithholdingMother.comandoDeIca());

            ArgumentCaptor<DocumentWithholding> guardada = ArgumentCaptor
                    .forClass(DocumentWithholding.class);
            verify(repository).save(guardada.capture());
            // 6,9 por mil es 0,69 %. Si alguien "normalizara" la tarifa aqui —dividiendo
            // por mil, o redondeando a dos decimales— este caso lo caza.
            assertThat(guardada.getValue().getRatePercent()).isEqualByComparingTo("0.690000");
            assertThat(guardada.getValue().getAmount()).isEqualByComparingTo("8518.52");
            assertThat(guardada.getValue().getMunicipalityCode()).isEqualTo("05001");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una factura de otra empresa aborta antes de mirar nada mas")
        void una_factura_de_otra_empresa_aborta_antes_de_escribir() {
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    DocumentWithholdingMother.FACTURA, DocumentWithholdingMother.EMPRESA))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.execute(DocumentWithholdingMother.comandoDeIca()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Billing document not found: 8400");

            // La FK compuesta lo pararia igual, pero como error de integridad: un 500
            // en la cara del operador en vez de un mensaje que dice que corregir.
            verifyNoInteractions(municipalityValidationPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un municipio que no existe en el catalogo aborta antes de escribir")
        void un_municipio_inexistente_aborta_antes_de_escribir() {
            laFacturaExiste();
            when(municipalityValidationPort.existsByDaneCode("05001")).thenReturn(false);

            assertThatThrownBy(() -> service.execute(DocumentWithholdingMother.comandoDeIca()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Municipality not found: 05001");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un municipio en una retencion de IVA lo rechaza el dominio, sin preguntar")
        void un_municipio_en_una_retencion_de_iva_lo_rechaza_el_dominio() {
            laFacturaExiste();

            // El orden importa: preguntando primero por el municipio, este caso y el
            // anterior saldrian con el mismo error —«municipio no encontrado»— y este
            // mentiria sobre lo que esta mal. Lo que falla aqui es el tipo, no el
            // codigo, y por eso el puerto no se llega a tocar.
            assertThatThrownBy(() -> service.execute(new RegisterDocumentWithholdingCommand(
                    DocumentWithholdingMother.EMPRESA, DocumentWithholdingMother.FACTURA,
                    WithholdingType.VAT, DocumentWithholdingMother.BASE_GRAVABLE,
                    new BigDecimal("15.000000"), DocumentWithholdingMother.RETENIDO_IVA, "05001",
                    2026, "2026-B02", DocumentWithholdingMother.PRACTICADA_EL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("municipalityCode is only allowed for ICA");

            verifyNoInteractions(municipalityValidationPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una retencion sin municipio no consulta el catalogo de geografia")
        void una_retencion_sin_municipio_no_consulta_el_catalogo() {
            laFacturaExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(DocumentWithholdingMother.comandoDeRenta());

            // Dos tercios de las retenciones no son municipales: preguntar por un
            // codigo nulo seria una consulta por peticion que nunca puede acertar.
            verify(municipalityValidationPort, never()).existsByDaneCode(anyString());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la factura se valida con los dos argumentos y en su orden")
        void la_factura_se_valida_con_los_dos_argumentos() {
            laFacturaExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(DocumentWithholdingMother.comandoDeRenta());

            // Acotar solo por el id de la factura no basta: el documento es de alguien.
            // Los dos argumentos exactos, en su orden, o el caso cae.
            ArgumentCaptor<Long> factura = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(billingDocumentValidationPort).existsByIdAndCompanyId(factura.capture(),
                    empresa.capture());
            assertThat(factura.getValue()).isEqualTo(DocumentWithholdingMother.FACTURA);
            assertThat(empresa.getValue()).isEqualTo(DocumentWithholdingMother.EMPRESA);
        }

        @Test
        @DisplayName("la empresa que se guarda es la del command y no otra")
        void la_empresa_que_se_guarda_es_la_del_command() {
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    DocumentWithholdingMother.FACTURA, DocumentWithholdingMother.OTRA_EMPRESA))
                    .thenReturn(true);
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(new RegisterDocumentWithholdingCommand(
                    DocumentWithholdingMother.OTRA_EMPRESA, DocumentWithholdingMother.FACTURA,
                    WithholdingType.INCOME_TAX, DocumentWithholdingMother.BASE_GRAVABLE,
                    DocumentWithholdingMother.TARIFA_RENTA, DocumentWithholdingMother.RETENIDO,
                    null, 2026, "2026-A", DocumentWithholdingMother.PRACTICADA_EL));

            ArgumentCaptor<DocumentWithholding> guardada = ArgumentCaptor
                    .forClass(DocumentWithholding.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCompanyId())
                    .isEqualTo(DocumentWithholdingMother.OTRA_EMPRESA);
        }
    }

    // --- andamio ------------------------------------------------------------

    private void laFacturaExiste() {
        when(billingDocumentValidationPort.existsByIdAndCompanyId(DocumentWithholdingMother.FACTURA,
                DocumentWithholdingMother.EMPRESA)).thenReturn(true);
    }

    private void elMunicipioExiste() {
        when(municipalityValidationPort
                .existsByDaneCode(DocumentWithholdingMother.MUNICIPIO_MEDELLIN)).thenReturn(true);
    }
}
