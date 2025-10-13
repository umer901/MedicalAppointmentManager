package smm.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reminder {
    public final UUID id = UUID.randomUUID();
    public String type;    // appointment / medication
    public String text;
    public LocalDateTime when;
    public boolean enabled = true;

    public Reminder(String type, String text, LocalDateTime when) {
        this.type = type; this.text = text; this.when = when;
    }
}
