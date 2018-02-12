/**
 * 
 */
package noctua.exchange;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * @author bgood
 *
 */
public class GoCAM {
	public static final IRI go_lego_iri = IRI.create("http://purl.obolibrary.org/obo/go/extensions/go-lego.owl");
	public static final IRI obo_iri = IRI.create("http://purl.obolibrary.org/obo/");
	public static final IRI uniprot_iri = IRI.create("http://identifiers.org/uniprot/");
	public static final IRI biopax_iri = IRI.create("http://www.biopax.org/release/biopax-level3.owl#");
	public static OWLAnnotationProperty title_prop, contributor_prop, date_prop, state_prop, evidence_prop, provided_by_prop, x_prop, y_prop, rdfs_label;
	public static OWLObjectProperty part_of, has_part, has_input, has_output, 
		provides_direct_input_for, directly_inhibits, directly_activates, occurs_in, enabled_by, enables, regulated_by, located_in;
	public static OWLClass bp_class, continuant_class, protein_class, reaction_class, go_complex, molecular_function;
	OWLOntology go_cam_ont;
	OWLDataFactory df;
	OWLOntologyManager ontman;
	String base_contributor, base_date, base_provider;
	/**
	 * @throws OWLOntologyCreationException 
	 * 
	 */

	public GoCAM(String gocam_title, String contributor, String date, String provider, boolean add_lego_import) throws OWLOntologyCreationException {
		base_contributor = contributor;
		if(date==null) {
			Date now = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			base_date = sdf.format(now);			
		}else {
			base_date = date;
		}
		base_provider = provider;
		
		ontman = OWLManager.createOWLOntologyManager();				
		String iri = "http://model.geneontology.org/"+gocam_title.hashCode(); //using a URL encoded string here confused the UI code...
		IRI ont_iri = IRI.create(iri);
		go_cam_ont = ontman.createOntology(ont_iri);
		df = OWLManager.getOWLDataFactory();

		if(add_lego_import) {
			String lego_iri = "http://purl.obolibrary.org/obo/go/extensions/go-lego.owl";
			OWLImportsDeclaration legoImportDeclaration = df.getOWLImportsDeclaration(IRI.create(lego_iri));
			ontman.applyChange(new AddImport(go_cam_ont, legoImportDeclaration));
		}
		/*
 <http://model.geneontology.org/5a5fd3de00000008> rdf:type owl:Ontology ;
                                                  owl:versionIRI <http://model.geneontology.org/5a5fd3de00000008> ;
                                                  owl:imports <http://purl.obolibrary.org/obo/go/extensions/go-lego.owl> ;
                                                  <http://geneontology.org/lego/modelstate> "development"^^xsd:string ;
                                                  <http://purl.org/dc/elements/1.1/contributor> "http://orcid.org/0000-0002-2874-6934"^^xsd:string ;
                                                  <http://purl.org/dc/elements/1.1/title> "Tre test"^^xsd:string ;
                                                  <http://purl.org/dc/elements/1.1/date> "2018-01-18"^^xsd:string .		
		 */

		//Annotation properties for metadata and evidence
		title_prop = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/title"));
		contributor_prop = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/contributor"));
		date_prop = df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/elements/1.1/date"));
		state_prop = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/modelstate"));
		evidence_prop = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/evidence"));
		provided_by_prop = df.getOWLAnnotationProperty(IRI.create("http://purl.org/pav/providedBy"));
		x_prop = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/hint/layout/x"));
		y_prop = df.getOWLAnnotationProperty(IRI.create("http://geneontology.org/lego/hint/layout/x"));
		rdfs_label = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		
		//Will add classes and relations as we need them now. 
		//TODO Work on using imports later to ensure we don't produce incorrect ids..

		//biological process
		bp_class = df.getOWLClass(IRI.create(obo_iri + "GO_0008150")); 
		addLabel(bp_class, "Biological Process");
		//molecular function GO:0003674
		molecular_function = df.getOWLClass(IRI.create(obo_iri + "GO_0003674")); 
		addLabel(molecular_function, "Molecular Function");
		//continuant 
		continuant_class = df.getOWLClass(IRI.create(obo_iri + "BFO_0000002")); 
		addLabel(continuant_class, "Continuant");
		//protein
		protein_class = df.getOWLClass(IRI.create(biopax_iri + "Protein")); 
		addLabel(protein_class, "Protein");
		//reaction
		reaction_class = df.getOWLClass(IRI.create(biopax_iri + "Reaction")); 
		addLabel(reaction_class, "Reaction");
		//complex GO_0032991
		go_complex = df.getOWLClass(IRI.create(obo_iri + "GO_0032991")); 
		addLabel(go_complex, "Macromolecular Complex");		

		//tmp for viewing while debugging, will be taken care of by import and reasoning
		OWLSubClassOfAxiom prot = df.getOWLSubClassOfAxiom(protein_class, continuant_class);
		ontman.addAxiom(go_cam_ont, prot);
		ontman.applyChanges();
		OWLSubClassOfAxiom comp = df.getOWLSubClassOfAxiom(go_complex, continuant_class);
		ontman.addAxiom(go_cam_ont, comp);
		ontman.applyChanges();

		//part of
		part_of = df.getOWLObjectProperty(IRI.create(obo_iri + "BFO_0000050"));
		addLabel(part_of, "part of"); 
		//has part
		has_part = df.getOWLObjectProperty(IRI.create(obo_iri + "BFO_0000051"));
		addLabel(has_part, "has part");
		//has input 
		has_input = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002233"));
		addLabel(has_input, "has input");
		//has output 
		has_output = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002234"));
		addLabel(has_output, "has output");
		//directly provides input for (process to process)
		provides_direct_input_for = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002413"));
		addLabel(provides_direct_input_for, "directly provides input for (process to process)");
		//RO_0002408 directly inhibits (process to process)
		directly_inhibits = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002408"));
		addLabel(directly_inhibits, "directly inhibits (process to process)");
		//RO_0002406 directly activates (process to process)
		directly_activates = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002406"));
		addLabel(directly_activates, "directly activates (process to process)");
		//BFO_0000066 occurs in (note that it can only be used for occurents in occurents)
		occurs_in = df.getOWLObjectProperty(IRI.create(obo_iri + "BFO_0000066"));
		addLabel(occurs_in, "occurs in");
		//RO_0001025
		located_in = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0001025"));
		addLabel(located_in, "located in");		
		//RO_0002333 enabled by
		enabled_by = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002333"));
		addLabel(enabled_by, "enabled by");
		//RO_0002327
		enables = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002327"));
		addLabel(enables, "enables");
		//RO_0002334 regulated by (processual) 
		regulated_by = df.getOWLObjectProperty(IRI.create(obo_iri + "RO_0002334"));
		addLabel(regulated_by, "regulated by");
		
		//Annotate the ontology
		OWLAnnotation title_anno = df.getOWLAnnotation(title_prop, df.getOWLLiteral(gocam_title));
		OWLAxiom titleaxiom = df.getOWLAnnotationAssertionAxiom(ont_iri, title_anno);
		ontman.addAxiom(go_cam_ont, titleaxiom);

		OWLAnnotation contributor_anno = df.getOWLAnnotation(contributor_prop, df.getOWLLiteral(base_contributor));
		OWLAxiom contributoraxiom = df.getOWLAnnotationAssertionAxiom(ont_iri, contributor_anno);
		ontman.addAxiom(go_cam_ont, contributoraxiom);

		OWLAnnotation date_anno = df.getOWLAnnotation(date_prop, df.getOWLLiteral(base_date));
		OWLAxiom dateaxiom = df.getOWLAnnotationAssertionAxiom(ont_iri, date_anno);
		ontman.addAxiom(go_cam_ont, dateaxiom);

		OWLAnnotation state_anno = df.getOWLAnnotation(state_prop, df.getOWLLiteral("development"));
		OWLAxiom stateaxiom = df.getOWLAnnotationAssertionAxiom(ont_iri, state_anno);
		ontman.addAxiom(go_cam_ont, stateaxiom);

		ontman.applyChanges();
	}

	public void addAnnotations2Individual(IRI individual_iri, String contributor_uri, String date, String provider_uri) {
		OWLOntologyManager ontman = go_cam_ont.getOWLOntologyManager();
		OWLDataFactory df = ontman.getOWLDataFactory();
		
		OWLAnnotation contributor_anno = df.getOWLAnnotation(contributor_prop, df.getOWLLiteral(contributor_uri));
		OWLAxiom contributoraxiom = df.getOWLAnnotationAssertionAxiom(individual_iri, contributor_anno);
		ontman.addAxiom(go_cam_ont, contributoraxiom);
		
		OWLAnnotation date_anno = df.getOWLAnnotation(date_prop, df.getOWLLiteral(date));
		OWLAxiom dateaxiom = df.getOWLAnnotationAssertionAxiom(individual_iri, date_anno);
		ontman.addAxiom(go_cam_ont, dateaxiom);

		OWLAnnotation provider_anno = df.getOWLAnnotation(provided_by_prop, df.getOWLLiteral(provider_uri));
		OWLAxiom stateaxiom = df.getOWLAnnotationAssertionAxiom(individual_iri, provider_anno);
		ontman.addAxiom(go_cam_ont, stateaxiom);

		ontman.applyChanges();
		
		return;
	}
	
	OWLOntology addLabel(OWLEntity entity, String label) {
		if(label==null) {
			return go_cam_ont;
		}		
		OWLLiteral lbl = df.getOWLLiteral(label);
		OWLAnnotation label_anno = df.getOWLAnnotation(rdfs_label, lbl);
		OWLAxiom labelaxiom = df.getOWLAnnotationAssertionAxiom(entity.getIRI(), label_anno);
		ontman.addAxiom(go_cam_ont, labelaxiom);
		ontman.applyChanges();
		return go_cam_ont;
	}
	
	void addObjectPropertyAssertion(OWLIndividual source, OWLObjectProperty prop, OWLIndividual target) {
		OWLObjectPropertyAssertionAxiom add_prop_axiom = df.getOWLObjectPropertyAssertionAxiom(prop, source, target);
		AddAxiom addAxiom = new AddAxiom(go_cam_ont, add_prop_axiom);
		ontman.applyChanges(addAxiom);
	}
	
	void addSubclassAssertion(OWLClass child, OWLClass parent) {
		OWLSubClassOfAxiom tmp = df.getOWLSubClassOfAxiom(child, parent);
		ontman.addAxiom(go_cam_ont, tmp);
		ontman.applyChanges();
	}

	void addTypeAssertion(OWLNamedIndividual individual, OWLClass type) {
		OWLClassAssertionAxiom isa_xrefedbp = df.getOWLClassAssertionAxiom(type, individual);
		ontman.addAxiom(go_cam_ont, isa_xrefedbp);
		ontman.applyChanges();		
	}
	
	void writeGoCAM(String outfilename) throws OWLOntologyStorageException {
		FileDocumentTarget outfile = new FileDocumentTarget(new File(outfilename));
		ontman.setOntologyFormat(go_cam_ont, new TurtleOntologyFormat());
		ontman.saveOntology(go_cam_ont,outfile);	
	}
	
}
