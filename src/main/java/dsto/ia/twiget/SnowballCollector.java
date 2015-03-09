package dsto.ia.twiget;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.codehaus.jackson.map.ObjectMapper;

import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SnowballCollector extends AbstractTwitterApp
{
  public static final int FOLLOWER_COUNT_THRESHOLD = 50001;

  public Neighbourhood collectNeighbourhoodOf (Object seed) // seed is String handle or Long userID
  {
    Neighbourhood neighbourhood = new Neighbourhood ();
    neighbourhood.profile = lookupProfile (seed);

    if (neighbourhood.profile == null)
    {
      LOG.warn ("Lookup of user @" + seed + " failed. Check log.");

    } else
    {
      // collect follower/ee stuff
      Utils.fetchFollowerIds (twitter, neighbourhood.profile.getId (), neighbourhood.followerIDs, true);
      Utils.fetchFolloweeIds (twitter, neighbourhood.profile.getId (), neighbourhood.followeeIDs, true);
    }

    return neighbourhood;
  }

  public MultiMap collectNetworkFrom (String seedHandle, int toLevel)
  {
    return collectNetworkFrom (seedHandle, false, toLevel);
  }

  public MultiMap collectNetworkFrom (String seedHandle, boolean backOne, int toLevel)
  {
    MultiMap follows = MultiValueMap.decorate (Maps.newHashMap (), FactoryUtils.prototypeFactory (Sets.newHashSet ()));

    // assert toLevel >= 1
    if (toLevel < 1) return follows;

    User seed = lookupProfile (seedHandle);

    if (seed == null)
    {
      LOG.warn ("Lookup of user @" + seedHandle + " failed. Check log.");
      return follows; // lookup failed
    }

    long userID = seed.getId ();
    Set<Long> followerIDs = Sets.newHashSet ();

    if (backOne) followerIDs.addAll (collectNeighbouringIds (userID, follows));

    // level 1
    for (int level = 1; toLevel >= level; level++)
    {
      if (level == 1 && followerIDs.isEmpty ()) followerIDs = Utils.fetchFollowerIds (twitter, userID);

      Set<Long> currentFollowerIDs = Sets.newHashSet (followerIDs);
      followerIDs.clear ();

      for (Long id : currentFollowerIDs)
        followerIDs.addAll (collectNeighbouringIds (id, follows));
    }

    return follows;
  }

  /**
   * COllects IDs of friends and followers of the given Twitter userID. Returns the set of userID's followers (for
   * deeper traversal).
   *
   * @param userID The seed user's Twitter ID.
   * @param follows The bidirectional map of which IDs follow which other IDs.
   * @return The set of IDs of userID's followers.
   */
  private Set<Long> collectNeighbouringIds (long userID, MultiMap follows)
  {
    try
    {
      initialise ();

      Set<Long> followerIDs = Utils.fetchFollowerIds (twitter, userID);

      for (Long id : followerIDs)
        follows.put (id, userID);

      Set<Long> followeeIDs = Utils.fetchFolloweeIds (twitter, userID);

      for (Long id : followeeIDs)
        follows.put (userID, id);

      return followerIDs;

    } catch (TwitterException e)
    {
      e.printStackTrace ();
    }
    return Collections.emptySet ();
  }

  public Set<User> collectAndExportNeighbouringProfiles (Object userID, int friendThreshold)
  {
    Set<User> users = Sets.newHashSet ();
    ObjectMapper json = new ObjectMapper ();

    try
    {
      initialise ();

      Neighbourhood n = collectNeighbourhoodOf (userID);
      String fn = n.profile.getScreenName () + "_neighbourhood_profiles.json";
      File f = new File (fn);
      Writer out = new BufferedWriter (new FileWriter (f));

      System.out.println ("Writing to " + f.getAbsolutePath ());

      out.write (json.writeValueAsString (Utils.toMap ("seed", n.getSnapshot ())) + '\n');
      // json.writeValue (out, Utils.toMap ("seed", n.getSnapshot ()));
      out.flush ();

      users.add (n.profile);

      Set<Long> ids = Sets.union (n.followeeIDs, n.followerIDs);
      Long[] idsArray = ids.toArray (new Long[ids.size ()]);

      // get the profiles (batch-style with '/lookupUsers/')
      int batchSize = 100;
      long[] batch = new long[batchSize];
      List<User> ffProfiles = Lists.newArrayList ();
      for (int i = 0; i < ids.size (); i += batchSize)
      {
        if (ids.size () - i < batchSize) batch = new long[ids.size () - i]; // shrink batch for final run

        for (int j = i; j < ids.size () && j < i + batchSize; j++)
          batch[j - i] = idsArray[j];

        ResponseList<User> profiles = twitter.lookupUsers (batch);

        // write the profiles out
        for (User profile : profiles)
        {
          out.write (json.writeValueAsString (Utils.toMap (profile.getId (), profile)) + '\n');
          out.flush ();
        }

        ffProfiles.addAll (profiles);
      }
      users.addAll (ffProfiles);

      // for each profile, check the number of friends, skip if > 2000
      for (User profile : ffProfiles)
      {
        if (friendThreshold != -1 && profile.getFriendsCount () > friendThreshold) continue;

        LOG.info (String.format ("  Grabbing @%s's %d friend and %d follower IDs\n", profile.getScreenName (),
                                 profile.getFriendsCount (), profile.getFollowersCount ()));

        // grab ids of friends and write them out, { userID: id, relationship: "follows", ids: [...] }
        Set<Long> followeeIds = Utils.fetchFolloweeIds (twitter, profile.getId ());

        out.write (json.writeValueAsString (Utils.toMap ("userID", profile.getId (), "reln", "follows", "ids",
                                                         followeeIds)) + '\n');
        out.flush ();

        if (profile.getFollowersCount () < FOLLOWER_COUNT_THRESHOLD)
        {
          // grab ids of followers and write them out, { userID: id, relationship: "has_followers", ids: [...] }
          Set<Long> followerIds = Utils.fetchFollowerIds (twitter, profile.getId ());

          out.write (json.writeValueAsString (Utils.toMap ("userID", profile.getId (), "reln", "followed_by", "ids",
                                                           followerIds)) + '\n');
          out.flush ();
        }
      }

      out.close ();

      LOG.info ("Wrote output to " + f.getAbsolutePath ());

    } catch (TwitterException | IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace ();
    }

    return users;
  }
}
