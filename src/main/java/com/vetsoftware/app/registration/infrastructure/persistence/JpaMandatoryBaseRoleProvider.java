package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.registration.application.port.out.MandatoryBaseRoleProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaMandatoryBaseRoleProvider implements MandatoryBaseRoleProvider {

    private final BaseRoleJpaRepository baseRoleJpaRepository;

    public JpaMandatoryBaseRoleProvider(BaseRoleJpaRepository baseRoleJpaRepository) {
        this.baseRoleJpaRepository = baseRoleJpaRepository;
    }

    @Override
    public List<BaseRoleData> findMandatory() {
        return baseRoleJpaRepository.findByMandatoryTrue().stream()
                .map(e -> new BaseRoleData(e.getId(), e.getName(), e.getCode()))
                .toList();
    }
}
