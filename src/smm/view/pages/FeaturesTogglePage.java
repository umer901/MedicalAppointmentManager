package smm.view.pages;

import smm.controller.Controller;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FeaturesTogglePage extends NavAwarePanel {
    private final Controller c;

    private final JCheckBox cbAppointments = new JCheckBox("Appointments");
    private final JCheckBox cbHistory      = new JCheckBox("Medical History");
    private final JCheckBox cbPayment      = new JCheckBox("Payment");
    private final JCheckBox cbReminders    = new JCheckBox("Reminders");

    private final ButtonGroup insGroup = new ButtonGroup();
    private final JRadioButton rbMin  = new JRadioButton("Insurance: Minimal");
    private final JRadioButton rbNorm = new JRadioButton("Insurance: Normal");
    private final JRadioButton rbPrem = new JRadioButton("Insurance: Premium");

    public FeaturesTogglePage(Controller controller) {
        this.c = controller;
        setLayout(new BorderLayout(10,10));

        insGroup.add(rbMin); insGroup.add(rbNorm); insGroup.add(rbPrem);

        JButton apply = new JButton("Apply changes");
        apply.addActionListener(e -> applyChanges());

        add(UI.col(
                UI.h1("Feature Toggle"),
                UI.h2("Modules"),
                cbAppointments, cbHistory, cbPayment, cbReminders,
                UI.h2("Insurance"),
                rbMin, rbNorm, rbPrem,
                apply
        ), BorderLayout.NORTH);
    }

    @Override public void refresh() {
        cbAppointments.setSelected(c.isModuleEnabled("APPOINTMENTS"));
        cbHistory.setSelected(c.isModuleEnabled("MEDICAL_HISTORY"));
        cbPayment.setSelected(c.isModuleEnabled("PAYMENT"));
        cbReminders.setSelected(c.isModuleEnabled("REMINDERS"));

        switch (c.getModel().profile.insurance) {
            case MINIMAL -> rbMin.setSelected(true);
            case NORMAL  -> rbNorm.setSelected(true);
            case PREMIUM -> rbPrem.setSelected(true);
        }
    }

    private void applyChanges() {
        java.util.List<String> act = new java.util.ArrayList<>(), deact = new java.util.ArrayList<>();

        if (cbAppointments.isSelected()) act.add("APPOINTMENTS"); else deact.add("APPOINTMENTS");
        if (cbHistory.isSelected())      act.add("MEDICAL_HISTORY"); else deact.add("MEDICAL_HISTORY");
        if (cbPayment.isSelected())      act.add("PAYMENT"); else deact.add("PAYMENT");
        if (cbReminders.isSelected())    act.add("REMINDERS"); else deact.add("REMINDERS");

        if (rbMin.isSelected())  act.add("INSURANCE_MINIMAL");
        if (rbNorm.isSelected()) act.add("INSURANCE_NORMAL");
        if (rbPrem.isSelected()) act.add("INSURANCE_PREMIUM");

        int code = c.activate(
            deact.isEmpty()? null : deact.toArray(new String[0]),
            act.isEmpty()?   null : act.toArray(new String[0])
        );
        JOptionPane.showMessageDialog(this, code==0 ? "Updated!" : ("Error code "+code));
        go("Home / Dashboard");
    }

}
