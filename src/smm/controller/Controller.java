package smm.controller;

import smm.model.*;
import smm.view.AppFrame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

/** Application controller that now implements the provided ControllerInterface. */
public class Controller implements ControllerInterface {
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

    // --- NEW: unified log3 TES section (kept between writes) ---
    private final java.util.List<String> tesSection3 = new java.util.ArrayList<>();

    public Controller(AppModel model) { this.model = model; }

    public AppModel getModel() { return model; }

    /** Optional: expose the view if callers need it */
    public AppFrame getView() { return view; }

    /* -----------------------------------------------------------
       ControllerInterface methods (implemented)
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

        // Write logs after any (de)activation
        writeStateLog();
        writeStateLog1();
        writeStateLog3();   // <-- NEW unified log
        return 0;
    }

    /** Used by the UI pages (you already rely on these) */
    public boolean isModuleEnabled(String moduleKey) { return enabledModules.contains(moduleKey); }
    public java.util.Set<String> getEnabledModules() {
        return java.util.Collections.unmodifiableSet(enabledModules);
    }

    @Override
    public boolean enableUIView() {
        if (uiEnabled) return true;
        try {
            view = new AppFrame(this);
            uiEnabled = true;
            view.setVisible(true);
            view.refreshAll();
            writeStateLog();  // optional, logs that the UI is now enabled
            writeStateLog3(); // NEW unified log
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean disableUIView() {
        if (!uiEnabled) return true;
        try {
            uiEnabled = false;
            if (view != null) {
                view.dispose();     // safer than just setVisible(false)
                view = null;
            }
            writeStateLog();  // optional, logs that the UI is now disabled
            writeStateLog3(); // NEW unified log
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        lines.add("timestamp:" + LocalDateTime.now());
        lines.add("uiEnabled:" + uiEnabled);
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
        // lines.add("creditBalance:" + String.format(java.util.Locale.ROOT, "%.2f", model.creditBalance));

        return lines.toArray(new String[0]);
    }

    @Override
    public String[] getStateAsLog1() {
        List<String> lines = new ArrayList<>();

        // Always include the base window
        lines.add("window mainWindow");

        // Dashboard elements (always visible)
        lines.add("label userName mainWindow-TopLeft");
        lines.add("label insuranceLevel mainWindow-TopRight");

        // Insurance buttons — describe available coverage options
        switch (model.profile.insurance) {
            case MINIMAL -> lines.add("buttonSmall upgradeToNormal mainWindow-BottomRight");
            case NORMAL  -> lines.add("buttonSmall upgradeToPremium mainWindow-BottomRight");
            case PREMIUM -> lines.add("label premiumActive mainWindow-BottomRight");
        }

        // --- Modules: add/remove visible buttons or windows depending on what’s enabled ---
        if (enabledModules.contains("APPOINTMENTS")) {
            lines.add("buttonMedium appointments mainWindow-Center");
            lines.add("window appointmentsList");
            lines.add("buttonSmall newAppointment mainWindow-BottomLeft");
        }

        if (enabledModules.contains("MEDICAL_HISTORY")) {
            lines.add("buttonMedium history mainWindow-LeftPanel");
            lines.add("window medicalHistory");
        }

        if (enabledModules.contains("PAYMENT")) {
            lines.add("buttonMedium payment mainWindow-RightPanel");
            lines.add("window invoicesList");
        }

        if (enabledModules.contains("REMINDERS")) {
            lines.add("buttonSmall reminders mainWindow-TopCenter");
            lines.add("window remindersDashboard");
        }

        // You can describe other small icons or text appearing based on state
        if (model.invoices.stream().anyMatch(i -> !i.paid)) {
            lines.add("icon warning unpaidInvoices mainWindow-TopRight");
        }

        // Add an always-present logout or exit button
        lines.add("buttonSmall logout mainWindow-TopRight");

        return lines.toArray(new String[0]);
    }

    /* ----------------------- NEW: unified log3 API ----------------------- */

    /** Build the unified, order-agnostic state log (business + UI + TES) */
    public String[] getStateAsLog3() {
        List<String> lines = new ArrayList<>();

        // --- Time & UI
        lines.add("time now=" + LocalDateTime.now());
        lines.add("ui enabled=" + uiEnabled);

        // --- User & policy
        lines.add("user name=" + model.profile.name);
        lines.add("insurance level=" + model.profile.insurance);
        lines.add("policy current=" + model.currentPolicy());

        // --- Features (modules)
        lines.add("feature APPOINTMENTS="    + (enabledModules.contains("APPOINTMENTS")    ? "ON" : "OFF"));
        lines.add("feature MEDICAL_HISTORY=" + (enabledModules.contains("MEDICAL_HISTORY") ? "ON" : "OFF"));
        lines.add("feature PAYMENT="         + (enabledModules.contains("PAYMENT")         ? "ON" : "OFF"));
        lines.add("feature REMINDERS="       + (enabledModules.contains("REMINDERS")       ? "ON" : "OFF"));

        // --- Aggregates / counts
        long unpaid = model.invoices.stream().filter(i -> !i.paid).count();
        lines.add("count appointments=" + model.appointments.size());
        lines.add("count history=" + model.history.size());
        lines.add("count invoices=" + model.invoices.size());
        lines.add("count invoices_unpaid=" + unpaid);
        lines.add("count reminders_enabled=" + model.reminders.stream().filter(r -> r.enabled).count());
        // lines.add("account credit_balance=" + String.format(java.util.Locale.ROOT, "%.2f", model.creditBalance));

        // --- UI affordances (what is visible/enabled)
        lines.add("window mainWindow");
        lines.add("label userName mainWindow-TopLeft");
        lines.add("label insuranceLevel mainWindow-TopRight");

        switch (model.profile.insurance) {
            case MINIMAL -> lines.add("buttonSmall upgradeToNormal mainWindow-BottomRight");
            case NORMAL  -> lines.add("buttonSmall upgradeToPremium mainWindow-BottomRight");
            case PREMIUM -> lines.add("label premiumActive mainWindow-BottomRight");
        }

        if (enabledModules.contains("APPOINTMENTS")) {
            lines.add("buttonMedium appointments mainWindow-Center");
            lines.add("window appointmentsList");
            lines.add("buttonSmall newAppointment mainWindow-BottomLeft");
        }
        if (enabledModules.contains("MEDICAL_HISTORY")) {
            lines.add("buttonMedium history mainWindow-LeftPanel");
            lines.add("window medicalHistory");
        }
        if (enabledModules.contains("PAYMENT")) {
            lines.add("buttonMedium payment mainWindow-RightPanel");
            lines.add("window invoicesList");
        }
        if (enabledModules.contains("REMINDERS")) {
            lines.add("buttonSmall reminders mainWindow-TopCenter");
            lines.add("window remindersDashboard");
        }
        if (unpaid > 0) {
            lines.add("icon warning unpaidInvoices mainWindow-TopRight");
        }

        // Always-present logout
        lines.add("buttonSmall logout mainWindow-TopRight");

        // --- TES section (if any recent advance/events)
        lines.addAll(tesSection3);

        return lines.toArray(new String[0]);
    }

    /** Persist unified log to state_log3.txt */
    private void writeStateLog3() {
        try (FileWriter fw = new FileWriter("state_log3.txt", false)) {
            for (String line : getStateAsLog3()) {
                fw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Called by TimePage when time advances — updates TES section + writes log3 */
    public void logTESAdvance3(TimeEvent event, java.util.List<String> triggers) {
        tesSection3.clear();
        tesSection3.add("tes advanced from=" + event.oldDate + " to=" + event.newDate);
        for (String s : triggers) {
            if (s.contains("Doctor became unavailable")) {
                tesSection3.add("event doctor_unavailable appointment=none");
            } else if (s.contains("User fell ill")) {
                tesSection3.add("event user_fell_ill");
            } else if (s.contains("Follow-up scheduled")) {
                tesSection3.add("event followup_auto_scheduled appointment=auto");
            }
        }
        writeStateLog3();
    }

    /* -----------------------------------------------------------
       Existing public API used by views (unchanged)
       ----------------------------------------------------------- */

    public void setInsurance(InsuranceLevel lvl) {
        model.profile.insurance = lvl;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public Appointment createBooking(LocalDate date, LocalTime time, String type, String service,
                                     String doctor, String center, String room, String equip,
                                     double basePrice, boolean addToCalendar, boolean payNow) {
        // If APPOINTMENTS module is OFF, we still allow creation (non-blocking),
        // but you could reject with an exception if you want to enforce toggles strictly.
        Appointment a = model.createAppointment(
                new Appointment(date, time, type, service, doctor, center, room, equip, basePrice),
                addToCalendar, payNow);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
        return a;
    }

    public void addHistory(String kind, String details) {
        model.history.add(new HistoryRecord(LocalDate.now(), kind, details));
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public void payInvoice(UUID invoiceId) {
        model.markInvoicePaid(invoiceId);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public void setReminderEnabled(UUID id, boolean enabled) {
        model.reminders.stream().filter(r -> r.id.equals(id)).findFirst().ifPresent(r -> r.enabled = enabled);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public void setNotificationPrefs(boolean email, boolean sms, boolean inApp) {
        model.profile.notifEmail = email; model.profile.notifSMS = sms; model.profile.notifInApp = inApp;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public void setTwoFA(boolean enable) {
        model.profile.twoFA = enable;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
    }

    public void setSelectedAppointment(UUID id) { this.selectedAppointmentId = id; }
    public Appointment getSelectedAppointment() {
        if (selectedAppointmentId == null) return null;
        return model.appointments.stream().filter(a -> a.id.equals(selectedAppointmentId)).findFirst().orElse(null);
    }

    public void updateUserProfile(String name, boolean email, boolean sms, boolean inApp, boolean twoFA) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        model.profile.name = name.trim();
        setNotificationPrefs(email, sms, inApp);
        setTwoFA(twoFA);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3(); // NEW
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

    /** Persist the current state log to a file, as required by the prof’s spec. */
    private void writeStateLog() {
        try (FileWriter fw = new FileWriter("state_log.txt", false)) {
            for (String line : getStateAsLog()) {
                fw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Persist the current state log to a file, as required by the prof’s spec. */
    private void writeStateLog1() {
        try (FileWriter fw = new FileWriter("state_log1.txt", false)) {
            for (String line : getStateAsLog1()) {
                fw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
