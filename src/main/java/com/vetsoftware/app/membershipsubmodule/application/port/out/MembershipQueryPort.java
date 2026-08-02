package com.vetsoftware.app.membershipsubmodule.application.port.out;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import java.util.Optional;

public interface MembershipQueryPort {
  Optional<MembershipRef> findById(Long membershipId);
}
