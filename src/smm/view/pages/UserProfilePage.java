package smm.view.pages;

import smm.controller.Controller;
import smm.view.NavAwarePanel;

import javax.swing.*;
import java.awt.*;

public class UserProfilePage extends NavAwarePanel {
    private final Controller c;
    private final Runnable after;

    private final JTextField tfName = new JTextField(16);
    private final JTextField tfEmail = new JTextField(16);
    private final JTextField tfPhone = new JTextField(16);

    private final JCheckBox cbEmail = new JCheckBox("Email");
    private final JCheckBox cbSMS = new JCheckBox("SMS");
    private final JCheckBox cbInApp = new JCheckBox("In-App");
    private final JCheckBox cb2FA = new JCheckBox("Enable 2FA");

    public UserProfilePage(Controller c, Runnable after) {
        this.c = c;
        this.after = after;

        setLayout(new BorderLayout());

        // Basic Information Panel
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Basic Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        infoPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(tfName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        infoPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(tfEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        infoPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(tfPhone, gbc);

        // Notification Panel
        JPanel notifPanel = new JPanel(new GridBagLayout());
        notifPanel.setBorder(BorderFactory.createTitledBorder("Notification Preferences"));
        GridBagConstraints nGbc = new GridBagConstraints();
        nGbc.insets = new Insets(6, 10, 6, 10);
        nGbc.anchor = GridBagConstraints.WEST;

        nGbc.gridx = 0; nGbc.gridy = 0;
        notifPanel.add(new JLabel("Channels:"), nGbc);

        nGbc.gridx = 1;
        notifPanel.add(cbEmail, nGbc);
        nGbc.gridx = 2;
        notifPanel.add(new JLabel("Email notification"), nGbc);

        nGbc.gridx = 1; nGbc.gridy++;
        notifPanel.add(cbSMS, nGbc);
        nGbc.gridx = 2;
        notifPanel.add(new JLabel("SMS notification"), nGbc);

        nGbc.gridx = 1; nGbc.gridy++;
        notifPanel.add(cbInApp, nGbc);
        nGbc.gridx = 2;
        notifPanel.add(new JLabel("In-app notification"), nGbc);

        nGbc.gridx = 1; nGbc.gridy++;
        notifPanel.add(cb2FA, nGbc);
        nGbc.gridx = 2;
        notifPanel.add(new JLabel("Enable 2FA"), nGbc);

        // Save Button
        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            c.getModel().profile.name = tfName.getText();
            c.getModel().profile.email = tfEmail.getText();
            c.getModel().profile.phoneNumber = tfPhone.getText();
            c.setNotificationPrefs(cbEmail.isSelected(), cbSMS.isSelected(), cbInApp.isSelected());
            c.setTwoFA(cb2FA.isSelected());
            JOptionPane.showMessageDialog(this, "Profile saved.");
            after.run();
        });

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(infoPanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(notifPanel);
        centerPanel.add(Box.createVerticalStrut(20));

        add(centerPanel, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        var p = c.getModel().profile;
        tfName.setText(p.name);
        tfEmail.setText(p.email);
        tfPhone.setText(p.phoneNumber);
        cbEmail.setSelected(p.notifEmail);
        cbSMS.setSelected(p.notifSMS);
        cbInApp.setSelected(p.notifInApp);
        cb2FA.setSelected(p.twoFA);
    }
}
