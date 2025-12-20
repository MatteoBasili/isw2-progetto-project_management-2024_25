package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public abstract class ProportionProcessor implements  IProportionProcessor {

    protected static final String NAME_OF_THIS_CLASS = ProportionProcessor.class.getName();
    protected static final String STARTING_SEPARATOR = "----------------------\n[";
    protected static final String ENDING_SEPARATOR = "]\n";

    protected static final Logger logger = Logger.getLogger(NAME_OF_THIS_CLASS);
    protected static final StringBuilder outputToFile = new StringBuilder();

    /**
     * Imposta, dati la proporzione e l'elenco delle release, l'IV di un ticket
     * @param ticket il ticket a cui assegnare l'IV
     * @param releasesList l'elenco da cui estrarre l'IV
     * @param proportion la proporzione per la formula
     */
    protected void computeInjectedVersion(Ticket ticket, List<Release> releasesList, float proportion) {
        int injectedVersionId;

        // IV previsto = min(1; FV - (FV - OV) * P), ma se FV = OV allora sostituisci FV - OV con 1
        if(ticket.getFixedVersion().getNumericId() == ticket.getOpeningVersion().getNumericId()){
            injectedVersionId = (int) (ticket.getFixedVersion().getNumericId() - proportion);
        }
        else{
            injectedVersionId = (int) (ticket.getFixedVersion().getNumericId() -
                    ((ticket.getFixedVersion().getNumericId() - ticket.getOpeningVersion().getNumericId()) * proportion));
        }

        injectedVersionId = Math.max(1, Math.min(injectedVersionId, releasesList.get(releasesList.size() - 1).getNumericId()));

        // Assegna l'IV al ticket
        int finalInjectedVersionId = injectedVersionId;
        ticket.setInjectedVersion(releasesList.stream()
                .filter(release -> release.getNumericId() == finalInjectedVersionId)
                .toList()
                .get(0)
        );
    }

    /**
     * Imposta, dato l'elenco delle release, le AV di un ticket
     * @param ticket il ticket a cui assegnare le AV
     * @param releasesList l'elenco delle release da cui estrarre le AV
     */
    protected void computeAffectedVersionsList(Ticket ticket, List<Release> releasesList) {
        List<Release> completeAffectedVersionsList = new ArrayList<>();

        // Gli ID delle AV sono tali che: IV <= AV(i) <= OV
        for(Release release: releasesList
                .stream()
                .filter(release ->
                        release.getNumericId() >= ticket.getInjectedVersion().getNumericId()
                                && release.getNumericId() <= ticket.getOpeningVersion().getNumericId())
                .toList()){
            completeAffectedVersionsList.add(new Release(release.getId(), release.getName(), release.getDateString()));
        }

        completeAffectedVersionsList.sort(Comparator.comparing(Release::getDateTime));
        ticket.setAffectedVersions(completeAffectedVersionsList);
    }

}
