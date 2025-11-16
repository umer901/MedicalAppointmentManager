package smm.model;
import java.time.LocalDate;
import java.util.List;

public class TimeEvent {
    public final LocalDate oldDate;
    public final LocalDate newDate;
    public final List<String> events;

    public TimeEvent(LocalDate oldDate, LocalDate newDate, List<String> events) {
        this.oldDate = oldDate;
        this.newDate = newDate;
        this.events  = events;
    }
}
