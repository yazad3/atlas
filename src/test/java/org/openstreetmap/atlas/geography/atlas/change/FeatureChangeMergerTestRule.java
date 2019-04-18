package org.openstreetmap.atlas.geography.atlas.change;

import org.openstreetmap.atlas.geography.atlas.Atlas;
import org.openstreetmap.atlas.utilities.testing.CoreTestRule;
import org.openstreetmap.atlas.utilities.testing.TestAtlas;

/**
 * @author lcram
 */
public class FeatureChangeMergerTestRule extends CoreTestRule
{
    private static final String ONE = "0,0";
    private static final String TWO = "35,120";

    @SuppressWarnings("unused")
    @TestAtlas(nodes = {
            @TestAtlas.Node(id = "1", coordinates = @TestAtlas.Loc(value = ONE), tags = {
                    "delete=me", "replace=me", "country=XYZ" }),
            @TestAtlas.Node(id = "2", coordinates = @TestAtlas.Loc(value = TWO), tags = {
                    "author=abc", "country=XYZ" }) })
    private Atlas atlas;

    public Atlas getAtlas()
    {
        return this.atlas;
    }
}
