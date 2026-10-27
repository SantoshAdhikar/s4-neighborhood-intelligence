
package com.s4.neighborhood.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_profiles")
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 120)
    private String industry;

    @Column(name = "street_address", nullable = false, length = 250)
    private String streetAddress;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(length = 500)
    private String website;

    @Column(nullable = false, columnDefinition = "text")
    private String services;

    @Column(name = "primary_goal", nullable = false, length = 1000)
    private String primaryGoal;

    @Column(name = "research_radius_miles", nullable = false)
    private int researchRadiusMiles;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getIndustry() { return industry; }
    public void setIndustry(String value) { industry = value; }
    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String value) { streetAddress = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = value; }
    public String getWebsite() { return website; }
    public void setWebsite(String value) { website = value; }
    public String getServices() { return services; }
    public void setServices(String value) { services = value; }
    public String getPrimaryGoal() { return primaryGoal; }
    public void setPrimaryGoal(String value) { primaryGoal = value; }
    public int getResearchRadiusMiles() { return researchRadiusMiles; }
    public void setResearchRadiusMiles(int value) { researchRadiusMiles = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
