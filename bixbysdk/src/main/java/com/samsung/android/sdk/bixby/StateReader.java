package com.samsung.android.sdk.bixby;

import com.samsung.android.sdk.bixby.data.CHObject;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class StateReader {
    StateReader() {
    }

    public static State read(String json) throws IllegalArgumentException {
        List<Parameter> parameterList = new ArrayList();
        try {
            String specVer;
            boolean z;
            Boolean isLastState;
            String subIntent;
            JSONObject jSONObject = new JSONObject(json);
            if (jSONObject.has("specVer")) {
                specVer = jSONObject.getString("specVer");
            } else {
                specVer = BuildConfig.VERSION_NAME;
            }
            Integer stepNum = Integer.valueOf(jSONObject.getInt("seqNum"));
            Boolean isExecuted = Boolean.valueOf(jSONObject.getBoolean("isExecuted"));
            String appName = jSONObject.getString("appName");
            String stateId = jSONObject.getString("stateId");
            String ruleId = jSONObject.getString("ruleId");
            if (jSONObject.has("isResent")) {
                z = jSONObject.getBoolean("isResent");
            } else {
                z = false;
            }
            Boolean isResent = Boolean.valueOf(z);
            Boolean isLandingState = Boolean.valueOf(jSONObject.getBoolean("isLandingState"));
            if (jSONObject.has("isLastState")) {
                isLastState = Boolean.valueOf(jSONObject.getBoolean("isLastState"));
            } else {
                isLastState = Boolean.valueOf(false);
            }
            if (jSONObject.has("subIntent")) {
                subIntent = jSONObject.getString("subIntent");
            } else {
                subIntent = "";
            }
            JSONArray parameters = jSONObject.getJSONArray("parameters");
            for (int i = 0; i < parameters.length(); i++) {
                JSONObject parameterObj = parameters.getJSONObject(i);
                Parameter parameter = new Parameter();
                if (parameterObj.has("slotType")) {
                    parameter.setSlotType(parameterObj.getString("slotType"));
                } else {
                    parameter.setSlotType("");
                }
                if (parameterObj.has("slotName")) {
                    parameter.setSlotName(parameterObj.getString("slotName"));
                } else {
                    parameter.setSlotName("");
                }
                if (parameterObj.has("slotValue")) {
                    parameter.setSlotValue(parameterObj.getString("slotValue"));
                } else {
                    parameter.setSlotValue("");
                }
                if (parameterObj.has("slotValueType")) {
                    parameter.setSlotValueType(parameterObj.getString("slotValueType"));
                } else {
                    parameter.setSlotValueType("");
                }
                if (parameterObj.has("CH_ObjectType")) {
                    parameter.setCHObjectType(parameterObj.getString("CH_ObjectType"));
                } else {
                    parameter.setCHObjectType("");
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
                    parameter.setCHObjects(CHObjectsList);
                } else {
                    parameter.setCHObjects(null);
                }
                if (parameterObj.has("parameterName")) {
                    parameter.setParameterName(parameterObj.getString("parameterName"));
                } else {
                    parameter.setParameterName("");
                }
                if (parameterObj.has("parameterType")) {
                    parameter.setParameterType(parameterObj.getString("parameterType"));
                } else {
                    parameter.setParameterType("");
                }
                if (parameterObj.has("isMandatory")) {
                    Parameter parameter2 = parameter;
                    parameter2.setIsMandatory(Boolean.valueOf(parameterObj.getBoolean("isMandatory")));
                } else {
                    parameter.setIsMandatory(Boolean.valueOf(false));
                }
                parameterList.add(parameter);
            }
            return new State(specVer, stepNum, isExecuted, appName, ruleId, stateId, isResent, isLandingState, isLastState, subIntent, parameterList);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
}
