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

package com.atomgraph.server.filter.response;

import com.atomgraph.processor.exception.OntologyException;
import org.apache.jena.ontology.AllValuesFromRestriction;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.vocabulary.SP;
import org.spinrdf.vocabulary.SPIN;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Constructor
{
    private static final Logger log = LoggerFactory.getLogger(Constructor.class);

    public Resource construct(OntClass forClass, Model targetModel, String baseURI)
    {
        if (targetModel == null) throw new IllegalArgumentException("Model cannot be null");

        return addInstance(forClass, SPIN.constructor, targetModel.createResource(), baseURI, new HashSet<OntClass>());
    }
    
    public Resource addInstance(OntClass forClass, Property property, Resource instance, String baseURI, Set<OntClass> reachedClasses)
    {
        if (forClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        if (instance == null) throw new IllegalArgumentException("Resource cannot be null");
        if (baseURI == null) throw new IllegalArgumentException("Base URI string cannot be null");
        if (reachedClasses == null) throw new IllegalArgumentException("Set<OntClass> cannot be null");
        
//        Statement stmt = getConstructorStmt(forClass, property);
//        if (stmt == null || !stmt.getObject().isResource())
//        {
//            if (log.isErrorEnabled()) log.error("Constructor is invoked but {} is not defined for class '{}'", property, forClass.getURI());
//            throw new OntologyException("Constructor is invoked but '" + property.getURI() + "' not defined for class '" + forClass.getURI() +"'");
//        }

        if (forClass.hasProperty(property))
        {
            Resource constructor = forClass.getPropertyResourceValue(property);
            if (constructor == null)
            {
                if (log.isErrorEnabled()) log.error("Constructor is invoked but {} is not defined for class '{}'", property, forClass.getURI());
                throw new OntologyException("Constructor is invoked but '" + property.getURI() + "' not defined for class '" + forClass.getURI() +"'");
            }
        
            Statement queryText = constructor.getProperty(SP.text);
            if (queryText == null || !queryText.getObject().isLiteral())
            {
                if (log.isErrorEnabled()) log.error("Constructor resource '{}' does not have sp:text property", constructor);
                throw new OntologyException("Constructor resource '" + constructor + "' does not have sp:text property");
            }
            
            Query basedQuery = new ParameterizedSparqlString(queryText.getString(), baseURI).asQuery();
            QuerySolutionMap bindings = new QuerySolutionMap();
            bindings.add(SPIN.THIS_VAR_NAME, instance);
            // skip SPIN template bindings for now - might support later
            
            // execute the constructor on the target model
            try (QueryExecution qex = QueryExecutionFactory.create(basedQuery))
            {
                qex.setInitialBinding(bindings);
                qex.execConstruct(instance.getModel());
            }
            instance.addProperty(RDF.type, forClass);            
        }
        
        //List<Resource> newResources = new ArrayList<>();
        //Set<Resource> reachedTypes = new HashSet<>();
        //Map<Resource, List<CommandWrapper>> class2Constructor = SPINQueryFinder.getClass2QueryMap(ontModel, ontModel, property, false, false);
        // SPINConstructors.constructInstance(ontModel, instance, forClass, targetModel, newResources, reachedTypes, class2Constructor, null, null, null);
        //instance.addProperty(RDF.type, forClass);
        //reachedClasses.add(forClass);
        
        // evaluate AllValuesFromRestriction to construct related instances
        ExtendedIterator<OntClass> superClassIt = forClass.listSuperClasses();
        try
        {
            while (superClassIt.hasNext())
            {
                OntClass superClass = superClassIt.next();

                // construct restriction
                if (superClass.canAs(AllValuesFromRestriction.class))
                {
                    AllValuesFromRestriction avfr = superClass.as(AllValuesFromRestriction.class);
                    if (avfr.getAllValuesFrom().canAs(OntClass.class))
                    {
                        OntClass valueClass = avfr.getAllValuesFrom().as(OntClass.class);
                        if (reachedClasses.contains(valueClass))
                        {
                            if (log.isErrorEnabled()) log.error("Circular template restriction between '{}' and '{}' is not allowed", forClass.getURI(), valueClass.getURI());
                            throw new OntologyException("Circular template restriction between '" + forClass.getURI() + "' and '" + valueClass.getURI() + "' is not allowed");
                        }
                        
                        Resource value = instance.getModel().createResource().
                            addProperty(RDF.type, valueClass);
                        instance.addProperty(avfr.getOnProperty(), value);

                        // add inverse properties
                        ExtendedIterator<? extends OntProperty> it = avfr.getOnProperty().listInverseOf();
                        try
                        {
                            while (it.hasNext())
                            {
                                value.addProperty(it.next(), instance);
                            }
                        }
                        finally
                        {
                            it.close();
                        }

                        addInstance(valueClass, property, value, baseURI, reachedClasses);
                    }
                }
                else
                    // otherwise, construct superclass
                    addInstance(superClass, property, instance, baseURI, reachedClasses);
            }
        }
        finally
        {
            superClassIt.close();
        }
        
        return instance;
    }
    
    /*
    public Statement getConstructorStmt(Resource cls, Property property)
    {
        if (cls == null) throw new IllegalArgumentException("Resource cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        Statement stmt = cls.getProperty(property);        
        if (stmt != null) return stmt;
        
        for (Resource superCls : JenaUtil.getAllSuperClasses(cls))
        {
            Statement superClassStmt = getConstructorStmt(superCls, property);
            if (superClassStmt != null) return superClassStmt;
        }
        
        return null;
    }
    */
    
}
