package com.vetsoftware.app.limitdimension.application.usecase;

import com.vetsoftware.app.limitdimension.application.command.UpdateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.application.port.in.UpdateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.out.LimitDimensionRepository;
import com.vetsoftware.app.limitdimension.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.LimitDimensionNotFoundException;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edita un eje limitable.
 *
 * <p>
 * El submódulo se resuelve por puerto y no se confía, igual que al declararlo:
 * apuntar a uno inexistente moriría en la clave foránea a mitad de transacción,
 * y un error del motor no le dice a nadie qué corregir.
 *
 * <p>
 * <strong>Un submódulo vacío es una decisión, no un olvido</strong>: hay ejes
 * que no cuelgan de ninguno —los que cuentan cosas del núcleo— y por eso el
 * {@code null} se propaga sin consultar nada. Lo que la validación cruzada de
 * los días de enfriamiento decide sigue viviendo en el constructor del dominio,
 * que es donde el {@code CLAUDE.md} pone las invariantes.
 *
 * <p>
 * Carga por id sin acotar por empresa, y es correcto: la tabla es catálogo
 * global y su puerto está cerrado a un principal cross-tenant, que no tiene
 * empresa con la que acotar.
 */
@Service
public class UpdateLimitDimensionService implements UpdateLimitDimensionUseCase {

    private final LimitDimensionRepository repository;
    private final SubModuleQueryPort subModuleQueryPort;

    public UpdateLimitDimensionService(LimitDimensionRepository repository,
            SubModuleQueryPort subModuleQueryPort) {
        this.repository = repository;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    @Transactional
    public LimitDimensionDto execute(UpdateLimitDimensionCommand command) {
        LimitDimension dimension = repository.findById(command.id())
                .orElseThrow(() -> new LimitDimensionNotFoundException(command.id()));
        SubModuleRef subModule = command.subModuleId() == null
                ? null
                : subModuleQueryPort.findById(command.subModuleId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Sub module " + command.subModuleId() + " not found"));
        dimension.update(command.name(), subModule, command.releaseDelayDays());
        return LimitDimensionDto.from(repository.save(dimension));
    }
}
