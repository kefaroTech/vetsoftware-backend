package com.vetsoftware.app.companylimitevent.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitevent.application.port.in.ListCompanyLimitEventsUseCase;
import com.vetsoftware.app.companylimitevent.infrastructure.web.response.CompanyLimitEventResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <strong>El cliente ve sus propios portazos.</strong> Solo lectura.
 *
 * <p>
 * Es literalmente lo que la ficha de construcción escribe para el bloque
 * <em>Contadores y bitácora de cupo</em> —escribe plataforma, leen ambos— y la
 * mitad del valor de D-59: un aviso de cupo que solo existe en el momento en
 * que salta no le sirve a nadie al día siguiente, y «¿cuántas veces topamos el
 * techo en marzo?» es a la vez la respuesta a una reclamación y la señal de
 * venta más limpia que produce el modelo.
 *
 * <p>
 * <strong>La empresa la pone el backend</strong> con
 * {@code authz.currentCompanyId()}; el puerto la revalida con
 * {@code @authz.isMyCompany(#companyId)}. El rango es obligatorio a propósito:
 * la bitácora crece sin techo y un listado sin ventana temporal acaba siendo un
 * volcado de la tabla.
 *
 * <p>
 * <strong>Aquí no se escribe nada, y eso incluye el propio hecho de
 * cupo.</strong> {@code RecordLimitEventUseCase} admite al tenant en su gate
 * —tiene que hacerlo: el hecho más frecuente nace <em>dentro</em> de una
 * petición de la clínica, cuando se le niega crear— pero su llamador es
 * {@code LimitDenialAdapter}, no un cliente HTTP. Publicarlo como endpoint le
 * daría a la clínica la capacidad de fabricar portazos que nunca ocurrieron, y
 * la bitácora vale justo lo que vale su credibilidad. La corrección del
 * consumo, que sí necesita a una persona detrás, vive en
 * {@link SystemCompanyLimitEventController} y está cerrada a plataforma.
 */
@RestController
@RequestMapping("/company-limit-events")
public class CompanyLimitEventController {

    private final ListCompanyLimitEventsUseCase listUseCase;
    private final Authz authz;

    public CompanyLimitEventController(ListCompanyLimitEventsUseCase listUseCase, Authz authz) {
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<CompanyLimitEventResponse> listMine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return listUseCase.listByCompanyId(authz.currentCompanyId(), from, to).stream()
                .map(CompanyLimitEventResponse::from).toList();
    }
}
