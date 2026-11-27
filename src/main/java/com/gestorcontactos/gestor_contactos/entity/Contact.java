package com.gestorcontactos.gestor_contactos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "contacts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_contacts_document", columnNames = {"document_type", "document_number"})
}, indexes = {
        @Index(name = "idx_contacts_status", columnList = "status"),
        @Index(name = "idx_contacts_person_type", columnList = "person_type"),
        @Index(name = "idx_contacts_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_contacts_first_name", columnList = "first_name"),
        @Index(name = "idx_contacts_last_name", columnList = "last_name"),
        @Index(name = "idx_contacts_legal_name", columnList = "legal_name")
})
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 15)
    private PersonType personType;

    @Column(name = "first_name", length = 100)
    private String firstNames;

    @Column(name = "last_name", length = 100)
    private String lastNames;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 25)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 11)
    private String documentNumber;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContactStatus status = ContactStatus.ACTIVO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_contacts_created_by"))
    private User createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_contacts_updated_by"))
    private User updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", foreignKey = @ForeignKey(name = "fk_contacts_deleted_by"))
    private User deletedBy;

    @OneToMany(mappedBy = "contact", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ContactPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "contact", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ContactEmail> emails = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contact_commercial_types",
            joinColumns = @JoinColumn(name = "contact_id", nullable = false,
                    foreignKey = @ForeignKey(name = "fk_contact_commercial_types_contact")),
            uniqueConstraints = @UniqueConstraint(name = "pk_contact_commercial_types",
                    columnNames = {"contact_id", "commercial_type"}),
            indexes = @Index(name = "idx_contact_commercial_type", columnList = "commercial_type, contact_id"))
    @Column(name = "commercial_type", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private Set<CommercialType> commercialTypes = new HashSet<>();

    protected Contact() {
    }

    public Contact(PersonType personType, DocumentType documentType, String documentNumber, String address,
            LocalDateTime createdAt, User createdBy) {
        this.personType = personType;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.address = address;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Contact(PersonType personType, String firstNames, String lastNames, DocumentType documentType,
            String documentNumber, String address, LocalDateTime createdAt, User createdBy) {
        this(personType, documentType, documentNumber, address, createdAt, createdBy);
        this.firstNames = firstNames;
        this.lastNames = lastNames;
    }

    public Contact(PersonType personType, String legalName, DocumentType documentType, String documentNumber,
            String address, LocalDateTime createdAt, User createdBy) {
        this(personType, documentType, documentNumber, address, createdAt, createdBy);
        this.legalName = legalName;
    }

    public void addPhone(ContactPhone phone) {
        Objects.requireNonNull(phone, "phone must not be null");
        phones.add(phone);
        phone.assignContact(this);
    }

    public void removePhone(ContactPhone phone) {
        if (phones.remove(phone)) {
            phone.assignContact(null);
        }
    }

    public void addEmail(ContactEmail email) {
        Objects.requireNonNull(email, "email must not be null");
        emails.add(email);
        email.assignContact(this);
    }

    public void removeEmail(ContactEmail email) {
        if (emails.remove(email)) {
            email.assignContact(null);
        }
    }

    public void addCommercialType(CommercialType commercialType) {
        commercialTypes.add(Objects.requireNonNull(commercialType, "commercialType must not be null"));
    }

    public void removeCommercialType(CommercialType commercialType) {
        commercialTypes.remove(commercialType);
    }

    public Long getId() {
        return id;
    }

    public PersonType getPersonType() {
        return personType;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastNames() {
        return lastNames;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    public List<ContactPhone> getPhones() {
        return phones;
    }

    public List<ContactEmail> getEmails() {
        return emails;
    }

    public Set<CommercialType> getCommercialTypes() {
        return commercialTypes;
    }
}
