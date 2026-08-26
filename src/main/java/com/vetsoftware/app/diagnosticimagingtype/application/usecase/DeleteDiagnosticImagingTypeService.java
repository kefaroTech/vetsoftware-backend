package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeHasActiveChildrenException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.type.delete")
@Service
public class DeleteDiagnosticImagingTypeService implements DeleteDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;
    private final DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort;

    public DeleteDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository,
            DiagnosticImagingChildrenQueryPort diagnosticImagingChildrenQueryPort) {
        this.repository = repository;
        this.diagnosticImagingChildrenQueryPort = diagnosticImagingChildrenQueryPort;
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
                ? repository.findById(id).filter(DiagnosticImagingType::isGeneral)
                : repository.findOwnedByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(id));
        if (diagnosticImagingChildrenQueryPort.existsActiveByDiagnosticImagingTypeId(id)) {
            throw new DiagnosticImagingTypeHasActiveChildrenException(id, "diagnosticImaging");
        }
        repository.delete(id);
    }
}
