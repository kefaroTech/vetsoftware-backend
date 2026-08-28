package com.vetsoftware.app.legaldocumentversion.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.legaldocumentversion.application.command.PublishLegalDocumentVersionCommand;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindAcceptedLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.FindCurrentLegalDocumentUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.ListLegalDocumentVersionsUseCase;
import com.vetsoftware.app.legaldocumentversion.application.port.in.PublishLegalDocumentVersionUseCase;
import com.vetsoftware.app.legaldocumentversion.infrastructure.web.request.PublishLegalDocumentVersionRequest;
import com.vetsoftware.app.legaldocumentversion.infrastructure.web.response.LegalDocumentVersionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los textos legales.
 *
 * <p>
 * <strong>No hay PUT ni PATCH</strong>, y no es un olvido: el disparador de la
 * tabla rechaza editar el contenido o la huella de una version publicada. Lo
 * que hay es un POST que publica una version nueva y sucede a la anterior.
 *
 * <p>
 * <strong>El endpoint por huella es el que hace util a la columna</strong>: es
 * como el cliente vuelve a leer exactamente el texto que acepto, aunque ya haya
 * sido sucedido tres veces desde entonces.
 */
@RestController
@RequestMapping("/legal-documents")
public class LegalDocumentVersionController {

    private final PublishLegalDocumentVersionUseCase publishUseCase;
    private final FindCurrentLegalDocumentUseCase findCurrentUseCase;
    private final FindAcceptedLegalDocumentUseCase findAcceptedUseCase;
    private final ListLegalDocumentVersionsUseCase listUseCase;
    private final Authz authz;

    public LegalDocumentVersionController(PublishLegalDocumentVersionUseCase publishUseCase,
            FindCurrentLegalDocumentUseCase findCurrentUseCase,
            FindAcceptedLegalDocumentUseCase findAcceptedUseCase,
            ListLegalDocumentVersionsUseCase listUseCase, Authz authz) {
        this.publishUseCase = publishUseCase;
        this.findCurrentUseCase = findCurrentUseCase;
        this.findAcceptedUseCase = findAcceptedUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LegalDocumentVersionResponse publish(
            @Valid @RequestBody PublishLegalDocumentVersionRequest request) {
        return LegalDocumentVersionResponse
                .from(publishUseCase.execute(new PublishLegalDocumentVersionCommand(request.code(),
                        request.kind(), request.title(), request.content(), request.effectiveFrom(),
                        authz.currentSystemUserId())));
    }

    @GetMapping("/{code}/current")
    public LegalDocumentVersionResponse findCurrent(@PathVariable String code) {
        return LegalDocumentVersionResponse
                .from(findCurrentUseCase.findCurrentByCode(code, authz.currentCompanyIdOrNull()));
    }

    /**
     * Relee el texto por su huella. Devuelve la version historica: si el cliente
     * acepto la 2 y hoy rige la 5, esto le devuelve la 2.
     */
    @GetMapping("/{code}/by-hash/{contentHash}")
    public LegalDocumentVersionResponse findByHash(@PathVariable String code,
            @PathVariable String contentHash) {
        return LegalDocumentVersionResponse.from(findAcceptedUseCase.findByCodeAndHash(code,
                contentHash, authz.currentCompanyIdOrNull()));
    }

    @GetMapping("/{code}/versions")
    public PageResponse<LegalDocumentVersionResponse> listByCode(@PathVariable String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCode(code, authz.currentCompanyIdOrNull(), page, pageSize),
                LegalDocumentVersionResponse::from);
    }
}
