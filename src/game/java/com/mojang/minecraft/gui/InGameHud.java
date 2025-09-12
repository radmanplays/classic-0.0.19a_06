package com.mojang.minecraft.gui;

import com.mojang.minecraft.ChatLine;
import com.mojang.minecraft.Minecraft;
import com.mojang.minecraft.User;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.net.ConnectionManager;
import com.mojang.minecraft.net.NetworkPlayer;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class InGameHud {
	public List messages = new ArrayList();
	private Minecraft minecraft;
	private int scaledWidth;
	private int scaledHeight;

	public InGameHud(Minecraft var1, int var2, int var3) {
		this.minecraft = var1;
		this.scaledWidth = var2 * 240 / var3;
		this.scaledHeight = var3 * 240 / var3;
	}

	public final void render() {
		Font var1 = this.minecraft.font;
		this.minecraft.initGui();
		Textures var2 = this.minecraft.textures;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.minecraft.textures.getTextureId("/gui.png"));
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		Tesselator var3 = Tesselator.instance;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		blit(this.scaledWidth / 2 - 91, this.scaledHeight - 22, 0, 0, 182, 22);
		int var10000 = this.scaledWidth / 2 - 91 - 1;
		Minecraft var4 = this.minecraft;
		int var5 = 0;

		int var10001;
		while(true) {
			if(var5 >= User.creativeTiles.length) {
				var10001 = 0;
				break;
			}

			if(User.creativeTiles[var5] == var4.paintTexture) {
				var10001 = var5;
				break;
			}

			++var5;
		}

		blit(var10000 + var10001 * 20, this.scaledHeight - 22 - 1, 0, 22, 24, 22);
		GL11.glDisable(GL11.GL_BLEND);

		int var6;
		int var13;
		for(var13 = 0; var13 < 9; ++var13) {
			var5 = User.creativeTiles[var13];
			GL11.glPushMatrix();
			GL11.glTranslatef((float)(this.scaledWidth / 2 - 90 + var13 * 20), (float)(this.scaledHeight - 16), -50.0F);
			GL11.glScalef(10.0F, 10.0F, 10.0F);
			GL11.glTranslatef(1.0F, 0.5F, 0.0F);
			GL11.glRotatef(-30.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef(-1.5F, 0.5F, 0.5F);
			GL11.glScalef(-1.0F, -1.0F, -1.0F);
			var6 = var2.getTextureId("/terrain.png");
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, var6);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			var3.begin();
			Tile.tiles[var5].render(var3, this.minecraft.level, 0, -2, 0, 0);
			var3.end();
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}

		var1.drawShadow("0.0.19a_06", 2, 2, 16777215);
		var1.drawShadow(this.minecraft.fpsString, 2, 12, 16777215);
		byte var14 = 10;
		boolean var15 = false;
		if(this.minecraft.screen instanceof ChatScreen) {
			var14 = 20;
			var15 = true;
		}

		for(var6 = 0; var6 < this.messages.size() && var6 < var14; ++var6) {
			if(((ChatLine)this.messages.get(var6)).counter < 200 || var15) {
				var1.drawShadow(((ChatLine)this.messages.get(var6)).message, 2, this.scaledHeight - 8 - (var6 << 3) - 16, 16777215);
			}
		}

		var6 = this.scaledWidth / 2;
		int var9 = this.scaledHeight / 2;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		var3.begin();
		var3.vertex((float)(var6 + 1), (float)(var9 - 4), 0.0F);
		var3.vertex((float)var6, (float)(var9 - 4), 0.0F);
		var3.vertex((float)var6, (float)(var9 + 5), 0.0F);
		var3.vertex((float)(var6 + 1), (float)(var9 + 5), 0.0F);
		var3.vertex((float)(var6 + 5), (float)var9, 0.0F);
		var3.vertex((float)(var6 - 4), (float)var9, 0.0F);
		var3.vertex((float)(var6 - 4), (float)(var9 + 1), 0.0F);
		var3.vertex((float)(var6 + 5), (float)(var9 + 1), 0.0F);
		var3.end();
		if(Keyboard.isKeyDown(Keyboard.KEY_TAB) && this.minecraft.connectionManager != null && this.minecraft.connectionManager.isConnected()) {
			ConnectionManager var16 = this.minecraft.connectionManager;
			ArrayList var17 = new ArrayList();
			var17.add(var16.minecraft.user.name);
			Iterator var7 = var16.players.values().iterator();

			while(var7.hasNext()) {
				NetworkPlayer var10 = (NetworkPlayer)var7.next();
				var17.add(var10.name);
			}

			ArrayList var8 = var17;
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.7F);
			GL11.glVertex2f((float)(var6 + 128), (float)(var9 - 68 - 12));
			GL11.glVertex2f((float)(var6 - 128), (float)(var9 - 68 - 12));
			GL11.glColor4f(0.2F, 0.2F, 0.2F, 0.8F);
			GL11.glVertex2f((float)(var6 - 128), (float)(var9 + 68));
			GL11.glVertex2f((float)(var6 + 128), (float)(var9 + 68));
			GL11.glEnd();
			GL11.glDisable(GL11.GL_BLEND);
			String var11 = "Connected players:";
			var1.drawShadow(var11, var6 - var1.width(var11) / 2, var9 - 64 - 12, 16777215);

			for(int var12 = 0; var12 < var8.size(); ++var12) {
				var13 = var6 + var12 % 2 * 120 - 120;
				var5 = var9 - 64 + (var12 / 2 << 3);
				var1.draw((String)var8.get(var12), var13, var5, 16777215);
			}
		}

	}

	private static void blit(int var0, int var1, int var2, int var3, int var4, int var5) {
		float var7 = 0.00390625F;
		float var8 = 0.015625F;
		Tesselator var6 = Tesselator.instance;
		var6.begin();
		var6.vertexUV((float)var0, (float)(var1 + 22), -90.0F, 0.0F, (float)(var3 + 22) * var8);
		var6.vertexUV((float)(var0 + var4), (float)(var1 + 22), -90.0F, (float)(var4 + 0) * var7, (float)(var3 + 22) * var8);
		var6.vertexUV((float)(var0 + var4), (float)var1, -90.0F, (float)(var4 + 0) * var7, (float)var3 * var8);
		var6.vertexUV((float)var0, (float)var1, -90.0F, 0.0F, (float)var3 * var8);
		var6.end();
	}
}
