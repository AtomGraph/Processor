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

package com.atomgraph.processor.util;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.constraints.ConstraintViolation;
import org.spinrdf.constraints.SPINConstraints;
import org.spinrdf.system.SPINModuleRegistry;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class Validator
{
    private static final Logger log = LoggerFactory.getLogger(Validator.class);
    
    private final OntModel ontModel;
        
    public Validator(OntModel ontModel)
    {
        if (ontModel == null) throw new IllegalArgumentException("OntModel cannot be null");
        this.ontModel = ontModel;
        SPINModuleRegistry.get().registerAll(ontModel, null);
    }

    public List<ConstraintViolation> validate(Model model)
    {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");

        OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM;
        OntModel tempModel = ModelFactory.createOntologyModel(ontModelSpec);
        tempModel.add(fixOntModel(getOntModel())).add(model);
        return SPINConstraints.check(tempModel, null);
    }

    // remove additional types from constraints, otherwise SPIN API will not find their queries :/
    // TO-DO: convert constraints from URI resources to bnodes. Otherwise SPINLabels.getLabel() returns corrupt label
    public OntModel fixOntModel(OntModel ontModel)
    {
        if (ontModel == null) throw new IllegalArgumentException("Model cannot be null");
        
        OntModel fixedModel = ModelFactory.createOntologyModel(ontModel.getSpecification());
        Query fix = QueryFactory.create("CONSTRUCT\n" +
"{\n" +
"  ?s ?p ?o\n" +
"}\n" +
"WHERE\n" +
"{\n" +
"  ?s ?p ?o\n" +
"  FILTER (!(?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> && ?o = <https://www.w3.org/ns/ldt#Constraint>))\n" +
"}");
        
        try (QueryExecution qex = QueryExecutionFactory.create(fix, ontModel))
        {
            fixedModel.add(qex.execConstruct());
        }
        
        return fixedModel;
    }
    
    public OntModel getOntModel()
    {
        return ontModel;
    }

}
