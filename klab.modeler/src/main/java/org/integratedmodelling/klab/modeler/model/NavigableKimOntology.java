package org.integratedmodelling.klab.modeler.model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.lang.kim.KimConcept;
import org.integratedmodelling.klab.api.lang.kim.KimConceptStatement;
import org.integratedmodelling.klab.api.lang.kim.KimOntology;

public class NavigableKimOntology extends NavigableKlabDocument<KimConceptStatement, KimOntology> implements KimOntology {

	@Serial
	private static final long serialVersionUID = 3213955882357790089L;

	public NavigableKimOntology(KimOntology document, NavigableKlabAsset<?> parent) {
		super(document, parent);
	}

	@Override
	public KimConcept getDomain() {
		return delegate.getDomain();
	}

	@Override
	public Collection<String> getImportedOntologies() {
		return delegate.getImportedOntologies();
	}

	@Override
	public List<Pair<String, String>> getOwlImports() {
		return delegate.getOwlImports();
	}

	@Override
	public List<Pair<String, List<String>>> getVocabularyImports() {
		return delegate.getVocabularyImports();
	}

	@Override
	public List<? extends NavigableKlabStatement> children() {
		return getStatements().stream().map(s -> new NavigableKlabStatement(s, this)).toList();
	}

	@Override
	public Set<String> importedNamespaces(boolean withinType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void visit(DocumentVisitor<KimConceptStatement> visitor) {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
