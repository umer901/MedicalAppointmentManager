package smm.view.pages;

import smm.controller.AppController;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;

public class UserProfilePage extends NavAwarePanel {
    private final AppController c; private final Runnable after;
    private final JTextField tfName = new JTextField(16);
    private final JCheckBox cbEmail = new JCheckBox("Email"), cbSMS = new JCheckBox("SMS"), cbInApp = new JCheckBox("In-App");
    private final JCheckBox cb2FA = new JCheckBox("Enable 2FA");

    public UserProfilePage(AppController c, Runnable after) {
        this.c = c; this.after = after;
        setLayout(new BorderLayout());
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            c.getModel().profile.name = tfName.getText();
            c.setNotificationPrefs(cbEmail.isSelected(), cbSMS.isSelected(), cbInApp.isSelected());
            c.setTwoFA(cb2FA.isSelected());
            JOptionPane.showMessageDialog(this, "Profile saved.");
            after.run();
        });

        add(UI.col(UI.h1("User Profile"),
                UI.row(new JLabel("Name:"), tfName),
                UI.h2("Notification Preferences"),
                UI.row(cbEmail, cbSMS, cbInApp),
                UI.row(cb2FA),
                save
        ), BorderLayout.NORTH);
    }

    @Override public void refresh() {
        var p = c.getModel().profile;
        tfName.setText(p.name);
        cbEmail.setSelected(p.notifEmail);
        cbSMS.setSelected(p.notifSMS);
        cbInApp.setSelected(p.notifInApp);
        cb2FA.setSelected(p.twoFA);
    }
}
