
package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "neighborhood_researches")
public class NeighborhoodResearch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "address_key", nullable = false, length = 700)
    private String addressKey;

    @Column(name = "radius_miles", nullable = false)
    private int radiusMiles;

    @Column(name = "center_latitude", nullable = false)
    private double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private double centerLongitude;

    @Column(name = "matched_address", nullable = false, columnDefinition = "text")
    private String matchedAddress;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID value) { businessId = value; }
    public String getAddressKey() { return addressKey; }
    public void setAddressKey(String value) { addressKey = value; }
    public int getRadiusMiles() { return radiusMiles; }
    public void setRadiusMiles(int value) { radiusMiles = value; }
    public double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(double value) { centerLatitude = value; }
    public double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(double value) { centerLongitude = value; }
    public String getMatchedAddress() { return matchedAddress; }
    public void setMatchedAddress(String value) { matchedAddress = value; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant value) { fetchedAt = value; }
}
