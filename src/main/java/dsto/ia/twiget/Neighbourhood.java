package dsto.ia.twiget;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import twitter4j.User;

public class Neighbourhood
{
  public User profile;
  public final Set<Long> followerIDs = Sets.newHashSet ();
  public final Set<Long> followeeIDs = Sets.newHashSet ();

  public Neighbourhood ()
  {
    // do nothing contructor
  }

  public Neighbourhood (User profile, Collection<Long> followeeIDs, Collection<Long> followerIDs)
  {
    this.profile = profile;
    this.followeeIDs.addAll (followeeIDs);
    this.followerIDs.addAll (followerIDs);
  }

  public Map<String, Object> getSnapshot ()
  {
    Map<String, Object> m = Maps.newHashMap ();

    m.put ("user", profile);
    m.put ("followerIDs", followerIDs);
    m.put ("followeeIDs", followeeIDs);

    return m;
  }
}
