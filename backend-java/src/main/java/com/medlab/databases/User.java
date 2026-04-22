package com.medlab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String realName;

    @Column(unique = true, nullable = false, length = 50)
    private String idNumber;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    private String drugAllergy;

    @Column(columnDefinition = "TEXT")
    private String lifetimeMedicalHistory;

    public User() {
    }

    public User(UUID id, String realName, String idNumber, String passwordHash, String drugAllergy, String lifetimeMedicalHistory) {
        this.id = id;
        this.realName = realName;
        this.idNumber = idNumber;
        this.passwordHash = passwordHash;
        this.drugAllergy = drugAllergy;
        this.lifetimeMedicalHistory = lifetimeMedicalHistory;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDrugAllergy() {
        return drugAllergy;
    }

    public void setDrugAllergy(String drugAllergy) {
        this.drugAllergy = drugAllergy;
    }

    public String getLifetimeMedicalHistory() {
        return lifetimeMedicalHistory;
    }

    public void setLifetimeMedicalHistory(String lifetimeMedicalHistory) {
        this.lifetimeMedicalHistory = lifetimeMedicalHistory;
    }
}
