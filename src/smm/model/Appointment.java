package smm.model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Appointment {
    public final UUID id = UUID.randomUUID();
    public LocalDate date;
    public LocalTime time;
    public String type;          // Consultation, Surgery, Follow-up
    public String service;       // Dermatology, Cardiology
    public String doctor;
    public String medicalCenter;
    public String roomType;      // Shared / Private
    public String equipment;     // CT scanner, ECG
    public boolean paid;
    public double price;

    public Appointment(LocalDate date, LocalTime time, String type, String service,
                       String doctor, String medicalCenter, String roomType,
                       String equipment, double price) {
        this.date = date; this.time = time; this.type = type; this.service = service;
        this.doctor = doctor; this.medicalCenter = medicalCenter;
        this.roomType = roomType; this.equipment = equipment; this.price = price;
    }

    public String summary() {
        return date + " " + time + " • " + type + " • " + service + " @ " + medicalCenter + " (" + doctor + ")";
    }
}
