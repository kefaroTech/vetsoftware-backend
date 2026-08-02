package com.vetsoftware.app.branch.application.usecase;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.in.ListBranchesUseCase;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "branch.list")
@Service
public class ListBranchesService implements ListBranchesUseCase {
  private final BranchRepository repository;

  public ListBranchesService(BranchRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<BranchDto> listAll(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(BranchDto::from).toList();
  }
}
