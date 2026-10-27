
package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "nearby_places")
public class NearbyPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "research_id", nullable = false)
    private UUID researchId;

    @Column(name = "osm_type", nullable = false, length = 12)
    private String osmType;

    @Column(name = "osm_id", nullable = false)
    private long osmId;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(nullable = false, length = 150)
    private String category;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "distance_miles", nullable = false)
    private double distanceMiles;

    @Column(length = 500)
    private String website;

    @Column(name = "source_url", nullable = false, length = 250)
    private String sourceUrl;

    public UUID getResearchId() { return researchId; }
    public void setResearchId(UUID value) { researchId = value; }
    public String getOsmType() { return osmType; }
    public void setOsmType(String value) { osmType = value; }
    public long getOsmId() { return osmId; }
    public void setOsmId(long value) { osmId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double value) { latitude = value; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double value) { longitude = value; }
    public double getDistanceMiles() { return distanceMiles; }
    public void setDistanceMiles(double value) { distanceMiles = value; }
    public String getWebsite() { return website; }
    public void setWebsite(String value) { website = value; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String value) { sourceUrl = value; }
}
