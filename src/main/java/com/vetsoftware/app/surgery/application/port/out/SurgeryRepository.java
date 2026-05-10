package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.Surgery;
import java.util.List;
import java.util.Optional;

public interface SurgeryRepository {
    Surgery save(Surgery surgery);
    Optional<Surgery> findById(Long id);
    List<Surgery> findAll();
    void delete(Long id);
}
