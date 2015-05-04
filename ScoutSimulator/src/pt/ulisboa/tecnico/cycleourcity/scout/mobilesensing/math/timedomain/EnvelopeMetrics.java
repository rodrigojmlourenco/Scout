package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.MathArrays;

/**
 * Created by rodrigo.jm.lourenco on 17/03/2015.
 */
public class EnvelopeMetrics {

    /**
     * In statistics and probability theory, the median is the number separating the higher half of
     * a data sample, a population, or a probability distribution, from the lower half.
     * <br>
     * This metric can be, for example, used in order to replace missing values from a sequence of
     * discrete values, as was performed by
     * <a href="http://3d-media-browser.googlecode.com/svn/trunk/articles/Wiimote%20interaction/%5BREAD%5D%20GestureRecognitionofNintendoWiimoteI.pdf">
     *     [Wiggins:2008]
     * </a>.
     * <br>
     * <table>
     *     <tr>
     *         <th>Comp. Cost</th>
     *         <th>Storage Req.</th>
     *     </tr>
     *     <tr>
     *         <td>
     *             Medium
     *         </td>
     *         <td>
     *             Very Low
     *         </td>
     *     </tr>
     * </table>
     * @param samples Sensor samples
     * @return Median
     */
    public static double calculateMedian(double[] samples){
        double median = 0;
        median = StatUtils.percentile(samples, 50);
        return median;
    }

    public static double[] calculateMode(double[] samples){
        double[] mode;
        mode = StatUtils.mode(samples);
        return mode;
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
    public static double calculateMax(double[] samples){
        return StatUtils.max(samples);
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
    public static double calculateMin(double[] samples){
        return StatUtils.min(samples);

    }

    /**
     * The range of a sample array is defined as the difference between the maximum and minimum samples
     * values.
     * <br>
     * This metric as successfully employed, together with other indicators, in order to distinguish
     * between walking and running
     * <a href="http://ieeexplore.ieee.org/xpl/freeabs_all.jsp?arnumber=806681&abstractAccess=no&userType=inst">
     *     [Farringdon:1999]
     * </a>.
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
     * @param samples
     * @return range
     */
    public static double calculateRange(double[] samples){
        return StatUtils.max(samples) - StatUtils.min(samples);
    }

    //TODO: implementar

    /**
     * Computing the difference, i.e. the delta value, between signals in a pairwise fashion allows
     * a basic comparison between the intensity of user activity. This is possible, because in general
     * every activity will be noticeable in one or more of the accelerometer's axis, so different
     * activities can in principle be distinguished by comparing the signal strength in all three axis.
     * @param aAxisSamples
     * @param bAxisSamples
     * @return
     */
    public static double[] calculateDelta(double[] aAxisSamples, double[] bAxisSamples){
        return MathArrays.ebeSubtract(aAxisSamples, bAxisSamples);
    }
}
