package com.hust.baseweb.applications.education.entity;

import com.hust.baseweb.entity.UserLogin;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ClassRegistrationId implements Serializable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private EduClass eduClass;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private UserLogin student;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassRegistrationId)) {
            return false;
        }

        ClassRegistrationId id = (ClassRegistrationId) o;

        return Objects.equals(getEduClass().getId(), id.getEduClass().getId()) &&
               Objects.equals(getStudent().getUserLoginId(), id.getStudent().getUserLoginId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEduClass().getId(), getStudent().getUserLoginId());
    }
}
