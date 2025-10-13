package smm.view.pages;

import smm.controller.AppController;
import smm.model.Appointment;
import smm.model.InsuranceLevel;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class AppointmentsPages {

    /* -------- List Page -------- */
    public static class ListPage extends NavAwarePanel {
        private final AppController c;
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID","Date","Time","Type","Service","Doctor","Center","Paid","Price"}, 0);
        private final JTable table = new JTable(model);
        private final JTextField tfDoctor = new JTextField(10);
        private final JComboBox<String> cbType = new JComboBox<>(new String[]{"All","Consultation","Surgery","Follow-up"});
        private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"Date ↑","Date ↓","Type","Doctor"});

        public ListPage(AppController c) {
            this.c = c;
            setLayout(new BorderLayout());

            table.setRowHeight(22);
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setPreferredWidth(0);

            JButton details = new JButton("View Details");
            details.addActionListener(e -> {
                int i = table.getSelectedRow();
                if (i < 0) { JOptionPane.showMessageDialog(this, "Select a row first"); return; }
                UUID id = (UUID) model.getValueAt(i, 0);
                c.setSelectedAppointment(id);
                go("Appointments • Details");
            });

            JButton cancel = new JButton("Cancel (demo)");
            cancel.addActionListener(e -> JOptionPane.showMessageDialog(this, "Cancellation demo"));

            JPanel top = UI.col(
                    UI.h1("Appointments List"),
                    UI.row(new JLabel("Type:"), cbType, new JLabel("Doctor:"), tfDoctor, new JLabel("Sort:"), cbSort)
            );
            cbType.addActionListener(e -> refresh());
            cbSort.addActionListener(e -> refresh());
            tfDoctor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
            });

            JPanel bottom = UI.row(details, cancel, btn("Appointments • Create Booking"));

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private JButton btn(String t){ JButton b=new JButton(t); b.addActionListener(e->go(t)); return b; }

        @Override public void refresh() {
            model.setRowCount(0);
            var M = c.getModel();
            String wantType = Objects.toString(cbType.getSelectedItem(), "All");
            String doctorQ = tfDoctor.getText().trim().toLowerCase();

            java.util.List<Appointment> list = new ArrayList<>(M.appointments);
            switch (Objects.toString(cbSort.getSelectedItem(), "")) {
                case "Date ↑" -> list.sort(Comparator.comparing((Appointment a)->a.date).thenComparing(a->a.time));
                case "Date ↓" -> list.sort(Comparator.comparing((Appointment a)->a.date).thenComparing(a->a.time).reversed());
                case "Type"   -> list.sort(Comparator.comparing(a->a.type));
                case "Doctor" -> list.sort(Comparator.comparing(a->a.doctor));
            }

            for (Appointment a : list) {
                boolean okType = wantType.equals("All") || a.type.equalsIgnoreCase(wantType);
                boolean okDoc  = doctorQ.isEmpty() || a.doctor.toLowerCase().contains(doctorQ);
                if (okType && okDoc) {
                    model.addRow(new Object[]{
                        a.id, a.date, a.time, a.type, a.service, a.doctor, a.medicalCenter,
                        a.paid ? "Yes" : "No", M.priceAfterInsurance(a.price)
                    });
                }
            }
        }
    }

    /* -------- Create Booking -------- */
    public static class CreatePage extends NavAwarePanel {
        private final AppController c; private final Runnable after;
        private final JComboBox<String> cbType = new JComboBox<>(new String[]{"Consultation","Surgery","Follow-up"});
        private final JComboBox<String> cbService = new JComboBox<>(new String[]{"Dermatology","Cardiology","Radiology","General"});
        private final JTextField tfDoctor = new JTextField("Dr. Smith", 12);
        private final JTextField tfCenter = new JTextField("St-Luc", 10);
        private final JComboBox<String> cbRoom = new JComboBox<>(new String[]{"Shared","Private"});
        private final JTextField tfEquip = new JTextField("—", 10);
        private final JSpinner spDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        private final JSpinner spTime = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        private final JSpinner spPrice = new JSpinner(new SpinnerNumberModel(100.0, 0, 10000, 10));
        private final JCheckBox chkCalendar = new JCheckBox("Add to Calendar", true);
        private final JCheckBox chkPayNow   = new JCheckBox("Pay Now");

        public CreatePage(AppController c, Runnable afterSave) {
            this.c = c; this.after = afterSave;
            setLayout(new BorderLayout());
            ((JSpinner.DateEditor)spDate.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
            ((JSpinner.DateEditor)spTime.getEditor()).getFormat().applyPattern("HH:mm");

            JButton create = new JButton("Create Booking");
            create.addActionListener(e -> {
                var date = LocalDate.parse(new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) spDate.getValue()));
                var time = LocalTime.parse(new java.text.SimpleDateFormat("HH:mm").format((Date) spTime.getValue()));
                var a = c.createBooking(date, time,
                        (String) cbType.getSelectedItem(),
                        (String) cbService.getSelectedItem(),
                        tfDoctor.getText(), tfCenter.getText(),
                        (String) cbRoom.getSelectedItem(), tfEquip.getText(),
                        ((Number) spPrice.getValue()).doubleValue(),
                        chkCalendar.isSelected(), chkPayNow.isSelected());
                JOptionPane.showMessageDialog(this, "Created:\n" + a.summary());
                after.run();
                go("Appointments • List");
            });

            add(UI.col(
                    UI.h1("Create Booking"),
                    UI.row(new JLabel("Type:"), cbType, new JLabel("Service:"), cbService),
                    UI.row(new JLabel("Doctor:"), tfDoctor, new JLabel("Center:"), tfCenter),
                    UI.row(new JLabel("Room:"), cbRoom, new JLabel("Equipment:"), tfEquip),
                    UI.row(new JLabel("Date:"), spDate, new JLabel("Time:"), spTime),
                    UI.row(new JLabel("Base Price (€):"), spPrice),
                    UI.row(chkCalendar, chkPayNow),
                    create
            ), BorderLayout.NORTH);
        }

        @Override public void refresh() {
            boolean premiumOrNormal = c.getModel().profile.insurance != InsuranceLevel.MINIMAL;
            cbRoom.setEnabled(true);
            if (!premiumOrNormal) cbRoom.setSelectedItem("Shared");
            cbRoom.setEnabled(premiumOrNormal);
        }
    }

    /* -------- Details -------- */
    public static class DetailsPage extends NavAwarePanel {
        private final AppController c;
        private final JTextArea area = new JTextArea(14, 60);

        public DetailsPage(AppController c) {
            this.c = c;
            setLayout(new BorderLayout());
            area.setEditable(false);
            JButton resched = new JButton("Reschedule (demo)");
            JButton cancel = new JButton("Cancel (demo)");
            JButton payNow = new JButton("Pay Now");
            payNow.addActionListener(e -> {
                // pay first unpaid invoice if present
                c.getModel().invoices.stream().filter(i -> !i.paid).findFirst()
                        .ifPresent(i -> { c.payInvoice(i.id); JOptionPane.showMessageDialog(this, "Invoice paid."); });
            });

            add(UI.h1("Appointment Details"), BorderLayout.NORTH);
            add(new JScrollPane(area), BorderLayout.CENTER);
            add(UI.row(resched, cancel, payNow), BorderLayout.SOUTH);
        }

        @Override public void refresh() {
            var m = c.getModel();
            var a = c.getSelectedAppointment();
            StringBuilder sb = new StringBuilder();
            if (a == null) {
                sb.append("No appointment selected.\n\nAll appointments:\n");
                for (Appointment ap : m.appointments) {
                    sb.append("• ").append(ap.summary()).append(" | After insurance: ")
                      .append(m.priceAfterInsurance(ap.price)).append("€ | Paid=").append(ap.paid).append("\n");
                }
            } else {
                sb.append(a.summary()).append("\n\n")
                  .append("Base price: ").append(a.price).append("€\n")
                  .append("After insurance: ").append(m.priceAfterInsurance(a.price)).append("€\n")
                  .append("Room: ").append(a.roomType).append(" | Equipment: ").append(a.equipment).append("\n")
                  .append("Paid: ").append(a.paid ? "Yes" : "No").append("\n");
            }
            area.setText(sb.toString());
        }
    }
}
