package com.vetsoftware.app.aiproposal.infrastructure.web;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.command.LegalAcceptanceCommand;
import com.vetsoftware.app.aiproposal.application.command.RefineProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.EditProposalLinesUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.GenerateProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.GetProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.in.RefineProposalUseCase;
import com.vetsoftware.app.aiproposal.domain.PackComparisonResult;
import com.vetsoftware.app.aiproposal.infrastructure.web.request.EditProposalLinesRequest;
import com.vetsoftware.app.aiproposal.infrastructure.web.request.GenerateProposalRequest;
import com.vetsoftware.app.aiproposal.infrastructure.web.request.RefineProposalRequest;
import com.vetsoftware.app.aiproposal.infrastructure.web.response.AssistantPackOfferResponse;
import com.vetsoftware.app.aiproposal.infrastructure.web.response.AssistantProposalLineResponse;
import com.vetsoftware.app.aiproposal.infrastructure.web.response.AssistantProposalResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El asistente comercial: cuatro rutas <strong>publicas y anonimas</strong>.
 *
 * <p>
 * <strong>Abrir cada una al mundo son TRES cosas, y CUATRO en los dos
 * {@code POST}</strong>, y ninguna basta por si sola:
 *
 * <ol>
 * <li>la ruta <strong>literal</strong> en {@code PublicRoutes.BUSINESS} -jamas
 * {@code /assistant/**}, que abriria de paso todo lo que acabe colgando del
 * prefijo-;</li>
 * <li>el {@code @NoAuthorizationRequired(reason = ...)} de cada puerto de
 * entrada;</li>
 * <li>la ruta escrita tambien en el {@code containsExactlyInAnyOrder} de
 * {@code PublicRoutesTest}, que afirma el inventario completo a proposito;</li>
 * <li>y su {@code RouteLimit} en {@code LoginRateLimitFilter}.
 * {@code POST_SIN_LIMITE_JUSTIFICADO} esta vacio: no hay ni un POST publico
 * perdonado.</li>
 * </ol>
 *
 * <p>
 * &#9940; <strong>El token de 43 caracteres no viaja en ningun segmento de
 * ruta.</strong> {@code RequestLoggingContextFilter} mete
 * {@code getRequestURI()} en el contexto de log de <em>toda</em> peticion
 * -incluidas las publicas, lo dice su javadoc- y ningun patron del redactor
 * casa con 43 caracteres de base64url sueltos: el token acabaria intacto en
 * CloudWatch y en Loki con 31 dias de retencion, y de paso en el
 * {@code Referer}. Va en {@code ?token=} en el {@code GET} y en el cuerpo en el
 * {@code POST} y el {@code PUT}, que es el precedente que ya dejaron el reseteo
 * de contrasena y las dos validaciones del alta de plataforma.
 *
 * <p>
 * <strong>Efecto secundario de esa decision:</strong> las cuatro rutas son
 * literales, asi que {@code LoginRateLimitFilter.routeLimit()} las casa con
 * {@code equals} y no las alcanza la trampa de
 * {@code LoginRateLimitFilterTest.rutaConcreta}, que no expande
 * {@code &#123;var&#125;} y dejaria el limite inoperante con el gate en verde.
 *
 * <p>
 * <strong>Los cuatro devuelven 200</strong>, tambien cuando el modelo no
 * respondio: el prospecto no puede hacer nada con un error y en dos de los
 * casos ya se pago por la llamada.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final GenerateProposalUseCase generateUseCase;

    private final RefineProposalUseCase refineUseCase;

    private final EditProposalLinesUseCase editUseCase;

    private final GetProposalUseCase getUseCase;

    public AssistantController(GenerateProposalUseCase generateUseCase,
            RefineProposalUseCase refineUseCase, EditProposalLinesUseCase editUseCase,
            GetProposalUseCase getUseCase) {
        this.generateUseCase = generateUseCase;
        this.refineUseCase = refineUseCase;
        this.editUseCase = editUseCase;
        this.getUseCase = getUseCase;
    }

    /**
     * Genera la propuesta inicial.
     *
     * <p>
     * <strong>{@code Idempotency-Key} es una cabecera y no un campo del
     * cuerpo</strong>: es metadato de transporte -"esta peticion es la misma que la
     * anterior"- y no un dato de la propuesta. El front genera el UUID al montar la
     * pantalla y el reintento lleva el mismo valor; sin el, un doble clic paga dos
     * llamadas al modelo y crea dos propuestas huerfanas que consumen cupo. Es
     * opcional: un cliente que no lo mande pierde la proteccion, no el servicio.
     *
     * <p>
     * &#9940; <strong>La clave se busca ACOTADA al correo</strong> (en el caso de
     * uso, contra {@code uq_ai_proposals_idempotency}). Buscarla sola convertiria
     * una cabecera que elige el cliente en una lectura de las propuestas ajenas.
     */
    @PostMapping("/proposal")
    public AssistantProposalResponse generate(@Valid @RequestBody GenerateProposalRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        return toResponse(generateUseCase.generate(new GenerateProposalCommand(request.email(),
                request.description(), idempotencyKey,
                request.acceptances().stream()
                        .map(aceptacion -> new LegalAcceptanceCommand(aceptacion.code(),
                                aceptacion.documentVersion()))
                        .toList(),
                hash(httpRequest.getRemoteAddr()), hash(httpRequest.getHeader("User-Agent")))));
    }

    /**
     * Anade texto y recalcula. Al cuarto turno de modelo devuelve 200 con la
     * propuesta intacta y {@code recalculated = false}, nunca un 400.
     */
    @PostMapping("/proposal/refine")
    public AssistantProposalResponse refine(@Valid @RequestBody RefineProposalRequest request) {
        return toResponse(refineUseCase.refine(
                new RefineProposalCommand(request.token(), request.text(), request.version())));
    }

    /** La edicion manual: no llama al modelo y no consume ni un token. */
    @PutMapping("/proposal/lines")
    public AssistantProposalResponse editLines(
            @Valid @RequestBody EditProposalLinesRequest request) {
        return toResponse(editUseCase.edit(new EditProposalLinesCommand(request.token(),
                request.addedCodes(), request.removedCodes(), request.version())));
    }

    /**
     * Relee la propuesta. El token entra por {@code ?token=} y no por un segmento
     * de ruta, igual que las tres rutas anonimas que ya existen.
     */
    @GetMapping("/proposal")
    public AssistantProposalResponse get(@RequestParam String token) {
        return toResponse(getUseCase.get(token));
    }

    /**
     * &#9940; <strong>Hash y nunca el valor.</strong> La IP y el agente de usuario
     * son la evidencia de <em>desde donde</em> se consintio, y las columnas de
     * {@code legal_document_acceptances} son {@code CHAR(64)} justamente porque
     * guardan la huella y no el dato: una IP en claro al lado de un correo es dato
     * personal que la anonimizacion a 90 dias tendria que borrar, y esta fila queda
     * excluida de esa anonimizacion por ser la evidencia.
     */
    private static String hash(String valor) {
        if (valor == null || valor.isBlank())
            return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", imposible);
        }
    }

    private static AssistantProposalResponse toResponse(ProposalViewDto vista) {
        return new AssistantProposalResponse(vista.publicToken(),
                vista.presentation() == null ? null : vista.presentation().name(),
                vista.expiresAt(), vista.version(),
                vista.lines().stream().map(AssistantController::toLineResponse).toList(),
                vista.recommendations().stream().map(AssistantController::toLineResponse).toList(),
                vista.discardedLines(), vista.currency(), vista.subtotal(), vista.taxes(),
                vista.total(), vista.firstPeriodTotal(), toPackResponse(vista.packOffer()),
                vista.refinementsLeft(), vista.recalculated());
    }

    private static AssistantProposalLineResponse toLineResponse(ProposalLineDto linea) {
        return new AssistantProposalLineResponse(linea.code(), linea.name(), linea.description(),
                linea.kind(), linea.quantity(), linea.unitAmount(), linea.taxRate(),
                linea.taxAmount(), linea.totalAmount(), linea.trialDays(), linea.currency(),
                linea.reason());
    }

    private static AssistantPackOfferResponse toPackResponse(PackComparisonResult oferta) {
        return oferta == null
                ? null
                : new AssistantPackOfferResponse(oferta.packCode(), oferta.packName(),
                        oferta.packAmount(), oferta.sumaSuelta(), oferta.ahorroMensual(),
                        oferta.currency(), oferta.diasDePruebaPerdidos(),
                        oferta.modulosQuePierdenPrueba());
    }
}
