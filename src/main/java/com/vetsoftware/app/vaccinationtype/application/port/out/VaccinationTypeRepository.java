package com.vetsoftware.app.vaccinationtype.application.port.out;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.util.List;
import java.util.Optional;

public interface VaccinationTypeRepository {
  VaccinationType save(VaccinationType vaccinationType);

  Optional<VaccinationType> findById(Long id);

  Optional<VaccinationType> findByIdAndCompanyId(Long id, Long companyId);

  List<VaccinationType> findAll();

  List<VaccinationType> findAllAvailableForCompany(Long companyId);

  void delete(Long id);

  int reactivate(Long id);
}
