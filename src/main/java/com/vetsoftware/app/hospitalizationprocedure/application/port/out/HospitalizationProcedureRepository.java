package com.vetsoftware.app.hospitalizationprocedure.application.port.out;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import java.util.List;
import java.util.Optional;

public interface HospitalizationProcedureRepository {
  HospitalizationProcedure save(HospitalizationProcedure procedure);

  Optional<HospitalizationProcedure> findById(Long id);

  Optional<HospitalizationProcedure> findByIdAndCompanyId(Long id, Long companyId);

  List<HospitalizationProcedure> findAllByHospitalizationId(Long hospitalizationId);

  void delete(Long id);

  int reactivate(Long id);
}
