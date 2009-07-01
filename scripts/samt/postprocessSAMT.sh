
if [ $# -ne 3 ]
then
  echo "Usage: postprocessSAMT.sh mergedrules.gz samt.tm.gz samt.glue.gz"
  exit 2
fi

if [ ! -r $1 ]
then
  echo "Error: file $1 does not exist or is not readable."
  exit 3
fi

zgrep -v COUNT $1 | gzip > $2
zgrep COUNT $1 | awk 'BEGIN { FS="#" } ; { print $3 "#@1#@GOAL#1 0 0 0 0 0 0 0";\
	 print "@GOAL " $3 "#@1 @2#@GOAL#1 0 0 0 0.434294482 0 0 0" }' | gzip > $3

