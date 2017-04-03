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

import com.atomgraph.core.exception.ConfigurationException;
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
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
import com.atomgraph.server.mapper.ClientExceptionMapper;
import com.atomgraph.server.mapper.ConfigurationExceptionMapper;
import com.atomgraph.server.mapper.ModelExceptionMapper;
import com.atomgraph.server.mapper.NotFoundExceptionMapper;
import com.atomgraph.server.mapper.ParameterExceptionMapper;
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
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.mapper.ConstraintViolationExceptionMapper;
import com.atomgraph.server.provider.ApplicationProvider;
import java.util.List;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Application extends com.atomgraph.core.Application
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Set<Class<?>> classes = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();
    private final String ontologyURI;
    private final OntModelSpec ontModelSpec;
    private final boolean cacheSitemap;
    
    /**
     * Initializes root resource classes and provider singletons
     * @param servletConfig
     */
    public Application(@Context ServletConfig servletConfig)
    {
        this(
            servletConfig.getInitParameter(A.dataset.getURI()) != null ? servletConfig.getInitParameter(A.dataset.getURI()) : null,
            servletConfig.getInitParameter(SD.endpoint.getURI()) != null ? servletConfig.getInitParameter(SD.endpoint.getURI()) : null,
            servletConfig.getInitParameter(A.graphStore.getURI()) != null ? servletConfig.getInitParameter(A.graphStore.getURI()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) : null,
            servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) != null ? servletConfig.getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) : null,
            servletConfig.getInitParameter(A.preemptiveAuth.getURI()) != null ? Boolean.parseBoolean(servletConfig.getInitParameter(A.preemptiveAuth.getURI())) : false,
            getFileManager(servletConfig, "/WEB-INF/classes/location-mapping.n3"),
            servletConfig.getInitParameter(LDT.ontology.getURI()) != null ? servletConfig.getInitParameter(LDT.ontology.getURI()) : null,
            getOntModelSpec(servletConfig),
            servletConfig.getInitParameter(AP.cacheSitemap.getURI()) != null ? Boolean.valueOf(servletConfig.getInitParameter(AP.cacheSitemap.getURI())) : true
        );       
    }
    
    public Application(final String datasetLocation, final String endpointURI, final String graphStoreURI, final String authUser, final String authPwd, final boolean preemptiveAuth,
            final FileManager fileManager, final String ontologyURI, final OntModelSpec ontModelSpec, boolean cacheSitemap)
    {
        super(datasetLocation, endpointURI, graphStoreURI, authUser, authPwd, preemptiveAuth);
        if (ontologyURI == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology URI (" + LDT.ontology.getURI() + ") not configured");
            throw new ConfigurationException(LDT.ontology);
        }
        this.ontologyURI = ontologyURI;
        this.ontModelSpec = ontModelSpec;
        this.cacheSitemap = cacheSitemap;

        BuiltinPersonalities.model.add(Parameter.class, ParameterImpl.factory);
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);

        SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
        ARQFactory.get().setUseCaches(false); // enabled caching leads to unexpected QueryBuilder behaviour
        
        OntDocumentManager.getInstance().setFileManager(fileManager);        
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());        
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
        classes.add(ResourceBase.class); // handles /

        singletons.add(new ApplicationProvider());
        singletons.add(new ServiceProvider(getService()));
        singletons.add(new OntologyProvider(getOntologyURI(), getOntModelSpec(), true));
        singletons.add(new TemplateProvider());
        singletons.add(new TemplateCallProvider());
        singletons.add(new SPARQLEndpointProvider());
        singletons.add(new GraphStoreProvider());
        singletons.add(new DatasetProvider(getDataset()));
        singletons.add(new SPARQLClientProvider());
        singletons.add(new GraphStoreClientProvider());
        singletons.add(new SkolemizingModelProvider());
        singletons.add(new ResultSetProvider());
        singletons.add(new QueryParamProvider());
        singletons.add(new UpdateRequestReader());
        singletons.add(new MediaTypesProvider());
        singletons.add(new DataManagerProvider(isPreemptiveAuth()));
        singletons.add(new ClientProvider());
        singletons.add(new RiotExceptionMapper());
        singletons.add(new ModelExceptionMapper());
        singletons.add(new ConstraintViolationExceptionMapper());
        singletons.add(new DatatypeFormatExceptionMapper());
        singletons.add(new NotFoundExceptionMapper());
        singletons.add(new ClientExceptionMapper());        
        singletons.add(new ConfigurationExceptionMapper());
        singletons.add(new OntologyExceptionMapper());
        singletons.add(new ParameterExceptionMapper());
        singletons.add(new QueryParseExceptionMapper());
     
        if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", classes, singletons);
    }
    
    public static OntModelSpec getOntModelSpec(ServletConfig servletConfig)
    {
        if (servletConfig == null)  throw new IllegalArgumentException("ServletConfig cannot be null");
        
        Object rulesParam = servletConfig.getInitParameter(AP.sitemapRules.getURI());
        if (rulesParam == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap Rules (" + AP.sitemapRules.getURI() + ") not configured");
            throw new ConfigurationException(AP.sitemapRules);
        }
        
        List<Rule> rules = Rule.parseRules(rulesParam.toString());
        OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        Reasoner reasoner = new GenericRuleReasoner(rules);
        //reasoner.setDerivationLogging(true);
        //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
        ontModelSpec.setReasoner(reasoner);
        return ontModelSpec;
    }
    
    public static FileManager getFileManager(ServletConfig servletConfig, String path)
    {
        String syntax = FileUtils.guessLang(path);
        Model mapping = ModelFactory.createDefaultModel();
        try (InputStream in = servletConfig.getServletContext().getResourceAsStream(path))
        {
            mapping.read(in, path, syntax) ;
            FileManager fileManager = FileManager.get();
            fileManager.setLocationMapper(new LocationMapper(mapping));
            return fileManager;
        }
        catch (IOException ex)
        {
            if (log.isDebugEnabled()) log.debug("Error reading location mapping: {}", path);
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
    
    public String getOntologyURI()
    {
        return ontologyURI;
    }
    
    public OntModelSpec getOntModelSpec()
    {
        return ontModelSpec;    
    }
    
    public final boolean isCacheSitemap()
    {
        return cacheSitemap;
    }
    
}
