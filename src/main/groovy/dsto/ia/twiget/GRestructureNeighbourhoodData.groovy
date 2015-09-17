package dsto.ia.twiget

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import com.google.common.collect.Maps
import com.google.common.collect.Sets

def rootDir = 'incoming/HuT/'
def suffix = '_neighbourhood_profiles.json'
//def seedFiles = [
//  "${rootDir}hamzah_q${suffix}",
//  "${rootDir}wassimdoureihi${suffix}",
//  "${rootDir}UthmanB${suffix}"
//]
def seedFiles = ['hamzah_q', 'wassimdoureihi', 'UthmanB'].collect { rootDir + it + suffix }
def outFilename = "${rootDir}/HuT_neighbourhood_public_profiles.json"
def seedConnections = Sets.newHashSet ()

def toLong (Object o) { (o instanceof Long) ? o : Long.parseLong (o.toString ()) }

def profiles = Maps.newHashMap ()
def json = new JsonSlurper ()
seedFiles.each { f ->
  new File (f).eachLine { line ->
    def jsObj = json.parseText (line)
    if (jsObj.seed) {
      println ("found seed " + jsObj.seed.user.id + " -> @" + jsObj.seed.user.screenName)
//      println (jsObj.seed.user)
      profiles[toLong (jsObj.seed.user.id)] = [profile: jsObj.seed.user, followeeIDs: jsObj.seed.followeeIDs, followerIDs: jsObj.seed.followerIDs]
      
//      println (profiles[toLong (jsObj.seed.user.id)].profile)
      jsObj.seed.followerIDs.each { seedConnections << toLong (it) }
      jsObj.seed.followeeIDs.each { seedConnections << toLong (it) }
    } else if (jsObj.reln) {
      if (jsObj.reln == 'followed_by') {
        def neighbourhood = profiles[toLong (jsObj.userID)]
        if (! neighbourhood) {
          println ("Can't find profile for " + jsObj.userID)
        } else {
          if (! neighbourhood.containsKey ('followerIDs')) neighbourhood.followerIDs = Sets.newHashSet ()
          jsObj.ids.each { neighbourhood.followerIDs << toLong (it) }
        }
      } else {
      def neighbourhood = profiles[toLong (jsObj.userID)]
      if (! neighbourhood) {
        println ("Can't find profile for " + jsObj.userID)
      } else {
        if (! neighbourhood.containsKey ('followeeIDs')) neighbourhood.followeeIDs = Sets.newHashSet ()
        jsObj.ids.each { neighbourhood.followeeIDs << toLong (it) }
      }

      }
    } else { // follower/ee profile
      def id = jsObj.keySet ().iterator ().next ()
//      println (jsObj[id] as User)
      if (! profiles.containsKey (toLong (id)))
        profiles[toLong (id)] = [profile: jsObj[id], followeeIDs: Sets.newHashSet (), followerIDs: Sets.newHashSet ()]
    }
  }
}

println ("Collected ${profiles.size ()} profiles, but...")
def disconnected = profiles.keySet ().findAll { key ->
  profiles[key].followerIDs.size () == 0 && profiles[key].followeeIDs.size () == 0
}

println ("This many profiles had no followers or followees: " + disconnected.size ())

def inCommon = seedConnections.findAll { profiles.containsKey (toLong (it)) }
def all = Sets.newHashSet (profiles.keySet ())
all.addAll (seedConnections)
println ("# seedConnections: ${seedConnections.size ()}")
println ("${all.size ()} unique IDs.")
println ("IDs in common: ${inCommon.size ()}")

// enforce connections
profiles.keySet ().each { currentID ->
  if (!profiles[currentID])
    println ("We have a key $currentID that has no value. WTF?")
  else {
    profiles[currentID].followerIDs.each { followerID ->
      if (profiles.containsKey (followerID)) profiles[followerID].followeeIDs << currentID
    }
    profiles[currentID].followeeIDs.each { followeeID ->
      if (profiles.containsKey (followeeID)) profiles[followeeID].followerIDs << currentID
    }
  }
}
// do it again to add in any that we missed
profiles.keySet ().each { currentID ->
  def c = 0
  profiles[currentID].followerIDs.each { followerID ->
    if (profiles.containsKey (toLong (followerID))) profiles[toLong (followerID)].followeeIDs << toLong (currentID)
    else c++
  }
//  if (c > 0) println ("Skipped $c profiles followers")
  c = 0
  profiles[currentID].followeeIDs.each { followeeID ->
    if (profiles.containsKey (toLong (followeeID))) profiles[toLong (followeeID)].followerIDs << toLong (currentID)
    else c++
  }
//  if (c > 0) println ("Skipped $c profiles followees")
}

disconnected = profiles.keySet ().findAll { key ->
  profiles[key].followerIDs.size () == 0 && profiles[key].followeeIDs.size () == 0
}

println ("Now, this many profiles had no followers or followees (protected?): " + disconnected.size ())
disconnected.each { profiles.remove (it) }

new File (outFilename).withWriter { out ->
  profiles.keySet ().each { id ->
    def p = profiles[id]
//    println (p.profile)
//    out.write (new ObjectMapper ().writeValueAsString ([id: id, neighbourhood: [ profile: p.profile, followerIDs: p.followerIDs, followeeIDs: p.followeeIDs]]))
    out.write (JsonOutput.toJson ([id: id, neighbourhood: p]))
//    out.write (JsonOutput.toJson ([id: id, neighbourhood: [ profile: p.profile.toString (), followerIDs: p.followerIDs, followeeIDs: p.followeeIDs]]))
    out.write ('\n')
  }
}
println ("Wrote output to $outFilename")