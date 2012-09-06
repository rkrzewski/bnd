package aQute.bnd.build;

import java.io.*;
import java.util.*;

/**
 * Locates projects within a Workspace.
 * 
 * @author rafal.krzewski@caltha.pl
 */
public interface WorkspaceScanner {
	/**
	 * Find project with the specified BSN
	 * 
	 * @param bsn
	 * @return project location, or {@code null} when not found.
	 */
	File findProject(String bsn);

	/**
	 * Find all projects within the workspace
	 * 
	 * @return list of project BSNs.
	 */
	List<String> findProjects();
}
