package smm.view;

import javax.swing.*;
import java.awt.*;

public abstract class NavAwarePanel extends JPanel implements AppFrame.Refreshable {
    protected void go(String key) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof AppFrame f) {
            f.navigateTo(key);
        }
    }
}
