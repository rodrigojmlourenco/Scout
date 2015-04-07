package pt.ulisboa.tecnico.cycleourcity.scout.math.timedomain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rodrigo.jm.lourenco on 18/03/2015.
 */
//TODO: verificar que os métodos associados ao Zero-Crossings estão correctos.
public class TimeDomainMetrics {

    /**
     * Zero-crossings rate is the rate, during the time interval, where the signal crosses the
     * reference (i.e. the delimeter) value.
     * <br>
     * Zero-crossings can be defined as the points where a signal passes through a specific value
     * corresponding to half of the signal range.
     * @param signalSample
     * @return Zero-crossings rate
     */
    public static int calculateZeroCrossingsRate(double[] signalSample, int timeLength){
        return calculateZeroCrossings(signalSample).length /timeLength;
    }

    /**
     * Zero-crossings can be defined as the points where a signal passes through a specific value
     * corresponding to half of the signal range.
     * @param signalSample The signal sample array
     * @return Array with the positions, in the signal, where crossing occurs.
     */
    public static Integer[] calculateZeroCrossings(double[] signalSample){

        List<Integer> crossings = new ArrayList<>();

        double delimiter = EnvelopeMetrics.calculateRange(signalSample)/2.0;

        for(int i=0; i < signalSample.length - 1 ; i++)
            if((signalSample[i]>=delimiter && signalSample[i+1] < delimiter) || //Top-down
                    (signalSample[i]<=delimiter && signalSample[i+1] > delimiter)) //Bottom-up
                crossings.add(i);

        return crossings.toArray(new Integer[0]);
    }

    public static double calculateAngle(double[] signalSample){
        return 0;
    }

    public static double calculateAngularAcceleration(double[] signalSample){
        return 0;
    }

}
