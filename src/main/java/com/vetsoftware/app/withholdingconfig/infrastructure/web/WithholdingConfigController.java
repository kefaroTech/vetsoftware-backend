package com.vetsoftware.app.withholdingconfig.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.withholdingconfig.application.command.SetWithholdingConfigCommand;
import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.in.FindWithholdingConfigUseCase;
import com.vetsoftware.app.withholdingconfig.application.port.in.SetWithholdingConfigUseCase;
import com.vetsoftware.app.withholdingconfig.infrastructure.web.request.SetWithholdingConfigRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/withholding-configs")
public class WithholdingConfigController {
  private final SetWithholdingConfigUseCase setUseCase;
  private final FindWithholdingConfigUseCase findUseCase;
  private final Authz authz;

  public WithholdingConfigController(
      SetWithholdingConfigUseCase setUseCase,
      FindWithholdingConfigUseCase findUseCase,
      Authz authz) {
    this.setUseCase = setUseCase;
    this.findUseCase = findUseCase;
    this.authz = authz;
  }

  @PutMapping
  public WithholdingConfigDto set(@Valid @RequestBody SetWithholdingConfigRequest request) {
    return setUseCase.execute(
        new SetWithholdingConfigCommand(
            request.reteFuenteRate(),
            request.reteIvaRate(),
            request.reteIcaRate(),
            authz.currentCompanyId()));
  }

  @GetMapping
  public WithholdingConfigDto find() {
    return findUseCase.findByCompany(authz.currentCompanyId());
  }
}
