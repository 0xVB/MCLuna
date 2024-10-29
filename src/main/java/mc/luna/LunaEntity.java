package mc.luna;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class LunaEntity extends LuaValue
{
    private final Entity entity;
    private final BlockEntity blockEntity;
    private final LuaValue luaProxy;
    public final LunaExecutor Executor;

    // Private constructor to prevent direct instantiation. Use LunaEntity.Fetch instead.
    private LunaEntity(Entity entity, LunaExecutor Exec)
    {
        this.entity = entity;
        this.blockEntity = null;
        this.luaProxy = LuaValue.userdataOf(this, __mt);
        this.Executor = Exec;
    }
    private LunaEntity(BlockEntity blockEntity, LunaExecutor Exec)
    {
        this.blockEntity = blockEntity;
        this.entity = null;
        this.luaProxy = LuaValue.userdataOf(this, __mt);
        this.Executor = Exec;
    }
    public boolean isBlockEntity() { return blockEntity != null; }
    public boolean isMobEntity() { return entity != null; }
    public Vec3d getEntityPos()
    {
        if (isMobEntity()) return entity.getPos();
        if (blockEntity == null) return new Vec3d(0, 0, 0);
        return blockEntity.getPos().toCenterPos();
    }
    public String getEntityName()
    {
        if (isMobEntity()) return entity.getName().toString();
        if (blockEntity == null) return "[DESTROYED_ENTITY]";
        return blockEntity.getType().toString();
    }
    public String getEntityType()
    {
        if (isMobEntity()) return entity.getType().toString();
        if (blockEntity == null) return "[DESTROYED_ENTITY]";
        return blockEntity.getType().toString();
    }
    @FunctionalInterface
    private interface idxJavaFn { LuaValue apply(LunaEntity self, String key); }
    @FunctionalInterface
    private interface ndxJavaFn { LuaValue apply(LunaEntity self, String key, LuaValue value); }
    private static class LunaField
    {
        protected idxJavaFn __idx;
        protected ndxJavaFn __ndx;
        protected String _fname;
        LunaField(String fieldName, idxJavaFn idx, ndxJavaFn ndx)
        {
            __idx = idx;
            __ndx = ndx;
            _fname = fieldName;

            Fields.put(fieldName, this);
        }
    }
    private static class LunaMethod extends LunaField
    {
        private LuaFunction _methodFn;
        private static ndxJavaFn _readOnlyNdx = new ndxJavaFn()
        {
            @Override
            public LuaValue apply(LunaEntity self, String key, LuaValue value)
            { return LuaValue.error("Methods are read-only."); }
        };

        protected idxJavaFn _readOnlyIdx = new idxJavaFn()
        {
            @Override
            public LuaValue apply(LunaEntity self, String key) { return _methodFn; }
        };

        LunaMethod(String MethodName, LuaFunction MethodFunction)
        {
            super(MethodName, null, _readOnlyNdx);
            _methodFn = MethodFunction;
            __idx = _readOnlyIdx;
        }
    }

    private static class ReadOnlyField extends LunaField
    {
        private ndxJavaFn _readOnlyNdx = new ndxJavaFn()
        {
            @Override
            public LuaValue apply(LunaEntity self, String key, LuaValue value)
            { return LuaValue.error(_fname + " is read-only."); }
        };

        ReadOnlyField(String fieldName, idxJavaFn idxFn)
        {
            super(fieldName, idxFn, null);
            __ndx = _readOnlyNdx;
        }
    }

    private static Map<String, LunaField> Fields = new HashMap<String, LunaField>();

    // Metatable
    private static LuaValue __mt = null;
    private static LuaFunction __index;
    private static LuaFunction __newindex;
    private static LuaFunction __tostring;

    // Metatable
    static
    {
        __mt = new LuaTable();
        __index = new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue key)
            {
                String k = key.tojstring();
                LunaEntity self = LuaToLuna(luaSelf);

                if (self.isBlockEntity())
                {
                    BlockEntity me = self.blockEntity;
                    if (me == null && k.equals("Destroyed")) return LuaValue.TRUE;
                    else if (me == null) return LuaValue.NIL;

                    if (k.equals("Destroyed"))
                        return LuaBoolean.valueOf(me.isRemoved());
                    else if (me.isRemoved() || self == null)
                        return LuaValue.NIL;

                    // Checks passed, attempt index
                    // Check for field getter
                    if (Fields.containsKey(k))
                        // Field exists, use custom getter
                        return Fields.get(k).__idx.apply(self, k);
                    else
                    {
                        // Field does not exist, use nbt
                        NbtCompound Values = me.createNbt(me.getWorld().getRegistryManager());

                        if (!Values.contains(k))
                            return LuaValue.NIL;
                        else
                            return NbtHelper.nbtToLua(Values.get(k));
                    }
                }

                Entity me = self.entity;

                if (me == null && k.equals("Destroyed")) return LuaValue.TRUE;
                else if (me == null) return LuaValue.NIL;
                
                if (k.equals("Destroyed"))
                    return LuaBoolean.valueOf(me.isRemoved());
                else if (me.isRemoved() || self == null)
                    return LuaValue.NIL;

                // Checks passed, attempt index
                // Check for field getter
                if (Fields.containsKey(k))
                    // Field exists, use custom getter
                    return Fields.get(k).__idx.apply(self, k);
                else
                {
                    // Field does not exist, use nbt
                    NbtCompound Values = new NbtCompound();
                    me.writeNbt(Values);

                    if (!Values.contains(k))
                        return LuaValue.NIL;
                    else
                        return NbtHelper.nbtToLua(Values.get(k));
                }
            }
        };
        __newindex = new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue key, LuaValue value) {
                String k = key.tojstring();
                LunaEntity self = LuaToLuna(luaSelf);

                if (self == null)
                    return LuaValue.error("Attempt to modify a removed or invalid entity.");

                if (self.isBlockEntity())
                {
                    BlockEntity me = self.blockEntity;

                    if (me == null || me.isRemoved())
                        return LuaValue.error("Attempt to modify a removed or invalid entity.");

                    // Checks passed, attempt setting
                    // Check for field setter
                    if (Fields.containsKey(k))
                        // Field exists, use custom setter
                        return Fields.get(k).__ndx.apply(self, k, value);
                    else
                    {
                        // Field does not exist, use nbt
                        NbtCompound nbtData = me.createNbt(me.getWorld().getRegistryManager());

                        // Convert Lua value to NBT and add to compound
                        NbtElement nbtValue = NbtHelper.luaToNbt(value);
                        nbtData.put(k, nbtValue);

                        // Write modified NBT back to entity
                        me.read(nbtData, me.getWorld().getRegistryManager());

                        // Mark blocks for update
                        me.markDirty();
                        self.getServerWorld().getChunkManager().markForUpdate(me.getPos());
                    }
                    return LuaValue.NIL;
                }

                Entity me = self.entity;

                if (me == null || me.isRemoved())
                    return LuaValue.error("Attempt to modify a removed or invalid entity.");

                // Checks passed, attempt setting
                // Check for field setter
                if (Fields.containsKey(k))
                    // Field exists, use custom setter
                    return Fields.get(k).__ndx.apply(self, k, value);
                else
                {
                    // Field does not exist, use nbt
                    NbtCompound nbtData = me.writeNbt(new NbtCompound());

                    // Convert Lua value to NBT and add to compound
                    NbtElement nbtValue = NbtHelper.luaToNbt(value);
                    nbtData.put(k, nbtValue);

                    // Write modified NBT back to entity
                    me.readNbt(nbtData);
                }
                return LuaValue.NIL;
            }
        };
        __tostring = new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) {
                LunaEntity self = LuaToLuna(luaSelf);

                if (self.isBlockEntity() && !self.blockEntity.isRemoved())
                        return LuaString.valueOf(self.blockEntity.getType().toString());
                else if (self.isBlockEntity())
                    return  LuaString.valueOf("[DESTROYED_ENTITY]");


                if (self != null && !self.entity.isRemoved())
                    return LuaString.valueOf(self.entity.getType().toString());
                else
                    return LuaString.valueOf("[DESTROYED_ENTITY]");
            }
        };

        __mt.set("__index", __index);
        __mt.set("__newindex", __newindex);
        __mt.set("__tostring", __tostring);
    }

    // Read-only Fields
    static
    {
        // String fields
        new ReadOnlyField("Name", (self, key) -> { return LuaString.valueOf(self.entity.getName().toString()); } );
        new ReadOnlyField("DisplayName", (self, key) -> { return LuaString.valueOf(self.entity.getDisplayName().toString()); } );
        new ReadOnlyField("Type", (self, key) -> { return LuaString.valueOf(self.entity.getType().toString()); } );
        new ReadOnlyField("UUID", (self, key) -> { return LuaString.valueOf(self.entity.getUuidAsString()); } );
        new ReadOnlyField("IsMob", (self, key) -> { return LuaBoolean.valueOf(self.isMobEntity()); } );
        new ReadOnlyField("IsBlock", (self, key) -> { return LuaBoolean.valueOf(self.isBlockEntity()); } );

        // Vector fields
        new ReadOnlyField("Position",
                (self, key) -> {
                    Vec3d pos = self.getEntityPos();
                    return new LunaVec3(pos);
                }
        );
        new ReadOnlyField("Velocity",
                (self, key) -> {
                    if (self.isBlockEntity())
                        return LuaValue.error("BlockEntity does not support velocity.");

                    Vec3d pos = self.entity.getVelocity();
                    return new LunaVec3(pos);
                }
        );
    }

    // Methods
    static
    {
        new LunaMethod("Rotate", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue Yaw, LuaValue Pitch)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                self.entity.rotate(Yaw.checknumber().tofloat(), Pitch.checknumber().tofloat());
                return LuaValue.NIL;
            }
        });
        new LunaMethod("HasTag", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaTag)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                if (!luaTag.isstring())
                    return LuaValue.error("Entity tags must be strings.");

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities do not support tags.");

                return LuaBoolean.valueOf(self.entity.getCommandTags().contains(luaTag.toString()));
            }
        });
        new LunaMethod("RemoveTag", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaTag)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                if (!luaTag.isstring())
                    return LuaValue.error("Entity tags must be strings.");

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities do not support tags.");

                self.entity.removeCommandTag(luaTag.toString());
                return LuaValue.NIL;
            }
        });
        new LunaMethod("AddTag", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaTag)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);

                if (!luaTag.isstring())
                    return LuaValue.error("Entity tags must be strings.");

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities do not support tags.");

                self.entity.addCommandTag(luaTag.toString());
                return LuaValue.NIL;
            }
        });
        new LunaMethod("SetVelocity", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaVel)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                Vec3d newVel = LunaVec3.fromLua(luaVel);

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities do not support velocity.");

                self.entity.setVelocity(newVel);
                self.entity.velocityDirty = true;
                self.entity.velocityModified = true;
                return LuaValue.NIL;
            }
        });
        new LunaMethod("ApplyForce", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaVel)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                Vec3d newVel = LunaVec3.fromLua(luaVel);

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities do not support velocity.");

                self.entity.addVelocity(newVel);
                self.entity.velocityDirty = true;
                self.entity.velocityModified = true;
                return LuaValue.NIL;
            }
        });
        new LunaMethod("Select", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaSelector) {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                String selectorString = luaSelector.tojstring();

                if (self == null)
                    return LuaValue.error("@s has been destroyed or is nil. Try using ':' instead of '.' to call this function.");

                try
                {
                    Collection<? extends  Entity> entities = self.Select(selectorString);
                    // Convert the entities to a Lua table
                    LuaTable rEnt = new LuaTable();
                    int index = 1;

                    for (Entity entity : entities)
                        rEnt.set(index++, LunaEntity.Fetch(entity, self.Executor).LunaToLua());

                    return rEnt;
                }
                catch (Exception e) { return LuaValue.error("Invalid selector string: " + e.toString()); }
            }
        });
        new LunaMethod("Teleport", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args)
            {
                LunaEntity self = LuaToLuna(args.arg(1));
                Vec3d Dest = new Vec3d(0, 0, 0);
                float Yaw = self.entity.getYaw(), Pitch = self.entity.getPitch();
                ServerWorld TargetWorld = self.getServerWorld();

                if (self.isBlockEntity())
                    return LuaValue.error("BlockEntities cannot be teleported.");

                if (args.narg() == 2)
                    // self, Dest
                    Dest = LunaVec3.fromLua(args.arg(2));
                else if (args.narg() == 3)
                {
                    // self, World, Dest
                }
                else if (args.narg() == 4)
                {
                    // self, Dest, Yaw, Pitch
                    Dest = LunaVec3.fromLua(args.arg(2));
                    Yaw = args.arg(3).tofloat();
                    Pitch = args.arg(4).tofloat();
                    TargetWorld = self.getServerWorld();
                }
                else if (args.narg() == 5)
                {
                    // self, World, Dest, Yaw, Pitch
                }

                MinecraftServer server = self.entity.getServer();
                LinkedHashSet<PositionFlag> Flags = new LinkedHashSet<PositionFlag>();
                Flags.add(PositionFlag.DELTA_X);
                Flags.add(PositionFlag.DELTA_Y);
                Flags.add(PositionFlag.DELTA_Z);
                self.entity.teleport(TargetWorld, Dest.x, Dest.y, Dest.z, Flags, Yaw, Pitch, false);
                return LuaValue.NIL;
            }
        });
    }

    // Static function to create or retrieve an instance
    public static LunaEntity Fetch(Entity TargetEntity, LunaExecutor TargetExecutor)
    {
        LunaEntity rEnt = TargetExecutor.RegisteredEntities.get(TargetEntity);
        if (rEnt == null || rEnt.entity == null || rEnt.entity.isRemoved())
        {
            // Clear previous
            if (rEnt != null) TargetExecutor.RegisteredEntities.remove(TargetEntity);

            rEnt = new LunaEntity(TargetEntity, TargetExecutor);
            TargetExecutor.RegisteredEntities.put(TargetEntity, rEnt);
        }
        return rEnt;
    }

    public static LunaEntity Fetch(BlockEntity TargetEntity, LunaExecutor TargetExecutor)
    {
        LunaEntity rEnt = TargetExecutor.RegisteredEntities.get(TargetEntity);
        if (rEnt == null || rEnt.entity == null || rEnt.entity.isRemoved())
        {
            // Clear previous
            if (rEnt != null) TargetExecutor.RegisteredEntities.remove(TargetEntity);

            rEnt = new LunaEntity(TargetEntity, TargetExecutor);
            TargetExecutor.RegisteredEntities.put(TargetEntity, rEnt);
        }
        return rEnt;
    }

    // Override necessary LuaValue methods
    @Override
    public int type() { return TUSERDATA; }
    @Override
    public String typename() { return "LunaEntity(" + getEntityType() + ")"; }
    @Override
    public String tojstring() { return getEntityName(); }

    public LuaValue LunaToLua() { return luaProxy; }
    public Entity GetSelf() { return entity; }
    // Retrieves LunaEntities from their lua proxies
    public static LunaEntity LuaToLuna(LuaValue luaEntity)
    {
        if (luaEntity.isuserdata())
            return (LunaEntity) luaEntity.checkuserdata(LunaEntity.class);
        return null;
    }

    public World getWorld()
    {
        if (isMobEntity()) return entity.getEntityWorld();
        else return blockEntity.getWorld();
    }
    public ServerWorld getServerWorld() {
        if (isMobEntity())
        {
            // Obtain the Minecraft server instance
            MinecraftServer server = entity.getServer();

            // Check if the server instance exists (server-side check)
            if (server != null)
            {
                // Retrieve the server world based on the entity's current dimension
                return server.getWorld(entity.getEntityWorld().getRegistryKey());
            }
        }
        else {
            MinecraftServer server = blockEntity.getWorld().getServer();

            // Check if the server instance exists (server-side check)
            if (server != null)
            {
                // Retrieve the server world based on the entity's current dimension
                return server.getWorld(blockEntity.getWorld().getRegistryKey());
            }
        }
        // Return null if the server or world is unavailable (e.g., if on client side)
        return null;
    }
    public Collection<? extends Entity> Select(String Selector)
    {
        try {
            // Use StringReader and EntitySelectorParser to parse the selector string
            StringReader reader = new StringReader(Selector);

            EntitySelectorReader parser = new EntitySelectorReader(reader, true);
            EntitySelector selector = parser.read();

            // Execute the selector to get entities from the command source context
            if (isBlockEntity())
                return selector.getEntities(new ServerCommandSource(null, getEntityPos(), Vec2f.ZERO, getServerWorld(), 2, "CommandBlock", null, getWorld().getServer(), null));
            return selector.getEntities(Executor.cmdContext.getSource().withEntity(entity));
        } catch (Exception e)
        {
            return null;
        }
    }

    public static LuaValue summonEntity(ServerWorld world, String entityTypeId, Vec3d position, LunaExecutor exec) {
        Optional<EntityType<?>> entityType = EntityType.get(entityTypeId); // Get the entity type by ID
        if (entityType == null)
            throw new IllegalArgumentException("Entity type '" + entityTypeId + "' does not exist.");

        Entity entity = entityType.get().create(world, SpawnReason.COMMAND); // Create the entity
        if (entity != null) {
            entity.setPosition(position.x, position.y, position.z); // Set the position
            world.spawnEntity(entity); // Spawn the entity in the world
            return Fetch(entity, exec).LunaToLua();
        }
        return null;
    }
    public static void gAssign(Globals gState, boolean isClient)
    {
        gState.set("Summon", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String eType = args.arg(1).checkjstring();
                Vec3d ePos = new Vec3d(0, 0, 0);
                if (args.narg() >= 2)
                    ePos = LunaVec3.fromLua(args.arg(2));

                ServerCommandSource source = Luna.GetExec(isClient).cmdContext.getSource();
                ServerWorld world = source.getWorld();

                return summonEntity(world, eType, ePos, Luna.GetExec(isClient));
            }
        });
    }
}