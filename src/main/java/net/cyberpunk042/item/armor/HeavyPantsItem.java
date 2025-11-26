package net.cyberpunk042.item.armor;

import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;

public class HeavyPantsItem extends Item {
	public HeavyPantsItem(Item.Settings settings) {
		super(settings.armor(ArmorMaterials.NETHERITE, EquipmentType.LEGGINGS));
	}
}

