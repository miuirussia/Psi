/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: http://psi.vazkii.us/license.php
 *
 * File Created @ [30/01/2016, 16:09:44 (GMT)]
 */
package vazkii.psi.common.entity;

import java.awt.Color;

import com.google.common.base.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import vazkii.psi.api.cad.ICADColorizer;
import vazkii.psi.api.spell.ISpellContainer;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.common.Psi;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EntitySpellCircle extends Entity {

	public static final int CAST_TIMES = 20;
	public static final int CAST_DELAY = 5;
	public static final int LIVE_TIME = (CAST_TIMES + 2) * CAST_DELAY;

	private static final String TAG_COLORIZER = "colorizer";
	private static final String TAG_BULLET = "bullet";
	private static final String TAG_CASTER = "caster";
	private static final String TAG_TIME_ALIVE = "timeAlive";
	private static final String TAG_TIMES_CAST = "timesCast";

	private static final String TAG_LOOK_X = "savedLookX";
	private static final String TAG_LOOK_Y = "savedLookY";
	private static final String TAG_LOOK_Z = "savedLookZ";
	
	// Generics are borked :|
	public static final DataParameter COLORIZER_DATA = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.OPTIONAL_ITEM_STACK);
	private static final DataParameter BULLET_DATA = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.OPTIONAL_ITEM_STACK);
	private static final DataParameter CASTER_NAME = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.STRING);
	private static final DataParameter TIME_ALIVE = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.VARINT);
	private static final DataParameter TIMES_CAST = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.VARINT);

	private static final DataParameter LOOK_X = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.FLOAT);
	private static final DataParameter LOOK_Y = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.FLOAT);
	private static final DataParameter LOOK_Z = EntityDataManager.createKey(EntitySpellCircle.class, DataSerializers.FLOAT);
	
	public EntitySpellCircle(World worldIn) {
		super(worldIn);
		setSize(3F, 0F);
	}

	public EntitySpellCircle setInfo(EntityPlayer player, ItemStack colorizer, ItemStack bullet) {
		dataWatcher.set(COLORIZER_DATA, Optional.fromNullable(colorizer));
		dataWatcher.set(BULLET_DATA, Optional.of(bullet));
		dataWatcher.set(CASTER_NAME, player.getName());
		
		Vec3d lookVec = player.getLook(1F);
		dataWatcher.set(LOOK_X, (float) lookVec.xCoord);
		dataWatcher.set(LOOK_Y, (float) lookVec.yCoord);
		dataWatcher.set(LOOK_Z, (float) lookVec.zCoord);
		return this;	
	}

	@Override
	protected void entityInit() {
		dataWatcher.register(COLORIZER_DATA, Optional.of(new ItemStack(Blocks.stone)));
		dataWatcher.register(BULLET_DATA, Optional.of(new ItemStack(Blocks.stone)));
		dataWatcher.register(CASTER_NAME, "");
		dataWatcher.register(TIME_ALIVE, 0);
		dataWatcher.register(TIMES_CAST, 0);
		dataWatcher.register(LOOK_X, 0F);
		dataWatcher.register(LOOK_Y, 0F);
		dataWatcher.register(LOOK_Z, 0F);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tagCompound) {
		NBTTagCompound colorizerCmp = new NBTTagCompound();
		ItemStack colorizer = ((Optional<ItemStack>) dataWatcher.get(COLORIZER_DATA)).orNull();
		if(colorizer != null)
			colorizer.writeToNBT(colorizerCmp);
		tagCompound.setTag(TAG_COLORIZER, colorizerCmp);

		NBTTagCompound bulletCmp = new NBTTagCompound();
		ItemStack bullet = ((Optional<ItemStack>) dataWatcher.get(BULLET_DATA)).orNull();
		if(bullet != null)
			bullet.writeToNBT(bulletCmp);
		tagCompound.setTag(TAG_BULLET, bulletCmp);

		tagCompound.setString(TAG_CASTER, (String) dataWatcher.get(CASTER_NAME));
		tagCompound.setInteger(TAG_TIME_ALIVE, getTimeAlive());
		tagCompound.setInteger(TAG_TIMES_CAST, (int) dataWatcher.get(TIMES_CAST));
		
		tagCompound.setFloat(TAG_LOOK_X, (float) dataWatcher.get(LOOK_X));
		tagCompound.setFloat(TAG_LOOK_Y, (float) dataWatcher.get(LOOK_Y));
		tagCompound.setFloat(TAG_LOOK_Z, (float) dataWatcher.get(LOOK_Z));
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tagCompound) {
		NBTTagCompound colorizerCmp = tagCompound.getCompoundTag(TAG_COLORIZER);
		ItemStack colorizer = ItemStack.loadItemStackFromNBT(colorizerCmp);
		dataWatcher.set(COLORIZER_DATA, Optional.fromNullable(colorizer));

		NBTTagCompound bulletCmp = tagCompound.getCompoundTag(TAG_BULLET);
		ItemStack bullet = ItemStack.loadItemStackFromNBT(bulletCmp);
		dataWatcher.set(BULLET_DATA, Optional.of(bullet));

		dataWatcher.set(CASTER_NAME, tagCompound.getString(TAG_CASTER));
		setTimeAlive(tagCompound.getInteger(TAG_TIME_ALIVE));
		dataWatcher.set(TIMES_CAST, tagCompound.getInteger(TAG_TIMES_CAST));
		
		dataWatcher.set(LOOK_X, tagCompound.getFloat(TAG_LOOK_X));
		dataWatcher.set(LOOK_Y, tagCompound.getFloat(TAG_LOOK_Y));
		dataWatcher.set(LOOK_Z, tagCompound.getFloat(TAG_LOOK_Z));
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		int timeAlive = getTimeAlive();
		if(timeAlive > LIVE_TIME)
			setDead();

		setTimeAlive(timeAlive + 1);
		int times = (int) dataWatcher.get(TIMES_CAST);

		if(timeAlive > CAST_DELAY && timeAlive % CAST_DELAY == 0 && times < 20) {
			SpellContext context = null;
			Entity thrower = getCaster();
			if(thrower != null && thrower instanceof EntityPlayer) {
				ItemStack spellContainer = ((Optional<ItemStack>)dataWatcher.get(BULLET_DATA)).orNull();
				if(spellContainer != null && spellContainer.getItem() instanceof ISpellContainer) {
					dataWatcher.set(TIMES_CAST, times + 1);
					Spell spell = ((ISpellContainer) spellContainer.getItem()).getSpell(spellContainer);
					if(spell != null)
						context = new SpellContext().setPlayer((EntityPlayer) thrower).setFocalPoint(this).setSpell(spell).setLoopcastIndex(times);
				}
			}

			if(context != null)
				context.cspell.safeExecute(context);
		}

		int colorVal = ICADColorizer.DEFAULT_SPELL_COLOR;
		ItemStack colorizer = ((Optional<ItemStack>) dataWatcher.get(COLORIZER_DATA)).orNull();
		if(colorizer != null && colorizer.getItem() instanceof ICADColorizer)
			colorVal = Psi.proxy.getColorizerColor(colorizer).getRGB();

		Color color = new Color(colorVal);
		float r = color.getRed() / 255F;
		float g = color.getGreen() / 255F;
		float b = color.getBlue() / 255F;
		for(int i = 0; i < 5; i++) {
			double x = posX + (Math.random() - 0.5) * width;
			double y = posY - getYOffset();
			double z = posZ + (Math.random() - 0.5) * width;
			float grav = -0.15F - (float) Math.random() * 0.03F;
			Psi.proxy.sparkleFX(worldObj, x, y, z, r, g, b, grav, 0.25F, 15);
		}
	}
	
	@Override
	public Vec3d getLook(float f) {
		float x = (float) dataWatcher.get(LOOK_X);
		float y = (float) dataWatcher.get(LOOK_Y);
		float z = (float) dataWatcher.get(LOOK_Z);
		return new Vec3d(x, y, z);
	}

	public int getTimeAlive() {
		return (int) dataWatcher.get(TIME_ALIVE);
	}

	public void setTimeAlive(int i) {
		dataWatcher.set(TIME_ALIVE, i);
	}

	public EntityLivingBase getCaster() {
		String name = (String) dataWatcher.get(CASTER_NAME);
		EntityPlayer player = worldObj.getPlayerEntityByName(name);
		return player;
	}
}
