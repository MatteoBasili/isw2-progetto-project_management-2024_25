package it.torvergata.bugprediction.processors.labeling;

import it.torvergata.bugprediction.models.Release;
import it.torvergata.bugprediction.models.Ticket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class ProportionProcessor implements  IProportionProcessor {

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
        ticket.setInjectedVersion(
                releasesList.stream()
                        .filter(r -> r.getNumericId() == finalInjectedVersionId)
                        .findFirst()
                        .orElseThrow()
        );
    }

    /**
     * Imposta, dato l'elenco delle release, le AV di un ticket
     * @param ticket il ticket a cui assegnare le AV
     * @param releasesList l'elenco delle release da cui estrarre le AV
     */
    protected void computeAffectedVersionsList(Ticket ticket, List<Release> releasesList) {
        List<Release> affectedVersions = new ArrayList<>();

        // Gli ID delle AV sono tali che: IV <= AV(i) <= OV
        for (Release release : releasesList) {
            if (release.getNumericId() >= ticket.getInjectedVersion().getNumericId()
                    && release.getNumericId() <= ticket.getOpeningVersion().getNumericId()) {

                affectedVersions.add(
                        new Release(release.getId(), release.getName(), release.getDateString())
                );
            }
        }

        affectedVersions.sort(Comparator.comparing(Release::getDateTime));
        ticket.setAffectedVersions(affectedVersions);
    }

}
