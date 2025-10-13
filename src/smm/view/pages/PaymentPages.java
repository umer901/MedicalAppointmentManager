package smm.view.pages;

import smm.controller.AppController;
import smm.model.Invoice;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;

public class PaymentPages {

    public static class BillingPage extends NavAwarePanel {
        private final AppController c; private final Runnable after;
        private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Invoice","Date","Amount (€)","Paid"}, 0);
        private final JTable table = new JTable(model);

        public BillingPage(AppController c, Runnable after) {
            this.c = c; this.after = after;
            setLayout(new BorderLayout());
            table.setRowHeight(22);

            JButton pay = new JButton("Pay Selected");
            pay.addActionListener(e -> {
                int i = table.getSelectedRow();
                if (i >= 0) {
                    UUID id = (UUID) model.getValueAt(i, 0);
                    c.payInvoice(id);
                    JOptionPane.showMessageDialog(this, "Paid.");
                    after.run();
                }
            });

            add(UI.h1("Billing & Payments"), BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(UI.row(pay, btn("Payment • Pricing Details"), btn("Payment • Credit Balance")), BorderLayout.SOUTH);
        }

        private JButton btn(String t){ JButton b=new JButton(t); b.addActionListener(e->go(t)); return b; }

        @Override public void refresh() {
            model.setRowCount(0);
            for (Invoice inv : c.getModel().invoices) {
                model.addRow(new Object[]{inv.id, inv.issuedOn, inv.amount, inv.paid ? "Yes" : "No"});
            }
        }
    }

    public static class PricingPage extends NavAwarePanel {
        private final AppController c;
        private final JTextArea area = new JTextArea(10, 60);

        public PricingPage(AppController c) {
            this.c = c;
            setLayout(new BorderLayout());
            area.setEditable(false);
            add(UI.h1("Pricing Details"), BorderLayout.NORTH);
            add(new JScrollPane(area), BorderLayout.CENTER);
        }

        @Override public void refresh() {
            var m = c.getModel();
            String policy = m.currentPolicy();
            StringBuilder sb = new StringBuilder("Policy: ").append(policy).append("\n\n");
            for (var a : m.appointments) {
                sb.append(a.summary()).append("\n")
                  .append("  Base: ").append(a.price).append("€  → After insurance: ")
                  .append(m.priceAfterInsurance(a.price)).append("€\n");
            }
            sb.append("\nDeferred allowed: ").append(m.canDeferredPayment() ? "Yes" : "No");
            area.setText(sb.toString());
        }
    }

    public static class CreditPage extends NavAwarePanel {
        private final AppController c;
        private final JLabel bal = new JLabel();

        public CreditPage(AppController c) {
            this.c = c;
            setLayout(new BorderLayout());
            JButton add = new JButton("Add 10€ (demo)");
            add.addActionListener(e -> { c.getModel().creditBalance += 10; refresh(); });

            add(UI.col(UI.h1("Credit Balance"),
                    UI.row(new JLabel("Balance: "), bal),
                    add
            ), BorderLayout.NORTH);
        }

        @Override public void refresh() {
            bal.setText(String.format("%.2f €", c.getModel().creditBalance));
        }
    }
}
