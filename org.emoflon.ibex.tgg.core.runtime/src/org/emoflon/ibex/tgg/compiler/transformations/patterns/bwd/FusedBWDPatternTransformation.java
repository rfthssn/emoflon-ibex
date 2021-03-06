package org.emoflon.ibex.tgg.compiler.transformations.patterns.bwd;

import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.ContextPatternTransformation;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.common.FusedPatternTransformation;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.common.OperationalPatternTransformation;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;

import language.DomainType;
import language.TGGRule;

public class FusedBWDPatternTransformation extends FusedPatternTransformation {

	public FusedBWDPatternTransformation(ContextPatternTransformation parent, IbexOptions options, TGGRule rule, OperationalStrategy strategy) {
		super(parent, options, rule, DomainType.TRG, strategy);
	}

	@Override
	protected String getSuffix() {
		return PatternSuffixes.BWD;
	}

	@Override
	protected OperationalPatternTransformation getKernelTransformer() {
		return new BWDPatternTransformation(parent, options, crule.getKernel());
	}
}
