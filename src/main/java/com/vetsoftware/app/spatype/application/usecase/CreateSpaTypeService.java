package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.CreateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import com.vetsoftware.app.spatype.domain.SpaTypeNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.type.create")
@Service
public class CreateSpaTypeService implements CreateSpaTypeUseCase {
    private final SpaTypeRepository repository;

    public CreateSpaTypeService(SpaTypeRepository repository) {
        this.repository = repository;
    }

    /**
     * El alta comprueba que el nombre esté libre y, si lo ocupa una fila dada de
     * baja, la reactiva en vez de insertar otra.
     *
     * <p>
     * Sin la guarda el choque lo detectaba solo la base y salía como un 409 con
     * {@code "Database constraint violation"}: en inglés, sin nombrar el campo y
     * sin código de negocio (#559). Sin la rama de reactivación el mensaje era
     * además engañoso: la fila que ocupaba el nombre estaba deshabilitada, no
     * aparecía en el listado —{@code @SQLRestriction("enabled = true")}— y el
     * sistema hablaba de un conflicto con algo invisible. La baja lógica LIBERA el
     * nombre, así que reactivar es la respuesta correcta (#432).
     *
     * <p>
     * La guarda NO sustituye al mapeo de la constraint en el handler: no cierra la
     * carrera entre dos altas simultáneas (#437).
     */
    @Override
    @Transactional
    public SpaTypeDto execute(CreateSpaTypeCommand command) {
        Optional<SpaType> existing = repository.findByNameIncludingDisabled(command.name());
        if (existing.isPresent()) {
            return SpaTypeDto.from(reactivate(existing.get(), command));
        }
        return SpaTypeDto
                .from(repository.save(SpaType.create(command.name(), command.description())));
    }

    /**
     * Rama de reactivación. El {@code update} del dominio va ANTES del UPDATE
     * nativo a propósito: valida el nombre y la descripción y aborta sin haber
     * resucitado nada si el alta es incoherente.
     */
    private SpaType reactivate(SpaType existing, CreateSpaTypeCommand command) {
        if (existing.isEnabled()) {
            throw new SpaTypeNameAlreadyExistsException(command.name());
        }
        existing.update(command.name(), command.description());
        existing.enable();
        int filas = repository.reactivateWithDetails(existing.getId(), command.name(),
                command.description());
        // La fila estaba ahi cuando la leimos, en ESTA misma transaccion: que el UPDATE
        // no alcance ninguna significa que otra operacion la borro o le cambio el dueno
        // entre medias. Devolver el DTO igualmente afirmaria un enabled = true que no
        // esta en la base -el fallo silencioso que la baja logica hace dificil de ver-.
        // Mismo codigo que el candado optimista (409 CONCURRENT_MODIFICATION) porque
        // para el front la accion es la misma: recargar y reintentar.
        if (filas == 0) {
            throw new ObjectOptimisticLockingFailureException(SpaType.class, existing.getId());
        }
        return existing;
    }
}
