package pt.ulisboa.tecnico.cycleourcity.evalscout.pipeline.stages.classification;

import android.util.Log;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;
import pt.ulisboa.tecnico.cycleourcity.scout.pipeline.stages.classification.exceptions.InvalidFeatureVectorException;

/**
 * J48 Decision Tree using 6 features, with 89.4% classification precision.
 * <br>
 * Tree Size:<br>
 *     <ul>
 *         <li>Nodes: 27</li>
 *         <li>Leafs: 14</li>
 *     </ul>
 * <br>
 * Features required:<br>
 *     <ul>
 *         <li>Variance</li>
 *         <li>Standard Deviation</li>
 *         <li>Range</li>
 *         <li>Maximum</li>
 *         <li>Minimum</li>
 *         <li>Root Mean Squares</li>
 *     </ul>
 */
public class BaseRoadClassificationStage extends RoadClassificationStage {

    public BaseRoadClassificationStage(){
        NAME = this.getClass().getSimpleName();
    }

    protected JsonObject generateClassification(JsonObject featureVector) throws InvalidFeatureVectorException {

        double variance,
                range,
                max,
                rms;

        try{
            variance= featureVector.get(SensingUtils.FeatureVectorKeys.VARIANCE).getAsDouble();
            range   = featureVector.get(SensingUtils.FeatureVectorKeys.RANGE).getAsDouble();
            max     = featureVector.get(SensingUtils.FeatureVectorKeys.MAX).getAsDouble();
            rms     = featureVector.get(SensingUtils.FeatureVectorKeys.RMS).getAsDouble();
        }catch (NullPointerException | ClassCastException e){
            throw new InvalidFeatureVectorException(e.getMessage());
        }

        PavementType.Pavements pavementType;

        //J48 Decision Tree
        if(rms <= 3.335484){ //Root-leaf
            //Branch #1
            if(rms <= 2.523847) {
                if (rms <= 1.160521)
                    if (range <= 12.762338)
                        if (variance <= 0.416339)
                            pavementType = PavementType.Pavements.CobblestoneBad;   //Leaf 1
                        else
                            pavementType = PavementType.Pavements.AsphaltGood;      //Leaf 2
                    else
                        pavementType = PavementType.Pavements.CobblestoneBad;       //Leaf 3
                else
                    pavementType = PavementType.Pavements.AsphaltGood;              //Leaf 4
            }else {
                if (rms <= 2.840665)
                    pavementType = PavementType.Pavements.AsphaltGood;              //Leaf 5
                else
                if (range <= 20.768969)
                    pavementType = PavementType.Pavements.CobblestoneGood;      //Leaf 6
                else
                    pavementType = PavementType.Pavements.AsphaltGood;          //Leaf 7
            }
            //Branch #2
        }else{
            if(variance <= 77.334899) { //Branch #2.1
                if (max <= 17.969101)
                    pavementType = PavementType.Pavements.CobblestoneGood;          //Leaf 8
                else if (max <= 18.277355)
                    if (variance <= 52.557136)
                        pavementType = PavementType.Pavements.AsphaltBad;           //Leaf 9
                    else
                        pavementType = PavementType.Pavements.CobblestoneGood;      //Leaf 10
                else
                    pavementType = PavementType.Pavements.AsphaltBad;               //Leaf 11
            }else{ //Branch #2.2
                if(variance <= 102.585187)
                    if(range <= 50.503662)
                        pavementType = PavementType.Pavements.CobblestoneBad;       //Leaf 12
                    else
                        pavementType = PavementType.Pavements.AsphaltBad;           //Leaf 13
                else
                    pavementType = PavementType.Pavements.CobblestoneBad;           //Leaf 14
            }
        }

        if(VERBOSE)
            Log.d(LOG_TAG, NAME+" has determined this route to be '"+pavementType+"'.");

        JsonObject classification = generateClassificationStub(featureVector);
        classification.addProperty(CLASSIFICATION, String.valueOf(pavementType));
        return classification;
    }
}
