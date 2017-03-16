package org.emoflon.ibex.tgg.compiler.pattern.rulepart.support;

import org.emoflon.ibex.tgg.compiler.pattern.protocol.nacs.TrgProtocolNACsPattern;

import language.TGGRule;

public class TrgProtocolAndDECPattern extends TrgProtocolNACsPattern {

	public TrgProtocolAndDECPattern(TGGRule rule) {
		super(rule);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String getPatternNameSuffix() {
		return "_PROTOCOL_DEC_TRG";
	}
}