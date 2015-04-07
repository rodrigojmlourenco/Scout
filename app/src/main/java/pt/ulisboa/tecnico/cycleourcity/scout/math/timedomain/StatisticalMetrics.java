package pt.ulisboa.tecnico.cycleourcity.scout.math.timedomain;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * Created by rodrigo.jm.lourenco on 17/03/2015.
 */
public class StatisticalMetrics {

    /**
     * Given a sample array, this method calculates the mean value of those samples.
     * <br>
     * The mean over a window of data samples is a meaningful metric for almost every
     * kind of sensor, and can be calculated with small computational cost.
     * <br>
     * <table>
     *     <tr>
     *         <th>Comp. Cost</th>
     *         <th>Storage Req.</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             Very Low
     *         </td>
     *         <td>
     *             Very Low
     *         </td>
     *     </tr>
     * </table>
     * @param samples the sample array.
     * @return mean value
     */
    public static double calculateMean(double[] samples){
        double mean = 0;

        mean = StatUtils.mean(samples);

        return mean;
    }


    /**
     * <table>
     *     <tr>
     *         <th>Comp. Cost</th>
     *         <th>Storage Req.</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             Very Low
     *         </td>
     *         <td>
     *             Very Low
     *         </td>
     *     </tr>
     * </table>
     * @param samples
     * @return
     */
    public static double calculateVariance(double[] samples){
        double variance = 0;
        variance = StatUtils.variance(samples);
        return variance;
    }

    /**
     * <table>
     *     <tr>
     *         <th>Comp. Cost</th>
     *         <th>Storage Req.</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             Very Low
     *         </td>
     *         <td>
     *             Very Low
     *         </td>
     *     </tr>
     * </table>
     * @param samples
     * @return
     */
    public static double calculateStandardDeviation(double[] samples){
        double stdDev = 0;
        stdDev = FastMath.sqrt(StatUtils.variance(samples));
        return stdDev;
    }


}
