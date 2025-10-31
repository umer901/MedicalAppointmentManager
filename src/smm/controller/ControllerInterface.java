package smm.controller;

import java.io.FileWriter;
import java.io.IOException;

public interface ControllerInterface {
    public int activate(String[] deactivations, String[] activations);
    public boolean enableUIView();
    public boolean disableUIView();
    public String[] getStateAsLog();
    public String[] getStateAsLog1();
}
