#to execute, from gnuplot type load "plot.plt"

reset
set datafile separator ","
set xlabel "Ranker"

set ylabel "Mean Average Precision" offset 1,0
set yrange [0.40:1.00]
set ytics 0.1
set format y "%1.2f"

set terminal postscript eps color enhanced size 6,2


set output "tmpP.eps"

set size 0.6, 1.2
#set grid


#set multiplot layout 1,4

#set origin 0,1
#set key off
set key under center
#set key maxcolumns 2 maxrows 6
set boxwidth 1
set style fill solid 1.00 noborder
set style data histograms
plot "summary_U0label100_set.csv" using 2:xtic(1) title "Flow" lt rgb "#406090",\
                               "" using 3 title "Temporal" lt rgb "#40FF00",\
                               "" using 4 title "Location" lt rgb "orange",\
                               "" using 5 title "Semantic" lt rgb "brown",\
                               "" using 6 title "Flow + Temporal" lt rgb "#A0A0A0",\
                               "" using 7 title "Flow + Temporal + Location" lt rgb "#404040",\
                               "" using 8 title "All" lt rgb "black"

unset multiplot

! eps2eps tmpP.eps feature_bar.eps
! rm tmpP.eps
