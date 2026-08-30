package com.vetsoftware.app.legaldocumentversion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersionNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El aviso vigente para quien todavia no tiene cuenta.
 *
 * <p>
 * <b>La firma es la prueba.</b> El puerto que habia recibia un
 * {@code companyId} que el servicio <b>no usaba para nada</b>: existia solo
 * para alimentar el SpEL de su {@code @PreAuthorize}. Ese parametro es lo que
 * hacia inalcanzable la ruta para un prospecto, y con el aviso llegando de una
 * copia local del bundle del front, el {@code privacy_notice_version_id} que se
 * guarda al lado de cada propuesta apuntaba a una version que nadie le sirvio
 * nunca desde aqui. Aqui no hay {@code companyId} que pasar, asi que no queda
 * nada que relajar despues.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindPublicLegalDocumentService — el aviso vigente, sin identidad")
class FindPublicLegalDocumentServiceTest {

    private static final String TEXTO = "Aviso de privacidad, version 3.";

    @Mock
    private LegalDocumentVersionRepository repository;

    @InjectMocks
    private FindPublicLegalDocumentService service;

    private static LegalDocumentVersion vigente() {
        return new LegalDocumentVersion(7_501L, "PRIVACY_NOTICE", 3,
                LegalDocumentKind.PRIVACY_NOTICE, "Aviso de privacidad", TEXTO,
                LegalDocumentVersion.hashOf(TEXTO), LocalDateTime.of(2026, 8, 1, 9, 0), 4L,
                LocalDate.of(2026, 8, 15), null, LocalDateTime.of(2026, 8, 1, 9, 0), 0L);
    }

    @Test
    @DisplayName("devuelve el texto y la huella, que es lo que la casilla tiene que guardar")
    void devuelve_el_texto_y_la_huella() {
        when(repository.findCurrentByCode("PRIVACY_NOTICE")).thenReturn(Optional.of(vigente()));

        var dto = service.findCurrentByCode("PRIVACY_NOTICE");

        assertThat(dto.content()).isEqualTo(TEXTO);
        assertThat(dto.contentHash()).isEqualTo(LegalDocumentVersion.hashOf(TEXTO));
        assertThat(dto.documentVersion())
                .as("el par (code, documentVersion) es lo que viaja de vuelta en la aceptacion")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("un codigo sin version vigente es 404, no una respuesta vacia")
    void un_codigo_sin_vigente_es_404() {
        when(repository.findCurrentByCode("NO_EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCurrentByCode("NO_EXISTE"))
                .isInstanceOf(LegalDocumentVersionNotFoundException.class);
    }
}
