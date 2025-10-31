package smm.view.pages;

import smm.controller.Controller;
import smm.model.InsuranceLevel;
import smm.view.AppFrame;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class FeaturesPage extends NavAwarePanel {
    private final Controller c;
    private final AppFrame frame;

    // Module checkboxes
    private final JCheckBox cbAppt  = new JCheckBox("Appointments (includes Calendar)");
    private final JCheckBox cbHist  = new JCheckBox("Medical History");
    private final JCheckBox cbPay   = new JCheckBox("Payment");
    private final JCheckBox cbRem   = new JCheckBox("Reminders");

    // Insurance radios
    private final ButtonGroup grpIns = new ButtonGroup();
    private final JRadioButton rbMin = new JRadioButton("Minimal");
    private final JRadioButton rbNor = new JRadioButton("Normal");
    private final JRadioButton rbPre = new JRadioButton("Premium");

    public FeaturesPage(Controller controller, AppFrame frame) {
        this.c = controller;
        this.frame = frame;

        setLayout(new BorderLayout(10,10));

        // Insurance group
        grpIns.add(rbMin); grpIns.add(rbNor); grpIns.add(rbPre);

        // Apply instantly on click (no separate Save needed)
        var applyBtn = new JButton("Apply Now");
        applyBtn.addActionListener(e -> apply());

        add(UI.col(
                UI.h1("Runtime Feature Toggles"),
                UI.h2("Modules"),
                UI.col(cbAppt, cbHist, cbPay, cbRem),
                UI.h2("Insurance Level"),
                UI.row(rbMin, rbNor, rbPre),
                applyBtn
        ), BorderLayout.NORTH);
    }

    private void apply() {
        // Snapshot BEFORE
        Set<String> before = new HashSet<>(c.getEnabledModules());

        // AFTER from checkboxes
        Set<String> after = new HashSet<>();
        if (cbAppt.isSelected()) after.add("APPOINTMENTS");
        if (cbHist.isSelected()) after.add("MEDICAL_HISTORY");
        if (cbPay.isSelected())  after.add("PAYMENT");
        if (cbRem.isSelected())  after.add("REMINDERS");

        // Diff → activations/deactivations
        List<String> activations   = new ArrayList<>();
        List<String> deactivations = new ArrayList<>();

        for (String m : after)  if (!before.contains(m)) deactivations.remove(m); // no-op; keep symmetric
        for (String m : after)  if (!before.contains(m)) activations.add(m);
        for (String m : before) if (!after.contains(m))  deactivations.add(m);

        // Insurance (push selected as activation; controller handles exclusivity)
        InsuranceLevel wanted = InsuranceLevel.NORMAL;
        if (rbMin.isSelected()) wanted = InsuranceLevel.MINIMAL;
        else if (rbPre.isSelected()) wanted = InsuranceLevel.PREMIUM;

        switch (wanted) {
            case MINIMAL -> activations.add("INSURANCE_MINIMAL");
            case NORMAL  -> activations.add("INSURANCE_NORMAL");
            case PREMIUM -> activations.add("INSURANCE_PREMIUM");
        }

        // Apply to controller
        c.activate(
                deactivations.isEmpty() ? null : deactivations.toArray(String[]::new),
                activations.isEmpty()   ? null : activations.toArray(String[]::new)
        );

        // Rebuild nav instantly & refresh pages
        frame.rebuildNavigationByFeatures();
        frame.refreshAll();

        // Keep the user here
        frame.navigateTo("Settings • Features");
    }

    @Override
    public void refresh() {
        // Sync UI from controller state
        var on = c.getEnabledModules();
        cbAppt.setSelected(on.contains("APPOINTMENTS"));
        cbHist.setSelected(on.contains("MEDICAL_HISTORY"));
        cbPay.setSelected(on.contains("PAYMENT"));
        cbRem.setSelected(on.contains("REMINDERS"));

        switch (c.getModel().profile.insurance) {
            case MINIMAL -> rbMin.setSelected(true);
            case NORMAL  -> rbNor.setSelected(true);
            case PREMIUM -> rbPre.setSelected(true);
        }
    }
}
