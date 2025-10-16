
import smm.controller.AppController;
import smm.model.AppModel;
import smm.view.AppFrame;
import smm.view.NavigationEvent;

import java.awt.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        var model = new AppModel();
        var controller = new AppController(model);

        // Let the controller bring up the UI (implements ControllerInterface)
        controller.enableUIView();
        AppFrame frame = controller.getView();

        // Event bus for navigation
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof NavigationEvent ne) {
                String target = ne.target;
                frame.refreshAll();
                frame.cards.show(frame.content, target);
                frame.nav.setSelectedValue(target, true);
            }
        }, NavigationEvent.NAV_ID);

        frame.refreshAll();

        // Example: controller.activate(...) works now
        // controller.activate(null, new String[]{"INSURANCE_PREMIUM"});
    }
}
