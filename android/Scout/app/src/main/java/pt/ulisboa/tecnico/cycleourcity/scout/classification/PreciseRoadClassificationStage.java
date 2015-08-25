package pt.ulisboa.tecnico.cycleourcity.scout.classification;

import android.util.Log;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.cycleourcity.scout.classification.exceptions.InvalidFeatureVectorException;
import pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.SensingUtils;

/**
 * J48 Decision Tree using 7 features, with precision 92.6%.
 * <br>
 * Tree Size:<br>
 *     <ul>
 *         <li>Nodes: 28</li>
 *         <li>Leafs: 55</li>
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
 *         <li>Mean Crossings</li>
 *     </ul>
 */
public class PreciseRoadClassificationStage extends RoadClassificationStage{

    public PreciseRoadClassificationStage(){
        NAME = this.getClass().getSimpleName();
    }

    protected JsonObject generateClassification(JsonObject featureVector) throws InvalidFeatureVectorException {

        JsonObject classification;

        double  variance,
                range,
                max,
                min,
                rms,
                meanCrossings;

        try{
            variance        = featureVector.get(SensingUtils.FeatureVectorKeys.VARIANCE).getAsDouble();
            range           = featureVector.get(SensingUtils.FeatureVectorKeys.RANGE).getAsDouble();
            max             = featureVector.get(SensingUtils.FeatureVectorKeys.MAX).getAsDouble();
            min             = featureVector.get(SensingUtils.FeatureVectorKeys.MIN).getAsDouble();
            rms             = featureVector.get(SensingUtils.FeatureVectorKeys.RMS).getAsDouble();
            meanCrossings   = featureVector.get(SensingUtils.FeatureVectorKeys.MEAN_CROSSING).getAsDouble();
        }catch (NullPointerException | ClassCastException e){
            throw new InvalidFeatureVectorException(e.getMessage());
        }

        PavementType.Pavements pavementType;

        if(rms <= 3.335484) { //Root-Node
            //Branch #1.1
            if(meanCrossings <= 144){
                if(variance <= 1.444214)
                    pavementType = PavementType.Pavements.CobblestoneBad;                       //Leaf 1
                else
                if(meanCrossings <= 121)
                    if(variance <= 3.789713)
                        pavementType = PavementType.Pavements.AsphaltGood;                      //Leaf 2
                    else
                        pavementType = PavementType.Pavements.CobblestoneBad;                   //Leaf 3
                else
                    pavementType = PavementType.Pavements.AsphaltGood;                          //Leaf 4
            }else{ //Branch #1.2
                if(rms <= 2.523847)
                    pavementType = PavementType.Pavements.AsphaltGood;                           //Leaf 5
                else
                if(min <= -9.16505)
                    pavementType = PavementType.Pavements.AsphaltGood;                          //Leaf 6
                else
                if(variance <= 7.834415)
                    pavementType = PavementType.Pavements.AsphaltGood;                          //Leaf 7
                else
                    pavementType = PavementType.Pavements.CobblestoneGood;                      //Leaf 8
            }
        }else{ //Branch #2 - rms > 3.335484
            //Branch #2.1
            if(variance <= 77.334899){
                //Branch #2.1.1
                if(max <= 17.969101){
                    //Branch #2.1.1.1
                    if(meanCrossings <= 163) {
                        //Branch #2.1.1.1.1
                        if (meanCrossings <= 145) {
                            if (variance <= 16.656637)
                                pavementType = PavementType.Pavements.CobblestoneBad;           //Leaf 9
                            else if (variance <= 57.620903)
                                pavementType = PavementType.Pavements.AsphaltBad;               //Leaf 10
                            else
                                pavementType = PavementType.Pavements.CobblestoneBad;           //Leaf 11
                            //Branch #2.1.1.1.1
                        } else { //meanCrossings > 145
                            if (max <= 15.143273) {
                                if (meanCrossings <= 153)
                                    if (meanCrossings <= 150)
                                        pavementType = PavementType.Pavements.CobblestoneGood;  //Leaf 12
                                    else
                                        pavementType = PavementType.Pavements.AsphaltBad;       //Leaf 13
                                else
                                    pavementType = PavementType.Pavements.CobblestoneGood;      //Leaf 14
                            } else {
                                if (variance <= 25.60135)
                                    if (variance <= 21.476511)
                                        pavementType = PavementType.Pavements.AsphaltBad;       //Leaf 15
                                    else if (max <= 16.272984)
                                        pavementType = PavementType.Pavements.CobblestoneBad;   //Leaf 16
                                    else
                                        pavementType = PavementType.Pavements.AsphaltBad;       //Leaf 17
                                else if (variance <= 53.546981)
                                    if (range <= 40.301949)
                                        pavementType = PavementType.Pavements.CobblestoneGood;  //Leaf 18
                                    else
                                        pavementType = PavementType.Pavements.AsphaltBad;       //Leaf 19
                                else
                                    pavementType = PavementType.Pavements.CobblestoneGood;      //Leaf 20
                            }
                        }
                        //Branch #2.1.1.2
                    } else //meanCrossings > 163
                        pavementType = PavementType.Pavements.CobblestoneGood;                  //Leaf 21
                    //Branch #2.1.2
                }else{ //max > 17.969101
                    if(max <= 18.280821)
                        if(variance <= 38.792347)
                            pavementType = PavementType.Pavements.AsphaltBad;                   //Leaf 22
                        else
                        if(meanCrossings <= 165)
                            pavementType = PavementType.Pavements.AsphaltBad;                   //Leaf 23
                        else
                            pavementType = PavementType.Pavements.CobblestoneGood;              //Leaf 24
                    else
                        pavementType = PavementType.Pavements.AsphaltBad;                       //Leaf 25
                }
                //Branch #2.2
            }else{ //variance > 77.334899
                if(variance <= 102.585187)
                    if(range <= 50.503662)
                        pavementType = PavementType.Pavements.CobblestoneBad;                   //Leaf 26
                    else
                        pavementType = PavementType.Pavements.AsphaltBad;                       //Leaf 27
                else
                    pavementType = PavementType.Pavements.CobblestoneBad;                       //Leaf 28
            }
        }


        if(VERBOSE)
            Log.d(LOG_TAG, NAME + " has determined this route to be '" + pavementType + "'.");

        classification = generateClassificationStub(featureVector);
        classification.addProperty(CLASSIFICATION, String.valueOf(pavementType));
        return classification;
    }
}
