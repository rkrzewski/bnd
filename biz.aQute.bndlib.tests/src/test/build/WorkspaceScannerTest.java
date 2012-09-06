package test.build;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.build.*;
import aQute.libg.clauses.*;

public class WorkspaceScannerTest extends TestCase {

	public void testOfflineScanner() {
		Clauses clauses = Clauses.parse("root1;depth=2,root2;depth=1", null);
		OfflineWorkspaceScanner scanner = new OfflineWorkspaceScanner(clauses, new File("test/multilevel/scanner"));

		List<String> projects = scanner.findProjects();
		assertEquals(Arrays.asList("P1", "P2", "P3", "P4", "P5"), projects);
	}

	public void testMinimalTraversal() {
		Clauses clauses = Clauses.parse("root1;depth=2,root2;depth=1", null);
		OfflineWorkspaceScanner scanner = new OfflineWorkspaceScanner(clauses, new File("test/multilevel/scanner"));

		scanner.findProject("P1");
		assertEquals(2, scanner.getNumVisited());
		scanner.findProject("P2");
		assertEquals(3, scanner.getNumVisited());
		scanner.findProject("P3");
		assertEquals(5, scanner.getNumVisited());

		scanner.findProject("P1");
		assertEquals(5, scanner.getNumVisited());
		scanner.findProject("P2");
		assertEquals(5, scanner.getNumVisited());
		scanner.findProject("P3");
		assertEquals(5, scanner.getNumVisited());
	}

	public void testNameClash() {
		Clauses clauses = Clauses.parse("root1;depth=1,root2;depth=1", null);
		OfflineWorkspaceScanner scanner = new OfflineWorkspaceScanner(clauses, new File("test/multilevel/name-clash"));
		try {
			scanner.findProjects();
			fail("should throw IllegalStateException");
		}
		catch (Exception e) {
			assertEquals(IllegalStateException.class, e.getClass());
		}
	}

}
