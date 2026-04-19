package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.application.port.out.CompanyRepository;
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
