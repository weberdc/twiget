package dsto.ia.twiget;

import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

public class ProfileWalker
{

  private static Logger LOG = LogManager.getLogger ("dsto.ia.twiget");

  @SuppressWarnings ("unused")
  private String seedHandle;
  private int followersLimit;
  private int friendsLimit;
  private Map<String, ProfileScraper> profilesByName = Maps.newHashMap ();
  private Map<Long, ProfileScraper> profilesById = Maps.newHashMap ();

  public ProfileWalker (String handle)
  {
    this.seedHandle = handle;
  }

  public void setSkipFollowersThreshold (int threshold)
  {
    this.followersLimit = threshold;
  }

  public void setSkipFriendsThreshold (int threshold)
  {
    this.friendsLimit = threshold;
  }

  public long getId (String handle)
  {
    return lookupProfileBy (handle).getTargetId ();
  }

  public int getNumFollowers (String handle)
  {
    return lookupProfileBy (handle).getNumFollowers ();
  }

  public Map<Long, ProfileScraper> getProfilesById ()
  {
    return profilesById;
  }

  private ProfileScraper lookupProfileBy (String handle)
  {
    ProfileScraper profile = profilesByName.get (handle);
    if (profile == null)
    {
      profile = new ProfileScraper (handle);
      profilesByName.put (handle, profile);
      profilesById.put (profile.getTargetId (), profile);
    }
    return profile;
  }

  private ProfileScraper lookupProfileBy (long id)
  {
    ProfileScraper profile = profilesById.get (id);
    if (profile == null)
    {
      profile = new ProfileScraper (id);
      profilesByName.put (profile.getTargetHandle (), profile);
      profilesById.put (id, profile);
    }
    return profile;
  }

  public void buildFollowerNetwork (long seedId)
  {
    ProfileScraper profile = lookupProfileBy (seedId);
    // User user = profile.getUser ();
    Set<Long> followerIds = profile.getFollowerIds ();
    Set<Long> friendIds = profile.getFriendIds ();

    LOG.info (toJSON (profile.getSnapshot ()));

    traverseNeighbours (followerIds);
    traverseNeighbours (friendIds);

    System.out.println ("Walker has accumulated " + profilesById.size () + " profiles.");
  }

  private String toJSON (Map<String, Object> snapshot)
  {
    StringWriter writer = new StringWriter (1000);

    Utils.persist (snapshot, writer);

    return writer.toString ();
  }

  private void traverseNeighbours (Set<Long> ids)
  {
    for (long neighbourId : ids)
    {
      ProfileScraper neighbour = lookupProfileBy (neighbourId);
      String handle = neighbour.getTargetHandle ();
      int numFollowers = neighbour.getUser ().getFollowersCount ();
      int numFollowees = neighbour.getUser ().getFriendsCount ();

      System.out.println ("User " + neighbourId + " has " + numFollowers + " followers, and follows " + numFollowees);

      if (numFollowers < followersLimit)
      {
        System.out.println ("  grabbing " + numFollowers + " followers");
        // neighbour.getFollowerIds (); // cache
      } else
      {
        System.out.println ("  skipping " + numFollowers + " followers");
      }
      if (numFollowees < friendsLimit)
      {
        System.out.println ("  grabbing " + numFollowees + " followers");
        // neighbour.getFriendIds (); // cache
      } else
      {
        System.out.println ("  skipping " + numFollowees + " followers");
      }

      LOG.info (toJSON (neighbour.getSnapshot ()));

      if (numFollowers < followersLimit)
      {
        for (long neighbourFollowerId : neighbour.getFollowerIds ())
          lookupProfileBy (neighbourFollowerId);
      } else
      {
        System.out.println ("Skipping @" + handle + "'s " + numFollowers + " followers.");
      }
      if (numFollowees < friendsLimit)
      {
        for (long neighbourFriendId : neighbour.getFriendIds ())
          lookupProfileBy (neighbourFriendId);
      } else
      {
        System.out.println ("Skipping @" + handle + "'s " + numFollowees + " friends.");
      }
    }
  }

  public Map<String, Object> getSnapshot ()
  {
    Map<String, Object> snapshot = Maps.newHashMap ();

    for (Long id : profilesById.keySet ())
      snapshot.put (String.valueOf (id), profilesById.get (id).getSnapshot ());

    return snapshot;
  }
}
