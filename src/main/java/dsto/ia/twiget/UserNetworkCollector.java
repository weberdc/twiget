package dsto.ia.twiget;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.collections.MultiMap;
import org.apache.logging.log4j.core.util.Integers;

import twitter4j.TwitterException;

public class UserNetworkCollector extends AbstractTwitterApp
{

  public static void main (String[] args) throws TwitterException, IOException
  {
    if (args.length < 1)
    {
      System.out.println ("Usage: UserNetworkCollector screen_name [depth (> 1)]");
      return;
    }

    String seedHandle = args[0];
    int depth = (args.length > 1) ? Integers.parseInt (args[1], 1) : 1;

    SnowballCollector collector = new SnowballCollector ();
    collector.initialise ();

    MultiMap mmap = collector.collectNetworkFrom (seedHandle, true, depth); // profile, follower IDs, followee IDs

    System.out.println ("Num users traversed: " + mmap.size ());

    String jsonFile = String.format ("%s-network-%d-%s.json", seedHandle, depth, Utils.format (new Date ()));
    BufferedWriter out = Files.newBufferedWriter (Paths.get (jsonFile));

    LOG.info ("Writing corpus to " + jsonFile);

    Utils.persist (mmap, out);

    out.close ();

    LOG.info ("Wrote corpus to " + jsonFile);
  }
}
