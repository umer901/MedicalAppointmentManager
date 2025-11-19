package smm.controller;

import smm.model.*;
import smm.view.AppFrame;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

/** Application controller that implements the provided ControllerInterface. */
public class Controller implements ControllerInterface {
    private final AppModel model;

    // --- UI wiring ---
    private AppFrame view;           // lazily created in enableUIView()
    private boolean uiEnabled = false;

    // --- “Feature model” known to the controller (names are UPPERCASE) ---
    private static final Set<String> FEATURE_MODEL = Set.of(
        // real features
        "APPOINTMENTS","MEDICAL_HISTORY","PAYMENT","REMINDERS",
        "APPOINTMENT_REMINDER","MEDICATION_REMINDER",
        "INSURANCE_MINIMAL","INSURANCE_NORMAL","INSURANCE_PREMIUM",
        "OUT_OF_POCKET","PRICE_REDUCTION","DEFERRED_PAYMENT",

        // test-only/no-op features so activations don't diverge
        "ADD_CONSULTATION","ADD_PRESCRIPTIONS","ADD_SURGERIES","ADD_TO_CALENDAR",
        "BASIC_SEARCH","ADVANCED_SEARCH"
    );

    // Current on/off state (parent REMINDERS + both children start enabled)
    private final Set<String> enabledModules = new HashSet<>(Set.of(
        "APPOINTMENTS", "MEDICAL_HISTORY", "PAYMENT",
        "REMINDERS", "APPOINTMENT_REMINDER", "MEDICATION_REMINDER"
    ));

    // NEW: keep explicit sets for insurance & pricing features
    private final Set<String> activeInsurance = new HashSet<>(Set.of("INSURANCE_NORMAL"));
    private final Set<String> activePricing   = new HashSet<>(Set.of("OUT_OF_POCKET"));

    // Selection used by pages
    private UUID selectedAppointmentId;

    // --- unified log3 TES section (kept between writes) ---
    private final java.util.List<String> tesSection3 = new java.util.ArrayList<>();

    public Controller() {
        this(new smm.model.AppModel());
    }

    public Controller(AppModel model) {
        this.model = model;
        // Ensure model is consistent with our initial feature sets
        this.model.profile.insurance = InsuranceLevel.NORMAL;
        this.model.pricing = AppModel.PricingType.OUT_OF_POCKET;
    }

    public AppModel getModel() { return model; }

    public AppFrame getView() { return view; }

    /* -----------------------------------------------------------
       ControllerInterface methods
       ----------------------------------------------------------- */

    @Override
    public int activate(String[] deactivations, String[] activations) {
        // normalize + sort (determinism)
        List<String> deact = new ArrayList<>();
        if (deactivations != null) {
            for (String s : deactivations) deact.add(normalize(s));
        }
        Collections.sort(deact);

        List<String> act = new ArrayList<>();
        if (activations != null) {
            for (String s : activations) act.add(normalize(s));
        }
        Collections.sort(act);

        /* -------- UPDATE INTERNAL FEATURE SETS (order-independent) -------- */

        // 1) DEACTIVATE
        for (String f : deact) {
            if (!FEATURE_MODEL.contains(f)) continue; // ignore unknown test toggles

            if (isInsurance(f)) {
                activeInsurance.remove(f);
            } else if (isPricing(f)) {
                activePricing.remove(f);
            } else {
                enabledModules.remove(f);
            }
        }

        // 2) ACTIVATE
        for (String f : act) {
            if (!FEATURE_MODEL.contains(f)) continue; // ignore unknown test toggles

            if (isInsurance(f)) {
                activeInsurance.add(f);
            } else if (isPricing(f)) {
                activePricing.add(f);
            } else {
                enabledModules.add(f);
            }
        }

        /* -------- DERIVE INSURANCE FROM activeInsurance --------
           Priority: PREMIUM > NORMAL > MINIMAL
         */
        InsuranceLevel pendingIns;
        if (activeInsurance.contains("INSURANCE_PREMIUM")) {
            pendingIns = InsuranceLevel.PREMIUM;
        } else if (activeInsurance.contains("INSURANCE_NORMAL")) {
            pendingIns = InsuranceLevel.NORMAL;
        } else if (activeInsurance.contains("INSURANCE_MINIMAL")) {
            pendingIns = InsuranceLevel.MINIMAL;
        } else {
            // no insurance feature active → default to NORMAL
            pendingIns = InsuranceLevel.NORMAL;
            activeInsurance.clear();
            activeInsurance.add("INSURANCE_NORMAL");
        }

        /* -------- DERIVE PRICING FROM activePricing --------
           Priority: DEFERRED > PRICE_REDUCTION > OUT_OF_POCKET
         */
        AppModel.PricingType pendingPricing;
        if (activePricing.contains("DEFERRED_PAYMENT")) {
            pendingPricing = AppModel.PricingType.DEFERRED_PAYMENT;
        } else if (activePricing.contains("PRICE_REDUCTION")) {
            pendingPricing = AppModel.PricingType.PRICE_REDUCTION;
        } else if (activePricing.contains("OUT_OF_POCKET")) {
            pendingPricing = AppModel.PricingType.OUT_OF_POCKET;
        } else {
            // no pricing feature active → default to OUT_OF_POCKET
            pendingPricing = AppModel.PricingType.OUT_OF_POCKET;
            activePricing.clear();
            activePricing.add("OUT_OF_POCKET");
        }

        /* -------- Commit model -------- */
        model.profile.insurance = pendingIns;
        model.pricing = pendingPricing;

        /* -------- Parent/children invariant for REMINDERS -------- */
        boolean parent = enabledModules.contains("REMINDERS");
        boolean ar = enabledModules.contains("APPOINTMENT_REMINDER");
        boolean mr = enabledModules.contains("MEDICATION_REMINDER");

        if (!parent) { // parent OFF ⇒ children OFF
            enabledModules.remove("APPOINTMENT_REMINDER");
            enabledModules.remove("MEDICATION_REMINDER");
        }
        if (ar || mr) { // any child ON ⇒ parent ON
            enabledModules.add("REMINDERS");
        }

        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog();
        writeStateLog1();
        writeStateLog3();
        return 0;
    }

    /* ----- helpers (add near your other helpers) ----- */
    private static String featureNameOf(InsuranceLevel lvl) {
        return switch (lvl) {
            case MINIMAL -> "INSURANCE_MINIMAL";
            case NORMAL  -> "INSURANCE_NORMAL";
            case PREMIUM -> "INSURANCE_PREMIUM";
        };
    }

    private static String featureNameOf(AppModel.PricingType p) {
        return switch (p) {
            case OUT_OF_POCKET    -> "OUT_OF_POCKET";
            case PRICE_REDUCTION  -> "PRICE_REDUCTION";
            case DEFERRED_PAYMENT -> "DEFERRED_PAYMENT";
        };
    }

    public boolean isModuleEnabled(String moduleKey) { return enabledModules.contains(moduleKey); }
    public java.util.Set<String> getEnabledModules() { return java.util.Collections.unmodifiableSet(enabledModules); }

    @Override
    public boolean enableUIView() {
        if (uiEnabled) return true;
        try {
            view = new AppFrame(this);
            uiEnabled = true;
            view.setVisible(true);
            view.refreshAll();
            writeStateLog();
            writeStateLog3();
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
                view.dispose();
                view = null;
            }
            writeStateLog();
            writeStateLog3();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* -----------------------------------------------------------
       Logs
       ----------------------------------------------------------- */

    @Override
    public String[] getStateAsLog() {
        List<String> lines = new ArrayList<>();
        lines.add("uiEnabled:" + uiEnabled);
        lines.add("user:" + model.profile.name);
        lines.add("insurance:" + model.profile.insurance);
        lines.add("pricing:" + model.pricing);

        // Parent modules
        for (String m : List.of("APPOINTMENTS", "MEDICAL_HISTORY", "PAYMENT", "REMINDERS")) {
            lines.add("module:" + m + "=" + (enabledModules.contains(m) ? "ON" : "OFF"));
        }

        // Sub-features of Reminders
        lines.add("module:APPOINTMENT_REMINDER=" + (enabledModules.contains("APPOINTMENT_REMINDER") ? "ON" : "OFF"));
        lines.add("module:MEDICATION_REMINDER=" + (enabledModules.contains("MEDICATION_REMINDER") ? "ON" : "OFF"));

        // Aggregated info
        long unpaid = model.invoices.stream().filter(i -> !i.paid).count();
        lines.add("appointments:count=" + model.appointments.size());
        lines.add("history:count=" + model.history.size());
        lines.add("invoices:total=" + model.invoices.size());
        lines.add("invoices:unpaid=" + unpaid);
        lines.add("reminders:enabled=" + model.reminders.stream().filter(r -> r.enabled).count());
        lines.add("policy:" + model.currentPolicy());
        return lines.toArray(new String[0]);
    }

    @Override
    public String[] getStateAsLog1() {
        List<String> lines = new ArrayList<>();
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
        if (model.invoices.stream().anyMatch(i -> !i.paid)) {
            lines.add("icon warning unpaidInvoices mainWindow-TopRight");
        }
        lines.add("buttonSmall logout mainWindow-TopRight");
        return lines.toArray(new String[0]);
    }

    /* ---------------- unified log3 ---------------- */
    public String[] getStateAsLog3() {
        List<String> lines = new ArrayList<>();

        lines.add("ui enabled=" + uiEnabled);
        lines.add("user name=" + model.profile.name);
        lines.add("insurance level=" + model.profile.insurance);
        lines.add("policy current=" + model.currentPolicy());
        lines.add("pricing current=" + model.currentPricing());

        // Main features
        lines.add("feature APPOINTMENTS="    + (enabledModules.contains("APPOINTMENTS")    ? "ON" : "OFF"));
        lines.add("feature MEDICAL_HISTORY=" + (enabledModules.contains("MEDICAL_HISTORY") ? "ON" : "OFF"));
        lines.add("feature PAYMENT="         + (enabledModules.contains("PAYMENT")         ? "ON" : "OFF"));
        lines.add("feature REMINDERS="       + (enabledModules.contains("REMINDERS")       ? "ON" : "OFF"));

        // Sub-features
        lines.add("feature APPOINTMENT_REMINDER=" + (enabledModules.contains("APPOINTMENT_REMINDER") ? "ON" : "OFF"));
        lines.add("feature MEDICATION_REMINDER="  + (enabledModules.contains("MEDICATION_REMINDER")  ? "ON" : "OFF"));

        long unpaid = model.invoices.stream().filter(i -> !i.paid).count();
        lines.add("count appointments=" + model.appointments.size());
        lines.add("count history=" + model.history.size());
        lines.add("count invoices=" + model.invoices.size());
        lines.add("count invoices_unpaid=" + unpaid);
        lines.add("count reminders_enabled=" + model.reminders.stream().filter(r -> r.enabled).count());

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
        if (unpaid > 0) lines.add("icon warning unpaidInvoices mainWindow-TopRight");
        lines.add("buttonSmall logout mainWindow-TopRight");
        lines.addAll(tesSection3);
        return lines.toArray(new String[0]);
    }

    private void writeStateLog3() {
        try (FileWriter fw = new FileWriter("state_log3.txt", false)) {
            for (String line : getStateAsLog3()) fw.write(line + System.lineSeparator());
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ---------------- Remaining API methods unchanged ---------------- */

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

    public void setInsurance(InsuranceLevel lvl) {
        // keep sets + model in sync
        activeInsurance.clear();
        switch (lvl) {
            case MINIMAL -> activeInsurance.add("INSURANCE_MINIMAL");
            case NORMAL  -> activeInsurance.add("INSURANCE_NORMAL");
            case PREMIUM -> activeInsurance.add("INSURANCE_PREMIUM");
        }
        model.profile.insurance = lvl;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
    }

    public Appointment createBooking(LocalDate date, LocalTime time, String type, String service,
                                     String doctor, String center, String room, String equip,
                                     double basePrice, boolean addToCalendar, boolean payNow) {
        Appointment a = model.createAppointment(
            new Appointment(date, time, type, service, doctor, center, room, equip, basePrice),
            addToCalendar, payNow
        );
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
        return a;
    }

    public void addHistory(String kind, String details) {
        model.history.add(new HistoryRecord(LocalDate.now(), kind, details));
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
    }

    public void payInvoice(UUID invoiceId) {
        model.markInvoicePaid(invoiceId);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
    }

    public void setReminderEnabled(UUID id, boolean enabled) {
        model.reminders.stream().filter(r -> r.id.equals(id)).findFirst().ifPresent(r -> r.enabled = enabled);
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
    }

    public void setNotificationPrefs(boolean email, boolean sms, boolean inApp) {
        model.profile.notifEmail = email;
        model.profile.notifSMS = sms;
        model.profile.notifInApp = inApp;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
    }

    public void setTwoFA(boolean enable) {
        model.profile.twoFA = enable;
        if (uiEnabled && view != null) view.refreshAll();
        writeStateLog3();
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
        writeStateLog3();
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

    private static boolean isPricing(String f) {
        return f.equals("OUT_OF_POCKET") || f.equals("PRICE_REDUCTION") || f.equals("DEFERRED_PAYMENT");
    }

    private void writeStateLog() {
        try (FileWriter fw = new FileWriter("state_log.txt", false)) {
            for (String line : getStateAsLog()) fw.write(line + System.lineSeparator());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void writeStateLog1() {
        try (FileWriter fw = new FileWriter("state_log1.txt", false)) {
            for (String line : getStateAsLog1()) fw.write(line + System.lineSeparator());
        } catch (IOException e) { e.printStackTrace(); }
    }
}
