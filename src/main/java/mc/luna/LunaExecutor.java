package mc.luna;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.HashMap;
import java.util.Map;

public class LunaExecutor
{
    public Map<Entity, LunaEntity> RegisteredEntities = new HashMap<>();
    public final Globals gState;
    public LunaEntity self = null;
    public CommandContext<ServerCommandSource> cmdContext;
    public boolean isClient;
    public boolean isServer;

    public LunaExecutor()
    {
        // Create a Lua _ENV
        this.gState = JsePlatform.standardGlobals();
        this.self = null;
    }

    private static LunaExecutor CurrentExec;
    public static LunaExecutor GetCurrent()
    { return CurrentExec; }
    public LuaValue Execute(String luaCode, LunaEntity lSelf)
    {
        CurrentExec = this;
        this.self = lSelf;

        // Setup _ENV
        try
        {
            gState.set("IsClient", LuaBoolean.valueOf(isClient));
            gState.set("IsServer", LuaBoolean.valueOf(isServer));
            gState.set("self", lSelf.LunaToLua());
            gState.set("_G", LunaGlobalState.getGlobalState(this));

            LunaVec3.gAssign(gState);
            LunaLogger.gAssign(gState);
        }
        catch (Exception e)
        { return LuaValue.valueOf("Error: [ENV_SETUP] " + e.getMessage()); }

        // Execute src
        try
        { return gState.load(luaCode).call(); }
        catch (Exception e)
        { return LuaValue.valueOf("Error: " + e.getMessage()); }
    }
}
