package com.vetsoftware.app.catalogitemaihint.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.catalogitemaihint.application.command.PublishCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.command.RetireCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.command.ReviseCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.port.in.FindCurrentCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCatalogItemAiHintRevisionsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCurrentCatalogItemAiHintsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.PublishCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.RetireCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ReviseCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.infrastructure.web.request.PublishCatalogItemAiHintRequest;
import com.vetsoftware.app.catalogitemaihint.infrastructure.web.request.ReviseCatalogItemAiHintRequest;
import com.vetsoftware.app.catalogitemaihint.infrastructure.web.response.CatalogItemAiHintResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las pistas que el prompt le ensena al modelo sobre cada articulo del
 * catalogo. Hasta ahora solo se sembraban por changeset, asi que corregir una
 * pista que proponia mal exigia un despliegue.
 *
 * <p>
 * <strong>El recurso es «la pista vigente de un articulo», y su identidad es el
 * {@code catalogItemId}</strong> —no el {@code id} de la fila—. De ahi salen la
 * forma y los verbos:
 *
 * <ul>
 * <li>{@code GET /catalog-item-ai-hints} — las vigentes, paginadas.</li>
 * <li>{@code GET /catalog-item-ai-hints/{catalogItemId}} — la vigente de un
 * articulo; 404 si no tiene.</li>
 * <li>{@code GET /catalog-item-ai-hints/{catalogItemId}/revisions} — el
 * historial completo, de la mas nueva a la mas vieja.</li>
 * <li>{@code POST} — publica la primera; 409 si ya hay vigente.</li>
 * <li>{@code PUT /{catalogItemId}} — corrige: sucede la vigente y publica la
 * revision siguiente.</li>
 * <li>{@code DELETE /{catalogItemId}} — retira: sucede la vigente sin
 * sucesora.</li>
 * </ul>
 *
 * <p>
 * &#9940; <strong>Ni el PUT ni el DELETE hacen lo que su nombre HTTP sugiere a
 * nivel de fila, y eso es deliberado.</strong> El PUT no sobrescribe
 * {@code hint_text} —inserta una revision nueva y marca la anterior— y el
 * DELETE no borra nada —solo cierra la vigencia—. A nivel de <em>recurso</em>
 * los dos son exactos: despues del PUT, «la pista vigente de este articulo» es
 * otra; despues del DELETE, no hay. Y a nivel de fila la tabla no admite otra
 * cosa: sobrescribir el texto destruiria la unica evidencia de que se le estaba
 * diciendo al modelo cuando genero una propuesta pasada, que es justo para lo
 * que existe {@code hint_revision}.
 *
 * <p>
 * <strong>Todos los endpoints exigen cuenta de plataforma</strong>
 * ({@code hasRole('SYSTEM')} en cada puerto). No hay ruta publica aqui: estas
 * son las instrucciones del modelo comercial, no algo que el prospecto tenga
 * que poder leer.
 */
@RestController
@RequestMapping("/catalog-item-ai-hints")
public class CatalogItemAiHintController {

    private final ListCurrentCatalogItemAiHintsUseCase listCurrentUseCase;
    private final FindCurrentCatalogItemAiHintUseCase findCurrentUseCase;
    private final ListCatalogItemAiHintRevisionsUseCase listRevisionsUseCase;
    private final PublishCatalogItemAiHintUseCase publishUseCase;
    private final ReviseCatalogItemAiHintUseCase reviseUseCase;
    private final RetireCatalogItemAiHintUseCase retireUseCase;
    private final Authz authz;

    public CatalogItemAiHintController(ListCurrentCatalogItemAiHintsUseCase listCurrentUseCase,
            FindCurrentCatalogItemAiHintUseCase findCurrentUseCase,
            ListCatalogItemAiHintRevisionsUseCase listRevisionsUseCase,
            PublishCatalogItemAiHintUseCase publishUseCase,
            ReviseCatalogItemAiHintUseCase reviseUseCase,
            RetireCatalogItemAiHintUseCase retireUseCase, Authz authz) {
        this.listCurrentUseCase = listCurrentUseCase;
        this.findCurrentUseCase = findCurrentUseCase;
        this.listRevisionsUseCase = listRevisionsUseCase;
        this.publishUseCase = publishUseCase;
        this.reviseUseCase = reviseUseCase;
        this.retireUseCase = retireUseCase;
        this.authz = authz;
    }

    @GetMapping
    public PageResponse<CatalogItemAiHintResponse> listCurrent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listCurrentUseCase.listCurrent(page, pageSize),
                CatalogItemAiHintResponse::from);
    }

    @GetMapping("/{catalogItemId}")
    public CatalogItemAiHintResponse findCurrent(@PathVariable Long catalogItemId) {
        return CatalogItemAiHintResponse
                .from(findCurrentUseCase.findCurrentByCatalogItemId(catalogItemId));
    }

    /**
     * El historial. Es lo que hace util al diseno append-only: sin esta lectura, la
     * revision reemplazada quedaria guardada y no la podria ver nadie.
     */
    @GetMapping("/{catalogItemId}/revisions")
    public PageResponse<CatalogItemAiHintResponse> listRevisions(@PathVariable Long catalogItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listRevisionsUseCase.listByCatalogItemId(catalogItemId, page, pageSize),
                CatalogItemAiHintResponse::from);
    }

    /**
     * &#9940; El firmante sale de {@code authz.currentSystemUserId()}, nunca del
     * cuerpo: {@link PublishCatalogItemAiHintRequest} no declara ese campo.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemAiHintResponse publish(
            @Valid @RequestBody PublishCatalogItemAiHintRequest request) {
        return CatalogItemAiHintResponse.from(
                publishUseCase.execute(new PublishCatalogItemAiHintCommand(request.catalogItemId(),
                        request.hintText(), authz.currentSystemUserId())));
    }

    /**
     * Corrige. Responde 200 con la revision nueva —no 201— porque el recurso que se
     * pidio no ha cambiado de direccion: sigue siendo la pista vigente de este
     * articulo, con otro texto y un numero de revision mas.
     */
    @PutMapping("/{catalogItemId}")
    public CatalogItemAiHintResponse revise(@PathVariable Long catalogItemId,
            @Valid @RequestBody ReviseCatalogItemAiHintRequest request) {
        return CatalogItemAiHintResponse
                .from(reviseUseCase.execute(new ReviseCatalogItemAiHintCommand(catalogItemId,
                        request.hintText(), authz.currentSystemUserId())));
    }

    /**
     * Retira. La revision retirada sigue en {@code /revisions}.
     *
     * <p>
     * &#9940; El firmante de la retirada sale de
     * {@code authz.currentSystemUserId()} y <b>no hay cuerpo del que pudiera
     * salir</b>: un {@code DELETE} no lo lleva. Es el mismo criterio que el POST y
     * el PUT, y desde el changeset 393 tambien el DELETE deja constancia de quien
     * decidio que el articulo dejara de proponerse.
     */
    @DeleteMapping("/{catalogItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retire(@PathVariable Long catalogItemId) {
        retireUseCase.retire(
                new RetireCatalogItemAiHintCommand(catalogItemId, authz.currentSystemUserId()));
    }
}
