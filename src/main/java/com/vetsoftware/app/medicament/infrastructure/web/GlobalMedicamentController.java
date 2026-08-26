package com.vetsoftware.app.medicament.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.medicament.application.command.CreateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.command.UpdateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.CompanySummaryDto;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.DeleteGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ReactivateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.UpdateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.infrastructure.web.request.CreateGlobalMedicamentRequest;
import com.vetsoftware.app.medicament.infrastructure.web.request.UpdateGlobalMedicamentRequest;
import com.vetsoftware.app.medicament.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.medicament.infrastructure.web.response.MedicamentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Superficie de administracion del catalogo GLOBAL de medicamentos, para la
 * consola de plataforma. Todos sus puertos van cerrados a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <b>Por que {@code /admin/medicaments} y no otra ruta.</b> El prefijo
 * {@code /admin} es el que el repositorio ya usa para lo que solo administra la
 * plataforma —{@code AdminPermissionPublishController} sirve
 * {@code POST /admin/admin-permissions/publish}—, y aqui hace ademas un trabajo
 * que en aquel no hacia falta: {@code /medicaments} YA existe y es la
 * superficie del tenant, con gates de {@code prescription.*}. Colgar la
 * administracion global de la misma raiz obligaria a bifurcar por rol dentro
 * del mismo endpoint, que es exactamente lo que el CLAUDE.md prohibe («no
 * mezclar admin global y employee-scoped»). Dos raices distintas hacen que la
 * separacion se lea en la URL y no dependa de leerse un {@code if}.
 * {@code PriceListController} no lleva prefijo porque la tarifa no tiene
 * contraparte de tenant: ahi no hay nada de lo que separarse.
 *
 * <p>
 * <b>Ningun endpoint recibe ni deriva {@code companyId}.</b> No se inyecta
 * {@code Authz}: no hay nada que sacar del principal. La empresa de la fila es
 * {@code null} por construccion y {@code general} es {@code true}, los dos
 * puestos por el servidor. Un {@code currentCompanyId()} aqui seria ademas un
 * error de funcionamiento: para una cuenta de plataforma exige la cabecera
 * {@code X-Company-Id} y lanza si falta.
 *
 * <p>
 * <b>El listado de contexto no se duplica.</b> La consola necesita ver tambien
 * los medicamentos privados de las empresas, y eso ya lo sirve
 * {@code GET /medicaments} ({@code ListMedicamentsUseCase.listAll}), que es
 * {@code hasRole('SYSTEM')} desde BE-29. Aqui solo estan los globales, que son
 * los unicos que el superusuario puede escribir: el catalogo privado de una
 * clinica se lee, nunca se toca desde esta consola.
 */
@RestController
@RequestMapping("/admin/medicaments")
public class GlobalMedicamentController {

    private final CreateGlobalMedicamentUseCase createUseCase;
    private final UpdateGlobalMedicamentUseCase updateUseCase;
    private final ListGlobalMedicamentsUseCase listUseCase;
    private final ListDisabledGlobalMedicamentsUseCase listDisabledUseCase;
    private final DeleteGlobalMedicamentUseCase deleteUseCase;
    private final ReactivateGlobalMedicamentUseCase reactivateUseCase;

    public GlobalMedicamentController(CreateGlobalMedicamentUseCase createUseCase,
            UpdateGlobalMedicamentUseCase updateUseCase, ListGlobalMedicamentsUseCase listUseCase,
            ListDisabledGlobalMedicamentsUseCase listDisabledUseCase,
            DeleteGlobalMedicamentUseCase deleteUseCase,
            ReactivateGlobalMedicamentUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.listUseCase = listUseCase;
        this.listDisabledUseCase = listDisabledUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentResponse create(@Valid @RequestBody CreateGlobalMedicamentRequest request) {
        return toResponse(createUseCase
                .execute(new CreateGlobalMedicamentCommand(request.name(), request.description())));
    }

    /**
     * El catalogo global, con busqueda opcional por nombre en el servidor. Con 153
     * moleculas sembradas y paginas de 20, sin ella encontrar una es pasar seis
     * paginas; y un filtro en cliente no vale, porque solo ve la pagina cargada y
     * llevaria al operador a crear un duplicado de algo que si existe.
     */
    @GetMapping
    public PageResponse<MedicamentResponse> listAll(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(q, page, pageSize), this::toResponse);
    }

    /** Los globales pausados: la unica pantalla desde la que se reactivan. */
    @GetMapping("/disabled")
    public List<MedicamentResponse> listDisabled() {
        return listDisabledUseCase.listDisabled().stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public MedicamentResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateGlobalMedicamentRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateGlobalMedicamentCommand(id, request.name(), request.description())));
    }

    /** Baja logica: la fila queda con {@code enabled = false}, no se borra. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public MedicamentResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    /**
     * La empresa siempre sale {@code null} en estas respuestas —un global no tiene
     * ninguna— y aun asi se mapea con el mismo metodo que el controller del tenant:
     * la {@code MedicamentResponse} es una sola y el front no tiene que aprender
     * dos formas del mismo recurso.
     */
    private MedicamentResponse toResponse(MedicamentDto dto) {
        CompanySummaryDto c = dto.company();
        return new MedicamentResponse(dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(), dto.createdDate(), dto.enabled());
    }
}
