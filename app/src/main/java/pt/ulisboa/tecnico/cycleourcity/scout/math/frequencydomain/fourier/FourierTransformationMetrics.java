package pt.ulisboa.tecnico.cycleourcity.scout.math.frequencydomain.fourier;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by rodrigo.jm.lourenco on 18/03/2015.
 *
 * Available Metrics:
 * <ul>
 *     <li>DC Component</li>
 *     <li>Coefficients Sum</li>
 *     <li>Dominant Frequency</li>
 *     <li>Energy</li>
 *     <li>Info. Entropy</li>
 * </ul>
 *
 * TODO: estudar a matemática em melhor detalhe principalmente normalizacao e tipo de transformacao
 */
public class FourierTransformationMetrics {
    private final static String LOG_TAG = "FourierTransformationMetrics";
    private final static FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);

    /**
     * The DC component is defined as the first coefficient in the spectral representation of a
     * signal and its value if often larger than the remaining spectral coefficients.
     * @param signal
     * @return
     */
    public double calculateDCComponent(double[] signal){
        //TODO: inserir padding de zeros para certificar que o comprimento do sinal é uma potencia de 2
        try {
            Complex[] transform = FFT.transform(signal, TransformType.FORWARD);
        }catch (MathIllegalArgumentException e){
            Log.e(LOG_TAG, "Signal's length must be a power of 2");
        }

        return 0;
    }
}
