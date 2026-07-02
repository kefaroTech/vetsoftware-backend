package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateWeightRecordUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('animal.create') and @authz.isMyCompany(#command.companyId))")
    WeightRecordDto execute(CreateWeightRecordCommand command);
}
