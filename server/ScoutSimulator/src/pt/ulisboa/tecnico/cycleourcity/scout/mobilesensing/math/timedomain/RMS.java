package pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.math.timedomain;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * Created by rodrigo.jm.lourenco on 17/03/2015.
 */
public class RMS {

    private static final double DEFAULT_THRESHOLD = .4; //TODO: descobrir um valor apropriado

    /**
     *
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
    public static double calculateRootMeanSquare(double[] samples){
        return FastMath.sqrt(StatUtils.sumSq(samples)/(double)samples.length);
    }

    public static double caculateIntegration(double[] samples){
        return 0;
    }

    public static double caculateIntegration(double[] samples, double threshold){
        return 0;
    }
}
