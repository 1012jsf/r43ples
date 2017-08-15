package de.tud.plt.r43ples.objects;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.management.Config;
import de.tud.plt.r43ples.management.RevisionGraph;
import de.tud.plt.r43ples.triplestoreInterface.TripleStoreInterfaceSingleton;
import org.apache.log4j.Logger;

/**
 * Provides information of an already existent revision.
 *
 * @author Stephan Hensel
 */
public class Revision {

    /** The logger. **/
    private Logger logger = Logger.getLogger(Revision.class);

    /** The revision identifier. */
    private String revisionIdentifier;
    /** The revision URI. */
    private String revisionURI;
    /** The ADD set URI. */
    private String addSetURI;
    /** The DELETE set URI. */
    private String deleteSetURI;

    /** The revision graph URI. */
    private String revisionGraphURI;
    /** The corresponding revision graph. */
    private RevisionGraph revisionGraph;


    /**
     * The constructor.
     *
     * @param revisionGraph the revision graph
     * @param revisionInformation the revision information (identifier or URI of the revision)
     * @param isIdentifier identifies if the identifier or the URI of the revision is specified (identifier => true; URI => false)
     * @throws InternalErrorException
     */
    public Revision(RevisionGraph revisionGraph, String revisionInformation, boolean isIdentifier) throws InternalErrorException {
        this.revisionGraph = revisionGraph;
        this.revisionGraphURI = this.revisionGraph.getRevisionGraphUri();

        if (isIdentifier) {
            this.revisionIdentifier = revisionInformation;
            this.revisionURI = calculateRevisionURI(this.revisionIdentifier);
        } else {
            this.revisionURI = revisionInformation;
            this.revisionIdentifier = calculateRevisionIdentifier(this.revisionURI);
        }

        calculateAdditionalInformation();
    }

    /**
     * Get the was derived from revision of the current revision.
     *
     * @return the derived from revision
     * @throws InternalErrorException
     */
    public Revision getDerivedFromRevision() throws InternalErrorException {
        //TODO merged revisions will have two derived from revisions
        logger.info("Get derived from revision of revision " + revisionIdentifier + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?rev "
                + "WHERE { GRAPH  <%s> {"
                + "	<%s> prov:wasDerivedFrom ?rev. "
                + "	?rev a rmo:Revision. "
                + "} }", revisionGraphURI, revisionURI);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            return new Revision(revisionGraph, qs.getResource("?rev").toString(), false);
        } else {
            throw new InternalErrorException("No derived from revision found for revision " + revisionIdentifier + ".");
        }
    }

    /**
     * Get the corresponding commit of the current revision. This commit created this revision.
     *
     * @return the corresponding commit
     * @throws InternalErrorException
     */
    public Commit getCorrespondingCommit() throws InternalErrorException {
        logger.info("Get corresponding commit of revision " + revisionIdentifier + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?com "
                + "WHERE { GRAPH  <%s> {"
                + "	?com a rmo:Commit; "
                + "	 prov:generated <%s>. "
                + "} }", revisionGraphURI, revisionURI);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            return new Commit(revisionGraph, qs.getResource("?com").toString());
        } else {
            throw new InternalErrorException("No corresponding commit found for revision " + revisionIdentifier + ".");
        }
    }

    /**
     * Get the associated branch of the current revision.
     *
     * @return the associated branch
     * @throws InternalErrorException
     */
    public Branch getAssociatedBranch() throws InternalErrorException {
        logger.info("Get associated branch of revision " + revisionIdentifier + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?branch "
                + "WHERE { GRAPH  <%s> {"
                + "	<%s> rmo:belongsTo ?branch. "
                + "	?branch a rmo:Branch. "
                + "} }", revisionGraphURI, revisionURI);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            return new Branch(revisionGraph, qs.getResource("?branch").toString(), false);
        } else {
            throw new InternalErrorException("No derived from revision found for revision " + revisionIdentifier + ".");
        }
    }

    /**
     * Get the revision identifier.
     *
     * @return the revision identifier
     */
    public String getRevisionIdentifier() {
        return revisionIdentifier;
    }

    /**
     * Get the revision URI.
     *
     * @return the revision URI
     */
    public String getRevisionURI() {
        return revisionURI;
    }

    /**
     * Get the ADD set URI.
     *
     * @return the ADD set URI
     */
    public String getAddSetURI() {
        return addSetURI;
    }

    /**
     * Get the DELETE set URI.
     *
     * @return the DELETE set URI
     */
    public String getDeleteSetURI() {
        return deleteSetURI;
    }

    /**
     * Get the corresponding revision graph.
     *
     * @return the corresponding revision graph
     */
    public RevisionGraph getRevisionGraph() {
        return revisionGraph;
    }

    /**
     * Calculate additional information of the current revision and store this information to local variables.
     *
     * @throws InternalErrorException
     */
    private void calculateAdditionalInformation() throws InternalErrorException {
        logger.info("Get additional information of current revision " + revisionIdentifier + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?addSetURI ?deleteSetURI "
                + "WHERE { GRAPH  <%s> {"
                + "	<%s> a rmo:Revision; "
                + "	 rmo:addSet ?addSetURI; "
                + "  rmo:deleteSet ?deleteSetURI. "
                + "} }", revisionGraphURI, revisionURI);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            addSetURI = qs.getResource("?addSetURI").toString();
            deleteSetURI = qs.getResource("?deleteSetURI").toString();
        } else {
            //TODO Check if add and delete sets are optional for first revision
//            throw new InternalErrorException("No additional information found for revision " + revisionIdentifier + ".");
        }
    }

    /**
     * Calculate the revision URI for a given revision identifier
     *
     * @param revisionIdentifier the revision identifier
     * @return URI of identified revision
     * @throws InternalErrorException
     */
    private String calculateRevisionURI(String revisionIdentifier) throws InternalErrorException {
        logger.info("Calculate the revision URI for current revision " + revisionIdentifier + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?uri "
                + "WHERE { GRAPH  <%s> {"
                + "	?uri a rmo:Revision; "
                + "	 rmo:revisionNumber \"%s\". "
                + "} }", revisionGraphURI, revisionIdentifier);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            return qs.getResource("?uri").toString();
        } else {
            throw new InternalErrorException("No revision URI found for revision " + revisionIdentifier + ".");
        }
    }

    /**
     * Calculate the revision identifier for a given revision URI
     *
     * @param revisionURI the revision URI
     * @return the revision identifier
     * @throws InternalErrorException
     */
    private String calculateRevisionIdentifier(String revisionURI) throws InternalErrorException {
        logger.info("Calculate the revision identifier for current revision URI " + revisionURI + ".");
        String query = Config.prefixes + String.format(""
                + "SELECT ?id "
                + "WHERE { GRAPH  <%s> {"
                + "	<%s> a rmo:Revision; "
                + "	 rmo:revisionNumber ?id. "
                + "} }", revisionGraphURI, revisionURI);
        this.logger.debug(query);
        ResultSet resultSet = TripleStoreInterfaceSingleton.get().executeSelectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution qs = resultSet.next();
            return qs.getLiteral("?id").toString();
        } else {
            throw new InternalErrorException("No revision identifier found for revision URI " + revisionURI + ".");
        }
    }

}