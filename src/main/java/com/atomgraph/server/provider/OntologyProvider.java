/**
 *  Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.atomgraph.server.provider;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.Lock;
import com.atomgraph.processor.exception.SitemapException;
import java.util.List;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ontology loader.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class OntologyProvider
{
    private static final Logger log = LoggerFactory.getLogger(OntologyProvider.class);

    private final String ontologyURI;
    private final OntModelSpec ontModelSpec;
        
    public OntologyProvider(String ontologyURI, List<Rule> rules)
    {
        if (ontologyURI == null) throw new IllegalArgumentException("URI String cannot be null");
        if (rules == null) throw new IllegalArgumentException("List<Rule> cannot be null");
        
        this.ontologyURI = ontologyURI;
        this.ontModelSpec = getOntModelSpec(rules);
    }
    
    public final OntModelSpec getOntModelSpec(List<Rule> rules)
    {
        if (rules == null) throw new IllegalArgumentException("List<Rule> cannot be null");

        OntModelSpec rulesSpec = new OntModelSpec(OntModelSpec.OWL_MEM);        
        Reasoner reasoner = new GenericRuleReasoner(rules);
        //reasoner.setDerivationLogging(true);
        //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
        rulesSpec.setReasoner(reasoner);
        return rulesSpec;
    }
    
    public class ImportCycleChecker
    {
        private final Map<Ontology, Boolean> marked = new HashMap<>(), onStack = new HashMap<>();
        private Ontology cycleOntology = null;

        public void check(Ontology ontology)
        {
            marked.put(ontology, Boolean.TRUE);
            onStack.put(ontology, Boolean.TRUE);

            ExtendedIterator<OntResource> it = ontology.listImports();
            try
            {
                while (it.hasNext())
                {
                    OntResource importRes = it.next();
                    if (importRes.canAs(Ontology.class))
                    {
                        Ontology imported = importRes.asOntology();
                        if (marked.get(imported) == null)
                            check(imported);
                        else if (onStack.get(imported))
                        {
                            cycleOntology = imported;
                            return;
                        }
                    }
                }

                onStack.put(ontology, Boolean.FALSE);
            }
            finally
            {
                it.close();
            }
        }
        
        public Ontology getCycleOntology()
        {
            return cycleOntology;
        }
        
    }
        
    public String getOntologyURI()
    {
        return ontologyURI;
    }
    
    public Ontology getOntology()
    {
        Ontology ontology = getOntModel(getOntologyURI(), getOntModelSpec()).getOntology(getOntologyURI());

        if (ontology != null)
        {
            ImportCycleChecker checker = new ImportCycleChecker();
            checker.check(ontology);
            if (checker.getCycleOntology() != null)
            {
                if (log.isErrorEnabled()) log.error("Sitemap contains an ontology which forms an import cycle: {}", checker.getCycleOntology());
                throw new SitemapException("Sitemap contains an ontology which forms an import cycle: " + checker.getCycleOntology().getURI());
            }
        }
        
        return ontology;
    }

    /**
     * Loads ontology by URI.
     * 
     * @param ontologyURI ontology location
     * @param ontModelSpec ontology model specification
     * @return ontology model
     */
    public OntModel getOntModel(String ontologyURI, OntModelSpec ontModelSpec)
    {
        if (ontologyURI == null) throw new IllegalArgumentException("URI cannot be null");
        if (ontModelSpec == null) throw new IllegalArgumentException("OntModelSpec cannot be null");        
        if (log.isDebugEnabled()) log.debug("Loading sitemap ontology from URI: {}", ontologyURI);

        OntModel ontModel = OntDocumentManager.getInstance().getOntology(ontologyURI, ontModelSpec);
        
        // explicitly loading owl:imports -- workaround for Jena bug: https://issues.apache.org/jira/browse/JENA-1210
        ontModel.enterCriticalSection(Lock.WRITE);
        try
        {
            ontModel.loadImports();
        }
        finally
        {
            ontModel.leaveCriticalSection();
        }

        // lock and clone the model to avoid ConcurrentModificationExceptions
        ontModel.enterCriticalSection(Lock.READ);
        try
        {
            Model baseModel = ModelFactory.createDefaultModel().add(ontModel.getBaseModel());
            OntModel clonedModel = ModelFactory.createOntologyModel(ontModelSpec, baseModel);
            if (log.isDebugEnabled()) log.debug("Sitemap model size: {}", clonedModel.size());
            return clonedModel;
        }
        finally
        {
            ontModel.leaveCriticalSection();
        }
    }

    public OntModelSpec getOntModelSpec()
    {
        return ontModelSpec;
    }
    
}