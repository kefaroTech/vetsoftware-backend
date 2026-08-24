package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.port.in.DeleteBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.BundleComponentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.bundlecomponent.delete")
@Service
public class DeleteBundleComponentService implements DeleteBundleComponentUseCase {

    private final BundleComponentRepository repository;

    public DeleteBundleComponentService(BundleComponentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long bundleItemId, Long id) {
        BundleComponent component = repository.findById(id)
                .orElseThrow(() -> new BundleComponentNotFoundException(id));
        if (!component.getBundleItemId().equals(bundleItemId)) {
            throw new BundleComponentNotFoundException(id);
        }
        repository.delete(id);
    }
}
