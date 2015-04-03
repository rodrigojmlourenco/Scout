package pt.ulisboa.tecnico.cycleourcity.scout.math.timedomain;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.util.MathArrays;

/**
 * Created by rodrigo.jm.lourenco on 17/03/2015.
 *
 * Signal Correlation is used to measure the strength and linear direction of a linear relationship
 * between two signals.
 * <br>
 * In activity recognition, correlation is specially useful for differentiating between activities
 * that involve translation into a single dimension.
 */
public class SignalCorrelation {

    /**
     * Pearson's product-moment coefficient, also known as the sample correlation coefficient.
     * <br>
     * The Sample Correlation Coefficient is calculated as the ratio of the covariance of the signals
     * along the x and y axis, to the product of their standard deviations.
     * @param xAxisSamples Samples along the x-axis of a tri-axial sensor
     * @param yAxisSamples Samples along the y-axis of a tri-axial sensor
     * @return Correlation Coefficient
     * @see org.apache.commons.math3.stat.correlation.PearsonsCorrelation
     */
    public static double calculateCorrelation(double[] xAxisSamples, double[] yAxisSamples){
        return new PearsonsCorrelation().correlation(xAxisSamples, yAxisSamples);
    }

    public static double[] calculateConvolution(double[] aAxisSamples, double[] bAxisSamples){
        return MathArrays.convolve(aAxisSamples, bAxisSamples);
    }

    /**
     * The cross-correlation is a measure of similarity between two waveforms and is commonly used to
     * search for a known pattern in a long signal.
     * <br>
     * Cross-correlation and convolution are closely related, and the only difference between the two
     * of them is a time-reversal of one of the signals, for the cross-correlation.
     * @param aAxisSamples
     * @param bAxisSamples
     * @return cross-correlation coefficients
     */
    public static double[] calculateCrossCorrelation(double[] aAxisSamples, double[] bAxisSamples){
        ArrayUtils.reverse(bAxisSamples);
        return calculateConvolution(aAxisSamples,bAxisSamples);
    }

    /**
     * The cross-correlation is a measure of similarity between two waveforms and is commonly used to
     * search for a known pattern in a long signal.
     * <br>
     * Cross-correlation and convolution are closely related, and the only difference between the two
     * of them is a time-reversal of one of the signals, for the cross-correlation.
     * <br>
     * The typical implementation of this metric computes the cross-correlation coefficients for the
     * three axis, of a tri-axial sensor, in a pairwise fashion and then selects the pair of signals
     * that exhibits the largest coefficients to distinguish between dynamic activities
     * <a href="http://ieeexplore.ieee.org/xpl/freeabs_all.jsp?arnumber=547939">[Veltink1996]</a>.
     * @param xAxisSamples Samples along the x-axis of a tri-axial sensor
     * @param yAxisSamples Samples along the y-axis of a tri-axial sensor
     * @param zAxisSamples Samples along the z-axis of a tri-axial sensor
     * @return
     */
    public static double[] calculateCrossCorrelationPairwise(double[] xAxisSamples, double[] yAxisSamples, double[] zAxisSamples){

        double[] xcorrXY = calculateCrossCorrelation(xAxisSamples, yAxisSamples);
        double[] xcorrXZ = calculateCrossCorrelation(xAxisSamples, zAxisSamples);
        double[] xcorrYZ = calculateCrossCorrelation(yAxisSamples, zAxisSamples);

        //TODO: certificar que é isto que é suposto ser feito
        double sumXY = StatUtils.sum(xcorrXY);
        double sumXZ = StatUtils.sum(xcorrXZ);
        double sumYZ = StatUtils.sum(xcorrYZ);

        return null;
    }

}
