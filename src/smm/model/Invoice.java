package smm.model;
import java.time.LocalDate;
import java.util.UUID;

public class Invoice {
    public final UUID id = UUID.randomUUID();
    public LocalDate issuedOn;
    public double amount;
    public boolean paid;
    public UUID appointmentId; // optional link

    public Invoice(LocalDate issuedOn, double amount, boolean paid, UUID appointmentId) {
        this.issuedOn = issuedOn; this.amount = amount; this.paid = paid; this.appointmentId = appointmentId;
    }
}
