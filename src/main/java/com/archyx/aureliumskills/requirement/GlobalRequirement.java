package com.archyx.aureliumskills.requirement;

import com.archyx.aureliumskills.skills.Skill;
import com.cryptomorin.xseries.XMaterial;

import java.util.Map;

public class GlobalRequirement {

    private final XMaterial material;
    private final Map<Skill, Integer> requirements;

    public GlobalRequirement(XMaterial material, Map<Skill, Integer> requirements) {
        this.material = material;
        this.requirements = requirements;
    }

    public XMaterial getMaterial() {
        return material;
    }

    public Map<Skill, Integer> getRequirements() {
        return requirements;
    }

}
