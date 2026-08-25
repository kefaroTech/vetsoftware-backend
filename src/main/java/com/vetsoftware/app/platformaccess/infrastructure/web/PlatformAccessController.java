package com.vetsoftware.app.platformaccess.infrastructure.web;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.dto.PlatformAccessRequestDto;
import com.vetsoftware.app.platformaccess.application.dto.PlatformInvitationDto;
import com.vetsoftware.app.platformaccess.application.port.in.AcceptPlatformInvitationUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ApprovePlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RejectPlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.RequestPlatformAccessUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformAccessTokenUseCase;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformInvitationTokenUseCase;
import com.vetsoftware.app.platformaccess.infrastructure.web.request.AcceptInvitationRequest;
import com.vetsoftware.app.platformaccess.infrastructure.web.request.CreateAccessRequestRequest;
import com.vetsoftware.app.platformaccess.infrastructure.web.request.ResolveAccessRequestRequest;
import com.vetsoftware.app.platformaccess.infrastructure.web.response.AccessRequestResponse;
import com.vetsoftware.app.platformaccess.infrastructure.web.response.InvitationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los seis endpoints del alta de superadministradores por invitación. Los seis
 * son anónimos por construcción, no por comodidad: quien solicita el acceso
 * todavía no tiene cuenta, y quien aprueba, rechaza o acepta se acredita con la
 * posesión de un token de un solo uso.
 *
 * <p>
 * <b>Un solo controller para las dos rutas hijas</b> ({@code /access-request/…}
 * e {@code /invitation/…}) porque son un mismo flujo y una misma rodaja de
 * contrato. El {@code @RequestMapping} es {@code /platform} y cada método
 * declara su path literal: nunca un comodín, porque de ese mismo prefijo
 * colgarán endpoints de administración de plataforma y un patrón amplio los
 * abriría al mundo sin que nadie lo vea en el diff.
 *
 * <p>
 * <b>Códigos que este controller introduce en el repositorio.</b> El 202 de
 * crear la solicitud dice «recibido, el desenlace llega por correo» y es
 * deliberadamente idéntico exista o no ya una cuenta con ese correo. El 422 del
 * código incorrecto y el 429 del bloqueo salen del manejador global, que es
 * también quien mantiene el 404 indistinguible de las tres familias de token
 * muerto.
 */
@RestController
@RequestMapping("/platform")
public class PlatformAccessController {

    private final RequestPlatformAccessUseCase requestUseCase;
    private final ValidatePlatformAccessTokenUseCase validateAccessTokenUseCase;
    private final ApprovePlatformAccessRequestUseCase approveUseCase;
    private final RejectPlatformAccessRequestUseCase rejectUseCase;
    private final ValidatePlatformInvitationTokenUseCase validateInvitationTokenUseCase;
    private final AcceptPlatformInvitationUseCase acceptUseCase;

    public PlatformAccessController(RequestPlatformAccessUseCase requestUseCase,
            ValidatePlatformAccessTokenUseCase validateAccessTokenUseCase,
            ApprovePlatformAccessRequestUseCase approveUseCase,
            RejectPlatformAccessRequestUseCase rejectUseCase,
            ValidatePlatformInvitationTokenUseCase validateInvitationTokenUseCase,
            AcceptPlatformInvitationUseCase acceptUseCase) {
        this.requestUseCase = requestUseCase;
        this.validateAccessTokenUseCase = validateAccessTokenUseCase;
        this.approveUseCase = approveUseCase;
        this.rejectUseCase = rejectUseCase;
        this.validateInvitationTokenUseCase = validateInvitationTokenUseCase;
        this.acceptUseCase = acceptUseCase;
    }

    /**
     * Recibe la solicitud. 202 sin cuerpo <b>siempre</b> que el formulario esté
     * abierto: exista o no ya una cuenta con ese correo, y la haya pedido ya esa
     * persona o no. El único desenlace distinto es el 404 del formulario cerrado.
     */
    @PostMapping("/access-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createAccessRequest(@Valid @RequestBody CreateAccessRequestRequest request) {
        requestUseCase.execute(new RequestPlatformAccessCommand(request.fullName(), request.email(),
                request.reason()));
    }

    /** Resuelve el enlace del aprobador. No consume nada y no gasta intentos. */
    @GetMapping("/access-request/validate")
    public AccessRequestResponse validateAccessRequest(@RequestParam String token) {
        PlatformAccessRequestDto dto = validateAccessTokenUseCase.execute(token);
        return new AccessRequestResponse(dto.fullName(), dto.email(), dto.reason(),
                dto.requestedAt());
    }

    @PostMapping("/access-request/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveAccessRequest(@Valid @RequestBody ResolveAccessRequestRequest request) {
        approveUseCase.execute(new ResolvePlatformAccessCommand(request.token(), request.code()));
    }

    @PostMapping("/access-request/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectAccessRequest(@Valid @RequestBody ResolveAccessRequestRequest request) {
        rejectUseCase.execute(new ResolvePlatformAccessCommand(request.token(), request.code()));
    }

    @GetMapping("/invitation/validate")
    public InvitationResponse validateInvitation(@RequestParam String token) {
        PlatformInvitationDto dto = validateInvitationTokenUseCase.execute(token);
        return new InvitationResponse(dto.email());
    }

    /**
     * Consume la invitación y crea la cuenta. 204 sin cuerpo: no hay autologin, no
     * se emite JWT, refresh ni cookie. El usuario recibe su código de acceso por
     * correo y entra por el login normal.
     */
    @PostMapping("/invitation/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        acceptUseCase
                .execute(new AcceptPlatformInvitationCommand(request.token(), request.password()));
    }
}
