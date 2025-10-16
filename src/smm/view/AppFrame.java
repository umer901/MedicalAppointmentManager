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

    public final CardLayout cards = new CardLayout();
    public final JPanel content = new JPanel(cards);
    public final JList<String> nav;
    public final DefaultListModel<String> navModel = new DefaultListModel<>();
    public final Map<String, JPanel> pages = new LinkedHashMap<>();
    public final AppController controller;

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
        addPage("Payment • Credit Balance", new PaymentPages.CreditPage(controller));
        addPage("Reminders • Dashboard", new RemindersPages.DashboardPage(controller, this::refreshAll));
        addPage("Reminders • Add / Edit", new RemindersPages.EditPage(controller, this::refreshAll));
        addPage("User Profile", new UserProfilePage(controller, this::refreshAll));
        addPage("Doctor / Admin View", new DoctorAdminPage(controller));

        nav.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) navigateTo(nav.getSelectedValue());
        });

        nav.setSelectedIndex(0);
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
    }

    // NEW: navigation publique pour les panels
    public void navigateTo(String key) {
        refreshAll();
        showCard(key);
        nav.setSelectedValue(key, true);
    }
}
