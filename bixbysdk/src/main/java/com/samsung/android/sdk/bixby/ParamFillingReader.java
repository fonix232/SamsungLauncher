package com.samsung.android.sdk.bixby;

import com.samsung.android.sdk.bixby.data.CHObject;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.ScreenParameter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ParamFillingReader {
    ParamFillingReader() {
    }

    public static ParamFilling read(String json) throws IllegalArgumentException {
        List<String> screenStates = new ArrayList();
        List<ScreenParameter> screenParameters = new ArrayList();
        try {
            JSONObject obj = new JSONObject(json);
            String utterance = obj.getString("utterance");
            String intent = obj.getString("intent");
            String appName = obj.getString("appName");
            JSONArray screenStatesObj = obj.getJSONArray("screenStates");
            for (int n = 0; n < screenStatesObj.length(); n++) {
                screenStates.add(screenStatesObj.optString(n));
            }
            JSONArray screenParamsObj = obj.getJSONArray("screenParameters");
            for (int i = 0; i < screenParamsObj.length(); i++) {
                JSONObject parameterObj = screenParamsObj.getJSONObject(i);
                ScreenParameter screenParameter = new ScreenParameter();
                if (parameterObj.has("slotType")) {
                    screenParameter.setSlotType(parameterObj.getString("slotType"));
                } else {
                    screenParameter.setSlotType("");
                }
                if (parameterObj.has("slotName")) {
                    screenParameter.setSlotName(parameterObj.getString("slotName"));
                } else {
                    screenParameter.setSlotName("");
                }
                if (parameterObj.has("slotValue")) {
                    screenParameter.setSlotValue(parameterObj.getString("slotValue"));
                } else {
                    screenParameter.setSlotValue("");
                }
                if (parameterObj.has("CH_ObjectType")) {
                    screenParameter.setCHObjectType(parameterObj.getString("CH_ObjectType"));
                } else {
                    screenParameter.setCHObjectType("");
                }
                if (parameterObj.has("CH_Objects")) {
                    List<CHObject> CHObjectsList = new ArrayList();
                    JSONArray CHObjects = parameterObj.getJSONArray("CH_Objects");
                    for (int j = 0; j < CHObjects.length(); j++) {
                        JSONObject CHObjectObj = CHObjects.getJSONObject(j);
                        CHObject chObject = new CHObject();
                        if (CHObjectObj.has("CH_Type")) {
                            chObject.setCHType(CHObjectObj.getString("CH_Type"));
                        } else {
                            chObject.setCHType("");
                        }
                        if (CHObjectObj.has("CH_Value")) {
                            chObject.setCHValue(CHObjectObj.getString("CH_Value"));
                        } else {
                            chObject.setCHValue("");
                        }
                        if (CHObjectObj.has("CH_ValueType")) {
                            chObject.setCHValueType(CHObjectObj.getString("CH_ValueType"));
                        } else {
                            chObject.setCHValueType("");
                        }
                        CHObjectsList.add(chObject);
                    }
                    screenParameter.setCHObjects(CHObjectsList);
                } else {
                    screenParameter.setCHObjects(null);
                }
                if (parameterObj.has("parameterName")) {
                    screenParameter.setParameterName(parameterObj.getString("parameterName"));
                } else {
                    screenParameter.setParameterName("");
                }
                if (parameterObj.has("parameterType")) {
                    screenParameter.setParameterType(parameterObj.getString("parameterType"));
                } else {
                    screenParameter.setParameterType("");
                }
                screenParameters.add(screenParameter);
            }
            return new ParamFilling(utterance, intent, appName, screenStates, screenParameters);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
}
