package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.Animal;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository {
  Animal save(Animal animal);

  Optional<Animal> findById(Long id);

  Optional<Animal> findByIdAndCompanyId(Long id, Long companyId);

  List<Animal> findAll();

  List<Animal> findAllByCompanyId(Long companyId);

  List<Animal> findByOwnerIdAndCompanyId(Long ownerId, Long companyId);

  void delete(Long id, Long companyId);

  int reactivate(Long id, Long companyId);
}
