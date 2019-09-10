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

import com.atomgraph.core.util.jena.DataManager;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.util.iterator.ExtendedIterator;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.Lock;
import com.atomgraph.processor.exception.OntologyException;
import com.sun.jersey.api.client.ClientHandlerException;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application ontology provider.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
public class OntologyProvider extends PerRequestTypeInjectableProvider<Context, Ontology> implements ContextResolver<Ontology>
{
    private static final Logger log = LoggerFactory.getLogger(OntologyProvider.class);
    
    @Context Providers providers;

    private final OntDocumentManager ontDocumentManager;
    private final String ontologyURI;
    
    public OntologyProvider(final OntDocumentManager ontDocumentManager, final String ontologyURI,
            final OntModelSpec materializationSpec, final boolean materialize)
    {
        super(Ontology.class);
        
        if (ontDocumentManager == null) throw new IllegalArgumentException("OntDocumentManager cannot be null");
        if (ontologyURI == null) throw new IllegalArgumentException("URI cannot be null");
        if (materializationSpec == null) throw new IllegalArgumentException("OntModelSpec cannot be null");
        
        this.ontDocumentManager = ontDocumentManager;
        this.ontologyURI = ontologyURI;
        
        // materialize OntModel inferences to avoid invoking rules engine on every request
        if (materialize && materializationSpec.getReasoner() != null)
        {
            OntModel ontModel = getOntModel(ontDocumentManager, ontologyURI, materializationSpec);
            Ontology ontology = ontModel.getOntology(ontologyURI);

            ImportCycleChecker checker = new ImportCycleChecker();
            checker.check(ontology);
            if (checker.getCycleOntology() != null)
            {
                if (log.isErrorEnabled()) log.error("Sitemap contains an ontology which forms an import cycle: {}", checker.getCycleOntology());
                throw new OntologyException("Sitemap contains an ontology which forms an import cycle: " + checker.getCycleOntology().getURI());
            }
            
            OntModel materializedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            materializedModel.add(ontModel);
            ontDocumentManager.addModel(ontologyURI, materializedModel, true);
        }
    }

    public class ImportCycleChecker
    {
        private final Map<Ontology, Boolean> marked = new HashMap<>(), onStack = new HashMap<>();
        private Ontology cycleOntology = null;

        public void check(Ontology ontology)
        {
            if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
            
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
    
    public Ontology getOntology()
    {
        OntModelSpec loadSpec = OntModelSpec.OWL_MEM;
        
        // attempt to use DataManager to retrieve owl:import Models
        if (getOntDocumentManager().getFileManager() instanceof DataManager)
            loadSpec.setImportModelGetter((DataManager)getOntDocumentManager().getFileManager());
        
        return getOntology(loadSpec);
    }
    
    public Ontology getOntology(OntModelSpec loadSpec)
    {   
        return getOntModel(getOntDocumentManager(), getOntologyURI(), loadSpec).getOntology(getOntologyURI());
    }
    
    /**
     * Loads ontology by URI.
     * 
     * @param manager
     * @param ontologyURI ontology location
     * @param ontModelSpec ontology model specification
     * @return ontology model
     */
    public static OntModel getOntModel(OntDocumentManager manager, String ontologyURI, OntModelSpec ontModelSpec)
    {
        if (manager == null) throw new IllegalArgumentException("OntDocumentManager cannot be null");
        if (ontologyURI == null) throw new IllegalArgumentException("URI cannot be null");
        if (ontModelSpec == null) throw new IllegalArgumentException("OntModelSpec cannot be null");
        if (log.isDebugEnabled()) log.debug("Loading sitemap ontology from URI: {}", ontologyURI);

        try
        {
            OntModel ontModel = manager.getOntology(ontologyURI, ontModelSpec);

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
        catch (ClientHandlerException ex) // thrown by DataManager
        {
            // remove ontology from cache, so next time it will be reloaded, possibly with fixed imports
            manager.getFileManager().removeCacheModel(ontologyURI);
            
            if (log.isErrorEnabled()) log.error("Could not load ontology '{}' or its imports", ontologyURI);
            throw new OntologyException("Could not load ontology '" + ontologyURI + "' or its imports", ex);
        }
    }

    public OntDocumentManager getOntDocumentManager()
    {
        return ontDocumentManager;
    }
    
    public String getOntologyURI()
    {
        return ontologyURI;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
}