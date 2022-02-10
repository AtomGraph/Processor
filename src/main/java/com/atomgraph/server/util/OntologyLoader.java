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
package com.atomgraph.server.util;

import com.atomgraph.core.util.jena.DataManager;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.rdf.model.ModelFactory;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.util.OntModelReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application ontology provider.
 * 
 * @see org.apache.jena.ontology.Ontology
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class OntologyLoader
{
    private static final Logger log = LoggerFactory.getLogger(OntologyLoader.class);
    
    private final OntDocumentManager ontDocumentManager;
    private final String ontologyURI;
    
    public OntologyLoader(final OntDocumentManager ontDocumentManager, final String ontologyURI,
            final OntModelSpec materializationSpec, final boolean materialize)
    {
        if (ontDocumentManager == null) throw new IllegalArgumentException("OntDocumentManager cannot be null");
        if (ontologyURI == null) throw new IllegalArgumentException("URI cannot be null");
        if (materializationSpec == null) throw new IllegalArgumentException("OntModelSpec cannot be null");
        
        this.ontDocumentManager = ontDocumentManager;
        this.ontologyURI = ontologyURI;
        
        // materialize OntModel inferences to avoid invoking rules engine on every request
        if (!ontDocumentManager.getFileManager().hasCachedModel(ontologyURI))
        {
            OntModel ontModel = ontDocumentManager.getOntology(ontologyURI, materializationSpec);
            Ontology ontology = ontModel.getOntology(ontologyURI);
            OntModel materializedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM); // no inference
            materializedModel.add(ontModel);
            ontDocumentManager.addModel(ontologyURI, new OntModelReadOnly(materializedModel), true); // make immutable

            ImportCycleChecker checker = new ImportCycleChecker();
            checker.check(ontology);
            if (checker.getCycleOntology() != null)
            {
                if (log.isErrorEnabled()) log.error("Sitemap contains an ontology which forms an import cycle: {}", checker.getCycleOntology());
                throw new OntologyException("Sitemap contains an ontology which forms an import cycle: " + checker.getCycleOntology().getURI());
            }
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
    
    public Ontology getOntology()
    {
        OntModelSpec loadSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        
        // attempt to use DataManager to retrieve owl:import Models
        if (getOntDocumentManager().getFileManager() instanceof DataManager)
            loadSpec.setImportModelGetter((DataManager)getOntDocumentManager().getFileManager());
        
        return getOntDocumentManager().getOntology(getOntologyURI(), loadSpec).getOntology(getOntologyURI());
    }

    public OntDocumentManager getOntDocumentManager()
    {
        return ontDocumentManager;
    }
    
    public String getOntologyURI()
    {
        return ontologyURI;
    }
    
}