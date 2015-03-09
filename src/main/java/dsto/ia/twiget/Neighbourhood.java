package dsto.ia.twiget;

import java.util.Map;
import java.util.Set;

import twitter4j.User;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Neighbourhood
{
  public User profile;
  public final Set<Long> followerIDs = Sets.newHashSet ();
  public final Set<Long> followeeIDs = Sets.newHashSet ();

  public Map<String, Object> getSnapshot ()
  {
    Map<String, Object> m = Maps.newHashMap ();

    m.put ("user", profile);
    m.put ("followerIDs", followerIDs);
    m.put ("followeeIDs", followeeIDs);

    return m;
  }
}
