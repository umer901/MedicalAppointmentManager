package smm.view.pages;

import smm.controller.Controller;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;

public class DoctorAdminPage extends NavAwarePanel {
    private final Controller c;
    private final JTextArea area = new JTextArea(14, 60);

    public DoctorAdminPage(Controller c) {
        this.c = c;
        setLayout(new BorderLayout());
        area.setEditable(false);
        add(UI.h1("Doctor / Admin View (demo)"), BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    @Override public void refresh() {
        StringBuilder sb = new StringBuilder("Assigned Appointments (demo):\n");
        for (var a : c.getModel().appointments) {
            sb.append("• ").append(a.date).append(" ").append(a.time).append(" – ").append(a.service)
              .append(" with ").append(a.doctor).append("\n");
        }
        sb.append("\nManage pricing rules (not implemented in demo).");
        area.setText(sb.toString());
    }
}
