#!/usr/bin/env python

import os, sys, codecs

def usage():
  print "Usage info for shorti.py"
  print "  shorti.py source target alignment [limit]"
  
  print
  sys.exit()

def main():
  if (len(sys.argv) < 4 or sys.argv[1] == "-h"):
    usage()

  limit = 99

  f = codecs.open(sys.argv[1], "r", "utf-8")
  e = codecs.open(sys.argv[2], "r", "utf-8")
  a = codecs.open(sys.argv[3], "r", "utf-8")
  
  if (len(sys.argv) >= 5):
    limit = int(sys.argv[4])
  
  f_out = codecs.open(sys.argv[1] + ".tr", "w", "utf-8")
  e_out = codecs.open(sys.argv[2] + ".tr", "w", "utf-8")
  a_out = codecs.open(sys.argv[3] + ".tr", "w", "utf-8")
  
  total = 0
  written = 0
  
  print "[INF]\tfiltering corpus, only sentences shorter than %d words" % (limit)
  
  while (True):
    f_line = f.readline()
    e_line = e.readline()
    a_line = a.readline()
    
    if ((a_line == "") or (f_line == "") or (e_line == "")):
      break

    f_line = f_line.rstrip().lstrip()
    e_line = e_line.rstrip().lstrip()
    a_line = a_line.rstrip().lstrip()
    
    oversized = False
    points = a_line.split()
    for point in points:
      coordinates = point.split("-")
      f_idx = int(coordinates[0])
      e_idx = int(coordinates[1])
      
      #print str(f_idx) + " - " + str(e_idx)
      
      if ((f_idx >= limit) or (e_idx >= limit)):
        oversized = True
        break
    
    if not oversized:
      written += 1
      
      f_out.write(f_line + "\n")
      e_out.write(e_line + "\n")
      a_out.write(a_line + "\n")
    total += 1
    
  print "[INF]\twritten %d sentences of %d (dropped %d)" % (written, total, total - written)


    
if __name__ == "__main__":
    main()
