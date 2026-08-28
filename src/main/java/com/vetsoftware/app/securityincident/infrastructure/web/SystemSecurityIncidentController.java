package com.vetsoftware.app.securityincident.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.securityincident.application.command.CloseSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.ReportSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.port.in.CloseSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.FindSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ListAffectedCompaniesUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ListSecurityIncidentsUseCase;
import com.vetsoftware.app.securityincident.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.securityincident.application.port.in.RegisterSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.in.ReportSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.infrastructure.web.request.CloseSecurityIncidentRequest;
import com.vetsoftware.app.securityincident.infrastructure.web.request.RegisterAffectedCompanyRequest;
import com.vetsoftware.app.securityincident.infrastructure.web.request.RegisterSecurityIncidentRequest;
import com.vetsoftware.app.securityincident.infrastructure.web.request.ReportSecurityIncidentRequest;
import com.vetsoftware.app.securityincident.infrastructure.web.response.AffectedCompanyResponse;
import com.vetsoftware.app.securityincident.infrastructure.web.response.SecurityIncidentResponse;
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
 * El expediente de incidentes de seguridad, entero y solo para plataforma.
 *
 * <p>
 * <strong>Aqui no hay {@code companyId} del principal por ninguna via, y no es
 * la ausencia de siempre.</strong> En otros bloques de plataforma la empresa
 * viaja como {@code @RequestParam} porque un principal SYSTEM no tiene empresa
 * propia y elige a que clinica afecta. Aqui el incidente no es de nadie en
 * particular: es de VetSoftware, y alcanza a las clinicas que alcance. El
 * {@code companyId} que si aparece —en la ruta de afectados— no dice quien
 * llama sino <em>a quien alcanzo</em>, y va en la URL y no en el cuerpo porque
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} no puede distinguir las dos cosas
 * mirando un DTO, ni debe.
 *
 * <p>
 * <strong>No hay {@code DELETE} en ninguna de las dos tablas.</strong> Un
 * incidente se <em>cierra</em>, que es escribir como acabo; retirarlo dejaria
 * sin explicacion el reporte ya presentado a la autoridad. Y quitar una clinica
 * de la lista de afectados destruiria la prueba de que se le notifico. Las dos
 * ausencias son la decision, no un pendiente.
 */
@RestController
@RequestMapping("/system/security-incidents")
public class SystemSecurityIncidentController {

    private final RegisterSecurityIncidentUseCase registerUseCase;
    private final ReportSecurityIncidentUseCase reportUseCase;
    private final CloseSecurityIncidentUseCase closeUseCase;
    private final FindSecurityIncidentUseCase findUseCase;
    private final ListSecurityIncidentsUseCase listUseCase;
    private final RegisterAffectedCompanyUseCase registerAffectedUseCase;
    private final ListAffectedCompaniesUseCase listAffectedUseCase;

    public SystemSecurityIncidentController(RegisterSecurityIncidentUseCase registerUseCase,
            ReportSecurityIncidentUseCase reportUseCase, CloseSecurityIncidentUseCase closeUseCase,
            FindSecurityIncidentUseCase findUseCase, ListSecurityIncidentsUseCase listUseCase,
            RegisterAffectedCompanyUseCase registerAffectedUseCase,
            ListAffectedCompaniesUseCase listAffectedUseCase) {
        this.registerUseCase = registerUseCase;
        this.reportUseCase = reportUseCase;
        this.closeUseCase = closeUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.registerAffectedUseCase = registerAffectedUseCase;
        this.listAffectedUseCase = listAffectedUseCase;
    }

    /**
     * Da de alta el incidente. El vencimiento del reporte lo calcula el servidor:
     * quince dias habiles desde el escalamiento.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityIncidentResponse register(
            @Valid @RequestBody RegisterSecurityIncidentRequest request) {
        return SecurityIncidentResponse.from(
                registerUseCase.execute(new RegisterSecurityIncidentCommand(request.detectedAt(),
                        request.occurredAt(), request.escalatedAt(), request.kind(),
                        request.severity(), request.summary(), request.affectedSubjectCount())));
    }

    @GetMapping
    public PageResponse<SecurityIncidentResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                SecurityIncidentResponse::from);
    }

    @GetMapping("/{id}")
    public SecurityIncidentResponse findById(@PathVariable Long id) {
        return SecurityIncidentResponse.from(findUseCase.findById(id));
    }

    /**
     * {@code PATCH} y no {@code POST}: anotar el reporte es escribir dos campos en
     * una fila que ya existe, no crear nada.
     */
    @PatchMapping("/{id}/report")
    public SecurityIncidentResponse report(@PathVariable Long id,
            @Valid @RequestBody ReportSecurityIncidentRequest request) {
        return SecurityIncidentResponse
                .from(reportUseCase.execute(new ReportSecurityIncidentCommand(id,
                        request.reportedAt(), request.reportReference())));
    }

    /**
     * {@code PATCH} y no {@code DELETE}: cerrar es escribir como acabo, no
     * retirarlo.
     */
    @PatchMapping("/{id}/close")
    public SecurityIncidentResponse close(@PathVariable Long id,
            @Valid @RequestBody CloseSecurityIncidentRequest request) {
        return SecurityIncidentResponse
                .from(closeUseCase.execute(new CloseSecurityIncidentCommand(id, request.closedAt(),
                        request.containment(), request.rootCause(), request.notifiedSubjectsAt())));
    }

    /**
     * <strong>La clinica va en la ruta y no en el cuerpo.</strong> Ver
     * {@code RegisterAffectedCompanyRequest}: la regla dura
     * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira el tipo del {@code @RequestBody} y
     * baja por sus campos, y la salida que ella misma documenta es un
     * {@code @PathVariable} cubierto por la familia «por id».
     */
    @PostMapping("/{id}/affected-companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AffectedCompanyResponse registerAffectedCompany(@PathVariable Long id,
            @PathVariable Long companyId,
            @Valid @RequestBody RegisterAffectedCompanyRequest request) {
        return AffectedCompanyResponse
                .from(registerAffectedUseCase.execute(new RegisterAffectedCompanyCommand(id,
                        companyId, request.affectedScope(), request.affectedSubjectCount())));
    }

    @GetMapping("/{id}/affected-companies")
    public PageResponse<AffectedCompanyResponse> listAffectedCompanies(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAffectedUseCase.listByIncident(id, page, pageSize),
                AffectedCompanyResponse::from);
    }
}
