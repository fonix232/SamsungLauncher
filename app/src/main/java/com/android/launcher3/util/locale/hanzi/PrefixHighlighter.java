package com.android.launcher3.util.locale.hanzi;

public class PrefixHighlighter {
    private PrefixHighlighterImplCHN mImpl = new PrefixHighlighterImplCHN();
    private final int mPrefixHighlightColor;

    public PrefixHighlighter(int prefixHighlightColor) {
        this.mPrefixHighlightColor = prefixHighlightColor;
    }

    public CharSequence apply(CharSequence text, String prefix) {
        return this.mImpl.doApply(text, prefix, this.mPrefixHighlightColor);
    }
}
