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
                ? repository.findById(command.id())
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
