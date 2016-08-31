/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.service.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import org.graphity.core.MediaTypes;
import org.graphity.core.util.jena.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of SPARQL endpoints.
 * Implements SPARQL Protocol on Jena dataset.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol for RDF</a>
 */
public class SPARQLEndpointBase extends org.graphity.core.model.impl.SPARQLEndpointBase
{
    private static final Logger log = LoggerFactory.getLogger(SPARQLEndpointBase.class);

    private final Dataset dataset;
    private final DataManager dataManager;

    /**
     * JAX-RS-compatible resource constructor with injected initialization objects.
     * 
     * @param dataset ontology of this webapp
     * @param dataManager RDF data manager for this endpoint
     * @param mediaTypes supported media types
     * @param request current request
     * @param servletConfig webapp context
     */
    public SPARQLEndpointBase(@Context Request request, @Context ServletConfig servletConfig, @Context MediaTypes mediaTypes,
            @Context Dataset dataset, @Context DataManager dataManager)
    {
	super(request, servletConfig, mediaTypes);
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");
        if (dataManager == null) throw new IllegalArgumentException("DataManager cannot be null");
        this.dataset = dataset;
        this.dataManager = dataManager;
    }
    
    /**
     * Loads RDF model by querying RDF dataset.
     * 
     * @param query query object
     * @return loaded model
     */
    @Override
    public Model loadModel(Query query)
    {
        if (log.isDebugEnabled()) log.debug("Loading Model from Dataset using Query: {}", query);
        return loadModel(getDataset(), query);
    }

    /**
     * Loads RDF model from an RDF dataset using a SPARQL query.
     * Only <code>DESCRIBE</code> and <code>CONSTRUCT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result RDF model
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#describe">DESCRIBE</a>
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">CONSTRUCT</a>
     */
    public Model loadModel(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{	
	    if (query.isConstructType()) return qex.execConstruct();
	    if (query.isDescribeType()) return qex.execDescribe();
	
	    throw new QueryExecException("Query to load Model must be CONSTRUCT or DESCRIBE"); // return null;
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }
    
    /**
     * Loads RDF model by querying dataset..
     * 
     * @param query query object
     * @return loaded model
     */
    @Override
    public ResultSetRewindable select(Query query)
    {
        if (log.isDebugEnabled()) log.debug("Loading ResultSet from Dataset using Query: {}", query);
        return loadResultSet(getDataset(), query);
    }

    /**
     * Loads result set from an RDF dataset using a SPARQL query.
     * Only <code>SELECT</code> queries can be used with this method.
     * 
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return result set
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SELECT</a>
     */
    public ResultSetRewindable loadResultSet(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");
	
	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{
	    if (query.isSelectType()) return ResultSetFactory.copyResults(qex.execSelect());
	    
	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }
    
    /**
     * Asks for boolean result by querying dataset.
     * 
     * @param query query object
     * @return boolean result
     */
    @Override
    public boolean ask(Query query)
    {
        if (log.isDebugEnabled()) log.debug("Loading Model from Dataset using Query: {}", query);
        return ask(getDataset(), query);
    }

    /**
     * Returns boolean result from an RDF dataset using a SPARQL query.
     * Only <code>ASK</code> queries can be used with this method.
     *
     * @param dataset the RDF dataset to be queried
     * @param query query object
     * @return boolean result
     * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#ask">ASK</a>
     */
    public boolean ask(Dataset dataset, Query query)
    {
	if (log.isDebugEnabled()) log.debug("Local Dataset Query: {}", query);
	if (dataset == null) throw new IllegalArgumentException("Dataset must be not null");
        if (query == null) throw new IllegalArgumentException("Query must be not null");

	QueryExecution qex = QueryExecutionFactory.create(query, dataset);
	try
	{
	    if (query.isAskType()) return qex.execAsk();

	    throw new QueryExecException("Query to load ResultSet must be SELECT");
	}
	catch (QueryExecException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Local query execution exception: {}", ex);
	    throw ex;
	}
	finally
	{
	    qex.close();
	}
    }
    
    /**
     * Executes update on dataset.
     * 
     * @param updateRequest update request
     */
    @Override
    public void update(UpdateRequest updateRequest)
    {
        if (log.isDebugEnabled()) log.debug("Attempting to update local Model, discarding UpdateRequest: {}", updateRequest);
    }

    public Dataset getDataset()
    {
        return dataset;
    }

    public DataManager getDataManager()
    {
        return dataManager;
    }

}