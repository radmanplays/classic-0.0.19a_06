package com.mojang.minecraft;

import com.mojang.minecraft.character.Vec3;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.character.ZombieModel;
import com.mojang.minecraft.gui.ChatScreen;
import com.mojang.minecraft.gui.ErrorScreen;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.gui.PauseScreen;
import com.mojang.minecraft.gui.Screen;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelIO;
import com.mojang.minecraft.level.levelgen.LevelGen;
import com.mojang.minecraft.level.liquid.Liquid;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.net.ConnectionManager;
import com.mojang.minecraft.net.NetworkPlayer;
import com.mojang.minecraft.net.Packet;
import com.mojang.minecraft.particle.Particle;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.player.MovementInputFromOptions;
import com.mojang.minecraft.player.Player;
import com.mojang.minecraft.renderer.Chunk;
import com.mojang.minecraft.renderer.Frustum;
import com.mojang.minecraft.renderer.LevelRenderer;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
import com.mojang.util.GLAllocation;
import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EagUtils;
import com.mojang.minecraft.renderer.DirtyChunkSorter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import net.lax1dude.eaglercraft.internal.EnumPlatformType;
import net.lax1dude.eaglercraft.internal.buffer.FloatBuffer;
import net.lax1dude.eaglercraft.internal.buffer.IntBuffer;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;

public final class Minecraft implements Runnable {
	private boolean fullscreen = false;
	public int width;
	public int height;
	private FloatBuffer fogColor0 = GLAllocation.createFloatBuffer(4);
	private FloatBuffer fogColor1 = GLAllocation.createFloatBuffer(4);
	private Timer timer = new Timer(20.0F);
	public Level level;
	private LevelRenderer levelRenderer;
	public Player player;
	private int paintTexture = 1;
	private ParticleEngine particleEngine;
	public User user = null;
	private int yMouseAxis = 1;
	private Textures textures;
	public Font font;
	private int editMode = 0;
	private Screen screen = null;
	private LevelIO levelIo = new LevelIO(this);
	private LevelGen levelGen = new LevelGen(this);
	private int ticksRan = 0;
	public String loadMapUser = null;
	public int loadMapID = 0;
	public ConnectionManager connectionManager;
	private List chatMessages = new ArrayList();
	String server = null;
	int port = 0;
	private float fogColorRed = 0.5F;
	private float fogColorGreen = 0.8F;
	private float fogColorBlue = 1.0F;
	private volatile boolean running = false;
	private String fpsString = "";
	private boolean mouseGrabbed = false;
	private int prevFrameTime = 0;
	private float renderDistance = 0.0F;
	private IntBuffer viewportBuffer = GLAllocation.createIntBuffer(16);
	private IntBuffer selectBuffer = GLAllocation.createIntBuffer(2000);
	private HitResult hitResult = null;
	private float fogColorMultiplier = 1.0F;
	private volatile int unusedInt1 = 0;
	private volatile int unusedInt2 = 0;
	private FloatBuffer lb = GLAllocation.createFloatBuffer(16);
	private String title = "";
	private String text = "";
	public boolean hideGui = false;
	public ZombieModel playerModel = new ZombieModel();

	
	public Minecraft(int var2, int var3, boolean var4) {
		this.width = width;
		this.height = height;
		this.fullscreen = false;
		this.textures = new Textures();
	}
	
	public final void setServer(String var1) {
		server = var1;
	}
	
	public final void setScreen(Screen var1) {
		if(!(this.screen instanceof ErrorScreen)) {
			if(this.screen != null) {
				this.screen.closeScreen();
			}

			this.screen = var1;
			if(var1 != null) {
				if(this.mouseGrabbed) {
					this.player.releaseAllKeys();
					this.mouseGrabbed = false;
					Mouse.setGrabbed(false);
				}

				int var2 = this.width * 240 / this.height;
				int var3 = this.height * 240 / this.height;
				var1.init(this, var2, var3);
			} else {
				this.grabMouse();
			}
		}
	}
	
	private static void checkGlError(String string) {
		int errorCode = GL11.glGetError();
		if(errorCode != 0) {
			String errorString = GLU.gluErrorString(errorCode);
			System.out.println("########## GL ERROR ##########");
			System.out.println("@ " + string);
			System.out.println(errorCode + ": " + errorString);
			throw new RuntimeException(errorCode + ": " + errorString);

		}

	}

	public final void destroy() {
		Minecraft var2 = this;
		try {
			if(this.connectionManager == null && var2.level != null) {
				LevelIO.save(var2.level, new VFile2("level.dat"));
			}
		} catch (Exception var1) {
			var1.printStackTrace();
		}
		if(this.connectionManager != null) {
			connectionManager.connection.disconnect();
		}
		EagRuntime.destroy();
	}

	public final void run() {
		this.running = true;

		try {
			Minecraft var4 = this;
			this.fogColor0.put(new float[]{this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 1.0F});
			this.fogColor0.flip();
			this.fogColor1.put(new float[]{(float)14 / 255.0F, (float)11 / 255.0F, (float)10 / 255.0F, 1.0F});
			this.fogColor1.flip();
			if(this.fullscreen) {
				Display.toggleFullscreen();
				this.width = Display.getWidth();
				this.height = Display.getHeight();
			} else {
				this.width = Display.getWidth();
				this.height = Display.getHeight();
			}

			Display.setTitle("Minecraft 0.0.17a");

			Display.create();
			Keyboard.create();
			Mouse.create();

			checkGlError("Pre startup");
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glClearDepth(1.0D);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			checkGlError("Startup");
			this.font = new Font("/default.png", this.textures);
			IntBuffer var8 = GLAllocation.createIntBuffer(256);
			var8.clear().limit(256);
			GL11.glViewport(0, 0, this.width, this.height);
			if(this.server != null && this.user != null) {
				this.connectionManager = new ConnectionManager(this, this.server, this.user.name);
				this.level = null;
			} else {
				boolean var9 = false;
	
				try {
					Level var10 = null;
					var10 = var4.levelIo.load(new VFile2("level.dat"));
					var9 = var10 != null;
					if(!var9) {
						var10 = var4.levelIo.loadLegacy(new VFile2("level.dat"));
						var9 = var10 != null;
					}
	
					var4.setLevel(var10);
				} catch (Exception var20) {
					var20.printStackTrace();
					var9 = false;
				}
	
				if(!var9) {
					this.generateLevel(1);
				}
			}

			this.levelRenderer = new LevelRenderer(this.textures);
			this.particleEngine = new ParticleEngine(this.level, this.textures);
			this.player = new Player(this.level, new MovementInputFromOptions());
			this.player.resetPos();
			if(this.level != null) {
				this.setLevel(this.level);
			}

			checkGlError("Post startup");
		} catch (Exception var26) {
			var26.printStackTrace();
			System.out.println("Failed to start Minecraft");
			return;
		}

		long var1 = System.currentTimeMillis();
		int var3 = 0;

		try {
			while(this.running) {
					if(Display.isCloseRequested()) {
						if(this.connectionManager != null) {
							connectionManager.connection.disconnect();
						}
						this.running = false;
					}
					
					if(this.connectionManager != null) {
						ConnectionManager c = this.connectionManager;

							try {
								c.connection.processData();
							} catch (IOException var7) {
								c.minecraft.setScreen(new ErrorScreen("Disconnected!", "You\'ve lost connection to the server"));
								var7.printStackTrace();
								c.connection.disconnect();
								c.minecraft.connectionManager = null;
							}
					}

					try {
						Timer var31 = this.timer;
						long var7 = System.nanoTime();
						long var35 = var7 - var31.lastTime;
						var31.lastTime = var7;
						if(var35 < 0L) {
							var35 = 0L;
						}

						if(var35 > 1000000000L) {
							var35 = 1000000000L;
						}

						var31.fps += (float)var35 * var31.timeScale * var31.ticksPerSecond / 1.0E9F;
						var31.ticks = (int)var31.fps;
						if(var31.ticks > 100) {
							var31.ticks = 100;
						}

						var31.fps -= (float)var31.ticks;
						var31.a = var31.fps;

						for(int var32 = 0; var32 < this.timer.ticks; ++var32) {
							++this.ticksRan;
							this.tick();
						}

						checkGlError("Pre render");
						float var33 = this.timer.a;
						if(!Display.isActive()) {
							if(this.screen == null){
								this.pauseGame();
							}
						}

						int var5;
						int var34;
						int var38;
						if(this.mouseGrabbed) {
							var34 = 0;
							var38 = 0;
							var34 = Mouse.getDX();
							var38 = Mouse.getDY();

							this.player.turn((float)var34, (float)(var38 * this.yMouseAxis));
						}

						if(!this.hideGui) {
							if(this.level != null) {
								this.render(var33);
								this.renderGui();
								checkGlError("Rendered gui");
							} else {
								GL11.glViewport(0, 0, this.width, this.height);
								GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
								GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
								GL11.glMatrixMode(GL11.GL_PROJECTION);
								GL11.glLoadIdentity();
								GL11.glMatrixMode(GL11.GL_MODELVIEW);
								GL11.glLoadIdentity();
								this.initGui();
							}

							if(this.screen != null) {
								var34 = this.width * 240 / this.height;
								var38 = this.height * 240 / this.height;
								int var37 = Mouse.getX() * var34 / this.width;
								var5 = var38 - Mouse.getY() * var38 / this.height - 1;
								this.screen.render(var37, var5);
							}

							Display.update();
						}

						checkGlError("Post render");
						++var3;
					} catch (Exception var27) {
						this.setScreen(new ErrorScreen("Client error", "The game broke! [" + var27 + "]"));
					}

					while(System.currentTimeMillis() >= var1 + 1000L) {
						this.fpsString = var3 + " fps, " + Chunk.updates + " chunk updates";
						Chunk.updates = 0;
						var1 += 1000L;
						var3 = 0;
					}
				}

			return;
		} catch (StopGameException var27) {
			return;
		} catch (Exception var24) {
			var24.printStackTrace();
		} finally {
			this.destroy();
		}

	}
	
	public final void grabMouse() {
		if(!this.mouseGrabbed) {
			this.mouseGrabbed = true;
			Mouse.setGrabbed(true);
			this.setScreen((Screen)null);
			this.prevFrameTime = this.ticksRan + 10000;
		}
	}
	
	private void pauseGame() {
		this.setScreen(new PauseScreen());
	}
	
	private int saveCountdown = 600;

	private void levelSave() {
	    if (level == null) return;

	    saveCountdown--;
	    if (saveCountdown <= 0) {
	    	LevelIO.save(this.level, new VFile2("level.dat"));
	        saveCountdown = 600;
	    }
	}
	

	private void clickMouse() {
		if(this.hitResult != null) {
			int var1 = this.hitResult.x;
			int var2 = this.hitResult.y;
			int var3 = this.hitResult.z;
			if(this.editMode != 0) {
				if(this.hitResult.f == 0) {
					--var2;
				}

				if(this.hitResult.f == 1) {
					++var2;
				}

				if(this.hitResult.f == 2) {
					--var3;
				}

				if(this.hitResult.f == 3) {
					++var3;
				}

				if(this.hitResult.f == 4) {
					--var1;
				}

				if(this.hitResult.f == 5) {
					++var1;
				}
			}

			Tile var4 = Tile.tiles[this.level.getTile(var1, var2, var3)];
			if(this.editMode == 0) {
				boolean var7 = this.level.setTile(var1, var2, var3, 0);
				if(var4 != null && var7) {
					if(this.isMultiplayer()) {
						this.connectionManager.sendBlockChange(var1, var2, var3, this.editMode, this.paintTexture);
					}

					var4.destroy(this.level, var1, var2, var3, this.particleEngine);
				}

			} else {
				Tile var5 = Tile.tiles[this.level.getTile(var1, var2, var3)];
				if(var5 == null || var5 == Tile.water || var5 == Tile.calmWater || var5 == Tile.lava || var5 == Tile.calmLava) {
					AABB var6 = Tile.tiles[this.paintTexture].getAABB(var1, var2, var3);
					if(var6 == null || (this.player.bb.intersects(var6) ? false : this.level.isFree(var6))) {
						if(this.isMultiplayer()) {
							this.connectionManager.sendBlockChange(var1, var2, var3, this.editMode, this.paintTexture);
						}

						this.level.setTile(var1, var2, var3, this.paintTexture);
						Tile.tiles[this.paintTexture].onBlockAdded(this.level, var1, var2, var3);
					}
				}

			}
		}
	}
	
	private void tick() {
		for(int var1 = 0; var1 < this.chatMessages.size(); ++var1) {
			++((ChatLine)this.chatMessages.get(var1)).counter;
		}
		int var3;
		int var4;
		int var10;
		if(this.connectionManager != null) {
			Player var9 = this.player;
			ConnectionManager var8 = this.connectionManager;
			var3 = (int)(var9.x * 32.0F);
			var4 = (int)(var9.y * 32.0F);
			int var5 = (int)(var9.z * 32.0F);
			int var6 = (int)(var9.yRot * 256.0F / 360.0F) & 255;
			var10 = (int)(var9.xRot * 256.0F / 360.0F) & 255;
			var8.connection.sendPacket(Packet.PLAYER_TELEPORT, new Object[]{Integer.valueOf(-1), Integer.valueOf(var3), Integer.valueOf(var4), Integer.valueOf(var5), Integer.valueOf(var6), Integer.valueOf(var10)});
		}

		LevelRenderer var11;
		if(this.screen != null) {
			this.prevFrameTime = this.ticksRan + 10000;
		} else {
			if(Mouse.isMouseGrabbed() || Mouse.isActuallyGrabbed()) {
				this.mouseGrabbed = true;
			}
			label194:
			while(true) {
				int var8;
				while(Mouse.next()) {
					var8 = Mouse.getEventDWheel();
					Minecraft var9;
					if(var8 != 0) {
						var10 = var8;
						var9 = this;
						if(var8 > 0) {
							var10 = 1;
						}

						if(var10 < 0) {
							var10 = -1;
						}

						var3 = 0;

						for(var4 = 0; var4 < User.creativeTiles.length; ++var4) {
							if(User.creativeTiles[var4] == var9.paintTexture) {
								var3 = var4;
							}
						}

						for(var3 += var10; var3 < 0; var3 += User.creativeTiles.length) {
						}

						while(var3 >= User.creativeTiles.length) {
							var3 -= User.creativeTiles.length;
						}

						var9.paintTexture = User.creativeTiles[var3];
					}

					if(!this.mouseGrabbed && Mouse.getEventButtonState()) {
						this.grabMouse();
					} else {
						if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
							this.clickMouse();
							this.prevFrameTime = this.ticksRan;
						}

						if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
							this.editMode = (this.editMode + 1) % 2;
						}

						if(Mouse.getEventButton() == 2 && Mouse.getEventButtonState()) {
							var9 = this;
							if(this.hitResult != null) {
								var10 = this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z);
								if(var10 == Tile.grass.id) {
									var10 = Tile.dirt.id;
								}

								for(var3 = 0; var3 < User.creativeTiles.length; ++var3) {
									if(var10 == User.creativeTiles[var3]) {
										var9.paintTexture = User.creativeTiles[var3];
									}
								}
							}
						}
					} 
				}

				while(true) {
					do {
						if(!Keyboard.next()) {
							if(Mouse.isButtonDown(0) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.mouseGrabbed) {
								this.clickMouse();
								this.prevFrameTime = this.ticksRan;
							}
							break label194;
						}

						this.player.setKey(Keyboard.getEventKey(), Keyboard.getEventKeyState());
					} while(!Keyboard.getEventKeyState());

					if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
						this.pauseGame();
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_R) {
						this.player.resetPos();
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
						this.level.setSpawnPos((int)this.player.x, (int)this.player.y, (int)this.player.z, this.player.yRot);
						this.player.resetPos();
					}

					for(var8 = 0; var8 < 9; ++var8) {
						if(Keyboard.getEventKey() == var8 + 2) {
							this.paintTexture = User.creativeTiles[var8];
						}
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_Y) {
						this.yMouseAxis = -this.yMouseAxis;
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_G && this.connectionManager == null && this.level.entities.size() < 256) {
						this.level.entities.add(new Zombie(this.level, this.player.x, this.player.y, this.player.z));
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_F) {
						LevelRenderer var10000 = this.levelRenderer;
						boolean var15 = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
						LevelRenderer var12 = var10000;
						var12.drawDistance = var12.drawDistance + (var15 ? -1 : 1) & 3;
					}
					
					if(Keyboard.getEventKey() == Keyboard.KEY_T  && this.connectionManager != null && this.connectionManager.isConnected()) {
						this.player.releaseAllKeys();
						this.setScreen(new ChatScreen());
					}
				}
			}
		}

		if(this.screen != null) {
			this.screen.updateEvents();
			if(this.screen != null) {
				this.screen.tick();
			}
		}
		
		if(this.connectionManager != null) {
			this.connectionManager.tick();
		}
		
		if(this.level != null) {
			var11 = this.levelRenderer;
			++var11.cloudTickCounter;
			this.level.tickEntities();
			if(!this.isMultiplayer()) {
				this.level.tick();
			}

			ParticleEngine var12 = this.particleEngine;

			for(var10 = 0; var10 < var12.particles.size(); ++var10) {
				Particle var13 = (Particle)var12.particles.get(var10);
				var13.tick();
				if(var13.removed) {
					var12.particles.remove(var10--);
				}
			}

			this.player.tick();
			if(this.connectionManager == null) {
				levelSave();
			}
		}
	}
	
	private boolean isMultiplayer() {
		return this.connectionManager != null;
	}

	private void orientCamera(float var1) {
		GL11.glTranslatef(0.0F, 0.0F, -0.3F);
		GL11.glRotatef(this.player.xRotO + (this.player.xRot - this.player.xRotO) * var1, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(this.player.yRotO + (this.player.yRot - this.player.yRotO) * var1, 0.0F, 1.0F, 0.0F);
		float var2 = this.player.xo + (this.player.x - this.player.xo) * var1;
		float var3 = this.player.yo + (this.player.y - this.player.yo) * var1;
		float var4 = this.player.zo + (this.player.z - this.player.zo) * var1;
		GL11.glTranslatef(-var2, -var3, -var4);
	}

	private void render(float var1) {
		if(!Display.isActive()) {
			this.pauseGame();
		}
		if (Display.wasResized()) {
			this.width = Display.getWidth();
			this.height = Display.getHeight();
			
			if(this.screen != null) {
				Screen sc = this.screen;
				this.setScreen((Screen)null);
				this.setScreen(sc);
			}
		}
		GL11.glViewport(0, 0, this.width, this.height);
		float var4 = 1.0F / (float)(4 - this.levelRenderer.drawDistance);
		var4 = (float)Math.pow((double)var4, 0.25D);
		this.fogColorRed = 0.6F * (1.0F - var4) + var4;
		this.fogColorGreen = 0.8F * (1.0F - var4) + var4;
		this.fogColorBlue = 1.0F * (1.0F - var4) + var4;
		this.fogColorRed *= this.fogColorMultiplier;
		this.fogColorGreen *= this.fogColorMultiplier;
		this.fogColorBlue *= this.fogColorMultiplier;
		Tile var5 = Tile.tiles[this.level.getTile((int)this.player.x, (int)(this.player.y + 0.12F), (int)this.player.z)];
		if(var5 != null && var5.getLiquidType() != Liquid.none) {
			Liquid var22 = var5.getLiquidType();
			if(var22 == Liquid.water) {
				this.fogColorRed = 0.02F;
				this.fogColorGreen = 0.02F;
				this.fogColorBlue = 0.2F;
			} else if(var22 == Liquid.lava) {
				this.fogColorRed = 0.6F;
				this.fogColorGreen = 0.1F;
				this.fogColorBlue = 0.0F;
			}
		}

		GL11.glClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
		checkGlError("Set viewport");
		float pitch = this.player.xRot;
		float yaw = this.player.yRot;

		double px = this.player.x;
		double py = this.player.y;
		double pz = this.player.z;

		Vec3 cameraPos = new Vec3((float)px, (float)py, (float)pz);

		float cosYaw = (float)Math.cos(-Math.toRadians(yaw) - Math.PI);
		float sinYaw = (float)Math.sin(-Math.toRadians(yaw) - Math.PI);
		float cosPitch = (float)Math.cos(-Math.toRadians(pitch));
		float sinPitch = (float)Math.sin(-Math.toRadians(pitch));

		float dirX = sinYaw * cosPitch;
		float dirY = sinPitch;
		float dirZ = cosYaw * cosPitch;
		float reachDistance = 3.0F;
		if (pitch > 60.0F) {
		    reachDistance += 1.0F;
		}
		if (pitch >= 55.0F && pitch <= 60.0F) {
		    reachDistance += 2.0F;
		}
		Vec3 reachVec = new Vec3(
		    cameraPos.x + dirX * reachDistance,
		    cameraPos.y + dirY * reachDistance,
		    cameraPos.z + dirZ * reachDistance
		);

		this.hitResult = this.level.clip(cameraPos, reachVec);
		checkGlError("Picked");
		this.fogColorMultiplier = 1.0F;
		this.renderDistance = (float)(512 >> (this.levelRenderer.drawDistance << 1));
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(70.0F, (float)this.width / (float)this.height, 0.05F, this.renderDistance);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
	    if (!Display.isActive() || !Mouse.isMouseGrabbed() || !Mouse.isActuallyGrabbed()) {
	        if (System.currentTimeMillis() - prevFrameTime > 250L) {
	            if (this.screen == null) {
	            	this.pauseGame();
	            }
	        }
	    }
	    
		this.orientCamera(var1);
		checkGlError("Set up camera");
		GL11.glEnable(GL11.GL_CULL_FACE);
		Frustum var23 = Frustum.getFrustum();
		Frustum var24 = var23;
		LevelRenderer lr = this.levelRenderer;

		for(int i = 0; i < lr.sortedChunks.length; ++i) {
			lr.sortedChunks[i].isInFrustum(var24);
		}

		Player var19 = this.player;
		lr = this.levelRenderer;
		List<Chunk> var28 = new ArrayList<>(lr.dirtyChunks);
		var28.sort(new DirtyChunkSorter(var19));
		var28.addAll(lr.dirtyChunks);
		int var25 = 4;
		Iterator var29 = var28.iterator();

		while(var29.hasNext()) {
			Chunk var30 = (Chunk)var29.next();
			var30.rebuild();
			lr.dirtyChunks.remove(var30);
			--var25;
			if(var25 == 0) {
				break;
			}
		}


		checkGlError("Update chunks");
		boolean var21 = this.level.isSolid(this.player.x, this.player.y, this.player.z, 0.1F);
		this.setupFog();
		GL11.glEnable(GL11.GL_FOG);
		this.levelRenderer.render(this.player, 0);
		if(var21) {
			int x = (int)this.player.x;
			int y = (int)this.player.y;
			var25 = (int)this.player.z;

			for(int var2 = x - 1; var2 <= x + 1; ++var2) {
				for(int var7 = y - 1; var7 <= y + 1; ++var7) {
					for(int var8 = var25 - 1; var8 <= var25 + 1; ++var8) {
						this.levelRenderer.render(var2, var7, var8);
					}
				}
			}
		}

		checkGlError("Rendered level");
		this.levelRenderer.renderEntities(var23, var1);
		checkGlError("Rendered entities");
		this.particleEngine.render(this.player, var1);
		checkGlError("Rendered particles");
		lr = this.levelRenderer;
		lr.renderSurroundingGround();
		GL11.glDisable(GL11.GL_LIGHTING);
		this.setupFog();
		this.levelRenderer.renderClouds(var1);
		this.setupFog();
		GL11.glEnable(GL11.GL_LIGHTING);
		if(this.hitResult != null) {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			this.levelRenderer.renderHit(this.player, this.hitResult, this.editMode, this.paintTexture);
			LevelRenderer.renderHitOutline(this.hitResult, this.editMode);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glEnable(GL11.GL_LIGHTING);
		}

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		this.setupFog();
		lr = this.levelRenderer;
//		lr.renderSurroundingGround();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
//		GL11.glColorMask(false, false, false, false);
		int var22 = this.levelRenderer.render(this.player, 1);
//		GL11.glColorMask(true, true, true, true);
		if(var22 > 0) {
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, lr.textures.getTextureId("/terrain.png"));
			GL11.glCallLists(lr.dummyBuffer);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if(this.hitResult != null) {
			GL11.glDepthFunc(GL11.GL_LESS);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
//			this.levelRenderer.renderHit(this.player, this.hitResult, this.editMode, this.paintTexture);
			LevelRenderer.renderHitOutline(this.hitResult, this.editMode);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		}
	}
	
	private void initGui() {
		int var1 = this.width * 240 / this.height;
		int var2 = this.height * 240 / this.height;
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, (double)var1, (double)var2, 0.0D, 100.0D, 300.0D);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslatef(0.0F, 0.0F, -200.0F);
	}

	private void renderGui() {
		int var1 = this.width * 240 / this.height;
		int var2 = this.height * 240 / this.height;
		this.initGui();
		checkGlError("GUI: Init");
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(var1 - 16), 16.0F, -50.0F);
		Tesselator var3 = Tesselator.instance;
		GL11.glScalef(16.0F, 16.0F, 16.0F);
		GL11.glRotatef(-30.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(-1.5F, 0.5F, 0.5F);
		GL11.glScalef(-1.0F, -1.0F, -1.0F);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int var4 = this.textures.getTextureId("/terrain.png");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, var4);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		var3.begin();
		Tile.tiles[this.paintTexture].render(var3, this.level, 0, -2, 0, 0);
		var3.end();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
		checkGlError("GUI: Draw selected");
		this.font.drawShadow("0.0.18a_02", 2, 2, 16777215);
		this.font.drawShadow(this.fpsString, 2, 12, 16777215);

		byte var13 = 10;
		boolean var5 = false;
		if(this.screen instanceof ChatScreen) {
			var13 = 20;
			var5 = true;
		}

		int var6;
		for(var6 = 0; var6 < this.chatMessages.size() && var6 < var13; ++var6) {
			if(((ChatLine)this.chatMessages.get(var6)).counter < 200 || var5) {
				this.font.drawShadow(((ChatLine)this.chatMessages.get(var6)).message, 2, var2 - 8 - (var6 << 3) - 16, 16777215);
			}
		}

		checkGlError("GUI: Draw text");
		var4 = var1 / 2;
		int var8 = var2 / 2;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		var3.begin();
		var3.vertex((float)(var4 + 1), (float)(var8 - 4), 0.0F);
		var3.vertex((float)var4, (float)(var8 - 4), 0.0F);
		var3.vertex((float)var4, (float)(var8 + 5), 0.0F);
		var3.vertex((float)(var4 + 1), (float)(var8 + 5), 0.0F);
		var3.vertex((float)(var4 + 5), (float)var8, 0.0F);
		var3.vertex((float)(var4 - 4), (float)var8, 0.0F);
		var3.vertex((float)(var4 - 4), (float)(var8 + 1), 0.0F);
		var3.vertex((float)(var4 + 5), (float)(var8 + 1), 0.0F);
		var3.end();
		checkGlError("GUI: Draw crosshair");
		
		if(Keyboard.isKeyDown(Keyboard.KEY_TAB) && this.connectionManager != null && this.connectionManager.isConnected()) {
			ConnectionManager var7 = this.connectionManager;
			ArrayList var10 = new ArrayList();
			var10.add(var7.minecraft.user.name);
			Iterator players = var7.players.values().iterator();

			while(players.hasNext()) {
				NetworkPlayer var14 = (NetworkPlayer)players.next();
				var10.add(var14.name);
			}

			ArrayList var9 = var10;
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.7F);
			GL11.glVertex2f((float)(var4 + 128), (float)(var8 - 68 - 12));
			GL11.glVertex2f((float)(var4 - 128), (float)(var8 - 68 - 12));
			GL11.glColor4f(0.2F, 0.2F, 0.2F, 0.8F);
			GL11.glVertex2f((float)(var4 - 128), (float)(var8 + 68));
			GL11.glVertex2f((float)(var4 + 128), (float)(var8 + 68));
			GL11.glEnd();
			GL11.glDisable(GL11.GL_BLEND);
			String var11 = "Connected players:";
                        this.font.drawShadow(var11, var4 - this.font.width(var11) / 2, var8 - 64 - 12, 16777215);

			for(int var12 = 0; var12 < var9.size(); ++var12) {
				int var15 = var4 + var12 % 2 * 120 - 120;
				int var16 = var8 - 64 + (var12 / 2 << 3);
				this.font.draw((String)var9.get(var12), var15, var16, 16777215);
			}
		}
	}
	
	private void setupFog() {
		GL11.glFog(GL11.GL_FOG_COLOR, this.getBuffer(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 1.0F));
		Tile var1 = Tile.tiles[this.level.getTile((int)this.player.x, (int)(this.player.y + 0.12F), (int)this.player.z)];
		if(var1 != null && var1.getLiquidType() != Liquid.none) {
			Liquid var2 = var1.getLiquidType();
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			if(var2 == Liquid.water) {
				GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1F);
//				GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, this.getBuffer(0.4F, 0.4F, 0.9F, 1.0F));
			} else if(var2 == Liquid.lava) {
				GL11.glFogf(GL11.GL_FOG_DENSITY, 2.0F);
//				GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, this.getBuffer(0.4F, 0.3F, 0.3F, 1.0F));
			}
		} else {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
			GL11.glFogf(GL11.GL_FOG_START, 0.0F);
			GL11.glFogf(GL11.GL_FOG_END, this.renderDistance);
//			GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, this.getBuffer(1.0F, 1.0F, 1.0F, 1.0F));
		}

//		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
//		GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	private FloatBuffer getBuffer(float a, float b, float c, float d) {
		this.lb.clear();
		this.lb.put(a).put(b).put(c).put(d);
		this.lb.flip();
		return this.lb;
	}

	public final void beginLevelLoading(String var1) {
		if(!this.running) {
			throw new StopGameException();
		} else {
			this.title = var1;
		    if (this.height == 0) {
		        return;
		    }
			int var3 = this.width * 240 / this.height;
			int var2 = this.height * 240 / this.height;
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glOrtho(0.0D, (double)var3, (double)var2, 0.0D, 100.0D, 300.0D);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			GL11.glTranslatef(0.0F, 0.0F, -200.0F);
		}
	}

	public final void levelLoadUpdate(String var1) {
		if(!this.running) {
			throw new StopGameException();
		} else {
			this.text = var1;
			this.setLoadingProgress(-1);
		}
	}

	public final void setLoadingProgress(int var1) {
		if(!this.running) {
			throw new StopGameException();
		} else {
			int var2 = this.width * 240 / this.height;
			int var3 = this.height * 240 / this.height;
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
			Tesselator var4 = Tesselator.instance;
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			int var5 = this.textures.getTextureId("/dirt.png");
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, var5);
			float var8 = 32.0F;
			var4.begin();
			var4.color(4210752);
			var4.vertexUV(0.0F, (float)var3, 0.0F, 0.0F, (float)var3 / var8);
			var4.vertexUV((float)var2, (float)var3, 0.0F, (float)var2 / var8, (float)var3 / var8);
			var4.vertexUV((float)var2, 0.0F, 0.0F, (float)var2 / var8, 0.0F);
			var4.vertexUV(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
			var4.end();
			if(var1 >= 0) {
				var5 = var2 / 2 - 50;
				int var6 = var3 / 2 + 16;
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				var4.begin();
				var4.color(8421504);
				var4.vertex((float)var5, (float)var6, 0.0F);
				var4.vertex((float)var5, (float)(var6 + 2), 0.0F);
				var4.vertex((float)(var5 + 100), (float)(var6 + 2), 0.0F);
				var4.vertex((float)(var5 + 100), (float)var6, 0.0F);
				var4.color(8454016);
				var4.vertex((float)var5, (float)var6, 0.0F);
				var4.vertex((float)var5, (float)(var6 + 2), 0.0F);
				var4.vertex((float)(var5 + var1), (float)(var6 + 2), 0.0F);
				var4.vertex((float)(var5 + var1), (float)var6, 0.0F);
				var4.end();
				GL11.glEnable(GL11.GL_TEXTURE_2D);
			}

			this.font.drawShadow(this.title, (var2 - this.font.width(this.title)) / 2, var3 / 2 - 4 - 16, 16777215);
			this.font.drawShadow(this.text, (var2 - this.font.width(this.text)) / 2, var3 / 2 - 4 + 8, 16777215);
			Display.update();
		}
	}

	public final void generateLevel(int var1) {
		String var2 = this.user != null ? this.user.name : "anonymous";
		this.setLevel(this.levelGen.generateLevel(var2, 128 << var1, 128 << var1, 64));
	}

	public final void setLevel(Level var1) {
		this.level = var1;
		if(this.levelRenderer != null) {
			LevelRenderer var2 = this.levelRenderer;
			if(var2.level != null) {
				var2.level.removeListener(var2);
			}

			var2.level = var1;
			if(var1 != null) {
				var1.addListener(var2);
				var2.compileSurroundingGround();
			}
		}

		if(this.particleEngine != null) {
			ParticleEngine var4 = this.particleEngine;
			var4.particles.clear();
		}

		if(this.player != null) {
			this.player.setLevel(var1);
			this.player.resetPos();
		}

		System.gc();
	}
	
	public final void addChatMessage(String var1) {
		this.chatMessages.add(0, new ChatLine(var1));

		while(this.chatMessages.size() > 50) {
			this.chatMessages.remove(this.chatMessages.size() - 1);
		}

	}
}
