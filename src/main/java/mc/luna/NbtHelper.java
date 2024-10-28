package mc.luna;

import net.minecraft.nbt.*;
import org.luaj.vm2.*;

public class NbtHelper {

    /**
     * Converts a LuaValue into an appropriate NbtElement.
     * @param luaValue The LuaValue to convert.
     * @return The converted NbtElement.
     */
    public static NbtElement luaToNbt(LuaValue luaValue)
    {
        if (luaValue.isnumber())
        {
            // Convert to NbtDouble if it has decimals, otherwise NbtInt
            double num = luaValue.todouble();
            return (num == (int) num) ? NbtInt.of(luaValue.toint()) : NbtDouble.of(num);
        } else if (luaValue.isstring()) {
            // Convert to NbtString
            return NbtString.of(luaValue.tojstring());
        } else if (luaValue.isboolean()) {
            // Convert to NbtByte (0 or 1) for booleans
            return NbtByte.of((byte) (luaValue.toboolean() ? 1 : 0));
        } else if (luaValue.istable()) {
            LuaTable table = luaValue.checktable();
            return tableToNbt(table);
        } else {
            // Unsupported Lua type, return empty compound as a fallback
            return new NbtCompound();
        }
    }

    /**
     * Converts a LuaTable to an NbtCompound or NbtList, depending on content.
     * @param table The LuaTable to convert.
     * @return The corresponding NbtElement.
     */
    private static NbtElement tableToNbt(LuaTable table)
    {
        if (isSequentialTable(table))
        {
            // Sequential table -> NbtList
            NbtList nbtList = new NbtList();
            for (int i = 1; !table.get(i).isnil(); i++) {
                nbtList.add(luaToNbt(table.get(i)));
            }
            return nbtList;
        } else {
            // Keyed table -> NbtCompound
            NbtCompound nbtCompound = new NbtCompound();
            LuaValue k = LuaValue.NIL;
            while (true)
            {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil()) break;
                LuaValue v = n.arg(2);
                nbtCompound.put(k.tojstring(), luaToNbt(v));
            }
            return nbtCompound;
        }
    }

    /**
     * Checks if a LuaTable is sequential, indicating it should map to an NbtList.
     * @param table The LuaTable to check.
     * @return True if the table is sequential, false otherwise.
     */
    private static boolean isSequentialTable(LuaTable table)
    {
        int maxIndex = 0;
        LuaValue k = LuaValue.NIL;

        while (true)
        {
            Varargs n = table.next(k);
            if ((k = n.arg1()).isnil()) break;
            if (!k.isint()) return false;
            maxIndex = Math.max(maxIndex, k.toint());
        }
        return maxIndex == table.length();
    }

    public static LuaValue nbtToLua(NbtElement nbtElement)
    {

        if (nbtElement instanceof NbtCompound)
            return convertNbtCompound((NbtCompound) nbtElement);

        else if (nbtElement instanceof NbtList)
            return convertNbtList((NbtList) nbtElement);

        else if (nbtElement instanceof NbtString)
            return LuaString.valueOf(((NbtString) nbtElement).asString());

        else if (nbtElement instanceof NbtInt)
            return LuaNumber.valueOf(((NbtInt) nbtElement).intValue());

        else if (nbtElement instanceof NbtFloat)
            return LuaNumber.valueOf(((NbtFloat) nbtElement).floatValue());

        else if (nbtElement instanceof NbtDouble)
            return LuaNumber.valueOf(((NbtDouble) nbtElement).doubleValue());

        else if (nbtElement instanceof NbtLong)
            return LuaNumber.valueOf(((NbtLong) nbtElement).longValue());

        else if (nbtElement instanceof NbtShort)
            return LuaNumber.valueOf(((NbtShort) nbtElement).shortValue());

        else if (nbtElement instanceof NbtByte)
            return LuaBoolean.valueOf(((NbtByte) nbtElement).byteValue() != 0);

        else if (nbtElement instanceof NbtByteArray)
            return convertNbtByteArray((NbtByteArray) nbtElement);

        else if (nbtElement instanceof NbtIntArray)
            return convertNbtIntArray((NbtIntArray) nbtElement);

        else if (nbtElement instanceof NbtLongArray)
            return convertNbtLongArray((NbtLongArray) nbtElement);

        return LuaValue.NIL;
    }

    private static LuaTable convertNbtCompound(NbtCompound compound)
    {
        LuaTable table = new LuaTable();
        for (String key : compound.getKeys())
        {
            NbtElement value = compound.get(key);
            table.set(LuaString.valueOf(key), nbtToLua(value));
        }
        return table;
    }

    private static LuaTable convertNbtList(NbtList list)
    {
        LuaTable table = new LuaTable();
        for (int i = 0; i < list.size(); i++)
            table.set(i + 1, nbtToLua(list.get(i))); // Lua tables start at 1 :vomit:

        return table;
    }

    private static LuaTable convertNbtByteArray(NbtByteArray byteArray)
    {
        LuaTable table = new LuaTable();
        byte[] bytes = byteArray.getByteArray();
        for (int i = 0; i < bytes.length; i++)
            table.set(i + 1, LuaNumber.valueOf(bytes[i]));

        return table;
    }

    private static LuaTable convertNbtIntArray(NbtIntArray intArray)
    {
        LuaTable table = new LuaTable();
        int[] ints = intArray.getIntArray();
        for (int i = 0; i < ints.length; i++)
            table.set(i + 1, LuaNumber.valueOf(ints[i]));

        return table;
    }

    private static LuaTable convertNbtLongArray(NbtLongArray longArray)
    {
        LuaTable table = new LuaTable();
        long[] longs = longArray.getLongArray();
        for (int i = 0; i < longs.length; i++)
            table.set(i + 1, LuaNumber.valueOf(longs[i]));
        return table;
    }

    // Returns true if the value is compatible with Nbt
    public static boolean isNbtCompatible(LuaValue value)
    {
        if (value.isnil())
            return true;

        else if (value.isboolean())
            return true;

        else if (value.isnumber())
            return true;

        else if (value.isstring())
            return true;

        else if (value.istable())
            return canConvertTableToNbt((LuaTable) value);

        return false;
    }

    private static boolean canConvertTableToNbt(LuaTable table)
    {
        if (isSequentialTable(table))
        {
            for (LuaValue key : table.keys())
                if (!isNbtCompatible(table.get(key)))
                    return false;
            return true;
        }

        for (LuaValue key : table.keys())
            if (!key.isstring())
                return false;
            else if (!isNbtCompatible(table.get(key)))
                return false;
        return true; // The table is compatible
    }
}