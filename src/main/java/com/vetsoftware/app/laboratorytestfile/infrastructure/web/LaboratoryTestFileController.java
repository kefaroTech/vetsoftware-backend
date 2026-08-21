package com.vetsoftware.app.laboratorytestfile.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestSummaryDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.CreateLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DeleteLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DownloadLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.in.ListLaboratoryTestFilesByLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytestfile.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.laboratorytestfile.infrastructure.web.response.LaboratoryTestFileResponse;
import com.vetsoftware.app.laboratorytestfile.infrastructure.web.response.LaboratoryTestSummary;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/laboratory-test-files")
public class LaboratoryTestFileController {
    private final CreateLaboratoryTestFileUseCase createUseCase;
    private final ListLaboratoryTestFilesByLaboratoryTestUseCase listByLaboratoryTestUseCase;
    private final DownloadLaboratoryTestFileUseCase downloadUseCase;
    private final DeleteLaboratoryTestFileUseCase deleteUseCase;
    private final Authz authz;

    public LaboratoryTestFileController(CreateLaboratoryTestFileUseCase createUseCase,
            ListLaboratoryTestFilesByLaboratoryTestUseCase listByLaboratoryTestUseCase,
            DownloadLaboratoryTestFileUseCase downloadUseCase,
            DeleteLaboratoryTestFileUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listByLaboratoryTestUseCase = listByLaboratoryTestUseCase;
        this.downloadUseCase = downloadUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryTestFileResponse upload(
            @RequestParam("laboratoryTestId") Long laboratoryTestId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
        String contentType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();
        return toResponse(createUseCase.execute(new CreateLaboratoryTestFileCommand(
                laboratoryTestId, file.getOriginalFilename(), contentType, file.getSize(), content,
                authz.currentEmployeeId(), authz.currentCompanyId())));
    }

    @GetMapping("/by-laboratory-test/{laboratoryTestId}")
    public List<LaboratoryTestFileResponse> listByLaboratoryTest(
            @PathVariable Long laboratoryTestId) {
        return listByLaboratoryTestUseCase
                .listByLaboratoryTest(laboratoryTestId, authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        LaboratoryTestFileDownloadDto dto = downloadUseCase.download(id,
                authz.currentCompanyIdOrNull());
        ContentDisposition disposition = ContentDisposition.attachment().filename(dto.fileName())
                .build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(dto.contentType()))
                .contentLength(dto.content().length).body(new ByteArrayResource(dto.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private LaboratoryTestFileResponse toResponse(LaboratoryTestFileDto dto) {
        EmployeeSummaryDto up = dto.uploadedBy();
        LaboratoryTestSummaryDto lt = dto.laboratoryTest();
        return new LaboratoryTestFileResponse(dto.id(), dto.storageKey(), dto.bucket(),
                dto.originalFileName(), dto.contentType(), dto.sizeBytes(), dto.eTag(),
                new EmployeeSummary(up.id(), up.employeeCode(), up.name()),
                new LaboratoryTestSummary(lt.id(), lt.date()), dto.createdDate());
    }
}
