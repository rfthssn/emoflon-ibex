package org.emoflon.ibex.tgg.compiler.pattern.protocol;

import java.util.Collection;

import org.emoflon.ibex.tgg.compiler.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.pattern.IbexPattern;

import language.DomainType;
import language.LanguageFactory;
import language.TGGRule;
import language.TGGRuleEdge;
import language.TGGRuleElement;
import language.TGGRuleNode;
import language.basic.expressions.ExpressionsFactory;
import language.basic.expressions.TGGExpression;
import language.basic.expressions.TGGLiteralExpression;
import language.inplaceAttributes.InplaceAttributesFactory;
import language.inplaceAttributes.TGGAttributeConstraintOperators;
import language.inplaceAttributes.TGGInplaceAttributeExpression;
import runtime.RuntimePackage;

public class ConsistencyPattern extends IbexPattern {

	private TGGRuleNode protocolNode;
	
	public ConsistencyPattern(TGGRule rule) {
		super(rule);
		
		protocolNode = LanguageFactory.eINSTANCE.createTGGRuleNode();
		protocolNode.setName(getProtocolNodeName());
		protocolNode.setType(RuntimePackage.eINSTANCE.getTGGRuleApplication());
		
		TGGInplaceAttributeExpression tae = InplaceAttributesFactory.eINSTANCE.createTGGInplaceAttributeExpression();
		tae.setAttribute(RuntimePackage.Literals.TGG_RULE_APPLICATION__NAME);
		tae.setOperator(TGGAttributeConstraintOperators.EQUAL);
		
		TGGLiteralExpression le = ExpressionsFactory.eINSTANCE.createTGGLiteralExpression();
		le.setValue("\"" + rule.getName() + "\"");
		
		tae.setValueExpr(le);
		protocolNode.getAttrExpr().add(tae);
		this.getBodyNodes().add(protocolNode);
	}
	
	@Override
	protected boolean isRelevantForSignature(TGGRuleElement e) {
		return true;
	}
	
	@Override
	public Collection<TGGRuleElement> getSignatureElements() {
	 Collection<TGGRuleElement> signatureElements = super.getSignatureElements();
	 signatureElements.add(protocolNode);
	 return signatureElements;
	}

	@Override
	protected String getPatternNameSuffix() {
		return PatternSuffixes.PROTOCOL;
	}

	public String getProtocolNodeName() {
		return "eMoflon_ProtocolNode";
	}

	public String getRuleName() {
		return rule.getName();
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleEdge e) {
		return false;
	}

	@Override
	protected boolean isRelevantForBody(TGGRuleNode n) {
		return n.getDomainType() != DomainType.CORR;
	}

	

}
