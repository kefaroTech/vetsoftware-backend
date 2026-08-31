package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.command.ReviseCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ReviseCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintTextAlreadyPublishedException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige una pista: cierra la vigente y publica la siguiente.
 *
 * <p>
 * <strong>Es un acto de dos mitades y por eso es
 * {@code @Transactional}</strong>: si la segunda falla y la primera hubiera
 * quedado escrita, el articulo se quedaria <em>sin</em> pista vigente y el
 * modelo dejaria de proponerlo sin que nadie lo hubiera decidido —el efecto de
 * «retirar», provocado por un error—.
 *
 * <p>
 * <strong>El cierre de la vigente se vacia antes de insertar la nueva</strong>
 * ({@code repository.supersede}, no {@code save}). Hibernate emite los INSERT
 * antes que los UPDATE, asi que dejar las dos escrituras al flush de la
 * transaccion mete la revision nueva mientras la vieja sigue vigente y
 * {@code uq_catalog_item_ai_hints_current} aborta la correccion entera. Ver el
 * contrato del puerto.
 *
 * <p>
 * <strong>El mismo actor firma las dos mitades, y no es redundante.</strong>
 * Quien corrige cierra la anterior ({@code superseded_by_system_user_id},
 * changeset 393) y publica la siguiente ({@code published_by_system_user_id}),
 * asi que el historial deja leer por separado <em>quien escribio cada
 * texto</em> y <em>quien decidio que cada uno dejara de regir</em>. Al corregir
 * coinciden; al retirar ({@code RetireCatalogItemAiHintService}) no hay
 * sucesora y solo existe el primero. Este servicio <b>no cambio de firma</b>
 * para ganarlo: el actor ya venia en el command desde el primer dia, puesto por
 * el controller desde la sesion.
 *
 * <p>
 * <strong>El instante es uno solo</strong> —{@code LocalDateTime.now(clock)}
 * leido una vez— para que el {@code superseded_at} de la vieja y el
 * {@code published_at} de la nueva coincidan al microsegundo. Con dos lecturas
 * del reloj queda un hueco en el que ninguna revision rigio, y
 * {@code chk_catalog_item_ai_hints_supersede} no lo detecta.
 */
@Observed(name = "catalogitemaihint.revise")
@Service
public class ReviseCatalogItemAiHintService implements ReviseCatalogItemAiHintUseCase {

    private final CatalogItemAiHintRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final Clock clock;

    public ReviseCatalogItemAiHintService(CatalogItemAiHintRepository repository,
            CatalogItemQueryPort catalogItemQueryPort, Clock clock) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogItemAiHintDto execute(ReviseCatalogItemAiHintCommand command) {
        CatalogItemAiHint vigente = repository.findCurrentByCatalogItemId(command.catalogItemId())
                .orElseThrow(() -> new CatalogItemAiHintNotFoundException(command.catalogItemId()));
        if (repository.existsPublishedText(command.catalogItemId(), command.hintText())) {
            throw new CatalogItemAiHintTextAlreadyPublishedException(command.catalogItemId());
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        vigente.supersede(ahora, command.revisedBySystemUserId());
        repository.supersede(vigente);
        int siguiente = repository.findLastRevision(command.catalogItemId()).orElse(0) + 1;
        CatalogItemAiHint nueva = CatalogItemAiHint.publish(command.catalogItemId(), siguiente,
                command.hintText(), command.revisedBySystemUserId(), ahora, ahora);
        return CatalogItemAiHintDto.from(repository.save(nueva),
                catalogItemQueryPort.findById(command.catalogItemId()).orElse(null));
    }
}
