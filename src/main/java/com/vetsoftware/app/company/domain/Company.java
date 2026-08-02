package com.vetsoftware.app.company.domain;

import java.time.LocalDateTime;

public class Company {
  private Long id;
  private String name;
  private String identifier;
  private String address;
  private String contactNumber;
  private CityRef city;
  private MembershipRef membership;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public Company(
      Long id,
      String name,
      String identifier,
      String address,
      String contactNumber,
      CityRef city,
      MembershipRef membership,
      LocalDateTime createdDate,
      boolean enabled) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (identifier == null || identifier.isBlank())
      throw new IllegalArgumentException("identifier is required");
    if (identifier.length() > 50)
      throw new IllegalArgumentException("identifier must be 50 chars or less");
    if (city == null) throw new IllegalArgumentException("city is required");
    if (membership == null) throw new IllegalArgumentException("membership is required");
    this.id = id;
    this.name = name;
    this.identifier = identifier;
    this.address = address;
    this.contactNumber = contactNumber;
    this.city = city;
    this.membership = membership;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static Company create(
      String name,
      String identifier,
      String address,
      String contactNumber,
      CityRef city,
      MembershipRef membership) {
    return new Company(
        null,
        name,
        identifier,
        address,
        contactNumber,
        city,
        membership,
        LocalDateTime.now(),
        true);
  }

  public void update(
      String name,
      String identifier,
      String address,
      String contactNumber,
      CityRef city,
      MembershipRef membership) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
    if (identifier == null || identifier.isBlank())
      throw new IllegalArgumentException("identifier is required");
    if (identifier.length() > 50)
      throw new IllegalArgumentException("identifier must be 50 chars or less");
    if (city == null) throw new IllegalArgumentException("city is required");
    if (membership == null) throw new IllegalArgumentException("membership is required");
    this.name = name;
    this.identifier = identifier;
    this.address = address;
    this.contactNumber = contactNumber;
    this.city = city;
    this.membership = membership;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getAddress() {
    return address;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public CityRef getCity() {
    return city;
  }

  public MembershipRef getMembership() {
    return membership;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }
}
