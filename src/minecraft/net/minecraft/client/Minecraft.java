package net.minecraft.client;

import java.awt.Canvas;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import javax.swing.JOptionPane;

import net.minecraft.client.controller.PlayerController;
import net.minecraft.client.controller.PlayerControllerCreative;
import net.minecraft.client.controller.PlayerControllerSP;
import net.minecraft.client.effect.EffectRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.container.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.net.Client;
import net.minecraft.client.net.NetworkPlayer;
import net.minecraft.client.net.Packet;
import net.minecraft.client.net.SocketConnection;
import net.minecraft.client.net.Textures;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.player.MovementInputFromOptions;
import net.minecraft.client.render.EntityRenderer;
import net.minecraft.client.render.ItemRenderer;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.texture.TextureFlamesFX;
import net.minecraft.client.render.texture.TextureGearsFX;
import net.minecraft.client.render.texture.TextureLavaFX;
import net.minecraft.client.render.texture.TextureWaterFX;
import net.minecraft.client.render.texture.TextureWaterFlowFX;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.level.LevelLoader;
import net.minecraft.game.level.World;
import net.minecraft.game.level.block.Block;
import net.minecraft.game.level.generator.LevelGenerator;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public final class Minecraft implements Runnable {
	private boolean fullscreen = false;
	public int displayWidth;
	public int displayHeight;
	private OpenGlCapsChecker glCapabilities;
	private Timer timer = new Timer(20.0F);
	public World theWorld;
	public RenderGlobal renderGlobal;
	public EntityPlayerSP thePlayer;
	public EffectRenderer effectRenderer;
	public Session session = null;
	public String minecraftUri;
	public Canvas mcCanvas;
	public boolean appletMode = true;
	public volatile boolean isGamePaused = false;
	public RenderEngine renderEngine;
	public FontRenderer fontRenderer;
	public GuiScreen currentScreen = null;
	public LoadingScreenRenderer loadingScreen = new LoadingScreenRenderer(this);
	public EntityRenderer entityRenderer = new EntityRenderer(this);
	private ThreadDownloadResources downloadResourcesThread;
	private int ticksRan = 0;
	private int leftClickCounter = 0;
	private int tempDisplayWidth;
	private int tempDisplayHeight;
	public String loadMapUser = null;
	public int loadMapID = 0;
	public GuiIngame ingameGUI;
	public boolean skipRenderWorld = false;
	public MovingObjectPosition objectMouseOver;
	public GameSettings options;
	private MinecraftApplet mcApplet;
	public SoundManager sndManager;
	public MouseHelper mouseHelper;
	public File mcDataDir;
	private TextureWaterFX textureWaterFX;
	private TextureLavaFX textureLavaFX;
	volatile boolean running;
	public String debug;
	public boolean inventoryScreen;
	private int prevFrameTime;
	public boolean inGameHasFocus;
	public Client networkClient;
	String server;
	int port;
	public PlayerController playerController;
	
	public Minecraft(Canvas canvas, MinecraftApplet minecraftApplet, int tempDisplayWidth, int tempDisplayHeight, boolean fullscreen) {
		new ModelBiped(0.0F);
		this.objectMouseOver = null;
		this.sndManager = new SoundManager();
		this.server = "localhost";
		this.port = 25565;
		this.textureWaterFX = new TextureWaterFX();
		this.textureLavaFX = new TextureLavaFX();
		this.running = false;
		this.debug = "";
		this.inventoryScreen = false;
		this.prevFrameTime = 0;
		this.inGameHasFocus = false;
		this.tempDisplayWidth = tempDisplayWidth;
		this.tempDisplayHeight = tempDisplayHeight;
		this.fullscreen = fullscreen;
		this.mcApplet = minecraftApplet;
		new ThreadSleepForever(this, "Timer hack thread");
		this.mcCanvas = canvas;
		this.displayWidth = tempDisplayWidth;
		this.displayHeight = tempDisplayHeight;
		this.fullscreen = fullscreen;
	}

	public final void displayGuiScreen(GuiScreen guiScreen) {
		if(!(this.currentScreen instanceof GuiErrorScreen)) {
			if(this.currentScreen != null) {
				this.currentScreen.onGuiClosed();
			}

			if(guiScreen == null && this.theWorld == null && !this.isOnlineClient()) {
				guiScreen = new GuiMainMenu();
			} else if(guiScreen == null && this.thePlayer.health <= 0) {
				guiScreen = new GuiGameOver();
			}

			this.currentScreen = (GuiScreen)guiScreen;
			if(guiScreen != null) {
				this.inputLock();
				ScaledResolution scaledResolution2;
				int i3 = (scaledResolution2 = new ScaledResolution(this.displayWidth, this.displayHeight)).getScaledWidth();
				int i4 = scaledResolution2.getScaledHeight();
				((GuiScreen)guiScreen).setWorldAndResolution(this, i3, i4);
				this.skipRenderWorld = false;
			} else {
				this.setIngameFocus();
			}
		}
	}

	public final void shutdownMinecraftApplet() {
		try {
			if(this.downloadResourcesThread != null) {
				this.downloadResourcesThread.closeMinecraft();
			}
		} catch (Exception exception5) {
		}

		try {
			this.sndManager.closeMinecraft();
			Mouse.destroy();
			Keyboard.destroy();
		} finally {
			Display.destroy();
		}

	}

	public final void run() {
		this.running = true;

		try {
			Minecraft minecraft1 = this;
			if(this.mcCanvas != null) {
				Display.setParent(this.mcCanvas);
			} else if(this.fullscreen) {
				Display.setFullscreen(true);
				this.displayWidth = Display.getDisplayMode().getWidth();
				this.displayHeight = Display.getDisplayMode().getHeight();
			} else {
				Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));
			}

			Display.setTitle("Minecraft Minecraft Indev");

			try {
				Display.create();
				System.out.println("LWJGL version: " + Sys.getVersion());
				System.out.println("GL RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
				System.out.println("GL VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
				System.out.println("GL VERSION: " + GL11.glGetString(GL11.GL_VERSION));
				ContextCapabilities contextCapabilities2 = GLContext.getCapabilities();
				System.out.println("OpenGL 3.0: " + contextCapabilities2.OpenGL30);
				System.out.println("OpenGL 3.1: " + contextCapabilities2.OpenGL31);
				System.out.println("OpenGL 3.2: " + contextCapabilities2.OpenGL32);
				System.out.println("ARB_compatibility: " + contextCapabilities2.GL_ARB_compatibility);
				if(contextCapabilities2.OpenGL32) {
					IntBuffer intBuffer24 = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
					GL11.glGetInteger(37158, intBuffer24);
					int i25 = intBuffer24.get(0);
					System.out.println("PROFILE MASK: " + Integer.toBinaryString(i25));
					System.out.println("CORE PROFILE: " + ((i25 & 1) != 0));
					System.out.println("COMPATIBILITY PROFILE: " + ((i25 & 2) != 0));
				}
			} catch (LWJGLException lWJGLException17) {
				lWJGLException17.printStackTrace();

				try {
					Thread.sleep(1000L);
				} catch (InterruptedException interruptedException16) {
				}

				Display.create();
			}

			Keyboard.create();
			Mouse.create();
			this.mouseHelper = new MouseHelper(this.mcCanvas);

			try {
				Controllers.create();
			} catch (Exception exception15) {
				exception15.printStackTrace();
			}
			EntityRenderer gameRenderer54 = this.entityRenderer;

			EntityRenderer gameRenderer88 = gameRenderer54;
			float f60 = this.timer.renderPartialTicks;
			float f92 = f60;
			if(gameRenderer88.pointedEntity != null) {
				gameRenderer88.pointedEntity.renderHover(gameRenderer88.mc.renderEngine, f92);
			}

			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glClearDepth(1.0D);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			this.glCapabilities = new OpenGlCapsChecker();
			String string3 = "minecraft";
			String string26 = System.getProperty("user.home", ".");
			String string4;
			File file27;
			switch(EnumOSMappingHelper.osValues[((string4 = System.getProperty("os.name").toLowerCase()).contains("win") ? EnumOS.windows : (string4.contains("mac") ? EnumOS.macos : (string4.contains("solaris") ? EnumOS.solaris : (string4.contains("sunos") ? EnumOS.solaris : (string4.contains("linux") ? EnumOS.linux : (string4.contains("unix") ? EnumOS.linux : EnumOS.unknown)))))).ordinal()]) {
			case 1:
			case 2:
				file27 = new File(string26, '.' + string3 + '/');
				break;
			case 3:
				if((string4 = System.getenv("APPDATA")) != null) {
					file27 = new File(string4, "." + string3 + '/');
				} else {
					file27 = new File(string26, '.' + string3 + '/');
				}
				break;
			case 4:
				file27 = new File(string26, "Library/Application Support/" + string3);
				break;
			default:
				file27 = new File(string26, string3 + '/');
			}

			if(!file27.exists() && !file27.mkdirs()) {
				throw new RuntimeException("The working directory could not be created: " + file27);
			}

			this.mcDataDir = file27;
			this.options = new GameSettings(this, this.mcDataDir);
			this.sndManager.loadSoundSettings(this.options);
			this.renderEngine = new RenderEngine(this.options);
			this.renderEngine.registerTextureFX(this.textureLavaFX);
			this.renderEngine.registerTextureFX(this.textureWaterFX);
			this.renderEngine.registerTextureFX(new TextureWaterFlowFX());
			this.renderEngine.registerTextureFX(new TextureFlamesFX(0));
			this.renderEngine.registerTextureFX(new TextureFlamesFX(1));
			this.renderEngine.registerTextureFX(new TextureGearsFX(0));
			this.renderEngine.registerTextureFX(new TextureGearsFX(1));
			this.fontRenderer = new FontRenderer(this.options, "/default.png", this.renderEngine);
			BufferUtils.createIntBuffer(256).clear().limit(256);
			this.renderGlobal = new RenderGlobal(this, this.renderEngine);
			GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
			if(this.server != null && this.session != null) {
				this.networkClient = new Client(this, this.server, this.port, this.session.username, this.session.mpPass);
			} else if(this.theWorld == null) {
				this.displayGuiScreen(new GuiMainMenu());
			}

			this.effectRenderer = new EffectRenderer(this.theWorld, this.renderEngine);

			try {
				minecraft1.downloadResourcesThread = new ThreadDownloadResources(minecraft1.mcDataDir, minecraft1);
				minecraft1.downloadResourcesThread.start();
			} catch (Exception exception14) {
			}

			this.ingameGUI = new GuiIngame(this);
		} catch (Exception exception22) {
			exception22.printStackTrace();
			JOptionPane.showMessageDialog((Component)null, exception22.toString(), "Failed to start Minecraft", 0);
			return;
		}

		long j23 = System.currentTimeMillis();
		int i28 = 0;

		try {
			while(this.running) {
				if(this.theWorld != null) {
					this.theWorld.updateLighting();
				}

				if(this.mcCanvas == null && Display.isCloseRequested()) {
					this.running = false;
				}

				try {
					if(this.isGamePaused) {
						float f29 = this.timer.renderPartialTicks;
						this.timer.updateTimer();
						this.timer.renderPartialTicks = f29;
					} else {
						this.timer.updateTimer();
					}

					for(int i30 = 0; i30 < this.timer.elapsedTicks; ++i30) {
						++this.ticksRan;
						this.runTick();
					}

					this.sndManager.setListener(this.thePlayer, this.timer.renderPartialTicks);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					if (this.playerController != null) {
					    this.playerController.setPartialTime(this.timer.renderPartialTicks);
					}
					this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
					if(!Display.isActive()) {
						if(this.fullscreen) {
							this.toggleFullscreen();
						}

						Thread.sleep(10L);
					}

					if(this.mcCanvas != null && !this.fullscreen && (this.mcCanvas.getWidth() != this.displayWidth || this.mcCanvas.getHeight() != this.displayHeight)) {
						this.displayWidth = this.mcCanvas.getWidth();
						this.displayHeight = this.mcCanvas.getHeight();
						this.resize(this.displayWidth, this.displayHeight);
					}

					if(this.options.limitFramerate) {
						Thread.sleep(5L);
					}

					++i28;
					this.isGamePaused = this.currentScreen != null && this.currentScreen.doesGuiPauseGame();
				} catch (Exception exception18) {
					this.displayGuiScreen(new GuiErrorScreen("Client error", "The game broke! [" + exception18 + "]"));
					exception18.printStackTrace();
					return;
				}

				while(System.currentTimeMillis() >= j23 + 1000L) {
					this.debug = i28 + " fps, " + WorldRenderer.chunksUpdated + " chunk updates";
					WorldRenderer.chunksUpdated = 0;
					j23 += 1000L;
					i28 = 0;
				}
			}

			return;
		} catch (MinecraftError minecraftError19) {
			return;
		} catch (Exception exception20) {
			exception20.printStackTrace();
		} finally {
			this.shutdownMinecraftApplet();
		}

	}

	public final void setIngameFocus() {
		if(Display.isActive()) {
			if(!this.inventoryScreen) {
				this.inventoryScreen = true;
				this.mouseHelper.grabMouseCursor();
				this.displayGuiScreen((GuiScreen)null);
				this.prevFrameTime = this.ticksRan + 10000;
			}
		}
	}

	private void inputLock() {
		if(this.inventoryScreen) {
			if(this.thePlayer != null) {
				EntityPlayerSP entityPlayerSP1 = this.thePlayer;
				this.thePlayer.movementInput.resetKeyState();
			}

			this.inventoryScreen = false;

			try {
				Mouse.setNativeCursor((Cursor)null);
			} catch (LWJGLException lWJGLException2) {
				lWJGLException2.printStackTrace();
			}
		}
	}

	public final void displayInGameMenu() {
		if(this.currentScreen == null) {
			this.displayGuiScreen(new GuiIngameMenu());
		}
	}

	private void clickMouse(int clickState) {
	    if (clickState != 0 || this.leftClickCounter <= 0) {
	        if (clickState == 0) {
	            this.entityRenderer.itemRenderer.equippedItemRender();
	        }

	        ItemStack itemStack = this.thePlayer.inventory.getCurrentItem();
	        int initialStackSize = itemStack != null ? itemStack.stackSize : 0;  // Initialize initialStackSize here
	        
	        if (clickState == 1 && itemStack != null) {
	            ItemStack updatedStack = itemStack.getItem().onItemRightClick(itemStack, this.theWorld, this.thePlayer);
	            if (updatedStack != itemStack || (updatedStack != null && updatedStack.stackSize != initialStackSize)) {
	                this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = updatedStack;
	                this.entityRenderer.itemRenderer.resetEquippedProgress();
	                if (updatedStack != null && updatedStack.stackSize == 0) {
	                    this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
	                }
	            }
	        }

	        if (this.objectMouseOver != null) {
	            if (this.objectMouseOver.typeOfHit == 1 && clickState == 0) {
	                Entity entityHit = this.objectMouseOver.entityHit;
	                int damage = itemStack != null ? Item.itemsList[itemStack.itemID].getDamageVsEntity() : 1;
	                entityHit.attackEntityFrom(this.thePlayer, damage);
	                if (itemStack != null && entityHit instanceof EntityLiving) {
	                    itemStack.getItem().hitEntity(itemStack);
	                    if (itemStack.stackSize <= 0) {
	                        this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
	                    }
	                }
	            } else if (this.objectMouseOver.typeOfHit == 0) {
	                int x = this.objectMouseOver.blockX;
	                int y = this.objectMouseOver.blockY;
	                int z = this.objectMouseOver.blockZ;
	                int sideHit = this.objectMouseOver.sideHit;
	                Block block = Block.blocksList[this.theWorld.getBlockId(x, y, z)];

	                if (clickState == 0) {
	                    this.theWorld.extinguishFire(x, y, z, sideHit);
	                    if (block != Block.bedrock || this.thePlayer.userType >= 100) {
	                        this.playerController.clickBlock(x, y, z);
	                    }
	                } else if (itemStack != null && itemStack.stackSize > 0) {
	                    Block blockToPlace = Block.blocksList[itemStack.itemID];
	                    if (block == null || block.isCollidable()) {
	                        if (this.theWorld.checkIfAABBIsClear1(blockToPlace.getCollisionBoundingBoxFromPool(x, y, z))) {
	                            if (this.playerController.removeResource(this.thePlayer, itemStack)) {
	                                this.theWorld.netSetTile(x, y, z, itemStack.itemID);
	                                blockToPlace.onBlockAdded(this.theWorld, x, y, z);
	                                this.entityRenderer.itemRenderer.equippedItemRender();
	                            }
	                        }
	                    }

	                    int newBlockId = this.theWorld.getBlockId(x, y, z);
	                    if (newBlockId > 0 && Block.blocksList[newBlockId].blockActivated(this.theWorld, x, y, z, this.thePlayer)) {
	                        return;
	                    }

	                    if (itemStack.stackSize != initialStackSize) {
	                        this.entityRenderer.itemRenderer.equipAnimationSpeed();
	                    }
	                }
	            }
	        }
	    }
	}
	
	public final void toggleFullscreen() {
		try {
			this.fullscreen = !this.fullscreen;
			System.out.println("Toggle fullscreen!");
			if(this.fullscreen) {
				Display.setDisplayMode(Display.getDesktopDisplayMode());
				this.displayWidth = Display.getDisplayMode().getWidth();
				this.displayHeight = Display.getDisplayMode().getHeight();
			} else {
				if(this.mcCanvas != null) {
					this.displayWidth = this.mcCanvas.getWidth();
					this.displayHeight = this.mcCanvas.getHeight();
				} else {
					this.displayWidth = this.tempDisplayWidth;
					this.displayHeight = this.tempDisplayHeight;
				}

				Display.setDisplayMode(new DisplayMode(this.tempDisplayWidth, this.tempDisplayHeight));
			}

			this.inputLock();
			Display.setFullscreen(this.fullscreen);
			Display.update();
			Thread.sleep(1000L);
			if(this.fullscreen) {
				this.setIngameFocus();
			}

			if(this.currentScreen != null) {
				this.inputLock();
				this.resize(this.displayWidth, this.displayHeight);
			}

			System.out.println("Size: " + this.displayWidth + ", " + this.displayHeight);
		} catch (Exception exception2) {
			exception2.printStackTrace();
		}
	}

	private void resize(int scaledWidth, int scaledHeight) {
		this.displayWidth = scaledWidth;
		this.displayHeight = scaledHeight;
		if(this.currentScreen != null) {
			ScaledResolution scaledWidth1;
			scaledHeight = (scaledWidth1 = new ScaledResolution(scaledWidth, scaledHeight)).getScaledWidth();
			scaledWidth = scaledWidth1.getScaledHeight();
			this.currentScreen.setWorldAndResolution(this, scaledHeight, scaledWidth);
		}

	}

	private void runTick() {
        this.ingameGUI.updateChatMessages();
		if(!this.isGamePaused && this.theWorld != null) {
			this.playerController.onUpdate();
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.renderEngine.getTexture("/terrain.png"));
		if(!this.isGamePaused) {
			this.renderEngine.updateDynamicTextures();
		}

		if(this.currentScreen == null && this.thePlayer != null && this.thePlayer.health <= 0) {
			this.displayGuiScreen((GuiScreen)null);
		}
		
		
		int i41;
		int i8;
		int i38;
		int i46;
		int i47;

		if(this.networkClient != null && !(this.currentScreen instanceof GuiErrorScreen)) {
			if(!this.networkClient.isConnected()) {
				this.loadingScreen.displayProgressMessage("Connecting..");
				this.loadingScreen.setLoadingProgress(0);
			} else {
				Client client18 = this.networkClient;
				if(this.networkClient.processData) {
					SocketConnection socketConnection22 = client18.serverConnection;
					if(client18.serverConnection.connected) {
						try {
							SocketConnection socketConnection21 = client18.serverConnection;
							client18.serverConnection.socketChannel.read(socketConnection21.readBuffer);
							i41 = 0;

							while(socketConnection21.readBuffer.position() > 0 && i41++ != 100) {
								socketConnection21.readBuffer.flip();
								byte b5 = socketConnection21.readBuffer.get(0);
								Packet packet6;
								if((packet6 = Packet.PACKETS[b5]) == null) {
									throw new IOException("Bad command: " + b5);
								}

								if(socketConnection21.readBuffer.remaining() < packet6.size + 1) {
									socketConnection21.readBuffer.compact();
									break;
								}

								socketConnection21.readBuffer.get();
								Object[] object7 = new Object[packet6.fields.length];

								for(i8 = 0; i8 < object7.length; ++i8) {
									object7[i8] = socketConnection21.read(packet6.fields[i8]);
								}

								Client client43 = socketConnection21.client;
								if(socketConnection21.client.processData) {
									if(packet6 == Packet.LOGIN) {
										client43.minecraft.loadingScreen.displayProgressMessage(object7[1].toString());
										client43.minecraft.loadingScreen.displayLoadingString(object7[2].toString());
										//client43.minecraft.thePlayer.userType = ((Byte)object7[3]).byteValue();
									} else if(packet6 == Packet.LEVEL_INITIALIZE) {
										client43.minecraft.setLevel((World)null);
										client43.levelBuffer = new ByteArrayOutputStream();
									} else if(packet6 == Packet.LEVEL_DATA_CHUNK) {
										short s11 = ((Short)object7[0]).shortValue();
										byte[] b12 = (byte[])((byte[])object7[1]);
										byte b13 = ((Byte)object7[2]).byteValue();
										client43.minecraft.loadingScreen.setLoadingProgress(b13);
										client43.levelBuffer.write(b12, 0, s11);
									} else if(packet6 == Packet.LEVEL_FINALIZE) {
										try {
											client43.levelBuffer.close();
										} catch (IOException iOException14) {
											iOException14.printStackTrace();
										}

										byte[] b56 = LevelLoader.loadBlocks(new ByteArrayInputStream(client43.levelBuffer.toByteArray()));
										client43.levelBuffer = null;
										short s59 = ((Short)object7[0]).shortValue();
										short s62 = ((Short)object7[1]).shortValue();
										short s27 = ((Short)object7[2]).shortValue();
										World level30;
										(level30 = new World()).setNetworkMode(true);
										level30.generate(s59, s62, s27, b56, b56);
										client43.minecraft.setLevel(level30);
										client43.minecraft.skipRenderWorld = false;
										client43.connected = true;
									} else if(packet6 == Packet.SET_TILE) {
										if(client43.minecraft.theWorld != null) {
											client43.minecraft.theWorld.netSetTile(((Short)object7[0]).shortValue(), ((Short)object7[1]).shortValue(), ((Short)object7[2]).shortValue(), ((Byte)object7[3]).byteValue());
										}
									} else {
										byte b10;
										byte b10001;
										short s10003;
										short s10004;
										String string33;
										NetworkPlayer networkPlayer34;
										short s36;
										short s45;
										if(packet6 == Packet.PLAYER_JOIN) {
											b10001 = ((Byte)object7[0]).byteValue();
											String string10002 = (String)object7[1];
											s10003 = ((Short)object7[2]).shortValue();
											s10004 = ((Short)object7[3]).shortValue();
											short s10005 = ((Short)object7[4]).shortValue();
											byte b10006 = ((Byte)object7[5]).byteValue();
											byte b57 = ((Byte)object7[6]).byteValue();
											b10 = b10006;
											short s9 = s10005;
											s45 = s10004;
											s36 = s10003;
											string33 = string10002;
											b5 = b10001;
											if(b5 >= 0) {
												b10 = (byte)(b10 + 128);
												s45 = (short)(s45 - 22);
												networkPlayer34 = new NetworkPlayer(client43.minecraft, b5, string33, s36, s45, s9, (float)(b10 * 360) / 256.0F, (float)(b57 * 360) / 256.0F);
												client43.players.put(b5, networkPlayer34);
												client43.minecraft.theWorld.spawnEntityInWorld(networkPlayer34);
											} else {
												client43.minecraft.theWorld.setSpawnLocation(s36 / 32, s45 / 32, s9 / 32, (float)(b10 * 320 / 256));
												client43.minecraft.thePlayer.setPositionAndRotation((float)s36 / 32.0F, (float)s45 / 32.0F, (float)s9 / 32.0F, (float)(b10 * 360) / 256.0F, (float)(b57 * 360) / 256.0F);
											}
										} else {
											byte b48;
											NetworkPlayer networkPlayer58;
											byte b69;
											if(packet6 == Packet.PLAYER_TELEPORT) {
												b10001 = ((Byte)object7[0]).byteValue();
												short s65 = ((Short)object7[1]).shortValue();
												s10003 = ((Short)object7[2]).shortValue();
												s10004 = ((Short)object7[3]).shortValue();
												b69 = ((Byte)object7[4]).byteValue();
												b10 = ((Byte)object7[5]).byteValue();
												b48 = b69;
												s45 = s10004;
												s36 = s10003;
												short s35 = s65;
												b5 = b10001;
												if(b5 < 0) {
													client43.minecraft.thePlayer.setPositionAndRotation((float)s35 / 32.0F, (float)s36 / 32.0F, (float)s45 / 32.0F, (float)(b48 * 360) / 256.0F, (float)(b10 * 360) / 256.0F);
												} else {
													b48 = (byte)(b48 + 128);
													s36 = (short)(s36 - 22);
													if((networkPlayer58 = (NetworkPlayer)client43.players.get(b5)) != null) {
														networkPlayer58.teleport(s35, s36, s45, (float)(b48 * 360) / 256.0F, (float)(b10 * 360) / 256.0F);
													}
												}
											} else {
												byte b37;
												byte b40;
												byte b50;
												byte b66;
												byte b67;
												if(packet6 == Packet.PLAYER_MOVE_AND_ROTATE) {
													b10001 = ((Byte)object7[0]).byteValue();
													b66 = ((Byte)object7[1]).byteValue();
													b67 = ((Byte)object7[2]).byteValue();
													byte b68 = ((Byte)object7[3]).byteValue();
													b69 = ((Byte)object7[4]).byteValue();
													b10 = ((Byte)object7[5]).byteValue();
													b48 = b69;
													b50 = b68;
													b40 = b67;
													b37 = b66;
													b5 = b10001;
													if(b5 >= 0) {
														b48 = (byte)(b48 + 128);
														if((networkPlayer58 = (NetworkPlayer)client43.players.get(b5)) != null) {
															networkPlayer58.queue(b37, b40, b50, (float)(b48 * 360) / 256.0F, (float)(b10 * 360) / 256.0F);
														}
													}
												} else if(packet6 == Packet.PLAYER_ROTATE) {
													b10001 = ((Byte)object7[0]).byteValue();
													b66 = ((Byte)object7[1]).byteValue();
													b40 = ((Byte)object7[2]).byteValue();
													b37 = b66;
													b5 = b10001;
													if(b5 >= 0) {
														b37 = (byte)(b37 + 128);
														NetworkPlayer networkPlayer51;
														if((networkPlayer51 = (NetworkPlayer)client43.players.get(b5)) != null) {
															networkPlayer51.queue((float)(b37 * 360) / 256.0F, (float)(b40 * 360) / 256.0F);
														}
													}
												} else if(packet6 == Packet.PLAYER_MOVE) {
													b10001 = ((Byte)object7[0]).byteValue();
													b66 = ((Byte)object7[1]).byteValue();
													b67 = ((Byte)object7[2]).byteValue();
													b50 = ((Byte)object7[3]).byteValue();
													b40 = b67;
													b37 = b66;
													b5 = b10001;
													NetworkPlayer networkPlayer52;
													if(b5 >= 0 && (networkPlayer52 = (NetworkPlayer)client43.players.get(b5)) != null) {
														networkPlayer52.queue(b37, b40, b50);
													}
												} else if(packet6 == Packet.PLAYER_DISCONNECT) {
													b5 = ((Byte)object7[0]).byteValue();
													if(b5 >= 0 && (networkPlayer34 = (NetworkPlayer)client43.players.remove(b5)) != null) {
														networkPlayer34.clear();
														client43.minecraft.theWorld.removeEntityFromWorld(networkPlayer34);
													}
												} else if(packet6 == Packet.CHAT_MESSAGE) {
													b10001 = ((Byte)object7[0]).byteValue();
													string33 = (String)object7[1];
													b5 = b10001;
													if(b5 < 0) {
														client43.minecraft.ingameGUI.addChatMessage("&e" + string33);
													} else {
														client43.players.get(b5);
														client43.minecraft.ingameGUI.addChatMessage(string33);
													}
												} else if(packet6 == Packet.KICK_PLAYER) {
													client43.serverConnection.disconnect();
													client43.minecraft.displayGuiScreen(new GuiErrorScreen("Connection lost", (String)object7[0]));
												} else if(packet6 == Packet.USER_TYPE) {
													client43.minecraft.thePlayer.userType = ((Byte)object7[0]).byteValue();
												}
											}
										}
									}
								}

								if(!socketConnection21.connected) {
									break;
								}

								socketConnection21.readBuffer.compact();
							}

							if(socketConnection21.writeBuffer.position() > 0) {
								socketConnection21.writeBuffer.flip();
								socketConnection21.socketChannel.write(socketConnection21.writeBuffer);
								socketConnection21.writeBuffer.compact();
							}
						} catch (Exception exception15) {
							client18.minecraft.displayGuiScreen(new GuiErrorScreen("Disconnected!", "You\'ve lost connection to the server"));
							client18.minecraft.skipRenderWorld = false;
							exception15.printStackTrace();
							client18.serverConnection.disconnect();
							client18.minecraft.networkClient = null;
						}
					}
				}

				EntityPlayerSP player31 = this.thePlayer;
				client18 = this.networkClient;
				if(this.networkClient.connected) {
					int i23 = (int)(player31.posX * 32.0F);
					i41 = (int)(player31.posY * 32.0F);
					i38 = (int)(player31.posZ * 32.0F);
					i46 = (int)(player31.rotationYaw * 256.0F / 360.0F) & 255;
					i47 = (int)(player31.rotationPitch * 256.0F / 360.0F) & 255;
					client18.serverConnection.sendPacket(Packet.PLAYER_TELEPORT, new Object[]{-1, i23, i41, i38, i46, i47});
				}
			}
		}
		if(this.currentScreen == null || this.currentScreen.allowUserInput) {
			label286:
			while(true) {
				while(true) {
					int i1;
					int i2;
					while(Mouse.next()) {
						if((i1 = Mouse.getEventDWheel()) != 0) {
							i2 = i1;
							if (this.thePlayer != null && this.thePlayer.inventory != null) {
							    InventoryPlayer inventoryPlayer5 = this.thePlayer.inventory;
							if(i1 > 0) {
								i2 = 1;
							}

							if(i2 < 0) {
								i2 = -1;
							}

							for(inventoryPlayer5.currentItem -= i2; inventoryPlayer5.currentItem < 0; inventoryPlayer5.currentItem += 9) {
							}

							while(inventoryPlayer5.currentItem >= 9) {
								inventoryPlayer5.currentItem -= 9;
							}
						}
						}

						if(this.currentScreen == null) {
							if(!this.inventoryScreen && Mouse.getEventButtonState()) {
								this.setIngameFocus();
							} else {
								if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
									this.clickMouse(0);
									this.prevFrameTime = this.ticksRan;
								}

								if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
									this.clickMouse(1);
									this.prevFrameTime = this.ticksRan;
								}

								if(Mouse.getEventButton() == 2 && Mouse.getEventButtonState() && this.objectMouseOver != null) {
									if((i2 = this.theWorld.getBlockId(this.objectMouseOver.blockX, this.objectMouseOver.blockY, this.objectMouseOver.blockZ)) == Block.grass.blockID) {
										i2 = Block.dirt.blockID;
									}

									if(i2 == Block.stairDouble.blockID) {
										i2 = Block.stairSingle.blockID;
									}

									if(i2 == Block.bedrock.blockID) {
										i2 = Block.stone.blockID;
									}

									this.thePlayer.inventory.getFirstEmptyStack(i2);
								}
							}
						} else if(this.currentScreen != null) {
							this.currentScreen.handleMouseInput();
						}
					}

					if(this.leftClickCounter > 0) {
						--this.leftClickCounter;
					}

					while(true) {
						while(true) {
							do {
								boolean z3;
								if(!Keyboard.next()) {
									if(this.currentScreen == null) {
										if(Mouse.isButtonDown(0) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.inventoryScreen) {
											this.clickMouse(0);
											this.prevFrameTime = this.ticksRan;
										}

										if(Mouse.isButtonDown(1) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.inventoryScreen) {
											this.clickMouse(1);
											this.prevFrameTime = this.ticksRan;
										}
									}

									z3 = this.currentScreen == null && Mouse.isButtonDown(0) && this.inventoryScreen;
									boolean z8 = false;
									if (this.playerController != null && !this.playerController.isInTestMode && this.leftClickCounter <= 0) {
										if(z3 && this.objectMouseOver != null && this.objectMouseOver.typeOfHit == 0) {
											i2 = this.objectMouseOver.blockX;
											int i7 = this.objectMouseOver.blockY;
											int i4 = this.objectMouseOver.blockZ;
											this.playerController.sendBlockRemoving(i2, i7, i4, this.objectMouseOver.sideHit);
											this.effectRenderer.addBlockHitEffects(i2, i7, i4, this.objectMouseOver.sideHit);
										} else {
											this.playerController.resetBlockRemoving();
										}
									}
									break label286;
								}

								EntityPlayerSP entityPlayerSP10000 = this.thePlayer;
								int i10001 = Keyboard.getEventKey();
								z3 = Keyboard.getEventKeyState();
								i2 = i10001;
								if (entityPlayerSP10000 != null && entityPlayerSP10000.movementInput != null) {
								    entityPlayerSP10000.movementInput.checkKeyForMovementInput(i2, z3);
								}
							} while(!Keyboard.getEventKeyState());

							if(Keyboard.getEventKey() == Keyboard.KEY_F11) {
								this.toggleFullscreen();
							} else {
								if(this.currentScreen != null) {
									this.currentScreen.handleKeyboardInput();
								} else {
									if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
										this.displayInGameMenu();
									}

									if(Keyboard.getEventKey() == Keyboard.KEY_F7) {
										this.entityRenderer.grabLargeScreenshot();
									}

									if(this.playerController instanceof PlayerControllerCreative) {
										if(Keyboard.getEventKey() == this.options.keyBindLoad.keyCode) {
											this.thePlayer.preparePlayerToSpawn();
										}

										if(Keyboard.getEventKey() == this.options.keyBindSave.keyCode) {
											this.theWorld.setSpawnLocation((int)this.thePlayer.posX, (int)this.thePlayer.posY, (int)this.thePlayer.posZ, this.thePlayer.rotationYaw);
											this.thePlayer.preparePlayerToSpawn();
										}
									}

									if(Keyboard.getEventKey() == Keyboard.KEY_F5) {
										this.options.thirdPersonView = !this.options.thirdPersonView;
									}

									if(Keyboard.getEventKey() == this.options.keyBindInventory.keyCode) {
										this.displayGuiScreen(new GuiInventory(this.thePlayer.inventory));
									}

									if(Keyboard.getEventKey() == this.options.keyBindDrop.keyCode) {
										this.thePlayer.dropPlayerItemWithRandomChoice(this.thePlayer.inventory.decrStackSize(this.thePlayer.inventory.currentItem, 1), false);
									}
									
									if(Keyboard.getEventKey() == Keyboard.KEY_T && this.networkClient != null && this.networkClient.isConnected()) {
										this.displayGuiScreen(new GuiChat());
									}
								}

								for(i1 = 0; i1 < 9; ++i1) {
									if(Keyboard.getEventKey() == i1 + Keyboard.KEY_1) {
										this.thePlayer.inventory.currentItem = i1;
									}
								}

								if(Keyboard.getEventKey() == this.options.keyBindToggleFog.keyCode) {
									this.options.setOptionValue(4, !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 1 : -1);
								}
							}
						}
					}
				}
			}
		}

		if(this.currentScreen != null) {
			this.prevFrameTime = this.ticksRan + 10000;
		}

		if(this.currentScreen != null) {
			GuiScreen guiScreen6 = this.currentScreen;

			while(Mouse.next()) {
				guiScreen6.handleMouseInput();
			}

			while(Keyboard.next()) {
				guiScreen6.handleKeyboardInput();
			}

			if(this.currentScreen != null) {
				this.currentScreen.updateScreen();
			}
		}

		if(this.theWorld != null) {
			this.theWorld.difficultySetting = this.options.difficulty;
			if(!this.isGamePaused) {
				this.entityRenderer.updateRenderer();
			}

			if(!this.isGamePaused) {
				this.renderGlobal.updateClouds();
			}

			if(!this.isGamePaused) {
				this.theWorld.updateEntities();
			}

			if(!this.isGamePaused) {
				this.theWorld.tick();
			}

			if(!this.isGamePaused) {
				this.theWorld.randomDisplayUpdates((int)this.thePlayer.posX, (int)this.thePlayer.posY, (int)this.thePlayer.posZ);
			}

			if(!this.isGamePaused) {
				this.effectRenderer.updateEffects();
			}
		}

	}

	public final void generateLevel(int width, int worldShape, int depth, int height) {
		this.setLevel((World)null);
		System.gc();
		String string5 = this.session != null ? this.session.username : "anonymous";
		LevelGenerator levelGenerator6;
		(levelGenerator6 = new LevelGenerator(this.loadingScreen)).islandGen = depth == 1;
		levelGenerator6.floatingGen = depth == 2;
		levelGenerator6.flatGen = depth == 3;
		levelGenerator6.levelType = height;
		depth = width = 128 << width;
		short height1 = 64;
		if(worldShape == 1) {
			width /= 2;
			depth <<= 1;
		} else if(worldShape == 2) {
			depth = width /= 2;
			height1 = 256;
		}

		World width1 = levelGenerator6.generate(string5, width, depth, height1);
		this.setLevel(width1);
	}

	public final void setLevel(World world) {
	    if (this.theWorld != null) {
	        this.theWorld.setLevel();
	    }

	    this.theWorld = world;

	    if (world != null) {
	        world.load();

	        // Initialize playerController based on network mode
	        if (world.networkMode) {
	            this.playerController = new PlayerControllerCreative(this);
	        } else {
	            this.playerController = new PlayerControllerSP(this);
	        }

	        this.thePlayer = (EntityPlayerSP) world.findSubclassOf(EntityPlayerSP.class);
	        world.playerEntity = this.thePlayer;

	        if (this.thePlayer == null) {
	            this.thePlayer = new EntityPlayerSP(this, world, this.session);
	            this.thePlayer.preparePlayerToSpawn();
	            world.spawnEntityInWorld(this.thePlayer);
	            world.playerEntity = this.thePlayer;
	        }

	        if (this.thePlayer != null) {
	            this.thePlayer.movementInput = new MovementInputFromOptions(this.options);
	            this.playerController.onRespawn(this.thePlayer);
	        }

	        if (this.renderGlobal != null) {
	            this.renderGlobal.changeWorld(world);
	        }
	        if (this.effectRenderer != null) {
	            this.effectRenderer.clearEffects(world);
	        }

	        this.textureWaterFX.textureId = 0;
	        this.textureLavaFX.textureId = 0;
	        int waterTextureId = this.renderEngine.getTexture("/water.png");
	        if (world.defaultFluid == Block.waterMoving.blockID) {
	            this.textureWaterFX.textureId = waterTextureId;
	        } else {
	            this.textureLavaFX.textureId = waterTextureId;
	        }

	    System.gc();
	    }
	}

	public boolean isOnlineClient() {
	    return theWorld != null && theWorld.networkMode;
	}
}