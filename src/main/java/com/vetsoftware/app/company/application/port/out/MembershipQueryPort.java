package com.vetsoftware.app.company.application.port.out;

import com.vetsoftware.app.company.domain.MembershipRef;
import java.util.Optional;

public interface MembershipQueryPort {
    Optional<MembershipRef> findById(Long membershipId);
}
