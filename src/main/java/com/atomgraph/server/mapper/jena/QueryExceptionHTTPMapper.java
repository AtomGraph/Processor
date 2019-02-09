/**
 *  Copyright 2013 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.server.mapper.jena;

import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.atomgraph.server.mapper.ExceptionMapperBase;
import org.apache.jena.query.DatasetFactory;

/**
 * Maps (tunnels) Jena's remote query execution exception.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
@Deprecated
public class QueryExceptionHTTPMapper extends ExceptionMapperBase implements ExceptionMapper<QueryExceptionHTTP>
{

    @Override
    public Response toResponse(QueryExceptionHTTP ex)
    {        
        if (ex.getResponseCode() > 0)
            return getResponseBuilder(DatasetFactory.create(toResource(ex, Response.Status.fromStatusCode(ex.getResponseCode()),
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel())).
                status(ex.getResponseCode()).
                build();
        else
            return getResponseBuilder(DatasetFactory.create(toResource(ex, Response.Status.INTERNAL_SERVER_ERROR,
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel())).
                status(Response.Status.INTERNAL_SERVER_ERROR).
                build();
    }

}