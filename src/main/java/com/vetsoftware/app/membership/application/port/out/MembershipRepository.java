package com.vetsoftware.app.membership.application.port.out;

import com.vetsoftware.app.membership.domain.Membership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    Membership save(Membership membership);

    Optional<Membership> findById(Long id);

    List<Membership> findAll();

    void delete(Long id);

    int reactivate(Long id);
}
