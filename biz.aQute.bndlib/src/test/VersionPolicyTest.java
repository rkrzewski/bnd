package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;

import org.osgi.service.event.*;

import aQute.lib.osgi.*;

public class VersionPolicyTest extends TestCase {

    /**
     * Test export annotation.
     */
    public void testExportAnnotation() throws Exception {
        Builder a = new Builder();
        a.addClasspath(new File("bin"));
        a.setProperty("Export-Package", "test.versionpolicy.api");
        a.build();
        Map<String,String> attrs = a.getExports().get("test.versionpolicy.api");
        assertEquals("4.3.2.1", attrs.get("version"));
        assertEquals("PrivateImpl", attrs.get("exclude:"));
        assertEquals("a", attrs.get("mandatory:"));
    }

    /**
     * Check implementation version policy.
     * 
     * Uses the package test.versionpolicy.(uses|implemented)
     */
    public void testImplementationVersionPolicies() throws Exception {
        assertPolicy("test.versionpolicy.implemented", "IMPL");
        assertPolicy("test.versionpolicy.implmajor", "USES");
        assertPolicy("test.versionpolicy.uses", "USES");
    }

    void assertPolicy(String pack, String type) throws Exception {
        Builder a = new Builder();
        a.addClasspath(new File("bin"));
        a.setProperty("Export-Package", "test.versionpolicy.api");
        Jar jar = a.build();

        Builder b = new Builder();
        b.addClasspath(jar);
        b.addClasspath(new File("bin"));

        b.setProperty("-versionpolicy-impl", "IMPL");
        b.setProperty("-versionpolicy-uses", "USES");
        b.setProperty("Private-Package", pack);
        b.build();
        Manifest m = b.getJar().getManifest();
        m.write(System.out);
        Map<String, String> map = b.getImports().get("test.versionpolicy.api");
        assertNotNull(map);
//        String s = map.get(Constants.IMPLEMENTED_DIRECTIVE);
//        assertEquals("true", s);
        Map<String, Map<String, String>> mp = Processor.parseHeader(m
                .getMainAttributes().getValue("Import-Package"), null);
        assertEquals(type, mp.get("test.versionpolicy.api").get("version"));
    }

    /**
     * hardcoded imports
     */
    public void testHardcodedImports() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.setProperty("-versionpolicy",
                "[${version;==;${@}},${version;+;${@}})");
        b.setProperty("Private-Package", "org.objectweb.asm");
        b.setProperty("Import-Package",
                "org.osgi.framework,org.objectweb.asm,abc;version=2.0.0,*");
        b.build();
        Manifest m = b.getJar().getManifest();
        m.write(System.out);
        String s = b.getImports().get("org.objectweb.asm").get("version");
        assertNull(s);
        s = b.getImports().get("abc").get("version");
        assertEquals("2.0.0", s);

        s = b.getImports().get("org.osgi.framework").get("version");
        assertEquals("[1.3,2)", s);

    }

    /**
     * Specify the version on the export and verify that the policy is applied
     * on the matching import.
     */
    public void testExportsSpecifiesVersion() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.addClasspath(new File("bin"));
        b.setProperty("Export-Package", "org.osgi.service.event");
        b.setProperty("Private-Package", "test.refer");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("[1.0,2)", s);

    }

    /**
     * See if we a can override the version from the export statement and the
     * version from the source.
     */
    public void testImportOverridesDiscoveredVersion() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.addClasspath(new File("bin"));
        b.setProperty("Export-Package", "org.osgi.service.event");
        b.setProperty("Private-Package", "test.refer");
        b.setProperty("Import-Package",
                "org.osgi.service.event;version=2.1.3.q");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("2.1.3.q", s);
    }

    /**
     * Test if we can get the version from the source and apply the default
     * policy.
     */
    public void testVersionPolicyImportedExportsDefaultPolicy()
            throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.addClasspath(new File("bin"));
        b.setProperty("Export-Package", "org.osgi.service.event");
        b.setProperty("Private-Package", "test.refer");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("[1.0,2)", s);
    }

    /**
     * Test if we can get the version from the source and apply a specific
     * policy.
     */
    public void testVersionPolicyImportedExportsWithPolicy() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.addClasspath(new File("bin"));
        b.setProperty("-versionpolicy",
                "[${version;==;${@}},${version;=+;${@}})");
        b.setProperty("Export-Package", "org.osgi.service.event");
        b.setProperty("Private-Package", "test.refer");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("[1.0,1.1)", s);
    }

    /**
     * The default policy is truncate micro. Check if this is applied to the
     * import.
     */
    public void testImportMicroTruncated() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.setProperty("Import-Package", "org.osgi.service.event");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("[1.0,2)", s);
    }

    /**
     * See if we can apply a policy to an import where we get the version from
     * the source.
     */
    public void testVersionPolicy() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.setProperty("-versionpolicy",
                "[${version;==;${@}},${version;=+;${@}})");
        b.setProperty("Import-Package", "org.osgi.service.event");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("[1.0,1.1)", s);
    }

    /**
     * Check if we can set a specific version on the import that does not use a
     * version policy.
     */
    public void testImportMicroNotTruncated() throws Exception {
        Builder b = new Builder();
        b.addClasspath(new File("jar/osgi.jar"));
        b.setProperty("Import-Package", "org.osgi.service.event;version=${@}");
        b.build();
        String s = b.getImports().get("org.osgi.service.event").get("version");
        assertEquals("1.0.1", s);
    }

}
