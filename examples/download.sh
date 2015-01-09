#!/bin/bash

echo "Downloading corpus..."
rm -rf fisher-callhome-corpus-1.0
rm -f fisher-callhome-corpus-1.0.tgz
wget -q -O fisher-callhome-corpus-1.0.tgz https://github.com/joshua-decoder/fisher-callhome-corpus/archive/v1.0.tar.gz

echo "Unpacking..."
tar xzf fisher-callhome-corpus-1.0.tgz

echo "Linking..."
ln -sf fisher-callhome-corpus-1.0 data

echo "Done. See the files in data/. You can now build the examples."
