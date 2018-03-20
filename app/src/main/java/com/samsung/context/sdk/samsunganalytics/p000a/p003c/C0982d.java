package com.samsung.context.sdk.samsunganalytics.p000a.p003c;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.c.d */
public class C0982d implements C0770a {
    /* renamed from: a */
    private static final String f197a = "\u0007";

    /* renamed from: a */
    public String mo1453a(String str, Throwable th) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        if (th != null) {
            stringBuilder.append(th.getClass().getName());
            stringBuilder.append(":");
            stringBuilder.append(th.getLocalizedMessage());
            stringBuilder.append(f197a);
            for (StackTraceElement stackTraceElement : th.getStackTrace()) {
                stringBuilder.append(stackTraceElement.toString());
                stringBuilder.append(f197a);
                if (stringBuilder.length() > 700) {
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }
}
