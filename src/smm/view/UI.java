package smm.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UI {
    public static JPanel col(Component... comps) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component c : comps) { p.add(c); p.add(Box.createVerticalStrut(8)); }
        return p;
    }
    public static JPanel row(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (Component c : comps) p.add(c);
        return p;
    }
    public static JLabel h1(String s){ var l=new JLabel(s); l.setFont(l.getFont().deriveFont(Font.BOLD,20f)); return l; }
    public static JLabel h2(String s){ var l=new JLabel(s); l.setFont(l.getFont().deriveFont(Font.BOLD,16f)); return l; }
    public static JScrollPane table(Object[] cols, Object[][] data) {
        JTable t = new JTable(new DefaultTableModel(data, cols){ public boolean isCellEditable(int r,int c){return false;}});
        t.setRowHeight(22);
        return new JScrollPane(t);
    }
}
