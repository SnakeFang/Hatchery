package com.gendeathrow.hatchery.block.ironnestpen;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import com.gendeathrow.hatchery.Hatchery;
import com.gendeathrow.hatchery.core.ModItems;
import com.gendeathrow.hatchery.core.Settings;
import com.gendeathrow.hatchery.network.HatcheryPacket;
import com.gendeathrow.hatchery.util.ItemStackEntityNBTHelper;

public class IronNestPenTileEntity extends TileEntity  implements ITickable, IInventory
{
	
	private EntityChicken chickenStored;
	private NBTTagCompound entityNBT;
	
	private int TimetoNextEgg = 0;
	
	private Random rand = new Random();
	
	ItemStack[] inventory = new ItemStack[5];
	
	
	public IronNestPenTileEntity()
	{
		super();
		entityNBT = new NBTTagCompound();
		this.TimetoNextEgg = this.rand.nextInt(6000) + 6000;
	}
	
	public int getTimeToNextDrop()
	{
		return this.TimetoNextEgg;
	}
	
	public Entity storedEntity()
	{
		return this.chickenStored;
	}
	
	public boolean trySetEntity(Entity entityin)
	{
		if(this.storedEntity() != null) return false;
		
		if(entityin instanceof EntityChicken)
		{
			this.chickenStored = (EntityChicken) entityin;
			
			entityNBT = new NBTTagCompound();
			((EntityChicken) entityin).writeEntityToNBT(entityNBT);
			
			entityin.setPosition(this.pos.getX(),this.pos.getY() , this.pos.getZ());
			entityin.motionY = 0;
			
			IronNestPenBlock.setState(true, this.worldObj, this.pos);
			return true;
		}else return false;
		
	}
	
	public Entity tryGetRemoveEntity()
	{
		if(this.storedEntity() == null) return null;
		
		Entity respondEntity = this.storedEntity();
		entityNBT = new NBTTagCompound();	
		this.chickenStored = null;
		IronNestPenBlock.setState(false, this.worldObj, this.pos);
		return respondEntity;
	}
	
	private void createEntity()
	{
		try
  		{
			chickenStored = (EntityChicken) EntityList.createEntityFromNBT(this.entityNBT , this.getWorld());
  		}
  		catch (Throwable e)
  		{
  			chickenStored = null;
  			this.entityNBT = new NBTTagCompound();
  			Hatchery.logger.error("Error trying to add chicken tp pen 'Null NBT' " + e);
  		}

	}

	boolean firstload = true;
	
	private int idleTime = 0;
    private double lookX;
    private double lookZ;
    
	@Override
	public void update() 
	{
		
		if(!this.entityNBT.hasNoTags() && chickenStored == null)
		{
			this.createEntity();
		}

		if(this.worldObj.isRemote) 
		{
			updateClient();
			
			
			
			if(chickenStored == null) return;
			
			this.chickenStored.onLivingUpdate();
			
			if(this.chickenStored.getRNG().nextFloat() < 0.02F)
			{
				this.chickenStored.onGround = true;
			}
			else if(this.chickenStored.getRNG().nextFloat() < 0.02F)
			{
				this.chickenStored.onGround = false;
			}

			return;
		}
		if(chickenStored != null)
		{
			this.chickenStored.captureDrops = true;
			
//			if(this.chickenStored.timeUntilNextEgg > 1000) this.chickenStored.timeUntilNextEgg = 1000;
//			if(this.TimetoNextEgg > 1200)this.TimetoNextEgg  = 1200;
 			
	        if (chickenStored.isEntityAlive() && this.rand.nextInt(1000) < chickenStored.livingSoundTime++)
	        {
	        	chickenStored.livingSoundTime = -chickenStored.getTalkInterval();
	        	
                if (!chickenStored.isSilent())
                {
                    this.worldObj.playSound((EntityPlayer)null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), SoundEvents.ENTITY_CHICKEN_AMBIENT, chickenStored.getSoundCategory(), 1, 1);
                }
	        }
	        
	        int i = chickenStored.getGrowingAge();

            if (i < 0)
            {
                ++i;
                chickenStored.setGrowingAge(i);
            }
            else if (i > 0)
            {
                --i;
                chickenStored.setGrowingAge(i);
            }
	        
	        if (!this.worldObj.isRemote && !chickenStored.isChild() && !chickenStored.isChickenJockey() && --TimetoNextEgg <= 0)
	        {
	            if(rand.nextInt(99)+1 < Settings.eggNestDropRate)
	            {
	            	ItemStack hatcheryegg = new ItemStack(ModItems.hatcheryEgg, 1, 0);
	            	
	            	NBTTagCompound babyTag = storedEntity().writeToNBT(new NBTTagCompound());
	            	babyTag.setString("id", EntityList.getEntityString(this.storedEntity()));
	            	
	            	EntityAgeable baby = (EntityAgeable) EntityList.createEntityFromNBT(babyTag, this.worldObj);
	            	baby.setGrowingAge(-24000);
	            	
	            	hatcheryegg.setStackDisplayName(baby.getDisplayName().getFormattedText() +" Egg");
	            	
	            	ItemStackEntityNBTHelper.addEntitytoItemStack(hatcheryegg, (EntityLiving)baby);
	            	
	            	putStackInInventoryAllSlots(this, hatcheryegg, EnumFacing.DOWN);
	            	

	            }
	            
	            putStackInInventoryAllSlots(this, new ItemStack(Items.FEATHER, 1), EnumFacing.DOWN);
	            putStackInInventoryAllSlots(this, new ItemStack(ModItems.manure, rand.nextInt(2)+1), EnumFacing.DOWN);
	        	this.TimetoNextEgg = this.rand.nextInt(6000) + 6000;
	        }
	        
	        
	        
	        
            
        	if(this.chickenStored.capturedDrops != null && this.chickenStored.capturedDrops.size() > 0)
        	{
        		System.out.println("Items Captured");
        		
        		for(EntityItem entity : this.chickenStored.capturedDrops)
        		{
        			putStackInInventoryAllSlots(this, entity.getEntityItem(), EnumFacing.DOWN);
        		}
        		
        		this.chickenStored.capturedDrops.clear();
        		
        	}
		}
//		else if(NestPenBlock.hasChicken(this.worldObj.getBlockState(pos)))
//		{
//			NestPenBlock.setState(false, this.worldObj, pos);
//		}
	}
	
	private boolean sentRequest = false;
	public void updateClient()
	{
		if(this.chickenStored != null) return;
		
		if(!IronNestPenBlock.hasChicken(this.worldObj.getBlockState(pos))) return;
	   
		if(!sentRequest)
		{
			Hatchery.network.sendToServer(HatcheryPacket.requestItemstackTE(this.getPos()));
    			
			//System.out.println("Requesting Packet");
			sentRequest = true;
		}
		else if ( Minecraft.getSystemTime() % 300 == 0 && sentRequest)
		{
			sentRequest = false;
			//System.out.println("Resending Packet");
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		
		if(compound.hasKey("storedEntity"))
		{
			//System.out.println("String: "+ compound.getTag("storedEntity").toString());
			 
			this.entityNBT = (NBTTagCompound) compound.getTag("storedEntity");
		}
		//else System.out.println("null");
		
		
        NBTTagList nbttaglist = compound.getTagList("Items", 5);

        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound.getByte("Slot") & 255;

            if (j >= 0 && j < this.inventory.length)
            {
                this.inventory[j] = ItemStack.loadItemStackFromNBT(nbttagcompound);
            }
        }
		
		
		super.readFromNBT(compound);
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound)
	{

		NBTTagCompound storedEntity = new NBTTagCompound();
		
		if(this.chickenStored != null)
		{
			storedEntity.setString("id", EntityList.getEntityString(chickenStored));
			chickenStored.writeEntityToNBT(storedEntity);

		}
		compound.setTag("storedEntity", storedEntity);
		
		//System.out.println("String: "+ storedEntity);
		
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.inventory.length; ++i)
        {
            if (this.inventory[i] != null)
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Slot", (byte)i);
                this.inventory[i].writeToNBT(nbttagcompound);
                nbttaglist.appendTag(nbttagcompound);
            }
        }
        
        compound.setTag("Items", nbttaglist);
		return super.writeToNBT(compound);
	}
	
    public static void func_189677_a(DataFixer p_189677_0_)
    {
       // p_189677_0_.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists("NestingBox", new String[] {"Items"}));
    }
    
	//////////////////////////////////////////////////////////////////
	// Inventory
	/////////////////////////////////////////////////////////////////
	public void dropContents()
	{
		
        for (int i = 0; i < this.inventory.length; ++i)
        {
        	ItemStack stack = ItemStackHelper.getAndRemove(this.inventory, i);
        	
        	if(stack != null)
        	{
        		this.worldObj.spawnEntityInWorld(new EntityItem(worldObj, this.pos.getX(), this.pos.getY()+1, this.pos.getZ(), stack));
        	}
        }
	}
    
    @Override
	public String getName() 
	{
		return null;
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int getSizeInventory() 
	{
		return 5;
	}

	@Override
	public ItemStack getStackInSlot(int index) 
	{
		return this.inventory[index];
	}

	@Override
	public ItemStack decrStackSize(int index, int count) 
	{
        ItemStack itemstack = ItemStackHelper.getAndSplit(this.inventory, index, count);

        if (itemstack != null)
        {
            this.markDirty();
        }

        return itemstack;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) 
	{
		return ItemStackHelper.getAndRemove(this.inventory, index);
	}

	@Override
	public void setInventorySlotContents(int index,@Nullable ItemStack stack) 
	{
		this.inventory[index] = stack;
		
		if (stack != null && stack.stackSize > this.getInventoryStackLimit())
        {
            stack.stackSize = this.getInventoryStackLimit();
        }
        
		this.markDirty();
	}

	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) 
	{
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player) { }

	@Override
	public void closeInventory(EntityPlayer player) { }

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) 
	{
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {return 0;}

	@Override
	public void clear() 
	{
	
	        for (int i = 0; i < this.inventory.length; ++i)
	        {
	            this.inventory[i] = null;
	        }
	}

	
	
    public static IItemHandler getItemHandler(TileEntity tile, EnumFacing side) 
    {
        if (tile == null) 
        {
            return null;
        }

        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);

        if (handler == null) 
        {
            if (side != null && tile instanceof ISidedInventory) 
            {
                handler = new SidedInvWrapper((ISidedInventory) tile, side);
            } else if (tile instanceof IInventory) 
            {
                handler = new InvWrapper((IInventory) tile);
            }
        }

        return handler;
    }
    
    
    public static ItemStack putStackInInventoryAllSlots(IInventory inventoryIn, ItemStack stack, @Nullable EnumFacing side)
    {
        if (inventoryIn instanceof ISidedInventory && side != null)
        {
            ISidedInventory isidedinventory = (ISidedInventory)inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int k = 0; k < aint.length && stack != null && stack.stackSize > 0; ++k)
            {
                stack = insertStack(inventoryIn, stack, aint[k], side);
            }
        }
        else
        {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i && stack != null && stack.stackSize > 0; ++j)
            {
                stack = insertStack(inventoryIn, stack, j, side);
            }
        }

        if (stack != null && stack.stackSize == 0)
        {
            stack = null;
        }

        return stack;
    }
    
    /**
     * Insert the specified stack to the specified inventory and return any leftover items
     */
    private static ItemStack insertStack(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side)
    {
        ItemStack itemstack = inventoryIn.getStackInSlot(index);

        if (canInsertItemInSlot(inventoryIn, stack, index, side))
        {
            boolean flag = false;

            if (itemstack == null)
            {
                //Forge: BUGFIX: Again, make things respect max stack sizes.
                int max = Math.min(stack.getMaxStackSize(), inventoryIn.getInventoryStackLimit());
                if (max >= stack.stackSize)
                {
                inventoryIn.setInventorySlotContents(index, stack);
                stack = null;
                }
                else
                {
                    inventoryIn.setInventorySlotContents(index, stack.splitStack(max));
                }
                flag = true;
            }
            else if (canCombine(itemstack, stack))
            {
                //Forge: BUGFIX: Again, make things respect max stack sizes.
                int max = Math.min(stack.getMaxStackSize(), inventoryIn.getInventoryStackLimit());
                if (max > itemstack.stackSize)
                {
                int i = max - itemstack.stackSize;
                int j = Math.min(stack.stackSize, i);
                stack.stackSize -= j;
                itemstack.stackSize += j;
                flag = j > 0;
                }
            }
        }

        return stack;
    }
    
    /**
     * Can this hopper insert the specified item from the specified slot on the specified side?
     */
    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side)
    {
        return !inventoryIn.isItemValidForSlot(index, stack) ? false : !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory)inventoryIn).canInsertItem(index, stack, side);
    }
    
    private static boolean canCombine(ItemStack stack1, ItemStack stack2)
    {
        return stack1.getItem() != stack2.getItem() ? false : (stack1.getMetadata() != stack2.getMetadata() ? false : (stack1.stackSize > stack1.getMaxStackSize() ? false : ItemStack.areItemStackTagsEqual(stack1, stack2)));
    }
    
    
	@Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
		//System.out.println("sending packet");
		NBTTagCompound sendnbt = new NBTTagCompound();  	
		sendnbt = this.writeToNBT(sendnbt);
       return new SPacketUpdateTileEntity(this.getPos(), this.getBlockMetadata(), sendnbt);
    }

	
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt)
    {
		//System.out.println("Reading packet");
  		this.readFromNBT(pkt.getNbtCompound());
    	this.markDirty();
    }
    
    private void playAmbientSound()
    {
    	
    	this.rand.nextFloat();
    	
    }
    
    /**
     * Used for sending Waila Infomation
     * @param te
     * @return
     */
    public static NBTTagList getInventoryContents(IronNestPenTileEntity te)
    {
    	NBTTagList nbttaglist = new NBTTagList();
    	
        for (int i = 0; i < te.inventory.length; ++i)
        {
            if (te.inventory[i] != null)
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Slot", (byte)i);
                nbttagcompound.setString("id", te.inventory[i].getDisplayName());
                nbttagcompound.setInteger("cnt", te.inventory[i].stackSize);
                nbttaglist.appendTag(nbttagcompound);
            }
        }
    	return nbttaglist;
    }


}
