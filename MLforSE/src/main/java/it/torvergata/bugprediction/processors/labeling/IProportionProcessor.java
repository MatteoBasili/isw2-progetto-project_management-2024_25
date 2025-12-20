package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.io.IOException;
import java.util.List;

public interface IProportionProcessor {

    void processProportion(List<Ticket> fixedTicketsList, List<Release> releaseList, String projName)
            throws IOException;

}
