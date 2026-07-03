package com.vetsoftware.app.animalalert.application.port.out;

import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import java.util.List;
import java.util.Optional;

public interface AnimalAlertRepository {
    AnimalAlert save(AnimalAlert alert);
    Optional<AnimalAlert> findByIdAndCompanyId(Long id, Long companyId);
    List<AnimalAlert> findByAnimalIdAndCompanyId(Long animalId, Long companyId);
    void delete(Long id, Long companyId);
}
