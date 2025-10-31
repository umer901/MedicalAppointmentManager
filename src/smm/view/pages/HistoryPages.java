package smm.view.pages;

import smm.controller.Controller;
import smm.model.HistoryRecord;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

public class HistoryPages {

    public static class OverviewPage extends NavAwarePanel {
        private final Controller c;
        private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Date","Type","Details"}, 0);
        private final JTextField tfSearch = new JTextField(20);
        private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"by date desc","by type","by doctor (demo)"});

        public OverviewPage(Controller c) {
            this.c = c;
            setLayout(new BorderLayout());
            JTable table = new JTable(model); table.setRowHeight(22);

            JButton details = new JButton("Record Details");
            details.addActionListener(e -> go("Medical History • Record Details"));
            JButton add = new JButton("Add Record");
            add.addActionListener(e -> go("Medical History • Add Record"));

            JPanel top = UI.row(UI.h1("History Overview"),
                    new JLabel("Search:"), tfSearch, new JLabel("Sort:"), cbSort);

            cbSort.addActionListener(e -> refresh());
            tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e){ refresh(); }
            });

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(UI.row(details, add), BorderLayout.SOUTH);
        }

        @Override public void refresh() {
            model.setRowCount(0);
            String q = tfSearch.getText().trim().toLowerCase();
            var list = new ArrayList<>(c.getModel().history);
            if (cbSort.getSelectedIndex() == 0) list.sort(Comparator.comparing((HistoryRecord r) -> r.date).reversed());
            else if (cbSort.getSelectedIndex() == 1) list.sort(Comparator.comparing(r -> r.kind));
            for (HistoryRecord r : list) {
                if (q.isEmpty() || r.details.toLowerCase().contains(q) || r.kind.toLowerCase().contains(q)) {
                    model.addRow(new Object[]{r.date, r.kind, r.details});
                }
            }
        }
    }

    public static class RecordDetailsPage extends NavAwarePanel {
        private final Controller c;
        private final JTextArea area = new JTextArea(12, 60);

        public RecordDetailsPage(Controller c) {
            this.c = c;
            setLayout(new BorderLayout());
            area.setEditable(false);
            add(UI.h1("Record Details (demo listing)"), BorderLayout.NORTH);
            add(new JScrollPane(area), BorderLayout.CENTER);
        }

        @Override public void refresh() {
            StringBuilder sb = new StringBuilder();
            for (var r : c.getModel().history) {
                sb.append("• ").append(r.date).append(" • ").append(r.kind).append(" • ").append(r.details).append("\n");
            }
            area.setText(sb.toString());
        }
    }

    public static class AddRecordPage extends NavAwarePanel {
        private final Controller c; private final Runnable after;
        private final JComboBox<String> cbKind = new JComboBox<>(new String[]{"Consultation","Surgery","Prescription"});
        private final JTextField tfDetails = new JTextField(30);

        public AddRecordPage(Controller c, Runnable after) {
            this.c = c; this.after = after;
            setLayout(new BorderLayout());
            JButton add = new JButton("Add");
            add.addActionListener(e -> {
                c.addHistory((String) cbKind.getSelectedItem(), tfDetails.getText());
                JOptionPane.showMessageDialog(this, "Added.");
                after.run();
                go("Medical History • Overview");
            });

            add(UI.col(UI.h1("Add History Record"),
                    UI.row(new JLabel("Type:"), cbKind),
                    UI.row(new JLabel("Details:"), tfDetails),
                    add
            ), BorderLayout.NORTH);
        }

        @Override public void refresh() {}
    }
}
