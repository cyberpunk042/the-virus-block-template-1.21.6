package net.cyberpunk042.item.armor;

import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;

public class CompositeElytraItem extends Item {
	public CompositeElytraItem(Item.Settings settings) {
		super(settings.armor(ArmorMaterials.NETHERITE, EquipmentType.CHESTPLATE));
	}
}

