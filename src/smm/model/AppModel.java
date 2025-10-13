package smm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class AppModel {
    public final UserProfile profile = new UserProfile();
    public final java.util.List<Appointment> appointments = new ArrayList<>();
    public final java.util.List<HistoryRecord> history = new ArrayList<>();
    public final java.util.List<Invoice> invoices = new ArrayList<>();
    public final java.util.List<Reminder> reminders = new ArrayList<>();
    public double creditBalance = 35.0;

    public AppModel() {
        // seed demo data
        appointments.add(new Appointment(LocalDate.now().plusDays(2), LocalTime.of(9,30),
                "Consultation", "Dermatology", "Dr. Martin", "St-Luc", "Shared", "—", 120));
        appointments.add(new Appointment(LocalDate.now().plusDays(9), LocalTime.of(15,0),
                "Consultation", "Cardiology", "Dr. Duval", "CHU", "Private", "ECG", 180));

        history.add(new HistoryRecord(LocalDate.now().minusDays(12), "Consultation", "Flu — Paracetamol 1g"));
        history.add(new HistoryRecord(LocalDate.now().minusMonths(2), "Prescription", "Amoxicillin 500mg"));
        history.add(new HistoryRecord(LocalDate.now().minusMonths(3), "Surgery", "Appendectomy"));

        invoices.add(new Invoice(LocalDate.now().minusDays(3), 60.0, true, null));
        invoices.add(new Invoice(LocalDate.now().minusDays(1), 120.0, false, appointments.get(0).id));

        reminders.add(new Reminder("appointment", "Dermatology in 2 days", LocalDateTime.now().plusDays(2).withHour(9).withMinute(0)));
        reminders.add(new Reminder("medication", "Take Vitamin D", LocalDateTime.now().withHour(8).withMinute(0)));
    }

    // pricing / policy
    public String currentPolicy() {
        return switch (profile.insurance) {
            case MINIMAL -> "Out of Pocket";
            case NORMAL -> "Standard Coverage";
            case PREMIUM -> "Price Reduction / Deferred";
        };
    }

    public double priceAfterInsurance(double base) {
        return switch (profile.insurance) {
            case MINIMAL -> base;
            case NORMAL -> base * 0.8;    // 20% covered
            case PREMIUM -> base * 0.5;   // 50% covered
        };
    }

    public boolean canDeferredPayment() {
        return profile.insurance == InsuranceLevel.PREMIUM;
    }

    public Appointment createAppointment(Appointment a, boolean addToCalendar, boolean payNow) {
        appointments.add(a);
        if (payNow) {
            invoices.add(new Invoice(LocalDate.now(), priceAfterInsurance(a.price), true, a.id));
            a.paid = true;
        } else {
            invoices.add(new Invoice(LocalDate.now(), priceAfterInsurance(a.price), false, a.id));
        }
        if (addToCalendar) {
            reminders.add(new Reminder("appointment", a.type + " @ " + a.medicalCenter, LocalDateTime.of(a.date, a.time)));
        }
        return a;
    }

    public void markInvoicePaid(UUID invoiceId) {
        invoices.stream().filter(i -> i.id.equals(invoiceId)).findFirst().ifPresent(i -> i.paid = true);
    }
}
