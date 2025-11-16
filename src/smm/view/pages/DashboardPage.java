package smm.view.pages;

import smm.controller.Controller;
import smm.model.Appointment;
import smm.view.NavAwarePanel;
import smm.view.UI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

public class DashboardPage extends NavAwarePanel {
    private final Controller c;
    private final JLabel welcome = UI.h1("");
    private final JLabel policyLabel = new JLabel("");
    private final JTextArea notifications = mkArea();
    private final JTextArea doctorAlert = mkArea();

    public DashboardPage(Controller c) {
        this.c = c;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        /* ---------------- HEADER ---------------- */
        JPanel header = new JPanel(new BorderLayout());
        header.add(welcome, BorderLayout.WEST);
        JPanel policyBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        policyBox.add(new JLabel("Insurance: "));
        policyBox.add(policyLabel);
        header.add(policyBox, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        /* ---------------- CENTER ---------------- */
        JPanel center = new JPanel(new BorderLayout(16, 0));
        add(center, BorderLayout.CENTER);

        // --- LEFT: Logo Card ---
        JPanel logoCard = cardPanel();
        logoCard.setPreferredSize(new Dimension(520, 320));
        logoCard.setLayout(new BorderLayout());

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("../../images/logoapp.png"));
            // Resize smoothly to fit the box
            Image scaled = icon.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel title = new JLabel("Smart Medical Booking", SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

            JPanel logoPanel = new JPanel(new BorderLayout());
            logoPanel.setOpaque(false);
            logoPanel.add(logoLabel, BorderLayout.CENTER);
            logoPanel.add(title, BorderLayout.SOUTH);

            logoCard.add(logoPanel, BorderLayout.CENTER);
        } catch (Exception ex) {
            JLabel error = new JLabel("Logo not found at /src/smm/view/pages/logoapp.png", SwingConstants.CENTER);
            logoCard.add(error, BorderLayout.CENTER);
        }
        center.add(logoCard, BorderLayout.CENTER);

        // --- RIGHT COLUMN ---
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.add(sectionTitle("Notifications"));
        rightCol.add(fillCard(notifications));
        rightCol.add(Box.createVerticalStrut(12));
        rightCol.add(sectionTitle("Doctor Alert"));
        rightCol.add(fillCard(doctorAlert));
        rightCol.add(Box.createVerticalStrut(12));

        JButton logout = new JButton("Logout");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(e -> {
            int ans = JOptionPane.showConfirmDialog(this, "Logout and close the app?", "Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans == JOptionPane.YES_OPTION) System.exit(0);
        });
        rightCol.add(logout);
        center.add(rightCol, BorderLayout.EAST);

        /* ---------------- BOTTOM ACTIONS ---------------- */
        JPanel bottom = new JPanel(new GridLayout(1, 4, 12, 0));
        bottom.setBorder(new EmptyBorder(6, 0, 0, 0));
        bottom.add(tileButton("Create Appointment", () -> go("Appointments • Create Booking")));
        bottom.add(tileButton("Medical History", () -> go("Medical History • Overview")));
        bottom.add(tileButton("Calendar", () -> go("Calendar")));
        bottom.add(tileButton("Insurance", () -> go("Insurance • Overview")));
        add(bottom, BorderLayout.SOUTH);
    }

    /* ---------- Helpers ---------- */
    private static JTextArea mkArea() {
        JTextArea a = new JTextArea(6, 24);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setBorder(null);
        a.setBackground(new Color(0, 0, 0, 0));
        return a;
    }

    private static JPanel sectionTitle(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel l = UI.h2(text);
        p.add(l);
        p.setOpaque(false);
        return p;
    }

    private static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        p.setBackground(new Color(248, 248, 248));
        return p;
    }

    private static JPanel fillCard(JComponent inner) {
        JPanel p = cardPanel();
        p.setLayout(new BorderLayout());
        p.add(inner, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JButton tileButton(String title, Runnable action) {
        JButton b = new JButton(title);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(200, 46));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.addActionListener(e -> action.run());
        return b;
    }

    @Override
    public void refresh() {
        welcome.setText("Welcome, " + c.getModel().profile.name + "!");
        policyLabel.setText(c.getModel().currentPolicy());

        var m = c.getModel();
        long unpaid = m.invoices.stream().filter(i -> !i.paid).count();
        Optional<Appointment> next = m.appointments.stream()
                .min(Comparator.comparing((Appointment a) -> a.date).thenComparing(a -> a.time));

        StringBuilder nb = new StringBuilder();
        nb.append("• Unpaid invoices: ").append(unpaid).append("\n");
        nb.append("• Reminders enabled: ")
          .append(m.reminders.stream().filter(r -> r.enabled).count()).append("\n");
        if (next.isPresent()) {
            var a = next.get();
            nb.append("• Next appointment: ").append(a.date).append(" ").append(a.time)
              .append(" • ").append(a.service).append(" (").append(a.doctor).append(")");
        } else {
            nb.append("• No upcoming appointments.");
        }
        notifications.setText(nb.toString());

        LocalDate today = LocalDate.now();
        var todayAppt = m.appointments.stream().filter(a -> a.date.equals(today)).findFirst();
        if (todayAppt.isPresent()) {
            var a = todayAppt.get();
            doctorAlert.setText("Doctor available today: " + a.doctor + " — " + a.service +
                    "\nLocation: " + a.medicalCenter +
                    "\nTime: " + a.time +
                    "\n\nNeed to reschedule or cancel? Open Appointments List.");
        } else {
            doctorAlert.setText("No doctor appointments today.\nTip: Create a new appointment or open the calendar to plan ahead.");
        }
    }
}
