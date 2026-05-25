package com.vetsoftware.app.membership.application.port.out;

public interface MembershipSubModuleChildrenQueryPort {
    boolean existsActiveByMembershipId(Long parentId);
}
