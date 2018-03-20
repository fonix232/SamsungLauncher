package com.android.launcher3.executor;

import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.proxy.OnLauncherTopViewChangedListener;
import com.samsung.android.sdk.bixby.BixbyApi;
import com.samsung.android.sdk.bixby.BixbyApi.InterimStateListener;
import com.samsung.android.sdk.bixby.BixbyApi.NlgParamMode;
import com.samsung.android.sdk.bixby.BixbyApi.ResponseResults;
import com.samsung.android.sdk.bixby.BixbyApi.StartStateListener;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.ScreenStateInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

public class StateManager {
    public static boolean DEBUG_MODE = false;
    private static final String TAG = StateManager.class.getSimpleName();
    private BixbyApi mBixbyApi = BixbyApi.getInstance();
    private String mCurrentState = null;
    private StateHandlerFactory mStateHandlerFactory;
    private TopViewListener mTopViewListener = new TopViewListener();
    private String mTopViewState = ExecutorState.HOME.toString();

    final class TopViewListener implements OnLauncherTopViewChangedListener {
        String mPrevViewState = ExecutorState.HOME.toString();

        TopViewListener() {
        }

        public void onLauncherTopViewChanged(int topView) {
            StateManager.this.mTopViewState = StateUtils.getStateIdFromViewId(topView);
            if (BixbyApi.isBixbySupported()) {
                Log.d(StateManager.TAG, "onLauncherTopViewChanged() : " + StateManager.this.mTopViewState);
                if (!this.mPrevViewState.equals(StateManager.this.mTopViewState)) {
                    StateManager.this.mBixbyApi.logExitState(this.mPrevViewState);
                }
                if (StateManager.this.mTopViewState != null) {
                    StateManager.this.mBixbyApi.logEnterState(StateManager.this.mTopViewState);
                    this.mPrevViewState = StateManager.this.mTopViewState;
                }
            }
        }
    }

    public boolean isRuleRunning() {
        return this.mBixbyApi.isRuleRunning();
    }

    public StateManager() {
        InterimStateListener internalStateListener = new InterimStateListener() {
            public void onStateReceived(State state) {
                StateManager.this.onStateReceived(state);
            }

            public ScreenStateInfo onScreenStatesRequested() {
                Log.d(StateManager.TAG, "onScreenStatesRequested() : " + StateManager.this.mTopViewState);
                return new ScreenStateInfo(StateManager.this.mTopViewState);
            }

            public boolean onParamFillingReceived(ParamFilling pf) {
                Log.d(StateManager.TAG, "onParamFillingReceived() : " + StateManager.this.mTopViewState);
                return LauncherAppState.getInstance().getLauncherProxy().onParamFillingReceived(StateManager.this.mTopViewState, pf);
            }

            public void onRuleCanceled(String ruleId) {
                Log.d(StateManager.TAG, "onRuleCanceled() : " + ruleId);
            }
        };
        this.mBixbyApi.setStartStateListener(new StartStateListener() {
            public void onStateReceived(State state) {
                Log.d(StateManager.TAG, "Start Rule : " + state.getRuleId());
                StateManager.DEBUG_MODE = StateManager.this.mBixbyApi.isTestMode();
                StateManager.this.onStateReceived(state);
            }

            public void onRuleCanceled(String ruleId) {
                Log.d(StateManager.TAG, "onRuleCanceled() : " + ruleId);
            }
        });
        this.mBixbyApi.setInterimStateListener(internalStateListener);
        this.mStateHandlerFactory = new StateHandlerFactory();
    }

    private void onStateReceived(final State state) {
        Log.d(TAG, "onStateReceived() : " + state.getStateId());
        this.mCurrentState = state.getStateId();
        final StateHandler stateHandler = this.mStateHandlerFactory.createHandler(this.mCurrentState);
        if (stateHandler == null) {
            Log.e(TAG, "Not supported ExecutorState : " + state.getStateId());
            this.mBixbyApi.sendResponse(ResponseResults.SUCCESS);
        } else if (!LauncherAppState.getInstance().getLauncherProxy().isHomeOnlyMode() || stateHandler.isAllowedInHomeOnlyMode()) {
            if ("PARAM_CHECK_OK".equals(stateHandler.parseParameters(state))) {
                stateHandler.execute(new StateExecutionCallback() {
                    public void executionCompleted(boolean success) {
                        try {
                            NlgRequestInfo nlgInfo = stateHandler.getNlgRequestInfo();
                            if (nlgInfo != null && (state.isLastState().booleanValue() || !success)) {
                                StateManager.this.mBixbyApi.requestNlg(nlgInfo, NlgParamMode.MULTIPLE);
                                Log.i(StateManager.TAG, (success ? "Success - " : " Fail - ") + nlgInfo.toString());
                            }
                            if (success || StateManager.DEBUG_MODE) {
                                StateManager.this.mBixbyApi.sendResponse(ResponseResults.SUCCESS);
                            } else {
                                StateManager.this.mBixbyApi.sendResponse(ResponseResults.FAILURE);
                            }
                        } catch (Exception e) {
                            Log.e(StateManager.TAG, "execute error : " + e.getMessage());
                        }
                    }
                });
                return;
            }
            Log.e(TAG, "Error parseParameters : " + state.getStateId());
            Map<String, Parameter> params = state.getParamMap();
            if (params != null) {
                Log.e(TAG, "Parameters size : " + params.size());
                int keyCnt = 1;
                for (String pKey : params.keySet()) {
                    Log.i(TAG, "Key_" + keyCnt + " : " + pKey);
                    keyCnt++;
                }
            }
            NlgRequestInfo nlgInfo = stateHandler.getNlgRequestInfo();
            if (nlgInfo != null) {
                this.mBixbyApi.requestNlg(nlgInfo, NlgParamMode.MULTIPLE);
                Log.i(TAG, "Param error : " + nlgInfo.toString());
            }
            if (DEBUG_MODE) {
                this.mBixbyApi.sendResponse(ResponseResults.SUCCESS);
            } else {
                this.mBixbyApi.sendResponse(ResponseResults.FAILURE);
            }
        } else {
            Log.e(TAG, "Not allowed ExecutorState in HomeOnlyMode : " + state.getStateId());
            this.mBixbyApi.requestNlg(new NlgRequestInfo("NLG_PRECONDITION").addScreenParam("Home", "HomeAndAppsScreenAlreadySet", "no"), NlgParamMode.MULTIPLE);
            this.mBixbyApi.sendResponse(ResponseResults.FAILURE);
        }
    }

    public TopViewListener getTopViewListener() {
        return this.mTopViewListener;
    }
}
