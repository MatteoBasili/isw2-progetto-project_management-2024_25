package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IncrementProportionProcessor extends ProportionProcessor {

    public void processProportion(List<Ticket> ticketList,
                                  List<Release> releaseList,
                                  String projName) {

        List<Ticket> ticketForProportionList = new ArrayList<>();
        List<Ticket> finalTicketList = new ArrayList<>();

        LocalDate firstTicketWithIVDate = ticketList.stream()
                .filter(Ticket::hasAffectedVersions)
                .findFirst()
                .orElseThrow()
                .getResolutionDate();

        ticketList.removeIf(t -> t.getResolutionDate().isBefore(firstTicketWithIVDate));

        for (Ticket ticket : ticketList) {

            if (ticket.hasAffectedVersions()) {
                ticket.setInjectedVersion(ticket.getAffectedVersions().get(0));
                ticketForProportionList.add(ticket);
            } else {
                // Ordina i ticket per data
                ticketForProportionList.sort(Comparator.comparing(Ticket::getResolutionDate));

                float proportion = computeProportion(ticketForProportionList);
                computeInjectedVersion(ticket, releaseList, proportion);
                computeAffectedVersionsList(ticket, releaseList);
            }

            finalTicketList.add(ticket);
        }

        finalTicketList.sort(Comparator.comparing(Ticket::getResolutionDate));
    }

    /**
     * Calcola la proporzione usando solo ticket con IV nota
     */
    private float computeProportion(List<Ticket> ticketForProportionList) {
        float totalProportion = 0.0F;

        for (Ticket t : ticketForProportionList) {
            if (!t.getOpeningVersion().getId().equals(t.getFixedVersion().getId())) {

                float denominator =
                        (float) t.getFixedVersion().getNumericId()
                                - t.getOpeningVersion().getNumericId();

                float prop =
                        (t.getFixedVersion().getNumericId()
                                - t.getInjectedVersion().getNumericId())
                                / denominator;

                totalProportion += prop;
            }
        }

        return totalProportion / ticketForProportionList.size();
    }

}
