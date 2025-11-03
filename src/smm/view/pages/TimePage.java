package smm.view.pages;

import smm.controller.Controller;
import smm.model.TimeEvent;
import smm.model.TimeEventSystem;
import smm.model.TimeObserver;
import smm.view.NavAwarePanel;
import smm.view.UI;

import javax.swing.*;
import java.awt.*;

public class TimePage extends NavAwarePanel implements TimeObserver {
    private final Controller c;
    private final TimeEventSystem tes;
    private final JTextArea log = new JTextArea(10, 60);

    public TimePage(Controller c, TimeEventSystem tes) {
        this.c = c; this.tes = tes;
        tes.addObserver(this);
        setLayout(new BorderLayout(10,10));

        log.setEditable(false);
        add(UI.h1("Time Event Simulator"), BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);

        JButton plus1d = new JButton("Advance +1 Day");
        JButton plus7d = new JButton("Advance +1 Week");
        JButton random = new JButton("Advance +1 Day (Random Event)");

        plus1d.addActionListener(e -> tes.advanceTime(1, false));
        plus7d.addActionListener(e -> tes.advanceTime(7, false));
        random.addActionListener(e -> tes.advanceTime(1, true));

        add(UI.row(plus1d, plus7d, random), BorderLayout.SOUTH);
    }

    @Override
    public void onTimeAdvanced(TimeEvent event) {
        // prepend a small header
        log.append("\nTime advanced to: " + event.newDate + "\n");

        // 1) Show any random/system events that were triggered
        for (String s : event.events) log.append(" • " + s + "\n");

        // 2) “Today” appointments on the new date
        var M = c.getModel();
        var todays = new java.util.ArrayList<>(M.appointments);
        todays.removeIf(a -> !a.date.equals(event.newDate));
        if (!todays.isEmpty()) {
            for (var a : todays) {
                log.append("   Today: " + a.time + " – " + a.service + " (" + a.doctor + ") @ " + a.medicalCenter + "\n");
            }
        } else {
            log.append("   No appointment today.\n");
        }

        // 3) Appointments that were crossed & finished during the jump
        //    They are now in history with dates between (oldDate+1) and newDate (inclusive of old<new).
        if (event.newDate.isAfter(event.oldDate)) {
            var crossed = new java.util.ArrayList<>(M.history);
            var from = event.oldDate.plusDays(1);
            var to   = event.newDate;
            crossed.removeIf(h -> h.date.isBefore(from) || h.date.isAfter(to));
            if (!crossed.isEmpty()) {
                for (var h : crossed) {
                    log.append("   Finished on " + h.date + ": " + h.details + "\n");
                }
            }
        }

        log.append("Appointments & history updated.\n");

        // write unified log3 with TES info (keeps your state_log and state_log1 unchanged)
        c.logTESAdvance3(event, java.util.List.copyOf(event.events));

        // Refresh whole UI so Lists/History reflect the change
        c.getView().refreshAll();
    }

    @Override public void refresh() {}
}
