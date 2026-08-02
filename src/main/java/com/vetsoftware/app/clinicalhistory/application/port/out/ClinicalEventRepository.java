package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import java.util.List;

public interface ClinicalEventRepository {
  List<ClinicalEvent> findHistory(GetClinicalHistoryQuery query);

  List<ClinicalEvent> findByCompany(ListCompanyClinicalEventsQuery query);
}
