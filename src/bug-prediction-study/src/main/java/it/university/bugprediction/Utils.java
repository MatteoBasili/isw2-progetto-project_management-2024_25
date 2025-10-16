package it.university.bugprediction;

import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;

public class Utils {

    public static CanonicalTreeParser prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        final CanonicalTreeParser treeParser = new CanonicalTreeParser();
        if (commit == null) return treeParser;
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, commit.getTree().getId());
        }
        return treeParser;
    }
}
