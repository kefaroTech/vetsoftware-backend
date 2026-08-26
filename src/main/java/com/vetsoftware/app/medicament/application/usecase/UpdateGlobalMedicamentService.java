package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.command.UpdateGlobalMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.UpdateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edicion en el vademecum de PLATAFORMA.
 *
 * <p>
 * El {@code filter(Medicament::isGeneral)} NO es defensa en profundidad: es LA
 * barrera. El id lo escribe el cliente en la URL y este puerto no recibe
 * empresa, asi que sin el filtro un PUT de plataforma con el id de una fila
 * PRIVADA la cargaria, y el {@code update} posterior le pondria
 * {@code company = null} y {@code general = true}: el medicamento privado de
 * una clinica pasaria en silencio al catalogo global, visible para todos los
 * tenants. Es el mismo razonamiento —y el mismo arreglo— que
 * {@code UpdateLaboratoryTestTypeService}. Un 404 y no un 403: no se revela de
 * quien es la fila.
 */
@Observed(name = "medicament.global.update")
@Service
public class UpdateGlobalMedicamentService implements UpdateGlobalMedicamentUseCase {
    private final MedicamentRepository repository;

    public UpdateGlobalMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MedicamentDto execute(UpdateGlobalMedicamentCommand command) {
        Medicament medicament = repository.findById(command.id()).filter(Medicament::isGeneral)
                .orElseThrow(() -> new MedicamentNotFoundException(command.id()));
        // Ambito nulo: la unicidad se comprueba contra el vademecum de plataforma,
        // que es donde la fila vive y donde va a seguir viviendo -este caso de uso
        // nunca cambia el scope-. Solo cuentan las ACTIVAS, que son las unicas que
        // el indice unico de la base cuenta.
        if (repository.existsActiveByNameAndCompanyIdExcludingId(command.name(), null,
                command.id())) {
            throw new MedicamentNameAlreadyExistsException(command.name());
        }
        // Solo nombre y descripcion. El scope se reafirma global y no se toma del
        // command, que ni siquiera lo transporta.
        medicament.update(command.name(), command.description(), null, true);
        return MedicamentDto.from(repository.save(medicament));
    }
}
