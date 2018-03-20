package com.android.launcher3.executor;

import java.util.HashMap;
import java.util.Map;

class StateHandlerFactory {
    private static Map<String, ExecutorState> mStateIdMap = new HashMap();

    static {
        for (ExecutorState state : ExecutorState.values()) {
            mStateIdMap.put(state.toString(), state);
        }
    }

    StateHandler createHandler(String stateId) {
        ExecutorState state = (ExecutorState) mStateIdMap.get(stateId);
        if (state == null || state.getCreator() == null) {
            return null;
        }
        return state.getCreator().create();
    }
}
