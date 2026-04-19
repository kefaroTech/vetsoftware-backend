package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListCompaniesService implements ListCompaniesUseCase {
    private final CompanyRepository repository;

    public ListCompaniesService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CompanyDto> listAll() {
        return repository.findAll().stream().map(CompanyDto::from).toList();
    }
}
