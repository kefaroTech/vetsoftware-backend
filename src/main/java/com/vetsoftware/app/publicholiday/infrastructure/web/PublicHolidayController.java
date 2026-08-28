package com.vetsoftware.app.publicholiday.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.publicholiday.application.command.CreatePublicHolidayCommand;
import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.port.in.CreatePublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.FindPublicHolidayUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.ListPublicHolidaysUseCase;
import com.vetsoftware.app.publicholiday.application.port.in.ResolveBusinessDayDeadlineUseCase;
import com.vetsoftware.app.publicholiday.infrastructure.web.request.CreatePublicHolidayRequest;
import com.vetsoftware.app.publicholiday.infrastructure.web.response.BusinessDayDeadlineResponse;
import com.vetsoftware.app.publicholiday.infrastructure.web.response.PublicHolidayResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
 * El calendario de festivos y la operacion de plazos que sale de el.
 *
 * <p>
 * <strong>La empresa la pone el controller, nunca el cuerpo ni la
 * query</strong> —{@code authz.currentCompanyIdOrNull()}—, y el puerto la
 * revalida con {@code @authz.isMyCompany(...)}. Se usa la variante
 * {@code OrNull} y no {@code currentCompanyId()} porque este recurso lo leen
 * los dos lados: para un principal de plataforma no hay empresa que declarar y
 * {@code currentCompanyId()} lanzaria {@code AccessDeniedException} antes de
 * que el gate pudiera aplicar su rama {@code hasRole('SYSTEM')}. Con
 * {@code null}, {@code isMyCompany} devuelve {@code false} y la unica via que
 * queda abierta es exactamente esa.
 */
@RestController
@RequestMapping("/public-holidays")
public class PublicHolidayController {

    private final CreatePublicHolidayUseCase createUseCase;
    private final FindPublicHolidayUseCase findUseCase;
    private final ListPublicHolidaysUseCase listUseCase;
    private final ResolveBusinessDayDeadlineUseCase deadlineUseCase;
    private final Authz authz;

    public PublicHolidayController(CreatePublicHolidayUseCase createUseCase,
            FindPublicHolidayUseCase findUseCase, ListPublicHolidaysUseCase listUseCase,
            ResolveBusinessDayDeadlineUseCase deadlineUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deadlineUseCase = deadlineUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicHolidayResponse create(@Valid @RequestBody CreatePublicHolidayRequest request) {
        return PublicHolidayResponse
                .from(createUseCase.execute(new CreatePublicHolidayCommand(request.holidayDate(),
                        request.name(), request.nominalDate(), Boolean.TRUE.equals(request.moved()),
                        request.legalReference())));
    }

    @GetMapping
    public PageResponse<PublicHolidayResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(authz.currentCompanyIdOrNull(), page, pageSize),
                PublicHolidayResponse::from);
    }

    @GetMapping("/years/{year}")
    public List<PublicHolidayResponse> listByYear(@PathVariable int year) {
        return listUseCase.listByYear(year, authz.currentCompanyIdOrNull()).stream()
                .map(PublicHolidayResponse::from).toList();
    }

    /**
     * El vencimiento de un plazo en dias habiles. {@code startDate} ausente
     * significa «desde hoy», y ese hoy lo resuelve el reloj del servicio con la
     * zona del negocio; que lo resolviera el controller con {@code LocalDate.now()}
     * devolveria el dia siguiente a partir de las 19:00 de Bogota.
     */
    @GetMapping("/deadline")
    public BusinessDayDeadlineResponse deadline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam int businessDays) {
        return BusinessDayDeadlineResponse
                .from(deadlineUseCase.resolve(new ResolveBusinessDayDeadlineCommand(startDate,
                        businessDays, authz.currentCompanyIdOrNull())));
    }

    @GetMapping("/{id}")
    public PublicHolidayResponse findById(@PathVariable Long id) {
        return PublicHolidayResponse.from(findUseCase.findById(id, authz.currentCompanyIdOrNull()));
    }
}
