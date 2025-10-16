package smm.controller;

import smm.model.*;
import smm.view.AppFrame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Application controller that now implements the provided ControllerInterface. */
public class AppController implements ControllerInterface {
    private final AppModel model;

    // --- UI wiring ---
    private AppFrame view;           // lazily created in enableUIView()
    private boolean uiEnabled = false;

    // --- “Feature model” known to the controller (names are UPPERCASE) ---
    // Modules/features that can be toggled on/off
    private static final Set<String> FEATURE_MODEL = Set.of(
            "APPOINTMENTS", "MEDICAL_HISTORY", "PAYMENT", "REMINDERS",
            "INSURANCE_MINIMAL", "INSURANCE_NORMAL", "INSURANCE_PREMIUM"
    );

    // Current on/off state for high-level modules (insurance is mutually exclusive, handled separately)
    private final Set<String> enabledModules = new HashSet<>(Set.of(
            "APPOINTMENTS", "MEDICAL_HISTORY", "PAYMENT", "REMINDERS"
    ));

    // Selection used by pages
    private UUID selectedAppointmentId;

    public AppController(AppModel model) { this.model = model; }

    public AppModel getModel() { return model; }

    /** Optional: expose the view if callers need it */
    public AppFrame getView() { return view; }

    /* -----------------------------------------------------------
       Your ControllerInterface methods (implemented)
       ----------------------------------------------------------- */

    /**
     * Activate/deactivate features that exist in the feature model only.
     * Returns 0 on success; non-zero codes on errors:
     *   1 -> deactivation refers to an unknown feature
     *   2 -> activation refers to an unknown feature
     */
    @Override
    public int activate(String[] deactivations, String[] activations) {
        // Deactivate modules or change insurance
        if (deactivations != null) {
            for (String name : deactivations) {
                String f = normalize(name);
                if (!FEATURE_MODEL.contains(f)) return 1;
                if (isInsurance(f)) {
                    // “Deactivating” an insurance level means falling back to NORMAL (safe default)
                    model.profile.insurance = InsuranceLevel.NORMAL;
                } else {
                    enabledModules.remove(f);
                }
            }
        }

        // Activate modules or set insurance
        if (activations != null) {
            for (String name : activations) {
                String f = normalize(name);
                if (!FEATURE_MODEL.contains(f)) return 2;
                if (isInsurance(f)) {
                    switch (f) {
                        case "INSURANCE_MINIMAL" -> model.profile.insurance = InsuranceLevel.MINIMAL;
                        case "INSURANCE_NORMAL"  -> model.profile.insurance = InsuranceLevel.NORMAL;
                        case "INSURANCE_PREMIUM" -> model.profile.insurance = InsuranceLevel.PREMIUM;
                    }
                } else {
                    enabledModules.add(f);
                }
            }
        }

        // Non-blocking: if UI is up, just ask it to refresh
        if (uiEnabled && view != null) view.refreshAll();
        return 0;
    }

    @Override
    public boolean enableUIView() {
        if (uiEnabled) return true;
        // Create view lazily and wire event bus (done in App.java too; having it here keeps contract self-contained)
        view = new AppFrame(this);
        uiEnabled = true;
        view.setVisible(true);
        view.refreshAll();
        return true;
    }

    @Override
    public boolean disableUIView() {
        if (!uiEnabled) return true;
        uiEnabled = false;
        if (view != null) view.setVisible(false);
        return true;
    }

    /**
     * Returns current state ONLY (no history), as unordered lines.
     * Examples:
     *   user:Hicham
     *   insurance:PREMIUM
     *   module:APPOINTMENTS=ON
     *   appointments:count=3
     *   invoices:unpaid=1
     */
    @Override
    public String[] getStateAsLog() {
        List<String> lines = new ArrayList<>();
        lines.add("user:" + model.profile.name);
        lines.add("insurance:" + model.profile.insurance);

        // Modules ON/OFF
        for (String m : List.of("APPOINTMENTS","MEDICAL_HISTORY","PAYMENT","REMINDERS")) {
            lines.add("module:" + m + "=" + (enabledModules.contains(m) ? "ON" : "OFF"));
        }

        // Aggregated counts
        long unpaid = model.invoices.stream().filter(i -> !i.paid).count();
        lines.add("appointments:count=" + model.appointments.size());
        lines.add("history:count=" + model.history.size());
        lines.add("invoices:total=" + model.invoices.size());
        lines.add("invoices:unpaid=" + unpaid);
        lines.add("reminders:enabled=" + model.reminders.stream().filter(r -> r.enabled).count());

        // A couple of demonstrative detail lines (current policy & balance)
        lines.add("policy:" + model.currentPolicy());
        lines.add("creditBalance:" + String.format(java.util.Locale.ROOT, "%.2f", model.creditBalance));

        return lines.toArray(new String[0]);
    }

    /* -----------------------------------------------------------
       Existing public API used by views (unchanged)
       ----------------------------------------------------------- */

    public void setInsurance(InsuranceLevel lvl) { model.profile.insurance = lvl; }

    public Appointment createBooking(LocalDate date, LocalTime time, String type, String service,
                                     String doctor, String center, String room, String equip,
                                     double basePrice, boolean addToCalendar, boolean payNow) {
        // If APPOINTMENTS module is OFF, we still allow creation (non-blocking),
        // but you could reject with an exception if you want to enforce toggles strictly.
        return model.createAppointment(
                new Appointment(date, time, type, service, doctor, center, room, equip, basePrice),
                addToCalendar, payNow);
    }

    public void addHistory(String kind, String details) { model.history.add(new HistoryRecord(LocalDate.now(), kind, details)); }

    public void payInvoice(UUID invoiceId) { model.markInvoicePaid(invoiceId); }

    public void setReminderEnabled(UUID id, boolean enabled) {
        model.reminders.stream().filter(r -> r.id.equals(id)).findFirst().ifPresent(r -> r.enabled = enabled);
    }

    public void setNotificationPrefs(boolean email, boolean sms, boolean inApp) {
        model.profile.notifEmail = email; model.profile.notifSMS = sms; model.profile.notifInApp = inApp;
    }

    public void setTwoFA(boolean enable) { model.profile.twoFA = enable; }

    public void setSelectedAppointment(UUID id) { this.selectedAppointmentId = id; }
    public Appointment getSelectedAppointment() {
        if (selectedAppointmentId == null) return null;
        return model.appointments.stream().filter(a -> a.id.equals(selectedAppointmentId)).findFirst().orElse(null);
    }

    /* -----------------------------------------------------------
       Helpers
       ----------------------------------------------------------- */
    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
    private static boolean isInsurance(String f) {
        return f.equals("INSURANCE_MINIMAL") || f.equals("INSURANCE_NORMAL") || f.equals("INSURANCE_PREMIUM");
    }
}
