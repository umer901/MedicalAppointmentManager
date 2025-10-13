package smm.view.pages;

import smm.controller.AppController;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;

public class DashboardPage extends NavAwarePanel {
    private final AppController c;
    private final JLabel welcome = UI.h1("");
    private final JLabel policy = new JLabel("");

    public DashboardPage(AppController c) {
        this.c = c;

        JButton toBook = new JButton("Book Appointment");
        toBook.addActionListener(e -> go("Appointments • Create Booking"));

        JButton doctorUnavailable = new JButton("Simulate: Doctor Unavailable Today");
        doctorUnavailable.addActionListener(e -> {
            var it = c.getModel().appointments.iterator();
            var today = java.time.LocalDate.now();
            boolean affected = false;
            while (it.hasNext()) {
                var a = it.next();
                if (a.date.equals(today)) { it.remove(); affected = true; }
            }
            JOptionPane.showMessageDialog(this, affected ? "Today's appointments cancelled." : "No appointments today.");
            go("Appointments • List");
        });

        JButton completeNext = new JButton("Simulate: Complete Next Upcoming");
        completeNext.addActionListener(e -> {
            var m = c.getModel();
            m.appointments.stream()
                .min(java.util.Comparator.comparing((smm.model.Appointment a)->a.date).thenComparing(a->a.time))
                .ifPresent(a -> {
                    m.history.add(new smm.model.HistoryRecord(java.time.LocalDate.now(), "Consultation", a.type + " completed"));
                    var follow = new smm.model.Appointment(
                            a.date.plusDays(14), a.time, "Follow-up", a.service, a.doctor, a.medicalCenter, a.roomType, a.equipment, a.price * 0.6
                    );
                    m.createAppointment(follow, true, false);
                    m.appointments.remove(a);
                });
            JOptionPane.showMessageDialog(this, "Processed next upcoming appointment.");
            go("Medical History • Overview");
        });

        setLayout(new BorderLayout());
        add(UI.col(
                welcome,
                UI.row(new JLabel("Insurance: "), policy),
                UI.h2("Shortcuts"),
                UI.row(btn("Appointments • Create Booking"),
                       btn("Medical History • Overview"),
                       btn("Payment • Billing & Payments"),
                       btn("Reminders • Dashboard"),
                       btn("Calendar")),
                UI.h2("Notifications"),
                new JLabel("• You have a follow-up appointment tomorrow (demo)."),
                UI.h2("Simulator"),
                UI.row(doctorUnavailable, completeNext)
        ), BorderLayout.NORTH);
    }

    private JButton btn(String target) {
        JButton b = new JButton(target);
        b.addActionListener(e -> go(target));
        return b;
    }

    @Override public void refresh() {
        welcome.setText("Welcome, " + c.getModel().profile.name + "!");
        policy.setText(c.getModel().currentPolicy());
    }
}
