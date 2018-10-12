package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.domain.DirectoryListing;
import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.rest.common.MavenRestAssuredBaseTest;
import org.carlspring.strongbox.storage.repository.MavenRepositoryFactory;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.xml.configuration.repository.MutableMavenRepositoryConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Guido Grazioli
 */

@IntegrationTest
@ExtendWith(SpringExtension.class)
public class BrowseControllerTest
        extends MavenRestAssuredBaseTest
{
    
    private static final Logger logger = LoggerFactory.getLogger(BrowseControllerTest.class);
    
    private static final String REPOSITORY = "browsing-test-repository";
    
    @Inject
    private MavenRepositoryFactory mavenRepositoryFactory;

    @BeforeAll
    public static void setup()
            throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }
    
    private static Set<MutableRepository> getRepositoriesToClean()
    {
        Set<MutableRepository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE0, REPOSITORY, Maven2LayoutProvider.ALIAS));
        return repositories;        
    }
    
    @Override
    public void init()
            throws Exception
    {
        super.init();

        File GENERATOR_BASEDIR = new File(ConfigurationResourceResolver.getVaultDirectory() + "/local");

        MutableMavenRepositoryConfiguration mavenRepositoryConfiguration = new MutableMavenRepositoryConfiguration();
        mavenRepositoryConfiguration.setIndexingEnabled(true);

        MutableRepository repository = mavenRepositoryFactory.createRepository(REPOSITORY);
        repository.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repository.setRepositoryConfiguration(mavenRepositoryConfiguration);

        createRepository(STORAGE0, repository);
        
        generateArtifact(getRepositoryBasedir(STORAGE0, REPOSITORY).getAbsolutePath(),
                         "org.carlspring.strongbox.browsing:test-browsing",
                         new String[]{ "1.1",
                                       "3.2"  
                         }
        );      
    } 
    
    @Override
    public void shutdown()
    {
        try
        {
            closeIndexersForRepository(STORAGE0, REPOSITORY);
        }
        catch (IOException e)
        {
            throw Throwables.propagate(e);
        }
        super.shutdown();
    }
    
    @Test
    public void testGetStorages()
            throws Exception
    {

        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT;
        
        String jsonResponse = given().accept(MediaType.APPLICATION_JSON_VALUE)
                                     .when()
                                     .get(url)
                                     .prettyPeek()
                                     .asString();

        DirectoryListing returned = new ObjectMapper().readValue(jsonResponse, DirectoryListing.class);
              
        assertNotNull("Failed to get storage list!", returned);
        assertNotNull("Failed to get storage list!", returned.getDirectories());
        assertFalse("Returned storage size does not match", returned.getDirectories().isEmpty());
        
        String htmlResponse = given().accept(MediaType.TEXT_HTML_VALUE)
                                     .when()
                                     .get(url + "/")
                                     .prettyPeek()
                                     .then()
                                     .statusCode(200)
                                     .and()
                                     .extract()
                                     .asString();
       
        assertTrue("Returned HTML is incorrect", htmlResponse.contains("storage0"));
    }

    @Test
    public void testGetRepositories()
            throws Exception
    {
        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT + "/" + STORAGE0;
        
        String jsonResponse = given().accept(MediaType.APPLICATION_JSON_VALUE)
                                     .when()
                                     .get(url)
                                     .prettyPeek()
                                     .asString();

        DirectoryListing returned = new ObjectMapper().readValue(jsonResponse, DirectoryListing.class);
        
        assertNotNull("Failed to get repository list!", returned);
        assertNotNull("Failed to get repository list!", returned.getDirectories());
        assertTrue("Returned repositories do not match", !returned.getDirectories().isEmpty());
        assertTrue("Repository not found", returned.getDirectories()
                                                   .stream()
                                                   .anyMatch(p -> p.getName().equals(REPOSITORY)));
        
        String htmlResponse = given().accept(MediaType.TEXT_HTML_VALUE)
                                     .when()
                                     .get(url + "/")
                                     .prettyPeek()
                                     .then()
                                     .statusCode(200)
                                     .and()
                                     .extract()
                                     .asString();

        assertTrue("Returned HTML is incorrect", htmlResponse.contains(REPOSITORY));
}
                                 


    @Test
    public void testGetRepositoriesWithStorageNotFound()
    {
        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT + "/storagefoo";
        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);
        
        given().accept(MediaType.TEXT_HTML_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);
    }

    @Test
    public void testRepositoryContents()
            throws Exception
    {
        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT + "/" + STORAGE0 + "/" + REPOSITORY
                     + "/org/carlspring/strongbox/browsing/test-browsing/1.1";
        
        String jsonResponse = given().accept(MediaType.APPLICATION_JSON_VALUE)
                                     .when()
                                     .get(url)
                                     .prettyPeek()
                                     .asString();
        
        DirectoryListing returned = new ObjectMapper().readValue(jsonResponse, DirectoryListing.class);
        
        assertTrue("Invalid files returned", returned.getFiles().size() == 6
                        && returned.getFiles().get(0).getName().equals("test-browsing-1.1.jar"));                                                        
    
        String htmlResponse = given().accept(MediaType.TEXT_HTML_VALUE)
                                     .when()
                                     .get(url + "/")
                                     .prettyPeek()
                                     .asString();

        String link = getContextBaseUrl() + "/storages/"
                      + STORAGE0 + "/" + REPOSITORY + "/org/carlspring/strongbox/browsing/test-browsing/1.1/test-browsing-1.1.jar";

        assertTrue("Expected to have found [ " + link + " ] in the response html", htmlResponse.contains(link));

    }

    @Test
    public void testRepositoryContentsWithRepositoryNotFound()
    {
        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT + "/storage0/repofoo";
        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);   
                
        given().accept(MediaType.TEXT_HTML_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);   
    }
  
    @Test
    public void testRepositoryContentsWithPathNotFound()
    {
        String url = getContextBaseUrl() + BrowseController.ROOT_CONTEXT + "/storage0/releases/foo/bar";
        given().accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);
       
        given().accept(MediaType.TEXT_HTML_VALUE)
               .when()
               .get(url)
               .prettyPeek()
               .then()
               .statusCode(404);        
    }
}
