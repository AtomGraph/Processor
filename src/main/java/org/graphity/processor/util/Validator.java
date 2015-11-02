/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.util;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.List;
import org.graphity.processor.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.system.SPINModuleRegistry;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Validator
{
    private static final Logger log = LoggerFactory.getLogger(Validator.class);
    
    private OntModel ontModel;
    
    protected Validator()
    {
    }
    
    protected static Validator newInstance()
    {
	return new Validator();
    }
    
    public Validator ontModel(OntModel ontModel)
    {
	if (ontModel == null) throw new IllegalArgumentException("OntModel cannot be null");
        this.ontModel = ontModel;
        SPINModuleRegistry.get().registerAll(ontModel, null);
        return this;
    }
    
    public static Validator fromOntModel(OntModel ontModel)
    {
        return newInstance().ontModel(ontModel);
    }

    public Model validate(Model model)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");

        // It looks like we don't need annotation inheritance Rules reasoner during validation.
        // Existing data should be valid; only the incoming RDF Model should be able to violate the constraints.
        OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM; // getOntModel().getSpecification()
        OntModel tempModel = ModelFactory.createOntologyModel(ontModelSpec);
        tempModel.add(fixOntModel(getOntModel())).add(model);
	List<ConstraintViolation> cvs = SPINConstraints.check(tempModel, null);
	if (!cvs.isEmpty())
        {
            if (log.isDebugEnabled()) log.debug("SPIN constraint violations: {}", cvs);
            throw new ConstraintViolationException(cvs, model);
        }
        
        return model;
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
"  FILTER (!(?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> && ?o = <http://graphity.org/gp#Constraint>))\n" +
"}");
        
        QueryExecution qex = QueryExecutionFactory.create(fix, ontModel);
        fixedModel.add(qex.execConstruct());
        
        return fixedModel;
    }
    
    public OntModel getOntModel()
    {
        return ontModel;
    }

}
