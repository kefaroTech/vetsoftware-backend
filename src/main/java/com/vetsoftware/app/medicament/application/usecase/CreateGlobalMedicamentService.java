package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.command.CreateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta en el vademecum de PLATAFORMA. La empresa es {@code null} y
 * {@code general} es {@code true}, los dos escritos aqui y no leidos del
 * command: ese es el punto entero del caso de uso. La invariante del dominio
 * —general implica sin empresa— sigue siendo quien lo comprueba, en el
 * constructor de {@link Medicament}; esto solo garantiza que nadie pueda
 * proponer otra combinacion desde fuera.
 *
 * <p>
 * No inyecta {@code CompanyQueryPort}, al reves que
 * {@link CreateMedicamentService}: no hay empresa que resolver, y tenerlo a
 * mano seria la puerta por la que este servicio acabaria escribiendo la fila de
 * un tenant.
 */
@Observed(name = "medicament.global.create")
@Service
public class CreateGlobalMedicamentService implements CreateGlobalMedicamentUseCase {
    private final MedicamentRepository repository;

    public CreateGlobalMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    /**
     * Mismo contrato que el alta del tenant y por el mismo motivo (#432, #559): el
     * indice unico de la base cubre solo las filas ACTIVAS
     * ({@code uq_medicaments_owner_active_name} sobre la columna generada
     * {@code active_name}), asi que un global pausado NO ocupa su nombre y la
     * respuesta correcta al reencontrarlo es reactivarlo, no insertar otro ni
     * fallar con un 409 que habla de algo que la usuaria no ve.
     *
     * <p>
     * El ambito de la busqueda es el vademecum de plataforma: se pasa {@code null}
     * y el adaptador lo traduce a {@code company_id IS NULL}, nunca a
     * {@code company_id = NULL}, que no casa jamas en SQL. El
     * {@code filter(Medicament::isGeneral)} es redundante con esa consulta y se
     * deja escrito igualmente: afirma que esta rama solo toca filas de la
     * plataforma y sobrevive a que alguien cambie el finder.
     *
     * <p>
     * Que la empresa 7 ya tenga su «Amoxicilina» privada no bloquea esta alta: la
     * clave unica es {@code (owner_scope, active_name)} con
     * {@code owner_scope = COALESCE(company_id, 0)}, asi que la global ocupa
     * {@code (0, ...)} y la privada {@code (7, ...)}. Son claves distintas.
     */
    @Override
    @Transactional
    public MedicamentDto execute(CreateGlobalMedicamentCommand command) {
        Optional<Medicament> existing = repository
                .findByNameAndCompanyIdIncludingDisabled(command.name(), null)
                .filter(Medicament::isGeneral);
        if (existing.isPresent()) {
            return MedicamentDto.from(reactivate(existing.get(), command));
        }
        return MedicamentDto.from(repository
                .save(Medicament.create(command.name(), command.description(), null, true)));
    }

    /**
     * Rama de reactivacion. El {@code update} del dominio va ANTES del UPDATE
     * nativo a proposito: valida el XOR general/empresa y aborta sin haber
     * resucitado nada si el alta es incoherente.
     */
    private Medicament reactivate(Medicament existing, CreateGlobalMedicamentCommand command) {
        if (existing.isEnabled()) {
            throw new MedicamentNameAlreadyExistsException(command.name());
        }
        existing.update(command.name(), command.description(), null, true);
        existing.enable();
        int filas = repository.reactivateWithDetails(existing.getId(), null, command.name(),
                command.description());
        // La fila estaba ahi cuando la leimos, en ESTA misma transaccion: que el
        // UPDATE no alcance ninguna significa que otra operacion la borro entre
        // medias. Devolver el DTO igualmente afirmaria un enabled = true que no esta
        // en la base -el fallo silencioso que la baja logica hace dificil de ver-.
        // Mismo codigo que el candado optimista (409 CONCURRENT_MODIFICATION) porque
        // para el front la accion es la misma: recargar y reintentar.
        if (filas == 0) {
            throw new ObjectOptimisticLockingFailureException(Medicament.class, existing.getId());
        }
        return existing;
    }
}
