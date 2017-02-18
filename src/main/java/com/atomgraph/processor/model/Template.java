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
package com.atomgraph.processor.model;

import com.atomgraph.processor.query.QueryBuilder;
import com.atomgraph.processor.update.ModifyBuilder;
import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.CacheControl;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public interface Template extends OntClass
{

    static public final Comparator<Template> COMPARATOR = new Comparator<Template>()
    {

        @Override
        public int compare(Template template1, Template template2)
        {
            // Template always has default priority
            double diff = template2.getPriority() - template1.getPriority();
            if (diff > 0) return 1;
            if (diff < 0) return -1;
            
            return UriTemplate.COMPARATOR.compare(template1.getPath(), template2.getPath());
        }

    };
        
    UriTemplate getPath();
    
    String getSkolemTemplate();

    String getFragmentTemplate();
    
    org.apache.jena.rdf.model.Resource getQuery();
    
    org.apache.jena.rdf.model.Resource getUpdate();
    
    Double getPriority();
        
    Map<Property, Parameter> getParameters();
    
    Map<Property, Parameter> getLocalParameters();
   
    Map<String, Parameter> getParameterMap();

    // Map<Property, RDFNode> getDefaultValues();

    List<Locale> getLanguages();
    
    org.apache.jena.rdf.model.Resource getLoadClass();
    
    CacheControl getCacheControl();

    QueryBuilder getQueryBuilder(URI base);
        
    QueryBuilder getQueryBuilder(URI base, Model commandModel);

    ModifyBuilder getModifyBuilder(URI base);
    
    ModifyBuilder getModifyBuilder(URI base, Model commandModel);

}
