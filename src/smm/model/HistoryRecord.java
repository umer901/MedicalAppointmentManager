package smm.model;
import java.time.LocalDate;

public class HistoryRecord {
    public LocalDate date;
    public String kind;    // Consultation , Surgery , Prescription
    public String details;

    public HistoryRecord(LocalDate date, String kind, String details) {
        this.date = date; this.kind = kind; this.details = details;
    }
}
