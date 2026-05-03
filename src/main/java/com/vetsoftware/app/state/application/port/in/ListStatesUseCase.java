package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.dto.StateDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListStatesUseCase {
    List<StateDto> listAll();
}
