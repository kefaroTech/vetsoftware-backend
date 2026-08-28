package com.vetsoftware.app.legaldocumentversion.application.usecase;

import com.vetsoftware.app.legaldocumentversion.application.command.PublishLegalDocumentVersionCommand;
import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.application.port.in.PublishLegalDocumentVersionUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentContentAlreadyPublishedException;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicar es un acto de dos mitades y por eso es {@code @Transactional}: se
 * cierra la vigencia de la version que habia y se abre la de la nueva. Si la
 * segunda falla y la primera hubiera quedado escrita, el documento se quedaria
 * <em>sin</em> version vigente y el flujo de aceptacion no tendria que mostrar.
 *
 * <p>
 * <strong>El cierre de la vigente se vacia antes de insertar la nueva</strong>
 * ({@code repository.supersede}, no {@code save}). Hibernate emite los INSERT
 * antes que los UPDATE, asi que dejar las dos escrituras al flush de la
 * transaccion mete la version nueva mientras la vieja sigue vigente y
 * {@code uq_ldv_current} aborta la publicacion entera. Lo destapo la rodaja de
 * persistencia con un «Duplicate entry ... for key uq_ldv_current»; ningun test
 * con el repositorio mockeado podia verlo, porque el orden del vaciado no
 * existe fuera de la base.
 *
 * <p>
 * El instante es uno solo —{@code LocalDateTime.now(clock)} leido una vez— para
 * que el {@code superseded_at} de la vieja y el {@code published_at} de la
 * nueva coincidan al microsegundo: con dos lecturas del reloj queda un hueco en
 * el que ninguna version rigio, y {@code chk_ldv_supersede} no lo detecta.
 */
@Observed(name = "legaldocument.publish")
@Service
public class PublishLegalDocumentVersionService implements PublishLegalDocumentVersionUseCase {

    private final LegalDocumentVersionRepository repository;
    private final Clock clock;

    public PublishLegalDocumentVersionService(LegalDocumentVersionRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LegalDocumentVersionDto execute(PublishLegalDocumentVersionCommand command) {
        String huella = LegalDocumentVersion.hashOf(command.content());
        if (repository.existsByCodeAndContentHash(command.code(), huella)) {
            throw new LegalDocumentContentAlreadyPublishedException(command.code(), huella);
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        repository.findCurrentByCode(command.code()).ifPresent(vigente -> {
            vigente.supersede(ahora);
            repository.supersede(vigente);
        });
        int siguiente = repository.findLastDocumentVersion(command.code()).orElse(0) + 1;
        LegalDocumentVersion nueva = LegalDocumentVersion.publish(command.code(), siguiente,
                command.kind(), command.title(), command.content(),
                command.publishedBySystemUserId(), command.effectiveFrom(), ahora, ahora);
        return LegalDocumentVersionDto.from(repository.save(nueva));
    }
}
