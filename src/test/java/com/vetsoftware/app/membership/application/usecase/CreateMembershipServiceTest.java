package com.vetsoftware.app.membership.application.usecase;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.membership.domain.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateMembershipServiceTest {
    private final AtomicLong sequence = new AtomicLong(1);
    private final MembershipRepository repository = new MembershipRepository() {
        @Override public Membership save(Membership m) { return new Membership(sequence.getAndIncrement(), m.getName(), m.getStatus(), m.getCreatedDate(), m.getCreatedBy()); }
        @Override public Optional<Membership> findById(Long id) { return Optional.empty(); }
        @Override public List<Membership> findAll() { return List.of(); }
        @Override public void delete(Long id) {}
    };
    private final CreateMembershipService service = new CreateMembershipService(repository);

    @Test
    void create_membership_saves_and_returns_dto() {
        MembershipDto dto = service.execute(new CreateMembershipCommand("Gold", "ACTIVE", null));
        assertNotNull(dto.id());
        assertEquals("Gold", dto.name());
        assertEquals(MembershipStatus.ACTIVE, dto.status());
        assertNotNull(dto.createdDate());
    }

    @Test
    void create_membership_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateMembershipCommand("", "ACTIVE", null)));
    }

    @Test
    void create_membership_with_null_name_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            service.execute(new CreateMembershipCommand(null, "ACTIVE", null)));
    }
}
