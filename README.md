# Twiget - Twitter data gathering scripts

Twiget is my collection of experiments and utilities to gather Twitter data.

# Scripts/Apps

## GUserCorpusCollector

Takes a commandline argument specifying a seeds file in csv form, where the last column is the Twitter ID. The file might simply be a list of IDs, or a list of rows with the screen name and then the ID,
separated by a comma.

An optional flag is '--no-proxy', which will tell the script to not look for proxy settings in `/home/\`whoami\`/.gradle/gradle.properties`.

Assuming an input file of `seeds.csv`, a file will be produced titled something like `seeds-corpus.json` which includes as many tweets as could be gathered from the list of seeds, with each tweet being a JSON blob on each line of the file, along with another file called `failed_ids-CURRENT_DATETIME.txt` (where `CURRENT_DATETIME` is replaced with a formatted string of the current date/time value), which is list of the IDs which failed to collect anything due to an error (i.e. an exception occurred while attempting to collect the tweets). This can then form the input to another run of this program to attempt to gather more tweets, as sometimes the attempt to gather tweets for a given ID simply fails once but works the next time.

## GNeighbourhoodCorpusCollector (pretty much not current, but should still work)

This program builds a set of IDs of users whose tweets to collect to build a corpus of a community. Starting with hardwired seeds in my custom neighbourhood profile files, it gathers the IDs of the seeds, their followers and whom they follow and then progressively gathers those users' tweets and writes them out (one tweet as a JSON blob per line) to a single file. IDs where a failure to gather the tweets (i.e. an exception occurred) are gathered to a file starting with `failed_ids-` so that repeat attempts can be made to gather their tweets.

An optional flag is '--no-proxy', which will tell the script to not look for proxy settings in `/home/'whoami'/.gradle/gradle.properties`.

This may be better refactored into a program that builds the list of IDs from the neighbourhood profile files and then combined with `GUserCorpusCollector`.

## GRestructureNeighbourhoodData (not current)

Starting with hardwired neighbourhood profile files, which are hetereogeneous in structure, produces a homogeneously structured file of JSON profiles, even if some of the information is lacking. This was done to support parsing data to put it into Hive and Titan.

## GFieldSummator (not current, but should work)

Tells you an upper bound on how many tweets (roughly) would need to be gathered based on the profiles contained in a specific neighbourhood profile file.

## NeighbourhoodCollector (not current, but should work)

Given a screen name, this collects the specified user's profile, and the IDs of their followers and whom they follow, and write the data out as a single JSON blob to a file titled `<seedname>-neighbourhood-<date>.json`.

## NeighbourhoodProfileCollector (not current, but should work)

Given a screen name, this collects the specified user's profile, and the profiles of their followers and whom they follow, and the IDs of _their_ followers and whom they follow, and write the data out as a single JSON blob to a file titled `<seedname>_neighbourhood_profiles.json`.

## RecentTweetCollector

Adopted from online somewhere. Gathers the most recent accessible tweets by a given user

## UserCorpusCollector (not current, but should work)

Collects the tweets from one specific user, specified with a screen name or a Twitter ID.

## UserNetworkCollector (not current)

Starting from a specific user (screen name or Twitter ID), collect the follower/followee relationships down to a particular depth and just report who follows whom as a multimap.

## replace_all_ids_with_names.groovy

Groovy script that takes two parameters, the first is a map file (id <space> name on each line) and the second is a text file with IDs. If the text file has IDs in the given map, they are replaced with the corresponding names. Output is send to stdout. If the second parameter (the text file) is a hyphen ('-'), then input is sought from stdin, to allow the script to be used in pipes.

## Useful command lines

Pulling the account's ID and screen name out of each line. These commands won't work unless you include the "E" (or an "r"), as these stipulate that the extended regular expression syntax is going to be used.

 * `head -3 HuT_neighbourhood_public_profiles_and_follower_info-resolved-20150317.json | jq -c '[.id] + [.neighbourhood.user.screenName]' | sed 's/,/ /' | sed 's/"//g' | sed 's/^\[//' | sed 's/\]$//' | less`
 * `head -3 HuT_neighbourhood_public_profiles_and_follower_info-resolved-20150317.json | jq -c '[.id] + [.neighbourhood.user.screenName]' | sed -Ee 's,\[(.*)\],\1,' -e 's/,/ /g' | less`
 * `head -3 HuT_neighbourhood_public_profiles_and_follower_info-resolved-20150317.json | jq -c '[.id] + [.neighbourhood.user.screenName]' | sed -Ee 's,(\[|\]),,g' -e 's/,/ /g' | less`
 * `head -3 HuT_neighbourhood_public_profiles_and_follower_info-resolved-20150317.json | jq -c '[.id] + [.neighbourhood.user.screenName]' | sed -Ee 's,^\[([^]]*)\]$,\1,' -e 's/,/ /g' | less`

## GExtractDomains.groovy

Groovy script to extract all URL domains it can from `<stdin>`. The JSON map of domains to counts is send to `<stdout>`. The `--pretty|-p` flag prints the map nicely, putting one domain and count per line. The `--debug|-v` flag turns on debug printlns. The `--help|-h|-q` flag prints the help text.

