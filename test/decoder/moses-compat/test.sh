#!/bin/bash

set -u

rm -f output
# should write translation to stdout, output-format info to n-best.txt
echo help | joshua -v 0 -moses -n-best-list n-best1.txt 10 distinct > output
# should write output-format info to n-best.txt (since no -moses)
echo help | joshua -v 0 -n-best-list n-best2.txt 10 distinct >> output
# should write translation to stdout
echo help | joshua -v 0 -moses >> output

echo >> output
echo "# n-best stuff to follow:" >> output
cat n-best1.txt n-best2.txt >> output

# Compare
diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
    rm -f diff log output n-best1.txt n-best2.txt
    exit 0
else
    exit 1
fi
