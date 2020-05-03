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

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.exception.ConfigurationException;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.util.FileManager;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.util.LocationMapper;
import com.atomgraph.core.provider.DataManagerProvider;
import com.atomgraph.server.model.impl.ResourceBase;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.core.io.ResultSetProvider;
import com.atomgraph.core.io.UpdateRequestReader;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.core.vocabulary.SD;
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
import com.atomgraph.server.util.OntologyLoader;
import com.atomgraph.server.io.SkolemizingModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.arq.ARQFactory;
import org.spinrdf.system.SPINModuleRegistry;
import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.mapper.ConstraintViolationExceptionMapper;
import java.util.List;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import static com.atomgraph.core.Application.getClient;
import com.atomgraph.core.io.QueryProvider;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.util.jena.DataManager;
import com.atomgraph.core.util.jena.DataManagerImpl;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.model.impl.ApplicationImpl;
import com.atomgraph.server.filter.response.HypermediaFilter;
import com.atomgraph.server.io.SkolemizingDatasetProvider;
import com.atomgraph.server.mapper.ClientErrorExceptionMapper;
import com.atomgraph.server.mapper.NotAcceptableExceptionMapper;
import com.atomgraph.server.mapper.NotSupportedExceptionMapper;
import com.atomgraph.server.factory.TemplateCallFactory;
import java.util.Optional;
import javax.ws.rs.client.Client;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ResourceFactory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.process.internal.RequestScoped;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class Application extends com.atomgraph.core.Application
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final com.atomgraph.processor.model.Application application;
    private final Service service;
    private final String ontologyURI;
    private final OntModelSpec ontModelSpec;
    private final Ontology ontology;
    private final boolean cacheSitemap;
    
    /**
     * Initializes root resource classes and provider singletons
     * @param servletConfig
     */
    public Application(@Context ServletConfig servletConfig)
    {
        this(
            servletConfig.getServletContext().getInitParameter(A.dataset.getURI()) != null ? getDataset(servletConfig.getServletContext().getInitParameter(A.dataset.getURI()), null) : null,
            servletConfig.getServletContext().getInitParameter(SD.endpoint.getURI()) != null ? servletConfig.getServletContext().getInitParameter(SD.endpoint.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(A.graphStore.getURI()) != null ? servletConfig.getServletContext().getInitParameter(A.graphStore.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(A.quadStore.getURI()) != null ? servletConfig.getServletContext().getInitParameter(A.quadStore.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) != null ? servletConfig.getServletContext().getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthUser.getSymbol()) : null,
            servletConfig.getServletContext().getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) != null ? servletConfig.getServletContext().getInitParameter(org.apache.jena.sparql.engine.http.Service.queryAuthPwd.getSymbol()) : null,
            new MediaTypes(), getClient(new ClientConfig()),
            servletConfig.getServletContext().getInitParameter(A.maxGetRequestSize.getURI()) != null ? Integer.parseInt(servletConfig.getServletContext().getInitParameter(A.maxGetRequestSize.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(A.preemptiveAuth.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(A.preemptiveAuth.getURI())) : false,
            new LocationMapper(servletConfig.getServletContext().getInitParameter(AP.locationMapping.getURI()) != null ? servletConfig.getServletContext().getInitParameter(AP.locationMapping.getURI()) : null),
            servletConfig.getServletContext().getInitParameter(LDT.ontology.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDT.ontology.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(AP.sitemapRules.getURI()) != null ? servletConfig.getServletContext().getInitParameter(AP.sitemapRules.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(AP.cacheSitemap.getURI()) != null ? Boolean.valueOf(servletConfig.getServletContext().getInitParameter(AP.cacheSitemap.getURI())) : true
        );
    }
    
    public Application(final Dataset dataset, final String endpointURI, final String graphStoreURI, final String quadStoreURI,
            final String authUser, final String authPwd,
            final MediaTypes mediaTypes, final Client client, final Integer maxGetRequestSize, final boolean preemptiveAuth,
            final LocationMapper locationMapper, final String ontologyURI, final String rulesString, boolean cacheSitemap)
    {
        super(dataset, endpointURI, graphStoreURI, quadStoreURI, authUser, authPwd,
                mediaTypes, client, maxGetRequestSize, preemptiveAuth);
        if (locationMapper == null) throw new IllegalArgumentException("LocationMapper be null");
        
        if (ontologyURI == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap ontology URI (" + LDT.ontology.getURI() + ") not configured");
            throw new ConfigurationException(LDT.ontology);
        }
        if (rulesString == null)
        {
            if (log.isErrorEnabled()) log.error("Sitemap Rules (" + AP.sitemapRules.getURI() + ") not configured");
            throw new ConfigurationException(AP.sitemapRules);
        }
        
        this.ontologyURI = ontologyURI;
        this.cacheSitemap = cacheSitemap;

        if (dataset != null)
            service = new com.atomgraph.core.model.impl.dataset.ServiceImpl(dataset, mediaTypes);
        else
        {
            if (endpointURI == null)
            {
                if (log.isErrorEnabled()) log.error("SPARQL endpoint not configured ('{}' not set in web.xml)", SD.endpoint.getURI());
                throw new ConfigurationException(SD.endpoint);
            }
            if (graphStoreURI == null)
            {
                if (log.isErrorEnabled()) log.error("Graph Store not configured ('{}' not set in web.xml)", A.graphStore.getURI());
                throw new ConfigurationException(A.graphStore);
            }

            service = new com.atomgraph.core.model.impl.remote.ServiceImpl(client, mediaTypes,
                    ResourceFactory.createResource(endpointURI), ResourceFactory.createResource(graphStoreURI),
                    quadStoreURI != null ? ResourceFactory.createResource(quadStoreURI) : null,
                    authUser, authPwd, maxGetRequestSize);
        }
        
        application = new ApplicationImpl(service, ResourceFactory.createResource(ontologyURI));

        List<Rule> rules = Rule.parseRules(rulesString);
        OntModelSpec rulesSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        Reasoner reasoner = new GenericRuleReasoner(rules);
        //reasoner.setDerivationLogging(true);
        //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
        rulesSpec.setReasoner(reasoner);
        this.ontModelSpec = rulesSpec;
        
        BuiltinPersonalities.model.add(Parameter.class, ParameterImpl.factory);
        BuiltinPersonalities.model.add(Template.class, TemplateImpl.factory);

        SPINModuleRegistry.get().init(); // needs to be called before any SPIN-related code
        ARQFactory.get().setUseCaches(false); // enabled caching leads to unexpected QueryBuilder behaviour
        
        DataManager dataManager = new DataManagerImpl(locationMapper, client, mediaTypes, preemptiveAuth);
        FileManager.setStdLocators((FileManager)dataManager);
        FileManager.setGlobalFileManager((FileManager)dataManager);
        if (log.isDebugEnabled()) log.debug("FileManager.get(): {}", FileManager.get());

        OntDocumentManager.getInstance().setFileManager((FileManager)dataManager);
        if (log.isDebugEnabled()) log.debug("OntDocumentManager.getInstance().getFileManager(): {}", OntDocumentManager.getInstance().getFileManager());
        OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // lets cache the ontologies FTW!!
        
        this.ontology = new OntologyLoader(OntDocumentManager.getInstance(), ontologyURI, ontModelSpec, true).getOntology();
    }
    
    /**
     * Initializes JAX-RS resource classes and providers.
     */
    @PostConstruct
    @Override
    public void init()
    {
        register(ResourceBase.class); // handles /
        register(HypermediaFilter.class);
        
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(application).to(com.atomgraph.processor.model.Application.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(ontology).to(Ontology.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(service).to(Service.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(TemplateCallFactory.class).to(new TypeLiteral<Optional<TemplateCall>>() {}).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new MediaTypes()).to(MediaTypes.class);
            }
        });
        
        register(new SkolemizingDatasetProvider());
        register(new SkolemizingModelProvider());
        register(new ResultSetProvider());
        register(new QueryParamProvider());
        register(new QueryProvider());
        register(new UpdateRequestReader());
        register(new DataManagerProvider(getDataManager()));
        register(RiotExceptionMapper.class);
        register(ModelExceptionMapper.class);
        register(ConstraintViolationExceptionMapper.class);
        register(DatatypeFormatExceptionMapper.class);
        register(NotFoundExceptionMapper.class);
        register(NotSupportedExceptionMapper.class);
        register(NotAcceptableExceptionMapper.class);
        register(ClientErrorExceptionMapper.class);
        register(ConfigurationExceptionMapper.class);
        register(OntologyExceptionMapper.class);
        register(ParameterExceptionMapper.class);
        register(QueryParseExceptionMapper.class);
     
        //if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", classes, singletons);
    }
    
    public static FileManager getFileManager(LocationMapper locationMapper)
    {
        FileManager fileManager = FileManager.get();
        fileManager.setLocationMapper(locationMapper);
        return fileManager;
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
