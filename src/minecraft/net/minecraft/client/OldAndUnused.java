//package net.minecraft.client;
//
//import net.minecraft.client.controller.PlayerControllerCreative;
//import net.minecraft.client.player.EntityPlayerSP;
//import net.minecraft.game.entity.Entity;
//import net.minecraft.game.entity.EntityLiving;
//import net.minecraft.game.entity.player.InventoryPlayer;
//import net.minecraft.game.item.Item;
//import net.minecraft.game.item.ItemStack;
//import net.minecraft.game.level.World;
//import net.minecraft.game.level.block.Block;
//
//public class OldAndUnused {
//	// mouse click left/right mp    
//	private void clickMouseLeftMP() {
//	    if (this.leftClickCounter <= 0) {
//	        this.entityRenderer.itemRenderer.equippedItemRender();
//
//	        ItemStack itemStack = this.thePlayer.inventory.getCurrentItem();
//	        int initialStackSize = itemStack != null ? itemStack.stackSize : 0;
//
//	        if (this.objectMouseOver != null) {
//	            if (this.objectMouseOver.typeOfHit == 1) { // Entity hit
//	                Entity entityHit = this.objectMouseOver.entityHit;
//	                int damage = itemStack != null ? Item.itemsList[itemStack.itemID].getDamageVsEntity() : 1;
//	                entityHit.attackEntityFrom(this.thePlayer, damage);
//
//	                if (itemStack != null && entityHit instanceof EntityLiving) {
//	                    itemStack.getItem().hitEntity(itemStack);
//	                    if (itemStack.stackSize <= 0) {
//	                        this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
//	                    }
//	                }
//	            } else if (this.objectMouseOver.typeOfHit == 0) { // Block hit
//	                int x = this.objectMouseOver.blockX;
//	                int y = this.objectMouseOver.blockY;
//	                int z = this.objectMouseOver.blockZ;
//	                int sideHit = this.objectMouseOver.sideHit;
//	                Block block = Block.blocksList[this.theWorld.getBlockId(x, y, z)];
//
//	                this.theWorld.extinguishFire(x, y, z, sideHit);
//
//	                // Allow block breaking even without an item in hand
//	                if (block != Block.bedrock || this.thePlayer.userType >= 100) {
//	                    this.playerController.clickBlock(x, y, z);
//	                }
//	            }
//	        }
//	    }
//	}
//
//	// Modified block place client with sendTileUpdated
//
//	private void clickMouseRightMP() {
//	    if (this.leftClickCounter <= 0) {
//	        ItemStack itemStack = this.thePlayer.inventory.getCurrentItem();
//	        int initialStackSize = itemStack != null ? itemStack.stackSize : 0;
//
//	        if (itemStack != null) {
//	            ItemStack updatedStack = itemStack.getItem().onItemRightClick(itemStack, this.theWorld, this.thePlayer);
//	            if (updatedStack != itemStack || (updatedStack != null && updatedStack.stackSize != initialStackSize)) {
//	                this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = updatedStack;
//	                this.entityRenderer.itemRenderer.resetEquippedProgress();
//	                if (updatedStack != null && updatedStack.stackSize == 0) {
//	                    this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
//	                }
//	            }
//	        }
//
//	        if (this.objectMouseOver == null) {
//	            if (!(this.playerController instanceof PlayerControllerCreative)) {
//	                this.leftClickCounter = 10;
//	            }
//	        } else if (this.objectMouseOver.typeOfHit == 0) { // Block interaction
//	            int x = this.objectMouseOver.blockX;
//	            int y = this.objectMouseOver.blockY;
//	            int z = this.objectMouseOver.blockZ;
//	            int sideHit = this.objectMouseOver.sideHit;
//	            Block block = Block.blocksList[this.theWorld.getBlockId(x, y, z)];
//
//	            if (block != null && itemStack != null) {
//	                int blockId = this.theWorld.getBlockId(x, y, z);
//	                if (Block.blocksList[blockId].blockActivated(this.theWorld, x, y, z, this.thePlayer)) {
//	                    return;
//	                }
//
//	                int stackSizeBeforeUse = itemStack.stackSize;
//	                if (itemStack.getItem().onItemUse(itemStack, this.theWorld, x, y, z, sideHit)) {
//	                    this.entityRenderer.itemRenderer.equippedItemRender();
//
//	                    // Define the actionId as '1' for the right-click action
//	                    int actionId = 1; // Set appropriate actionId for right-click or other actions
//	                    int itemId = itemStack.itemID;
//
//	                    if (this.isOnlineClient()) {
//	                        // Send the block interaction update to the server in the specified format
//	                        this.networkClient.sendTileUpdated(x, y, z, actionId, itemId);
//	                    }
//
//	                    // Update the level locally (if necessary)
//	                    this.theWorld.netSetTile(x, y, z, itemId);
//	                }
//
//	                if (itemStack.stackSize == 0) {
//	                    this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
//	                } else if (itemStack.stackSize != stackSizeBeforeUse) {
//	                    this.entityRenderer.itemRenderer.equipAnimationSpeed();
//	                }
//	            }
//	        }
//	    }
//	}
//	
//	// mouse click left/right sp
//	
//	private void clickMouseLeftSP() {
//	    if (this.leftClickCounter <= 0) {
//	        this.entityRenderer.itemRenderer.equippedItemRender();
//	        if (this.objectMouseOver == null) {
//	            if (!(this.playerController instanceof PlayerControllerCreative)) {
//	                this.leftClickCounter = 10;
//	            }
//	        } else if (this.objectMouseOver.typeOfHit == 1) { // Hit entity
//	            Entity entityHit = this.objectMouseOver.entityHit;
//	            EntityPlayerSP entityPlayerSP = this.thePlayer;
//	            InventoryPlayer inventory = this.thePlayer.inventory;
//	            ItemStack currentItem = inventory.getStackInSlot(inventory.currentItem);
//	            int damage = (currentItem != null) ? Item.itemsList[currentItem.itemID].getDamageVsEntity() : 1;
//
//	            if (damage > 0) {
//	                entityHit.attackEntityFrom(entityPlayerSP, damage);
//	                ItemStack itemStack = entityPlayerSP.inventory.getCurrentItem();
//	                if (itemStack != null && entityHit instanceof EntityLiving) {
//	                    EntityLiving entityLiving = (EntityLiving) entityHit;
//	                    Item.itemsList[itemStack.itemID].hitEntity(itemStack);
//	                    if (itemStack.stackSize <= 0) {
//	                        entityPlayerSP.destroyCurrentEquippedItem();
//	                    }
//	                }
//	            }
//	        } else if (this.objectMouseOver.typeOfHit == 0) { // Hit block
//	            int blockX = this.objectMouseOver.blockX;
//	            int blockY = this.objectMouseOver.blockY;
//	            int blockZ = this.objectMouseOver.blockZ;
//	            Block block = Block.blocksList[this.theWorld.getBlockId(blockX, blockY, blockZ)];
//	            this.theWorld.extinguishFire(blockX, blockY, blockZ, this.objectMouseOver.sideHit);
//	            if (block != Block.bedrock) {
//	                this.playerController.clickBlock(blockX, blockY, blockZ);
//	            }
//	        }
//	    }
//	}
//
//	private void clickMouseRightSP() {
//	    ItemStack itemStack = this.thePlayer.inventory.getCurrentItem();
//	    if (itemStack != null) {
//	        int initialStackSize = itemStack.stackSize;
//	        EntityPlayerSP entityPlayerSP = this.thePlayer;
//	        World world = this.theWorld;
//	        ItemStack updatedStack = itemStack.getItem().onItemRightClick(itemStack, world, entityPlayerSP);
//
//	        if (updatedStack != itemStack || (updatedStack != null && updatedStack.stackSize != initialStackSize)) {
//	            this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = updatedStack;
//	            this.entityRenderer.itemRenderer.resetEquippedProgress();
//	            if (updatedStack.stackSize == 0) {
//	                this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
//	            }
//	        }
//
//	        if (this.objectMouseOver != null && this.objectMouseOver.typeOfHit == 0) { // Hit block
//	            int blockX = this.objectMouseOver.blockX;
//	            int blockY = this.objectMouseOver.blockY;
//	            int blockZ = this.objectMouseOver.blockZ;
//	            int sideHit = this.objectMouseOver.sideHit;
//	            int blockId = this.theWorld.getBlockId(blockX, blockY, blockZ);
//
//	            if (blockId > 0 && Block.blocksList[blockId].blockActivated(this.theWorld, blockX, blockY, blockZ, this.thePlayer)) {
//	                return;
//	            }
//
//	            if (itemStack != null) {
//	                int initialStackSizeRight = itemStack.stackSize;
//	                if (itemStack.getItem().onItemUse(itemStack, world, blockX, blockY, blockZ, sideHit)) {
//	                    this.entityRenderer.itemRenderer.equippedItemRender();
//	                }
//	                if (itemStack.stackSize == 0) {
//	                    this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
//	                } else if (itemStack.stackSize != initialStackSizeRight) {
//	                    this.entityRenderer.itemRenderer.equipAnimationSpeed();
//	                }
//	            }
//	        }
//	    }
//	}
//	
//	// clickmouse switch ig idk
//	
//	private void clickMouse(int clickState) {
//	    if (clickState != 0 || this.leftClickCounter <= 0) {
//	        if (clickState == 0) {
//	            if (this.isOnlineClient()) {
//	                clickMouseLeftMP();
//	            } else {
//	                clickMouseLeftSP();
//	            }
//	        } else if (clickState == 1) {
//	            if (this.isOnlineClient()) {
//	                clickMouseRightMP();
//	            } else {
//	                clickMouseRightSP();
//	            }
//	        }
//	    }
//	}
//}
