package com.android.launcher3.allapps;

import android.graphics.Bitmap;

class ItemDetails {
    private Bitmap mIconBitmap;
    private String name;
    private String pkgName;
    private String price;
    private String rating;
    private String sellerName;

    ItemDetails() {
    }

    String getName() {
        return this.name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getItemSeller() {
        return this.sellerName;
    }

    void setItemSeller(String seller) {
        this.sellerName = seller;
    }

    String getPrice() {
        return this.price;
    }

    void setPrice(String price) {
        this.price = price;
    }

    Bitmap getIconImage() {
        return this.mIconBitmap;
    }

    void setIconImage(Bitmap image) {
        this.mIconBitmap = image;
    }

    String getRating() {
        return this.rating;
    }

    void setRating(String rank) {
        this.rating = rank;
    }

    String getPkgName() {
        return this.pkgName;
    }

    void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
}
