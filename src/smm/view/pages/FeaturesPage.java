package smm.view.pages;

import smm.controller.Controller;
import smm.model.InsuranceLevel;
import smm.view.AppFrame;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime feature toggles page.
 * Modules: Appointments, Medical History, Payment, Reminders
 * Reminders sub-features (real features): APPOINTMENT_REMINDER, MEDICATION_REMINDER
 * Insurance: Minimal / Normal / Premium  (mutually exclusive)
 * Pricing: Out of Pocket / Price Reduction / Deferred Payment  (mutually exclusive)
 *
 * Sends (de)activations to Controller.activate(..).
 */
public class FeaturesPage extends NavAwarePanel {
    private final Controller c;
    private final AppFrame frame;

    // Modules
    private final JCheckBox cbAppt  = new JCheckBox("Appointments (includes Calendar)");
    private final JCheckBox cbHist  = new JCheckBox("Medical History");
    private final JCheckBox cbPay   = new JCheckBox("Payment");
    private final JCheckBox cbRem   = new JCheckBox("Reminders");

    // Reminders children (independent features)
    private final JCheckBox cbRemAppt = new JCheckBox("Appointment Reminder");
    private final JCheckBox cbRemMed  = new JCheckBox("Medication Reminder");

    // Insurance radios
    private final ButtonGroup grpIns = new ButtonGroup();
    private final JRadioButton rbMin = new JRadioButton("Minimal");
    private final JRadioButton rbNor = new JRadioButton("Normal");
    private final JRadioButton rbPre = new JRadioButton("Premium");

    // Pricing radios
    private final ButtonGroup grpPricing = new ButtonGroup();
    private final JRadioButton rbOut  = new JRadioButton("Out of Pocket");
    private final JRadioButton rbDisc = new JRadioButton("Price Reduction");
    private final JRadioButton rbDef  = new JRadioButton("Deferred Payment");

    public FeaturesPage(Controller controller, AppFrame frame) {
        this.c = controller;
        this.frame = frame;

        setLayout(new BorderLayout(10, 10));

        // Group radios
        grpIns.add(rbMin); grpIns.add(rbNor); grpIns.add(rbPre);
        grpPricing.add(rbOut); grpPricing.add(rbDisc); grpPricing.add(rbDef);

        // Parent toggles both children; children derive parent on change
        cbRem.addActionListener(e -> {
            boolean v = cbRem.isSelected();
            cbRemAppt.setSelected(v);
            cbRemMed.setSelected(v);
        });
        cbRemAppt.addActionListener(e -> reflectParentFromChildren());
        cbRemMed.addActionListener(e -> reflectParentFromChildren());

        JButton applyBtn = new JButton("Apply Now");
        applyBtn.addActionListener(e -> apply());

        add(UI.col(
                UI.h1("Runtime Feature Toggles"),

                UI.h2("Modules"),
                UI.col(cbAppt, cbHist, cbPay, cbRem),
                UI.col( // visually indented children
                        UI.row(Box.createHorizontalStrut(16), cbRemAppt),
                        UI.row(Box.createHorizontalStrut(16), cbRemMed)
                ),

                UI.h2("Insurance Level"),
                UI.row(rbMin, rbNor, rbPre),

                UI.h2("Pricing"),
                UI.row(rbOut, rbDisc, rbDef),

                applyBtn
        ), BorderLayout.NORTH);
    }

    /** Parent checkbox becomes (child1 || child2). */
    private void reflectParentFromChildren() {
        cbRem.setSelected(cbRemAppt.isSelected() || cbRemMed.isSelected());
    }

    /** Build (de)activations and push to controller. */
    private void apply() {
        // Ensure parent mirrors children before diffing
        reflectParentFromChildren();

        // BEFORE
        Set<String> before = new HashSet<>(c.getEnabledModules());

        // AFTER from checkboxes
        Set<String> after = new HashSet<>();
        if (cbAppt.isSelected()) after.add("APPOINTMENTS");
        if (cbHist.isSelected()) after.add("MEDICAL_HISTORY");
        if (cbPay.isSelected())  after.add("PAYMENT");

        // Reminders: children are real features; parent is ON if any child ON
        if (cbRemAppt.isSelected()) after.add("APPOINTMENT_REMINDER");
        if (cbRemMed.isSelected())  after.add("MEDICATION_REMINDER");
        if (cbRemAppt.isSelected() || cbRemMed.isSelected()) after.add("REMINDERS");

        // Diff
        java.util.List<String> activations = new java.util.ArrayList<>();
        java.util.List<String> deactivations = new java.util.ArrayList<>();
        for (String f : after)  if (!before.contains(f)) activations.add(f);
        for (String f : before) if (!after.contains(f))  deactivations.add(f);

        // Insurance
        InsuranceLevel wanted = InsuranceLevel.NORMAL;
        if (rbMin.isSelected()) wanted = InsuranceLevel.MINIMAL;
        else if (rbPre.isSelected()) wanted = InsuranceLevel.PREMIUM;
        switch (wanted) {
            case MINIMAL -> activations.add("INSURANCE_MINIMAL");
            case NORMAL  -> activations.add("INSURANCE_NORMAL");
            case PREMIUM -> activations.add("INSURANCE_PREMIUM");
        }

        // Pricing
        if (rbOut.isSelected())  activations.add("OUT_OF_POCKET");
        if (rbDisc.isSelected()) activations.add("PRICE_REDUCTION");
        if (rbDef.isSelected())  activations.add("DEFERRED_PAYMENT");

        // Apply
        c.activate(
                deactivations.isEmpty() ? null : deactivations.toArray(String[]::new),
                activations.isEmpty()   ? null : activations.toArray(String[]::new)
        );

        frame.rebuildNavigationByFeatures();
        frame.refreshAll();
        frame.navigateTo("Settings • Features");
    }

    /** Sync UI from controller state. */
    @Override
    public void refresh() {
        var on = c.getEnabledModules();
        cbAppt.setSelected(on.contains("APPOINTMENTS"));
        cbHist.setSelected(on.contains("MEDICAL_HISTORY"));
        cbPay.setSelected(on.contains("PAYMENT"));

        // Children reflect individual features
        cbRemAppt.setSelected(on.contains("APPOINTMENT_REMINDER"));
        cbRemMed.setSelected(on.contains("MEDICATION_REMINDER"));
        // Parent mirrors children
        reflectParentFromChildren();

        // Insurance
        switch (c.getModel().profile.insurance) {
            case MINIMAL -> rbMin.setSelected(true);
            case NORMAL  -> rbNor.setSelected(true);
            case PREMIUM -> rbPre.setSelected(true);
        }

        // Pricing
        switch (c.getModel().pricing) {
            case OUT_OF_POCKET   -> rbOut.setSelected(true);
            case PRICE_REDUCTION -> rbDisc.setSelected(true);
            case DEFERRED_PAYMENT-> rbDef.setSelected(true);
        }
    }
}
