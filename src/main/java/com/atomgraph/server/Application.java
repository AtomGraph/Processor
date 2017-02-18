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
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.util.FileManager;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;
import org.apache.jena.util.LocationMapper;
import com.atomgraph.core.provider.ApplicationProvider;
import com.atomgraph.core.provider.ClientProvider;
import com.atomgraph.core.provider.DataManagerProvider;
import com.atomgraph.core.provider.MediaTypesProvider;
import com.atomgraph.server.model.impl.ResourceBase;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.io.ResultSetProvider;
import com.atomgraph.core.io.UpdateRequestReader;
import com.atomgraph.core.provider.DatasetProvider;
import com.atomgraph.core.provider.GraphStoreClientProvider;
import com.atomgraph.core.provider.GraphStoreProvider;
import com.atomgraph.core.provider.SPARQLClientProvider;
import com.atomgraph.core.provider.SPARQLEndpointProvider;
import com.atomgraph.core.provider.ServiceProvider;
import com.atomgraph.server.mapper.ClientExceptionMapper;
import com.atomgraph.server.mapper.ConfigurationExceptionMapper;
import com.atomgraph.server.mapper.ModelExceptionMapper;
import com.atomgraph.server.mapper.NotFoundExceptionMapper;
import com.atomgraph.server.mapper.ArgumentExceptionMapper;
import com.atomgraph.server.mapper.jena.DatatypeFormatExceptionMapper;
import com.atomgraph.server.mapper.jena.QueryParseExceptionMapper;
import com.atomgraph.server.mapper.jena.RiotExceptionMapper;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.impl.ParameterImpl;
import com.atomgraph.processor.model.impl.TemplateImpl;
import com.atomgraph.processor.vocabulary.AP;
import com.atomgraph.server.mapper.OntologyExceptionMapper;
import com.atomgraph.server.provider.OntologyProvider;
import com.atomgraph.server.provider.TemplateProvider;
import com.atomgraph.server.provider.SkolemizingModelProvider;
import com.atomgraph.server.provider.TemplateCallProvider;
import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.arq.ARQFactory;
import org.spinrdf.system.SPINModuleRegistry;
import com.atomgraph.processor.model.Parameter;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Application extends com.atomgraph.core.Application
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();
    private final boolean cacheSitemap;
    
    /**
     * Initializes root resource classes and provider singletons
     * @param servletConfig
     */
    public Application(@Context ServletConfig servletConfig)
    {
        super(servletConfig);

        BuiltinPersonalities.model.add(Parameter.class, ParameterImpl.factory);
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);

        SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
        ARQFactory.get().setUseCaches(false); // enabled caching leads to unexpected QueryBuilder behaviour
        
        if (servletConfig.getInitParameter(AP.cacheSitemap.getURI()) != null)
            cacheSitemap = Boolean.valueOf(servletConfig.getInitParameter(AP.cacheSitemap.getURI()));
        else cacheSitemap = true;
        OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // lets cache the ontologies FTW!!        
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
    @Override
    public void init()
    {
        OntDocumentManager.getInstance().setFileManager(getFileManager(getServletConfig()));        
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());
        
	classes.add(ResourceBase.class); // handles /

        singletons.add(new ApplicationProvider(getServletConfig()));
	singletons.add(new ServiceProvider(getServletConfig()));
        singletons.add(new OntologyProvider(getServletConfig()));
        singletons.add(new TemplateProvider());
        singletons.add(new TemplateCallProvider());
        singletons.add(new SPARQLEndpointProvider(getServletConfig()));
        singletons.add(new GraphStoreProvider(getServletConfig()));
        singletons.add(new DatasetProvider());
	singletons.add(new SPARQLClientProvider());
	singletons.add(new GraphStoreClientProvider());
	singletons.add(new SkolemizingModelProvider());
	singletons.add(new ResultSetProvider());
	singletons.add(new QueryParamProvider());
	singletons.add(new UpdateRequestReader());
        singletons.add(new MediaTypesProvider());
        singletons.add(new DataManagerProvider(getServletConfig()));
        singletons.add(new ClientProvider());
        singletons.add(new RiotExceptionMapper());
	singletons.add(new ModelExceptionMapper());
	singletons.add(new DatatypeFormatExceptionMapper());
        singletons.add(new NotFoundExceptionMapper());
        singletons.add(new ClientExceptionMapper());        
        singletons.add(new ConfigurationExceptionMapper());
        singletons.add(new OntologyExceptionMapper());
        singletons.add(new ArgumentExceptionMapper());
	singletons.add(new QueryParseExceptionMapper());
     
        if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", classes, singletons);
    }
    
    public FileManager getFileManager(ServletConfig servletConfig)
    {
        String uriConfig = "/WEB-INF/classes/location-mapping.n3"; // TO-DO: make configurable (in web.xml)
        String syntax = FileUtils.guessLang(uriConfig);
        Model mapping = ModelFactory.createDefaultModel();
        try (InputStream in = servletConfig.getServletContext().getResourceAsStream(uriConfig))
        {
            mapping.read(in, uriConfig, syntax) ;
            FileManager fileManager = FileManager.get();
            fileManager.setLocationMapper(new LocationMapper(mapping));
            return fileManager;
        }
        catch (IOException ex)
        {
            if (log.isDebugEnabled()) log.debug("Error reading location mapping: {}", uriConfig);
            throw new WebApplicationException(ex);
        }
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
    
    public final boolean getCacheSitemap()
    {
        return cacheSitemap;
    }
    
}
