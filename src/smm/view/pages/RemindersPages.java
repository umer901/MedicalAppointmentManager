package smm.view.pages;

import smm.controller.Controller;
import smm.model.Reminder;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class RemindersPages {

    public static class DashboardPage extends NavAwarePanel {
        private final Controller c; private final Runnable after;
        private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Type","Text","When","Enabled"}, 0);

        public DashboardPage(Controller c, Runnable after) {
            this.c = c; this.after = after;
            setLayout(new BorderLayout());
            JTable t = new JTable(model) {
                public Class<?> getColumnClass(int col) { return col==4 ? Boolean.class : Object.class; }
                public boolean isCellEditable(int r,int c){ return c==4; }
            };
            t.getModel().addTableModelListener(e -> {
                if (e.getColumn() == 4 && e.getFirstRow() >= 0) {
                    UUID id = (UUID) model.getValueAt(e.getFirstRow(), 0);
                    boolean enabled = (boolean) model.getValueAt(e.getFirstRow(), 4);
                    c.setReminderEnabled(id, enabled);
                    after.run();
                }
            });

            add(UI.h1("Reminders Dashboard"), BorderLayout.NORTH);
            add(new JScrollPane(t), BorderLayout.CENTER);
            add(UI.row(btn("Reminders • Add / Edit")), BorderLayout.SOUTH);
        }

        private JButton btn(String t){ JButton b=new JButton(t); b.addActionListener(e->go(t)); return b; }

        @Override public void refresh() {
            model.setRowCount(0);
            for (Reminder r : c.getModel().reminders) {
                model.addRow(new Object[]{r.id, r.type, r.text, r.when, r.enabled});
            }
        }
    }

    public static class EditPage extends NavAwarePanel {
        private final Controller c; private final Runnable after;
        private final JComboBox<String> cbType = new JComboBox<>(new String[]{"appointment","medication"});
        private final JTextField tfText = new JTextField(20);
        private final JSpinner spWhen = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));

        public EditPage(Controller c, Runnable after) {
            this.c = c; this.after = after;
            setLayout(new BorderLayout());
            ((JSpinner.DateEditor)spWhen.getEditor()).getFormat().applyPattern("yyyy-MM-dd HH:mm");

            JButton add = new JButton("Add Reminder");
            add.addActionListener(e -> {
                Date d = (Date) spWhen.getValue();
                var when = LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
                c.getModel().reminders.add(new Reminder((String) cbType.getSelectedItem(), tfText.getText(), when));
                JOptionPane.showMessageDialog(this, "Reminder added.");
                after.run();
                go("Reminders • Dashboard");
            });

            add(UI.col(UI.h1("Add / Edit Reminder"),
                    UI.row(new JLabel("Type:"), cbType),
                    UI.row(new JLabel("Text:"), tfText),
                    UI.row(new JLabel("When:"), spWhen),
                    add
            ), BorderLayout.NORTH);
        }

        @Override public void refresh() {}
    }
}
