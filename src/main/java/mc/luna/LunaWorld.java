package mc.luna;

import net.minecraft.server.world.ServerWorld;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;

public class LunaWorld extends LuaUserdata
{
    private LunaExecutor exec;
    public ServerWorld world;

    private static LuaTable __mt = new LuaTable();

    public LunaWorld(ServerWorld world, LunaExecutor exec)
    {
        super(world, __mt);
        this.exec = exec;
        this.world = world;
    }

    @Override
    public String typename() { return "LunaWorld"; }
}
