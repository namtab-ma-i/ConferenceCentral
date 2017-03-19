package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;
import static com.google.devrel.training.conference.service.OfyService.factory;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import java.util.*;


@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
		Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

	private static String extractDefaultDisplayNameFromEmail(String email) {
		return email == null ? null : email.substring(0, email.indexOf("@"));
	}

	 /**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     * @param user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List queryConferences(ConferenceQueryForm conferenceQueryForm) { 
    	Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery(); 
    	List<Conference> result = new ArrayList<>(0); 
    	List<Key<Profile>> organizersKeyList = new ArrayList<>(0); 
    	for (Conference conference : conferenceIterable) { 
    		organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId())); 
    		result.add(conference); 
    	} 
    	// To avoid separate datastore gets for each Conference, pre-fetch the Profiles. 
    	ofy().load().keys(organizersKeyList); 
    	return result; 
    }


    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user)
    		throws UnauthorizedException {
    	if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
    	Key<Profile> profileKey = Key.create(Profile.class, user.getUserId());
        Query query = ofy().load().type(Conference.class).ancestor(profileKey);
        return query.list();
    }
    
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences() {
        Query query = ofy().load().type(Conference.class).order("name");
        return query.list();
    }
/**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        String userId = user.getUserId();

        Key<Profile> profileKey = Key.create(Profile.class,userId);

        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        final long conferenceId = conferenceKey.getId();

        Profile profile = ofy().load().key(Key.create(Profile.class, userId))
				.now();

        if (profile == null) { 
        	profile = new Profile(userId, "Меховая булочка", user.getEmail(), TeeShirtSize.NOT_SPECIFIED); 
        }

		Conference conference = new Conference(conferenceId, userId, conferenceForm);

		ofy().save().entity(conference).now();

        return conference;
    }

	@ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
	public Profile saveProfile(final User user, ProfileForm profileForm)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		String mainEmail = user.getEmail();
		String userId = user.getUserId();

		String displayName = profileForm.getDisplayName();
		TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

		Profile profile = ofy().load().key(Key.create(Profile.class, userId))
				.now();

		if (profile == null) {
			profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
		} else {
			profile.update(displayName, teeShirtSize);
		}

		ofy().save().entity(profile).now();

		return profile;
	}

	@ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
	public Profile getProfile(final User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}

		String userId = user.getUserId();
		Key key = Key.create(Profile.class, userId);

		Profile profile = (Profile) ofy().load().key(key).now();
		return profile;
	}
}