package com.android.launcher3.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;

public enum LightingEffectManager {
    INSTANCE;
    
    private static final int ANIMATION_HIDING = 0;
    private static final int ANIMATION_SHOWING = 1;
    public static final int DIRECTION_BOTTOM = 1;
    public static final int DIRECTION_TOP = 0;
    private static final String TAG = "LightingEffectManager";
    private Animator mEffectAnim;
    private ImageView mEffectBottomOff;
    private ImageView mEffectBottomOn;
    private View mEffectLayer;
    private ImageView mEffectTopOff;
    private ImageView mEffectTopOn;
    private boolean mExceptBottom;
    private Launcher mLauncher;

    public void setup(Launcher launcher) {
        this.mEffectLayer = launcher.findViewById(R.id.lighting_effect);
        this.mLauncher = launcher;
        if (this.mEffectLayer != null) {
            this.mEffectTopOn = (ImageView) this.mEffectLayer.findViewById(R.id.lighting_effect_top_on);
            this.mEffectTopOff = (ImageView) this.mEffectLayer.findViewById(R.id.lighting_effect_top_off);
            this.mEffectBottomOn = (ImageView) this.mEffectLayer.findViewById(R.id.lighting_effect_bottom_on);
            this.mEffectBottomOff = (ImageView) this.mEffectLayer.findViewById(R.id.lighting_effect_bottom_off);
        }
    }

    public void showEffect(final boolean toBeShown, int animDuration, boolean exceptBottom) {
        float f = 1.0f;
        if (this.mEffectLayer != null) {
            if (this.mEffectAnim != null) {
                if (isShowingAnimation() != toBeShown || (this.mExceptBottom != exceptBottom && toBeShown)) {
                    this.mEffectAnim.cancel();
                } else {
                    return;
                }
            } else if (!toBeShown && this.mEffectLayer.getVisibility() == 8) {
                return;
            } else {
                if (!toBeShown || this.mEffectLayer.getVisibility() != 0 || this.mExceptBottom != exceptBottom) {
                    if (this.mEffectLayer.getVisibility() != 0) {
                        float f2;
                        View view = this.mEffectLayer;
                        if (toBeShown) {
                            f2 = 0.0f;
                        } else {
                            f2 = 1.0f;
                        }
                        view.setAlpha(f2);
                    }
                    if (toBeShown) {
                        this.mEffectLayer.setVisibility(4);
                    }
                } else {
                    return;
                }
            }
            if (toBeShown) {
                this.mExceptBottom = exceptBottom;
                if (this.mExceptBottom) {
                    this.mEffectBottomOn.setVisibility(View.GONE);
                    this.mEffectBottomOff.setVisibility(View.GONE);
                } else {
                    this.mEffectBottomOn.setVisibility(View.VISIBLE);
                    this.mEffectBottomOff.setVisibility(View.VISIBLE);
                }
            }
            View view2;
            if (animDuration > 0) {
                view2 = this.mEffectLayer;
                String name = View.ALPHA.getName();
                float[] fArr = new float[1];
                if (!toBeShown) {
                    f = 0.0f;
                }
                fArr[0] = f;
                this.mEffectAnim = LauncherAnimUtils.ofFloat(view2, name, fArr);
                this.mEffectAnim.setDuration((long) animDuration);
                setAnimationType(toBeShown);
                this.mEffectAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animation) {
                        if (toBeShown) {
                            LightingEffectManager.this.setLightingImage(LightingEffectManager.this.mLauncher);
                            LightingEffectManager.this.mEffectLayer.setVisibility(View.VISIBLE);
                        }
                    }

                    public void onAnimationEnd(Animator animation) {
                        LightingEffectManager.this.mEffectAnim = null;
                        if (!toBeShown) {
                            LightingEffectManager.this.mEffectTopOn.setBackground(null);
                            LightingEffectManager.this.mEffectTopOff.setBackground(null);
                            LightingEffectManager.this.mEffectBottomOn.setBackground(null);
                            LightingEffectManager.this.mEffectBottomOff.setBackground(null);
                            LightingEffectManager.this.mEffectLayer.setVisibility(View.GONE);
                            LightingEffectManager.this.mEffectTopOn.setAlpha(0.0f);
                            LightingEffectManager.this.mEffectBottomOn.setAlpha(0.0f);
                        }
                    }
                });
                this.mEffectAnim.start();
                return;
            }
            int i;
            view2 = this.mEffectLayer;
            if (!toBeShown) {
                f = 0.0f;
            }
            view2.setAlpha(f);
            View view3 = this.mEffectLayer;
            if (toBeShown) {
                i = 0;
            } else {
                i = 8;
            }
            view3.setVisibility(i);
        }
    }

    public void turnOnEachLight(int direction, boolean turnOn) {
        if (this.mEffectLayer != null && this.mEffectLayer.getVisibility() == 0) {
            if (this.mEffectAnim == null || isShowingAnimation()) {
                float toAlpha = turnOn ? 1.0f : 0.0f;
                View targetView = direction == 0 ? this.mEffectTopOn : this.mEffectBottomOn;
                if (targetView != null && targetView.getAlpha() != toAlpha) {
                    targetView.animate().alpha(toAlpha).start();
                }
            }
        }
    }

    public void turnOffAllLight() {
        if (this.mEffectLayer != null && this.mEffectLayer.getVisibility() == 0) {
            this.mEffectTopOn.animate().alpha(0.0f).start();
            this.mEffectBottomOn.animate().alpha(0.0f).start();
        }
    }

    private void setAnimationType(boolean showing) {
        if (this.mEffectAnim != null) {
            this.mEffectAnim.setStartDelay(showing ? 1 : 0);
        }
    }

    private boolean isShowingAnimation() {
        if (this.mEffectAnim != null) {
            return this.mEffectAnim.getStartDelay() == 1;
        } else {
            return false;
        }
    }

    public void setLightingImage(Context context) {
        if (this.mEffectLayer != null) {
            Resources res = context.getResources();
            if (((Launcher) context).getDeviceProfile().isLandscape) {
                this.mEffectTopOn.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_on_land));
                this.mEffectTopOff.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_off_land));
                this.mEffectBottomOn.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_on_land));
                this.mEffectBottomOff.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_off_land));
                return;
            }
            this.mEffectTopOn.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_on));
            this.mEffectTopOff.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_off));
            this.mEffectBottomOn.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_on));
            this.mEffectBottomOff.setImageDrawable(res.getDrawable(R.drawable.apps_top_lighting_off));
        }
    }
}
