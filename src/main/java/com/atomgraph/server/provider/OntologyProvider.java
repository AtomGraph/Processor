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
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.iterator.ExtendedIterator;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.Lock;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.vocabulary.AP;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application ontology provider.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class OntologyProvider extends PerRequestTypeInjectableProvider<Context, Ontology> implements ContextResolver<Ontology>
{
    private static final Logger log = LoggerFactory.getLogger(OntologyProvider.class);

    @Context Providers providers;
    
    private final OntModelSpec ontModelSpec;
    
    public OntologyProvider(@Context ServletConfig servletConfig)
    {
        super(Ontology.class);
            
        Object rulesParam = servletConfig.getInitParameter(AP.sitemapRules.getURI());
        if (rulesParam == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap Rules (" + AP.sitemapRules.getURI() + ") not configured");
            throw new ConfigurationException(AP.sitemapRules);
        }
        List<Rule> rules = Rule.parseRules(rulesParam.toString());
        this.ontModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        Reasoner reasoner = new GenericRuleReasoner(rules);
        //reasoner.setDerivationLogging(true);
        //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
        this.ontModelSpec.setReasoner(reasoner);
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
    
    @Override
    public Injectable<Ontology> getInjectable(ComponentContext cc, Context context)
    {
	//if (log.isDebugEnabled()) log.debug("OntologyProvider UriInfo: {} ResourceConfig.getProperties(): {}", uriInfo, resourceConfig.getProperties());
	
	return new Injectable<Ontology>()
	{
	    @Override
	    public Ontology getValue()
	    {
                return getOntology();
	    }
	};
    }

    @Override
    public Ontology getContext(Class<?> type)
    {
        return getOntology();
    }

    public com.atomgraph.processor.model.Application getApplication()
    {
	return getProviders().getContextResolver(com.atomgraph.processor.model.Application.class, null).getContext(com.atomgraph.processor.model.Application.class);
    }

    public Ontology getOntology()
    {
        return getOntology(getApplication().getOntology().getURI());
    }
        
    public Ontology getOntology(String ontologyURI)
    {
        Ontology ontology = getOntModel(ontologyURI, getOntModelSpec()).getOntology(ontologyURI);

        if (ontology == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology resource '{}' not found; processing aborted", ontologyURI);
            throw new OntologyException("Sitemap ontology resource '" + ontologyURI + "' not found; processing aborted");
        }

        ImportCycleChecker checker = new ImportCycleChecker();
        checker.check(ontology);
        if (checker.getCycleOntology() != null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap contains an ontology which forms an import cycle: {}", checker.getCycleOntology());
            throw new OntologyException("Sitemap contains an ontology which forms an import cycle: " + checker.getCycleOntology().getURI());
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
            return ModelFactory.createOntologyModel(ontModelSpec,
                    ModelFactory.createUnion(ModelFactory.createDefaultModel(), ontModel.getBaseModel()));
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
 
    public Providers getProviders()
    {
        return providers;
    }
    
}