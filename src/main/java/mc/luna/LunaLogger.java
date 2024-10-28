package mc.luna;

import net.minecraft.text.Text;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class LunaLogger {
    public static class LunaPrint extends VarArgFunction
    {
        @Override
        public LuaValue invoke(Varargs args) {
            StringBuilder output = new StringBuilder();

            for (int i = 1; i <= args.narg(); i++)
                output.append(args.arg(i).tojstring()).append("\t");

            String Output = output.toString().trim();
            Luna.LOGGER.info(Output);
            LunaExecutor.GetCurrent().cmdContext.getSource().sendFeedback(() -> Text.literal("[Luna]: " + Output), false);

            return LuaValue.NIL;
        }
    }

    public static class LunaWarn extends VarArgFunction
    {
        @Override
        public LuaValue invoke(Varargs args) {
            StringBuilder output = new StringBuilder();

            for (int i = 1; i <= args.narg(); i++)
                output.append(args.arg(i).tojstring()).append("\t");

            String Output = "[ERROR] " + output.toString().trim();

            Luna.LOGGER.info(Output);
            LunaExecutor.GetCurrent().cmdContext.getSource().sendError(Text.literal(Output));

            return LuaValue.NIL;
        }
    }

    public static class LunaSay extends VarArgFunction
    {
        @Override
        public LuaValue invoke(Varargs args) {
            StringBuilder output = new StringBuilder();

            for (int i = 1; i <= args.narg(); i++)
                output.append(args.arg(i).tojstring()).append("\t");

            String MsgContent = output.toString().trim();

            if (LunaExecutor.GetCurrent().cmdContext != null)
                LunaExecutor.GetCurrent().cmdContext.getSource().sendFeedback(() -> Text.literal(MsgContent), false);
            else
                Luna.LOGGER.warn("Command context is null, cannot send message: " + MsgContent);
            return LuaValue.NIL;
        }
    }
    public static void gAssign(Globals globals)
    {
        globals.set("print", new LunaPrint());
        globals.set("warn", new LunaWarn());
        globals.set("say", new LunaSay());
    }
}
