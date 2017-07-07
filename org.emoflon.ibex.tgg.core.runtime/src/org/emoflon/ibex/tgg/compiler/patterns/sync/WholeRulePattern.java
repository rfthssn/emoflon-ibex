package org.emoflon.ibex.tgg.compiler.patterns.sync;

import org.emoflon.ibex.tgg.compiler.patterns.PatternFactory;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.common.CorrContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.RulePartPattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.SrcPattern;
import org.emoflon.ibex.tgg.compiler.patterns.common.TrgPattern;

import language.BindingType;
import language.DomainType;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;

public class WholeRulePattern extends RulePartPattern {

	public WholeRulePattern(PatternFactory factory) {
		super(factory.getRule());
		
		// Create pattern network
		addTGGPositiveInvocation(factory.create(SrcPattern.class));
		addTGGPositiveInvocation(factory.create(TrgPattern.class));
		addTGGPositiveInvocation(factory.create(CorrContextPattern.class));
	}

	@Override
	protected boolean injectivityIsAlreadyChecked(TGGRuleNode node1, TGGRuleNode node2) {
		return true;
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleEdge e) {
		return false;
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleNode n) {
		return n.getBindingType() == BindingType.CREATE && n.getDomainType() == DomainType.CORR;
	}

	@Override
	public boolean isRelevantForSignature(TGGRuleElement e) {
		return true;
	}

	@Override
	protected String getPatternNameSuffix() {
		return PatternSuffixes.WHOLE;
	}

}