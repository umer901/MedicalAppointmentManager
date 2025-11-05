package smm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Core domain model for the app.
 * Holds user/profile, appointments, history, invoices, reminders,
 * and computes pricing based on Insurance + Pricing types.
 */
public class AppModel implements TimeObserver {

    // --- Pricing selection (added) ---
    public enum PricingType { OUT_OF_POCKET, PRICE_REDUCTION, DEFERRED_PAYMENT }
    public PricingType pricing = PricingType.OUT_OF_POCKET;   // default pricing

    // --- Existing state ---
    public final UserProfile profile = new UserProfile();
    public final List<Appointment> appointments = new ArrayList<>();
    public final List<HistoryRecord> history = new ArrayList<>();
    public final List<Invoice> invoices = new ArrayList<>();
    public final List<Reminder> reminders = new ArrayList<>();
    public double creditBalance = 35.0;

    public AppModel() {
        // seed demo data
        appointments.add(new Appointment(
                LocalDate.now().plusDays(2), LocalTime.of(9,30),
                "Consultation", "Dermatology", "Dr. Martin", "St-Luc",
                "Shared", "—", 120));
        appointments.add(new Appointment(
                LocalDate.now().plusDays(9), LocalTime.of(15,0),
                "Consultation", "Cardiology", "Dr. Duval", "CHU",
                "Private", "ECG", 180));

        history.add(new HistoryRecord(LocalDate.now().minusDays(12), "Consultation", "Flu — Paracetamol 1g"));
        history.add(new HistoryRecord(LocalDate.now().minusMonths(2), "Prescription", "Amoxicillin 500mg"));
        history.add(new HistoryRecord(LocalDate.now().minusMonths(3), "Surgery", "Appendectomy"));

        invoices.add(new Invoice(LocalDate.now().minusDays(3), 60.0, true, null));
        invoices.add(new Invoice(LocalDate.now().minusDays(1), 120.0, false, appointments.get(0).id));

        reminders.add(new Reminder("appointment", "Dermatology in 2 days",
                LocalDateTime.now().plusDays(2).withHour(9).withMinute(0)));
        reminders.add(new Reminder("medication", "Take Vitamin D",
                LocalDateTime.now().withHour(8).withMinute(0)));
    }

    // --- Insurance (kept as you had) ---
    public String currentPolicy() {
        return switch (profile.insurance) {
            case MINIMAL -> "Out of Pocket";
            case NORMAL  -> "Standard Coverage";
            case PREMIUM -> "Price Reduction/Deferred";
        };
    }

    /** Price after insurance coverage only (no pricing applied). */
    public double priceAfterInsurance(double base) {
        return switch (profile.insurance) {
            case MINIMAL -> base;
            case NORMAL  -> base * 0.8;   // 20% covered
            case PREMIUM -> base * 0.5;   // 50% covered
        };
    }

    // --- Pricing (new) ---
    public String currentPricing() {
        return switch (pricing) {
            case OUT_OF_POCKET   -> "Out of Pocket";
            case PRICE_REDUCTION -> "Price Reduction";
            case DEFERRED_PAYMENT-> "Deferred Payment";
        };
    }

    /**
     * Final amount to pay for a service, considering both:
     *  - Insurance coverage
     *  - Pricing strategy (extra discount or deferral)
     */
    public double priceAfterPlan(double base) {
        double afterInsurance = priceAfterInsurance(base);
        return switch (pricing) {
            case OUT_OF_POCKET   -> afterInsurance;
            case PRICE_REDUCTION -> afterInsurance * 0.9; // extra 10% discount
            case DEFERRED_PAYMENT-> afterInsurance;       // same amount, paid later
        };
    }

    /** True if payment should be deferred by policy (used by UI to disable Pay Now). */
    public boolean canDeferredPayment() {
        return pricing == PricingType.DEFERRED_PAYMENT;
    }

    // --- App operations ---
    public Appointment createAppointment(Appointment a, boolean addToCalendar, boolean payNow) {
        appointments.add(a);

        // Compute price with insurance + pricing
        double amount = priceAfterPlan(a.price);

        // If pricing = DEFERRED_PAYMENT, force unpaid (cannot pay now)
        boolean payNowEffective = payNow && !canDeferredPayment();

        invoices.add(new Invoice(LocalDate.now(), amount, payNowEffective, a.id));
        a.paid = payNowEffective;

        if (addToCalendar) {
            reminders.add(new Reminder(
                    "appointment",
                    a.type + " @ " + a.medicalCenter,
                    LocalDateTime.of(a.date, a.time)
            ));
        }
        return a;
    }

    public void markInvoicePaid(UUID invoiceId) {
        invoices.stream()
                .filter(i -> i.id.equals(invoiceId))
                .findFirst()
                .ifPresent(i -> i.paid = true);
    }

    // --- TimeEvent handling (unchanged logic) ---
    @Override
    public void onTimeAdvanced(TimeEvent event) {
        // Move past appointments to history
        var past = new ArrayList<>(appointments);
        for (var a : past) {
            if (a.date.isBefore(event.newDate)) {
                history.add(new HistoryRecord(
                        a.date,
                        a.type,
                        "Completed: " + a.service + " with " + a.doctor + " @ " + a.medicalCenter
                ));
            }
        }
        appointments.removeIf(a -> a.date.isBefore(event.newDate));

        // Random/system events that append to history (demo)
        for (String ev : event.events) {
            if (ev.contains("User fell ill")) {
                history.add(new HistoryRecord(event.newDate, "Consultation", "Random illness treated"));
            }
        }
    }
}
