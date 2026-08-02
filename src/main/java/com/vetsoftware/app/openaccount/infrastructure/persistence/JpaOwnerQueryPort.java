package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("openAccountJpaOwnerQueryPort")
public class JpaOwnerQueryPort implements OwnerQueryPort {
  private final OwnerJpaRepository ownerJpaRepository;

  public JpaOwnerQueryPort(OwnerJpaRepository ownerJpaRepository) {
    this.ownerJpaRepository = ownerJpaRepository;
  }

  @Override
  public Optional<OwnerRef> findById(Long ownerId) {
    return ownerJpaRepository
        .findById(ownerId)
        .map(e -> new OwnerRef(e.getId(), e.getName(), e.getDocument()));
  }
}
