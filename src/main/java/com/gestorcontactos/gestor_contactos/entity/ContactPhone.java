package com.gestorcontactos.gestor_contactos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "contact_phones", uniqueConstraints = @UniqueConstraint(name = "uq_contact_phone",
        columnNames = {"contact_id", "phone_type", "area_code", "phone_number"}),
        indexes = @Index(name = "idx_contact_phones_contact_id", columnList = "contact_id"))
public class ContactPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_contact_phones_contact"))
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type", nullable = false, length = 10)
    private PhoneType phoneType;

    @Column(name = "area_code", nullable = false, length = 5)
    private String areaCode;

    @Column(name = "phone_number", nullable = false, length = 9)
    private String phoneNumber;

    protected ContactPhone() {
    }

    public ContactPhone(PhoneType phoneType, String areaCode, String phoneNumber) {
        this.phoneType = phoneType;
        this.areaCode = areaCode;
        this.phoneNumber = phoneNumber;
    }

    void assignContact(Contact contact) {
        this.contact = contact;
    }

    public Long getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    public PhoneType getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(PhoneType phoneType) {
        this.phoneType = phoneType;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
