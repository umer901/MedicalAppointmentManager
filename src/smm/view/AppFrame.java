// imports identiques
package smm.view;

import smm.controller.AppController;
import smm.view.pages.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppFrame extends JFrame {
    public interface Refreshable { void refresh(); }
    private boolean suppressNavEvents = false;
    public final CardLayout cards = new CardLayout();
    public final JPanel content = new JPanel(cards);
    public final JList<String> nav;
    public final DefaultListModel<String> navModel = new DefaultListModel<>();
    public final Map<String, JPanel> pages = new LinkedHashMap<>();
    public final AppController controller;
    public void addExternalPage(String key, JPanel panel) {
        addPage(key, panel);   // reuse the private helper
    }
    public AppFrame(AppController controller) {
        super("Smart Medical Booking App");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new EmptyBorder(12, 12, 12, 6));
        nav = new JList<>(navModel);
        nav.setFixedCellWidth(220);                       // largeur stable
        nav.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JLabel("Navigation"), BorderLayout.NORTH);
        left.add(new JScrollPane(nav), BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        content.setBorder(new EmptyBorder(12, 6, 12, 12));
        add(content, BorderLayout.CENTER);

        // pages
        addPage("Home / Dashboard", new DashboardPage(controller));
        addPage("Appointments • List", new AppointmentsPages.ListPage(controller));
        addPage("Appointments • Create Booking", new AppointmentsPages.CreatePage(controller, this::refreshAll));
        addPage("Appointments • Details", new AppointmentsPages.DetailsPage(controller));
        addPage("Calendar", new CalendarPage(controller));
        addPage("Medical History • Overview", new HistoryPages.OverviewPage(controller));
        addPage("Medical History • Record Details", new HistoryPages.RecordDetailsPage(controller));
        addPage("Medical History • Add Record", new HistoryPages.AddRecordPage(controller, this::refreshAll));
        addPage("Insurance • Overview", new InsurancePage(controller, this::refreshAll));
        addPage("Payment • Billing & Payments", new PaymentPages.BillingPage(controller, this::refreshAll));
        addPage("Payment • Pricing Details", new PaymentPages.PricingPage(controller));
        // addPage("Payment • Credit Balance", new PaymentPages.CreditPage(controller));
        addPage("Reminders • Dashboard", new RemindersPages.DashboardPage(controller, this::refreshAll));
        addPage("Reminders • Add / Edit", new RemindersPages.EditPage(controller, this::refreshAll));
        addPage("User Profile", new UserProfilePage(controller, this::refreshAll));
        addPage("Doctor / Admin View", new DoctorAdminPage(controller));
        addPage("Settings • Features", new FeaturesPage(controller, this));

        nav.addListSelectionListener(e -> {
            if (suppressNavEvents || e.getValueIsAdjusting()) return;
            String sel = nav.getSelectedValue();
            if (sel != null) navigateTo(sel);
        });

        nav.setSelectedIndex(0);
    }
// Decide if a page should be visible given enabled modules
private boolean isPageAllowedByFeatures(String key) {
    var on = controller.getEnabledModules();
    if (key.startsWith("Appointments")) return on.contains("APPOINTMENTS");
    if (key.equals("Calendar"))         return on.contains("APPOINTMENTS");

    if (key.startsWith("Medical History")) return on.contains("MEDICAL_HISTORY");

    if (key.startsWith("Payment")) return on.contains("PAYMENT");

    if (key.startsWith("Reminders")) return on.contains("REMINDERS");

    // Always visible:
    // Home / Dashboard, Insurance • Overview, User Profile, Doctor / Admin View,
    // Settings • Features, Time Event System
    return true;
}

/** Public: called after toggles to recompute the left navigation */
public void rebuildNavigationByFeatures() {
    suppressNavEvents = true;                 // start suppressing

    String current = nav.getSelectedValue();
    navModel.clear();
    for (String k : pages.keySet()) {
        if (isPageAllowedByFeatures(k)) navModel.addElement(k);
    }

    if (current == null || !isPageAllowedByFeatures(current)) {
        current = navModel.size() > 0 ? navModel.get(0) : null;
    }

    if (current != null) {
        // show without triggering listener
        cards.show(content, current);
        nav.setSelectedValue(current, true);
    }

    suppressNavEvents = false;                // done
}


    private void addPage(String key, JPanel panel) {
        pages.put(key, panel);
        navModel.addElement(key);
        content.add(panel, key);
    }

    private void showCard(String key) {
        if (key == null) return;
        refresh(key);
        cards.show(content, key);
    }

    private void refresh(String key) {
        JPanel p = pages.get(key);
        if (p instanceof Refreshable r) r.refresh();
    }
public void refreshAll() {
    for (JPanel p : pages.values()) if (p instanceof Refreshable r) r.refresh();
    rebuildNavigationByFeatures();   // <— add this line
}

public void navigateTo(String key) {
    refreshAll();                 // keep this line if you like it
    showCard(key);
    // nav.setSelectedValue(key, true); // you can remove this if you prefer
}

}
