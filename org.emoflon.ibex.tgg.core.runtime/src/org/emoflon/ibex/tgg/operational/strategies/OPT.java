package org.emoflon.ibex.tgg.operational.strategies;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EStructuralFeatureImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.compiler.patterns.sync.ConsistencyPattern;
import org.emoflon.ibex.tgg.operational.defaults.IbexGreenInterpreter;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.edge.RuntimeEdge;
import org.emoflon.ibex.tgg.operational.edge.RuntimeEdgeHashingStrategy;
import org.emoflon.ibex.tgg.operational.matches.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.cc.Bundle;
import org.emoflon.ibex.tgg.operational.strategies.cc.ConsistencyReporter;
import org.emoflon.ibex.tgg.operational.strategies.cc.HandleDependencies;
import org.emoflon.ibex.tgg.operational.updatepolicy.IUpdatePolicy;
import org.emoflon.ibex.tgg.util.ilp.ILPFactory;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem.Comparator;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem.ILPLinearExpression;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem.ILPSolution;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem.ILPTerm;
import org.emoflon.ibex.tgg.util.ilp.ILPProblem.Objective;
import org.emoflon.ibex.tgg.util.ilp.ILPSolver;

import com.google.common.collect.Sets;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import language.TGGRuleNode;

public abstract class OPT extends OperationalStrategy {

	protected TIntObjectHashMap<IMatch> idToMatch = new TIntObjectHashMap<>();
	protected TCustomHashMap<RuntimeEdge, TIntHashSet> edgeToMarkingMatches = new TCustomHashMap<>(
			new RuntimeEdgeHashingStrategy());
	protected THashMap<EObject, TIntHashSet> nodeToMarkingMatches = new THashMap<>();
	protected THashMap<IMatch, HashMap<String, EObject>> matchToCoMatch = new THashMap<>();
	protected ConsistencyReporter consistencyReporter = new ConsistencyReporter();
	protected int nameCounter = 0;
	protected TIntObjectMap<THashSet<EObject>> matchToContextNodes = new TIntObjectHashMap<>();
	protected TIntObjectMap<TCustomHashSet<RuntimeEdge>> matchToContextEdges = new TIntObjectHashMap<>();

	/**
	 * Collection of constraints to guarantee uniqueness property; key: Complement
	 * rule (CR) match ID; value: other CR matches of the same CR using the same
	 * context as CR match
	 */
	protected THashMap<Integer, TIntHashSet> sameComplementMatches = new THashMap<>();

	/**
	 * Collection of constraints to guarantee maximality property;
	 * value: kernels whose complement rules did not fulfill maximality property
	 */
	protected TIntHashSet invalidKernels = new TIntHashSet();

	/**
	 * Collection of constraints to guarantee cyclic dependences are avoided;
	 * value: correctly applied bundles (kernel match + its CRs matches)
	 */
	protected HashSet<Bundle> appliedBundles = new HashSet<Bundle>();
	protected Bundle lastAppliedBundle;

	protected TIntObjectHashMap<String> matchIdToRuleName = new TIntObjectHashMap<>();
	protected int idCounter = 1;

	//Hash maps to save the old metamodel state
	THashMap<EReference, Integer> referenceToUpperBound = new THashMap<EReference, Integer>();
	THashMap<EReference, Integer> referenceToLowerBound = new THashMap<EReference, Integer>();
	THashMap<EReference, EReference> referenceToEOpposite = new THashMap<EReference, EReference>();
	THashMap<EReference, Boolean> referenceToContainment = new THashMap<EReference, Boolean>();

	public OPT(IbexOptions options) throws IOException {
		super(options);
	}

	public OPT(IbexOptions options, IUpdatePolicy policy) {
		super(options, policy);
	}

	public void relaxReferences(EList<EPackage> model) {

		EPackage[] packages = (EPackage[])model.toArray();

		for (EPackage p : packages) {
			TreeIterator<EObject> it = p.eAllContents();

			while (it.hasNext()) {
				EObject next = it.next();
				if (next instanceof EClassImpl) {
					EClassImpl nextEClassImpl = (EClassImpl)next;

					for (EReference reference : nextEClassImpl.getEAllReferences()) {
						if (referenceToUpperBound.containsKey(reference) &&
								referenceToLowerBound.containsKey(reference) &&
								referenceToContainment.containsKey(reference) &&
								referenceToEOpposite.containsKey(reference)) {
							// Reference already exists, values must not be overwritten
							continue;
						}

						//Save metamodel values
						referenceToUpperBound.put(reference, reference.getUpperBound());
						referenceToLowerBound.put(reference, reference.getLowerBound());
						referenceToContainment.put(reference, reference.isContainment());
						referenceToEOpposite.put(reference, reference.getEOpposite());


						//Change metamodel values
						reference.setUpperBound(-1);
						reference.setLowerBound(0);
						reference.setContainment(false);
						reference.setEOpposite(null);						
					}
				}
			}
		}		
	}

	public void unrelaxReferences(EList<EPackage> model) {

		EPackage[] packages = (EPackage[])model.toArray();

		for (EPackage p : packages) {
			TreeIterator<EObject> it = p.eAllContents();

			while (it.hasNext()) {
				EObject next = it.next();
				if (next instanceof EClassImpl) {
					EClassImpl nextEClassImpl = (EClassImpl)next;

					for (EReference reference : nextEClassImpl.getEAllReferences()) {				
						// Get old metamodel values
						int upperBound = referenceToUpperBound.get(reference);
						int lowerBound = referenceToLowerBound.get(reference);
						boolean containment = referenceToContainment.get(reference);
						EReference eOpposite = referenceToEOpposite.get(reference);

						// Change metamodel values
						reference.setUpperBound(upperBound);
						reference.setLowerBound(lowerBound);
						reference.setContainment(containment);
						reference.setEOpposite(eOpposite);

						// Reset setting for reference
						((EStructuralFeatureImpl) reference).setSettingDelegate(null);
					}
				}
			}
		}
	}

	protected void defineILPExclusions(ILPProblem ilpProblem) {
		for (EObject node : nodeToMarkingMatches.keySet()) {
			TIntHashSet variables = nodeToMarkingMatches.get(node);
			List<ILPTerm> ilpTerms = new LinkedList<>();
			variables.forEach(v -> {
				ilpTerms.add(ilpProblem.createTerm("x" + v, 1.0));
				return true;
			});
			ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
			ilpProblem.addConstraint(expr, Comparator.le, 1.0, "EXCL" + nameCounter++);
		}

		for (RuntimeEdge edge : edgeToMarkingMatches.keySet()) {
			TIntHashSet variables = edgeToMarkingMatches.get(edge);
			List<ILPTerm> ilpTerms = new LinkedList<>();
			variables.forEach(v -> {
				ilpTerms.add(ilpProblem.createTerm("x" + v, 1.0));
				return true;
			});
			ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
			ilpProblem.addConstraint(expr, Comparator.le, 1.0, "EXCL" + nameCounter++);
		}

		for (Integer match : sameComplementMatches.keySet()) {
			TIntHashSet variables = sameComplementMatches.get(match);
			List<ILPTerm> ilpTerms = new LinkedList<>();
			variables.forEach(v -> {
				ilpTerms.add(ilpProblem.createTerm("x" + v, 1.0));
				return true;
			});
			ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
			ilpProblem.addConstraint(expr, Comparator.le, 1.0, "EXCL" + nameCounter++);
		}

		if (!invalidKernels.isEmpty()) {
			TIntHashSet variables = invalidKernels;
			List<ILPTerm> ilpTerms = new LinkedList<>();
			variables.forEach(v -> {
				ilpTerms.add(ilpProblem.createTerm("x" + v, 1.0));
				return true;
			});
			ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
			ilpProblem.addConstraint(expr, Comparator.le, 0.0, "EXCL" + nameCounter++);
		}

		HandleDependencies handleCycles = new HandleDependencies(appliedBundles, edgeToMarkingMatches, nodeToMarkingMatches, matchToContextNodes, matchToContextEdges);
		Set<Integer> allCyclicBundles = handleCycles.getCyclicDependenciesBetweenBundles().keySet();

		for (int cycle : allCyclicBundles) {
			Set<List<Integer>> cyclicConstraints = getCyclicConstraints(handleCycles.getComplementRuleApplications(cycle));
			for (List<Integer> variables : cyclicConstraints) {
				List<ILPTerm> ilpTerms = new LinkedList<>();
				variables.forEach(v -> {
					ilpTerms.add(ilpProblem.createTerm("x" + v, 1.0));
				});
				ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
				ilpProblem.addConstraint(expr, Comparator.le, variables.size()-1, "EXCL" + nameCounter++);
			}
		}
	}

	private Set<List<Integer>> getCyclicConstraints(List<HashSet<Integer>> dependedRuleApplications) {
		return Sets.cartesianProduct(dependedRuleApplications);
	}

	protected void defineILPImplications(ILPProblem ilpProblem) {
		for (int v : idToMatch.keySet().toArray()) {			
			THashSet<EObject> contextNodes = matchToContextNodes.get(v);
			for (EObject node : contextNodes) {
				ILPTerm term = ilpProblem.createTerm("x" + v, 1.0);
				ILPLinearExpression expr = ilpProblem.createLinearExpression(term);
				if (nodeToMarkingMatches.contains(node)) {
					for (int v2 : nodeToMarkingMatches.get(node).toArray()) {
						expr.addTerm(ilpProblem.createTerm("x" + v2, -1.0));
					}
					ilpProblem.addConstraint(expr, Comparator.le, 0.0, "IMPL" + nameCounter++);
				} else {
					ilpProblem.addConstraint(expr, Comparator.le, 1.0, "IMPL" + nameCounter++);
				}
			}

			TCustomHashSet<RuntimeEdge> contextEdges = matchToContextEdges.get(v);
			for (RuntimeEdge edge : contextEdges) {
				ILPTerm term = ilpProblem.createTerm("x" + v, 1.0);
				ILPLinearExpression expr = ilpProblem.createLinearExpression(term);
				if (edgeToMarkingMatches.contains(edge)) {
					for (int v2 : edgeToMarkingMatches.get(edge).toArray()) {
						expr.addTerm(ilpProblem.createTerm("x" + v2, -1.0));
					}
					ilpProblem.addConstraint(expr, Comparator.le, 0.0, "IMPL" + nameCounter++);
				} else {
					ilpProblem.addConstraint(expr, Comparator.le, 1.0, "IMPL" + nameCounter++);
				}
			}
		}
	}

	protected void defineILPObjective(ILPProblem ilpProblem) {
		List<ILPTerm> ilpTerms = new LinkedList<>();
		idToMatch.keySet().forEach(v -> {
			int weight = getWeightForMatch(idToMatch.get(v), matchIdToRuleName.get(v));
			ilpTerms.add(ilpProblem.createTerm("x" + v, weight));
			return true;
		});
		ILPLinearExpression expr = ilpProblem.createLinearExpression(ilpTerms.toArray(new ILPTerm[0]));
		ilpProblem.setObjective(expr, Objective.maximize);
	}

	protected int[] chooseTGGRuleApplications() {
		
		ILPProblem ilpProblem = ILPFactory.createILPProblem();
		defineILPExclusions(ilpProblem);
		defineILPImplications(ilpProblem);
		addUserDefinedConstraints(ilpProblem);
		defineILPObjective(ilpProblem);
		
		try {
			ILPSolution ilpSolution = ILPSolver.solveBinaryILPProblem(ilpProblem, this.options.getIlpSolver());

			int[] result = new int[idToMatch.size()];
			idToMatch.keySet().forEach(v -> {
				if (ilpSolution.getVariable("x"+ v) > 0)
					result[v - 1] = v;
				else
					result[v - 1] = -v;
				return true;
			});

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected abstract void addUserDefinedConstraints(ILPProblem ilpProblem);

	protected void handleBundles(IMatch comatch, String ruleName) {
		if(!getComplementRule(ruleName).isPresent()) {
			Bundle appliedBundle = new Bundle(idCounter);
			appliedBundles.add(appliedBundle);
			lastAppliedBundle = appliedBundle;
		}

		lastAppliedBundle.addMatch(idCounter);

		// add context nodes and edges of this concrete match to its bundle
		lastAppliedBundle.addBundleContextNodes(getBlackNodes(comatch, ruleName));
		lastAppliedBundle.addBundleContextEdges(getBlackEdges(comatch, ruleName));
	}

	protected THashSet<EObject> getGreenNodes(IMatch comatch, String ruleName) {
		THashSet<EObject> result = new THashSet<>();
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getGreenSrcNodesInRule()));
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getGreenTrgNodesInRule()));
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getGreenCorrNodesInRule()));
		return result;
	}

	protected THashSet<EObject> getBlackNodes(IMatch comatch, String ruleName) {
		THashSet<EObject> result = new THashSet<>();
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getBlackSrcNodesInRule()));
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getBlackTrgNodesInRule()));
		result.addAll(getNodes(comatch, getGreenFactory(ruleName).getBlackCorrNodesInRule()));
		return result;
	}

	protected THashSet<EObject> getNodes(IMatch comatch,
			Collection<? extends TGGRuleNode> specNodes) {
		THashSet<EObject> result = new THashSet<>();
		specNodes.forEach(n -> {
			result.add((EObject) comatch.get(n.getName()));
		});
		return result;
	}

	protected THashSet<RuntimeEdge> getGreenEdges(IMatch comatch, String ruleName) {
		THashSet<RuntimeEdge> result = new THashSet<>();
		result.addAll(((IbexGreenInterpreter)greenInterpreter).createEdges(comatch, getGreenFactory(ruleName).getGreenSrcEdgesInRule(), false));
		result.addAll(((IbexGreenInterpreter)greenInterpreter).createEdges(comatch, getGreenFactory(ruleName).getGreenTrgEdgesInRule(), false));
		return result;
	}

	protected THashSet<RuntimeEdge> getBlackEdges(IMatch comatch, String ruleName) {
		THashSet<RuntimeEdge> result = new THashSet<>();
		result.addAll(((IbexGreenInterpreter)greenInterpreter).createEdges(comatch, getGreenFactory(ruleName).getBlackSrcEdgesInRule(), false));
		result.addAll(((IbexGreenInterpreter)greenInterpreter).createEdges(comatch, getGreenFactory(ruleName).getBlackTrgEdgesInRule(), false));
		return result;
	}

	protected Collection<EObject> getRuleApplicationNodes(IMatch comatch) {
		return comatch.getParameterNames()
				.stream()
				.filter(p -> p.endsWith(ConsistencyPattern.protocolNodeSuffix))
				.map(comatch::get)
				.map(EObject.class::cast)
				.collect(Collectors.toList());
	}
	
	protected abstract int getWeightForMatch(IMatch comatch, String ruleName);
	
	@Override
	public Resource loadResource(String workspaceRelativePath) throws IOException{
		return super.loadResource(workspaceRelativePath);
	}
}
