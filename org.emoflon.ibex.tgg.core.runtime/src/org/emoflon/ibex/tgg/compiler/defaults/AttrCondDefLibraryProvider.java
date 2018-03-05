package org.emoflon.ibex.tgg.compiler.defaults;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.emoflon.ibex.tgg.core.transformation.TGGProject;
import org.moflon.core.utilities.LogUtils;
import org.moflon.core.utilities.WorkspaceHelper;

import language.csp.definition.TGGAttributeConstraintDefinition;

public class AttrCondDefLibraryProvider {
	private static final Logger logger = Logger.getLogger(AttrCondDefLibraryProvider.class);
	private static final String USER_ATTR_COND_DEF_FACTORY_PATH = "src/org/emoflon/ibex/tgg/operational/csp/constraints/factories/UserDefinedRuntimeTGGAttrConstraintFactory.java";
	private static final String USER_ATTR_CONDS_PATH = "src/org/emoflon/ibex/tgg/operational/csp/constraints/custom/";

	public static void userAttrCondDefFactory(IProject project, Collection<String> userDefConstraints) throws CoreException, IOException {
		String path = USER_ATTR_COND_DEF_FACTORY_PATH;
		String userLib = DefaultFilesGenerator.generateUserRuntimeAttrCondFactory(userDefConstraints);
		IPath pathToLib = new Path(path);
		WorkspaceHelper.addAllFoldersAndFile(project, pathToLib, userLib, new NullProgressMonitor());
	}

	public static void userAttrCondDefStubs(IProject project, Collection<TGGAttributeConstraintDefinition> userDefConstraints) throws CoreException, IOException {
		for(TGGAttributeConstraintDefinition tacd : userDefConstraints) {
			String path = USER_ATTR_CONDS_PATH + UserAttrCondHelper.getFileName(tacd.getName()) + ".java";
			String userLib = DefaultFilesGenerator.generateUserAttrCondDefStub(tacd);
			IPath pathToLib = new Path(path);
			IFile userAttrLibFile = project.getFile(pathToLib);
			if (!userAttrLibFile.exists()) {
				WorkspaceHelper.addAllFoldersAndFile(project, pathToLib, userLib, new NullProgressMonitor());
			}
		}
	}
	
	public void generateAttrCondLibsAndStubs(TGGProject internalModel, IProject project) {
		Collection<TGGAttributeConstraintDefinition> userAttrCondDefs = internalModel.getTggModel()
				.getAttributeConstraintDefinitionLibrary()
				.getTggAttributeConstraintDefinitions()
				.stream()
				.filter(ac -> ac.isUserDefined())
				.collect(Collectors.toList());
		Collection<String> userAttrCondNames = userAttrCondDefs.stream()
				.map(udc -> udc.getName())
				.collect(Collectors.toList());
		try {
			AttrCondDefLibraryProvider.userAttrCondDefFactory(project, userAttrCondNames);
			AttrCondDefLibraryProvider.userAttrCondDefStubs(project, userAttrCondDefs);
		} catch (CoreException | IOException e) {
			LogUtils.error(logger, e);
		}
	}
}