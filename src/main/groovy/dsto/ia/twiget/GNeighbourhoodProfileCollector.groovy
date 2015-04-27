package dsto.ia.twiget

import groovy.json.JsonOutput

import java.util.concurrent.TimeUnit

import twitter4j.User

import com.google.common.base.Stopwatch
import com.google.common.collect.Sets

if (! args.contains ('--no-proxy')) {
  GUtils.configureProxy ("/home/${System.properties.'user.name'}/.gradle/gradle.properties")
  println ("Proxy configured...")
}

def timer = Stopwatch.createStarted ()

def stripOptions (argList) { argList.findAll { ! it.startsWith ('-') } }

if (! stripOptions (args).size ()) {
  println ("Must supply a path to a seed file as an argument")
  return
} else {
  println ("Running with args: ${args.join (' ')}")
}

def seedsFile = new File (stripOptions (args)[0])

if (! seedsFile.exists ()) {
  println ("Could not find file $seedsFile (${seedsFile.absolutePath})")
  return
}

def seedNames = Sets.newHashSet ()
seedsFile.eachLine { line ->
  def id = line.trim ()
  if (id) seedNames << id
}

def rootDir = seedsFile.parentFile.absolutePath
def seedFN = seedsFile.name - ".csv"

println ("A total of " + seedNames.size () + " accounts to collect tweets from.")

def snowballCollector = new SnowballCollector ()
def count = 0
def idCount = 1
def idsToCollect = Sets.newHashSet ()
def collectedProfiles = Sets.newHashSet ()

def dateStr = Utils.format (new Date ())
def failedNames = new FileWriter("${rootDir}/failed_names-${dateStr}.txt")

def outfile = new File("${rootDir}/${seedFN}-neighbourhood_profiles.json")
println ("Writing to ${outfile.absolutePath}")
outfile.withWriter ('UTF-8') { out ->
  println ("Collecting ${seedNames.size ()} seed neighbourhoods")
  seedNames.each { String id ->
    println ("Collecting neighbourhood for id ${idCount++}: @$id")
    def n = snowballCollector.collectNeighbourhoodOf (id)
    
    if (! n.profile) {
      failedNames << "$id\n"
      failedNames.flush ()
    }

    idsToCollect.addAll (n.followeeIDs)
    idsToCollect.addAll (n.followerIDs)
    println ("idsToCollect now this big: " + idsToCollect.size ())

    //  corpus << tweets
    out.write (JsonOutput.toJson (n))
    out.write ('\n')
    out.flush ()
    count++
  }
}
failedNames.close ()

// grab neighbour profiles
def neighbourprofilesfile = new File("${rootDir}/${seedFN}-neighbourhood_profiles-tmp.json")
println ("Writing to ${neighbourprofilesfile.absolutePath}")
neighbourprofilesfile.withWriter ('UTF-8') { out ->
  // collect follower/followee neighbourhoods
  println ("Collecting ${idsToCollect.size ()} neighbouring neighbourhoods")
  
  (idsToCollect as List<Long>).collate (100).each { ids ->
    def profiles = Utils.grabUpTo100Users (snowballCollector.twitter, ids as List<Long>)
    
    profiles.each { profile ->
      out.write (JsonOutput.toJson (profile))
      out.write ('\n')
    }
    out.flush ()
    println ("captured another ${profiles.size ()} profiles...")
    
    collectedProfiles.addAll (profiles)
  }
}

// build Neighbourhood objects with follower/followee IDs for all the collected profiles
println ("Writing to ${outfile.absolutePath} again")
outfile.withWriterAppend ('UTF-8') { out ->
  profiles.each { User user ->
    println ("Collecting neighbourhood for id ${idCount++}: @${user.screenName}")
    println ("  followers: ${user.followersCount} followees: ${user.friendsCount}")
    def n = new Neighbourhood ()
    n.profile = user

    Utils.fetchFollowerIds (snowballCollector.twitter, user.id, n.followerIDs, true)
    Utils.fetchFolloweeIds (snowballCollector.twitter, user.id, n.followeeIDs, true)

    out.write (JsonOutput.toJson (n))
    out.write ('\n')
    out.flush ()
    
    count++
    println ("Collected ${n.followerIDs.size ()} follower IDs and ${n.followeeIDs.size ()} followee IDs.")
  }
}

println ("Collected " + count + " profiles in " + timer.elapsed (TimeUnit.MINUTES) + " minutes.")
