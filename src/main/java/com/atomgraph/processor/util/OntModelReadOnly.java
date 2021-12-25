/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.AllDifferent;
import org.apache.jena.ontology.AllValuesFromRestriction;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.CardinalityQRestriction;
import org.apache.jena.ontology.CardinalityRestriction;
import org.apache.jena.ontology.ComplementClass;
import org.apache.jena.ontology.DataRange;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.EnumeratedClass;
import org.apache.jena.ontology.FunctionalProperty;
import org.apache.jena.ontology.HasValueRestriction;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.IntersectionClass;
import org.apache.jena.ontology.InverseFunctionalProperty;
import org.apache.jena.ontology.MaxCardinalityQRestriction;
import org.apache.jena.ontology.MaxCardinalityRestriction;
import org.apache.jena.ontology.MinCardinalityQRestriction;
import org.apache.jena.ontology.MinCardinalityRestriction;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.Profile;
import org.apache.jena.ontology.QualifiedRestriction;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.SomeValuesFromRestriction;
import org.apache.jena.ontology.SymmetricProperty;
import org.apache.jena.ontology.TransitiveProperty;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Alt;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelChangedListener;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.NsIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFReaderI;
import org.apache.jena.rdf.model.RDFWriterI;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceF;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Derivation;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.Lock;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 *
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public class OntModelReadOnly implements OntModel
{

    private final OntModel ontModel;
    
    public OntModelReadOnly(OntModel ontModel)
    {
        this.ontModel = ontModel;
    }
    
    @Override
    public ExtendedIterator<Ontology> listOntologies()
    {
        return ontModel.listOntologies();
    }

    @Override
    public ExtendedIterator<OntProperty> listOntProperties()
    {
     return ontModel.listOntProperties();
    }

    @Override
    public ExtendedIterator<OntProperty> listAllOntProperties()
    {
        return ontModel.listAllOntProperties();
    }

    @Override
    public ExtendedIterator<ObjectProperty> listObjectProperties()
    {
        return ontModel.listObjectProperties();
    }

    @Override
    public ExtendedIterator<DatatypeProperty> listDatatypeProperties()
    {
        return ontModel.listDatatypeProperties();
    }

    @Override
    public ExtendedIterator<FunctionalProperty> listFunctionalProperties()
    {
        return ontModel.listFunctionalProperties();
    }

    @Override
    public ExtendedIterator<TransitiveProperty> listTransitiveProperties()
    {
        return ontModel.listTransitiveProperties();
    }

    @Override
    public ExtendedIterator<SymmetricProperty> listSymmetricProperties()
    {
        return ontModel.listSymmetricProperties();
    }

    @Override
    public ExtendedIterator<InverseFunctionalProperty> listInverseFunctionalProperties()
    {
        return ontModel.listInverseFunctionalProperties();
    }

    @Override
    public ExtendedIterator<Individual> listIndividuals()
    {
        return ontModel.listIndividuals();
    }

    @Override
    public ExtendedIterator<Individual> listIndividuals(Resource arg0)
    {
        return ontModel.listIndividuals(arg0);
    }

    @Override
    public ExtendedIterator<OntClass> listClasses()
    {
        return ontModel.listClasses();
    }

    @Override
    public ExtendedIterator<OntClass> listHierarchyRootClasses()
    {
     return ontModel.listHierarchyRootClasses();
    }

    @Override
    public ExtendedIterator<EnumeratedClass> listEnumeratedClasses()
    {
        return ontModel.listEnumeratedClasses();
    }

    @Override
    public ExtendedIterator<UnionClass> listUnionClasses()
    {
        return ontModel.listUnionClasses();
    }

    @Override
    public ExtendedIterator<ComplementClass> listComplementClasses()
    {
        return ontModel.listComplementClasses();
    }

    @Override
    public ExtendedIterator<IntersectionClass> listIntersectionClasses()
    {
        return ontModel.listIntersectionClasses();
    }

    @Override
    public ExtendedIterator<OntClass> listNamedClasses()
    {
        return ontModel.listNamedClasses();
    }

    @Override
    public ExtendedIterator<Restriction> listRestrictions()
    {
        return ontModel.listRestrictions();
    }

    @Override
    public ExtendedIterator<AnnotationProperty> listAnnotationProperties()
    {
        return ontModel.listAnnotationProperties();
    }

    @Override
    public ExtendedIterator<AllDifferent> listAllDifferent()
    {
        return ontModel.listAllDifferent();
    }

    @Override
    public ExtendedIterator<DataRange> listDataRanges()
    {
        return ontModel.listDataRanges();
    }

    @Override
    public Ontology getOntology(String arg0)
    {
        return ontModel.getOntology(arg0);
    }

    @Override
    public Individual getIndividual(String arg0)
    {
        return ontModel.getIndividual(arg0);
    }

    @Override
    public OntProperty getOntProperty(String arg0)
    {
        return ontModel.getOntProperty(arg0);
    }

    @Override
    public ObjectProperty getObjectProperty(String arg0)
    {
        return ontModel.getObjectProperty(arg0);
    }

    @Override
    public TransitiveProperty getTransitiveProperty(String arg0)
    {
        return ontModel.getTransitiveProperty(arg0);
    }

    @Override
    public SymmetricProperty getSymmetricProperty(String arg0)
    {
        return ontModel.getSymmetricProperty(arg0);
    }

    @Override
    public InverseFunctionalProperty getInverseFunctionalProperty(String arg0)
    {
        return ontModel.getInverseFunctionalProperty(arg0);
    }

    @Override
    public DatatypeProperty getDatatypeProperty(String arg0)
    {
        return ontModel.getDatatypeProperty(arg0);
    }

    @Override
    public AnnotationProperty getAnnotationProperty(String arg0)
    {
        return ontModel.getAnnotationProperty(arg0);
    }

    @Override
    public OntResource getOntResource(String arg0)
    {
        return ontModel.getOntResource(arg0);
    }

    @Override
    public OntResource getOntResource(Resource arg0)
    {
        return ontModel.getOntResource(arg0);
    }

    @Override
    public OntClass getOntClass(String arg0)
    {
        return ontModel.getOntClass(arg0);
    }

    @Override
    public ComplementClass getComplementClass(String arg0)
    {
        return ontModel.getComplementClass(arg0);
    }

    @Override
    public EnumeratedClass getEnumeratedClass(String arg0)
    {
        return ontModel.getEnumeratedClass(arg0);
    }

    @Override
    public UnionClass getUnionClass(String arg0)
    {
        return ontModel.getUnionClass(arg0);
    }

    @Override
    public IntersectionClass getIntersectionClass(String arg0)
    {
        return ontModel.getIntersectionClass(arg0);
    }

    @Override
    public Restriction getRestriction(String arg0)
    {
        return ontModel.getRestriction(arg0);
    }

    @Override
    public HasValueRestriction getHasValueRestriction(String arg0)
    {
        return ontModel.getHasValueRestriction(arg0);
    }

    @Override
    public SomeValuesFromRestriction getSomeValuesFromRestriction(String arg0)
    {
        return ontModel.getSomeValuesFromRestriction(arg0);
    }

    @Override
    public AllValuesFromRestriction getAllValuesFromRestriction(String arg0)
    {
        return ontModel.getAllValuesFromRestriction(arg0);
    }

    @Override
    public CardinalityRestriction getCardinalityRestriction(String arg0)
    {
        return ontModel.getCardinalityRestriction(arg0);
    }

    @Override
    public MinCardinalityRestriction getMinCardinalityRestriction(String arg0)
    {
        return ontModel.getMinCardinalityRestriction(arg0);
    }

    @Override
    public MaxCardinalityRestriction getMaxCardinalityRestriction(String arg0)
    {
        return ontModel.getMaxCardinalityRestriction(arg0);
    }

    @Override
    public QualifiedRestriction getQualifiedRestriction(String arg0)
    {
        return ontModel.getQualifiedRestriction(arg0);
    }

    @Override
    public CardinalityQRestriction getCardinalityQRestriction(String arg0)
    {
        return ontModel.getCardinalityQRestriction(arg0);
    }

    @Override
    public MinCardinalityQRestriction getMinCardinalityQRestriction(String arg0)
    {
        return ontModel.getMinCardinalityQRestriction(arg0);
    }

    @Override
    public MaxCardinalityQRestriction getMaxCardinalityQRestriction(String arg0)
    {
        return ontModel.getMaxCardinalityQRestriction(arg0);
    }

    @Override
    public Ontology createOntology(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Individual createIndividual(Resource arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Individual createIndividual(String arg0, Resource arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public OntProperty createOntProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public ObjectProperty createObjectProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public ObjectProperty createObjectProperty(String arg0, boolean arg1) 
    {
        throw new AddDeniedException();
    }

    @Override
    public TransitiveProperty createTransitiveProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public TransitiveProperty createTransitiveProperty(String arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public SymmetricProperty createSymmetricProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public SymmetricProperty createSymmetricProperty(String arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public InverseFunctionalProperty createInverseFunctionalProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public InverseFunctionalProperty createInverseFunctionalProperty(String arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public DatatypeProperty createDatatypeProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public DatatypeProperty createDatatypeProperty(String arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public AnnotationProperty createAnnotationProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public OntClass createClass()
    {
        throw new AddDeniedException();
    }

    @Override
    public OntClass createClass(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public ComplementClass createComplementClass(String arg0, Resource arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public EnumeratedClass createEnumeratedClass(String arg0, RDFList arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public UnionClass createUnionClass(String arg0, RDFList arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public IntersectionClass createIntersectionClass(String arg0, RDFList arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Restriction createRestriction(Property arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Restriction createRestriction(String arg0, Property arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public HasValueRestriction createHasValueRestriction(String arg0, Property arg1, RDFNode arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public SomeValuesFromRestriction createSomeValuesFromRestriction(String arg0, Property arg1, Resource arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public AllValuesFromRestriction createAllValuesFromRestriction(String arg0, Property arg1, Resource arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public CardinalityRestriction createCardinalityRestriction(String arg0, Property arg1, int arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public MinCardinalityRestriction createMinCardinalityRestriction(String arg0, Property arg1, int arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public MaxCardinalityRestriction createMaxCardinalityRestriction(String arg0, Property arg1, int arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public MaxCardinalityQRestriction createMaxCardinalityQRestriction(String arg0, Property arg1, int arg2, OntClass arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public MinCardinalityQRestriction createMinCardinalityQRestriction(String arg0, Property arg1, int arg2, OntClass arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public CardinalityQRestriction createCardinalityQRestriction(String arg0, Property arg1, int arg2, OntClass arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public DataRange createDataRange(RDFList arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public AllDifferent createAllDifferent()
    {
        throw new AddDeniedException();
    }

    @Override
    public AllDifferent createAllDifferent(RDFList arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public <T extends OntResource> T createOntResource(Class<T> arg0, Resource arg1, String arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public OntResource createOntResource(String arg0) 
    {
        throw new AddDeniedException();
    }

    @Override
    public void loadImports()
    {
        throw new AddDeniedException();
    }

    @Override
    public Set<String> listImportedOntologyURIs()
    {
        return ontModel.listImportedOntologyURIs();
    }

    @Override
    public Set<String> listImportedOntologyURIs(boolean arg0)
    {
        return ontModel.listImportedOntologyURIs(arg0);
    }

    @Override
    public boolean hasLoadedImport(String arg0)
    {
        return ontModel.hasLoadedImport(arg0);
    }

    @Override
    public void addLoadedImport(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public void removeLoadedImport(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Profile getProfile()
    {
        return ontModel.getProfile();
    }

    @Override
    public ModelMaker getModelMaker()
    {
        return ontModel.getModelMaker();
    }

    @Override
    public ModelMaker getImportModelMaker()
    {
        return ontModel.getImportModelMaker();
    }

    @Override
    public List<Graph> getSubGraphs()
    {
        return ontModel.getSubGraphs();
    }

    @Override
    public ExtendedIterator<OntModel> listImportedModels()
    {
        return ontModel.listImportedModels();
    }

    @Override
    public ExtendedIterator<OntModel> listSubModels(boolean arg0)
    {
        return ontModel.listSubModels(arg0);
    }

    @Override
    public ExtendedIterator<OntModel> listSubModels()
    {
        return ontModel.listSubModels();
    }

    @Override
    public int countSubModels()
    {
        return ontModel.countSubModels();
    }

    @Override
    public OntModel getImportedModel(String arg0)
    {
        return ontModel.getImportedModel(arg0);
    }

    @Override
    public Model getBaseModel()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.getBaseModel().getGraph()));
    }

    @Override
    public void addSubModel(Model arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public void addSubModel(Model arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public void removeSubModel(Model arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public void removeSubModel(Model arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public boolean isInBaseModel(RDFNode arg0)
    {
        return ontModel.isInBaseModel(arg0);
    }

    @Override
    public boolean isInBaseModel(Statement arg0)
    {
        return ontModel.isInBaseModel(arg0);
    }

    @Override
    public boolean strictMode()
    {
        return ontModel.strictMode();
    }

    @Override
    public void setStrictMode(boolean arg0)
    {
        ontModel.setStrictMode(arg0);
    }

    @Override
    public void setDynamicImports(boolean arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public boolean getDynamicImports()
    {
        return ontModel.getDynamicImports();
    }

    @Override
    public OntDocumentManager getDocumentManager()
    {
        return ontModel.getDocumentManager();
    }

    @Override
    public OntModelSpec getSpecification()
    {
        return ontModel.getSpecification();
    }

    @Override
    public Model write(Writer arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0).getGraph()));
    }

    @Override
    public Model write(Writer arg0, String arg1)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0, arg1).getGraph()));
    }

    @Override
    public Model write(Writer arg0, String arg1, String arg2)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0, arg1, arg2).getGraph()));
    }

    @Override
    public Model write(OutputStream arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0).getGraph()));
    }

    @Override
    public Model write(OutputStream arg0, String arg1)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0, arg1).getGraph()));
    }

    @Override
    public Model write(OutputStream arg0, String arg1, String arg2)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.write(arg0, arg1, arg2).getGraph()));
    }

    @Override
    public Model writeAll(Writer arg0, String arg1, String arg2)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.writeAll(arg0, arg1, arg2).getGraph()));
    }

    @Override
    public Model writeAll(OutputStream arg0, String arg1, String arg2)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.writeAll(arg0, arg1, arg2).getGraph()));
    }

    @Override
    public Model writeAll(Writer arg0, String arg1)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.writeAll(arg0, arg1).getGraph()));
    }

    @Override
    public Model writeAll(OutputStream arg0, String arg1)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.writeAll(arg0, arg1).getGraph()));
    }

    @Override
    public Model getRawModel()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.getRawModel().getGraph()));
    }

    @Override
    public Reasoner getReasoner()
    {
        return ontModel.getReasoner();
    }

    @Override
    public void rebind()
    {
        throw new AddDeniedException();
    }

    @Override
    public void prepare()
    {
        ontModel.prepare();
    }

    @Override
    public void reset()
    {
        ontModel.reset();
    }

    @Override
    public ValidityReport validate()
    {
        return ontModel.validate();
    }

    @Override
    public StmtIterator listStatements(Resource arg0, Property arg1, RDFNode arg2, Model arg3)
    {
        return ontModel.listStatements(arg0, arg1, arg2, arg3);
    }

    @Override
    public void setDerivationLogging(boolean arg0)
    {
        ontModel.setDerivationLogging(arg0);
    }

    @Override
    public Iterator<Derivation> getDerivation(Statement arg0)
    {
        return ontModel.getDerivation(arg0);
    }

    @Override
    public Model getDeductionsModel()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.getDeductionsModel().getGraph()));
    }

    @Override
    public long size()
    {
        return ontModel.size();
    }

    @Override
    public boolean isEmpty()
    {
        return ontModel.isEmpty();
    }

    @Override
    public ResIterator listSubjects()
    {
        return ontModel.listSubjects();
    }

    @Override
    public NsIterator listNameSpaces()
    {
        return ontModel.listNameSpaces();
    }

    @Override
    public Resource getResource(String arg0)
    {
        return ontModel.getResource(arg0);
    }

    @Override
    public Property getProperty(String arg0, String arg1)
    {
        return ontModel.getProperty(arg0, arg1);
    }

    @Override
    public Resource createResource()
    {
        throw new AddDeniedException();
    }

    @Override
    public Resource createResource(AnonId arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Resource createResource(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Resource createResource(Statement arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Property createProperty(String arg0, String arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createLiteral(String arg0, String arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createLiteral(String arg0, boolean arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(String arg0, RDFDatatype arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(Object arg0, RDFDatatype arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(Object arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createStatement(Resource arg0, Property arg1, RDFNode arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public RDFList createList()
    {
        throw new AddDeniedException();
    }

    @Override
    public RDFList createList(Iterator<? extends RDFNode> arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public RDFList createList(RDFNode... arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Statement arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Statement[] arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model remove(Statement[] arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(List<Statement> arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model remove(List<Statement> arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(StmtIterator arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Model arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model read(String arg0)
    {
        return ontModel.read(arg0);
    }

    @Override
    public Model read(InputStream arg0, String arg1)
    {
        return ontModel.read(arg0, arg1);
    }

    @Override
    public Model read(InputStream arg0, String arg1, String arg2)
    {
        return ontModel.read(arg0, arg1, arg2);
    }

    @Override
    public Model read(Reader arg0, String arg1)
    {
        return ontModel.read(arg0, arg1);
    }

    @Override
    public Model read(String arg0, String arg1)
    {
        return ontModel.read(arg0, arg1);
    }

    @Override
    public Model read(Reader arg0, String arg1, String arg2)
    {
        return ontModel.read(arg0, arg1, arg2);
    }

    @Override
    public Model read(String arg0, String arg1, String arg2)
    {
        return ontModel.read(arg0, arg1, arg2);
    }

    @Override
    public Model remove(Statement arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement getRequiredProperty(Resource arg0, Property arg1)
    {
        return ontModel.getRequiredProperty(arg0, arg1);
    }

    @Override
    public Statement getRequiredProperty(Resource arg0, Property arg1, String arg2)
    {
        return ontModel.getRequiredProperty(arg0, arg1, arg2);
    }

    @Override
    public Statement getProperty(Resource arg0, Property arg1)
    {
        return ontModel.getProperty(arg0, arg1);
    }

    @Override
    public Statement getProperty(Resource arg0, Property arg1, String arg2)
    {
        return ontModel.getProperty(arg0, arg1, arg2);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property arg0)
    {
        return ontModel.listSubjectsWithProperty(arg0);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0)
    {
        return ontModel.listResourcesWithProperty(arg0);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property arg0, RDFNode arg1)
    {
        return ontModel.listSubjectsWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, RDFNode arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public NodeIterator listObjects()
    {
        return ontModel.listObjects();
    }

    @Override
    public NodeIterator listObjectsOfProperty(Property arg0)
    {
        return ontModel.listObjectsOfProperty(arg0);
    }

    @Override
    public NodeIterator listObjectsOfProperty(Resource arg0, Property arg1)
    {
        return ontModel.listObjectsOfProperty(arg0, arg1);
    }

    @Override
    public boolean contains(Resource arg0, Property arg1)
    {
        return ontModel.contains(arg0, arg1);
    }

    @Override
    public boolean containsResource(RDFNode arg0)
    {
        return ontModel.containsResource(arg0);
    }

    @Override
    public boolean contains(Resource arg0, Property arg1, RDFNode arg2)
    {
        return ontModel.contains(arg0, arg1, arg2);
    }

    @Override
    public boolean contains(Statement arg0)
    {
        return ontModel.contains(arg0);
    }

    @Override
    public boolean containsAny(StmtIterator arg0)
    {
        return ontModel.containsAny(arg0);
    }

    @Override
    public boolean containsAll(StmtIterator arg0)
    {
        return ontModel.containsAll(arg0);
    }

    @Override
    public boolean containsAny(Model arg0)
    {
        return ontModel.containsAny(arg0);
    }

    @Override
    public boolean containsAll(Model arg0)
    {
        return ontModel.containsAll(arg0);
    }

    @Override
    public boolean isReified(Statement arg0)
    {
        return ontModel.isReified(arg0);
    }

    @Override
    public Resource getAnyReifiedStatement(Statement arg0)
    {
        return ontModel.getAnyReifiedStatement(arg0);
    }

    @Override
    public void removeAllReifications(Statement arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public void removeReification(ReifiedStatement arg0)
    {
        ontModel.removeReification(arg0);
    }

    @Override
    public StmtIterator listStatements()
    {
        return ontModel.listStatements();
    }

    @Override
    public StmtIterator listStatements(Selector arg0)
    {
        return ontModel.listStatements(arg0);
    }

    @Override
    public StmtIterator listStatements(Resource arg0, Property arg1, RDFNode arg2)
    {
        return ontModel.listStatements(arg0, arg1, arg2);
    }

    @Override
    public ReifiedStatement createReifiedStatement(Statement arg0)
    {
        return ontModel.createReifiedStatement(arg0);
    }

    @Override
    public ReifiedStatement createReifiedStatement(String arg0, Statement arg1)
    {
        return ontModel.createReifiedStatement(arg0, arg1);
    }

    @Override
    public RSIterator listReifiedStatements()
    {
        return ontModel.listReifiedStatements();
    }

    @Override
    public RSIterator listReifiedStatements(Statement arg0)
    {
        return ontModel.listReifiedStatements(arg0);
    }

    @Override
    public Model query(Selector arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.query(arg0).getGraph()));
    }

    @Override
    public Model union(Model arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.union(arg0).getGraph()));
    }

    @Override
    public Model intersection(Model arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.intersection(arg0).getGraph()));
    }

    @Override
    public Model difference(Model arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.difference(arg0).getGraph()));
    }

    @Override
    public Model begin()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.begin().getGraph()));
    }

    @Override
    public Model abort()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.abort().getGraph()));
    }

    @Override
    public Model commit()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.commit().getGraph()));
    }

    @Override
    public void executeInTxn(Runnable arg0)
    {
        ontModel.executeInTxn(arg0);
    }

    @Override
    public <T> T calculateInTxn(Supplier<T> arg0)
    {
        return ontModel.calculateInTxn(arg0);
    }

    @Override
    public boolean independent()
    {
        return ontModel.independent();
    }

    @Override
    public boolean supportsTransactions()
    {
        return ontModel.supportsTransactions();
    }

    @Override
    public boolean supportsSetOperations()
    {
        return ontModel.supportsSetOperations();
    }

    @Override
    public boolean isIsomorphicWith(Model arg0)
    {
        return ontModel.isIsomorphicWith(arg0);
    }

    @Override
    public void close()
    {
        ontModel.close();
    }

    @Override
    public Lock getLock()
    {
        return ontModel.getLock();
    }

    @Override
    public Model register(ModelChangedListener arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.register(arg0).getGraph()));
    }

    @Override
    public Model unregister(ModelChangedListener arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.unregister(arg0).getGraph()));
    }

    @Override
    public Model notifyEvent(Object arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.notifyEvent(arg0).getGraph()));
    }

    @Override
    public Model removeAll()
    {
        throw new AddDeniedException();
    }

    @Override
    public Model removeAll(Resource arg0, Property arg1, RDFNode arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public boolean isClosed()
    {
        return ontModel.isClosed();
    }

    @Override
    public Model setNsPrefix(String arg0, String arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model removeNsPrefix(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model clearNsPrefixMap()
    {
        throw new AddDeniedException();
    }

    @Override
    public Model setNsPrefixes(PrefixMapping arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model setNsPrefixes(Map<String, String> arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model withDefaultMappings(PrefixMapping arg0)
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(ontModel.withDefaultMappings(arg0).getGraph()));
    }

    @Override
    public Resource getResource(String arg0, ResourceF arg1)
    {
        return ontModel.getResource(arg0, arg1);
    }

    @Override
    public Property getProperty(String arg0)
    {
        return ontModel.getProperty(arg0);
    }

    @Override
    public Bag getBag(String arg0)
    {
        return ontModel.getBag(arg0);
    }

    @Override
    public Bag getBag(Resource arg0)
    {
        return ontModel.getBag(arg0);
    }

    @Override
    public Alt getAlt(String arg0)
    {
        return ontModel.getAlt(arg0);
    }

    @Override
    public Alt getAlt(Resource arg0)
    {
        return ontModel.getAlt(arg0);
    }

    @Override
    public Seq getSeq(String arg0)
    {
        return ontModel.getSeq(arg0);
    }

    @Override
    public Seq getSeq(Resource arg0)
    {
        return ontModel.getSeq(arg0);
    }

    @Override
    public RDFList getList(String arg0)
    {
        return ontModel.getList(arg0);
    }

    @Override
    public RDFList getList(Resource arg0)
    {
        return ontModel.getList(arg0);
    }

    @Override
    public Resource createResource(Resource arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public RDFNode getRDFNode(Node arg0)
    {
        return ontModel.getRDFNode(arg0);
    }

    @Override
    public Resource createResource(String arg0, Resource arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Resource createResource(ResourceF arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Resource createResource(String arg0, ResourceF arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Property createProperty(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createLiteral(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(boolean arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(int arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(long arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(Calendar arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(char arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(float arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(double arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(String arg0, String arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Literal createTypedLiteral(Object arg0, String arg1)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, boolean arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, float arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, double arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, long arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, int arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, char arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createLiteralStatement(Resource arg0, Property arg1, Object arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createStatement(Resource arg0, Property arg1, String arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createStatement(Resource arg0, Property arg1, String arg2, String arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createStatement(Resource arg0, Property arg1, String arg2, boolean arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public Statement createStatement(Resource arg0, Property arg1, String arg2, String arg3, boolean arg4) 
    {
        throw new AddDeniedException();
    }

    @Override
    public Bag createBag()
    {
        throw new AddDeniedException();
    }

    @Override
    public Bag createBag(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Alt createAlt()
    {
        throw new AddDeniedException();
    }

    @Override
    public Alt createAlt(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Seq createSeq()
    {
        throw new AddDeniedException();
    }

    @Override
    public Seq createSeq(String arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Resource arg0, Property arg1, RDFNode arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, boolean arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, long arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, int arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, char arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, float arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, double arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, Object arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model addLiteral(Resource arg0, Property arg1, Literal arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Resource arg0, Property arg1, String arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Resource arg0, Property arg1, String arg2, RDFDatatype arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Resource arg0, Property arg1, String arg2, boolean arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model add(Resource arg0, Property arg1, String arg2, String arg3)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model remove(Resource arg0, Property arg1, RDFNode arg2)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model remove(StmtIterator arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public Model remove(Model arg0)
    {
        throw new AddDeniedException();
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, boolean arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, char arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, long arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, int arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, float arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource arg0, Property arg1, double arg2)
    {
        return ontModel.listLiteralStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listStatements(Resource arg0, Property arg1, String arg2)
    {
        return ontModel.listStatements(arg0, arg1, arg2);
    }

    @Override
    public StmtIterator listStatements(Resource arg0, Property arg1, String arg2, String arg3)
    {
        return ontModel.listStatements(arg0, arg1, arg2, arg3);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, boolean arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, long arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, char arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, float arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, double arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property arg0, Object arg1)
    {
        return ontModel.listResourcesWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property arg0, String arg1)
    {
        return ontModel.listSubjectsWithProperty(arg0, arg1);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property arg0, String arg1, String arg2)
    {
        return ontModel.listSubjectsWithProperty(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, boolean arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, long arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, int arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, char arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, float arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, double arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean containsLiteral(Resource arg0, Property arg1, Object arg2)
    {
        return ontModel.containsLiteral(arg0, arg1, arg2);
    }

    @Override
    public boolean contains(Resource arg0, Property arg1, String arg2)
    {
        return ontModel.contains(arg0, arg1, arg2);
    }

    @Override
    public boolean contains(Resource arg0, Property arg1, String arg2, String arg3)
    {
        return ontModel.contains(arg0, arg1, arg2, arg3);
    }

    @Override
    public Statement asStatement(Triple arg0)
    {
        return ontModel.asStatement(arg0);
    }

    @Override
    public Graph getGraph()
    {
        return new GraphReadOnly(ontModel.getGraph());
    }

    @Override
    public RDFNode asRDFNode(Node arg0)
    {
        return ontModel.asRDFNode(arg0);
    }

    @Override
    public Resource wrapAsResource(Node arg0)
    {
        return ontModel.wrapAsResource(arg0);
    }

    @Override
    public RDFReaderI getReader()
    {
        return ontModel.getReader();
    }

    @Override
    public RDFReaderI getReader(String arg0)
    {
        return ontModel.getReader(arg0);
    }

    @Override
    public RDFWriterI getWriter()
    {
        return ontModel.getWriter();
    }

    @Override
    public RDFWriterI getWriter(String arg0)
    {
        return ontModel.getWriter(arg0);
    }

    @Override
    public String getNsPrefixURI(String arg0)
    {
        return ontModel.getNsPrefixURI(arg0);
    }

    @Override
    public String getNsURIPrefix(String arg0)
    {
        return ontModel.getNsPrefixURI(arg0);
    }

    @Override
    public Map<String, String> getNsPrefixMap()
    {
        return ontModel.getNsPrefixMap();
    }

    @Override
    public String expandPrefix(String arg0)
    {
        return ontModel.expandPrefix(arg0);
    }

    @Override
    public String shortForm(String arg0)
    {
        return ontModel.shortForm(arg0);
    }

    @Override
    public String qnameFor(String arg0)
    {
        return ontModel.qnameFor(arg0);
    }

    @Override
    public PrefixMapping lock()
    {
        return ontModel.lock();
    }

    @Override
    public int numPrefixes()
    {
        return ontModel.numPrefixes();
    }

    @Override
    public boolean samePrefixMappingAs(PrefixMapping arg0)
    {
        return ontModel.samePrefixMappingAs(arg0);
    }

    @Override
    public void enterCriticalSection(boolean arg0)
    {
        ontModel.enterCriticalSection(arg0);
    }

    @Override
    public void leaveCriticalSection()
    {
        ontModel.leaveCriticalSection();
    }

    @Override
    public boolean hasNoMappings()
    {
        return ontModel.hasNoMappings();
    }
}
