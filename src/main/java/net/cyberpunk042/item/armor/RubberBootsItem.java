package net.cyberpunk042.item.armor;

import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;

public class RubberBootsItem extends Item {
	public RubberBootsItem(Item.Settings settings) {
		super(settings.armor(ArmorMaterials.LEATHER, EquipmentType.BOOTS));
	}
}

