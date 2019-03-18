package org.openstreetmap.atlas.utilities.groovy.security;

import org.openstreetmap.atlas.geography.atlas.Atlas;
import org.openstreetmap.atlas.utilities.testing.CoreTestRule;
import org.openstreetmap.atlas.utilities.testing.TestAtlas;

/**
 * System.exit(N), Eval, Shell tests are inspired by https://github
 * .com/groovy/groovy-core/blob/master/src/examples/groovyShell/BlacklistingShellTest.groovy.
 *
 * @author Yazad Khambata
 */
public class SaferCompilerConfigurationTestRule extends CoreTestRule {
    private static final String ONE = "37.780574, -122.472852";
    private static final String TWO = "37.780592, -122.472242";
    private static final String THREE = "37.780724, -122.472249";

    @TestAtlas(
            nodes = {

                    @TestAtlas.Node(id = "1", coordinates = @TestAtlas.Loc(value = ONE)),
                    @TestAtlas.Node(id = "2", coordinates = @TestAtlas.Loc(value = TWO)),
                    @TestAtlas.Node(id = "3", coordinates = @TestAtlas.Loc(value = THREE))

            })
    @SuppressWarnings("unused")
    private Atlas atlas;

    public Atlas getAtlas() {
        return this.atlas;
    }
}
