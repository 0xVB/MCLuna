package mc.luna;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.apache.logging.log4j.core.jmx.Server;
import org.luaj.vm2.*;

public class LunaGlobalState extends LuaTable
{

    private static final String STATE_KEY = "LunaGlobalState";
    private NbtCompound gData;
    private PersistentState gState;
    private static LunaGlobalState singleton;
    private LunaStateSaver gLunaState;

    private static class LunaStateSaver extends PersistentState
    {
        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries)
        { return nbt.copyFrom(singleton.gData); }
    }

    private static LuaTable __mt = new LuaTable();
    private static LuaFunction __index = new LuaFunction()
    {
        @Override
        public LuaValue call(LuaValue luaSelf, LuaValue key)
        {
            if (!key.isstring())
                return LuaValue.NIL;

            return NbtHelper.nbtToLua(singleton.gData.get(key.tojstring()));
        }
    };
    private static LuaFunction __newindex = new LuaFunction()
    {
        @Override
        public LuaValue call(LuaValue luaSelf, LuaValue key, LuaValue val)
        {
            if (!key.isstring())
                return LuaValue.error("_G keys must all be strings.");

            if (!NbtHelper.isNbtCompatible(val))
                return LuaValue.error(val.typename() + " is not NBT compatible.");

            singleton.gData.put(key.tojstring(), NbtHelper.luaToNbt(val));
            singleton.gLunaState.markDirty();
            Luna.LOGGER.info("[Luna] Marked GlobalState Dirty.");
            return LuaValue.TRUE;
        }
    };
    private LunaGlobalState()
    {
        __mt.set("__index", __index);
        __mt.set("__newindex", __newindex);
        setmetatable(__mt);

        gData = new NbtCompound();
        singleton = this;
    }

    public void newState(LunaExecutor Exec)
    {
        gData = new NbtCompound();
        Exec.gState.set("_G", this);
    }
    public static LunaGlobalState getGlobalState(LunaExecutor Exec)
    {
        if (singleton == null)
            new LunaGlobalState();
        return singleton;
    }

    public static void NewWorldLoaded(MinecraftServer mcServer, ServerWorld mcWorld)
    {
        Luna.LOGGER.info("[Luna] Loading GlobalLunaState");
        getGlobalState(Luna.GetExec(false));
        singleton.newState(Luna.GetExec(false));
        singleton.gLunaState = new LunaStateSaver();
    }
}
