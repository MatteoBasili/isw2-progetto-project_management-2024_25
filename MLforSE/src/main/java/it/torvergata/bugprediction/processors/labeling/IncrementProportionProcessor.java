package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;
import it.torvergata.bugprediction.utils.FileWriterUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static it.torvergata.bugprediction.controllers.DatasetsBuilder.RESULTS_DIR;

public class IncrementProportionProcessor extends ProportionProcessor {

    public void processProportion(List<Ticket> ticketList, List<Release> releaseList, String projName) throws IOException {
        List<Ticket> ticketForProportionList = new ArrayList<>(); // Elenco dei ticket già con la IV
        List<Ticket> finalTicketList = new ArrayList<>();
        float proportion = 0;

        File file = new File(RESULTS_DIR + projName.toLowerCase() + "/reportFiles");
        if (!file.exists() && !file.mkdirs()) throw new IOException();

        // Possiamo iniziare la proporzione dal primo biglietto con un IV. I precedenti non possono essere proporzionati
        LocalDate firstTicketWithIVDate = ticketList
                .stream()
                .filter(Ticket::isCorrect)
                .toList()
                .get(0).getResolutionDate();
        ticketList.removeIf(t -> t.getResolutionDate().isBefore(firstTicketWithIVDate));

        for (Ticket ticket : ticketList){
            // Se il ticket ha una lista di AV
            if (ticket.isCorrect()){
                getProportion(ticketForProportionList, ticket, false);
                ticket.setInjectedVersion(ticket.getAffectedVersions().get(0));

                // Per la proporzione, utilizziamo solo i ticket con la IV
                // già nota, non quella elaborata tramite proporzione
                ticketForProportionList.add(ticket);
            }
            // Se il ticket non ha un elenco di AV
            else {
                proportion = getProportion(ticketForProportionList, ticket, true);
                computeInjectedVersion(ticket, releaseList, proportion);
                computeAffectedVersionsList(ticket, releaseList);
            }

            finalTicketList.add(ticket);
        }

        finalTicketList.sort(Comparator.comparing(Ticket::getResolutionDate));

        file = new File(RESULTS_DIR + projName + "/reportFiles/Proportion.txt");
        try(FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.append(outputToFile.toString());
            FileWriterUtils.flushAndCloseFW(fileWriter, logger, NAME_OF_THIS_CLASS);
        }
    }

    /**
     * Aggiunge informazioni su come viene calcolata la proporzione per ogni ticket e calcola il valore della proporzione
     * @param ticketForProportionList elenco dei ticket validi per calcolare la proporzione
     * @param ticket il ticket per impostare l'IV
     * @param doActualComputation se l'IV del ticket deve essere calcolato o meno
     * @return la proporzione
     */
    private float getProportion(List<Ticket> ticketForProportionList, Ticket ticket,
                                boolean doActualComputation) {
        outputToFile.append("\n[*]PROPORTION[*]-----------------------------------------------\n")
                .append(STARTING_SEPARATOR)
                .append(ticket.getKey())
                .append(ENDING_SEPARATOR);

        // Se il calcolo non deve essere eseguito, aggiunge semplicemente una riga
        if (!doActualComputation) return 0;

        // Ordina i ticket per data
        ticketForProportionList.sort(Comparator.comparing(Ticket::getResolutionDate));

        // Calcola la proporzione
        float proportion = computeProportion(ticketForProportionList);

        // Write report
        outputToFile.append("SIZE OF FILTERED TICKET LIST: ")
                .append(ticketForProportionList.size())
                .append("\n")
                .append("PROPORTION : ")
                .append(proportion)
                .append("\n")
                .append("----------------------------------------------------------\n");
        return proportion;
    }

    /**
     * I ticket con IV vengono utilizzati per calcolare la proporzione
     * @param ticketForProportionList elenco dei ticket validi per calcolare la proporzione
     * @return la proporzione
     */
    private float computeProportion(List<Ticket> ticketForProportionList) {
        float totalProportion = 0.0F;
        float denominator;
        float propForTicket;

        for (Ticket correctTicket : ticketForProportionList) {
            propForTicket = 0.0F;

            // Se OV != FV il denominatore può essere calcolato, altrimenti la proporzione è 0
            if (!correctTicket.getOpeningVersion().getId().equals(correctTicket.getFixedVersion().getId())) {
                denominator = ((float) correctTicket.getFixedVersion().getNumericId() - (float) correctTicket.getOpeningVersion().getNumericId());
                propForTicket = ((float) correctTicket.getFixedVersion().getNumericId() - (float) correctTicket.getInjectedVersion().getNumericId())
                        / denominator;
            }

            totalProportion += propForTicket;
        }
        return totalProportion / ticketForProportionList.size();
    }

}
