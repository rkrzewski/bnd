package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.bnd.osgi.*;
import aQute.libg.clauses.*;

/**
 * {@link WorkspaceScanner} implementation used for offline builds (command line
 * BND and Ant), as opposed to builds inside an IDE.
 * <p>
 * The scanner assumes that workspace structure does not change in time. This
 * allows for an efficient implementation that performs scanning in incremental
 * fashion, and caches detected project for future queries.
 * </p>
 * <p>
 * Directory tree is traversed depth first, sibling directories are traversed in
 * lexicographical order.
 * <p/>
 * <p>
 * Project BSNs are required to be unique across the workspace. When a duplicate
 * BSN is encountered, {@link IllegalStateException} will be thrown by
 * {@link #findProject(String)} or {@link #findProjects()}. Note that the
 * scanning is done incrementally, so the exception will be thrown only when BSN
 * is encountered for the second time.
 * </p>
 * 
 * @author rafal.krzewski@caltha.pl
 */
public class OfflineWorkspaceScanner implements WorkspaceScanner {

	private final File												baseDir;
	private final Iterator<Map.Entry<String,Map<String,String>>>	clauses;
	private OfflineWorkspaceScanner.SearchClause					currentClause	= null;
	private final LinkedList<OfflineWorkspaceScanner.Traversal>		stack			= new LinkedList<Traversal>();
	private final Map<String,OfflineWorkspaceScanner.Project>		cache			= new LinkedHashMap<String,Project>();
	private int														numVisited		= 0;

	public OfflineWorkspaceScanner(Clauses clauses, File baseDir) {
		this.clauses = clauses.entrySet().iterator();
		this.baseDir = baseDir;
	}

	public File findProject(String bsn) {
		OfflineWorkspaceScanner.Project project = cache.get(bsn);
		if (project == null) {
			do {
				project = next();
			} while (project != null && !project.bsn.equals(bsn));
		}
		if (project != null) {
			return project.dir;
		}
		return null;
	}

	public List<String> findProjects() {
		if (clauses.hasNext()) {
			Project project;
			do {
				project = next();
			} while (project != null);
		}
		return new ArrayList<String>(cache.keySet());
	}

	/**
	 * Find next project in the workspace.
	 * <p>
	 * Make sure to traverse minimum number of directories possible.
	 * <p>
	 * 
	 * @return a Project
	 */
	private Project next() {
		if (stack.isEmpty()) {
			if (clauses.hasNext()) {
				currentClause = new SearchClause(clauses.next(), baseDir);
				stack.addLast(new Traversal(currentClause.dir));
			} else {
				return null;
			}
		}
		OfflineWorkspaceScanner.Traversal cur = stack.getLast();
		if (cur.hasNext()) {
			File dir = cur.next();
			if (isProject(dir)) {
				OfflineWorkspaceScanner.Project project = new Project(dir);
				Project prev = cache.get(project.bsn);
				if (prev != null) {
					throw new IllegalStateException("project " + project.bsn
							+ " appears in two places in the workspace " + prev.dir.getAbsolutePath() + " and "
							+ project.dir.getAbsolutePath());
				}
				cache.put(project.bsn, project);
				return project;
			}
			if (stack.size() < currentClause.depth) {
				stack.addLast(new Traversal(dir));
			}
		} else {
			stack.removeLast();
		}
		return next();
	}

	/**
	 * Check if a given directory contains a BND project.
	 * <p>
	 * Presence of {@value aQute.bnd.build.Project#BNDFILE} file inside the
	 * given directory is the used as the discriminator.
	 * </p>
	 * 
	 * @param dir
	 * @return
	 */
	private boolean isProject(File dir) {
		numVisited++;
		return dir.isDirectory() && new File(dir, aQute.bnd.build.Project.BNDFILE).exists();
	}

	/**
	 * Returns the number of directories visited by the scanner.
	 */
	public int getNumVisited() {
		return numVisited;
	}

	/**
	 * Scanner's internal project representation - a pair of BSN and directory.
	 */
	private static class Project {
		public final String	bsn;
		public final File	dir;

		public Project(File dir) {
			this.dir = dir;
			this.bsn = bsn(dir);
		}

		/**
		 * Project name == BSN convention is encoded here.
		 */
		private static String bsn(File projectDir) {
			return projectDir.getName();
		}

		@Override
		public String toString() {
			return bsn + " " + dir.getAbsolutePath();
		}
	}

	/**
	 * A clause in {@value Constants#PROJECT_SEARCH} instruction.
	 */
	private static class SearchClause {
		public final File	dir;
		public final int	depth;

		public SearchClause(Map.Entry<String,Map<String,String>> clause, File baseDir) {
			dir = new File(baseDir, clause.getKey());
			if (clause.getValue().containsKey(Constants.PROJECT_SEARCH_DEPTH)) {
				depth = Integer.parseInt(clause.getValue().get(Constants.PROJECT_SEARCH_DEPTH));
			} else {
				depth = 1;
			}
		}

		@Override
		public String toString() {
			return dir.getAbsolutePath() + ";depth=" + depth;
		}
	}

	/**
	 * Traversal stage - enclosing a director an iterator over it's
	 * subdirectories.
	 */
	private static class Traversal {
		public final File				dir;
		private final Iterator<File>	subdirIterator;
		private File					last;

		public Traversal(File dir) {
			this.dir = dir;
			this.subdirIterator = subdirs(dir).iterator();
		}

		public boolean hasNext() {
			return subdirIterator.hasNext();
		}

		public File next() {
			return (last = subdirIterator.next());
		}

		@Override
		public String toString() {
			return dir.getAbsolutePath() + " lastChild=" + last + " hasNext=" + subdirIterator.hasNext();
		}

		/**
		 * Returns the sub-directories of a given directory, sorted
		 * lexicographically.
		 * 
		 * @param dir
		 * @return
		 */
		private List<File> subdirs(File dir) {
			File[] children = dir.listFiles(new FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory();
				}
			});
			Arrays.sort(children, new Comparator<File>() {
				public int compare(File f1, File f2) {
					return f1.getName().compareTo(f2.getName());
				}
			});
			return Arrays.asList(children);
		}
	}
}