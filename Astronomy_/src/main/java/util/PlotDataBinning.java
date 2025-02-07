package util;

import flanagan.analysis.Stat;
import flanagan.math.ArrayMaths;
import ij.astro.types.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class PlotDataBinning {

    public static Pair.GenericPair<DoubleArrayTriple, Double> binData(double[] x, double[] y, double binWidth) {
        return binDataErr(x, y, null, binWidth);
    }

    public static Pair.GenericPair<DoubleArrayTriple, Double> binDataErr(double[] x, double[] y, double[] err, double binWidth) {
        try {
            if (x.length != y.length) {
                System.err.println("Binning arrays do match");
                return null;
            }

            if (x.length == 0) {
                return null;
            }

            var withErr = err != null;
            if (!withErr) err = new double[x.length];

            var t = new ArrayMaths(x);
            var xMin = t.minimum();
            var xMax = t.maximum();

            var l = IntStream.range(1, x.length).parallel().mapToDouble(i -> x[i] - x[i-1]).toArray();
            if (l.length == 0) {
                return null;
            }
            var minBinWidth = new ArrayMaths(l).minimum();
            if (minBinWidth > binWidth) binWidth = minBinWidth;

            var span = xMax - xMin;
            var nBins = (int) Math.ceil(span/binWidth);

            if (nBins == 0) nBins = x.length;

            var binBounds = new double[nBins + 1];
            double finalBinWidth = binWidth;
            Arrays.setAll(binBounds, i -> (i * finalBinWidth) + xMin);

            var bins = new DataBin[nBins];
            Arrays.setAll(bins, i -> new DataBin(x.length, i));

            for (int i = 0; i < x.length; i++) {
                var p = 0;
                var accepted = false;
                while (!accepted) {
                    accepted = bins[p].accept(binBounds, x[i], y[i], err[i]);
                    p++;
                    if (p == bins.length) break;
                }

                if (!accepted) throw new RuntimeException("data did not fit into a bin");
            }

            var binCompleted = Arrays.asList(Arrays.stream(bins).parallel().filter(DataBin::hasData)
                    // Converts to array first as Stream#toList returns unmodifiable list, and we need to sort it
                    .map(withErr ? DataBin::binnedDatumErr : DataBin::binnedDatum).toArray(DoubleTriple[]::new));
            var outX = new double[binCompleted.size()];
            var outY = new double[binCompleted.size()];
            var outErr = new double[binCompleted.size()];

            // Order the elements by time
            binCompleted.sort(Comparator.comparingDouble(c -> c.x));
            for (int i = 0; i < binCompleted.size(); i++) {
                outX[i] = binCompleted.get(i).x();
                outY[i] = binCompleted.get(i).y();
                outErr[i] = binCompleted.get(i).err();
            }

            return new Pair.GenericPair<>(new DoubleArrayTriple(outX, outY, outErr), binWidth);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private record DataBin(double[] x, double[] y, double[] err, Holder lastIndex, int binIndex) {
        public DataBin(int maximumSize, int binIndex) {
            this(new double[maximumSize], new double[maximumSize], new double[maximumSize], new Holder(), binIndex);
        }

        public void addData(double xDatum, double yDatum, double errDatum) {
            x[lastIndex.val] = xDatum;
            y[lastIndex.val] = yDatum;
            err[lastIndex.val] = errDatum;
            lastIndex.val++;
        }

        public boolean hasData() {
            return lastIndex.val > 0;
        }

        public DoubleTriple binnedDatum() {
            return new DoubleTriple(Stat.mean(Arrays.copyOf(x, lastIndex.val)), Stat.mean(Arrays.copyOf(y, lastIndex.val)), 0);
        }

        public DoubleTriple binnedDatumErr() {
            var err = Arrays.copyOf(this.err, lastIndex.val);
            var totalErr = Arrays.stream(err).sum();
            var totalErr2 = totalErr * totalErr;
            var err2 = Arrays.stream(err).map(d -> d*d).toArray();
            var invErr2 = Arrays.stream(err2).map(d -> d != 0 ? 1/d : 0).toArray();
            var totInvErr2 = Arrays.stream(invErr2).sum();

            var totX = IntStream.range(0, lastIndex.val).mapToDouble(i -> x[i]/err2[i]).sum();
            var binX = totX / totInvErr2;

            var totY = IntStream.range(0, lastIndex.val).mapToDouble(i -> y[i]/err2[i]).sum();
            var binY = totY / totInvErr2;

            var binErr = 1/Math.sqrt(totInvErr2);

            return new DoubleTriple(binX, binY, binErr);
        }

        public boolean accept(double[] binBounds, double x, double y) {
            return accept(binBounds, x, y, 0);
        }

        public boolean accept(double[] binBounds, double x, double y, double err) {
            // As the bins are populated in order, the upper bound can be <=
            if (x>=binBounds[binIndex] && x<=binBounds[binIndex+1]) {
                addData(x, y, err);
                return true;
            }

            return false;
        }

        static class Holder {
            public int val = 0;
        }
    }

    private record DoubleTriple(double x, double y, double err) {}

    public record DoubleArrayTriple(double[] x, double[] y, double[] err) {}
}
