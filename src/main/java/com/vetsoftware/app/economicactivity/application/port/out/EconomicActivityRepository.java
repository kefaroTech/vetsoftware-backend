package com.vetsoftware.app.economicactivity.application.port.out;

import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import java.util.List;
import java.util.Optional;

public interface EconomicActivityRepository {
  EconomicActivity save(EconomicActivity economicActivity);

  Optional<EconomicActivity> findById(Long id);

  List<EconomicActivity> findAll();

  void delete(Long id);

  int reactivate(Long id);

  boolean existsByCode(String code);
}
