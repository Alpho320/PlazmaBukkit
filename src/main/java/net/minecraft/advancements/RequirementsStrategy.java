package net.minecraft.advancements;

import java.util.Collection;

public interface RequirementsStrategy {
    RequirementsStrategy AND = (criteriaNames) -> {
        String[][] strings = new String[criteriaNames.size()][];
        int i = 0;

        for(String string : criteriaNames) {
            strings[i++] = new String[]{string};
        }

        return strings;
    };
    RequirementsStrategy OR = (criteriaNames) -> {
        return new String[][]{criteriaNames.toArray(new String[0])};
    };

    String[][] createRequirements(Collection<String> criteriaNames);
}
