#to execute, from gnuplot type load "plot.plt"

reset
set datafile separator ","
set xlabel "Analysis time (min)"
set xrange [0:20]
set xtic 0,5
set mxtic 5

set ylabel "Impact Count %" offset 2,0
set yrange [0:105]
set ytics 10

set terminal postscript eps color enhanced size 6,2


set output "tmpP.eps"

set size 0.7, 1
set grid


#show colorname for a list of colors
set style line 1 lt 5 lw 4 pt 1 ps 1 lc rgb "black"
set style line 2 lt 2 lw 4 pt 2 ps 1 lc rgb "#4daf4a"
set style line 3 lt 1 lw 4 pt 3 ps 1 lc rgb "blue"

set multiplot layout 1,4

#set origin 0,1
#set key outside center right
#set key outside center bottom
#set key maxcolumns 2 maxrows 6
set key inside right bottom
set title "Time to Mission Impact: 5 min"
plot "< tail -n+2 p1-comp300.csv" using 1:2 with lines title "FIFO" ls 1,\
	 "< tail -n+2 p1-comp300.csv" using 1:3 with lines title "LIFO" ls 2,\
	 "< tail -n+2 p1-comp300.csv" using 1:4 with lines title "Impact" ls 3

set key off
set title "Time to Mission Impact: 10 min"
plot "< tail -n+2 p1-comp600.csv" using 1:2 with lines title "FIFO" ls 1,\
	 "< tail -n+2 p1-comp600.csv" using 1:3 with lines title "LIFO" ls 2,\
	 "< tail -n+2 p1-comp600.csv" using 1:4 with lines title "Impact" ls 3

set key off
set title "Time to Mission Impact: 20 min"
plot "< tail -n+2 p1-comp1200.csv" using 1:2 with lines title "FIFO" ls 1,\
	 "< tail -n+2 p1-comp1200.csv" using 1:3 with lines title "LIFO" ls 2,\
	 "< tail -n+2 p1-comp1200.csv" using 1:4 with lines title "Impact" ls 3

set key off
set title "Time to Mission Impact: 40 min"
plot "< tail -n+2 p1-comp2400.csv" using 1:2 with lines title "FIFO" ls 1,\
	 "< tail -n+2 p1-comp2400.csv" using 1:3 with lines title "LIFO" ls 2,\
	 "< tail -n+2 p1-comp2400.csv" using 1:4 with lines title "Impact" ls 3
	 	 
unset multiplot

! eps2eps tmpP.eps p1.eps
! rm tmpP.eps
