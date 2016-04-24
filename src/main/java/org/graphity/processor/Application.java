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
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.util.FileManager;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.graphity.core.exception.ConfigurationException;
import org.graphity.core.provider.ClientProvider;
import org.graphity.core.provider.DataManagerProvider;
import org.graphity.core.provider.MediaTypesProvider;
import org.graphity.processor.model.impl.ResourceBase;
import org.graphity.processor.provider.DatasetProvider;
import org.graphity.core.provider.QueryParamProvider;
import org.graphity.core.provider.ResultSetProvider;
import org.graphity.core.provider.UpdateRequestReader;
import org.graphity.processor.mapper.ClientExceptionMapper;
import org.graphity.processor.mapper.ConfigurationExceptionMapper;
import org.graphity.processor.mapper.ConstraintViolationExceptionMapper;
import org.graphity.processor.mapper.NotFoundExceptionMapper;
import org.graphity.processor.mapper.QueryArgumentExceptionMapper;
import org.graphity.processor.mapper.SkolemizationExceptionMapper;
import org.graphity.processor.provider.ConstraintViolationExceptionWriter;
import org.graphity.processor.provider.GraphStoreOriginProvider;
import org.graphity.processor.provider.GraphStoreProvider;
import org.graphity.processor.provider.OntologyProvider;
import org.graphity.processor.provider.OntClassProvider;
import org.graphity.processor.provider.SPARQLEndpointOriginProvider;
import org.graphity.processor.provider.SPARQLEndpointProvider;
import org.graphity.processor.provider.SkolemizationExceptionWriter;
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
public class Application extends org.graphity.core.Application
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();
    
    /**
     * Initializes root resource classes and provider singletons
     * @param servletConfig
     */
    public Application(@Context ServletConfig servletConfig)
    {
        super(servletConfig);
        
	classes.add(ResourceBase.class); // handles /

	singletons.add(new SkolemizingModelProvider());
	singletons.add(new ResultSetProvider());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new MediaTypesProvider());
        singletons.add(new DataManagerProvider());
        singletons.add(new DatasetProvider());
        singletons.add(new ClientProvider());
        singletons.add(new OntologyProvider(getServletConfig()));
        singletons.add(new OntClassProvider());
	singletons.add(new SPARQLEndpointProvider());
	singletons.add(new SPARQLEndpointOriginProvider());
        singletons.add(new GraphStoreProvider());
        singletons.add(new GraphStoreOriginProvider());
        singletons.add(new ConstraintViolationExceptionWriter());
        singletons.add(new SkolemizationExceptionWriter());        
	singletons.add(new ConstraintViolationExceptionMapper());
	singletons.add(new SkolemizationExceptionMapper());
        singletons.add(new NotFoundExceptionMapper());
        singletons.add(new ClientExceptionMapper());        
        singletons.add(new ConfigurationExceptionMapper());
        singletons.add(new QueryArgumentExceptionMapper());
	singletons.add(new org.graphity.processor.mapper.jena.QueryParseExceptionMapper());
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
        
        FileManager fileManager = getFileManager();
	if (log.isDebugEnabled()) log.debug("getFileManager(): {}", fileManager);
        initOntDocumentManager(fileManager);
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());

        boolean cacheSitemap = true;
        if (getServletConfig().getInitParameter(GP.cacheSitemap.getURI()) != null)
            cacheSitemap = Boolean.valueOf(getServletConfig().getInitParameter(GP.cacheSitemap.getURI()));
        OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // lets cache the ontologies FTW!!
    }

    public void initOntDocumentManager(FileManager fileManager)
    {
        OntDocumentManager.getInstance().setFileManager(fileManager);
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
    
    public Query getQuery(DatatypeProperty property)
    {
        return getQuery(getServletConfig().getServletContext(), property);
    }
        
    public Query getQuery(ServletContext servletContext, DatatypeProperty property)
    {
        if (servletContext == null) throw new IllegalArgumentException("ServletContext cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Object query = servletContext.getInitParameter(property.getURI());
        if (query == null)
        {
            if (log.isErrorEnabled()) log.error("Query property '{}' not configured", property);
            throw new ConfigurationException("Sitemap query '" + property + "' not configured");
        }
        
        ParameterizedSparqlString queryString = new ParameterizedSparqlString(query.toString());
        return queryString.asQuery();
    }
    
}
