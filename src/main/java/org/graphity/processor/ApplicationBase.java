/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.graphity.processor;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.graphity.core.exception.ConfigurationException;
import org.graphity.core.provider.DataManagerProvider;
import org.graphity.core.provider.MediaTypesProvider;
import org.graphity.processor.model.impl.ResourceBase;
import org.graphity.processor.provider.DatasetProvider;
import org.graphity.core.provider.QueryParamProvider;
import org.graphity.core.provider.ResultSetWriter;
import org.graphity.core.provider.UpdateRequestReader;
import org.graphity.processor.mapper.ConfigurationExceptionMapper;
import org.graphity.processor.mapper.ConstraintViolationExceptionMapper;
import org.graphity.processor.mapper.NotFoundExceptionMapper;
import org.graphity.processor.provider.ConstraintViolationExceptionProvider;
import org.graphity.processor.provider.GraphStoreOriginProvider;
import org.graphity.processor.provider.GraphStoreProvider;
import org.graphity.processor.provider.HypermediaProvider;
import org.graphity.processor.provider.OntClassMatcher;
import org.graphity.processor.provider.OntologyProvider;
import org.graphity.processor.provider.ModifiersProvider;
import org.graphity.processor.provider.QueriedResourceProvider;
import org.graphity.processor.provider.SPARQLEndpointOriginProvider;
import org.graphity.processor.provider.SPARQLEndpointProvider;
import org.graphity.processor.provider.SkolemizingModelProvider;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ApplicationBase extends org.graphity.core.ApplicationBase
{
    private static final Logger log = LoggerFactory.getLogger(ApplicationBase.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();
    
    /**
     * Initializes root resource classes and provider singletons
     * @param servletConfig
     */
    public ApplicationBase(@Context ServletConfig servletConfig)
    {
        super(servletConfig);
        
	classes.add(ResourceBase.class); // handles /

	singletons.add(new SkolemizingModelProvider());
	singletons.add(new ResultSetWriter());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new MediaTypesProvider());
        singletons.add(new QueriedResourceProvider());
        singletons.add(new ModifiersProvider());
        singletons.add(new HypermediaProvider());
        singletons.add(new DataManagerProvider());
        singletons.add(new DatasetProvider());
        //singletons.add(new OntologyProvider(servletConfig));
        singletons.add(new OntClassMatcher());
	singletons.add(new SPARQLEndpointProvider());
	singletons.add(new SPARQLEndpointOriginProvider());
        singletons.add(new GraphStoreProvider());
        singletons.add(new GraphStoreOriginProvider());
        singletons.add(new ConstraintViolationExceptionProvider());
	singletons.add(new ConstraintViolationExceptionMapper());
        singletons.add(new NotFoundExceptionMapper());
        singletons.add(new ConfigurationExceptionMapper());        
	singletons.add(new org.graphity.processor.mapper.jena.QueryExceptionHTTPMapper());
	singletons.add(new org.graphity.processor.mapper.jena.QueryParseExceptionMapper());
	singletons.add(new org.graphity.processor.mapper.jena.HttpExceptionMapper());
    }

    /**
     * Initializes (post construction) DataManager, its LocationMapper and Locators, and Context
     * 
     * @see org.graphity.client.util.DataManager
     * @see org.graphity.processor.locator
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">FileManager</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html">LocationMapper</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html">Locator</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">Context</a>
     */
    @PostConstruct
    public void init()
    {
        if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", getClasses(), getSingletons());

	SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
        ARQFactory.get().setUseCaches(false); // enabled caching leads to unexpected QueryBuilder behaviour

        boolean cacheSitemap = true;
        if (getServletConfig().getInitParameter(GP.cacheSitemap.getURI()) != null)
            cacheSitemap = Boolean.valueOf(getServletConfig().getInitParameter(GP.cacheSitemap.getURI()));
        
        FileManager fileManager = getFileManager();
        FileManager.setGlobalFileManager(fileManager);
	if (log.isDebugEnabled()) log.debug("getFileManager(): {}", fileManager);
        
        OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // lets cache the ontologies FTW!!
        OntDocumentManager.getInstance().setFileManager(fileManager);
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());
        
        singletons.add(new OntologyProvider(getOntology()));
    }

    public FileManager getFileManager()
    {
        return FileManager.get();
    }
    
    /**
     * Provides JAX-RS root resource classes.
     *
     * @return set of root resource classes
     * @see <a
     * href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getClasses()">Application.getClasses()</a>
     */
    @Override
    public Set<Class<?>> getClasses()
    {
	return classes;
    }

    /**
     * Provides JAX-RS singleton objects (e.g. resources or Providers)
     * 
     * @return set of singleton objects
     * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Application.html#getSingletons()">Application.getSingletons()</a>
     */
    @Override
    public Set<Object> getSingletons()
    {
	return singletons;
    }

    public Ontology getOntology()
    {
        Ontology ontology = getOntology(getServletConfig(), GP.sitemap, GP.sitemapRules);
        
        if (ontology == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology resource not found; processing aborted");
            throw new ConfigurationException("Sitemap ontology resource not found; processing aborted");
        }

        return ontology;
    }
    
    public OntModelSpec getOntModelSpec(List<Rule> rules)
    {
        OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        
        if (rules != null)
        {
            Reasoner reasoner = new GenericRuleReasoner(rules);
            //reasoner.setDerivationLogging(true);
            //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
            ontModelSpec.setReasoner(reasoner);
        }
        
        return ontModelSpec;
    }
    
    /**
     * Returns configured sitemap ontology.
     * Uses <code>gp:sitemap</code> context parameter value from web.xml as dataset location.
     * 
     * @param ontModel
     * @param ontologyURI
     * @return ontology model
     */
    public Ontology getOntology(OntModel ontModel, String ontologyURI)
    {
        if (ontModel == null) throw new IllegalArgumentException("OntModel cannot be null");
        if (ontologyURI == null) throw new IllegalArgumentException("Ontology URI String cannot be null");
        
        return ontModel.getOntology(ontologyURI);
    }

    public final Ontology getOntology(ServletConfig servletConfig, ObjectProperty ontologyURIProperty, DatatypeProperty sitemapRulesProperty)
    {
        String ontologyURI = getOntologyURI(servletConfig, ontologyURIProperty);
        if (ontologyURI == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology URI (gp:sitemap) not configured");
            throw new ConfigurationException("Sitemap ontology URI (gp:sitemap) not configured");
        }

        return getOntology(getOntModel(ontologyURI, getOntModelSpec(getRules(servletConfig, sitemapRulesProperty))), ontologyURI);
    }
    
    public String getOntologyURI(ServletConfig servletConfig, ObjectProperty property)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object ontologyURI = servletConfig.getInitParameter(property.getURI());
        if (ontologyURI != null) return ontologyURI.toString();

        return null;
    }
    
    /**
     * Reads ontology model from a file.
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
        if (log.isDebugEnabled()) log.debug("Sitemap model size: {}", ontModel.size());
        
        return ontModel;
    }

    public final List<Rule> getRules(ServletConfig servletConfig, DatatypeProperty property)
    {
        String rules = getRulesString(servletConfig, property);
        if (rules == null) return null;
        
        return Rule.parseRules(rules);
    }

    public String getRulesString(ServletConfig servletConfig, DatatypeProperty property)
    {
        if (servletConfig == null) throw new IllegalArgumentException("ServletConfig cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object rules = servletConfig.getInitParameter(property.getURI());
        if (rules != null) return rules.toString();

        return null;
    }
    
}
