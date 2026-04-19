package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindMembershipServiceTest {
    private Membership stored = new Membership(1L, "Gold", MembershipStatus.ACTIVE, LocalDateTime.now(), null);
    private final MembershipRepository repository = new MembershipRepository() {
        @Override public Membership save(Membership m) { return m; }
        @Override public Optional<Membership> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Membership> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final FindMembershipService service = new FindMembershipService(repository);

    @Test
    void find_existing_membership_returns_dto() {
        MembershipDto dto = service.findById(1L);
        assertEquals(1L, dto.id());
        assertEquals("Gold", dto.name());
    }

    @Test
    void find_non_existing_membership_throws() {
        stored = null;
        assertThrows(MembershipNotFoundException.class, () -> service.findById(99L));
    }
}
