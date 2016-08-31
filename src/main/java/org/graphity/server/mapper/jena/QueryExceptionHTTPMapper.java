/**
 *  Copyright 2013 Martynas Jusevičius <martynas@graphity.org>
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
package org.graphity.server.mapper.jena;

import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.graphity.server.mapper.ExceptionMapperBase;

/**
 * Maps (tunnels) Jena's remote query execution exception.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
@Deprecated
public class QueryExceptionHTTPMapper extends ExceptionMapperBase implements ExceptionMapper<QueryExceptionHTTP>
{

    @Override
    public Response toResponse(QueryExceptionHTTP ex)
    {        
        if (ex.getResponseCode() > 0)
            return org.graphity.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(toResource(ex, Response.Status.fromStatusCode(ex.getResponseCode()),
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel(), getVariants()).
                status(ex.getResponseCode()).
                build();
        else
            return org.graphity.core.model.impl.Response.fromRequest(getRequest()).
                getResponseBuilder(toResource(ex, Response.Status.INTERNAL_SERVER_ERROR,
                        ResourceFactory.createResource("http://www.w3.org/2011/http-statusCodes#InternalServerError")).
                    getModel(), getVariants()).
                status(Response.Status.INTERNAL_SERVER_ERROR).
                build();
    }

}