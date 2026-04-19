package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
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

class UpdateMembershipServiceTest {
    private Membership stored = new Membership(1L, "Gold", MembershipStatus.ACTIVE, LocalDateTime.now(), null);
    private final MembershipRepository repository = new MembershipRepository() {
        @Override public Membership save(Membership m) { return m; }
        @Override public Optional<Membership> findById(Long id) { return Optional.ofNullable(stored); }
        @Override public List<Membership> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final UpdateMembershipService service = new UpdateMembershipService(repository);

    @Test
    void update_existing_membership_returns_updated_dto() {
        MembershipDto dto = service.execute(new UpdateMembershipCommand(1L, "Platinum", "INACTIVE"));
        assertEquals("Platinum", dto.name());
        assertEquals(MembershipStatus.INACTIVE, dto.status());
    }

    @Test
    void update_non_existing_membership_throws() {
        stored = null;
        assertThrows(MembershipNotFoundException.class, () ->
            service.execute(new UpdateMembershipCommand(99L, "Platinum", "ACTIVE")));
    }

    @Test
    void update_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new UpdateMembershipCommand(1L, "", "ACTIVE")));
    }
}
