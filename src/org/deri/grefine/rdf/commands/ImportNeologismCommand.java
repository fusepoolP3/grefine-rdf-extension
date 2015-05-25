package org.deri.grefine.rdf.commands;

import com.google.refine.Jsonizable;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyImporter;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Giuliano Mega (mega@spaziodati.eu)
 */
public class ImportNeologismCommand extends RdfCommand {

    final static Logger logger = LoggerFactory.getLogger(ImportNeologismCommand.class);


    private static final String REPOSITORY_URI = "uri";

    public ImportNeologismCommand(ApplicationContext ctxt) {
        super(ctxt);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Model index = ModelFactory.createDefaultModel();
        index.read(request.getParameter(REPOSITORY_URI).trim());
        String projectId = request.getParameter("project");

        try {
            StmtIterator it = index.listStatements(new SimpleSelector(null, RDF.type, OWL.Ontology));
            JSONWriter writer = new JSONWriter(response.getWriter());

            ArrayList<Record> success = new ArrayList<Record>();
            ArrayList<Record> fail = new ArrayList<Record>();

            while (it.hasNext()) {
                Record vocabulary = new Record(it.next().getSubject());
                try {
                    importVocabulary(vocabulary, projectId);
                    success.add(vocabulary);
                } catch (Exception ex) {
                    logger.error("Failed to import " + vocabulary + ".", ex);
                    fail.add(vocabulary);
                }
            }

            respondOK(response, success, fail);
        } catch (Exception ex) {
            respondException(response, ex);
        }
    }

    private void respondOK(HttpServletResponse response, final List<Record> success,
                           final List<Record> failures) throws IOException, JSONException {
        respondJSON(response, new Jsonizable() {
            @Override
            public void write(JSONWriter writer, Properties options)
                    throws JSONException {
                writer.object()
                        .key("code").value("ok")
                        .key("vocabs");
                writeVocabularies(writer, success);

                writer.key("message").value(stats(success, failures));

                writer.endObject();
            }
        });
    }

    private String stats(List<Record> success, List<Record> failure) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Imported ");
        buffer.append(success.size());
        buffer.append(". Failures: ");
        buffer.append(failure.size());
        return buffer.toString();
    }

    private void importVocabulary(Record record, String projectId) throws Exception {
        getRdfContext().getVocabularySearcher().importAndIndexVocabulary(
                record.prefix,
                record.uri,
                record.uri,
                projectId,
                new VocabularyImporter()
        );
    }

    private void writeVocabularies(JSONWriter writer, List<Record> records) throws JSONException {
        writer.array();
        for (Record record : records) {
            writer.object();
            writer.key("prefix").value(record.prefix);
            writer.key("name").value(record.name);
            writer.key("uri").value(record.uri);
            writer.endObject();
        }
        writer.endArray();
    }

    class Record {
        public String name;
        public String uri;
        public String prefix;

        public Record(Resource statement) {
            prefix = statement.getProperty(uri("vann", "preferredNamespacePrefix")).getString();
            name = statement.getProperty(uri("dcterms", "title")).getString();
            uri = statement.getURI();
        }

        Property uri(String prefix, String prop) {
            Vocabulary vocab = getRdfContext().getPredefinedVocabularyManager().getPredefinedVocabulariesMap().get(prefix);
            if (vocab == null) {
                throw new IllegalStateException("Missing vocabulary for prefix " + prefix + ".");
            }
            return new PropertyImpl(vocab.getUri(), prop);
        }

        public String toString() {
            return uri;
        }
    }
}


