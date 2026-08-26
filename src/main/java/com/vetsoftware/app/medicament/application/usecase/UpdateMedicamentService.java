package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.UpdateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNameAlreadyExistsException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.update")
@Service
public class UpdateMedicamentService implements UpdateMedicamentUseCase {
    private final MedicamentRepository repository;

    public UpdateMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    /**
     * La carga va acotada a la empresa. El {@code isMyCompany} del puerto solo
     * prueba que el llamante declara SU empresa; con un {@code findById} pelado el
     * efecto no era un rechazo sino una edicion del vademecum de otro tenant.
     *
     * <p>
     * Se usa el finder de lo PROPIO, no el de lo disponible: un medicamento general
     * es de la plataforma y no lo edita ningun tenant. {@code companyId == null} es
     * el camino SYSTEM.
     *
     * <p>
     * El camino SYSTEM filtra ademas por {@code isGeneral}, y el filtro no es
     * defensa en profundidad: es la barrera (#590). Un principal de plataforma no
     * tiene empresa que acotar, asi que sin el, un PUT con el id de una fila
     * PRIVADA la cargaba y la reescribia. Aqui el dano es mas sutil que en el
     * delete —este servicio conserva el scope de la fila, no lo reescribe—, pero
     * sigue siendo una edicion del vademecum de una clinica hecha desde una consola
     * que no deberia poder tocarlo. La administracion del catalogo global vive en
     * {@code UpdateGlobalMedicamentService}. Un 404 y no un 403: no se revela de
     * quien es la fila.
     *
     * <p>
     * La guarda de nombre mira el ambito de la FILA
     * —{@code medicament.getCompany()}— y no el del command: la edicion conserva el
     * scope del medicamento, asi que el nombre tiene que estar libre donde la fila
     * ya vive. Con el companyId del command el camino SYSTEM ({@code null}) habria
     * comprobado el vademecum de plataforma mientras editaba una fila de empresa.
     */
    @Override
    @Transactional
    public MedicamentDto execute(UpdateMedicamentCommand command) {
        Medicament medicament = (command.companyId() == null
                ? repository.findById(command.id()).filter(Medicament::isGeneral)
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new MedicamentNotFoundException(command.id()));
        Long scopeCompanyId = medicament.getCompany() == null ? null : medicament.getCompany().id();
        if (repository.existsActiveByNameAndCompanyIdExcludingId(command.name(), scopeCompanyId,
                command.id())) {
            throw new MedicamentNameAlreadyExistsException(command.name());
        }
        // Solo nombre/descripción; se conserva el scope (general/empresa) del
        // medicamento.
        medicament.update(command.name(), command.description(), medicament.getCompany(),
                medicament.isGeneral());
        return MedicamentDto.from(repository.save(medicament));
    }
}
