package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ListMembershipsServiceTest {
    private final MembershipRepository repository = new MembershipRepository() {
        @Override public Membership save(Membership m) { return m; }
        @Override public Optional<Membership> findById(Long id) { return Optional.empty(); }
        @Override public List<Membership> findAll() {
            return List.of(
                new Membership(1L, "Gold", MembershipStatus.ACTIVE, LocalDateTime.now(), null),
                new Membership(2L, "Platinum", MembershipStatus.INACTIVE, LocalDateTime.now(), null)
            );
        }
        @Override public void delete(Long id) {}
    };
    private final ListMembershipsService service = new ListMembershipsService(repository);

    @Test
    void list_all_returns_all_memberships() {
        List<MembershipDto> result = service.listAll();
        assertEquals(2, result.size());
        assertEquals("Gold", result.get(0).name());
        assertEquals("Platinum", result.get(1).name());
    }
}
