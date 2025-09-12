package com.mojang.minecraft;

import com.mojang.minecraft.level.tile.Tile;

public final class User {
	public static final int[] creativeTiles = new int[]{Tile.rock.id, Tile.dirt.id, Tile.sponge.id, Tile.wood.id, Tile.bush.id, Tile.log.id, Tile.leaf.id, Tile.glass.id, Tile.gravel.id};
	public String name;
	public String sessionId;

	public User(String var1, String var2) {
		this.name = var1;
		this.sessionId = var2;
	}
}
