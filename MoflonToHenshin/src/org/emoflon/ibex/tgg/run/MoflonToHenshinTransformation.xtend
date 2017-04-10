
	package org.emoflon.ibex.tgg.run
	
	import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine
	import org.eclipse.viatra.query.runtime.emf.EMFScope
	import org.eclipse.viatra.transformation.runtime.emf.rules.eventdriven.EventDrivenTransformationRuleFactory
	import org.eclipse.viatra.transformation.runtime.emf.transformation.eventdriven.EventDrivenTransformation
	import org.eclipse.viatra.transformation.evm.specific.Lifecycles
	import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum
	import org.eclipse.viatra.transformation.runtime.emf.rules.EventDrivenTransformationRuleGroup
	import org.eclipse.emf.ecore.resource.Resource	
	import org.eclipse.emf.ecore.resource.ResourceSet
	import org.emoflon.ibex.tgg.operational.TGGRuntimeUtil
	import org.emoflon.ibex.tgg.operational.MODELGEN
	import org.emoflon.ibex.tgg.operational.OperationMode
	import org.emoflon.ibex.tgg.operational.OperationStrategy
	import org.apache.log4j.Logger
	import org.apache.log4j.LogManager
	import java.util.Collections
	import org.apache.log4j.Level
	import org.emoflon.ibex.tgg.common.*
	import org.emoflon.ibex.tgg.operational.csp.constraints.factories.MoflonToHenshinAttrCondDefLibrary
	
	
	import org.emoflon.ibex.tgg.abstracttggruletohenshinrulerule.AbstractTGGRuleToHenshinRuleRule_FWDMatcher
	import org.emoflon.ibex.tgg.abstracttggruletohenshinrulerule.AbstractTGGRuleToHenshinRuleRule_PROTOCOLMatcher
	import org.emoflon.ibex.tgg.abstracttggruletohenshinrulerule.AbstractTGGRuleToHenshinRuleRule_MODELGENMatcher
	import org.emoflon.ibex.tgg.abstracttggruletohenshinrulerule.AbstractTGGRuleToHenshinRuleRule_BWDMatcher
	import org.emoflon.ibex.tgg.abstracttggruletohenshinrulerule.AbstractTGGRuleToHenshinRuleRule_CCMatcher
	import org.emoflon.ibex.tgg.createtggnodetocreatehenshinnoderule.CreateTGGNodeToCreateHenshinNodeRule_FWDMatcher
	import org.emoflon.ibex.tgg.createtggnodetocreatehenshinnoderule.CreateTGGNodeToCreateHenshinNodeRule_PROTOCOLMatcher
	import org.emoflon.ibex.tgg.createtggnodetocreatehenshinnoderule.CreateTGGNodeToCreateHenshinNodeRule_MODELGENMatcher
	import org.emoflon.ibex.tgg.createtggnodetocreatehenshinnoderule.CreateTGGNodeToCreateHenshinNodeRule_BWDMatcher
	import org.emoflon.ibex.tgg.createtggnodetocreatehenshinnoderule.CreateTGGNodeToCreateHenshinNodeRule_CCMatcher
	import org.emoflon.ibex.tgg.tggruletohenshinrulerule.TGGRuleToHenshinRuleRule_FWDMatcher
	import org.emoflon.ibex.tgg.tggruletohenshinrulerule.TGGRuleToHenshinRuleRule_PROTOCOLMatcher
	import org.emoflon.ibex.tgg.tggruletohenshinrulerule.TGGRuleToHenshinRuleRule_MODELGENMatcher
	import org.emoflon.ibex.tgg.tggruletohenshinrulerule.TGGRuleToHenshinRuleRule_BWDMatcher
	import org.emoflon.ibex.tgg.tggruletohenshinrulerule.TGGRuleToHenshinRuleRule_CCMatcher
	import org.emoflon.ibex.tgg.tggtomodulerule.TGGToModuleRule_FWDMatcher
	import org.emoflon.ibex.tgg.tggtomodulerule.TGGToModuleRule_PROTOCOLMatcher
	import org.emoflon.ibex.tgg.tggtomodulerule.TGGToModuleRule_MODELGENMatcher
	import org.emoflon.ibex.tgg.tggtomodulerule.TGGToModuleRule_BWDMatcher
	import org.emoflon.ibex.tgg.tggtomodulerule.TGGToModuleRule_CCMatcher
	
	class MoflonToHenshinTransformation{	
		
		
		/* Transformation-related extensions */
		extension EventDrivenTransformation transformation
	
		/* Transformation rule-related extensions */
		extension EventDrivenTransformationRuleFactory = new EventDrivenTransformationRuleFactory
	
		protected ViatraQueryEngine engine
		protected Resource resource
	
		private TGGRuntimeUtil tggRuntimeUtil;
		
		// protected EventDrivenTransformationRule<?,?> exampleRule
		new(ResourceSet set, TGGRuntimeUtil tggRuntimeUtil) {
			this.resource = resource
			this.tggRuntimeUtil = tggRuntimeUtil
			tggRuntimeUtil.getCSPProvider().registerFactory(new MoflonToHenshinAttrCondDefLibrary());
			
			// Create EMF scope and EMF IncQuery engine based on the resource
			val scope = new EMFScope(set)
			engine = ViatraQueryEngine.on(scope);
			
			val loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
			loggers.add(LogManager.getRootLogger());
			for ( Logger logger : loggers ) {
			   logger.setLevel(Level.OFF);
			}
		
			createTransformation
		
		}
		
		public def execute() {
			transformation.executionSchema.startUnscheduledExecution
		}
		
		def dispose() {
			if (transformation != null) {
				transformation.dispose
			}
			transformation = null
			return
		}
		
		private def createTransformation() {
			// Initialize event-driven transformation
			transformation = EventDrivenTransformation.forEngine(engine).addRules(getTransformationRuleGroup).build
		}
		
		private def getTransformationRuleGroup() {
			if (tggRuntimeUtil.mode == OperationMode.FWD || tggRuntimeUtil.mode == OperationMode.BWD)
				return getSynch
			else if (tggRuntimeUtil.mode == OperationMode.MODELGEN)
				return get_MODELGEN
			else if (tggRuntimeUtil.mode == OperationMode.CC)
				return get_CC
		}
		
		private def getSynch() {
			new EventDrivenTransformationRuleGroup(
				getAbstractTGGRuleToHenshinRuleRule_FWD(),
				getAbstractTGGRuleToHenshinRuleRule_BWD(),
				getAbstractTGGRuleToHenshinRuleRule_PROTOCOL(), 
				getCreateTGGNodeToCreateHenshinNodeRule_FWD(),
				getCreateTGGNodeToCreateHenshinNodeRule_BWD(),
				getCreateTGGNodeToCreateHenshinNodeRule_PROTOCOL(), 
				getTGGRuleToHenshinRuleRule_FWD(),
				getTGGRuleToHenshinRuleRule_BWD(),
				getTGGRuleToHenshinRuleRule_PROTOCOL(), 
				getTGGToModuleRule_FWD(),
				getTGGToModuleRule_BWD(),
				getTGGToModuleRule_PROTOCOL()
				)
		}
		
		private def get_MODELGEN(){
			new EventDrivenTransformationRuleGroup(
				getAbstractTGGRuleToHenshinRuleRule_MODELGEN(), 
				getCreateTGGNodeToCreateHenshinNodeRule_MODELGEN(), 
				getTGGRuleToHenshinRuleRule_MODELGEN(), 
				getTGGToModuleRule_MODELGEN()
			 )
		}
		
		private def get_CC(){
			new EventDrivenTransformationRuleGroup(
				getAbstractTGGRuleToHenshinRuleRule_CC(), 
				getCreateTGGNodeToCreateHenshinNodeRule_CC(), 
				getTGGRuleToHenshinRuleRule_CC(), 
				getTGGToModuleRule_CC()
			)
		}
			
		
		private def getAbstractTGGRuleToHenshinRuleRule_FWD() {
			createRule.name("AbstractTGGRuleToHenshinRuleRule_FWD").precondition(AbstractTGGRuleToHenshinRuleRule_FWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("AbstractTGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getCreateTGGNodeToCreateHenshinNodeRule_FWD() {
			createRule.name("CreateTGGNodeToCreateHenshinNodeRule_FWD").precondition(CreateTGGNodeToCreateHenshinNodeRule_FWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("CreateTGGNodeToCreateHenshinNodeRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGRuleToHenshinRuleRule_FWD() {
			createRule.name("TGGRuleToHenshinRuleRule_FWD").precondition(TGGRuleToHenshinRuleRule_FWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGToModuleRule_FWD() {
			createRule.name("TGGToModuleRule_FWD").precondition(TGGToModuleRule_FWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGToModuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		
		private def getAbstractTGGRuleToHenshinRuleRule_PROTOCOL() {
			createRule.name("AbstractTGGRuleToHenshinRuleRule_PROTOCOL").precondition(AbstractTGGRuleToHenshinRuleRule_PROTOCOLMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) []
				.action(CRUDActivationStateEnum.DELETED)[
				tggRuntimeUtil.addBrokenMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getCreateTGGNodeToCreateHenshinNodeRule_PROTOCOL() {
			createRule.name("CreateTGGNodeToCreateHenshinNodeRule_PROTOCOL").precondition(CreateTGGNodeToCreateHenshinNodeRule_PROTOCOLMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) []
				.action(CRUDActivationStateEnum.DELETED)[
				tggRuntimeUtil.addBrokenMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGRuleToHenshinRuleRule_PROTOCOL() {
			createRule.name("TGGRuleToHenshinRuleRule_PROTOCOL").precondition(TGGRuleToHenshinRuleRule_PROTOCOLMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) []
				.action(CRUDActivationStateEnum.DELETED)[
				tggRuntimeUtil.addBrokenMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGToModuleRule_PROTOCOL() {
			createRule.name("TGGToModuleRule_PROTOCOL").precondition(TGGToModuleRule_PROTOCOLMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) []
				.action(CRUDActivationStateEnum.DELETED)[
				tggRuntimeUtil.addBrokenMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		
		private def getAbstractTGGRuleToHenshinRuleRule_MODELGEN() {
			createRule.name("AbstractTGGRuleToHenshinRuleRule_MODELGEN").precondition(AbstractTGGRuleToHenshinRuleRule_MODELGENMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("AbstractTGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getCreateTGGNodeToCreateHenshinNodeRule_MODELGEN() {
			createRule.name("CreateTGGNodeToCreateHenshinNodeRule_MODELGEN").precondition(CreateTGGNodeToCreateHenshinNodeRule_MODELGENMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("CreateTGGNodeToCreateHenshinNodeRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGRuleToHenshinRuleRule_MODELGEN() {
			createRule.name("TGGRuleToHenshinRuleRule_MODELGEN").precondition(TGGRuleToHenshinRuleRule_MODELGENMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGToModuleRule_MODELGEN() {
			createRule.name("TGGToModuleRule_MODELGEN").precondition(TGGToModuleRule_MODELGENMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGToModuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		
		private def getAbstractTGGRuleToHenshinRuleRule_BWD() {
			createRule.name("AbstractTGGRuleToHenshinRuleRule_BWD").precondition(AbstractTGGRuleToHenshinRuleRule_BWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("AbstractTGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getCreateTGGNodeToCreateHenshinNodeRule_BWD() {
			createRule.name("CreateTGGNodeToCreateHenshinNodeRule_BWD").precondition(CreateTGGNodeToCreateHenshinNodeRule_BWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("CreateTGGNodeToCreateHenshinNodeRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGRuleToHenshinRuleRule_BWD() {
			createRule.name("TGGRuleToHenshinRuleRule_BWD").precondition(TGGRuleToHenshinRuleRule_BWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGToModuleRule_BWD() {
			createRule.name("TGGToModuleRule_BWD").precondition(TGGToModuleRule_BWDMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGToModuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		
		private def getAbstractTGGRuleToHenshinRuleRule_CC() {
			createRule.name("AbstractTGGRuleToHenshinRuleRule_CC").precondition(AbstractTGGRuleToHenshinRuleRule_CCMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("AbstractTGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getCreateTGGNodeToCreateHenshinNodeRule_CC() {
			createRule.name("CreateTGGNodeToCreateHenshinNodeRule_CC").precondition(CreateTGGNodeToCreateHenshinNodeRule_CCMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("CreateTGGNodeToCreateHenshinNodeRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGRuleToHenshinRuleRule_CC() {
			createRule.name("TGGRuleToHenshinRuleRule_CC").precondition(TGGRuleToHenshinRuleRule_CCMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGRuleToHenshinRuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		private def getTGGToModuleRule_CC() {
			createRule.name("TGGToModuleRule_CC").precondition(TGGToModuleRule_CCMatcher.querySpecification).action(
				CRUDActivationStateEnum.CREATED) [
				         tggRuntimeUtil.addOperationalRuleMatch("TGGToModuleRule", it)
				].action(CRUDActivationStateEnum.DELETED)[
				         tggRuntimeUtil.removeOperationalRuleMatch(it)]
			.addLifeCycle(				
				Lifecycles.getDefault(false, true)
				).build
		}
		
	}		
	
