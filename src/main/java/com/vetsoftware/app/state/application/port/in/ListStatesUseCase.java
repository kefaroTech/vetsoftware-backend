package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.dto.StateDto;
import java.util.List;

public interface ListStatesUseCase {
  List<StateDto> listAll();
}
