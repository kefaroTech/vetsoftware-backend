package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.UpdateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.BundleComponentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.bundlecomponent.update")
@Service
public class UpdateBundleComponentService implements UpdateBundleComponentUseCase {

    private final BundleComponentRepository repository;

    public UpdateBundleComponentService(BundleComponentRepository repository) {
        this.repository = repository;
    }

    /**
     * Lo único editable es la cantidad. Cambiar el paquete o la pieza no es editar
     * este componente: es otro componente distinto.
     */
    @Override
    @Transactional
    public BundleComponentDto execute(UpdateBundleComponentCommand command) {
        BundleComponent component = repository.findById(command.id())
                .orElseThrow(() -> new BundleComponentNotFoundException(command.id()));
        if (!component.getBundleItemId().equals(command.bundleItemId())) {
            throw new BundleComponentNotFoundException(command.id());
        }
        component.changeQuantity(command.quantity());
        return BundleComponentDto.from(repository.save(component));
    }
}
