package reach.backend.Music;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;

import java.util.logging.Logger;

import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "musicVisibilityApi",
        version = "v1",
        resource = "musicVisibility",
        namespace = @ApiNamespace(
                ownerDomain = "Music.backend.reach",
                ownerName = "Music.backend.reach",
                packagePath = ""
        )
)
public class MusicVisibilityEndpoint {

    private static final Logger logger = Logger.getLogger(MusicVisibilityEndpoint.class.getName());

    /**
     * Returns the {@link MusicVisibility} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code MusicVisibility} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MusicVisibility get(@Named("id") long id) throws NotFoundException {
        logger.info("Getting MusicVisibility with ID: " + id);
        MusicVisibility musicVisibility = ofy().load().type(MusicVisibility.class).id(id).now();
        if (musicVisibility == null) {
            throw new NotFoundException("Could not find MusicVisibility with ID: " + id);
        }
        return musicVisibility;
    }

    /**
     * Inserts a new {@code MusicVisibility}.
     */
    @ApiMethod(
            name = "insert",
            path = "musicVisibility",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MusicVisibility insert(MusicVisibility musicVisibility) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that musicVisibility.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(musicVisibility).now();
        logger.info("Created MusicVisibility with ID: " + musicVisibility.getId());

        return ofy().load().entity(musicVisibility).now();
    }

    /**
     * Updates an existing {@code MusicVisibility}.
     *
     * @param id         the ID of the entity to be updated
     * @param musicId    the ID of the music object
     * @param visibility the desired state of the entity
     * @return false : re-run musicScanner, true : OK
     */
    @ApiMethod(
            name = "update",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MyString update(@Named("id") long id,
                           @Named("musicId") int musicId,
                           @Named("visibility") boolean visibility) {

        MusicVisibility musicVisibility = ofy().load().type(MusicVisibility.class).id(id).now();

        if (musicVisibility == null || musicVisibility.getVisibility() == null) {
            logger.info("visibility error " + id);
            return new MyString("false"); //not found, run scanner
        }

        musicVisibility.getVisibility().put(musicId, visibility);
        ofy().save().entity(musicVisibility).now();
        logger.info("Updated MusicVisibility: " + musicVisibility);
        return new MyString("true");
    }
}