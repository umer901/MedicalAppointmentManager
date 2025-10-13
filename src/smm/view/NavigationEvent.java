package smm.view;

import java.awt.*;

public class NavigationEvent extends AWTEvent {
    public static final int NAV_ID = AWTEvent.RESERVED_ID_MAX + 4321;
    public final String target;
    public NavigationEvent(Component src, String target) { super(src, NAV_ID); this.target = target; }
}
