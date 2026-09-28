package aplication.nexusMarket.domain.valueobjects;

import aplication.nexusMarket.domain.exceptions.InvalidReportException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Inclusive date range of an administrative report (OBJ-12). Not a catalog. */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public ReportPeriod {
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidReportException("A report period needs both dates, with 'from' not after 'to'.");
        }
    }

    public boolean contains(LocalDateTime moment) {
        return moment != null && !moment.toLocalDate().isBefore(from) && !moment.toLocalDate().isAfter(to);
    }
}
