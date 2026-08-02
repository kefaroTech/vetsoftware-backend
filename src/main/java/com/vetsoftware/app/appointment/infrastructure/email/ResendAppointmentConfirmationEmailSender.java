package com.vetsoftware.app.appointment.infrastructure.email;

import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Correo de confirmación de cita enviado con una <b>plantilla de Resend</b> ({@code template: { id,
 * variables }}). Async y no bloqueante (lo garantiza {@link ResendEmailClient}): si Resend falla,
 * el agendamiento continúa.
 *
 * <p>Variables de la plantilla (placeholders {@code {{{VARIABLE}}}} del HTML en Resend):
 * RECIPIENT_NAME, RECIPIENT_EMAIL, COMPANY_NAME, APPOINTMENT_DATE, APPOINTMENT_TIME,
 * APPOINTMENT_TYPE, VET_NAME, PET_NAME, BRANCH_NAME, BRANCH_ADDRESS, NOTES.
 */
@Component
public class ResendAppointmentConfirmationEmailSender
    implements AppointmentConfirmationEmailSender {

  private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES_CO);
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", ES_CO);

  private final ResendEmailClient email;
  private final String templateId;

  public ResendAppointmentConfirmationEmailSender(
      ResendEmailClient email,
      @Value("${vetsoftware.appointment.confirmation-template-id:}") String templateId) {
    this.email = email;
    this.templateId = templateId;
  }

  @Override
  public void send(AppointmentConfirmationData d) {
    String companyName = nz(d.companyName(), "tu clínica");
    String subject = "Tu cita en " + companyName + " quedó agendada";

    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("RECIPIENT_NAME", nz(d.recipientName(), ""));
    variables.put("RECIPIENT_EMAIL", nz(d.recipientEmail(), ""));
    variables.put("COMPANY_NAME", companyName);
    variables.put("APPOINTMENT_DATE", capitalize(DATE_FMT.format(d.startAt())));
    variables.put("APPOINTMENT_TIME", TIME_FMT.format(d.startAt()));
    variables.put("APPOINTMENT_TYPE", typeLabel(d.type()));
    variables.put("VET_NAME", nz(d.vetName(), "Por asignar"));
    variables.put("PET_NAME", nz(d.petName(), "—"));
    variables.put("BRANCH_NAME", nz(d.branchName(), "—"));
    variables.put("BRANCH_ADDRESS", nz(d.branchAddress(), "—"));
    variables.put("NOTES", nz(d.notes(), "Sin observaciones."));

    email.sendTemplate(d.recipientEmail(), null, subject, templateId, variables);
  }

  private static String typeLabel(AppointmentType type) {
    if (type == null) return "Cita";
    return switch (type) {
      case CONSULTATION -> "Consulta";
      case CONTROL -> "Control";
      case VACCINATION -> "Vacunación";
      case DEWORMING -> "Desparasitación";
      case SURGERY -> "Cirugía";
      case IMAGING -> "Imagenología";
      case LABORATORY -> "Laboratorio";
      case GROOMING -> "Estética";
      case OTHER -> "Otro";
    };
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static String nz(String s, String fallback) {
    return (s == null || s.isBlank()) ? fallback : s;
  }
}
