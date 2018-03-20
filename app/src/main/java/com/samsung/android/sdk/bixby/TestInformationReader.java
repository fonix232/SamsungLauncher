package com.samsung.android.sdk.bixby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class TestInformationReader {

    public static class TestInformation {
        public static final String TYPE_SETUP = "setup";
        public static final String TYPE_TEARDOWN = "teardown";
        private Map<String, String> content = null;
        private String type = null;

        public TestInformation(String type, Map<String, String> content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getContent() {
            return this.content;
        }

        public void setContent(Map<String, String> content) {
            this.content = content;
        }
    }

    TestInformationReader() {
    }

    private static Map<String, String> getTestParams(JSONObject jObj) {
        HashMap<String, String> map = new HashMap();
        try {
            Iterator<String> it = jObj.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                map.put(key, jObj.getString(key));
            }
            if (map.isEmpty()) {
                return null;
            }
            return map;
        } catch (JSONException e) {
            return null;
        }
    }

    public static List<TestInformation> read(String tiString) {
        try {
            JSONArray jObj = new JSONArray(tiString);
            int len = jObj.length();
            if (len == 0) {
                return null;
            }
            List<TestInformation> tiList = new ArrayList();
            for (int n = 0; n < len; n++) {
                JSONObject jsonObject = jObj.getJSONObject(n);
                if (jsonObject.has("type")) {
                    String type = jsonObject.get("type").toString();
                    if (jsonObject.has("content")) {
                        tiList.add(new TestInformation(type, getTestParams(jsonObject.getJSONObject("content"))));
                    }
                }
            }
            return tiList;
        } catch (JSONException e) {
            return null;
        }
    }
}
