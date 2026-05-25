package com.vetsoftware.app.membershipsubmodule.application.port.out;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import java.util.List;
import java.util.Optional;

public interface MembershipSubModuleRepository {
    MembershipSubModule save(MembershipSubModule membershipSubModule);
    Optional<MembershipSubModule> findById(Long id);
    List<MembershipSubModule> findAll();
    void delete(Long id);
    int reactivate(Long id);
    Optional<Long> findDisabledIdByMembershipAndSubModule(Long membershipId, Long subModuleId);
}
