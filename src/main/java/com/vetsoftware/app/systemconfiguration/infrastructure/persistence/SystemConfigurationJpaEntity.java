package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_configurations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_system_configurations_property_name", columnNames = {"property_name"})
})
@SQLDelete(sql = "UPDATE system_configurations SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class SystemConfigurationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_name", nullable = false, length = 100)
    private String propertyName;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected SystemConfigurationJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
