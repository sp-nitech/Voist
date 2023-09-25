#!/bin/bash

# second
sec_sil=0.1
sec_long=0.4
sec_short=0.1

# magnitude
mag=0.05

# f0
f0=10000  # standard: 8000, twice: 10000
fs=48000

# period
p0=12.3
ps=`echo "$fs / $f0 * $p0" | bc`

# length
len_sil=`echo "scale=0; $sec_sil * $fs" | bc`
len_long=`echo "scale=0; $sec_long * $fs / $ps * $ps" | bc`
len_short=`echo "scale=0; $sec_short * $fs / $ps * $ps" | bc`

# file name
file_sil=sil.raw
file_long=long.raw
file_short=short.raw
file_twice=twice.raw

# sil
nrand -l $len_sil -v 0 | \
sox -c 1 -f -4 -L -t raw -r $fs - -c 1 -s -3 -L -t raw -r $fs $file_sil

# long
sin -p $ps -l $len_long -m $mag |  \
sox -c 1 -f -4 -L -t raw -r $fs - -c 1 -s -3 -L -t raw -r $fs tmp
cat tmp $file_sil > $file_long

# short
sin -p $ps -l $len_short -m $mag |  \
sox -c 1 -f -4 -L -t raw -r $fs - -c 1 -s -3 -L -t raw -r $fs tmp
cat tmp $file_sil > $file_short

# twice
cat $file_short tmp $file_sil > $file_twice

rm -f tmp
rm -f $file_sil
