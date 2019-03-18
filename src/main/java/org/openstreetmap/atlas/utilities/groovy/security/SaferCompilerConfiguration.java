package org.openstreetmap.atlas.utilities.groovy.security;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.Eval;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A safer version of the compiler configuration that is very conservative about what is allowed to run in Groovy.
 *
 * @author Yazad Khambata
 */
public enum SaferCompilerConfiguration {
    instance;

    public static SaferCompilerConfiguration getInstance() {
        return instance;
    }

    public CompilerConfiguration groovyCompilerConfiguration(final Optional<List<String>> starImportsWhitelist, final Optional<List<String>> starImports, final Optional<List<String>> optionalStaticStarImports) {
        final SecureASTCustomizer secureASTCustomizer = getSecureASTCustomizer(starImportsWhitelist);
        final ImportCustomizer importCustomizer = getImportCustomizer(starImports, optionalStaticStarImports);
        return getCompilerConfiguration(secureASTCustomizer, importCustomizer);
    }

    private ImportCustomizer getImportCustomizer(final Optional<List<String>> optionalStarImports, final Optional<List<String>> optionalStaticStarImports) {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        optionalStarImports.ifPresent(starImports -> importCustomizer.addStarImports(starImports.stream().toArray(String[]::new)));
        optionalStaticStarImports.ifPresent(staticStarImports -> importCustomizer.addStaticStars(staticStarImports.stream().toArray(String[]::new)));
        return importCustomizer;
    }

    private SecureASTCustomizer getSecureASTCustomizer(final Optional<List<String>> optionalStarImportsWhitelist) {
        final SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        optionalStarImportsWhitelist.ifPresent(starImportsWhitelist -> secureASTCustomizer.setStarImportsWhitelist(starImportsWhitelist));
        secureASTCustomizer.setPackageAllowed(false);
        secureASTCustomizer.setMethodDefinitionAllowed(false);
        secureASTCustomizer.setIndirectImportCheckEnabled(true);
        secureASTCustomizer.setReceiversClassesBlackList(Arrays.asList(Script.class, GroovyShell.class, Eval.class, System.class, Class.class));
        secureASTCustomizer.setExpressionsBlacklist(Arrays.asList(MethodPointerExpression.class, ConstructorCallExpression.class));
        return secureASTCustomizer;
    }

    private CompilerConfiguration getCompilerConfiguration(final SecureASTCustomizer secureASTCustomizer, final ImportCustomizer importCustomizer) {
        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);
        compilerConfiguration.addCompilationCustomizers(importCustomizer);
        return compilerConfiguration;
    }
}