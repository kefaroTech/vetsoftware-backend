package com.vetsoftware.app.publishadminpermissions.application.port.out;

import java.util.Map;
import java.util.Set;

public interface MembershipSubModuleIdsQueryPort {
    Map<Long, Set<Long>> findSubModuleIdsByMembershipIds(Set<Long> membershipIds);
}
