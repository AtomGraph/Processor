/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.server.model.impl;

import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.model.GraphStore;
import com.atomgraph.core.model.SPARQLEndpoint;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.Application;
import com.atomgraph.processor.query.QueryBuilder;
import com.atomgraph.processor.query.SelectBuilder;
import com.atomgraph.processor.util.TemplateCall;
import com.atomgraph.processor.vocabulary.LDTDH;
import com.sun.jersey.api.core.ResourceContext;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ContainerBase extends ResourceBase
{
    
    private static final Logger log = LoggerFactory.getLogger(ContainerBase.class);

    private QueryBuilder queryBuilder;
    
    public ContainerBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletConfig servletConfig,
            @Context MediaTypes mediaTypes,
            @Context Application application, @Context SPARQLEndpoint sparqlEndpoint, @Context GraphStore graphStore,
            @Context Ontology ontology, @Context TemplateCall stateBuilder,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
        super(uriInfo, request, servletConfig, mediaTypes,
            application, sparqlEndpoint, graphStore,
            ontology, stateBuilder,
            httpHeaders, resourceContext);
    }
    
    @Override
    public void init()
    {
        super.init();
        
        if (getTemplateCall().getTemplate().equals(LDTDH.Container) || hasSuperClass(getTemplateCall().getTemplate(), LDTDH.Container))
            queryBuilder = getPageQueryBuilder(super.getQueryBuilder(), getTemplateCall().build());
        else queryBuilder = super.getQueryBuilder();
    }
    
    @Override
    public QueryBuilder getPageQueryBuilder(QueryBuilder builder, Resource stateBuilder)
    {
	if (builder == null) throw new IllegalArgumentException("QueryBuilder cannot be null");
	if (stateBuilder == null) throw new IllegalArgumentException("StateBuilder cannot be null");
                
        if (builder.getSubSelectBuilders().isEmpty())
        {
            if (log.isErrorEnabled()) log.error("QueryBuilder '{}' does not contain a sub-SELECT", queryBuilder);
            throw new OntologyException("Sub-SELECT missing in QueryBuilder: " + queryBuilder + "'");
        }

        SelectBuilder subSelectBuilder = builder.getSubSelectBuilders().get(0);
        if (log.isDebugEnabled()) log.debug("Found main sub-SELECT of the query: {}", subSelectBuilder);

        if (stateBuilder.hasProperty(LDTDH.offset))
        {
            Long offset = stateBuilder.getProperty(LDTDH.offset).getLong();
            if (log.isDebugEnabled()) log.debug("Setting OFFSET on container sub-SELECT: {}", offset);
            subSelectBuilder.replaceOffset(offset);
        }

        if (stateBuilder.hasProperty(LDTDH.limit))
        {
            Long limit = stateBuilder.getProperty(LDTDH.limit).getLong();
            if (log.isDebugEnabled()) log.debug("Setting LIMIT on container sub-SELECT: {}", limit);
            subSelectBuilder.replaceLimit(limit);
        }

        if (stateBuilder.hasProperty(LDTDH.orderBy))
        {
            try
            {
                String orderBy = stateBuilder.getProperty(LDTDH.orderBy).getString();

                Boolean desc = false; // ORDERY BY is ASC() by default
                if (stateBuilder.hasProperty(LDTDH.desc)) desc = stateBuilder.getProperty(LDTDH.desc).getBoolean();

                if (log.isDebugEnabled()) log.debug("Setting ORDER BY on container sub-SELECT: ?{} DESC: {}", orderBy, desc);
                subSelectBuilder.replaceOrderBy(null). // any existing ORDER BY condition is removed first
                    orderBy(orderBy, desc);
            }
            catch (IllegalArgumentException ex)
            {
                if (log.isWarnEnabled()) log.warn(ex.getMessage(), ex);
            }
        }
        
        return builder;
    }

    @Override
    public QueryBuilder getQueryBuilder()
    {
        return queryBuilder;
    }
    
}
