package org.carlspring.strongbox.dependency.snippet;

import org.carlspring.strongbox.artifact.coordinates.NugetArtifactCoordinates;
import org.carlspring.strongbox.config.NugetLayoutProviderTestConfig;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.providers.layout.NugetLayoutProvider;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author carlspring
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NugetLayoutProviderTestConfig.class)
public class NugetDependencyFormatterTest
{

    @Inject
    private CompatibleDependencyFormatRegistry compatibleDependencyFormatRegistry;


    @Test
    public void testGradleDependencyGeneration()
            throws ProviderImplementationException
    {
        DependencySynonymFormatter formatter = compatibleDependencyFormatRegistry.getProviderImplementation(NugetLayoutProvider.ALIAS,
                                                                                                            NugetDependencyFormatter.ALIAS);
        assertNotNull(formatter, "Failed to look up dependency synonym formatter!");

        NugetArtifactCoordinates coordinates = new NugetArtifactCoordinates();
        coordinates.setId("Org.Carlspring.Strongbox.NuGet.Snippet");
        coordinates.setVersion("1.0");

        String snippet = formatter.getDependencySnippet(coordinates);

        System.out.println(snippet);

        assertEquals("<dependency id=\"" + coordinates.getId() + "\" version=\"" + coordinates.getVersion() + "\" />\n",
                     snippet,
                     "Failed to generate dependency!");
    }

}
