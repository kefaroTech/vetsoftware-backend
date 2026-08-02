package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalHistoryReportModel;

public interface ClinicalHistoryPdfPort {
  byte[] render(ClinicalHistoryReportModel model);
}
