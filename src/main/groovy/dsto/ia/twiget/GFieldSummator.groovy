package dsto.ia.twiget

def filename = 'data/HuT/hamzah_q_neighbourhood_profiles.json'
def fieldname = 'statusesCount'

def f = new File (filename)
def matcher = f.text =~ /"$fieldname":(\d+),/
def total = matcher.collect { Integer.parseInt (it[1]) }.sum ()
println "Total $fieldname: $total"