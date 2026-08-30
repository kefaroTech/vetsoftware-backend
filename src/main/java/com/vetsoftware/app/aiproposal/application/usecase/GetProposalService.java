package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.GetProposalUseCase;
import org.springframework.stereotype.Service;

/**
 * La relectura por token. Es lo que abre el enlace del correo, y lo que el
 * front llama tras un 409 para reintentar con la version buena.
 *
 * <p>
 * <strong>Sin {@code @Transactional} y sin escribir nada</strong>: ni siquiera
 * mueve {@code last_activity_at}. Tocar la fila en cada lectura convertiria un
 * {@code GET} anonimo y cacheable en una escritura, y le daria a cualquiera con
 * el token la forma de mantener viva una propuesta indefinidamente contra la
 * politica de retencion.
 */
@Service
public class GetProposalService implements GetProposalUseCase {

    private final ProposalReader reader;

    public GetProposalService(ProposalReader reader) {
        this.reader = reader;
    }

    @Override
    public ProposalViewDto get(String publicToken) {
        return reader.vista(reader.exigir(publicToken), false);
    }
}
