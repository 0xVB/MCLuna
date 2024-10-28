package mc.luna;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LunaEntity extends LuaValue
{
    private final Entity entity;
    private final LuaValue luaProxy;
    public final LunaExecutor Executor;

    // Private constructor to prevent direct instantiation. Use LunaEntity.Fetch instead.
    private LunaEntity(Entity entity, LunaExecutor Exec)
    {
        this.entity = entity;
        this.luaProxy = LuaValue.userdataOf(this, __mt);
        this.Executor = Exec;
    }
    @FunctionalInterface
    private interface idxJavaFn { LuaValue apply(LunaEntity self, String key); }
    @FunctionalInterface
    private interface ndxJavaFn { LuaValue apply(LunaEntity self, String key, LuaValue value); }
    private static class LunaField
    {
        protected idxJavaFn __idx;
        protected ndxJavaFn __ndx;
        LunaField(String fieldName, idxJavaFn idx, ndxJavaFn ndx)
        {
            __idx = idx;
            __ndx = ndx;

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
            public LuaValue apply(LunaEntity self, String key) { Luna.LOGGER.info("Called!"); return _methodFn; }
        };

        LunaMethod(String MethodName, LuaFunction MethodFunction)
        {
            super(MethodName, null, _readOnlyNdx);
            __idx = _readOnlyIdx;
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
                Entity me = self.entity;

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
                Entity me = self.entity;

                if (me.isRemoved() || self == null)
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

                if (self != null && self.entity.isAlive())
                    return LuaString.valueOf(self.entity.getType().toString());
                else
                    return LuaString.valueOf("[DESTROYED_ENTITY]");
            }
        };

        __mt.set("__index", __index);
        __mt.set("__newindex", __newindex);
        __mt.set("__tostring", __tostring);
    }

    // Fields
    static
    {
        // Position Field (returns Vec3 as Lua table)
        new LunaField("Position",
                (self, key) -> {
                    Vec3d pos = self.entity.getPos();
                    return new LunaVec3(pos);
                },
                (self, key, value) -> {
                    Vec3d pos = ((LunaVec3)value).ToVec3d();
                    self.entity.updatePosition(pos.x, pos.y, pos.z);
                    return LuaValue.NIL;
                }
        );
    }

    // Methods
    static
    {
        // Entity:Select(string: Selector)
        new LunaMethod("Select", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaSelector)
            {
                LunaEntity self = LunaEntity.LuaToLuna(luaSelf);
                String selectorString = luaSelector.tojstring();

                if (self == null)
                    return LuaValue.error("@s has been destroyed or is nil. Try using ':' instead of '.' to call this function.");

                try
                {
                    // Use StringReader and EntitySelectorParser to parse the selector string
                    StringReader reader = new StringReader(selectorString);

                    EntitySelectorReader parser = new EntitySelectorReader(reader, true);
                    EntitySelector selector = parser.read();

                    // Execute the selector to get entities from the command source context
                    Collection<? extends Entity> entities = selector.getEntities(self. Executor.cmdContext.getSource().withEntity(self.entity));

                    // Convert the entities to a Lua table
                    LuaTable rEnt = new LuaTable();
                    int index = 1;
                    for (Entity entity : entities) { rEnt.set(index++, LunaEntity.Fetch(entity, self.Executor).LunaToLua()); }

                    return rEnt;
                }
                catch (CommandSyntaxException e) { return LuaValue.error("Invalid selector string: " + e.toString()); }
            }
        });
    }

    // Static function to create or retrieve an instance
    public static LunaEntity Fetch(Entity TargetEntity, LunaExecutor TargetExecutor) {
        LunaEntity rEnt = TargetExecutor.RegisteredEntities.get(TargetEntity);
        if (rEnt == null || rEnt.entity.isRemoved())
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
    public String typename() { return entity.getType().toString(); }
    @Override
    public String tojstring() { return entity.getType().toString(); }

    public LuaValue LunaToLua() { return luaProxy; }
    public Entity GetSelf() { return entity; }
    // Retrieves LunaEntities from their lua proxies
    public static LunaEntity LuaToLuna(LuaValue luaEntity)
    {
        if (luaEntity.isuserdata())
            return (LunaEntity) luaEntity.checkuserdata(LunaEntity.class);
        return null;
    }
}