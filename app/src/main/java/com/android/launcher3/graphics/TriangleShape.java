package com.android.launcher3.graphics;

import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.support.annotation.NonNull;

public class TriangleShape extends PathShape {
    private Path mTriangularPath;

    public TriangleShape(Path path, float stdWidth, float stdHeight) {
        super(path, stdWidth, stdHeight);
        this.mTriangularPath = path;
    }

    public static TriangleShape create(float width, float height, boolean isPointingUp) {
        Path triangularPath = new Path();
        if (isPointingUp) {
            triangularPath.moveTo(0.0f, height);
            triangularPath.lineTo(width, height);
            triangularPath.lineTo(width / 2.0f, 0.0f);
            triangularPath.close();
        } else {
            triangularPath.moveTo(0.0f, 0.0f);
            triangularPath.lineTo(width / 2.0f, height);
            triangularPath.lineTo(width, 0.0f);
            triangularPath.close();
        }
        return new TriangleShape(triangularPath, width, height);
    }

    public void getOutline(@NonNull Outline outline) {
        outline.setConvexPath(this.mTriangularPath);
    }
}
