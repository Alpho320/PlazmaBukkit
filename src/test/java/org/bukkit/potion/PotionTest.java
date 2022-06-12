package org.bukkit.potion;

import static org.junit.Assert.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.bukkit.NamespacedKey;
import org.bukkit.support.AbstractTestingBase;
import org.junit.Test;

public class PotionTest extends AbstractTestingBase {
    @Test
    public void testEffectCompleteness() throws Throwable {
        Map<PotionType, String> effects = new EnumMap(PotionType.class);
        for (Object reg : BuiltInRegistries.POTION) {
            List<MobEffectInstance> eff = ((Potion) reg).getEffects();
            if (eff.size() != 1) continue;
            int id = MobEffect.getId(eff.get(0).getEffect());
            PotionEffectType type = PotionEffectType.getById(id);
            assertNotNull(String.valueOf(id), PotionEffectType.getById(id));

            PotionType enumType = PotionType.getByEffect(type);
            assertNotNull(type.getName(), enumType);

            effects.put(enumType, enumType.name());
        }

        assertEquals(effects.entrySet().size(), PotionType.values().length - /* PotionTypes with no/shared Effects */ 6);
    }

    @Test
    public void testEffectType() {
        for (MobEffect nms : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(nms);

            int id = MobEffect.getId(nms);
            PotionEffectType bukkit = PotionEffectType.getById(id);

            assertNotNull("No Bukkit type for " + key, bukkit);
            assertFalse("No name for " + key, bukkit.getName().contains("UNKNOWN"));

            PotionEffectType byName = PotionEffectType.getByName(bukkit.getName());
            assertEquals("Same type not returned by name " + key, bukkit, byName);
        }
    }

    // Purpur start
    @Test
    public void testNamespacedKey() {
        NamespacedKey key = new NamespacedKey("testnamespace", "testkey");
        PotionEffect namedSpacedEffect = new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20, 0, true, true, true, key);
        assertNotNull(namedSpacedEffect.getKey());
        assertTrue(namedSpacedEffect.hasKey());
        assertFalse(namedSpacedEffect.withKey(null).hasKey());

        PotionEffect effect = new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20, 0, true, true, true);
        assertNull(effect.getKey());
        assertFalse(effect.hasKey());
        assertTrue(namedSpacedEffect.withKey(key).hasKey());

        Map<String, Object> s1 = namedSpacedEffect.serialize();
        Map<String, Object> s2 = effect.serialize();
        assertTrue(s1.containsKey("namespacedKey"));
        assertFalse(s2.containsKey("namespacedKey"));
        assertNotNull(new PotionEffect(s1).getKey());
        assertNull(new PotionEffect(s2).getKey());
    }
    // Purpur end
}
