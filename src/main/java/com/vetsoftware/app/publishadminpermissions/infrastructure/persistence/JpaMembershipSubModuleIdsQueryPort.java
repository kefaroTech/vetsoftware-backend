package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaEntity;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.MembershipSubModuleIdsQueryPort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JpaMembershipSubModuleIdsQueryPort implements MembershipSubModuleIdsQueryPort {

    private final MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;

    public JpaMembershipSubModuleIdsQueryPort(MembershipSubModuleJpaRepository membershipSubModuleJpaRepository) {
        this.membershipSubModuleJpaRepository = membershipSubModuleJpaRepository;
    }

    @Override
    public Map<Long, Set<Long>> findSubModuleIdsByMembershipIds(Set<Long> membershipIds) {
        if (membershipIds.isEmpty()) return Map.of();
        Map<Long, Set<Long>> result = new HashMap<>();
        for (MembershipSubModuleJpaEntity msm : membershipSubModuleJpaRepository.findByMembershipIdIn(membershipIds)) {
            result.computeIfAbsent(msm.getMembership().getId(), k -> new HashSet<>())
                  .add(msm.getSubModule().getId());
        }
        return result;
    }
}
