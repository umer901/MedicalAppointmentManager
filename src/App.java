import smm.controller.Controller;
import smm.model.AppModel;
import smm.model.TimeEventSystem;
import smm.view.AppFrame;
import smm.view.NavigationEvent;
import smm.view.pages.TimePage;

import java.awt.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Model + Controller
        var model = new AppModel();
        var controller = new Controller(model);

        // Time Event System (Observer pattern)
        var tes = new TimeEventSystem();
        tes.addObserver(model); // model adapts when time advances

        // Show UI
        controller.enableUIView();
        AppFrame frame = controller.getView();

        // Register TES page ONCE here (not inside the event listener)
        frame.addExternalPage("Time Event System", new TimePage(controller, tes));

        // Navigation event bus
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof NavigationEvent ne) {
                String target = ne.target;
                frame.refreshAll();
                frame.cards.show(frame.content, target);
                frame.nav.setSelectedValue(target, true);
            }
        }, NavigationEvent.NAV_ID);

        // Initial refresh
        frame.refreshAll();
    }
}
