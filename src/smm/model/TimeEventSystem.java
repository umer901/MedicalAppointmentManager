package smm.model;
import java.time.LocalDate;
import java.util.*;

/**
 * Time Event System (TES)
 * Implements the Observer pattern.
 */
public class TimeEventSystem {
    private LocalDate currentDate = LocalDate.now();
    private final List<TimeObserver> observers = new ArrayList<>();
    private final Random random = new Random();

    public void addObserver(TimeObserver o) { observers.add(o); }
    public void removeObserver(TimeObserver o) { observers.remove(o); }

    public LocalDate getCurrentDate() { return currentDate; }

    public void advanceTime(int days, boolean randomEvents) {
    LocalDate old = currentDate;
    currentDate = currentDate.plusDays(days);
    List<String> triggered = new ArrayList<>();

    if (randomEvents) {
        int roll = random.nextInt(3);
        if (roll == 0) triggered.add("Doctor became unavailable");
        else if (roll == 1) triggered.add("User fell ill");
        else triggered.add("Follow-up scheduled automatically");
    }

    TimeEvent event = new TimeEvent(old, currentDate, triggered);
    for (TimeObserver o : observers) o.onTimeAdvanced(event);
    }

}
