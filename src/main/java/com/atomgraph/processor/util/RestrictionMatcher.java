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

package com.atomgraph.processor.util;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.atomgraph.processor.vocabulary.LDT;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class RestrictionMatcher
{
    private final Ontology ontology;
    private final Query query;    
    
    public RestrictionMatcher(Ontology ontology, Query query)
    {
        this.ontology = ontology;
        this.query = query;
    }
    
    public Map<Property, List<OntClass>> match(Property onProperty, OntClass allValuesFrom)
    {
        return match(getOntology(), onProperty, allValuesFrom);
    }
    
    public Map<Property, List<OntClass>> match(Ontology ontology, Property onProperty, OntClass allValuesFrom)
    {
	if (ontology == null) throw new IllegalArgumentException("OntModel cannot be null");
        if (allValuesFrom == null) throw new IllegalArgumentException("OntClass cannot be null");

        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add(RDFS.isDefinedBy.getLocalName(), ontology);
        qsm.add(OWL.allValuesFrom.getLocalName(), allValuesFrom);
        if (onProperty != null) qsm.add(OWL.onProperty.getLocalName(), onProperty);

        QueryExecution qex = QueryExecutionFactory.create(getQuery(getQuery(), qsm), ontology.getOntModel());
        try
        {
            Map<Property, List<OntClass>> matchedClasses = new HashMap<>();
            ResultSet templates = qex.execSelect();

            while (templates.hasNext())
            {
                QuerySolution solution = templates.next();
                if (solution.contains(LDT.Template.getLocalName())) // solution.contains(OWL.onProperty.getLocalName()
                {
                    OntClass template = solution.getResource(LDT.Template.getLocalName()).as(OntClass.class);

                    if (!matchedClasses.containsKey(onProperty))
                        matchedClasses.put(onProperty, new ArrayList<OntClass>());
                    matchedClasses.get(onProperty).add(template);
                }
            }

            if (matchedClasses.isEmpty())
            {
                ExtendedIterator<OntResource> imports = ontology.listImports();
                try
                {
                    while (imports.hasNext())
                    {
                        OntResource importRes = imports.next();
                        if (importRes.canAs(Ontology.class))
                        {
                            Ontology importedOntology = importRes.asOntology();
                            // traverse imports recursively
                            Map<Property, List<OntClass>> matchedImportClasses = match(importedOntology, onProperty, allValuesFrom);
                            Iterator<Map.Entry<Property, List<OntClass>>> entries = matchedImportClasses.entrySet().iterator();
                            while (entries.hasNext())
                            {
                                Map.Entry<Property, List<OntClass>> entry = entries.next();
                                if (matchedClasses.containsKey(entry.getKey()))
                                    matchedClasses.get(entry.getKey()).addAll(entry.getValue());
                                else
                                    matchedClasses.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
                finally
                {
                    imports.close();
                }
            }

            return matchedClasses;
        }
        finally
        {
            qex.close();
        }
    }

    public Query getQuery(Query query, QuerySolutionMap qsm)
    {
        if (query == null) throw new IllegalArgumentException("Query cannot be null");
        if (qsm == null) throw new IllegalArgumentException("QuerySolution cannot be null");
        
        return new ParameterizedSparqlString(query.toString(), qsm).asQuery();
    }
    
    public Ontology getOntology()
    {
        return ontology;
    }
    
    public Query getQuery()
    {
        return query;
    }
    
}
