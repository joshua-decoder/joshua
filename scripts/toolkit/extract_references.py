#!/usr/bin/env python

import os, sys, codecs, re

def usage():
  print "Usage info for extract_references.py"
  print "  extract_references.py ref_sgml ref_prefix"
  
  print
  sys.exit()

def main():
  if (len(sys.argv) < 3 or sys.argv[1] == "-h"):
    usage()
  
  sgml = codecs.open(sys.argv[1], "r", "utf-8")
  prefix = sys.argv[2]
  
  doc_pattern = re.compile('.* docid="([^"]*).*"')
  seg_pattern = re.compile('.* id="([^"]*)".*')
  
  ref_sets = []
  cur_ref_set = []

  cur_doc = ""
  cur_seg = ""
  cur_txt = ""
  
  for line in sgml.readlines():
    line = line.strip().lower()
    if ("<doc " in line):
      cur_doc = doc_pattern.search(line).groups()[0]      

    if ("</refset " in line or 
        ("<doc " in line and cur_doc in map(lambda x: x[0], cur_ref_set))):
      ref_sets.append(cur_ref_set)
      cur_ref_set = []

    if ("<seg " in line):
      cur_seg = seg_pattern.search(line).groups()[0]
      cur_txt = re.sub("<[^>]*>", "", line)
      cur_ref_set.append((cur_doc, cur_seg, cur_txt))
  
  ref_files = []
  ref_count = len(ref_sets[0])
  for i, ref_set in enumerate(ref_sets):
    if (ref_count != len(ref_set)):
      print "[ERR] reference lengths do not match: " + str(ref_count) \
          + " vs. " + str(len(ref_set)) + " (ref " + str(i) + ")"
    ref_files.append(codecs.open(prefix + "_ref." + str(i), "w", "utf-8"))
    
  for j in range(ref_count):
    (cur_doc, cur_seg, cur_txt) = ref_sets[0][j]
    for i in range(len(ref_sets)):
      if (j >= len(ref_sets[i])):
        continue
      (doc, seg, txt) = ref_sets[i][j]
      if (doc != cur_doc or seg != cur_seg):
        print "[ERR] document, segment ids don't match up: "
        print "\t" + doc + " vs. " + cur_doc
        print "\t" + seg + " vs. " + cur_seg
      ref_files[i].write(txt + "\n")
    
  for ref_file in ref_files:
    ref_file.close()


if __name__ == "__main__":
    main()
