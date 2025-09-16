package com.kushmakar.akashjewellers;



public class PriceData {
    private Double gold_price;
    private Double silver_price;
    private Double gold_rtgs_price;
    private Double silver_rtgs_price;
    private Long timestamp; // Firebase ServerValue.TIMESTAMP is stored as Long

    // IMPORTANT: Required default constructor for Firebase Database mapping
    public PriceData() {
    }

    // --- Getters ---
    // (Firebase needs getters to populate the object)
    public Double getGold_price() {
        return gold_price;
    }

    public Double getSilver_price() {
        return silver_price;
    }

    public Double getGold_rtgs_price() {
        return gold_rtgs_price;
    }

    public Double getSilver_rtgs_price() {
        return silver_rtgs_price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    // --- Optional: Setters (not strictly required for reading) ---
    public void setGold_price(Double gold_price) { this.gold_price = gold_price; }
    public void setSilver_price(Double silver_price) { this.silver_price = silver_price; }
    public void setGold_rtgs_price(Double gold_rtgs_price) { this.gold_rtgs_price = gold_rtgs_price; }
    public void setSilver_rtgs_price(Double silver_rtgs_price) { this.silver_rtgs_price = silver_rtgs_price; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}