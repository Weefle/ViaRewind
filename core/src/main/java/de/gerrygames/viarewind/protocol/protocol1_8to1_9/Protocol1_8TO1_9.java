package de.gerrygames.viarewind.protocol.protocol1_8to1_9;

import de.gerrygames.viarewind.protocol.protocol1_8to1_9.chunks.BlockStorage;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.chunks.ChunkPacketTransformer;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.items.ItemReplacement;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.items.ItemRewriter;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.metadata.MetadataRewriter;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.sound.SoundRemapper;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.BlockPlaceDestroyTracker;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.BossBarStorage;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.Cooldown;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.Levitation;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.PlayerPosition;
import de.gerrygames.viarewind.utils.ChatUtil;
import de.gerrygames.viarewind.utils.PacketUtil;
import de.gerrygames.viarewind.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_10Types;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueCreator;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_8;
import us.myles.ViaVersion.api.type.types.version.Types1_9;
import us.myles.ViaVersion.packets.State;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Protocol1_8TO1_9 extends Protocol {
	public static final ValueTransformer<Double, Integer> toOldInt = new ValueTransformer<Double, Integer>(Type.INT) {
		public Integer transform(PacketWrapper wrapper, Double inputValue) {
			return (int)(inputValue * 32.0D);
		}
	};
	public static final ValueTransformer<Float, Byte> degreesToAngle = new ValueTransformer<Float, Byte>(Type.BYTE) {
		@Override
		public Byte transform(PacketWrapper packetWrapper, Float degrees) throws Exception {
			return (byte)((degrees/360F) * 256);
		}
	};

	@Override
	protected void registerPackets() {

		//Spawn Object
		this.registerOutgoing(State.PLAY, 0x00, 0x0E, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.UUID);
					}
				});
				map(Type.BYTE);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.BYTE);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int data = packetWrapper.read(Type.INT);
						packetWrapper.write(Type.INT, data);
						short vX = packetWrapper.read(Type.SHORT);
						short vY = packetWrapper.read(Type.SHORT);
						short vZ = packetWrapper.read(Type.SHORT);
						if (data!=0) {
							packetWrapper.write(Type.SHORT, vX);
							packetWrapper.write(Type.SHORT, vY);
							packetWrapper.write(Type.SHORT, vZ);
						} else {
							int entityId = packetWrapper.get(Type.VAR_INT, 0);
							PacketWrapper velocity = new PacketWrapper(0x12, null, packetWrapper.user());
							velocity.write(Type.VAR_INT, entityId);
							velocity.write(Type.SHORT, vX);
							velocity.write(Type.SHORT, vY);
							velocity.write(Type.SHORT, vZ);
							try {
								velocity.send(Protocol1_8TO1_9.class, true, false);
							} catch (Exception ex) {ex.printStackTrace();}
						}
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(final PacketWrapper packetWrapper) throws Exception {
						final int entityID = packetWrapper.get(Type.VAR_INT, 0);
						final int typeID = packetWrapper.get(Type.BYTE, 0);
						if (typeID==3 || typeID==67 || typeID==91 || typeID==92 || typeID==93) {
							packetWrapper.cancel();
							return;
						}
						final EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						final Entity1_10Types.EntityType type = Entity1_10Types.getTypeFromId(typeID, true);
						tracker.getClientEntityTypes().put(entityID, type);
						tracker.sendMetadataBuffer(entityID);
					}
				});
			}
		});

		//Spawn Experience Orb
		this.registerOutgoing(State.PLAY, 0x01, 0x11, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.SHORT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int entityID = packetWrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.getClientEntityTypes().put(entityID, Entity1_10Types.EntityType.EXPERIENCE_ORB);
						tracker.sendMetadataBuffer(entityID);
					}
				});
			}
		});

		//Spawn Global Entity
		this.registerOutgoing(State.PLAY, 0x02, 0x2C, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int entityID = packetWrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.getClientEntityTypes().put(entityID, Entity1_10Types.EntityType.LIGHTNING);
						tracker.sendMetadataBuffer(entityID);
					}
				});
			}
		});

		//Spawn Mob
		this.registerOutgoing(State.PLAY, 0x03, 0x0F, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.UUID);
					}
				});
				map(Type.UNSIGNED_BYTE);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int entityID = packetWrapper.get(Type.VAR_INT, 0);
						int typeID = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
						if (typeID==69) {
							packetWrapper.cancel();
							return;
						}
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.getClientEntityTypes().put(entityID, Entity1_10Types.getTypeFromId(typeID, false));
						tracker.sendMetadataBuffer(entityID);
					}
				});
				handler(new PacketHandler() {
					public void handle(PacketWrapper wrapper) throws Exception {
						List<Metadata> metadataList = wrapper.get(Types1_8.METADATA_LIST, 0);
						int entityID = wrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = wrapper.user().get(EntityTracker.class);
						if (tracker.getClientEntityTypes().containsKey(entityID)) {
							MetadataRewriter.transform(tracker.getClientEntityTypes().get(entityID), metadataList);
						} else {
							wrapper.cancel();
						}
					}
				});
			}
		});

		//Spawn Painting
		this.registerOutgoing(State.PLAY, 0x04, 0x10, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.UUID);
					}
				});
				map(Type.STRING);
				map(Type.POSITION);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int entityID = packetWrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.getClientEntityTypes().put(entityID, Entity1_10Types.EntityType.PAINTING);
						tracker.sendMetadataBuffer(entityID);
					}
				});
			}
		});

		//Spawn Player
		this.registerOutgoing(State.PLAY, 0x05, 0x0C, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.BYTE);
				map(Type.BYTE);
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.SHORT, (short)0);
					}
				});
				map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
				this.handler(new PacketHandler() {
					public void handle(PacketWrapper wrapper) throws Exception {
						List<Metadata> metadataList = wrapper.get(Types1_8.METADATA_LIST, 0);
						MetadataRewriter.transform(Entity1_10Types.EntityType.PLAYER, metadataList);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int entityID = packetWrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.getClientEntityTypes().put(entityID, Entity1_10Types.EntityType.PLAYER);
						tracker.sendMetadataBuffer(entityID);
					}
				});
			}
		});

		//Animation
		this.registerOutgoing(State.PLAY, 0x06, 0x0B);

		//Statistics
		this.registerOutgoing(State.PLAY, 0x07, 0x37);

		//Block Break Animation
		this.registerOutgoing(State.PLAY, 0x08, 0x25);

		//Update Block Entity
		this.registerOutgoing(State.PLAY, 0x09, 0x35, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				map(Type.UNSIGNED_BYTE);
				map(Type.NBT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						CompoundTag tag = packetWrapper.get(Type.NBT, 0);
						if (tag != null) {
							if (tag.contains("SpawnData")) {
								String entity = (String) ((CompoundTag)tag.get("SpawnData")).get("id").getValue();
								tag.remove("SpawnData");
								tag.put(new StringTag("EntityId", entity));
							}
						}
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						if (packetWrapper.get(Type.UNSIGNED_BYTE, 0)==2) return;
						CompoundTag nbt = packetWrapper.get(Type.NBT, 0);
						Utils.iterateCompountTagRecursive(nbt, tag -> {
							if (tag instanceof StringTag) {
								String value = (String) tag.getValue();
								value = ChatUtil.jsonToLegacy(value);
								value = ChatUtil.removeUnusedColor(value);
								((StringTag) tag).setValue(value);
							}
						});
					}
				});
			}
		});

		//Block Action
		this.registerOutgoing(State.PLAY, 0x0A, 0x24);

		//Block Change
		this.registerOutgoing(State.PLAY, 0x0B, 0x23, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int combined = packetWrapper.get(Type.VAR_INT, 0);
						BlockStorage.BlockState state = BlockStorage.rawToState(combined);
						state = ItemReplacement.replaceBlock(state);
						packetWrapper.set(Type.VAR_INT, 0, BlockStorage.stateToRaw(state));
					}
				});
			}
		});

		//Boss Bar
		this.registerOutgoing(State.PLAY, 0x0C, -1, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.cancel();
						UUID uuid = packetWrapper.read(Type.UUID);
						int action = packetWrapper.read(Type.VAR_INT);
						BossBarStorage bossBarStorage = packetWrapper.user().get(BossBarStorage.class);
						if (action==0) {
							bossBarStorage.add(uuid, ChatUtil.jsonToLegacy(packetWrapper.read(Type.STRING)), packetWrapper.read(Type.FLOAT));
							packetWrapper.read(Type.VAR_INT);
							packetWrapper.read(Type.VAR_INT);
							packetWrapper.read(Type.UNSIGNED_BYTE);
						} else if (action==1) {
							bossBarStorage.remove(uuid);
						} else if (action==2) {
							bossBarStorage.updateHealth(uuid, packetWrapper.read(Type.FLOAT));
						} else if (action==3) {
							String title = packetWrapper.read(Type.STRING);
							title = ChatUtil.jsonToLegacy(title);
							bossBarStorage.updateTitle(uuid, title);
						}
					}
				});
			}
		});

		//Server Difficulty
		this.registerOutgoing(State.PLAY, 0x0D, 0x41);

		//Tab-Complete
		this.registerOutgoing(State.PLAY, 0x0E, 0x3A);

		//Chat Message
		this.registerOutgoing(State.PLAY, 0x0F, 0x02);

		//Multi Block Change
		this.registerOutgoing(State.PLAY, 0x10, 0x22);

		//Confirm Transaction
		this.registerOutgoing(State.PLAY, 0x11, 0x32);

		//Close Window
		this.registerOutgoing(State.PLAY, 0x12, 0x2E);

		//Open Window
		this.registerOutgoing(State.PLAY, 0x13, 0x2D, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.UNSIGNED_BYTE);
				map(Type.STRING);
				map(Type.STRING);
				map(Type.UNSIGNED_BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						String type = packetWrapper.get(Type.STRING, 0);
						if (type.equals("EntityHorse")) packetWrapper.passthrough(Type.INT);
					}
				});
			}
		});

		//Window Items
		this.registerOutgoing(State.PLAY, 0x14, 0x30, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.UNSIGNED_BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						short windowId = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
						Item[] items = packetWrapper.read(Type.ITEM_ARRAY);
						for (int i = 0; i<items.length; i++) {
							items[i] = ItemRewriter.toClient(items[i]);
						}
						if (windowId==0 && items.length==46) {
							Item[] old = items;
							items = new Item[45];
							System.arraycopy(old, 0, items, 0, 45);
						}
						packetWrapper.write(Type.ITEM_ARRAY, items);
					}
				});
			}
		});

		//Window Property
		this.registerOutgoing(State.PLAY, 0x15, 0x31);

		//Set Slot
		this.registerOutgoing(State.PLAY, 0x16, 0x2F, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.BYTE);
				map(Type.SHORT);
				map(Type.ITEM);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.set(Type.ITEM, 0, ItemRewriter.toClient(packetWrapper.get(Type.ITEM, 0)));
						byte windowId = packetWrapper.get(Type.BYTE, 0);
						short slot = packetWrapper.get(Type.SHORT, 0);
						if (windowId==0 && slot==45) packetWrapper.cancel();
					}
				});
			}
		});

		//Set Cooldown
		this.registerOutgoing(State.PLAY, 0x17, -1, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.cancel();
					}
				});
			}
		});

		//Plugin Message
		this.registerOutgoing(State.PLAY, 0x18, 0x3F);

		//Named Sound Effect
		this.registerOutgoing(State.PLAY, 0x19, 0x29, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						String name = packetWrapper.get(Type.STRING, 0);
						name = SoundRemapper.getOldName(name);
						if (name==null) packetWrapper.cancel();
						else packetWrapper.set(Type.STRING, 0, name);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.VAR_INT);
					}
				});
				map(Type.INT);
				map(Type.INT);
				map(Type.INT);
				map(Type.FLOAT);
				map(Type.UNSIGNED_BYTE);
			}
		});

		//Disconnect
		this.registerOutgoing(State.PLAY, 0x1A, 0x40);

		//Entity Status
		this.registerOutgoing(State.PLAY, 0x1B, 0x1A, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						byte status = packetWrapper.read(Type.BYTE);
						if (status>23) {
							packetWrapper.cancel();
							return;
						}
						packetWrapper.write(Type.BYTE, status);
					}
				});
			}
		});

		//Explosion
		this.registerOutgoing(State.PLAY, 0x1C, 0x27, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.FLOAT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int count = packetWrapper.read(Type.INT);
						packetWrapper.write(Type.INT, count);
						for (int i = 0; i<count; i++) {
							packetWrapper.passthrough(Type.UNSIGNED_BYTE);
							packetWrapper.passthrough(Type.UNSIGNED_BYTE);
							packetWrapper.passthrough(Type.UNSIGNED_BYTE);
						}
					}
				});
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.FLOAT);
			}
		});

		//Unload Chunk
		this.registerOutgoing(State.PLAY, 0x1D, 0x21, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.INT);
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.BOOLEAN, true);
						packetWrapper.write(Type.UNSIGNED_SHORT, 0);
						packetWrapper.write(Type.VAR_INT, 0);
					}
				});
			}
		});

		//Change Game State
		this.registerOutgoing(State.PLAY, 0x1E, 0x2B, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.UNSIGNED_BYTE);
				map(Type.FLOAT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int reason = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
						if (reason==3)
							packetWrapper.user().get(EntityTracker.class).setPlayerGamemode(packetWrapper.get(Type.FLOAT, 0).intValue());
					}
				});
			}
		});

		//Keep Alive
		this.registerOutgoing(State.PLAY, 0x1F, 0x00);

		//Chunk Data
		this.registerOutgoing(State.PLAY, 0x20, 0x21, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						ChunkPacketTransformer.transformChunk(packetWrapper);
					}
				});
			}
		});

		//Effect
		this.registerOutgoing(State.PLAY, 0x21, 0x28);

		//Particle
		this.registerOutgoing(State.PLAY, 0x22, 0x2A, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int type = packetWrapper.get(Type.INT, 0);
						if (type>41) packetWrapper.cancel();
					}
				});
			}
		});

		//Join Game
		this.registerOutgoing(State.PLAY, 0x23, 0x01, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.UNSIGNED_BYTE);
				map(Type.BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.STRING);
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						tracker.setPlayerId(packetWrapper.get(Type.INT, 0));
						tracker.setPlayerGamemode(packetWrapper.get(Type.UNSIGNED_BYTE, 0));
						}
				});
			}
		});

		//Map
		this.registerOutgoing(State.PLAY, 0x24, 0x34, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.BOOLEAN);
					}
				});
			}
		});

		//Entity Relative Move
		this.registerOutgoing(State.PLAY, 0x25, 0x15, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						//devide into two packets because Short.MAX_VALUE / 128 = 2 * Byte.MAX_VALUE
						short relX = packetWrapper.read(Type.SHORT);
						short relY = packetWrapper.read(Type.SHORT);
						short relZ = packetWrapper.read(Type.SHORT);

						byte relX1 = (byte)(relX / 256);
						byte relX2 = (byte)((relX - relX1 * 128) / 128);
						byte relY1 = (byte)(relY / 256);
						byte relY2 = (byte)((relY - relY1 * 128) / 128);
						byte relZ1 = (byte)(relZ / 256);
						byte relZ2 = (byte)((relZ - relZ1 * 128) / 128);

						packetWrapper.write(Type.BYTE, relX1);
						packetWrapper.write(Type.BYTE, relY1);
						packetWrapper.write(Type.BYTE, relZ1);

						boolean onGround = packetWrapper.passthrough(Type.BOOLEAN);

						PacketWrapper secondPacket = new PacketWrapper(0x15, null, packetWrapper.user());
						secondPacket.write(Type.VAR_INT, packetWrapper.get(Type.VAR_INT, 0));
						secondPacket.write(Type.BYTE, relX2);
						secondPacket.write(Type.BYTE, relY2);
						secondPacket.write(Type.BYTE, relZ2);
						secondPacket.write(Type.BOOLEAN, onGround);

						secondPacket.send(Protocol1_8TO1_9.class);
					}
				});
			}
		});

		//Entity Relative Move And Look
		this.registerOutgoing(State.PLAY, 0x26, 0x17, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						//devide into two packets because Short.MAX_VALUE / 128 = 2 * Byte.MAX_VALUE
						short relX = packetWrapper.read(Type.SHORT);
						short relY = packetWrapper.read(Type.SHORT);
						short relZ = packetWrapper.read(Type.SHORT);

						byte relX1 = (byte)(relX / 256);
						byte relX2 = (byte)((relX - relX1 * 128) / 128);
						byte relY1 = (byte)(relY / 256);
						byte relY2 = (byte)((relY - relY1 * 128) / 128);
						byte relZ1 = (byte)(relZ / 256);
						byte relZ2 = (byte)((relZ - relZ1 * 128) / 128);

						packetWrapper.write(Type.BYTE, relX1);
						packetWrapper.write(Type.BYTE, relY1);
						packetWrapper.write(Type.BYTE, relZ1);

						byte yaw = packetWrapper.passthrough(Type.BYTE);
						byte pitch = packetWrapper.passthrough(Type.BYTE);
						boolean onGround = packetWrapper.passthrough(Type.BOOLEAN);

						PacketWrapper secondPacket = new PacketWrapper(0x17, null, packetWrapper.user());
						secondPacket.write(Type.VAR_INT, packetWrapper.get(Type.VAR_INT, 0));
						secondPacket.write(Type.BYTE, relX2);
						secondPacket.write(Type.BYTE, relY2);
						secondPacket.write(Type.BYTE, relZ2);
						secondPacket.write(Type.BYTE, yaw);
						secondPacket.write(Type.BYTE, pitch);
						secondPacket.write(Type.BOOLEAN, onGround);

						secondPacket.send(Protocol1_8TO1_9.class);
					}
				});
			}
		});

		//Entity Look
		this.registerOutgoing(State.PLAY, 0x27, 0x16, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.BOOLEAN);
			}
		});

		//Entity
		this.registerOutgoing(State.PLAY, 0x28, 0x14);

		//Vehicle Move -> Entity Teleport
		this.registerOutgoing(State.PLAY, 0x29, 0x18, new PacketRemapper() {
			@Override
			public void registerMap() {
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						int vehicle = tracker.getVehicle(tracker.getPlayerId());
						if (vehicle==-1) packetWrapper.cancel();
						packetWrapper.write(Type.VAR_INT, vehicle);
					}
				});
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.FLOAT, degreesToAngle);
				map(Type.FLOAT, degreesToAngle);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						if (packetWrapper.isCancelled()) return;
						PlayerPosition position = packetWrapper.user().get(PlayerPosition.class);
						double x = packetWrapper.get(Type.INT, 0) / 32d;
						double y = packetWrapper.get(Type.INT, 1) / 32d;
						double z = packetWrapper.get(Type.INT, 2) / 32d;
						position.setPos(x, y, z);
					}
				});
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.BOOLEAN, true);
					}
				});
			}
		});

		//Open Sign Editor
		this.registerOutgoing(State.PLAY, 0x2A, 0x36);

		//Player Abilities
		this.registerOutgoing(State.PLAY, 0x2B, 0x39);

		//Combat Event
		this.registerOutgoing(State.PLAY, 0x2C, 0x42);

		//Player List Item
		this.registerOutgoing(State.PLAY, 0x2D, 0x38);

		//Player Position And Look
		this.registerOutgoing(State.PLAY, 0x2E, 0x08, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int id = packetWrapper.read(Type.VAR_INT);
						PacketWrapper confirm = new PacketWrapper(0x00, null, packetWrapper.user());

						confirm.write(Type.VAR_INT, id);

						PacketUtil.sendToServer(confirm, Protocol1_8TO1_9.class, true, true);
					}
				});
			}
		});

		//Use Bed
		this.registerOutgoing(State.PLAY, 0x2F, 0x0A);

		//Destroy Entities
		this.registerOutgoing(State.PLAY, 0x30, 0x13, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT_ARRAY);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						for (int entityId : packetWrapper.get(Type.VAR_INT_ARRAY, 0)) tracker.removeEntity(entityId);
					}
				});
			}
		});

		//Remove Entity Effect
		this.registerOutgoing(State.PLAY, 0x31, 0x1E, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int id = packetWrapper.get(Type.BYTE, 0);
						if (id>23) packetWrapper.cancel();
						if (id==25) {
							Levitation levitation = packetWrapper.user().get(Levitation.class);
							levitation.setActive(false);
						}
					}
				});
			}
		});

		//Resource Pack Send
		this.registerOutgoing(State.PLAY, 0x32, 0x48);

		//Respawn
		this.registerOutgoing(State.PLAY, 0x33, 0x07, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.UNSIGNED_BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.STRING);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(EntityTracker.class).setPlayerGamemode(packetWrapper.get(Type.UNSIGNED_BYTE, 1));
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
						packetWrapper.user().get(BossBarStorage.class).changeWorld();
					}
				});
			}
		});

		//Entity Head Look
		this.registerOutgoing(State.PLAY, 0x34, 0x19);

		//World Border
		this.registerOutgoing(State.PLAY, 0x35, 0x44);

		//Camera
		this.registerOutgoing(State.PLAY, 0x36, 0x43);

		//Held Item Change
		this.registerOutgoing(State.PLAY, 0x37, 0x09);

		//Display Scoreboard
		this.registerOutgoing(State.PLAY, 0x38, 0x3D);

		//Entity Metadata
		this.registerOutgoing(State.PLAY, 0x39, 0x1C, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
				handler(new PacketHandler() {
					public void handle(PacketWrapper wrapper) throws Exception {
						List<Metadata> metadataList = wrapper.get(Types1_8.METADATA_LIST, 0);
						int entityID = wrapper.get(Type.VAR_INT, 0);
						EntityTracker tracker = wrapper.user().get(EntityTracker.class);
						if (tracker.getClientEntityTypes().containsKey(entityID)) {
							MetadataRewriter.transform(tracker.getClientEntityTypes().get(entityID), metadataList);
							if (metadataList.isEmpty()) wrapper.cancel();
						} else {
							tracker.addMetadataToBuffer(entityID, metadataList);
							wrapper.cancel();
						}
					}
				});
			}
		});

		//Attach Entity
		this.registerOutgoing(State.PLAY, 0x3A, 0x1B, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.INT);
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.BOOLEAN, true);
					}
				});
			}
		});

		//Entity Velocity
		this.registerOutgoing(State.PLAY, 0x3B, 0x12);

		//Entity Equipment
		this.registerOutgoing(State.PLAY, 0x3C, 0x04, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int slot = packetWrapper.read(Type.VAR_INT);
						if (slot==1) {
							packetWrapper.cancel();
						} else if (slot>1) {
							slot -=1 ;
						}
						packetWrapper.write(Type.SHORT, (short)slot);
					}
				});
				map(Type.ITEM);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.set(Type.ITEM, 0, ItemRewriter.toClient(packetWrapper.get(Type.ITEM, 0)));
					}
				});
			}
		});

		//Set Experience
		this.registerOutgoing(State.PLAY, 0x3D, 0x1F);

		//Update Health
		this.registerOutgoing(State.PLAY, 0x3E, 0x06);

		//Scoreboard Objective
		this.registerOutgoing(State.PLAY, 0x3F, 0x3B, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int mode = packetWrapper.get(Type.BYTE, 0);
						if (mode==0 || mode==2) {
							packetWrapper.passthrough(Type.STRING);
							packetWrapper.passthrough(Type.STRING);
						}
					}
				});
			}
		});

		//Set Passengers
		this.registerOutgoing(State.PLAY, 0x40, 0x1B, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.cancel();
						EntityTracker entityTracker = packetWrapper.user().get(EntityTracker.class);
						int vehicle = packetWrapper.read(Type.VAR_INT);
						int count = packetWrapper.read(Type.VAR_INT);
						ArrayList<Integer> passengers = new ArrayList<>();
						for (int i = 0; i<count; i++) passengers.add(packetWrapper.read(Type.VAR_INT));
						ArrayList<Integer> oldPassengers = entityTracker.getPassengers(vehicle);
						entityTracker.setPassengers(vehicle, passengers);
						if (!oldPassengers.isEmpty()) {
							for (Integer passenger : oldPassengers) {
								PacketWrapper detach = new PacketWrapper(0x1B, null, packetWrapper.user());
								detach.write(Type.INT, passenger);
								detach.write(Type.INT, -1);
								detach.write(Type.BOOLEAN, false);
								try {
									detach.send(Protocol1_8TO1_9.class, true, false);
								} catch (Exception ex) {ex.printStackTrace();}
							}
						}
						for (int i = 0; i<count; i++) {
							int v = i==0 ? vehicle : passengers.get(i-1);
							int p = passengers.get(i);
							PacketWrapper attach = new PacketWrapper(0x1B, null, packetWrapper.user());
							attach.write(Type.INT, p);
							attach.write(Type.INT, v);
							attach.write(Type.BOOLEAN, false);
							try {
								attach.send(Protocol1_8TO1_9.class, true, false);
							} catch (Exception ex) {ex.printStackTrace();}
						}
					}
				});
			}
		});

		//Scoreboard Team
		this.registerOutgoing(State.PLAY, 0x41, 0x3E, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						byte mode = packetWrapper.get(Type.BYTE, 0);
						if (mode==0 || mode==2) {
							packetWrapper.passthrough(Type.STRING);  //Display Name
							packetWrapper.passthrough(Type.STRING);  //Prefix
							packetWrapper.passthrough(Type.STRING);  //Suffix
							packetWrapper.passthrough(Type.BYTE);  //Friendly Flags
							packetWrapper.passthrough(Type.STRING);  //Name Tag Visibility
							packetWrapper.read(Type.STRING);  //Skip Collision Rule
							packetWrapper.passthrough(Type.BYTE);  //Friendly Flags
						}

						if (mode==0 || mode==3 || mode==4) {
							int size = packetWrapper.read(Type.VAR_INT);
							packetWrapper.write(Type.VAR_INT, size);
							for (int i = 0; i<size; i++) {
								packetWrapper.passthrough(Type.STRING);
							}
						}
					}
				});
			}
		});

		//Update Score
		this.registerOutgoing(State.PLAY, 0x42, 0x3C);

		//Spawn Position
		this.registerOutgoing(State.PLAY, 0x43, 0x05, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						Position position = packetWrapper.get(Type.POSITION, 0);
						PlayerPosition playerPosition = packetWrapper.user().get(PlayerPosition.class);
						playerPosition.setPos(position.getX(), position.getY(), position.getZ());
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
					}
				});
			}
		});

		//Update Time
		this.registerOutgoing(State.PLAY, 0x44, 0x03);

		//Title
		this.registerOutgoing(State.PLAY, 0x45, 0x45);

		//Update Sign
		this.registerOutgoing(State.PLAY, 0x46, 0x33, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						for (int i = 0; i<4; i++) {
							String line = packetWrapper.read(Type.STRING);
							line = ChatUtil.jsonToLegacy(line);
							line = ChatUtil.removeUnusedColor(line);
							if (line.length()>15) {
								line = ChatColor.stripColor(line);
								if (line.length()>15) line = line.substring(0, 15);
							}
							packetWrapper.write(Type.STRING, line);
						}
					}
				});
			}
		});

		//Sound Effects
		this.registerOutgoing(State.PLAY, 0x47, 0x29, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int soundId = packetWrapper.read(Type.VAR_INT);
						String sound = SoundRemapper.oldNameFromId(soundId);
						if (sound==null) packetWrapper.cancel();
						else packetWrapper.write(Type.STRING, sound);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.VAR_INT);
					}
				});
				map(Type.INT);
				map(Type.INT);
				map(Type.INT);
				map(Type.FLOAT);
				map(Type.UNSIGNED_BYTE);
			}
		});

		//Player List Header And Footer
		this.registerOutgoing(State.PLAY, 0x48, 0x47);

		//Collect Item
		this.registerOutgoing(State.PLAY, 0x49, 0x0D);

		//Entity Teleport
		this.registerOutgoing(State.PLAY, 0x4A, 0x18, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.DOUBLE, toOldInt);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.BOOLEAN);
			}
		});

		//Entity Properties
		this.registerOutgoing(State.PLAY, 0x4B, 0x20, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int size = packetWrapper.get(Type.INT, 0);
						int removed = 0;
						for (int i = 0; i<size; i++) {
							String key = packetWrapper.read(Type.STRING);
							boolean skip = key.equals("generic.armor") || key.equals("generic.attackSpeed") || key.equals("generic.luck") || key.equals("generic.armorToughness");
							double value = packetWrapper.read(Type.DOUBLE);
							int modifiersize = packetWrapper.read(Type.VAR_INT);
							if (!skip) {
								packetWrapper.write(Type.STRING, key);
								packetWrapper.write(Type.DOUBLE, value);
								packetWrapper.write(Type.VAR_INT, modifiersize);
							} else removed++;
							ArrayList<Pair<Byte, Double>> modifiers = new ArrayList<>();
							for (int j = 0; j<modifiersize; j++) {
								UUID uuid = packetWrapper.read(Type.UUID);
								double amount = packetWrapper.read(Type.DOUBLE);
								byte operation = packetWrapper.read(Type.BYTE);
								modifiers.add(new Pair<>(operation, amount));
								if (skip) continue;
								packetWrapper.write(Type.UUID, uuid);
								packetWrapper.write(Type.DOUBLE, amount);
								packetWrapper.write(Type.BYTE, operation);
							}
							if (key.equals("generic.attackSpeed")) {
								packetWrapper.user().get(Cooldown.class).setAttackSpeed(value, modifiers);
							}
						}
						packetWrapper.set(Type.INT, 0, size-removed);
					}
				});
			}
		});

		//Entity Effect
		this.registerOutgoing(State.PLAY, 0x4C, 0x1D, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.VAR_INT);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int id = packetWrapper.get(Type.BYTE, 0);
						if (id>23) packetWrapper.cancel();
						if (id==25) {
							Levitation levitation = packetWrapper.user().get(Levitation.class);
							levitation.setActive(true);
							levitation.setAmplifier(packetWrapper.get(Type.BYTE, 1));
						}
					}
				});
			}
		});

		//Keep Alive
		this.registerIncoming(State.PLAY, 0x0B, 0x00);

		//Chat Message
		this.registerIncoming(State.PLAY, 0x02, 0x01);

		//Use Entity
		this.registerIncoming(State.PLAY, 0x0A, 0x02, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int type = packetWrapper.get(Type.VAR_INT, 1);
						if (type==2) {
							packetWrapper.passthrough(Type.FLOAT);
							packetWrapper.passthrough(Type.FLOAT);
							packetWrapper.passthrough(Type.FLOAT);
						}
						if (type==2 || type==0) {
							packetWrapper.write(Type.VAR_INT, 0);
						}
					}
				});
			}
		});

		//Player
		this.registerIncoming(State.PLAY, 0x0F, 0x03, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						int playerId = tracker.getPlayerId();
						if (tracker.isInsideVehicle(playerId)) packetWrapper.cancel();
					}
				});
			}
		});

		//Player Position
		this.registerIncoming(State.PLAY, 0x0C, 0x04, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
						pos.setPos(packetWrapper.get(Type.DOUBLE, 0), packetWrapper.get(Type.DOUBLE, 1), packetWrapper.get(Type.DOUBLE, 2));
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
					}
				});
			}
		});

		//Player Look
		this.registerIncoming(State.PLAY, 0x0E, 0x05, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
						pos.setYaw(packetWrapper.get(Type.FLOAT, 0));
						pos.setPitch(packetWrapper.get(Type.FLOAT, 1));
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
					}
				});
			}
		});

		//Player Position And Look
		this.registerIncoming(State.PLAY, 0x0D, 0x06, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
						pos.setPos(packetWrapper.get(Type.DOUBLE, 0), packetWrapper.get(Type.DOUBLE, 1), packetWrapper.get(Type.DOUBLE, 2));
						pos.setYaw(packetWrapper.get(Type.FLOAT, 0));
						pos.setPitch(packetWrapper.get(Type.FLOAT, 1));
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
					}
				});
			}
		});

		//Player Digging
		this.registerIncoming(State.PLAY, 0x13, 0x07, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.BYTE, Type.VAR_INT);
				map(Type.POSITION);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int state = packetWrapper.get(Type.BYTE, 0);
						if (state==0) packetWrapper.user().get(BlockPlaceDestroyTracker.class).setMining(true);
						else if (state==2) packetWrapper.user().get(BlockPlaceDestroyTracker.class).setMining(false);
						else if (state==1) {
							packetWrapper.user().get(BlockPlaceDestroyTracker.class).setMining(false);
							packetWrapper.user().get(Cooldown.class).setLastHit(System.currentTimeMillis());
						}
					}
				});
			}
		});

		//Player Block Placement
		this.registerIncoming(State.PLAY, 0x1C, 0x08, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				map(Type.BYTE, Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.read(Type.ITEM);
					}
				});
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.VAR_INT, 0);  //Main Hand
					}
				});
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						if (packetWrapper.get(Type.VAR_INT, 0)==-1) {
							packetWrapper.cancel();
							PacketWrapper useItem = new PacketWrapper(0x1D, null, packetWrapper.user());
							useItem.write(Type.VAR_INT, 0);

							PacketUtil.sendToServer(useItem, Protocol1_8TO1_9.class, true, true);
						}
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						if (packetWrapper.get(Type.VAR_INT, 0)!=-1) {
							packetWrapper.user().get(BlockPlaceDestroyTracker.class).place();
						}
					}
				});
			}
		});

		//Held Item Change
		this.registerIncoming(State.PLAY, 0x17, 0x09, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(Cooldown.class).hit();
					}
				});
			}
		});

		//Animation
		this.registerIncoming(State.PLAY, 0x1A, 0x0A, new PacketRemapper() {
			@Override
			public void registerMap() {
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.cancel();
						final PacketWrapper delayedPacket = new PacketWrapper(0x1A, null, packetWrapper.user());
						delayedPacket.write(Type.VAR_INT, 0);  //Main Hand
						//delay packet in order to deal damage to entites
						//the cooldown value gets reset by this packet
						//1.8 sends it before the use entity packet
						//1.9 afterwards
						PacketUtil.sendToServer(delayedPacket, Protocol1_8TO1_9.class, true, false);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.user().get(BlockPlaceDestroyTracker.class).updateMinig();
						packetWrapper.user().get(Cooldown.class).hit();
					}
				});
			}
		});

		//Entity Action
		this.registerIncoming(State.PLAY, 0x14, 0x0B, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						int action = packetWrapper.get(Type.VAR_INT, 1);
						if (action==6) {
							packetWrapper.set(Type.VAR_INT, 1, 7);
						}
					}
				});
			}
		});

		//Steer Vehicle
		this.registerIncoming(State.PLAY, 0x15, 0x0C, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.UNSIGNED_BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						int playerId = tracker.getPlayerId();
						int vehicle = tracker.getVehicle(playerId);
						if (vehicle!=-1 && tracker.getClientEntityTypes().getOrDefault(vehicle, Entity1_10Types.EntityType.SLIME)==Entity1_10Types.EntityType.BOAT) {
							PacketWrapper steerBoat = new PacketWrapper(0x11, null, packetWrapper.user());
							float left = packetWrapper.get(Type.FLOAT, 0);
							float forward = packetWrapper.get(Type.FLOAT, 1);
							steerBoat.write(Type.BOOLEAN, forward!=0.0f || left<0.0f);
							steerBoat.write(Type.BOOLEAN, forward!=0.0f || left>0.0f);
							PacketUtil.sendToServer(steerBoat, Protocol1_8TO1_9.class, true, true);
						}
					}
				});
			}
		});

		//Close Window
		this.registerIncoming(State.PLAY, 0x08, 0x0D);

		//Click Window
		this.registerIncoming(State.PLAY, 0x07, 0x0E, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.UNSIGNED_BYTE);
				map(Type.SHORT);
				map(Type.BYTE);
				map(Type.SHORT);
				map(Type.BYTE, Type.VAR_INT);
				map(Type.ITEM);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.set(Type.ITEM, 0, ItemRewriter.toServer(packetWrapper.get(Type.ITEM, 0)));
					}
				});
			}
		});

		//Confirm Transaction
		this.registerIncoming(State.PLAY, 0x05, 0x0F);

		//Creative Inventory Action
		this.registerIncoming(State.PLAY, 0x18, 0x10, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.SHORT);
				map(Type.ITEM);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.set(Type.ITEM, 0, ItemRewriter.toServer(packetWrapper.get(Type.ITEM, 0)));
					}
				});
			}
		});

		//Enchant Item
		this.registerIncoming(State.PLAY, 0x06, 0x11);

		//Update Sign
		this.registerIncoming(State.PLAY, 0x19, 0x12);

		//Player Abilities
		this.registerIncoming(State.PLAY, 0x12, 0x13);

		//Tab Complete
		this.registerIncoming(State.PLAY, 0x01, 0x14, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.BOOLEAN, false);
					}
				});
				map(Type.BOOLEAN);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper packetWrapper) throws Exception {
						boolean hasPosition = packetWrapper.get(Type.BOOLEAN, 1);
						if (hasPosition) packetWrapper.passthrough(Type.POSITION);
					}
				});
			}
		});

		//Client Settings
		this.registerIncoming(State.PLAY, 0x04, 0x15, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				map(Type.BYTE, Type.VAR_INT);
				map(Type.BOOLEAN);
				map(Type.UNSIGNED_BYTE);
				create(new ValueCreator() {
					@Override
					public void write(PacketWrapper packetWrapper) throws Exception {
						packetWrapper.write(Type.VAR_INT, 1);
					}
				});
			}
		});

		//Client Status
		this.registerIncoming(State.PLAY, 0x03, 0x16);

		//Plugin Message
		this.registerIncoming(State.PLAY, 0x09, 0x17);

		//Spectate
		this.registerIncoming(State.PLAY, 0x1B, 0x18);

		//Resource Pack Status
		this.registerIncoming(State.PLAY, 0x16, 0x19);
	}

	@Override
	public void init(UserConnection userConnection) {
		userConnection.put(new EntityTracker(userConnection));
		userConnection.put(new Levitation(userConnection));
		userConnection.put(new PlayerPosition(userConnection));
		userConnection.put(new Cooldown(userConnection));
		userConnection.put(new BlockPlaceDestroyTracker(userConnection));
		userConnection.put(new BossBarStorage(userConnection));
	}
}
