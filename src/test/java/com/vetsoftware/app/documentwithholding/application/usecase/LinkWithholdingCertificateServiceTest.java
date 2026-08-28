package com.vetsoftware.app.documentwithholding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.documentwithholding.application.command.LinkWithholdingCertificateCommand;
import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.application.port.out.WithholdingCertificateValidationPort;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholdingNotFoundException;
import com.vetsoftware.app.documentwithholding.domain.WithholdingAlreadyCertifiedException;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
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
 * La segunda y ultima escritura que la ficha admite: apuntar la retencion a su
 * certificado.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es que la carga va acotada por
 * empresa aunque el puerto este cerrado a plataforma.</b> Son dos cosas
 * distintas y se confunden con facilidad: el {@code @PreAuthorize} decide
 * <em>quien</em> llama, el {@code companyId} del command decide <em>sobre que
 * fila</em> se escribe. Sin lo segundo, un id equivocado de tesoreria
 * certificaria la retencion de otra clinica — y esa fila ya no se puede
 * corregir editandola, porque la tabla solo se agrega.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LinkWithholdingCertificateService — el papel que vuelve descontable la retencion")
class LinkWithholdingCertificateServiceTest {

    private static final Long RETENCION = 41L;

    @Mock
    private DocumentWithholdingRepository repository;
    @Mock
    private WithholdingCertificateValidationPort certificateValidationPort;

    private LinkWithholdingCertificateService service;

    @BeforeEach
    void servicio() {
        service = new LinkWithholdingCertificateService(repository, certificateValidationPort);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("apunta la retencion al certificado y conserva su version")
        void apunta_la_retencion_al_certificado_y_conserva_su_version() {
            DocumentWithholding sinRespaldo = DocumentWithholdingMother.yaRegistrada(RETENCION);
            laRetencionExiste(sinRespaldo);
            elCertificadoExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            DocumentWithholdingDto devuelta = service
                    .execute(comando(DocumentWithholdingMother.CERTIFICADO));

            ArgumentCaptor<DocumentWithholding> guardada = ArgumentCaptor
                    .forClass(DocumentWithholding.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue()).satisfies(retencion -> {
                assertThat(retencion.getId()).isEqualTo(RETENCION);
                assertThat(retencion.getCertificateId())
                        .isEqualTo(DocumentWithholdingMother.CERTIFICADO);
                assertThat(retencion.isUncertified()).isFalse();
                // La version viaja intacta o el UPDATE no puede detectar que otro
                // operario certifico esta misma fila desde la misma bandeja.
                assertThat(retencion.getVersion()).isEqualTo(sinRespaldo.getVersion());
            });
            assertThat(devuelta.certificateId()).isEqualTo(DocumentWithholdingMother.CERTIFICADO);
        }

        @Test
        @DisplayName("repetir el mismo certificado no falla y sale por el mismo camino")
        void repetir_el_mismo_certificado_no_falla() {
            laRetencionExiste(DocumentWithholdingMother.yaCertificada(RETENCION,
                    DocumentWithholdingMother.CERTIFICADO));
            elCertificadoExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            // El reintento del operador no se castiga: el resultado que pedia ya es el
            // que hay.
            assertThat(
                    service.execute(comando(DocumentWithholdingMother.CERTIFICADO)).certificateId())
                    .isEqualTo(DocumentWithholdingMother.CERTIFICADO);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una retencion inexistente sale como no encontrada y no consulta el certificado")
        void una_retencion_inexistente_no_consulta_el_certificado() {
            when(repository.findByIdAndCompanyId(RETENCION, DocumentWithholdingMother.EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(comando(DocumentWithholdingMother.CERTIFICADO)))
                    .isInstanceOf(DocumentWithholdingNotFoundException.class)
                    .hasMessageContaining("Document withholding not found: 41");

            verifyNoInteractions(certificateValidationPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un certificado de otra empresa aborta antes de escribir")
        void un_certificado_de_otra_empresa_aborta_antes_de_escribir() {
            laRetencionExiste(DocumentWithholdingMother.yaRegistrada(RETENCION));
            when(certificateValidationPort.existsByIdAndCompanyId(
                    DocumentWithholdingMother.CERTIFICADO, DocumentWithholdingMother.EMPRESA))
                    .thenReturn(false);

            // La FK compuesta (company_id, certificate_id) lo pararia igual, pero al
            // hacer flush y como error de integridad: un 500 sin explicacion.
            assertThatThrownBy(
                    () -> service.execute(comando(DocumentWithholdingMother.CERTIFICADO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Withholding certificate not found: 8410");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("repuntarla a otro certificado es un conflicto y no escribe")
        void repuntarla_a_otro_certificado_es_un_conflicto() {
            laRetencionExiste(DocumentWithholdingMother.yaCertificada(RETENCION,
                    DocumentWithholdingMother.CERTIFICADO));
            when(certificateValidationPort.existsByIdAndCompanyId(8411L,
                    DocumentWithholdingMother.EMPRESA)).thenReturn(true);

            // La regla vive en el dominio y no en este metodo: es una invariante de la
            // retencion, no un paso del caso de uso.
            assertThatThrownBy(() -> service.execute(comando(8411L)))
                    .isInstanceOf(WithholdingAlreadyCertifiedException.class)
                    .hasMessageContaining("already backed by certificate 8410");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("carga la retencion con la empresa del command, nunca solo por id")
        void carga_la_retencion_con_la_empresa_del_command() {
            laRetencionExiste(DocumentWithholdingMother.yaRegistrada(RETENCION));
            elCertificadoExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DocumentWithholdingMother.CERTIFICADO));

            // El puerto de salida no ofrece siquiera la variante ancha, pero este
            // verify congela que la que se usa lleva los dos argumentos en su orden.
            ArgumentCaptor<Long> id = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(repository).findByIdAndCompanyId(id.capture(), empresa.capture());
            assertThat(id.getValue()).isEqualTo(RETENCION);
            assertThat(empresa.getValue()).isEqualTo(DocumentWithholdingMother.EMPRESA);
        }

        @Test
        @DisplayName("la retencion de otra empresa no se encuentra, en vez de prohibirse")
        void la_retencion_de_otra_empresa_no_se_encuentra() {
            when(repository.findByIdAndCompanyId(RETENCION, DocumentWithholdingMother.OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            // Un 403 confirmaria que la fila existe, y con ids consecutivos eso es un
            // censo de las retenciones de la competencia.
            assertThatThrownBy(
                    () -> service.execute(new LinkWithholdingCertificateCommand(RETENCION,
                            DocumentWithholdingMother.OTRA_EMPRESA,
                            DocumentWithholdingMother.CERTIFICADO)))
                    .isInstanceOf(DocumentWithholdingNotFoundException.class);
        }

        @Test
        @DisplayName("el certificado se valida con la misma empresa que cargo la retencion")
        void el_certificado_se_valida_con_la_misma_empresa() {
            laRetencionExiste(DocumentWithholdingMother.yaRegistrada(RETENCION));
            elCertificadoExiste();
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            service.execute(comando(DocumentWithholdingMother.CERTIFICADO));

            ArgumentCaptor<Long> certificado = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> empresa = ArgumentCaptor.forClass(Long.class);
            verify(certificateValidationPort).existsByIdAndCompanyId(certificado.capture(),
                    empresa.capture());
            assertThat(certificado.getValue()).isEqualTo(DocumentWithholdingMother.CERTIFICADO);
            assertThat(empresa.getValue()).isEqualTo(DocumentWithholdingMother.EMPRESA);
        }
    }

    // --- andamio ------------------------------------------------------------

    private static LinkWithholdingCertificateCommand comando(Long certificateId) {
        return new LinkWithholdingCertificateCommand(RETENCION, DocumentWithholdingMother.EMPRESA,
                certificateId);
    }

    private void laRetencionExiste(DocumentWithholding retencion) {
        when(repository.findByIdAndCompanyId(RETENCION, DocumentWithholdingMother.EMPRESA))
                .thenReturn(Optional.of(retencion));
    }

    private void elCertificadoExiste() {
        when(certificateValidationPort.existsByIdAndCompanyId(DocumentWithholdingMother.CERTIFICADO,
                DocumentWithholdingMother.EMPRESA)).thenReturn(true);
    }
}
