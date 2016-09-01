/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.server;

import java.io.InputStream;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.util.FileManager;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileUtils;
import org.apache.jena.util.LocationMapper;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.provider.ClientProvider;
import com.atomgraph.core.provider.DataManagerProvider;
import com.atomgraph.core.provider.MediaTypesProvider;
import com.atomgraph.server.model.impl.ResourceBase;
import com.atomgraph.server.provider.DatasetProvider;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.provider.ResultSetProvider;
import com.atomgraph.core.provider.UpdateRequestReader;
import com.atomgraph.server.mapper.ClientExceptionMapper;
import com.atomgraph.server.mapper.ConfigurationExceptionMapper;
import com.atomgraph.server.mapper.ModelExceptionMapper;
import com.atomgraph.server.mapper.NotFoundExceptionMapper;
import com.atomgraph.server.mapper.SPINArgumentExceptionMapper;
import com.atomgraph.server.mapper.jena.DatatypeFormatExceptionMapper;
import com.atomgraph.server.mapper.jena.QueryParseExceptionMapper;
import com.atomgraph.server.mapper.jena.RiotExceptionMapper;
import com.atomgraph.processor.model.Argument;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.model.impl.ArgumentImpl;
import com.atomgraph.processor.model.impl.TemplateCallImpl;
import com.atomgraph.processor.model.impl.TemplateImpl;
import com.atomgraph.processor.vocabulary.AP;
import com.atomgraph.server.provider.GraphStoreOriginProvider;
import com.atomgraph.server.provider.GraphStoreProvider;
import com.atomgraph.server.provider.OntologyProvider;
import com.atomgraph.server.provider.TemplateCallProvider;
import com.atomgraph.server.provider.SPARQLEndpointOriginProvider;
import com.atomgraph.server.provider.SPARQLEndpointProvider;
import com.atomgraph.server.provider.SkolemizingModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Application extends com.atomgraph.core.Application
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
        singletons.add(new OntologyProvider(servletConfig));
        singletons.add(new TemplateCallProvider());
	singletons.add(new SPARQLEndpointProvider());
	singletons.add(new SPARQLEndpointOriginProvider());
        singletons.add(new GraphStoreProvider());
        singletons.add(new GraphStoreOriginProvider());
        singletons.add(new RiotExceptionMapper());
	singletons.add(new ModelExceptionMapper());
	singletons.add(new DatatypeFormatExceptionMapper());
        singletons.add(new NotFoundExceptionMapper());
        singletons.add(new ClientExceptionMapper());        
        singletons.add(new ConfigurationExceptionMapper());
        singletons.add(new SPINArgumentExceptionMapper());
	singletons.add(new QueryParseExceptionMapper());
    }
    
    /**
     * Initializes (post construction) DataManager, its LocationMapper and Locators, and Context
     * 
     * @see com.atomgraph.client.util.DataManager
     * @see com.atomgraph.processor.locator
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/FileManager.html">FileManager</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/LocationMapper.html">LocationMapper</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/util/Locator.html">Locator</a>
     * @see <a href="http://jena.apache.org/documentation/javadoc/arq/com/hp/hpl/jena/sparql/util/Context.html">Context</a>
     */
    @PostConstruct
    public void init()
    {
        if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", getClasses(), getSingletons());

        initPersonalities(BuiltinPersonalities.model); // map model interfaces to implementations
	SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
        ARQFactory.get().setUseCaches(false); // enabled caching leads to unexpected QueryBuilder behaviour
        
        FileManager fileManager = getFileManager();
	if (log.isDebugEnabled()) log.debug("getFileManager(): {}", fileManager);
        initOntDocumentManager(fileManager);
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());

        boolean cacheSitemap = true;
        if (getServletConfig().getInitParameter(AP.cacheSitemap.getURI()) != null)
            cacheSitemap = Boolean.valueOf(getServletConfig().getInitParameter(AP.cacheSitemap.getURI()));
        OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // lets cache the ontologies FTW!!
    }

    private static void initPersonalities(Personality<RDFNode> personality)
    {
        if (personality == null) throw new IllegalArgumentException("Personality<RDFNode> cannot be null");
        
        personality.add(Argument.class, ArgumentImpl.factory);
        personality.add(Template.class, TemplateImpl.factory);
        personality.add(TemplateCall.class, TemplateCallImpl.factory);
    }
    
    public void initOntDocumentManager(FileManager fileManager)
    {
        OntDocumentManager.getInstance().setFileManager(fileManager);
    }
    
    public FileManager getFileManager()
    {
        String uriConfig = "/WEB-INF/classes/location-mapping.n3"; // TO-DO: make configurable (in web.xml)
        String syntax = FileUtils.guessLang(uriConfig);
        Model mapping = ModelFactory.createDefaultModel();
        InputStream in = getServletConfig().getServletContext().getResourceAsStream(uriConfig);
        mapping.read(in, uriConfig, syntax) ;
        FileManager fileManager = FileManager.get();
        fileManager.setLocationMapper(new LocationMapper(mapping));
        return fileManager;
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
