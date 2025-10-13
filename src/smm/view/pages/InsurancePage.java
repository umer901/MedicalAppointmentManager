package smm.view.pages;

import smm.controller.AppController;
import smm.model.InsuranceLevel;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;

public class InsurancePage extends NavAwarePanel {
    private final AppController c; private final Runnable after;
    private final JLabel policy = new JLabel();
    private final ButtonGroup grp = new ButtonGroup();
    private final JRadioButton rbMin = new JRadioButton("Minimal");
    private final JRadioButton rbNorm = new JRadioButton("Normal");
    private final JRadioButton rbPrem = new JRadioButton("Premium");

    public InsurancePage(AppController c, Runnable after) {
        this.c = c; this.after = after;
        setLayout(new BorderLayout());

        grp.add(rbMin); grp.add(rbNorm); grp.add(rbPrem);
        JButton upgrade = new JButton("Upgrade Plan (demo)");
        upgrade.addActionListener(e -> { rbPrem.setSelected(true); c.setInsurance(InsuranceLevel.PREMIUM); after.run(); });

        rbMin.addActionListener(e -> { c.setInsurance(InsuranceLevel.MINIMAL); after.run(); });
        rbNorm.addActionListener(e -> { c.setInsurance(InsuranceLevel.NORMAL);  after.run(); });
        rbPrem.addActionListener(e -> { c.setInsurance(InsuranceLevel.PREMIUM); after.run(); });

        add(UI.col(UI.h1("Insurance Overview"),
                UI.row(new JLabel("Current Policy:"), policy),
                UI.h2("Select Plan"),
                UI.row(rbMin, rbNorm, rbPrem),
                upgrade
        ), BorderLayout.NORTH);
    }

    @Override public void refresh() {
        policy.setText(c.getModel().currentPolicy());
        switch (c.getModel().profile.insurance) {
            case MINIMAL -> rbMin.setSelected(true);
            case NORMAL  -> rbNorm.setSelected(true);
            case PREMIUM -> rbPrem.setSelected(true);
        }
    }
}
