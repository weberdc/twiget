package dsto.ia.twiget

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.util.concurrent.TimeUnit

import twitter4j.TwitterObjectFactory

import com.google.common.base.Stopwatch

def timer = Stopwatch.createStarted ()

def seedNeighbourhoodsFile = new File ("incoming/may2015.txt-neighbourhood_profiles.json.bkp")
def rescuedFFProfilesFile = new File ("incoming/may2015.txt-neighbourhood_profiles-tmp.json")
def outFile = new File ("incoming/may2015-immediate_neighbourhood_profiles.json")

def seedNeighbourhoods = [:]
def json = new JsonSlurper ()
seedNeighbourhoodsFile.readLines ().each { line ->
  def nStr = (line - "{\"snapshot\":")[0..-2] //(line =~ /^\{\"snapshot\":(\{.*\})\}$/)[0]
  def followerIDs = (nStr =~ /followerIDs..(\[[^\]]+\])/ )[0][1]
  def followeeIDs = (nStr =~ /followeeIDs..(\[[^\]]+\])/ )[0][1]
  def n = ((json.parseText (line) as Map).snapshot as Map)
  seedNeighbourhoods[n.user.id] = [nstr: nStr, followerIDs: followerIDs, followeeIDs: followeeIDs, n: n]
//  println ("followerIDs: " + followerIDs)
//  println ("followeeIDs: " + followeeIDs)
}
//System.exit(0)

def text = seedNeighbourhoodsFile.text
rescuedNeighbourhoods = []
rescuedFFProfilesFile.readLines ().each { line ->
  
  def user = TwitterObjectFactory.createUser (line)
  def followeeIDs = seedNeighbourhoods.findAll { entry -> entry.value.followerIDs.contains (String.valueOf (user.id)) }.collect { it.key }
  def followerIDs = seedNeighbourhoods.findAll { entry -> entry.value.followeeIDs.contains (String.valueOf (user.id)) }.collect { it.key }

  rescuedNeighbourhoods << new Neighbourhood (user, followeeIDs, followerIDs)
}
println ("Read ${rescuedNeighbourhoods.size ()} profiles...")

println ("Writing to ${outFile.absolutePath}")
outFile.withWriter { out ->
  seedNeighbourhoods.each { id, map ->
    out.write (JsonOutput.toJson ([id: id, neighbourhood: map.n]))
    out.write ('\n')
    out.flush ()
  }
  
  rescuedNeighbourhoods.each { n ->
    out.write (JsonOutput.toJson ([id: n.profile.id, neighbourhood: n]))
    out.write ('\n')
    out.flush ()
  }
}
println ("Finished after ${timer.elapsed (TimeUnit.SECONDS)} seconds.")