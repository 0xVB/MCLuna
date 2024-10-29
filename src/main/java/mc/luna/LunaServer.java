package mc.luna;

import net.minecraft.server.MinecraftServer;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class LunaServer extends LuaUserdata
{
    private static LuaTable __mt = new LuaTable();
    private LunaExecutor exec;
    public MinecraftServer server;
    public LunaServer(MinecraftServer server, LunaExecutor exec)
    {
        super(server, __mt);
        this.server = server;
        this.exec = exec;
    }

    // Metatable
    static
    {
        __mt.set("__tostring", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) { return LuaString.valueOf("LunaServer"); }
        });

        __mt.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue key) {
                LunaServer self = (LunaServer) luaSelf.checkuserdata(LunaServer.class);
                String k = key.checkjstring();

                return null;
            }
        });
    }
    @Override
    public int type()
    { return LuaValue.TUSERDATA; }

    @Override
    public String typename()
    { return "LunaServer"; }
}
