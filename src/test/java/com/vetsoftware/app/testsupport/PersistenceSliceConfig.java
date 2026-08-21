package com.vetsoftware.app.testsupport;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaMapper;
import com.vetsoftware.app.animal.infrastructure.persistence.JpaAnimalRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.JpaWeightRecordRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaMapper;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaMapper;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.JpaAnimalColorRepository;
import com.vetsoftware.app.appointment.infrastructure.persistence.AppointmentJpaMapper;
import com.vetsoftware.app.appointment.infrastructure.persistence.JpaAppointmentRepository;
import com.vetsoftware.app.auth.infrastructure.persistence.JpaSystemUserCredentialsRepository;
import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaMapper;
import com.vetsoftware.app.basepermission.infrastructure.persistence.JpaBasePermissionRepository;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaMapper;
import com.vetsoftware.app.baserole.infrastructure.persistence.JpaBaseRoleRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaMapper;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.JpaBaseRolePermissionRepository;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaMapper;
import com.vetsoftware.app.branch.infrastructure.persistence.JpaBranchRepository;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaMapper;
import com.vetsoftware.app.breed.infrastructure.persistence.JpaBreedRepository;
import com.vetsoftware.app.cashregister.infrastructure.persistence.JpaCashSessionRepository;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaMapper;
import com.vetsoftware.app.city.infrastructure.persistence.JpaCityRepository;
import com.vetsoftware.app.clinicalhistory.infrastructure.persistence.ClinicalEventJpaMapper;
import com.vetsoftware.app.clinicalhistory.infrastructure.persistence.JpaClinicalEventRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaMapper;
import com.vetsoftware.app.company.infrastructure.persistence.JpaCompanyRepository;
import com.vetsoftware.app.companysettings.infrastructure.persistence.JpaCompanySettingRepository;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaMapper;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.JpaCompanyTaxProfileRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaMapper;
import com.vetsoftware.app.consultation.infrastructure.persistence.JpaConsultationRepository;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaMapper;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.JpaConsultationTypeRepository;
import com.vetsoftware.app.country.infrastructure.persistence.CountryJpaMapper;
import com.vetsoftware.app.country.infrastructure.persistence.JpaCountryRepository;
import com.vetsoftware.app.daycare.infrastructure.persistence.DayCareJpaMapper;
import com.vetsoftware.app.daycare.infrastructure.persistence.JpaDayCareRepository;
import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.DebtOpenAccountJpaMapper;
import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.JpaDebtOpenAccountRepository;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaMapper;
import com.vetsoftware.app.deworming.infrastructure.persistence.JpaDewormingRepository;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaMapper;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.JpaDiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaMapper;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.JpaDiagnosticImagingTypeRepository;
import com.vetsoftware.app.dianprovider.infrastructure.persistence.DianProviderConfigJpaMapper;
import com.vetsoftware.app.dianprovider.infrastructure.persistence.JpaDianProviderConfigRepository;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaMapper;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.JpaEconomicActivityRepository;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaMapper;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.JdbcDianJobLeasePort;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.JpaElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.JpaNumberingAllocationPort;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.JpaSalePromotionQueryPort;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaMapper;
import com.vetsoftware.app.employee.infrastructure.persistence.JpaEmployeeRepository;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.JpaEmployeeBranchRepository;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaMapper;
import com.vetsoftware.app.employeerole.infrastructure.persistence.JpaEmployeeRoleRepository;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.GeneralChargeOpenAccountJpaMapper;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.JpaGeneralChargeOpenAccountRepository;
import com.vetsoftware.app.goodsreceipt.infrastructure.persistence.GoodsReceiptJpaMapper;
import com.vetsoftware.app.goodsreceipt.infrastructure.persistence.JpaGoodsReceiptRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaMapper;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.JpaHospitalizationRepository;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaMapper;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.JpaHospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.HospitalizationObservationJpaMapper;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.JpaHospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.JpaHospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaMapper;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.JpaHospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence.HospitalizationProgressNoteJpaMapper;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence.JpaHospitalizationProgressNoteRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.JpaInventoryCountRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.JpaStockBalanceRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.JpaStockLotRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.JpaStockMovementRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockQueryAdapter;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.JpaLaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaMapper;
import com.vetsoftware.app.laboratorytestfile.infrastructure.persistence.JpaLaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.infrastructure.persistence.JpaLaboratoryTestQueryPort;
import com.vetsoftware.app.laboratorytestfile.infrastructure.persistence.LaboratoryTestFileJpaMapper;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.JpaLaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaMapper;
import com.vetsoftware.app.medicament.infrastructure.persistence.JpaMedicamentRepository;
import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaMapper;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.JpaMedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaMapper;
import com.vetsoftware.app.medicationschedule.infrastructure.persistence.JpaMedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.infrastructure.persistence.MedicationScheduleJpaMapper;
import com.vetsoftware.app.membership.infrastructure.persistence.JpaMembershipRepository;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaMapper;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.JpaMembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaMapper;
import com.vetsoftware.app.module.infrastructure.persistence.JpaModuleRepository;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaMapper;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.JpaNumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.infrastructure.persistence.NumberingResolutionJpaMapper;
import com.vetsoftware.app.openaccount.infrastructure.persistence.JpaOpenAccountRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.JpaOpenAccountTotalsAdapter;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaMapper;
import com.vetsoftware.app.owner.infrastructure.persistence.JpaOwnerRepository;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaMapper;
import com.vetsoftware.app.passwordreset.infrastructure.persistence.JpaPasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.infrastructure.persistence.PasswordResetTokenJpaMapper;
import com.vetsoftware.app.permission.infrastructure.persistence.JpaPermissionRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaMapper;
import com.vetsoftware.app.prescription.infrastructure.persistence.JpaPrescriptionRepository;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaMapper;
import com.vetsoftware.app.procedureschedule.infrastructure.persistence.JpaProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.infrastructure.persistence.ProcedureScheduleJpaMapper;
import com.vetsoftware.app.product.infrastructure.persistence.JpaProductRepository;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaMapper;
import com.vetsoftware.app.productcategory.infrastructure.persistence.JpaProductCategoryRepository;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaMapper;
import com.vetsoftware.app.promotion.infrastructure.persistence.JpaPromotionRepository;
import com.vetsoftware.app.promotion.infrastructure.persistence.PromotionJpaMapper;
import com.vetsoftware.app.purchaseorder.infrastructure.persistence.JpaPurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.infrastructure.persistence.PurchaseOrderJpaMapper;
import com.vetsoftware.app.role.infrastructure.persistence.JpaRoleRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaMapper;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.JpaRolePermissionRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaMapper;
import com.vetsoftware.app.service.infrastructure.persistence.JpaServiceRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaMapper;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.JpaServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaMapper;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.JpaServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.ServiceChargeOpenAccountJpaMapper;
import com.vetsoftware.app.spa.infrastructure.persistence.JpaSpaRepository;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaMapper;
import com.vetsoftware.app.specie.infrastructure.persistence.JpaSpecieRepository;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaMapper;
import com.vetsoftware.app.state.infrastructure.persistence.JpaStateRepository;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaMapper;
import com.vetsoftware.app.submodule.infrastructure.persistence.JpaSubModuleRepository;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaMapper;
import com.vetsoftware.app.supplier.infrastructure.persistence.JpaSupplierRepository;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaMapper;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.JpaSupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoiceJpaMapper;
import com.vetsoftware.app.surgery.infrastructure.persistence.JpaSurgeryRepository;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaMapper;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.JpaSurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaMapper;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.JpaSystemConfigurationRepository;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaMapper;
import com.vetsoftware.app.systempermission.infrastructure.persistence.JpaSystemPermissionRepository;
import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaMapper;
import com.vetsoftware.app.systemuser.infrastructure.persistence.JpaSystemUserRepository;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaMapper;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.JpaSystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaMapper;
import com.vetsoftware.app.tax.infrastructure.persistence.JpaTaxRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaMapper;
import com.vetsoftware.app.vaccination.infrastructure.persistence.JpaVaccinationRepository;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaMapper;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.JpaVaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaMapper;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.JpaWithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.WithholdingConfigJpaMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Gemelo de persistencia de {@link WebMvcSliceConfig}: los adaptadores y
 * mappers que las rodajas {@code @DataJpaTest} necesitan, reunidos en UNA sola
 * configuracion para que todas compartan contexto.
 *
 * <p>
 * <b>Por que existe.</b> La clave del {@code MergedContextConfiguration}
 * incluye el conjunto de clases importadas ({@code ImportsContextCustomizer}).
 * Con cada rodaja declarando su propio {@code @Import} —el adaptador y el
 * mapper de su feature— cada una producia una clave distinta: 90 claves para 90
 * rodajas, 90 arranques de contexto y cero aciertos de cache. Importando
 * siempre esta misma clase la clave es identica en todas y el
 * {@code DefaultContextCache} sirve un unico contexto.
 *
 * <p>
 * <b>Que NO cambia.</b> Cada rodaja sigue inyectando su adaptador real y
 * ejercitando su SQL contra el MySQL de {@link AbstractDataJpaTest}. Aqui solo
 * cambia que beans hay registrados en el contexto, no que hacen los tests
 * contra la base: el aislamiento lo sigue dando el rollback transaccional de
 * {@code @DataJpaTest}, intacto. Los beans que una rodaja no usa no la afectan.
 *
 * <p>
 * <b>Por que {@code @Import} y no {@code @ComponentScan}.</b> El vertical
 * slicing repite nombres de clase entre features: hay dieciseis
 * {@code JpaEmployeeQueryPort}, cuatro {@code JpaHospitalizationQueryPort} y
 * cuatro {@code JpaOpenAccountQueryPort}, cada uno en su paquete. Un
 * {@code @ComponentScan} los nombraria a todos por su nombre simple y moriria
 * con {@code ConflictingBeanDefinitionException}; las clases importadas las
 * nombra {@code FullyQualifiedAnnotationBeanNameGenerator}, asi que conviven.
 * Por eso los nombres repetidos aparecen abajo cualificados: en Java no se
 * pueden importar dos clases con el mismo nombre simple.
 *
 * <p>
 * <b>Al anadir una rodaja nueva.</b> Anade aqui su adaptador y su mapper y deja
 * la rodaja con {@code @Import(PersistenceSliceConfig.class)} pelado. Si la
 * rodaja declara ademas un {@code @Import} propio vuelve a tener clave unica y
 * se paga otro arranque de contexto entero.
 */
@TestConfiguration
@Import({AnimalColorJpaMapper.class, AnimalJpaMapper.class, AppointmentJpaMapper.class,
        BasePermissionJpaMapper.class, BaseRoleJpaMapper.class, BaseRolePermissionJpaMapper.class,
        BranchJpaMapper.class, BreedJpaMapper.class, CityJpaMapper.class,
        ClinicalEventJpaMapper.class, CompanyJpaMapper.class, CompanyTaxProfileJpaMapper.class,
        ConsultationJpaMapper.class, ConsultationTypeJpaMapper.class, CountryJpaMapper.class,
        DayCareJpaMapper.class, DebtOpenAccountJpaMapper.class, DewormingJpaMapper.class,
        DiagnosticImagingJpaMapper.class, DiagnosticImagingTypeJpaMapper.class,
        DianProviderConfigJpaMapper.class, EconomicActivityJpaMapper.class,
        ElectronicDocumentJpaMapper.class, EmployeeJpaMapper.class, EmployeeRoleJpaMapper.class,
        GeneralChargeOpenAccountJpaMapper.class, GoodsReceiptJpaMapper.class,
        HospitalizationJpaMapper.class, HospitalizationMedicationJpaMapper.class,
        HospitalizationObservationJpaMapper.class, HospitalizationProcedureJpaMapper.class,
        HospitalizationProgressNoteJpaMapper.class, JdbcDianJobLeasePort.class,
        JpaAnimalColorRepository.class, JpaAnimalRepository.class, JpaAppointmentRepository.class,
        JpaBasePermissionRepository.class, JpaBaseRolePermissionRepository.class,
        JpaBaseRoleRepository.class, JpaBranchRepository.class, JpaBreedRepository.class,
        JpaCashSessionRepository.class, JpaCityRepository.class, JpaClinicalEventRepository.class,
        JpaCompanyRepository.class, JpaCompanySettingRepository.class,
        JpaCompanyTaxProfileRepository.class, JpaConsultationRepository.class,
        JpaConsultationTypeRepository.class, JpaCountryRepository.class, JpaDayCareRepository.class,
        JpaDebtOpenAccountRepository.class, JpaDewormingRepository.class,
        JpaDiagnosticImagingRepository.class, JpaDiagnosticImagingTypeRepository.class,
        JpaDianProviderConfigRepository.class, JpaEconomicActivityRepository.class,
        JpaElectronicDocumentRepository.class, JpaEmployeeBranchRepository.class,
        JpaEmployeeRepository.class, JpaEmployeeRoleRepository.class,
        JpaGeneralChargeOpenAccountRepository.class, JpaGoodsReceiptRepository.class,
        JpaHospitalizationMedicationRepository.class, JpaHospitalizationObservationRepository.class,
        JpaHospitalizationProcedureRepository.class, JpaHospitalizationProgressNoteRepository.class,
        JpaHospitalizationQueryPort.class, JpaHospitalizationRepository.class,
        JpaInventoryCountRepository.class, JpaLaboratoryTestFileRepository.class,
        JpaLaboratoryTestQueryPort.class, JpaLaboratoryTestRepository.class,
        JpaLaboratoryTestTypeRepository.class, JpaMedicamentPrescriptionRepository.class,
        JpaMedicamentRepository.class, JpaMedicationScheduleRepository.class,
        JpaMembershipRepository.class, JpaMembershipSubModuleRepository.class,
        JpaModuleRepository.class, JpaNumberingAllocationPort.class,
        JpaNumberingResolutionRepository.class, JpaOpenAccountRepository.class,
        JpaOpenAccountTotalsAdapter.class, JpaOwnerRepository.class,
        JpaPasswordResetTokenRepository.class, JpaPermissionRepository.class,
        JpaPrescriptionRepository.class, JpaProcedureScheduleRepository.class,
        JpaProductCategoryRepository.class, JpaProductRepository.class,
        JpaPromotionRepository.class, JpaPurchaseOrderRepository.class,
        JpaRolePermissionRepository.class, JpaRoleRepository.class, JpaSalePromotionQueryPort.class,
        JpaServiceCategoryRepository.class, JpaServiceChargeOpenAccountRepository.class,
        JpaServiceRepository.class, JpaSpaRepository.class, JpaSpecieRepository.class,
        JpaStateRepository.class, JpaStockBalanceRepository.class, JpaStockLotRepository.class,
        JpaStockMovementRepository.class, JpaSubModuleRepository.class,
        JpaSupplierInvoiceRepository.class, JpaSupplierRepository.class, JpaSurgeryRepository.class,
        JpaSurgeryTypeRepository.class, JpaSystemConfigurationRepository.class,
        JpaSystemPermissionRepository.class, JpaSystemUserCredentialsRepository.class,
        JpaSystemUserPermissionRepository.class, JpaSystemUserRepository.class,
        JpaTaxRepository.class, JpaVaccinationRepository.class, JpaVaccinationTypeRepository.class,
        JpaWeightRecordRepository.class, JpaWithholdingConfigRepository.class,
        LaboratoryTestFileJpaMapper.class, LaboratoryTestJpaMapper.class,
        LaboratoryTestTypeJpaMapper.class, MedicamentJpaMapper.class,
        MedicamentPrescriptionJpaMapper.class, MedicationScheduleJpaMapper.class,
        MembershipJpaMapper.class, MembershipSubModuleJpaMapper.class, ModuleJpaMapper.class,
        NumberingResolutionJpaMapper.class, OpenAccountJpaMapper.class, OwnerJpaMapper.class,
        PasswordResetTokenJpaMapper.class, PermissionJpaMapper.class, PrescriptionJpaMapper.class,
        ProcedureScheduleJpaMapper.class, ProductCategoryJpaMapper.class, ProductJpaMapper.class,
        PromotionJpaMapper.class, PurchaseOrderJpaMapper.class, RoleJpaMapper.class,
        RolePermissionJpaMapper.class, ServiceCategoryJpaMapper.class,
        ServiceChargeOpenAccountJpaMapper.class, ServiceJpaMapper.class, SpaJpaMapper.class,
        SpecieJpaMapper.class, StateJpaMapper.class, StockQueryAdapter.class,
        SubModuleJpaMapper.class, SupplierInvoiceJpaMapper.class, SupplierJpaMapper.class,
        SurgeryJpaMapper.class, SurgeryTypeJpaMapper.class, SystemConfigurationJpaMapper.class,
        SystemPermissionJpaMapper.class, SystemUserJpaMapper.class,
        SystemUserPermissionJpaMapper.class, TaxJpaMapper.class, VaccinationJpaMapper.class,
        VaccinationTypeJpaMapper.class, WeightRecordJpaMapper.class,
        WithholdingConfigJpaMapper.class,
        com.vetsoftware.app.debtopenaccount.infrastructure.persistence.JpaOpenAccountQueryPort.class,
        com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.JpaOpenAccountQueryPort.class,
        com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence.JpaEmployeeQueryPort.class,
        com.vetsoftware.app.laboratorytestfile.infrastructure.persistence.JpaEmployeeQueryPort.class,
        com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.JpaOpenAccountQueryPort.class})
public class PersistenceSliceConfig {
}
