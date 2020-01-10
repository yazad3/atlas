package org.openstreetmap.atlas.geography.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openstreetmap.atlas.exception.CoreException;
import org.openstreetmap.atlas.geography.atlas.builder.text.TextAtlasBuilder;
import org.openstreetmap.atlas.geography.atlas.items.AtlasEntity;
import org.openstreetmap.atlas.geography.atlas.multi.MultiAtlas;
import org.openstreetmap.atlas.geography.atlas.packed.PackedAtlas;
import org.openstreetmap.atlas.geography.atlas.sub.AtlasCutType;
import org.openstreetmap.atlas.streaming.resource.File;
import org.openstreetmap.atlas.streaming.resource.FileSuffix;
import org.openstreetmap.atlas.streaming.resource.Resource;
import org.openstreetmap.atlas.utilities.collections.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load an {@link Atlas} from a {@link Resource} or an {@link Iterable} of {@link Resource}s. Also
 * supports loading based on a resource name filter. To recursively load all {@link Atlas}es in a
 * directory, see the {@link AtlasResourceLoader2#loadRecursively} method.
 *
 * @author lcram
 */
public class AtlasResourceLoader2
{
    private static final Logger logger = LoggerFactory.getLogger(AtlasResourceLoader2.class);
    private static final Predicate<Resource> LOOKS_LIKE_TEXT_ATLAS = resource ->
    {
        checkFileExistsAndIsNotDirectory(resource);
        return resource.firstLine().equals(TextAtlasBuilder.getNodesHeader());
    };

    private Predicate<Resource> resourceFilter;
    private Predicate<AtlasEntity> atlasEntityFilter;
    private String multiAtlasName;

    private static void checkFileExistsAndIsNotDirectory(final Resource resource)
    {
        if (resource instanceof File)
        {
            final File fileResource = (File) resource;
            if (!fileResource.exists())
            {
                throw new CoreException("Resource {} was of type File but it could not be found",
                        resource.getName());
            }
            else if (fileResource.isDirectory())
            {
                throw new CoreException("Resource {} was of type File but it was a directory",
                        resource.getName());
            }
        }
    }

    public AtlasResourceLoader2()
    {
        this.resourceFilter = resource -> true;
        this.atlasEntityFilter = null;
    }

    /**
     * Load an {@link Atlas} from the provided {@link Resource}(s). If more than one
     * {@link Resource} is provided, the method will utilize the {@link MultiAtlas} to combine them.
     * This method will fail if any of the provided {@link Resource}s do not contain a valid binary
     * or text {@link Atlas}. This method should never return null.
     *
     * @param resources
     *            the {@link Resource}(s) from which to load
     * @return the non-null loaded {@link Atlas}
     */
    public Atlas load(final Resource... resources)
    {
        return load(Iterables.from(resources));
    }

    /**
     * Load an {@link Atlas} from an {@link Iterable} of {@link Resource}s. If more than one
     * {@link Resource} is provided, the method will utilize the {@link MultiAtlas} to combine them.
     * This method will fail if any of the provided {@link Resource}s do not contain a valid binary
     * or text {@link Atlas}. This method should never return null.
     *
     * @param resources
     *            the {@link Iterable} of {@link Resource}s from which to load
     * @return the non-null loaded {@link Atlas}
     */
    public Atlas load(final Iterable<? extends Resource> resources)
    {
        final List<Resource> atlasResources = Iterables.stream(resources)
                .flatMap(this::upcastAndRemoveNullResources).filter(this.resourceFilter)
                .collectToList();

        final Optional<Atlas> resultAtlasOptional;
        if (atlasResources.isEmpty())
        {
            throw new CoreException("No loadable Resources were found.");
        }
        else if (atlasResources.size() == 1)
        {
            resultAtlasOptional = loadAtlasResource(atlasResources.get(0));
        }
        else
        {
            resultAtlasOptional = loadMultipleAtlasResources(atlasResources);
        }

        if (!resultAtlasOptional.isPresent())
        {
            throw new CoreException("Unable to load atlas from provided Resources");
        }
        Atlas resultAtlas = resultAtlasOptional.get();

        // Apply the filter at the end
        if (this.atlasEntityFilter != null)
        {
            final Optional<Atlas> subAtlas = resultAtlas.subAtlas(this.atlasEntityFilter,
                    AtlasCutType.SOFT_CUT);
            resultAtlas = subAtlas.orElseThrow(
                    () -> new CoreException("Entity filter resulted in an empty atlas"));
        }
        return resultAtlas;
    }

    /**
     * Load an {@link Atlas} from the provided {@link File} {@link Resource}(s). If any of the
     * provided {@link File}(s) are directories, the method will recursively descend into the
     * directory and include every {@link Atlas} it discovers. It identifies {@link Atlas}es by
     * looking for {@link FileSuffix#ATLAS}, {@link FileSuffix#TEXT_ATLAS}, and
     * {@link FileSuffix#GZIP_ATLAS} file extensions. Like with the
     * {@link AtlasResourceLoader2#load} method, this method will utilize the {@link MultiAtlas} to
     * combine the {@link Atlas}es. This method should never return null.
     *
     * @param files
     *            the {@link File}(s) from which to load
     * @return the non-null loaded {@link Atlas}
     */
    public Atlas loadRecursively(final File... files)
    {
        return loadRecursively(Iterables.from(files));
    }

    /**
     * Load an {@link Atlas} from an {@link Iterable} of {@link File} {@link Resource}s. If any of
     * the provided {@link File}(s) are directories, the method will recursively descend into the
     * directory and include every {@link Atlas} it discovers. It identifies {@link Atlas}es by
     * looking for {@link FileSuffix#ATLAS} {@link FileSuffix#TEXT_ATLAS}, and
     * {@link FileSuffix#GZIP_ATLAS} file extensions. Like with the
     * {@link AtlasResourceLoader2#load} method, this method will utilize the {@link MultiAtlas} to
     * combine the {@link Atlas}es. This method should never return null.
     *
     * @param files
     *            the {@link Iterable} of {@link File} {@link Resource}s from which to load
     * @return the non-null loaded {@link Atlas}
     */
    public Atlas loadRecursively(final Iterable<File> files)
    {
        // TODO implement
        return null;
    }

    /**
     * // TODO fill in doc
     * 
     * @param resources
     *            the {@link Resource}(s) from which to load
     * @return an {@link Optional} wrapping the loaded {@link Atlas} if present
     */
    public Optional<Atlas> safeLoad(final Resource... resources)
    {
        // TODO fill in overloaded versions
        try
        {
            return Optional.of(load(resources));
        }
        catch (final Exception exception)
        {
            logger.warn("Could not load atlas from supplied resources", exception);
            return Optional.empty();
        }
    }

    /**
     * Optionally add an {@link AtlasEntity} filter
     *
     * @param filter
     *            filter which {@link AtlasEntity}s to include/exclude in the {@link Atlas}
     */
    public void setAtlasEntityFilter(final Predicate<AtlasEntity> filter)
    {
        this.atlasEntityFilter = filter;
    }

    /**
     * Optionally add a {@link Resource} filter
     *
     * @param filter
     *            filter which {@link Resource} to load
     */
    public void setResourceFilter(final Predicate<Resource> filter)
    {
        this.resourceFilter = filter;
    }

    /**
     * Optionally add an {@link AtlasEntity} filter
     *
     * @param filter
     *            filter which {@link AtlasEntity}s to include/exclude in the {@link Atlas}
     * @return fluent interface requires this be returned
     */
    public AtlasResourceLoader2 withAtlasEntityFilter(final Predicate<AtlasEntity> filter)
    {
        setAtlasEntityFilter(filter);
        return this;
    }

    /**
     * Set the name for the {@link MultiAtlas} that results from the load.
     *
     * @param multiAtlasName
     *            the name
     * @return instance of {@link AtlasResourceLoader2} for method chaining
     */
    public AtlasResourceLoader2 withMultiAtlasName(final String multiAtlasName)
    {
        this.multiAtlasName = multiAtlasName;
        return this;
    }

    /**
     * Optionally add a {@link Resource} filter
     *
     * @param filter
     *            filter which {@link Resource} to load
     * @return instance of {@link AtlasResourceLoader2} for method chaining
     */
    public AtlasResourceLoader2 withResourceFilter(final Predicate<Resource> filter)
    {
        setResourceFilter(filter);
        return this;
    }

    private List<Resource> filterForBinaryAtlasResources(final List<Resource> atlasResources)
    {
        return atlasResources.stream().filter(LOOKS_LIKE_TEXT_ATLAS.negate())
                .collect(Collectors.toList());
    }

    private List<Resource> filterForTextAtlasResources(final List<Resource> atlasResources)
    {
        return atlasResources.stream().filter(LOOKS_LIKE_TEXT_ATLAS).collect(Collectors.toList());
    }

    private Optional<Atlas> loadAtlasResource(final Resource resource)
    {
        final Atlas result;

        if (LOOKS_LIKE_TEXT_ATLAS.test(resource))
        {
            result = new TextAtlasBuilder().read(resource);
        }
        else
        {
            try
            {
                result = PackedAtlas.load(resource);
            }
            catch (final Exception exception)
            {
                throw new CoreException("Failed to load an atlas from {} with name {}",
                        resource.getClass().getName(), resource.getName(), exception);
            }
        }
        return Optional.ofNullable(result);
    }

    private Optional<Atlas> loadMultipleAtlasResources(final List<Resource> atlasResources)
    {
        final List<Resource> binaryResources = filterForBinaryAtlasResources(atlasResources);
        final List<Resource> textResources = filterForTextAtlasResources(atlasResources);

        /*
         * There are three scenarios that must be handled. 1) There were only binary atlases. 2)
         * There was a mix of binary and text atlases. 3) There were only text atlases.
         */
        MultiAtlas resultAtlas = null;
        if (!binaryResources.isEmpty())
        {
            resultAtlas = MultiAtlas.loadFromPackedAtlas(binaryResources);
        }
        if (!textResources.isEmpty())
        {
            final List<Atlas> textAtlases = loadTextAtlases(textResources);
            if (!textAtlases.isEmpty())
            {
                final MultiAtlas textMultiAtlas = new MultiAtlas(textAtlases);
                /*
                 * In this case, 'resultAtlas' is not null because there was a mix of binary and
                 * text atlases.
                 */
                if (resultAtlas != null)
                {
                    resultAtlas = new MultiAtlas(resultAtlas, textMultiAtlas);
                }
                /*
                 * For this case, there was no previous resultAtlas since no binary atlases were
                 * found.
                 */
                else
                {
                    resultAtlas = textMultiAtlas;
                }
            }
        }

        if (this.multiAtlasName != null && resultAtlas != null)
        {
            resultAtlas.setName(this.multiAtlasName);
        }
        return Optional.ofNullable(resultAtlas);
    }

    private List<Atlas> loadTextAtlases(final List<Resource> textAtlasResources)
    {
        final List<Atlas> textAtlases = new ArrayList<>();
        for (final Resource textResource : textAtlasResources)
        {
            final Atlas atlas = new TextAtlasBuilder().read(textResource);
            textAtlases.add(atlas);
        }
        return textAtlases;
    }

    private List<Resource> upcastAndRemoveNullResources(final Resource resource)
    {
        final List<Resource> result = new ArrayList<>();
        if (resource != null)
        {
            result.add(resource);
        }
        return result;
    }
}
