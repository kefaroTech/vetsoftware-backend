package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.animal.domain.Animal;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository {
    Animal save(Animal animal);

    Optional<Animal> findById(Long id);

    Optional<Animal> findByIdAndCompanyId(Long id, Long companyId);

    List<Animal> findAll();

    PageResult<Animal> findAllByCompanyId(Long companyId, int page, int pageSize);

    List<Animal> findByOwnerIdAndCompanyId(Long ownerId, Long companyId);
}
