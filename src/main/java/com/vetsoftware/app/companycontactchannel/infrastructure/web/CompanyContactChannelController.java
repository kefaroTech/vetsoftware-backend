package com.vetsoftware.app.companycontactchannel.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companycontactchannel.application.command.AuthorizeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.command.DesignatePrimaryCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.command.RevokeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.port.in.AuthorizeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.DesignatePrimaryCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.FindCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.ListUsableCompanyContactChannelsUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.in.RevokeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.companycontactchannel.infrastructure.web.request.AuthorizeCompanyContactChannelRequest;
import com.vetsoftware.app.companycontactchannel.infrastructure.web.request.RevokeCompanyContactChannelRequest;
import com.vetsoftware.app.companycontactchannel.infrastructure.web.response.CompanyContactChannelResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los canales de contacto autorizados de la empresa que firma el token.
 *
 * <p>
 * <strong>La empresa sale siempre de {@code authz.currentCompanyId()} y nunca
 * de la URL ni del cuerpo.</strong> Es lo que impide sembrar canales en la
 * ficha de otra clinica —o leer los suyos— escribiendo su id, y por eso ningun
 * endpoint de aqui acepta una empresa por parametro.
 *
 * <p>
 * <strong>No hay {@code DELETE}, y esa ausencia es la feature.</strong> Un
 * canal que deja de valer se cierra con {@code PATCH .../revoke} y se queda a
 * la vista con su fecha y su motivo. Hay que poder demostrar que el aviso de
 * marzo iba a una direccion autorizada en marzo; una fila borrada demuestra lo
 * contrario de lo que hace falta.
 *
 * <p>
 * <strong>Tampoco hay controller de plataforma</strong>, y hoy no falta: la
 * bitacora de una empresa la lee esa empresa. El dia que la consola necesite un
 * barrido cross-tenant, no se abre este controller: van en casos de uso
 * separados y cerrados a {@code hasRole('SYSTEM')} a secas, porque un listado
 * sin filtro de empresa aqui son las direcciones de contacto de todas las
 * clinicas.
 */
@RestController
@RequestMapping("/company-contact-channels")
public class CompanyContactChannelController {

    private final AuthorizeCompanyContactChannelUseCase authorizeUseCase;
    private final RevokeCompanyContactChannelUseCase revokeUseCase;
    private final DesignatePrimaryCompanyContactChannelUseCase designatePrimaryUseCase;
    private final FindCompanyContactChannelUseCase findUseCase;
    private final ListUsableCompanyContactChannelsUseCase listUsableUseCase;
    private final ListCompanyContactChannelsUseCase listUseCase;
    private final Authz authz;

    public CompanyContactChannelController(AuthorizeCompanyContactChannelUseCase authorizeUseCase,
            RevokeCompanyContactChannelUseCase revokeUseCase,
            DesignatePrimaryCompanyContactChannelUseCase designatePrimaryUseCase,
            FindCompanyContactChannelUseCase findUseCase,
            ListUsableCompanyContactChannelsUseCase listUsableUseCase,
            ListCompanyContactChannelsUseCase listUseCase, Authz authz) {
        this.authorizeUseCase = authorizeUseCase;
        this.revokeUseCase = revokeUseCase;
        this.designatePrimaryUseCase = designatePrimaryUseCase;
        this.findUseCase = findUseCase;
        this.listUsableUseCase = listUsableUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyContactChannelResponse authorize(
            @Valid @RequestBody AuthorizeCompanyContactChannelRequest request) {
        return CompanyContactChannelResponse
                .from(authorizeUseCase.execute(new AuthorizeCompanyContactChannelCommand(
                        authz.currentCompanyId(), request.channelType(), request.address(),
                        request.purpose(), request.authorizationEvidence())));
    }

    /**
     * <strong>No es un {@code DELETE} aunque saque el canal de
     * circulacion.</strong> La fila se queda con su cierre fechado y motivado: es
     * la prueba de hasta cuando estuvo permitido escribir por ahi.
     */
    @PatchMapping("/{id}/revoke")
    public CompanyContactChannelResponse revoke(@PathVariable Long id,
            @Valid @RequestBody RevokeCompanyContactChannelRequest request) {
        return CompanyContactChannelResponse
                .from(revokeUseCase.execute(new RevokeCompanyContactChannelCommand(id,
                        authz.currentCompanyId(), request.reason())));
    }

    /**
     * <strong>{@code PATCH} y sin cuerpo</strong>: la operacion no recibe ningun
     * dato. El proposito no viaja porque es el que ya tiene el canal senalado;
     * aceptarlo permitiria pedir que un canal de marketing pase a ser el primario
     * de facturacion, que no es una designacion sino una reescritura del
     * consentimiento.
     */
    @PatchMapping("/{id}/primary")
    public CompanyContactChannelResponse designatePrimary(@PathVariable Long id) {
        return CompanyContactChannelResponse.from(designatePrimaryUseCase.execute(
                new DesignatePrimaryCompanyContactChannelCommand(id, authz.currentCompanyId())));
    }

    /**
     * Por donde se le puede escribir hoy a esta empresa para ese fin.
     *
     * <p>
     * El proposito es obligatorio y no tiene valor por defecto <strong>a
     * proposito</strong>: autorizar un fin no autoriza los demas, y un defecto
     * silencioso convertiria esta consulta en la lista de a quien se puede escribir
     * para cualquier cosa.
     *
     * <p>
     * Va <em>antes</em> del mapeo por {@code {id}} porque {@code PathPatternParser}
     * da preferencia al literal frente a la variable, pero dejarlo escrito arriba
     * evita que alguien tenga que comprobarlo.
     */
    @GetMapping("/usable")
    public PageResponse<CompanyContactChannelResponse> listUsable(
            @RequestParam ContactPurpose purpose, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUsableUseCase.listUsable(authz.currentCompanyId(), purpose, page, pageSize),
                CompanyContactChannelResponse::from);
    }

    @GetMapping("/{id}")
    public CompanyContactChannelResponse findById(@PathVariable Long id) {
        return CompanyContactChannelResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    /** La bitacora completa de la empresa, revocados incluidos. */
    @GetMapping
    public PageResponse<CompanyContactChannelResponse> listByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompany(authz.currentCompanyId(), page, pageSize),
                CompanyContactChannelResponse::from);
    }
}
