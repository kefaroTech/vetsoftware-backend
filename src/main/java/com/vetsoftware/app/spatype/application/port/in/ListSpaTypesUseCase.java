package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import java.util.List;

public interface ListSpaTypesUseCase {
  List<SpaTypeDto> listAll();
}
