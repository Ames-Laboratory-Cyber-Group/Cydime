#to execute, from gnuplot type load "plot.plt"

reset
set datafile separator ","
set xdata time
set timefmt "%H:%M"
set format x "%H"
set xlabel "Hour of Day"
set xrange ["00:00":"19:00"]
set xtic "00:00", 3600, "19:00"

set ylabel "Frequency" offset 2,0
#set yrange [0:105]
#set ytics 10

set terminal postscript eps color enhanced size 6,2


set output "tmpP.eps"

set size 0.7, 1
set grid


#show colorname for a list of colors
set style line 1 lt 1 lw 4 pt 1 ps 1 lc rgb "black"

#set multiplot layout 1,4

#set origin 0,1
#set key outside center right
#set key outside center bottom
#set key maxcolumns 2 maxrows 6
set key off
set title "Priority 3 Alerts"
plot "< cat sortedSummary.csv" using 1:4 with lines title "1" ls 1

! eps2eps tmpP.eps palert3.eps
! rm tmpP.eps
