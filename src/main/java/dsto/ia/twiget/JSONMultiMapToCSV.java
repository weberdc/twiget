package dsto.ia.twiget;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.io.Files;

public class JSONMultiMapToCSV
{
  public static void main (String[] args) throws JsonParseException, JsonMappingException, IOException
  {
    if (args.length == 0)
    {
      System.out.println ("Usage: JSONMultiMapToCSV jsonFile [csvFile]");
      return;
    }

    String jsonPath = args[0];
    File jsonFile = new File (jsonPath);
    String csvPath = (args.length > 1) ? args[1] : makeCSVEquivalent (jsonFile);
    System.out.println ("Reading JSON from: " + jsonPath);
    System.out.println ("Writing CSV to:    " + csvPath);

    ObjectMapper mapper = new ObjectMapper ();
    MultiValueMap followers = mapper.readValue (jsonFile, MultiValueMap.class);

    File out = new File (csvPath);
    if (out.exists ()) out.delete ();

    Files.append ("Follower,Followee\n", out, Charset.defaultCharset ());

    for (Object follower : followers.keySet ())
    {
      @SuppressWarnings ("unchecked")
      List<List<Long>> listOfLists = (List<List<Long>>) followers.getCollection (follower);

      List<Long> followees = listOfLists.get (0);

      for (Long followee : followees)
        Files.append (follower.toString () + ',' + followee + '\n', out, Charset.defaultCharset ());
    }

    System.out.println ("\nDONE. Output in " + out.getAbsolutePath ());
  }

  private static String makeCSVEquivalent (File jsonFile)
  {
    String dirPath = jsonFile.getParent () + System.getProperty ("file.separator");
    return dirPath + Files.getNameWithoutExtension (jsonFile.getAbsolutePath ()) + ".csv";
  }
}
