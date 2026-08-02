package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationobservation.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import org.springframework.stereotype.Component;

@Component
public class HospitalizationObservationJpaMapper {

  public HospitalizationObservationJpaEntity toJpa(
      HospitalizationObservation observation,
      HospitalizationJpaEntity hospitalization,
      EmployeeJpaEntity createdBy) {
    HospitalizationObservationJpaEntity entity = new HospitalizationObservationJpaEntity();
    entity.setId(observation.getId());
    entity.setDescription(observation.getDescription());
    entity.setHospitalization(hospitalization);
    entity.setCreatedBy(createdBy);
    entity.setCreatedDate(observation.getCreatedDate());
    entity.setEnabled(observation.isEnabled());
    return entity;
  }

  // Read path — el @EntityGraph ya hidrató hospitalization y createdBy
  public HospitalizationObservation toDomain(HospitalizationObservationJpaEntity entity) {
    HospitalizationJpaEntity h = entity.getHospitalization();
    EmployeeJpaEntity e = entity.getCreatedBy();
    return toDomain(
        entity,
        new HospitalizationRef(h.getId(), h.getDate()),
        new EmployeeRef(e.getId(), e.getEmployeeCode(), e.getName()));
  }

  // Write path — reusa los refs precargados, evita inicializar el proxy de getReferenceById
  public HospitalizationObservation toDomain(
      HospitalizationObservationJpaEntity entity,
      HospitalizationRef hospitalizationRef,
      EmployeeRef createdByRef) {
    return new HospitalizationObservation(
        entity.getId(),
        entity.getDescription(),
        hospitalizationRef,
        createdByRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
