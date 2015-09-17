if (args.size () < 2) {
  println ("Usage: groovy replace_ids_with_screennames.groovy <mapFile> <inFile>")
  println ("  If <inFile> is a hyphen ('-'), programme reads from stdin")
  return
}

mapFile = args[0]
inFile = args[1]

map = [:]
new File (mapFile).eachLine { line ->
  line = line.trim ().replaceAll ('  ', ' ')
  kv = line.split (' ')
  map[kv[0]] = kv[1]
}

reader = (inFile == '-' ? new InputStreamReader (System.in) : new FileReader (inFile))
reader.eachLine { line ->
  words = line.split (' ')
  words.each { word -> print ("${map[word] ?: word} ") }
  println ()
}
