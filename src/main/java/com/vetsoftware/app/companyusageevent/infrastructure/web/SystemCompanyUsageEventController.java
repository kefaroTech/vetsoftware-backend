package com.vetsoftware.app.companyusageevent.infrastructure.web;

import com.vetsoftware.app.companyusageevent.application.command.AttachUsageEventToChargeCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.AttachUsageEventToChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.FindCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.ListCompanyUsageEventsUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.ListUsageEventsByChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.infrastructure.web.request.AttachUsageEventToChargeRequest;
import com.vetsoftware.app.companyusageevent.infrastructure.web.request.RecordCompanyUsageEventRequest;
import com.vetsoftware.app.companyusageevent.infrastructure.web.response.CompanyUsageEventResponse;
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
 * Los hechos de consumo, desde la consola de plataforma.
 *
 * <h2>Por que esto es {@code SYSTEM} y no una pantalla del tenant</h2>
 *
 * <p>
 * <strong>Esta tabla es la prueba de un cobro, y la prueba no la escribe la
 * parte a la que se le cobra.</strong> Un tenant capaz de anotar sus propios
 * hechos podria no anotarlos —y dejar de pagar el excedente— o anotar de mas
 * los de otro. Por eso los cinco casos de uso estan cerrados a
 * {@code hasRole('SYSTEM')} a secas y no hay ni un camino por permiso: una
 * {@code hasAuthority} suelta seria un endpoint que se abre sembrando una fila
 * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
 *
 * <p>
 * <strong>La empresa viaja como {@code @RequestParam}, nunca en el
 * cuerpo.</strong> Un principal {@code SYSTEM} no tiene empresa propia, asi que
 * tiene que elegir a que clinica afecta cada operacion —el mismo patron que los
 * demas controllers de plataforma—. En el cuerpo estaria prohibido
 * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}) porque un {@code companyId} escrito
 * en el JSON convierte cualquier comprobacion de tenant en una comparacion del
 * numero consigo mismo.
 *
 * <p>
 * <strong>Lo que el cliente vera algun dia es el desglose de su propio
 * cargo</strong>, y ese es {@link #listByCharge}: recibe {@code companyId} y
 * acota con el, asi que es el unico de los tres listados que se puede abrir a
 * un tenant sin tocar nada mas. {@link #listAll} <b>no</b>: cruza clinicas por
 * definicion.
 *
 * <h2>No hay borrado, y no es un olvido</h2>
 *
 * <p>
 * Un hecho de uso no se borra ni se desactiva —la tabla ni siquiera tiene
 * {@code enabled}—. Borrar el hecho que sostiene un excedente ya facturado
 * destruiria justo la prueba que hace defendible ese cobro. Si un hecho se
 * anoto mal, lo que entra es otro hecho que lo compensa, con su propio
 * instante.
 */
@RestController
@RequestMapping("/system/company-usage-events")
public class SystemCompanyUsageEventController {

    private final RecordCompanyUsageEventUseCase recordUseCase;
    private final AttachUsageEventToChargeUseCase attachUseCase;
    private final FindCompanyUsageEventUseCase findUseCase;
    private final ListCompanyUsageEventsUseCase listUseCase;
    private final ListUsageEventsByChargeUseCase listByChargeUseCase;

    public SystemCompanyUsageEventController(RecordCompanyUsageEventUseCase recordUseCase,
            AttachUsageEventToChargeUseCase attachUseCase, FindCompanyUsageEventUseCase findUseCase,
            ListCompanyUsageEventsUseCase listUseCase,
            ListUsageEventsByChargeUseCase listByChargeUseCase) {
        this.recordUseCase = recordUseCase;
        this.attachUseCase = attachUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByChargeUseCase = listByChargeUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyUsageEventResponse record(@RequestParam Long companyId,
            @Valid @RequestBody RecordCompanyUsageEventRequest request) {
        return toResponse(recordUseCase.execute(new RecordCompanyUsageEventCommand(companyId,
                request.limitDimensionCode(), request.usageReferenceId(), request.occurredAt(),
                request.periodKey(), Boolean.TRUE.equals(request.billable()))));
    }

    /**
     * {@code PATCH} y no {@code PUT}: colgar el cargo escribe una columna de una
     * fila que se queda tal cual en todo lo demas.
     */
    @PatchMapping("/{id}/charge")
    public CompanyUsageEventResponse attachToCharge(@PathVariable Long id,
            @RequestParam Long companyId,
            @Valid @RequestBody AttachUsageEventToChargeRequest request) {
        return toResponse(attachUseCase
                .execute(new AttachUsageEventToChargeCommand(id, companyId, request.chargeId())));
    }

    @GetMapping("/{id}")
    public CompanyUsageEventResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    /**
     * El barrido de plataforma, paginado siempre.
     *
     * <p>
     * La proyeccion de esta tabla son doce millones de filas: no hay ninguna via
     * que devuelva la coleccion entera, y {@code Pages} acota el {@code pageSize} a
     * 200 aunque alguien pida mas.
     */
    @GetMapping
    public PageResponse<CompanyUsageEventResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), this::toResponse);
    }

    /** Los hechos de una clinica: el hermano acotado por empresa. */
    @GetMapping("/by-company")
    public PageResponse<CompanyUsageEventResponse> listByCompany(@RequestParam Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByCompany(companyId, page, pageSize),
                this::toResponse);
    }

    /**
     * El desglose de un cargo por excedente: la consulta que gana la reclamacion.
     */
    @GetMapping("/by-charge")
    public PageResponse<CompanyUsageEventResponse> listByCharge(@RequestParam Long companyId,
            @RequestParam Long chargeId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listByChargeUseCase.listByCharge(companyId, chargeId, page, pageSize),
                this::toResponse);
    }

    private CompanyUsageEventResponse toResponse(CompanyUsageEventDto dto) {
        return CompanyUsageEventResponse.from(dto);
    }
}
