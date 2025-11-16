package smm.view.pages;
import smm.controller.Controller;
import smm.view.NavAwarePanel;
import smm.view.UI;
import javax.swing.*;
import java.awt.*;

public class CalendarPage extends NavAwarePanel {
    private final Controller c;
    private final JTextArea area = new JTextArea(14, 60);

    public CalendarPage(Controller c) {
        this.c = c;
        setLayout(new BorderLayout());
        area.setEditable(false);
        add(UI.h1("Calendar (Day/Week/Month – demo listing)"), BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(UI.row(btn("Appointments • Create Booking")), BorderLayout.SOUTH);
    }

    private JButton btn(String t){ JButton b=new JButton(t); b.addActionListener(e->go(t)); return b; }

    @Override public void refresh() {
        StringBuilder sb = new StringBuilder();
        for (var a : c.getModel().appointments) {
            sb.append("• ").append(a.date).append(" ").append(a.time).append(" – ")
              .append(a.type).append(" @ ").append(a.medicalCenter).append("\n");
        }
        for (var r : c.getModel().reminders) {
            sb.append("• [Reminder] ").append(r.when).append(" – ").append(r.text).append("\n");
        }
        area.setText(sb.toString());
    }
}
