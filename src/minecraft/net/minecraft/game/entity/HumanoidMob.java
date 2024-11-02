package net.minecraft.game.entity;


import org.lwjgl.opengl.GL11;

import net.minecraft.game.entity.monster.EntityMob;
import net.minecraft.game.level.World;

public class HumanoidMob extends EntityMob {

	public HumanoidMob(World world) {
		super(world);
		this.worldObj = world;
		this.setPosition(0.0F, 0.0F, 0.0F);
	}
}