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
import java.util.List;

public class AppointmentsPages {

/* -------- List Page (improved + fixes) -------- */
/* -------- List Page (with Details popup) -------- */
public static class ListPage extends NavAwarePanel {
    private final AppController c;

    // --- Table
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Date","Time","Type","Service","Doctor","Center","Paid","Price (€)"}, 0) {
        @Override public boolean isCellEditable(int r,int c){ return false; }
        @Override public Class<?> getColumnClass(int c){
            return switch (c){ case 0 -> UUID.class; case 1 -> java.time.LocalDate.class; case 2 -> java.time.LocalTime.class;
                               case 7 -> String.class; case 8 -> Double.class; default -> String.class; };
        }
    };
    private final JTable table = new JTable(model);

    // --- Filters
    private final JComboBox<String> cbType = new JComboBox<>(new String[]{"All","Consultation","Surgery","Follow-up"});
    private final JTextField tfDoctor = new JTextField(14);
    private final JSpinner spFrom = new JSpinner(new SpinnerDateModel(new Date(System.currentTimeMillis()-7L*86400000), null, null, Calendar.DAY_OF_MONTH));
    private final JSpinner spTo   = new JSpinner(new SpinnerDateModel(new Date(System.currentTimeMillis()+30L*86400000), null, null, Calendar.DAY_OF_MONTH));
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"Date ↑","Date ↓","Type","Doctor"});

    public ListPage(AppController c) {
        this.c = c;
        setLayout(new BorderLayout(8,8));

        // Table
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        ((JSpinner.DateEditor)spFrom.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
        ((JSpinner.DateEditor)spTo.getEditor()).getFormat().applyPattern("yyyy-MM-dd");

        // Filters row (GridBag so the Doctor field grows)
        JPanel filters = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0,6,0,6);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=10; gc.anchor=GridBagConstraints.WEST;
        filters.add(UI.h1("Appointments List"), gc);
        gc.gridwidth=1;

        gc.gridy=1; gc.anchor=GridBagConstraints.WEST; gc.fill = GridBagConstraints.NONE; gc.weightx=0;
        gc.gridx=0; filters.add(new JLabel("Type:"), gc);
        gc.gridx=1; filters.add(cbType, gc);

        gc.gridx=2; filters.add(new JLabel("Doctor:"), gc);
        gc.gridx=3; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx=1.0;
        filters.add(tfDoctor, gc);

        gc.gridx=4; gc.fill = GridBagConstraints.NONE; gc.weightx=0; filters.add(new JLabel("From:"), gc);
        gc.gridx=5; filters.add(spFrom, gc);
        gc.gridx=6; filters.add(new JLabel("To:"), gc);
        gc.gridx=7; filters.add(spTo, gc);

        gc.gridx=8; filters.add(new JLabel("Sort:"), gc);
        gc.gridx=9; filters.add(cbSort, gc);

        add(filters, BorderLayout.NORTH);

        // Actions (right column)
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JButton btnView   = wideBtn("View");
        JButton btnModify = wideBtn("Modify");
        JButton btnRemove = wideBtn("Remove");
        JButton btnPay    = wideBtn("Pay");
        actions.add(btnView); actions.add(Box.createVerticalStrut(6));
        actions.add(btnModify); actions.add(Box.createVerticalStrut(6));
        actions.add(btnRemove); actions.add(Box.createVerticalStrut(6));
        actions.add(btnPay);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);

        // Bottom
        JPanel bottom = new JPanel(new BorderLayout());
        JButton create = new JButton("Create Appointment");
        create.addActionListener(e -> go("Appointments • Create Booking"));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            int ans = JOptionPane.showConfirmDialog(this, "Logout and close the app?", "Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans == JOptionPane.YES_OPTION) System.exit(0);
        });
        bottom.add(create, BorderLayout.WEST);
        bottom.add(logout, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Live filters
        cbType.addActionListener(e -> refresh());
        cbSort.addActionListener(e -> refresh());
        tfDoctor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
        });
        spFrom.addChangeListener(e -> refresh());
        spTo.addChangeListener(e -> refresh());

        // Actions
        btnView.addActionListener(e -> {
            var a = selectedApptOrWarn();
            if (a == null) return;
            new DetailsDialog(SwingUtilities.getWindowAncestor(this), a).setVisible(true);
            refresh();
        });

        btnModify.addActionListener(e -> {
            var a = selectedApptOrWarn();
            if (a == null) return;
            new EditAppointmentDialog(SwingUtilities.getWindowAncestor(this), a).setVisible(true);
            refresh();
        });

        btnRemove.addActionListener(e -> {
            var a = selectedApptOrWarn();
            if (a == null) return;
            int ok = JOptionPane.showConfirmDialog(this, "Remove the selected appointment?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                c.getModel().invoices.removeIf(inv -> Objects.equals(inv.appointmentId, a.id));
                c.getModel().appointments.remove(a);
                refresh();
            }
        });

        btnPay.addActionListener(e -> paySelected());
    }

    private void paySelected() {
        var a = selectedApptOrWarn();
        if (a == null) return;

        var invUnpaid = c.getModel().invoices.stream()
                .filter(i -> Objects.equals(i.appointmentId, a.id) && !i.paid)
                .findFirst();
        if (invUnpaid.isPresent()) {
            c.payInvoice(invUnpaid.get().id);
            a.paid = true;
            JOptionPane.showMessageDialog(this, "Invoice paid.");
            refresh();
            return;
        }
        var invAny = c.getModel().invoices.stream().filter(i -> Objects.equals(i.appointmentId, a.id)).findFirst();
        if (invAny.isPresent() && invAny.get().paid) {
            JOptionPane.showMessageDialog(this, "This appointment is already paid.");
            return;
        }
        double amount = c.getModel().priceAfterInsurance(a.price);
        c.getModel().invoices.add(new smm.model.Invoice(java.time.LocalDate.now(), amount, true, a.id));
        a.paid = true;
        JOptionPane.showMessageDialog(this, "Invoice created and paid (" + amount + "€).");
        refresh();
    }

    private JButton wideBtn(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setPreferredSize(new Dimension(120, 28));
        b.setMaximumSize(new Dimension(160, 28));
        return b;
    }

    private UUID selectedIdOrWarn() {
        int i = table.getSelectedRow();
        if (i < 0) { JOptionPane.showMessageDialog(this, "Select a row first"); return null; }
        return (UUID) model.getValueAt(i, 0);
    }
    private smm.model.Appointment selectedApptOrWarn() {
        UUID id = selectedIdOrWarn(); if (id == null) return null;
        return c.getModel().appointments.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
    }

    @Override public void refresh() {
        model.setRowCount(0);
        var M = c.getModel();

        String wantType = Objects.toString(cbType.getSelectedItem(), "All");
        String doctorQ = tfDoctor.getText().trim().toLowerCase();
        LocalDate from = LocalDate.parse(new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) spFrom.getValue()));
        LocalDate to   = LocalDate.parse(new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) spTo.getValue()));

        List<Appointment> list = new ArrayList<>(M.appointments);
        switch (Objects.toString(cbSort.getSelectedItem(), "")) {
            case "Date ↑" -> list.sort(Comparator.comparing((Appointment a)->a.date).thenComparing(a->a.time));
            case "Date ↓" -> list.sort(Comparator.comparing((Appointment a)->a.date).thenComparing(a->a.time).reversed());
            case "Type"   -> list.sort(Comparator.comparing(a->a.type));
            case "Doctor" -> list.sort(Comparator.comparing(a->a.doctor));
        }

        for (Appointment a : list) {
            boolean okType = wantType.equals("All") || a.type.equalsIgnoreCase(wantType);
            boolean okDoc  = doctorQ.isEmpty() || a.doctor.toLowerCase().contains(doctorQ);
            boolean okDate = (!a.date.isBefore(from) && !a.date.isAfter(to));
            if (okType && okDoc && okDate) {
                model.addRow(new Object[]{
                        a.id, a.date, a.time, a.type, a.service, a.doctor, a.medicalCenter,
                        a.paid ? "Yes" : "No", M.priceAfterInsurance(a.price)
                });
            }
        }
    }

    /* ---------- Details popup ---------- */
    private class DetailsDialog extends JDialog {
        private final smm.model.Appointment a;
        private final JTextArea info = new JTextArea(10, 50);

        DetailsDialog(Window owner, smm.model.Appointment a) {
            super(owner, "Appointment Details", ModalityType.APPLICATION_MODAL);
            this.a = a;

            info.setEditable(false);
            info.setLineWrap(true);
            info.setWrapStyleWord(true);

            JButton payNow = new JButton("Pay Now");
            payNow.addActionListener(e -> { paySelected(); dispose(); });

            JButton resched = new JButton("Reschedule (demo)");
            JButton cancel  = new JButton("Cancel (demo)");

            JPanel root = new JPanel(new BorderLayout(8,8));
            root.add(UI.h1("Appointment Details"), BorderLayout.NORTH);
            root.add(new JScrollPane(info), BorderLayout.CENTER);
            root.add(UI.row(resched, cancel, payNow), BorderLayout.SOUTH);
            setContentPane(root);
            pack();
            setLocationRelativeTo(owner);
            fill();
        }

        private void fill() {
            var m = c.getModel();
            StringBuilder sb = new StringBuilder();
            sb.append(a.summary()).append("\n\n")
              .append("Base price: ").append(a.price).append("€\n")
              .append("After insurance: ").append(m.priceAfterInsurance(a.price)).append("€\n")
              .append("Paid: ").append(a.paid ? "Yes" : "No").append("\n");
            var inv = m.invoices.stream().filter(i -> Objects.equals(i.appointmentId, a.id)).findFirst();
            inv.ifPresent(invoice -> sb.append("Invoice: ").append(invoice.id).append(" • ").append(invoice.issuedOn)
                    .append(" • ").append(invoice.paid ? "Paid" : "Unpaid").append("\n"));
            info.setText(sb.toString());
        }
    }
    /* ---------- Edit dialog ---------- */
    private class EditAppointmentDialog extends JDialog {
        private final Appointment appt;
        private final JTextField tfType = new JTextField(12);
        private final JTextField tfService = new JTextField(12);
        private final JTextField tfDoctor = new JTextField(12);
        private final JTextField tfCenter = new JTextField(12);
        private final JComboBox<String> cbRoom = new JComboBox<>(new String[]{"Shared","Private"});
        private final JTextField tfEquip = new JTextField(10);
        private final JSpinner spDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        private final JSpinner spTime = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        private final JSpinner spPrice = new JSpinner(new SpinnerNumberModel(100.0, 0, 10000, 10));
        private final JCheckBox cbPaid = new JCheckBox("Paid");

        EditAppointmentDialog(Window owner, Appointment appt) {
            super(owner, "Modify Appointment", ModalityType.APPLICATION_MODAL);
            this.appt = appt;

            ((JSpinner.DateEditor)spDate.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
            ((JSpinner.DateEditor)spTime.getEditor()).getFormat().applyPattern("HH:mm");

            // preload
            tfType.setText(appt.type);
            tfService.setText(appt.service);
            tfDoctor.setText(appt.doctor);
            tfCenter.setText(appt.medicalCenter);
            cbRoom.setSelectedItem(appt.roomType == null ? "Shared" : appt.roomType);
            tfEquip.setText(appt.equipment == null ? "—" : appt.equipment);
            try {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(appt.date.getYear(), appt.date.getMonthValue()-1, appt.date.getDayOfMonth(),
                        appt.time.getHour(), appt.time.getMinute(), 0);
                spDate.setValue(cal.getTime());
                spTime.setValue(cal.getTime());
            } catch (Exception ignored) {}
            // spPrice.setValue(appt.price);
            cbPaid.setSelected(appt.paid);

            JPanel form = UI.col(
                    UI.row(new JLabel("Type:"), tfType, new JLabel("Service:"), tfService),
                    UI.row(new JLabel("Doctor:"), tfDoctor, new JLabel("Center:"), tfCenter),
                    UI.row(new JLabel("Room:"), cbRoom, new JLabel("Equipment:"), tfEquip),
                    UI.row(new JLabel("Date:"), spDate, new JLabel("Time:"), spTime)
                    // UI.row(new JLabel("Base Price (€):"), spPrice, cbPaid)
            );

            JButton save = new JButton("Save");
            save.addActionListener(e -> {
                appt.type = tfType.getText().trim();
                appt.service = tfService.getText().trim();
                appt.doctor = tfDoctor.getText().trim();
                appt.medicalCenter = tfCenter.getText().trim();
                appt.roomType = Objects.toString(cbRoom.getSelectedItem(), "Shared");
                appt.equipment = tfEquip.getText().trim();
                appt.date = LocalDate.parse(new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) spDate.getValue()));
                appt.time = java.time.LocalTime.parse(new java.text.SimpleDateFormat("HH:mm").format((Date) spTime.getValue()));
                appt.price = ((Number) spPrice.getValue()).doubleValue();
                appt.paid = cbPaid.isSelected();
                dispose();
            });
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());

            JPanel root = new JPanel(new BorderLayout(8,8));
            root.add(UI.h1("Modify Appointment"), BorderLayout.NORTH);
            root.add(form, BorderLayout.CENTER);
            root.add(UI.row(save, cancel), BorderLayout.SOUTH);
            setContentPane(root);
            pack();
            setLocationRelativeTo(owner);
        }
    }
}


 /* -------- Create Booking (updated) -------- */
public static class CreatePage extends NavAwarePanel {
    private final AppController c;
    private final Runnable after;

    private final JComboBox cbType = new JComboBox<>(new String[]{"Consultation", "Surgery", "Follow-up"});
    private final JComboBox cbService = new JComboBox<>(new String[]{"Dermatology", "Cardiology", "Radiology", "General"});
    private final JTextField tfCenter = new JTextField("St-Luc", 12);

    // Doctor list (scrollable)
    private final DefaultListModel<String> doctorsModel = new DefaultListModel<>();
    private final JList<String> listDoctors = new JList<>(doctorsModel);

    private final JSpinner spDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JSpinner spTime = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));

    // Remove user input for price
    private final JLabel lbComputed = new JLabel("You pay: —");
    private final JCheckBox chkCalendar = new JCheckBox("Add to Calendar", true);
    private final JCheckBox chkPayNow = new JCheckBox("Pay Now");

    // Map doctor to base price
    private static final Map<String, Double> DOCTOR_PRICES;
    static {
        DOCTOR_PRICES = new HashMap<>();
        DOCTOR_PRICES.put("Dr. Martin", 120.0);
        DOCTOR_PRICES.put("Dr. Lambert", 100.0);
        DOCTOR_PRICES.put("Dr. Rossi", 130.0);
        DOCTOR_PRICES.put("Dr. Duval", 150.0);
        DOCTOR_PRICES.put("Dr. Bernard", 110.0);
        DOCTOR_PRICES.put("Dr. Kassis", 115.0);
        DOCTOR_PRICES.put("Dr. Selim", 140.0);
        DOCTOR_PRICES.put("Dr. Pereira", 125.0);
        DOCTOR_PRICES.put("Dr. Smith", 90.0);
        DOCTOR_PRICES.put("Dr. Patel", 100.0);
        DOCTOR_PRICES.put("Dr. Garcia", 95.0);
    }

    private static final Map<String, String[]> DOCTORS = Map.of(
        "Dermatology", new String[]{"Dr. Martin","Dr. Lambert","Dr. Rossi"},
        "Cardiology", new String[]{"Dr. Duval","Dr. Bernard","Dr. Kassis"},
        "Radiology", new String[]{"Dr. Selim","Dr. Pereira"},
        "General", new String[]{"Dr. Smith","Dr. Patel","Dr. Garcia"}
    );

    public CreatePage(AppController c, Runnable afterSave) {
        this.c = c; this.after = afterSave;

        setLayout(new BorderLayout(16,0));
        ((JSpinner.DateEditor)spDate.getEditor()).getFormat().applyPattern("yyyy-MM-dd");
        ((JSpinner.DateEditor)spTime.getEditor()).getFormat().applyPattern("HH:mm");

        listDoctors.setVisibleRowCount(6);
        listDoctors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // LEFT: form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(UI.h1("Create Booking"));
        form.add(Box.createVerticalStrut(6));
        form.add(UI.row(new JLabel("Type:"), cbType, new JLabel("Service:"), cbService));
        form.add(Box.createVerticalStrut(4));
        JScrollPane spDoctors = new JScrollPane(listDoctors);
        spDoctors.setPreferredSize(new Dimension(220, 120));
        form.add(UI.row(new JLabel("Doctor:"), spDoctors, new JLabel("Center:"), tfCenter));
        form.add(Box.createVerticalStrut(4));
        form.add(UI.row(new JLabel("Date:"), spDate, new JLabel("Time:"), spTime));
        form.add(Box.createVerticalStrut(4));
        // User does not input price anymore
        lbComputed.setFont(lbComputed.getFont().deriveFont(Font.BOLD));
        form.add(UI.row(lbComputed));
        form.add(Box.createVerticalStrut(6));
        form.add(UI.row(chkCalendar, chkPayNow));
        form.add(Box.createVerticalStrut(8));
        JButton create = new JButton("Create Booking");
        create.addActionListener(e -> onCreate());
        JPanel centerCreate = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerCreate.add(create);
        form.add(centerCreate);
        add(form, BorderLayout.CENTER);

        // RIGHT: logo card
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(320, 320));
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("logoapp.png"));
            Image scaled = raw.getImage().getScaledInstance(260, 260, Image.SCALE_SMOOTH);
            JLabel logo = new JLabel(new ImageIcon(scaled));
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            right.add(logo, BorderLayout.CENTER);
        } catch (Exception ex) {
            right.add(new JLabel("Logo not found", SwingConstants.CENTER), BorderLayout.CENTER);
        }
        add(right, BorderLayout.EAST);

        // listeners
        cbService.addActionListener(e -> refreshDoctors());
        listDoctors.addListSelectionListener(e -> updateComputedPrice());

        // initial
        refreshDoctors();
        updateComputedPrice();
    }

    private void refreshDoctors() {
        doctorsModel.clear();
        String svc = Objects.toString(cbService.getSelectedItem(), "General");
        for (String d : DOCTORS.getOrDefault(svc, DOCTORS.get("General"))) doctorsModel.addElement(d);
        if (!doctorsModel.isEmpty()) listDoctors.setSelectedIndex(0);
    }

    private void updateComputedPrice() {
        String doctor = listDoctors.getSelectedValue();
        double base = DOCTOR_PRICES.getOrDefault(doctor, 100.0);
        double due = c.getModel().priceAfterInsurance(base);
        lbComputed.setText("You pay: " + String.format(java.util.Locale.ROOT, "%.2f€", due)
            + " (Policy: " + c.getModel().currentPolicy() + ")");
    }

    private void onCreate() {
        String doctor = listDoctors.getSelectedValue();
        if (doctor == null || doctor.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please choose a doctor from the list.");
            return;
        }
        var date = LocalDate.parse(new java.text.SimpleDateFormat("yyyy-MM-dd").format((Date) spDate.getValue()));
        var time = LocalTime.parse(new java.text.SimpleDateFormat("HH:mm").format((Date) spTime.getValue()));
        double base = DOCTOR_PRICES.getOrDefault(doctor, 100.0);
        var a = c.createBooking(
            date, time,
            (String) cbType.getSelectedItem(),
            (String) cbService.getSelectedItem(),
            doctor, tfCenter.getText(),
            null, null,
            base,
            chkCalendar.isSelected(), chkPayNow.isSelected()
        );
        JOptionPane.showMessageDialog(this,
            "Created:\n" + a.summary() +
            "\nYou pay now: " + String.format(java.util.Locale.ROOT, "%.2f€", c.getModel().priceAfterInsurance(a.price)));
        after.run();
        go("Appointments • List");
    }

    @Override public void refresh() {
        updateComputedPrice(); // recompute in case insurance changed
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
