package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.aiproposal.application.port.in.MarkProposalConvertedUseCase;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.registration.application.port.out.ProposalConverter;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Marca la propuesta como convertida, escalando a plataforma.
 *
 * <p>
 * <strong>Bajo {@link SystemAuthRunner} por lo mismo que los otros nueve
 * adaptadores del alta</strong>: el registro es un flujo publico sin token, en
 * ese instante no hay principal, y {@code MarkProposalConvertedUseCase} exige
 * {@code hasRole('SYSTEM')}. Sin el envoltorio el alta entera moriria con un
 * <b>403</b> que ni siquiera mencionaria a las propuestas — exactamente el
 * defecto que {@code PlatformCatalogSubscriptionCreator} lleva documentado.
 *
 * <p>
 * {@link SystemAuthRunner} solo intercambia el {@code SecurityContext} y lo
 * restaura en un {@code finally}: no toca la propagacion transaccional, asi que
 * esta escritura sigue dentro de la transaccion del alta.
 */
@Component
public class AiProposalConverterAdapter implements ProposalConverter {

    private final MarkProposalConvertedUseCase markProposalConvertedUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public AiProposalConverterAdapter(MarkProposalConvertedUseCase markProposalConvertedUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.markProposalConvertedUseCase = markProposalConvertedUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public Optional<Long> markConverted(String publicToken) {
        return systemAuthRunner.call(() -> markProposalConvertedUseCase.execute(publicToken));
    }
}
