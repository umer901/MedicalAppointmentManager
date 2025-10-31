package smm.view.pages;

import smm.controller.Controller;
import smm.model.InsuranceLevel;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature Settings page:
 * - For every module, radio: Enable / Disable
 * - Insurance selection with 3 radios
 * - Save button applies changes via controller.activate(...)
 */
public class FeatureSettingsPage extends NavAwarePanel {

    private final Controller c;

    // Appointments radios
    private final JRadioButton apptOn  = new JRadioButton("Enable");
    private final JRadioButton apptOff = new JRadioButton("Disable");

    // History radios
    private final JRadioButton histOn  = new JRadioButton("Enable");
    private final JRadioButton histOff = new JRadioButton("Disable");

    // Payment radios
    private final JRadioButton payOn   = new JRadioButton("Enable");
    private final JRadioButton payOff  = new JRadioButton("Disable");

    // Reminders radios
    private final JRadioButton remOn   = new JRadioButton("Enable");
    private final JRadioButton remOff  = new JRadioButton("Disable");

    // Insurance radios
    private final JRadioButton insMin  = new JRadioButton("Minimal");
    private final JRadioButton insNorm = new JRadioButton("Normal");
    private final JRadioButton insPrem = new JRadioButton("Premium");

    public FeatureSettingsPage(Controller controller) {
        this.c = controller;
        setLayout(new BorderLayout(12,12));

        // Group per-module radios
        var gAppt = new ButtonGroup(); gAppt.add(apptOn); gAppt.add(apptOff);
        var gHist = new ButtonGroup(); gHist.add(histOn); gHist.add(histOff);
        var gPay  = new ButtonGroup(); gPay.add(payOn);  gPay.add(payOff);
        var gRem  = new ButtonGroup(); gRem.add(remOn);  gRem.add(remOff);

        // Insurance single-choice group
        var gIns = new ButtonGroup(); gIns.add(insMin); gIns.add(insNorm); gIns.add(insPrem);

        // Layout
        add(UI.h1("Feature Settings"), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(section("Appointments", apptOn, apptOff));
        center.add(section("Medical History", histOn, histOff));
        center.add(section("Payment", payOn, payOff));
        center.add(section("Reminders", remOn, remOff));
        center.add(insuranceSection());
        add(center, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        save.addActionListener(e -> applyAndNotify());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(save);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel section(String title, JRadioButton on, JRadioButton off) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setBorder(new TitledBorder(title));
        p.add(on); p.add(off);
        return p;
    }

    private JPanel insuranceSection() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setBorder(new TitledBorder("Insurance"));
        p.add(new JLabel("Plan:"));
        p.add(insMin); p.add(insNorm); p.add(insPrem);
        return p;
    }

    private void applyAndNotify() {
        List<String> act = new ArrayList<>();
        List<String> deact = new ArrayList<>();

        // modules
        if (apptOn.isSelected()) act.add("APPOINTMENTS"); else deact.add("APPOINTMENTS");
        if (histOn.isSelected()) act.add("MEDICAL_HISTORY"); else deact.add("MEDICAL_HISTORY");
        if (payOn.isSelected())  act.add("PAYMENT"); else deact.add("PAYMENT");
        if (remOn.isSelected())  act.add("REMINDERS"); else deact.add("REMINDERS");

        // insurance – mutually exclusive; select exactly one
        if (insMin.isSelected())  act.add("INSURANCE_MINIMAL");
        if (insNorm.isSelected()) act.add("INSURANCE_NORMAL");
        if (insPrem.isSelected()) act.add("INSURANCE_PREMIUM");

        int code = c.activate(
                deact.isEmpty() ? null : deact.toArray(new String[0]),
                act.isEmpty()   ? null : act.toArray(new String[0])
        );

        JOptionPane.showMessageDialog(this,
                code == 0 ? "✅ Changes saved and applied." : "⚠️ Error applying changes (code " + code + ")");

        // Go back or just refresh everything so the nav updates
        go("Home / Dashboard");
    }

    @Override
    public void refresh() {
        // Sync radios from current state
        apptOn.setSelected(c.isModuleEnabled("APPOINTMENTS"));
        apptOff.setSelected(!c.isModuleEnabled("APPOINTMENTS"));

        histOn.setSelected(c.isModuleEnabled("MEDICAL_HISTORY"));
        histOff.setSelected(!c.isModuleEnabled("MEDICAL_HISTORY"));

        payOn.setSelected(c.isModuleEnabled("PAYMENT"));
        payOff.setSelected(!c.isModuleEnabled("PAYMENT"));

        remOn.setSelected(c.isModuleEnabled("REMINDERS"));
        remOff.setSelected(!c.isModuleEnabled("REMINDERS"));
    }
}
