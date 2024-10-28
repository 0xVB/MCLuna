package mc.luna;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.session.telemetry.WorldLoadedEvent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.luaj.vm2.LuaValue;

public class Luna implements ModInitializer
{
	public static final String MOD_ID = "luna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static LunaExecutor ServerExecutor;
	private static LunaExecutor ClientExecutor;

	public static LunaExecutor GetExec(boolean OnClient)
	{
		if (OnClient)
		{
			if (ClientExecutor != null) return ClientExecutor;

			ClientExecutor = new LunaExecutor();
			ClientExecutor.isClient = true;
			ClientExecutor.isServer = false;
			return ClientExecutor;
		}

		if (ServerExecutor != null) return ServerExecutor;

		ServerExecutor = new LunaExecutor();
		ServerExecutor.isClient = false;
		ServerExecutor.isServer = true;
		return ServerExecutor;
	}

	public static LunaExecutor GetExec(CommandContext<ServerCommandSource> cmdContext)
	{
		LunaExecutor Exec = GetExec(cmdContext.getSource().getEntity() instanceof ServerPlayerEntity);
		Exec.cmdContext = cmdContext;
		return  Exec;
	}

	@Override
	public void onInitialize()
	{
		CommandRegistrationCallback.EVENT.register(Luna::RegisterLuaCommand);
		ServerWorldEvents.LOAD.register(new ServerWorldEvents.Load() {
            @Override
            public void onWorldLoad(MinecraftServer server, ServerWorld world)
			{ LunaGlobalState.NewWorldLoaded(server, world); }
        });
	}

	private static void RegisterLuaCommand(CommandDispatcher<ServerCommandSource> cDispatcher, CommandRegistryAccess cRegAccess, CommandManager.RegistrationEnvironment regEnv)
	{
		cDispatcher.register(CommandManager.literal("lua")
				.then(CommandManager.argument("src", StringArgumentType.greedyString())
						.executes(Luna::LuaExec)));
	}

	private static int LuaExec(CommandContext<ServerCommandSource> cmdContext)
	{
		String luaSrc = StringArgumentType.getString(cmdContext, "src");
		LunaExecutor Exec = GetExec(cmdContext);
		LunaEntity self = LunaEntity.Fetch(cmdContext.getSource().getEntity(), Exec);

		LuaValue luaResult = Exec.Execute(luaSrc, self);

		// Check if the result is an error message
		if (luaResult.isstring() && luaResult.tojstring().startsWith("Error: "))
		{
			cmdContext.getSource().sendError(Text.literal("[Luna]" + luaResult.tojstring()));
			return 0;
		}
		else
		{
			cmdContext.getSource().sendFeedback(() -> Text.literal("[Luna]: " + luaResult.tojstring()), false);
			return 1;
		}
	}
}
