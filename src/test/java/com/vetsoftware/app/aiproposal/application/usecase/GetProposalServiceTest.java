package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.AiProposalNotFoundException;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La relectura por token: lo que abre el enlace del correo.
 *
 * <p>
 * <b>Lo que de verdad hay que probar aqui no es lo que devuelve, sino lo que no
 * hace.</b> Un {@code GET} que tocara {@code last_activity_at} le daria a
 * cualquiera con el token la forma de mantener viva una propuesta contra la
 * politica de retencion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProposalService — releer sin escribir")
class GetProposalServiceTest {

    private static final Long TURNO = 71L;

    private final SellableCatalog catalog = SellableCatalogMother.sinPaquetes();

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    private GetProposalService service;

    @BeforeEach
    void montar() {
        service = new GetProposalService(new ProposalReader(repository, catalogQueryPort));
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("devuelve el carrito vigente sin marcarlo como recalculado")
        void devuelve_el_carrito_vigente_sin_recalcular() {
            AiProposal propuesta = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    4L);
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.of(catalog));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA)).thenReturn(
                    List.of(ProposalMother.turnoInicial(TURNO, "una veterinaria de barrio")));
            when(repository.findLinesByTurnId(TURNO)).thenReturn(
                    List.of(ProposalMother.lineaDelModelo(TURNO, "CORE", "69000.00", 0)));

            ProposalViewDto vista = service.get(ProposalMother.TOKEN);

            assertThat(vista.publicToken()).isEqualTo(ProposalMother.TOKEN);
            assertThat(vista.version()).isEqualTo(4L);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
        }

        @Test
        @DisplayName("no escribe nada, ni siquiera mueve la ultima actividad")
        void no_escribe_nada_ni_mueve_la_ultima_actividad() {
            AiProposal propuesta = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    4L);
            LocalDateTime antes = propuesta.getLastActivityAt();
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.of(catalog));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA)).thenReturn(
                    List.of(ProposalMother.turnoInicial(TURNO, "una veterinaria de barrio")));
            when(repository.findLinesByTurnId(TURNO)).thenReturn(
                    List.of(ProposalMother.lineaDelModelo(TURNO, "CORE", "69000.00", 0)));

            service.get(ProposalMother.TOKEN);

            assertThat(propuesta.getLastActivityAt()).isEqualTo(antes);
            verify(repository, never()).save(any());
            verify(repository, never()).saveTurn(any());
            verify(repository, never()).saveLines(any());
        }

        @Test
        @DisplayName("un token desconocido no revela si existio: 404 y nada mas")
        void un_token_desconocido_da_404() {
            when(repository.findByPublicToken("otro")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get("otro"))
                    .isInstanceOf(AiProposalNotFoundException.class)
                    .hasMessageContaining("AI proposal not found");
            verify(repository, never()).save(any());
        }
    }
}
