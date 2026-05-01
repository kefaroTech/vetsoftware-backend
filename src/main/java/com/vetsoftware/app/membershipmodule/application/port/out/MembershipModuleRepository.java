package com.vetsoftware.app.membershipmodule.application.port.out;

import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import java.util.List;
import java.util.Optional;

public interface MembershipModuleRepository {
    MembershipModule save(MembershipModule membershipModule);
    Optional<MembershipModule> findById(Long id);
    List<MembershipModule> findAll();
    void delete(Long id);
}
