package com.gestorcontactos.gestor_contactos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "contact_emails", uniqueConstraints = @UniqueConstraint(name = "uq_contact_email",
        columnNames = {"contact_id", "email"}),
        indexes = @Index(name = "idx_contact_emails_contact_id", columnList = "contact_id"))
public class ContactEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_contact_emails_contact"))
    private Contact contact;

    @Column(nullable = false, length = 254)
    private String email;

    protected ContactEmail() {
    }

    public ContactEmail(String email) {
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
