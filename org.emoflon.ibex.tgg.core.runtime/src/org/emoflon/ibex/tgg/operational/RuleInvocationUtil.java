package org.emoflon.ibex.tgg.operational;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.transformation.runtime.emf.modelmanipulation.IModelManipulations;
import org.eclipse.viatra.transformation.runtime.emf.modelmanipulation.ModelManipulationException;

import language.BindingType;
import language.DomainType;
import language.TGG;
import language.TGGRule;
import language.TGGRuleCorr;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;
import runtime.Edge;
import runtime.RuntimePackage;
import runtime.TGGRuleApplication;

public class RuleInvocationUtil {

	private IModelManipulations manipulator;

	/*
	 * these hash maps serve as rule info to indicate what variables are black/green & src/corr/trg in a rule
	 */
	private HashMap<String, Collection<TGGRuleNode>> greenSrcNodes = new HashMap<>();
	private HashMap<String, Collection<TGGRuleNode>> greenTrgNodes = new HashMap<>();
	private HashMap<String, Collection<TGGRuleEdge>> greenSrcEdges = new HashMap<>();
	private HashMap<String, Collection<TGGRuleEdge>> greenTrgEdges = new HashMap<>();
	private HashMap<String, Collection<TGGRuleCorr>> greenCorrNodes = new HashMap<>();

	private HashMap<String, Collection<TGGRuleElement>> blackSrcElements = new HashMap<>();
	private HashMap<String, Collection<TGGRuleElement>> blackCorrElements = new HashMap<>();
	private HashMap<String, Collection<TGGRuleElement>> blackTrgElements = new HashMap<>();

	private Resource srcR;
	private Resource trgR;
	private Resource corrR;
	private Resource protocolR;

	private RuntimePackage runtimePackage = RuntimePackage.eINSTANCE;

	public RuleInvocationUtil(TGG tgg, Resource srcR, Resource corrR, Resource trgR,
			Resource protocolR) {
		tgg.getRules().forEach(r -> prepareRuleInfo(r));
		this.srcR = srcR;
		this.corrR = corrR;
		this.trgR = trgR;
		this.protocolR = protocolR;
	}
	
	public void setModelManipulation(IModelManipulations manipulator){
		this.manipulator = manipulator;
	}

	public TGGRuleApplication applyFWD(String ruleName, IPatternMatch match) {
		return apply(ruleName, match, AppType.FWD);
	}

	public TGGRuleApplication applyBWD(String ruleName, IPatternMatch match) {
		return apply(ruleName, match, AppType.BWD);
	}

	public TGGRuleApplication applyCC(String ruleName, IPatternMatch match) {
		return apply(ruleName, match, AppType.CC);
	}

	public TGGRuleApplication applyModelgen(String ruleName, IPatternMatch match) {
		return apply(ruleName, match, AppType.MODELGEN);
	}

	private TGGRuleApplication apply(String ruleName, IPatternMatch match, AppType appType) {

		/*
		 * this hash map complements the match to a comatch of an original triple rule application
		 */
		HashMap<String, EObject> createdElements = new HashMap<>();

		boolean createSrcElements = appType == AppType.BWD || appType == AppType.MODELGEN;
		boolean createTrgElements = appType == AppType.FWD || appType == AppType.MODELGEN;

		if (createSrcElements) {
			createNonCorrs(match, createdElements, greenSrcNodes.get(ruleName), greenSrcEdges.get(ruleName), srcR);
		}

		if (createTrgElements) {
			createNonCorrs(match, createdElements, greenTrgNodes.get(ruleName), greenTrgEdges.get(ruleName), trgR);
		}

		createCorrs(ruleName, match, createdElements, greenCorrNodes.get(ruleName));

		return prepareProtocol(ruleName, match, createdElements);
	}

	private void createCorrs(String ruleName, IPatternMatch match, HashMap<String, EObject> createdElements,
			Collection<TGGRuleCorr> greenCorrs) {
		for (TGGRuleCorr c : greenCorrs) {
			createdElements.put(c.getName(),
					createCorr(c, getVariableByName(c.getSource().getName(), createdElements, match),
							getVariableByName(c.getTarget().getName(), createdElements, match)));
		}
	}

	private void createNonCorrs(IPatternMatch match, HashMap<String, EObject> createdElements,
			Collection<TGGRuleNode> greenNodes, Collection<TGGRuleEdge> greenEdges, Resource nodeResource) {
		for (TGGRuleNode n : greenNodes) {
			createdElements.put(n.getName(), createNode(n, nodeResource));
		}
		for (TGGRuleEdge e : greenEdges) {
			createdElements.put(e.getName(),
					createEdge(e, getVariableByName(e.getSrcNode().getName(), createdElements, match),
							getVariableByName(e.getTrgNode().getName(), createdElements, match)));
		}
	}
	
	private EObject createCorr(TGGRuleCorr c, EObject src, EObject trg) {
		EObject corr = createNode(c, corrR);
		try {
			manipulator.add(corr, corr.eClass().getEStructuralFeature("source"), src);
			manipulator.add(corr, corr.eClass().getEStructuralFeature("target"), trg);
		} catch (ModelManipulationException e) {
			e.printStackTrace();
		}
		return corr;
	}

	private EObject createNode(TGGRuleNode n, Resource resource) {
		try {
			return manipulator.create(resource, n.getType());
		} catch (ModelManipulationException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Edge createEdge(TGGRuleEdge e, EObject src, EObject trg) {
		try {
			Edge edge = (Edge) manipulator.create(protocolR, runtimePackage.getEdge());
			manipulator.set(edge, runtimePackage.getEdge_Name(), e.getType().getName());
			manipulator.set(edge, runtimePackage.getEdge_Src(), src);
			manipulator.set(edge, runtimePackage.getEdge_Trg(), trg);
			return edge;
		} catch (ModelManipulationException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	private EObject getVariableByName(String name, HashMap<String, EObject> createdElements, IPatternMatch match) {
		if (createdElements.containsKey(name))
			return createdElements.get(name);
		return (EObject) match.get(name);
	}

	private TGGRuleApplication prepareProtocol(String ruleName, IPatternMatch match,
			HashMap<String, EObject> createdElements) {

		try {
			TGGRuleApplication protocol = (TGGRuleApplication) manipulator.create(protocolR,
					runtimePackage.getTGGRuleApplication());


			fillProtocolInfo(blackSrcElements.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_ContextSrc(), createdElements, match);
			fillProtocolInfo(blackTrgElements.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_ContextTrg(), createdElements, match);
			fillProtocolInfo(blackCorrElements.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_ContextCorr(), createdElements, match);

			fillProtocolInfo(greenSrcNodes.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_CreatedSrc(), createdElements, match);
			fillProtocolInfo(greenSrcEdges.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_CreatedSrc(), createdElements, match);

			fillProtocolInfo(greenTrgNodes.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_CreatedTrg(), createdElements, match);
			fillProtocolInfo(greenTrgEdges.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_CreatedTrg(), createdElements, match);

			fillProtocolInfo(greenCorrNodes.get(ruleName), protocol,
					runtimePackage.getTGGRuleApplication_CreatedCorr(), createdElements, match);
			return protocol;
		} catch (ModelManipulationException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void fillProtocolInfo(Collection<? extends TGGRuleElement> ruleInfos, TGGRuleApplication protocol,
			EStructuralFeature feature, HashMap<String, EObject> createdElements, IPatternMatch match) {
		ruleInfos.forEach(e -> {
			try {
				manipulator.add(protocol, feature, getVariableByName(e.getName(), createdElements, match));
			} catch (ModelManipulationException e1) {
				e1.printStackTrace();
			}
		});

	}

	private void prepareRuleInfo(TGGRule r) {
		String ruleName = r.getName();
		greenSrcNodes.put(ruleName,
				r.getNodes().stream()
						.filter(n -> n.getBindingType() == BindingType.CREATE && n.getDomainType() == DomainType.SRC)
						.collect(Collectors.toSet()));
		greenTrgNodes.put(ruleName,
				r.getNodes().stream()
						.filter(n -> n.getBindingType() == BindingType.CREATE && n.getDomainType() == DomainType.TRG)
						.collect(Collectors.toSet()));
		greenCorrNodes.put(ruleName,
				r.getNodes().stream()
						.filter(n -> n.getBindingType() == BindingType.CREATE && n.getDomainType() == DomainType.CORR)
						.map(n -> (TGGRuleCorr) n).collect(Collectors.toSet()));

		greenSrcEdges.put(ruleName,
				r.getEdges().stream()
						.filter(e -> e.getBindingType() == BindingType.CREATE && e.getDomainType() == DomainType.SRC)
						.collect(Collectors.toSet()));

		greenTrgEdges.put(ruleName,
				r.getEdges().stream()
						.filter(e -> e.getBindingType() == BindingType.CREATE && e.getDomainType() == DomainType.TRG)
						.collect(Collectors.toSet()));

		blackSrcElements.put(ruleName,
				Stream.concat(r.getNodes().stream(), r.getEdges().stream())
						.filter(e -> e.getBindingType() == BindingType.CONTEXT && e.getDomainType() == DomainType.SRC)
						.collect(Collectors.toSet()));

		blackTrgElements.put(ruleName,
				Stream.concat(r.getNodes().stream(), r.getEdges().stream())
						.filter(e -> e.getBindingType() == BindingType.CONTEXT && e.getDomainType() == DomainType.TRG)
						.collect(Collectors.toSet()));

		blackCorrElements.put(ruleName,
				r.getNodes().stream()
						.filter(e -> e.getBindingType() == BindingType.CONTEXT && e.getDomainType() == DomainType.TRG)
						.collect(Collectors.toSet()));

	}

	private enum AppType {
		FWD, BWD, CC, MODELGEN
	}

}
