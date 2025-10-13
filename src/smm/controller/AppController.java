package smm.controller;

import smm.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class AppController {
    private final AppModel model;
    private UUID selectedAppointmentId;

    public AppController(AppModel model) { this.model = model; }
    public AppModel getModel() { return model; }

    public void setInsurance(InsuranceLevel lvl) { model.profile.insurance = lvl; }

    public Appointment createBooking(LocalDate date, LocalTime time, String type, String service,
                                     String doctor, String center, String room, String equip,
                                     double basePrice, boolean addToCalendar, boolean payNow) {
        return model.createAppointment(
                new Appointment(date, time, type, service, doctor, center, room, equip, basePrice),
                addToCalendar, payNow);
    }

    public void addHistory(String kind, String details) {
        model.history.add(new HistoryRecord(LocalDate.now(), kind, details));
    }

    public void payInvoice(UUID invoiceId) { model.markInvoicePaid(invoiceId); }

    public void setReminderEnabled(UUID id, boolean enabled) {
        model.reminders.stream().filter(r -> r.id.equals(id)).findFirst().ifPresent(r -> r.enabled = enabled);
    }

    public void setNotificationPrefs(boolean email, boolean sms, boolean inApp) {
        model.profile.notifEmail = email; model.profile.notifSMS = sms; model.profile.notifInApp = inApp;
    }

    public void setTwoFA(boolean enable) { model.profile.twoFA = enable; }

    // Selection for details page
    public void setSelectedAppointment(UUID id) { this.selectedAppointmentId = id; }
    public Appointment getSelectedAppointment() {
        if (selectedAppointmentId == null) return null;
        return model.appointments.stream().filter(a -> a.id.equals(selectedAppointmentId)).findFirst().orElse(null);
    }
}
