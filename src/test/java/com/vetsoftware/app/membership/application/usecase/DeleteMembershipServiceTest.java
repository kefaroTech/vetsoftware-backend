package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipNotFoundException;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeleteMembershipServiceTest {
    private Membership stored = new Membership(1L, "Gold", MembershipStatus.ACTIVE, LocalDateTime.now(), null);
    private boolean deleted;
    private final MembershipRepository repository = new MembershipRepository() {
        @Override public Membership save(Membership m) { return m; }
        @Override public Optional<Membership> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Membership> findAll() { return List.of(); }
        @Override public void delete(Long id) { deleted = true; }
    };
    private final DeleteMembershipService service = new DeleteMembershipService(repository);

    @Test
    void delete_existing_membership_removes_it() {
        service.execute(1L);
        assertTrue(deleted);
    }

    @Test
    void delete_non_existing_membership_throws() {
        stored = null;
        assertThrows(MembershipNotFoundException.class, () -> service.execute(99L));
        assertFalse(deleted);
    }
}
