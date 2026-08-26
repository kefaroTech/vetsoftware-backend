package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.type.delete")
@Service
public class DeleteVaccinationTypeService implements DeleteVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;
    private final VaccinationChildrenQueryPort vaccinationChildrenQueryPort;

    public DeleteVaccinationTypeService(VaccinationTypeRepository repository,
            VaccinationChildrenQueryPort vaccinationChildrenQueryPort) {
        this.repository = repository;
        this.vaccinationChildrenQueryPort = vaccinationChildrenQueryPort;
    }

    /**
     * {@code companyId} null = caller sin empresa (SYSTEM), único que puede borrar
     * una fila general. Con empresa, la lectura previa va al finder ESTRICTO: el
     * tipo de otro tenant y el general compartido son ambos un 404, no un borrado.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        // El .filter es la barrera, no un adorno: sin el, un DELETE de plataforma con
        // el
        // id de una fila PRIVADA la cargaba y la daba de baja. 204, sin error, y la
        // clinica dejaba de verla por el @SQLRestriction — mas silencioso todavia que
        // la
        // expropiacion del update, porque alli al menos la fila reaparecia en el
        // catalogo
        // global. Este camino ya era alcanzable ANTES de #565: el delete de estos
        // controllers usaba currentCompanyIdOrNull() desde el principio. Con el filtro,
        // el
        // codigo hace por fin lo que su propio javadoc dice: SYSTEM borra filas
        // generales.
        // Si algun dia plataforma necesita retirar la fila de un tenant como operacion
        // de
        // soporte, va en un caso de uso aparte y auditado (#584), no en este.
        (companyId == null
                ? repository.findById(id).filter(VaccinationType::isGeneral)
                : repository.findOwnedByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new VaccinationTypeNotFoundException(id));
        if (vaccinationChildrenQueryPort.existsActiveByVaccinationTypeId(id)) {
            throw new VaccinationTypeHasActiveChildrenException(id, "vaccination");
        }
        repository.delete(id);
    }
}
