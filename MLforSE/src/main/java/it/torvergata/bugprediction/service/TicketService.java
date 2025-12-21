package it.torvergata.bugprediction.service;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.processors.labeling.IProportionProcessor;
import it.torvergata.bugprediction.processors.labeling.IncrementProportionProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TicketService {

    public static List<Ticket> getAllTicketsProportioned(List<Release> jiraReleases, List<Ticket> ticketList,
                                                   String projName) throws IOException {
        List<Ticket> allTickets = new ArrayList<>();
        for (Ticket t: ticketList) {
            Ticket newTicket = t.cloneAtRelease(jiraReleases.get(jiraReleases.size() - 1));
            allTickets.add(newTicket);
        }
        proportionTickets(allTickets, jiraReleases, projName);

        return allTickets;
    }

    public static void proportionTickets(List<Ticket> ticketsList, List<Release> releaseList, String projName) throws IOException {
        // E' implementata soltanto la variante "Increment"
        IProportionProcessor proportionProcessor = new IncrementProportionProcessor();
        proportionProcessor.processProportion(ticketsList, releaseList, projName);
    }

    /**
     * L'elenco dei ticket da considerare in una determinata release è composto dalla visualizzazione dei ticket
     * che erano disponibili in quella release
     * @param ticketList tutti i ticket disponibili
     * @param currentRelease la release da considerare come punto di vista dei ticket
     * @return ticket con OV <= currentRelease
     */
    public static List<Ticket> getConsideringTickets(List<Ticket> ticketList, Release currentRelease) {
        List<Ticket> consideringTicketList = ticketList
                .stream()
                .filter(t -> t.getOpeningVersion().getNumericId() <= currentRelease.getNumericId())
                .toList();

        List<Ticket> returningTicketList = new ArrayList<>();

        for (Ticket ticket: consideringTicketList) {
            Ticket newTicket = ticket.cloneAtRelease(currentRelease);
            if (newTicket == null) continue;
            returningTicketList.add(newTicket);
        }

        return returningTicketList;
    }

}
