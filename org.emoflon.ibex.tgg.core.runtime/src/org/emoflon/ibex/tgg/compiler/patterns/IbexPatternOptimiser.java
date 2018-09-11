package org.emoflon.ibex.tgg.compiler.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.emf.ecore.EReference;
import org.emoflon.ibex.tgg.compiler.patterns.common.EdgePattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.IBlackPattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.IbexBasePattern;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;

import language.BindingType;
import language.DomainType;
import language.LanguageFactory;
import language.TGGRule;
import language.TGGRuleCorr;
import language.TGGRuleEdge;
import language.TGGRuleNode;
import language.basic.expressions.TGGLiteralExpression;
import language.inplaceAttributes.TGGInplaceAttributeExpression;

public class IbexPatternOptimiser {

	private static Map<EReference,Collection<EdgePattern>> edgePatterns = new LinkedHashMap<>();
	
	/**
	 * Simple String comparison
	 * @author NilsWeidmann
	 *
	 */
	public static class StringComparator implements Comparator<String> {

		@Override
		public int compare(String arg0, String arg1) {
			return arg0.compareTo(arg1);
		}
	}

	/**
	 * This method takes a pair of nodes which potentially need an
	 * unequal-constraint and checks if this is really necessary.
	 * 
	 * @param nodes
	 *            The pair of nodes between which a unequal-constraint might be
	 *            necessary.
	 * @return true, if the pair requires an unequal-constraint.
	 */
	public boolean unequalConstraintNecessary(TGGRuleNode left, TGGRuleNode right) {
		return !unequalConstantAttributeValues(left, right) && !transitiveContainment(left, right)
				&& !differentContainmentSubTrees(left, right);
	}

	private boolean unequalConstantAttributeValues(TGGRuleNode left, TGGRuleNode right) {
		for (TGGInplaceAttributeExpression attrExprLeft : left.getAttrExpr()) {
			if (attrExprLeft.getValueExpr() instanceof TGGLiteralExpression)
				for (TGGInplaceAttributeExpression attrExprRight : right.getAttrExpr()) {
					if (attrExprRight.getValueExpr() instanceof TGGLiteralExpression
							&& attrExprLeft.getAttribute().equals(attrExprRight.getAttribute()))
						if (!((TGGLiteralExpression) attrExprLeft.getValueExpr()).getValue()
								.equals(((TGGLiteralExpression) attrExprRight.getValueExpr()).getValue())) {
							return true;
						}
				}
		}

		return false;
	}

	private boolean transitiveContainment(TGGRuleNode left, TGGRuleNode right) {
		List<TGGRuleNode> leftHierarchy = containmentHierarchyFromNodeToRoot(left);
		List<TGGRuleNode> rightHierarchy = containmentHierarchyFromNodeToRoot(right);

		for (TGGRuleNode leftHierNode : leftHierarchy) {
			if (leftHierNode.equals(right))
				return true;
		}
		for (TGGRuleNode rightHierNode : rightHierarchy) {
			if (rightHierNode.equals(left))
				return true;
		}

		return false;
	}

	private boolean differentContainmentSubTrees(TGGRuleNode left, TGGRuleNode right) {
		List<TGGRuleNode> leftHierarchy = containmentHierarchyFromNodeToRoot(left);
		List<TGGRuleNode> rightHierarchy = containmentHierarchyFromNodeToRoot(right);

		// if one of the nodes is the root of its tree, both nodes cannot be in
		// different sub-trees
		if (leftHierarchy.size() <= 1 || rightHierarchy.size() <= 1)
			return false;

		// if the roots of both nodes' trees are not equal, they are in unconnected
		// trees,
		// which makes unequal-constraints necessary
		if (!leftHierarchy.get(leftHierarchy.size() - 1).equals(rightHierarchy.get(rightHierarchy.size() - 1)))
			return false;

		// if the parents of both nodes are equal,
		// they have to take care of checking unequality
		if (leftHierarchy.get(1).equals(rightHierarchy.get(1)))
			return false;

		// in the remaining cases, the nodes are in different but connected sub-trees
		// and their parent nodes transitively take care of the unequality
		return true;
	}

	private List<TGGRuleNode> containmentHierarchyFromNodeToRoot(TGGRuleNode node) {
		List<TGGRuleNode> hierarchy = new ArrayList<>();
		Optional<TGGRuleNode> possibleCurrentNode = Optional.of(node);

		while (possibleCurrentNode.isPresent()) {
			TGGRuleNode currentNode = possibleCurrentNode.get();
			hierarchy.add(currentNode);

			Optional<TGGRuleEdge> possibleContainmentEdge = currentNode.getIncomingEdges().stream()
					.filter(e -> e.getType().isContainment()).findAny();

			possibleCurrentNode = possibleContainmentEdge.map(e -> e.getSrcNode());
		}

		return hierarchy;
	}

	/**
	 * Used to avoid redundant reference constraints due to EMF eOpposite semantics.
	 * 
	 * @param e
	 * @param ibexPattern
	 * @return true if e either does not have an eOpposite in the body edges, or if
	 *         it has an eOpposite but it is alphabetically "larger" than it.
	 */
	public boolean retainAsOpposite(TGGRuleEdge edge, IBlackPattern ibexPattern) {
		EReference eOpposite = edge.getType().getEOpposite();

		// No eOpposite possible anyway
		if (eOpposite == null)
			return true;

		List<TGGRuleEdge> eOpposites = ibexPattern.getLocalEdges().stream()
				.filter(otherEdge -> eOpposite.equals(otherEdge.getType())
						&& edge.getSrcNode().equals(otherEdge.getTrgNode())
						&& edge.getTrgNode().equals(otherEdge.getSrcNode()))
				.collect(Collectors.toList());

		// No eOpposite amongst bodyEdges
		if (eOpposites.isEmpty())
			return true;

		// Not alphabetically larger than eOpposite
		if (edge.getType().getName().compareTo(eOpposite.getName()) > 0)
			return true;

		// If not can be skipped!
		return false;
	}

	/**
	 * If the premise of a filter NAC consists of three nodes and two edges of the
	 * same type and the edge type is containment then it can never be violated as a
	 * node cannot have two containers. In this case it is redundant.
	 * 
	 * @param filterNAC
	 * @return true if filter NAC is redundant
	 */
	public boolean isRedundantDueToEMFContainmentSemantics(IBlackPattern filterNAC) {
		// Premise is the only positive invocation
		assert (filterNAC.getPositiveInvocations().size() == 1);
		IBlackPattern premise = premise(filterNAC);

		// Check for expected structure: three nodes and two edges
		if (premise.getLocalNodes().size() == 3 && premise.getLocalEdges().size() == 2) {
			Iterator<TGGRuleEdge> iterator = premise.getLocalEdges().iterator();
			TGGRuleEdge edge1 = iterator.next();
			TGGRuleEdge edge2 = iterator.next();

			// Edges must be of same type and be containment
			if (edge1.getType().equals(edge2.getType()) && edge1.getType().isContainment()) {
				// Edges must contain the same node (which is impossible so filter NAC can be
				// ignored)
				if (edge1.getTrgNode().equals(edge2.getTrgNode()))
					return true;
			}
		}

		return false;
	}

	/**
	 * If the premise of a filter NAC consists of two nodes connected by an edge e,
	 * and there exists another filter NAC with a premise with the same two nodes
	 * but with e' = eOpposite(e) then one of these filter NACs can be removed.
	 * 
	 * @param allFilterNACs
	 * @return filter NACs that can be safely ignored
	 */
	public Collection<IBlackPattern> ignoreDueToEOppositeSemantics(Collection<IBlackPattern> allFilterNACs) {
		List<IBlackPattern> candidates = allFilterNACs.stream().filter(nac -> nac.getPositiveInvocations().size() == 1)
				.filter(nac -> premiseHasTwoNodesAndOneEdge(
						nac.getPositiveInvocations().iterator().next().getInvokedPattern()))
				.collect(Collectors.toList());

		return candidates.stream().filter(nac -> {
			return allFilterNACs.stream().anyMatch(otherNAC -> isGreaterEOpposite(premise(otherNAC), premise(nac)));
		}).collect(Collectors.toList());
	}

	private boolean isGreaterEOpposite(IBlackPattern premise1, IBlackPattern premise2) {
		TGGRuleEdge e1 = premise1.getLocalEdges().iterator().next();
		TGGRuleEdge e2 = premise2.getLocalEdges().iterator().next();

		if (e2.getType().equals(e1.getType().getEOpposite())) {
			if (e1.getSrcNode().getName().equals(e2.getTrgNode().getName())
					|| e1.getTrgNode().getName().equals(e2.getSrcNode().getName())) {
				return e1.getType().getName().compareTo(e2.getType().getName()) >= 0;
			}
		}

		return false;
	}

	private IBlackPattern premise(IBlackPattern nac) {
		return nac.getPositiveInvocations().iterator().next().getInvokedPattern();
	}

	private boolean premiseHasTwoNodesAndOneEdge(IBlackPattern premise) {
		return premise.getLocalNodes().size() == 2 && premise.getLocalEdges().size() == 1;
	}

	/**
	 * Replaces local edges of a given pattern by invocations to the respective edge
	 * pattern
	 * 
	 * @param pattern:
	 *            Given Pattern
	 * @throws NotImplementedException:
	 *             Thrown if the factory does not contain a suitable pattern
	 */
	public void replaceEdges(IbexBasePattern pattern, IbexOptions options) {
		Collection<TGGRuleEdge> edges = pattern.getLocalEdges();

		// Only extract edges if the pattern is complex enough	
		if (edges.size() < options.minimumNumberOfEdgesToCreateEdgePatterns())
			return;

		Collection<TGGRuleEdge> edgesToRemove = new ArrayList<TGGRuleEdge>();

		for (TGGRuleEdge edge : edges) {
			EReference key = edge.getType();
			Collection<EdgePattern> patterns = edgePatterns.get(key);
			EdgePattern ep = null;
			
			// Initialize collection if necessary
			if (patterns == null)
				patterns = new ArrayList<>();
		
			// Check if the EdgePattern matches the current edge
			for (EdgePattern p : patterns) {
				if (!options.stronglyTypedEdgedPatterns() || edgeMatchesPattern(p,edge) ) {
					ep = p;
					break;
				}
			}
			
			if (ep == null) {
				ep = new EdgePattern(pattern.getPatternFactory(), edge, options.stronglyTypedEdgedPatterns());
				patterns.add(ep);
				edgePatterns.put(key, patterns);
			}

			// Mapping via source and target node
			Map<TGGRuleNode, TGGRuleNode> mapping = new HashMap<>();
			mapping.put(edge.getSrcNode(), ep.getEdge().getSrcNode());
			mapping.put(edge.getTrgNode(), ep.getEdge().getTrgNode());

			// Invocation
			pattern.addPositiveInvocation(ep, mapping);
			edgesToRemove.add(edge);
		}

		edges.removeAll(edgesToRemove);
	}
	
	/**
	 * 
	 * @param p: EdgePattern to test
	 * @param e: Edge that possibly matches the pattern
	 * @return True, if e matches p
	 */
	private boolean edgeMatchesPattern(EdgePattern p, TGGRuleEdge e) {
		TGGRuleNode srcEdge = e.getSrcNode();
		TGGRuleNode trgEdge = e.getTrgNode();
		TGGRuleEdge patternEdge = p.getEdge();
		TGGRuleNode srcPattern = patternEdge.getSrcNode();
		TGGRuleNode trgPattern = patternEdge.getTrgNode();
		
		// Handle source nodes
		if (srcEdge.getAttrExpr().size() != srcPattern.getAttrExpr().size())
			return false;
		
		if (!convertToString(srcEdge.getAttrExpr()).equals(convertToString(srcPattern.getAttrExpr())))
			return false;
		
		// Handle target nodes
		if (trgEdge.getAttrExpr().size() != trgPattern.getAttrExpr().size())
			return false;
		
		if (!convertToString(trgEdge.getAttrExpr()).equals(convertToString(trgPattern.getAttrExpr())))
			return false;
		
		return true;
	}
	
	/**
	 * Creates a unique string out of a collection of attribute expressions for comparing them
	 * @param list: List of attribute expressions
	 * @return Unique string
	 */
	public static String convertToString(Collection<TGGInplaceAttributeExpression> list) {
		String result = "";
		ArrayList<String> attributeStrings = new ArrayList<>();
		
		
		for (TGGInplaceAttributeExpression expr : list) {
			// Safely convert component names to strings		
			String attributeName = expr.getAttribute() == null ? "" : expr.getAttribute().getName();
			String operatorName = expr.getOperator() == null ? "" : expr.getOperator().getName();
			String valueName = expr.getValueExpr() == null ? "" : expr.getValueExpr().eGet(
					expr.getValueExpr().eClass().getEStructuralFeature("value")).toString();
			attributeStrings.add(attributeName + "_" + operatorName + "_" + valueName + "/");
		}
		
		
		attributeStrings.sort(new StringComparator());
		
		for (String as : attributeStrings) {
			result += as;
		}
			
		return result;
	}

	public Collection<TGGRuleNode> determineLocalNodes(IbexOptions options, Collection<TGGRuleEdge> localEdges,
			TGGRule rule) {
		if (options.setCorrContextNodesAsLocalNodes()) {
			rule.getNodes().stream()//
					.filter(this::isContextCorr)//
					.map(TGGRuleCorr.class::cast)//
					.forEach(corr -> extractTargetEdges(corr, localEdges));
			return rule.getNodes().stream().filter(this::isContextCorr).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}
	
	private boolean isContextCorr(TGGRuleNode n) {
		return n.getBindingType() == BindingType.CONTEXT && n instanceof TGGRuleCorr;
	}
	
	private void extractTargetEdges(TGGRuleCorr corr, Collection<TGGRuleEdge> localEdges) {
		TGGRuleEdge target = LanguageFactory.eINSTANCE.createTGGRuleEdge();
		target.setBindingType(corr.getBindingType());
		target.setDomainType(DomainType.TRG);
		target.setName("target");
		target.setType((EReference) corr.getType().getEStructuralFeature("target"));
		target.setSrcNode(corr);
		target.setTrgNode(corr.getTarget());
		localEdges.add(target);
	}
}