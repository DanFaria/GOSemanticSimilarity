/******************************************************************************
 * The Gene Ontology object, loaded from either the OWL or OBO release, using  *
 * the OWL API.                                                                *
 *                                                                             *
 * @author Daniel Faria, Beatriz Lima                                          *
 ******************************************************************************/

package ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import main.Main;
import util.Table2Set;
import util.Table3List;

public class GeneOntology
{

	//Attributes

	//The OWL Ontology Manager and Data Factory
	private OWLOntologyManager manager;
	//The entity expansion limit property
	private final String LIMIT = "entityExpansionLimit";

	//The uris
	protected HashSet<String> uriClasses;

	//The local name <-> uri 
	protected HashMap<String,String> nameClasses;
	protected HashMap<String,String> classNames;
	//The uri <-> label map of ontology classes
	protected HashMap<String,String> classLabels;
	protected HashMap<String,String> labelClasses;

	//The uri -> ontology object properties
	protected HashSet<String> uriProperties;
	protected HashMap<String, String> propertyNames;

	//Map of object properties that are transitive over other object properties
	//(including themselves)
	protected Table2Set<String, String> transitiveOver;

	//Map between ancestor classes and their descendants (with transitive closure)
	private Table3List<String,String,Relationship> descendantMap;
	//Map between descendant classes and their ancestors (with transitive closure)
	private Table3List<String,String,Relationship> ancestorMap;

	//The map of uri -> GOType indexes in the ontology
	private HashMap<String,GOType> termTypes;
	//The map of GOType -> root uri in the ontology
	private HashMap<GOType,String> rootTerms;

	private HashSet<String> deprecated;
	private HashMap<String,String> alternatives;

	//The annotation map of gene accs <-> GO terms
	private Table2Set<String,String> geneTerms;
	private Table2Set<String,String> termGenes;

	//The map of gene synonyms (for GAF file)
	private HashMap<String,String> geneSynonyms;

	private boolean useAllRelations;
	private boolean structural;

	//The map of uri -> IC in the ontology
	private HashMap<String,Double> termICs;


	//Constructors

	/**
	 * Constructs an empty ontology
	 */
	private GeneOntology()
	{
		//Initialize the data structures
		uriClasses = new HashSet<String>();
		nameClasses = new HashMap<String,String>();
		classNames = new HashMap<String,String>();
		classLabels = new HashMap<String,String>();
		labelClasses = new HashMap<String,String>();
		uriProperties = new HashSet<String>();
		propertyNames = new HashMap<String,String>();
		transitiveOver = new Table2Set<String,String>();
		descendantMap = new Table3List<String,String,Relationship>();
		ancestorMap = new Table3List<String,String,Relationship>();
		termTypes = new HashMap<String,GOType>();
		rootTerms = new HashMap<GOType,String>();
		deprecated = new HashSet<String>();
		alternatives = new HashMap<String,String>();
		geneTerms = new Table2Set<String,String>();
		termGenes = new Table2Set<String,String>();
		geneSynonyms = new HashMap<String,String>();
		termICs = new HashMap<String,Double>();

		//Increase the entity expansion limit to allow large ontologies
		System.setProperty(LIMIT, "1000000");
		//Get an Ontology Manager
		manager = OWLManager.createOWLOntologyManager();
	}

	/**
	 * Constructs; an Ontology from file 
	 * @param path: the path to the input Ontology file
	 * @param annotFile: gene product annotation file
	 * @throws OWLOntologyCreationException 
	 */
	public GeneOntology(String path, String annotFile, boolean useAllRelations, boolean structural) throws OWLOntologyCreationException, IOException
	{
		this((new File(path)).toURI(), annotFile, useAllRelations, structural);
	}

	/**
	 * Constructs an Ontology from an URI  
	 * @param uri: the URI of the input Ontology
	 * @param annotFile: gene product annotation file
	 * @throws OWLOntologyCreationException 
	 */
	public GeneOntology(URI uri, String annotFile, boolean useAllRelations, boolean structural) throws OWLOntologyCreationException, IOException
	{
		this();
		OWLOntology o;
		//Check if the URI is local
		if(uri.toString().startsWith("file:"))
		{
			File f = new File(uri);
			o = manager.loadOntologyFromOntologyDocument(f);
		}
		else
		{
			IRI i = IRI.create(uri);
			o = manager.loadOntology(i);
		}
		init(o);
		//Close the OntModel
		manager.removeOntology(o);
		//Reset the entity expansion limit
		System.clearProperty(LIMIT);
		this.useAllRelations = useAllRelations;
		this.structural = structural;
		readAnnotationFile(annotFile);
		extendAnnotations();
	}


	//Public Methods

	/**
	 * @param gene: identifier of the gene product
	 * @return whether the gene product is listed in the annotation or synonym
	 * tables
	 */
	public boolean contains(String gene)
	{
		return geneTerms.contains(gene) || geneSynonyms.containsKey(gene);
	}

	/**
	 * @param name: the local name of the class in the ontology
	 * @return whether the class belongs to the ontology
	 */
	public boolean containsName(String name)
	{
		return nameClasses.containsKey(name) || alternatives.containsKey(name);
	}

	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return whether the ontology contains a relationship between child and parent
	 */
	public boolean containsRelationship(String child, String parent)
	{
		return descendantMap.contains(parent,child);
	}

	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return whether the ontology contains an 'is_a' relationship between child and parent
	 */	
	public boolean containsSubClass(String child, String parent)
	{
		if(!descendantMap.contains(parent,child))
			return false;
		Vector<Relationship> rels = descendantMap.get(parent,child);
		for(Relationship r : rels)
			if(r.getProperty() == null)
				return true;
		return false;
	}

	public int countAnnotations(String go)
	{
		if(termGenes.contains(go))
			return termGenes.get(go).size();
		else
			return 0;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of ancestors of the given class
	 */
	public Set<String> getAncestors(String uri)
	{
		if(ancestorMap.contains(uri))
			return ancestorMap.keySet(uri);
		else
			return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<String> getAncestors(String uri, int distance)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorMap.contains(uri))
			return asc;
		for(String i : ancestorMap.keySet(uri))
			for(Relationship r : ancestorMap.get(uri, i))
				if(r.getDistance() == distance)
					asc.add(i);
		return asc;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors of the input class that are at the given
	 * distance and with the given property
	 */
	public Set<String> getAncestors(String uri, int distance, String prop)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorMap.contains(uri))
			return asc;
		for(String i : ancestorMap.keySet(uri))
			for(Relationship r : ancestorMap.get(uri, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					asc.add(i);
		return asc;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<String> getAncestorsProperty(String uri, String prop)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorMap.contains(uri))
			return asc;
		for(String i : ancestorMap.keySet(uri))
			for(Relationship r : ancestorMap.get(uri, i))
				if(r.getProperty() == prop)
					asc.add(i);
		return asc;
	}

	/**
	 * @param go: the uri of the GO term for which to retrieve annotations
	 * @return the set of gene products annotated with the given GO term
	 */
	public Set<String> getAnnotationsGO(String go)
	{
		if(termGenes.contains(go))
			return new HashSet<String>(termGenes.get(go));
		else
			return new HashSet<String>();		
	}

	/**
	 * @param gene: the identifier of the gene product for which to retrieve annotations
	 * @return the set of GO terms annotated to the gene product
	 */
	public Set<String> getAnnotationsGene(String gene)
	{
		if(geneTerms.contains(gene))
			return new HashSet<String>(geneTerms.get(gene));
		else if(geneSynonyms.containsKey(gene) && geneTerms.contains(geneSynonyms.get(gene)))
			return new HashSet<String>(geneTerms.get(geneSynonyms.get(gene)));
		else
			return new HashSet<String>();		
	}

	/**
	 * @param gene: the identifier of the gene product for which to retrieve annotations
	 * @param t: the GO type of terms to return
	 * @return the set of GO terms of the given type annotated to the gene product
	 */
	public Set<String> getAnnotationsGene(String gene, GOType t)
	{
		Set<String> results;
		if(geneTerms.contains(gene))
			results = geneTerms.get(gene);
		else if(geneSynonyms.containsKey(gene) && geneTerms.contains(geneSynonyms.get(gene)))
			results = geneTerms.get(geneSynonyms.get(gene));
		else
			results = new HashSet<String>();
		HashSet<String> finalResults = new HashSet<String>();
		for(String s : results)
			if(termTypes.get(s).equals(t))
				finalResults.add(s);
		return finalResults;
	}

	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<String> getChildren()
	{
		if(ancestorMap != null)
			return ancestorMap.keySet();
		return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of direct children of the given class
	 */
	public Set<String> getChildren(String uri)
	{
		return getDescendants(uri,1);
	}


	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of combined ancestors of the input classes
	 */
	public HashSet<String> getCombinedAncestors(String uri1,String uri2)
	{
		if(useAllRelations)
		{
			HashSet<String> commonAncestors = new HashSet<String>(getAncestors(uri1));
			commonAncestors.addAll(getAncestors(uri2));
			return commonAncestors;
		}
		else
		{
			HashSet<String> commonAncestors = new HashSet<String>(getSuperClasses(uri1,false));
			commonAncestors.addAll(getSuperClasses(uri2,false));
			return commonAncestors;
		}
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of common ancestors of the input classes
	 */
	public HashSet<String> getCommonAncestors(String uri1,String uri2)
	{
		if(useAllRelations)
		{
			HashSet<String> commonAncestors = new HashSet<String>(getAncestors(uri1));
			commonAncestors.retainAll(getAncestors(uri2));
			return commonAncestors;
		}
		else
		{
			HashSet<String> commonAncestors = new HashSet<String>(getSuperClasses(uri1,false));
			commonAncestors.retainAll(getSuperClasses(uri2,false));
			return commonAncestors;
		}
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of descendants of the input class
	 */
	public Set<String> getDescendants(String uri)
	{
		if(descendantMap.contains(uri))
			return descendantMap.keySet(uri);
		return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<String> getDescendants(String uri, int distance)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantMap.contains(uri))
			return desc;
		for(String i : descendantMap.keySet(uri))
			for(Relationship r : descendantMap.get(uri, i))
				if(r.getDistance() == distance)
					desc.add(i);
		return desc;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants of the input class at the given distance
	 * and with the given property
	 */
	public Set<String> getDescendants(String uri, int distance, String prop)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantMap.contains(uri))	
			return desc;
		for(String i : descendantMap.keySet(uri))
			for(Relationship r : descendantMap.get(uri, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					desc.add(i);
		return desc;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<String> getDescendantsProperty(String uri, String prop)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantMap.contains(uri))
			return desc;
		for(String i : descendantMap.keySet(uri))
			for(Relationship r : descendantMap.get(uri, i))
				if(r.getProperty() == prop)
					desc.add(i);
		return desc;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of equivalences of the given class
	 */
	public Set<String> getEquivalences(String uri)
	{
		return getDescendants(uri, 0);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of classes equivalent to the given class
	 */
	public Set<String> getEquivalentClasses(String uri)
	{
		return getDescendants(uri,0,null);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the information content of the given uri
	 */
	public double getInfoContent(String uri)
	{
		if (uri == null)
			return 0.0;

		if(termICs.containsKey(uri))
			return termICs.get(uri);

		double score = 0.0;
		String root = rootTerms.get(getType(uri));

		if(root.equals(uri))
		{
			termICs.put(uri, score);
			return score;
		}

		if(structural)
		{
			if (useAllRelations)
			{	
				score = 1-Math.log(1+getDescendants(uri).size())/
						Math.log(1+getDescendants(root).size());
			}
			else
			{
				score = 1-Math.log(1+getSubClasses(uri,false).size())/
						Math.log(1+getSubClasses(rootTerms.get(getType(uri)),false).size());
			}			
		}
		else
		{
			score = 1-Math.log(Math.max(1, countAnnotations(uri)))/
					Math.log(Math.max(1, countAnnotations(rootTerms.get(getType(uri)))));
		}
		termICs.put(uri, score);
		return score;
	}


	/**
	 * @param uri: the URI of the input Ontology
	 * @return the label of the class with the given uri
	 */
	public String getLabel(String uri)
	{
		return classLabels.get(uri);
	}



	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return the max distance between the child and parent,
	 * or 0 if child equals parent, or -1 if they aren't related
	 */
	public int getMaxDistance(String child, String parent)
	{
		if(child.equals(parent))
			return 0;
		if(!ancestorMap.contains(child, parent) || (!useAllRelations && !containsSubClass(child,parent)))
			return -1;

		Vector<Relationship> rels = ancestorMap.get(child,parent);
		int distance = rels.get(0).getDistance();
		for(Relationship r : rels)
			if((useAllRelations || r.getProperty()==null) || r.getDistance() > distance)
				distance = r.getDistance();
		return distance;
	}


	//Computes the most common ancestor of two given clases
	public String getMICA(String uri1, String uri2)
	{
		//If one term is the other's immediate ancestor, it is also the MICA
		if (useAllRelations)
		{
			if (getChildren(uri1).contains(uri2))
				return uri1;
			if (getChildren(uri2).contains(uri1))
				return uri2;
		}
		else
		{
			if (containsSubClass(uri2, uri1))
				return uri1;
			if (containsSubClass(uri1, uri2))
				return uri2;
		}

		String mica = null;
		int min = 1000000;

		if (!getCommonAncestors(uri1,uri2).isEmpty())
		{
			for (String a: getCommonAncestors(uri1,uri2))
			{
				int newCount = countAnnotations(a);
				if(newCount < min)
				{
					min = newCount;
					mica = a;
				}
			}
			return mica;
		}

		else return null;
	}

	/**
	 * @param gene: the identifier of the gene product 
	 * @return non redundant terms: none of the set terms is another one's ancestor 
	 */
	public Set<String> getNonRedundantTerms(String gene)
	{
		Vector<String> termVector = new Vector<String>(geneTerms.get(gene));

		for(int i= 0; i < termVector.size()-1; i++)
		{
			for(int j = i+1; j < termVector.size(); j++)
			{
				if((useAllRelations && containsRelationship(termVector.get(j), termVector.get(i)))
						|| containsSubClass(termVector.get(j), termVector.get(i)))
				{
					termVector.remove(i--);
					j--;
					break;
				}				
				if((useAllRelations && containsRelationship(termVector.get(i), termVector.get(j)))
						|| containsSubClass(termVector.get(i), termVector.get(j)))
				{
					termVector.remove(j--);
				}
			}
		}
		return new HashSet<String>(termVector);
	}
	
	/**
	 * @param gene: the identifier of the gene product 
	 * @return non redundant terms: none of the set terms is another one's ancestor 
	 */
	public Set<String> getNonRedundantTerms(Set<String> terms)
	{
		Vector<String> termVector = new Vector<String>(terms);

		for(int i= 0; i < termVector.size()-1; i++)
		{
			for(int j = i+1; j < termVector.size(); j++)
			{
				if((useAllRelations && containsRelationship(termVector.get(j), termVector.get(i)))
						|| containsSubClass(termVector.get(j), termVector.get(i)))
				{
					termVector.remove(i--);
					j--;
					break;
				}				
				if((useAllRelations && containsRelationship(termVector.get(i), termVector.get(j)))
						|| containsSubClass(termVector.get(i), termVector.get(j)))
				{
					termVector.remove(j--);
				}
			}
		}
		return new HashSet<String>(termVector);
	}

	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<String> getParents()
	{
		if(descendantMap != null)
			return descendantMap.keySet();
		return new HashSet<String>();
	}

	/**
	 * @param class: the id of the class to search in the map
	 * @return the list of direct parents of the given class
	 */
	public Set<String> getParents(String uri)
	{
		return getAncestors(uri,1);
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the 'best' relationship between the two classes
	 */
	public Relationship getRelationship(String child, String parent)
	{
		if(!ancestorMap.contains(child, parent))
			return null;
		Relationship rel = ancestorMap.get(child).get(parent).get(0);
		for(Relationship r : ancestorMap.get(child).get(parent))
			if(r.compareTo(rel) > 0)
				rel = r;
		return rel;
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the relationships between the two classes
	 */
	public Vector<Relationship> getRelationships(String child, String parent)
	{
		return ancestorMap.get(child).get(parent);
	}

	/**
	 * @param t: the GOType to search
	 * @return the Ontology uri of the root term for that GOType
	 */
	public String getRoot(GOType t)
	{
		return rootTerms.get(t);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return just the direct subclasses or all subclasses
	 * @return the list of direct or indirect subclasses of the input class
	 */
	public Set<String> getSubClasses(String uri, boolean direct)
	{
		if(direct)
			return getDescendants(uri,1,null);
		else
			return getDescendantsProperty(uri,null);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return just the direct superclasses or all superclasses
	 * @return the list of direct or indirect superclasses of the input class
	 */
	public Set<String> getSuperClasses(String uri, boolean direct)
	{
		if(direct)
			return getAncestors(uri,1,null);
		else
			return getAncestorsProperty(uri,null);
	}

	/**
	 * @param uri: the uri of the GO term to get
	 * @return the GOType of the GO term wiriNameth the given index
	 */
	public GOType getType(String uri)
	{
		return termTypes.get(uri);
	}

	/**
	 * @param name: the local name of the input Ontology class
	 * @return the uri of the class with the given local name
	 */
	public String getURI(String name)
	{
		if(nameClasses.containsKey(name))
			return nameClasses.get(name);
		else if(alternatives.containsKey(name))
			return nameClasses.get(alternatives.get(name));
		else
			return null;
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @param property: the id of the property between child and parent
	 * @return whether there is a relationship between child and parent
	 *  with the given property
	 */
	public boolean hasProperty(String child, String parent, String property)
	{
		Vector<Relationship> rels = getRelationships(child,parent);
		for(Relationship r : rels)
			if(r.getProperty() == property)
				return true;
		return false;
	}

	/**
	 * @return the number of relationships in the map
	 */
	public int relationshipCount()
	{
		return ancestorMap.size();
	}

	/**
	 * @return the number of annotations in the AnnotationSet
	 */
	public int size()
	{
		return geneTerms.size();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the number of direct or indirect subclasses of the input class
	 */
	public int subClassCount(String uri, boolean direct)
	{
		return getSubClasses(uri,direct).size();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the number of direct or indirect superclasses of the input class
	 */
	public int superClassCount(String uri, boolean direct)
	{
		return getSuperClasses(uri,direct).size();
	}	

	//Private Methods	

	private void addRelationship(OWLClass c, OWLClassExpression e, boolean sub)
	{
		String child = c.getIRI().toString();
		String parent = null;
		int distance = (sub) ? 1 : 0;
		String prop = null;
		ClassExpressionType type = e.getClassExpressionType();
		//If it is a class, process it here
		if(type.equals(ClassExpressionType.OWL_CLASS))
		{
			parent = e.asOWLClass().getIRI().toString();
			if(!uriClasses.contains(parent))
				return;
		}
		//If it is a 'some values' object property restriction, process it
		else if(type.equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM) ||
				type.equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			prop = p.getIRI().toString();
			if(!uriProperties.contains(prop))
				return;
			Set<OWLClass> sup = e.getClassesInSignature();
			if(sup == null || sup.size() != 1)
				return;					
			OWLClass cls = sup.iterator().next();
			parent = cls.getIRI().toString();
			if(!uriClasses.contains(parent))
				return;
		}
		//If it is an intersection of classes, capture the implied subclass relationships
		else if(type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
		{
			Set<OWLClassExpression> inter = e.asConjunctSet();
			for(OWLClassExpression cls : inter)
				addRelationship(c,cls,true);
		}
		if(parent == null)
			return;
		Relationship r = new Relationship(distance,prop);
		descendantMap.add(parent,child,r);
		ancestorMap.add(child,parent,r);
	}

	//Extends the AnnotationSet for transitive closure
	private void extendAnnotations()
	{
		//We must store the new annotations in a temporary table in order
		//to avoid concurrent modifications
		Table2Set<String,String> tempAnnotations = new Table2Set<String,String>();
		for(String gene : geneTerms.keySet())
		{		
			for(String go : geneTerms.get(gene))
			{
				if(useAllRelations)
				{
					if( go!= null)
					{
						for(String ancestor : getAncestors(go))
							tempAnnotations.add(gene, ancestor);
					}
				}
				else
				{
					for(String ancestor : getSuperClasses(go, false))
						tempAnnotations.add(gene, ancestor);
				}
			}
		}
		//Once we have all the new annotations, we can add them to the
		//AnnotationSet tables
		for(String gene : tempAnnotations.keySet())
		{
			for(String go : tempAnnotations.get(gene))
			{
				geneTerms.add(gene,go);
				termGenes.add(go,gene);
			}
		}
	}

	//Builds the ontology data structures
	private void init(OWLOntology o)
	{
		//Get the classes and their names and synonyms
		getClasses(o);
		//Get the properties
		getProperties(o);
		//Build the relationship map
		getRelationships(o);
		//Extend the relationship map
		transitiveClosure();
	}

	//Processes the classes, their lexical information and cross-references
	private void getClasses(OWLOntology o)
	{
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		for(OWLClass c : classes)
		{
			//Then get the URI for each class
			String classUri = c.getIRI().toString();
			if(classUri == null || classUri.endsWith("owl#Thing") || classUri.endsWith("owl:Thing"))
				continue;
			//Check if the class is deprecated
			boolean dep = false;
			for(OWLAnnotation a : c.getAnnotations(o))
			{
				if((a.getProperty().toString().equals("owl:deprecated") || a.getProperty().toString().equals("owl:is_obsolete")) && ((OWLLiteral)a.getValue()).parseBoolean())
				{
					dep = true;
					break;
				}
			}
			//If it is deprecated, record it and skip it
			if(dep)
			{
				deprecated.add(getLocalName(classUri).replace('_', ':'));
				continue;
			}

			uriClasses.add(classUri);

			//Get the local name from the URI
			String name = getLocalName(classUri).replace('_', ':');

			classNames.put(classUri,name);
			nameClasses.put(name,classUri);


			//Now get the class' label and type
			Set<OWLAnnotation> annots = c.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(c.getAnnotations(ont));
			for(OWLAnnotation annotation : annots)
			{
				//Label
				if(annotation.getProperty().toString().equals("rdfs:label") && annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String lab = val.getLiteral();
					classLabels.put(classUri,lab);
					labelClasses.put(lab, classUri);
					//If the label is a GOType, then the term is a root
					GOType t = GOType.parse(lab);
					if(t != null)
						rootTerms.put(t,classUri);
				}
				//Type
				else if(annotation.getProperty().toString().contains("hasOBONamespace") && annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String type = val.getLiteral();
					GOType t = GOType.parse(type);
					if(t != null)
						termTypes.put(classUri, t);
				}
				//Alternative
				if(annotation.getProperty().toString().contains("hasAlternativeId") && annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String alt = val.getLiteral();
					alternatives.put(alt, name);
				}
			}
		}
	}

	//Reads the object properties
	private void getProperties(OWLOntology o)
	{
		//Get the Object Properties
		Set<OWLObjectProperty> oProps = o.getObjectPropertiesInSignature(true);
		for(OWLObjectProperty op : oProps)
		{
			//Get the URI of each property
			String propUri = op.getIRI().toString();
			if(propUri == null || propUri.equals("http://purl.obolibrary.org/obo/BFO_0000051"))
				continue;
			uriProperties.add(propUri);
			//Get its label
			Set<OWLAnnotation> annots = op.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(op.getAnnotations(ont));
			for(OWLAnnotation annotation : annots)
			{
				//Label
				if(annotation.getProperty().toString().equals("rdfs:label") && annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String lab = val.getLiteral();
					propertyNames.put(propUri,lab);
					break;
				}
			}
			//If it is transitive, add it to the transitiveOver map
			//(as transitive over itself)
			if(op.isTransitive(o))
				transitiveOver.add(propUri,propUri);
		}
		//Process transitive_over relations (this needs to be done
		//in a 2nd loop as all properties must already be indexed)
		for(OWLObjectProperty op : oProps)
		{
			//Transitive over relations go to the transitiveOver map
			for(OWLAxiom e : op.getReferencingAxioms(o))
			{
				//In OWL, the OBO transitive_over relation is encoded as a sub-property chain of
				//the form: "SubObjectPropertyOf(ObjectPropertyChain( <p1> <p2> ) <this_p> )"
				//in which 'this_p' is usually 'p1' but can also be 'p2' (in case you want to
				//define that another property is transitive over this one, which may happen when
				//the other property is imported and this property occurs only in this ontology)
				if(!e.isOfType(AxiomType.SUB_PROPERTY_CHAIN_OF))
					continue;
				//Unfortunately, there isn't much support for "ObjectPropertyChain"s in the OWL
				//API, so the only way to get the referenced properties while preserving their
				//order is to parse the String representation of the sub-property chain
				//(getObjectPropertiesInSignature() does NOT preserve the order)
				String[] chain = e.toString().split("[\\(\\)]");
				//Make sure the structure of the sub-property chain is in the expected format
				if(!chain[0].equals("SubObjectPropertyOf") || !chain[1].equals("ObjectPropertyChain"))
					continue;
				//Get the indexes of the tags surrounding the URIs
				int index1 = chain[2].indexOf("<")+1;
				int index2 = chain[2].indexOf(">");
				int index3 = chain[2].lastIndexOf("<")+1;
				int index4 = chain[2].lastIndexOf(">");
				//Make sure the indexes check up
				if(index1 < 0 || index2 <= index1 || index3 <= index2 || index4 <= index3)
					continue;
				String uri1 = chain[2].substring(index1,index2);
				String uri2 = chain[2].substring(index3,index4);
				//Make sure the URIs are listed object properties
				if(!uriProperties.contains(uri1) || !uriProperties.contains(uri2))
					continue;
				//If everything checks up, add the relation to the transitiveOver map
				transitiveOver.add(uri1, uri2);
			}
		}
	}

	//Reads all class relationships
	private void getRelationships(OWLOntology o)
	{
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		//For each term index (from 'termURIs' list)
		for(OWLClass c : classes)
		{
			if(!uriClasses.contains(c.getIRI().toString()))
				continue;
			//Get the subclass expressions to capture and add relationships
			Set<OWLClassExpression> superClasses = c.getSuperClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				superClasses.addAll(c.getSuperClasses(ont));
			for(OWLClassExpression e : superClasses)
				addRelationship(c,e,true);

			//Get the equivalence expressions to capture and add relationships
			Set<OWLClassExpression> equivClasses = c.getEquivalentClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				equivClasses.addAll(c.getEquivalentClasses(ont));
			for(OWLClassExpression e : equivClasses)
				addRelationship(c,e,false);
		}

	}

	//Get the local name of an entity from its URI
	private String getLocalName(String uri)
	{
		int index = uri.indexOf("#") + 1;
		if(index == 0)
			index = uri.lastIndexOf("/") + 1;
		return uri.substring(index);
	}


	//Reads the set of annotations listed in an input file
	private void readAnnotationFile(String annotFile) throws IOException
	{
		//Open the input file or die
		BufferedReader in = new BufferedReader(new FileReader(annotFile));
		String line = in.readLine();
		//Detect the annotation file format
		AnnotationFileFormat f;
		if(line.startsWith("!"))
		{
			//A GO annotation file should start with a commented section
			//with '!' being the comment sign
			f = AnnotationFileFormat.GAF;
			while(line != null && line.startsWith("!"))
				line = in.readLine();
		}
		else if(line.startsWith("("))
		{
			//A BINGO file should start with an info line which contains
			//information within parenthesis
			f = AnnotationFileFormat.BINGO;
			while(line != null && line.startsWith("("))
				line = in.readLine();
		}
		else
		{
			//Otherwise, we assume we have a tabular file, which may or may
			//not include header information, so we skip lines until a GO
			//term is found
			f = AnnotationFileFormat.TABULAR;
			while(line != null && !line.contains("GO:"))
				line = in.readLine();
		}
		while(line != null)
		{
			String[] values;
			String gene = null, go = null, geneSyn = null;
			if(f.equals(AnnotationFileFormat.BINGO))
			{
				values = line.split(" = ");
				gene = values[0];
				go = ("GO:" + values[1]);
			}
			else
			{
				values = line.split("\t");
				if(f.equals(AnnotationFileFormat.GAF))
				{
					if(values[3].equalsIgnoreCase("NOT"))
					{
						line = in.readLine();
						continue;
					}
					gene = values[1];
					geneSyn = values[2];
					go = values[4];					
				}
				else
				{
					gene = values[0].trim();
					for(int i = 1; i < values.length; i++)
					{
						if(values[i].trim().startsWith("GO:"))
						{
							go = values[i].trim();
							break;
						}
					}
				}
			}
			line = in.readLine();
			if(!containsName(go))
				continue;

			String uri = getURI(go);
			if(uri != null)
			{
				geneTerms.add(gene,uri);
				termGenes.add(uri, gene);
			}

			if(geneSyn != null)
				geneSynonyms.put(geneSyn, gene);
		}
		in.close();
	}


	/**
	 * Compute the transitive closure of the RelationshipMap
	 * by adding inherited relationships (and their distances)
	 * This is an implementation of the Semi-Naive Algorithm
	 */
	public void transitiveClosure()
	{
		Set<String> t = descendantMap.keySet();
		int lastCount = 0;
		for(int distance = 1; lastCount != descendantMap.size(); distance++)
		{
			lastCount = descendantMap.size();
			for(String i : t)
			{
				Set<String> childs = getChildren(i);
				childs.addAll(getEquivalences(i));
				Set<String> pars = getAncestors(i,distance);
				for(String j : pars)
				{
					Vector<Relationship> rel1 = getRelationships(i,j);
					for(int k = 0; k < rel1.size(); k++)
					{
						Relationship r1 = rel1.get(k);
						String p1 = r1.getProperty();
						for(String h : childs)
						{
							Vector<Relationship> rel2 = getRelationships(h,i);
							for(int l = 0; l < rel2.size(); l++)
							{
								Relationship r2 = rel2.get(l);
								String p2 = r2.getProperty();
								//We only do transitive closure if at least one of the properties
								//is 'is_a' (-1) or the child property is transitive over the parent
								//(which covers the case where they are both the same transitive
								//property)
								if(!(p1 == null || p2 == null || transitiveOver.contains(p2,p1)))
									continue;
								int dist = r1.getDistance() + r2.getDistance();
								//The child property wins in most cases: if p2 = p1,
								//if p2 transitive_over p1, and otherwise if p1 = is_a
								String prop = p2;
								//The parent property only wins if p2 = is_a and p1 != is_a
								if(p2 == null || p1 != null)
									prop = p1;
								Relationship r = new Relationship(dist,prop);	
								descendantMap.add(j,h,r);
								ancestorMap.add(h,j,r);
							}
						}
					}
				}
			}
		}
		for(String uri : classNames.keySet())
		{
			Relationship r = new Relationship(0,null);	
			descendantMap.add(uri,uri,r);
			ancestorMap.add(uri,uri,r);
		}
	}

	public HashMap<String,Integer> termCountMap (Vector<String> geneSet, GOType type)
	{
		HashMap<String, Integer> count = new HashMap<String, Integer>();

		if(type == null)
		{
			for(String g: geneSet)
			{
				for (String t: Main.getGO().getAnnotationsGene(g))
				{
					if (count.containsKey(t))
						count.put(t, count.get(t)+1);
					else count.put(t,1);
				}
			}
		}
		else
		{
			for(String g: geneSet)
			{
				for (String t: Main.getGO().getAnnotationsGene(g,type))
				{
					if (count.containsKey(t))
						count.put(t, count.get(t)+1);
					else count.put(t,1);
				}
			}
		}
		return count;
	}

	public Set<String> termSet(Vector<String> geneSet, GOType type)
	{
		Set<String> terms = new HashSet<String>();

		if(type == null)
		{
			for(String g: geneSet)
			{
				for (String t: Main.getGO().getAnnotationsGene(g))
					terms.add(t);
			}
		}
		else
		{
			for(String g: geneSet)
			{
				for (String t: Main.getGO().getAnnotationsGene(g,type))
					terms.add(t);
			}
		}
		return terms;
	}
}