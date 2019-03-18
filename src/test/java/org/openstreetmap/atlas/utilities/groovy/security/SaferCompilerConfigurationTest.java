package org.openstreetmap.atlas.utilities.groovy.security;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.atlas.geography.atlas.Atlas;
import org.openstreetmap.atlas.geography.atlas.BareAtlasTestRule;
import org.openstreetmap.atlas.geography.atlas.items.Node;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * System.exit(N), Eval, Shell tests are inspired by https://github
 * .com/groovy/groovy-core/blob/master/src/examples/groovyShell/BlacklistingShellTest.groovy.
 *
 * @author Yazad Khambata
 */
public class SaferCompilerConfigurationTest {

    private static final Optional<List<String>> optionalStarImportsWhitelist = Optional.empty();
    private static final Optional<List<String>> optionalStarImports = Optional.empty();
    private static final Optional<List<String>> optionalStaticStarImports = Optional.empty();

    @Rule
    public final BareAtlasTestRule rule = new BareAtlasTestRule();

    @Test
    public void sanity() {
        final GroovyShell groovyShell =
                new GroovyShell(SaferCompilerConfiguration.getInstance().groovyCompilerConfiguration(optionalStarImportsWhitelist, optionalStarImports, optionalStaticStarImports));
        final Object result = groovyShell.evaluate("10 + 20");
        Assert.assertEquals(30, result);
    }

    @Test
    public void runBooleanExpression() {
        final Atlas atlas = rule.getAtlas();
        final Iterable<Node> matchingNodesIterator = atlas.nodes(entity -> {
            final Binding binding = new Binding();
            binding.setProperty("entity", entity);
            final GroovyShell groovyShell = new GroovyShell(binding,
                    SaferCompilerConfiguration.getInstance().groovyCompilerConfiguration(optionalStarImportsWhitelist
                            , optionalStarImports, optionalStaticStarImports));
            return (Boolean) groovyShell.evaluate("entity.identifier < 2");
        });

        final List<Node> matchingNodes = StreamSupport.stream(matchingNodesIterator.spliterator(), false).collect(Collectors.toList());

        Assert.assertEquals(1, matchingNodes.size());
        Assert.assertEquals(1, matchingNodes.get(0).getIdentifier());
    }

    @Test
    public void preventDangerous01() {
        executionFails("System.exit(1)", "java.lang.SecurityException: Method calls not allowed on [java.lang.System]");
    }

    @Test
    public void preventDangerous02() {
        executionFails("Class.forName('java.lang.System').exit(0)", "Method calls not allowed on [java.lang.Class]");
    }

    @Test
    public void preventDangerous03() {
        executionFails("def e = System.&exit; e.call(0)", "MethodPointerExpressions are not allowed: java.lang.System" +
                ".&exit");
    }

    @Test
    public void preventDangerous04() {
        executionFails("System.&exit.call(0)", "MethodPointerExpressions are not allowed: java.lang.System.&exit");
    }

    @Test
    public void preventDangerous05() {
        executionFails("System.getMetaClass().invokeMethod('exit',0)", "Method calls not allowed on [java.lang.System]");
    }

//    @Test
//    public void preventDangerous06() {
//        executionFails("evaluate('System.exit(0)')", "java.lang.SecurityException: Method calls not allowed on [java.lang.Object]");
//    }

    @Test
    public void preventDangerous07() {
        executionFails("(new GroovyShell()).evaluate('System.exit(0)')", "java.lang.SecurityException: Method calls " +
                "not allowed on [groovy.lang.GroovyShell]");
    }

    @Test
    public void preventDangerous08() {
        executionFails("(new GroovyShell()).evaluate('System.exit(0)')", "java.lang.SecurityException: Method calls not allowed on [groovy.lang.GroovyShell]");
    }

    @Test
    public void preventDangerous09() {
        executionFails("def sh = new GroovyShell(); sh.evaluate('System.exit(0)')", "ConstructorCallExpressions are not allowed: new groovy.lang.GroovyShell()");
    }

    @Test
    public void preventDangerous10() {
        executionFails("Eval.me('System.exit(0)')", "java.lang.SecurityException: Method calls not allowed on [groovy.util.Eval]");
    }

//    @Test
//    public void preventDangerous11() {
//        executionFails("def s = System; s.exit(0)", "java.lang.SecurityException: Method calls not allowed on [java.lang.Object]");
//    }

    @Test
    public void preventDangerous12() {
        executionFails("Script t = this; t.evaluate('System.exit(0)')", "Method calls not allowed on [groovy.lang.Script]");
    }

    private void executionFails(final String scriptText, final String errorMessageSnippet) {
        final GroovyShell groovyShell =
                new GroovyShell(SaferCompilerConfiguration.getInstance().groovyCompilerConfiguration(optionalStarImportsWhitelist, optionalStarImports, optionalStaticStarImports));
        try {
            groovyShell.evaluate(scriptText);
            Assert.fail();
        } catch (MultipleCompilationErrorsException e) {
            Assert.assertTrue("Actual Message: " + e.getMessage(), e.getMessage().contains(errorMessageSnippet));
            return;
        }

        Assert.fail();
    }
}