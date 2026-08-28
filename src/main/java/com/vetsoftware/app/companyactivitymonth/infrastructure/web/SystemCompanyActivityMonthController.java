package com.vetsoftware.app.companyactivitymonth.infrastructure.web;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.command.UpdateCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.FindCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListCompanyActivityMonthsUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListDormantCompaniesUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.RecordCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.in.UpdateCompanyActivityMonthUseCase;
import com.vetsoftware.app.companyactivitymonth.infrastructure.web.request.RecordCompanyActivityMonthRequest;
import com.vetsoftware.app.companyactivitymonth.infrastructure.web.request.UpdateCompanyActivityMonthRequest;
import com.vetsoftware.app.companyactivitymonth.infrastructure.web.response.CompanyActivityMonthResponse;
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
 * La serie de actividad mensual de las clinicas, desde la consola de
 * plataforma.
 *
 * <h2>Por que es {@code SYSTEM} y no tiene camino de tenant</h2>
 *
 * <p>
 * <strong>Esta tabla es el instrumento con el que plataforma mide a sus
 * clientes</strong>: cuantos dias entro cada clinica, cuanta gente suya la uso
 * y cuanto MRR aportaba ese mes. Es lo que permite ver que una cuenta se esta
 * enfriando <em>antes</em> de que cancele. Abrirla por permiso tendria dos
 * efectos y los dos son malos: la clinica podria <b>escribir</b> los numeros
 * con los que se la evalua, y podria <b>leer</b> los de las demas en cualquier
 * listado que no filtrara. Por eso los cinco puertos de entrada llevan
 * {@code hasRole('SYSTEM')} a secas, sin una sola {@code hasAuthority}
 * alternativa —que en una feature cerrada a {@code SYSTEM} seria un endpoint
 * que se abre sembrando un permiso
 * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM})—.
 *
 * <p>
 * Tres de los listados —{@link #listAll}, {@link #listByPeriod} y sobre todo
 * {@link #listDormant}— <strong>no filtran por empresa a proposito</strong>:
 * comparar clinicas entre si <em>es</em> el producto, y el indice
 * {@code ix_cam_dormant} esta construido justo para eso. Un listado que no
 * filtra por empresa solo lo puede servir {@code hasRole('SYSTEM')} a secas
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, BE-29). {@link #listByCompany} es
 * el hermano acotado, tambien de plataforma.
 *
 * <h2>La empresa viaja en la query string, nunca en el cuerpo</h2>
 *
 * <p>
 * En el alta, un principal {@code SYSTEM} tiene que elegir a que clinica se
 * refiere la fila —no tiene empresa propia de la que derivarla—. Esa eleccion
 * va como {@code @RequestParam}: {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe
 * el cuerpo y solo el cuerpo, porque un {@code companyId} dentro del JSON
 * convierte cualquier comprobacion de tenant en una comparacion del numero
 * consigo mismo.
 *
 * <h2>No hay borrado, y tampoco deshabilitado</h2>
 *
 * <p>
 * Una medicion no se retira. Un mes mal calculado se <em>recalcula</em> encima
 * ({@link #update}), que ademas es la operacion por la que la tabla lleva
 * bloqueo optimista. Poder borrar una fila floja seria poder maquillar la unica
 * serie que dice si un cliente se esta yendo.
 */
@RestController
@RequestMapping("/system/company-activity-months")
public class SystemCompanyActivityMonthController {

    private final RecordCompanyActivityMonthUseCase recordUseCase;
    private final UpdateCompanyActivityMonthUseCase updateUseCase;
    private final FindCompanyActivityMonthUseCase findUseCase;
    private final ListCompanyActivityMonthsUseCase listUseCase;
    private final ListDormantCompaniesUseCase listDormantUseCase;

    public SystemCompanyActivityMonthController(RecordCompanyActivityMonthUseCase recordUseCase,
            UpdateCompanyActivityMonthUseCase updateUseCase,
            FindCompanyActivityMonthUseCase findUseCase,
            ListCompanyActivityMonthsUseCase listUseCase,
            ListDormantCompaniesUseCase listDormantUseCase) {
        this.recordUseCase = recordUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listDormantUseCase = listDormantUseCase;
    }

    /**
     * Da de alta la fila del mes. La empresa llega por la query string; ver el
     * javadoc de la clase.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyActivityMonthResponse record(@RequestParam Long companyId,
            @Valid @RequestBody RecordCompanyActivityMonthRequest request) {
        return toResponse(recordUseCase.execute(new RecordCompanyActivityMonthCommand(companyId,
                request.periodKey(), request.commercialState(), request.activeDays(),
                request.activeUsers(), request.recordsCreated(), request.mrrSnapshot())));
    }

    /**
     * {@code PATCH} y no {@code PUT}: recalcular el mes es reescribir sus cinco
     * numeros sobre la fila que ya existe, no reemplazar el recurso —la empresa, el
     * periodo y la fecha de creacion se quedan donde estaban—.
     */
    @PatchMapping("/{id}")
    public CompanyActivityMonthResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateCompanyActivityMonthRequest request) {
        return toResponse(updateUseCase.execute(new UpdateCompanyActivityMonthCommand(id,
                request.commercialState(), request.activeDays(), request.activeUsers(),
                request.recordsCreated(), request.mrrSnapshot())));
    }

    @GetMapping("/{id}")
    public CompanyActivityMonthResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    /**
     * La fila de una clinica en un mes. Ruta propia y no un filtro sobre el listado
     * porque devuelve <b>un recurso</b>, no una pagina: {@code uq_cam_month}
     * garantiza que hay como mucho uno, y un 404 dice mas que una lista vacia.
     */
    @GetMapping("/lookup")
    public CompanyActivityMonthResponse findByCompanyAndPeriod(@RequestParam Long companyId,
            @RequestParam String periodKey) {
        return toResponse(findUseCase.findByCompanyIdAndPeriodKey(companyId, periodKey));
    }

    @GetMapping
    public PageResponse<CompanyActivityMonthResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), this::toResponse);
    }

    @GetMapping("/by-company")
    public PageResponse<CompanyActivityMonthResponse> listByCompany(@RequestParam Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByCompany(companyId, page, pageSize),
                this::toResponse);
    }

    @GetMapping("/by-period")
    public PageResponse<CompanyActivityMonthResponse> listByPeriod(@RequestParam String periodKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByPeriod(periodKey, page, pageSize),
                this::toResponse);
    }

    /**
     * El barrido de dormidos de un mes.
     *
     * <p>
     * El umbral no tiene defecto util y por eso es obligatorio: «dormido» son tres
     * dias para quien mira retencion y cero para quien mira bajas, y elegir uno
     * aqui seria decidir por quien pregunta.
     */
    @GetMapping("/dormant")
    public PageResponse<CompanyActivityMonthResponse> listDormant(@RequestParam String periodKey,
            @RequestParam int activeDaysThreshold, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listDormantUseCase.listDormant(periodKey, activeDaysThreshold, page, pageSize),
                this::toResponse);
    }

    private CompanyActivityMonthResponse toResponse(CompanyActivityMonthDto dto) {
        return CompanyActivityMonthResponse.from(dto);
    }
}
