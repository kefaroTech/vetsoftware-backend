package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import java.util.List;

public interface ListSubModulesUseCase {
  List<SubModuleDto> listAll();
}
