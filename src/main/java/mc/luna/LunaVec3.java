package mc.luna;

import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class LunaVec3 extends LuaUserdata {
    private static final LuaTable metatable = new LuaTable();
    private static final LuaTable methods = new LuaTable();

    // Static method initializer block
    static {
        // GetUnit(Vector3: self): Vector3 | Returns a Vector3 value with a magnitude of 1.
        methods.set("GetUnit", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) {
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                double magnitude = Math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z);
                if (magnitude == 0) return new LunaVec3(0, 0, 0); // Avoid division by zero
                return new LunaVec3(self.x / magnitude, self.y / magnitude, self.z / magnitude);
            }
        });

        // SetUnit(Vector3: self, Vector3: NewUnit): Vector3 | Returns a Vector3 value with an equivalent magnitude to self but a direction equal to NewUnit.
        methods.set("SetUnit", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue newUnit) {
                if (!(newUnit instanceof LunaVec3)) {
                    return LuaValue.error("SetUnit requires a Vector3.");
                }
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d unit = (Vec3d) ((LunaVec3) newUnit).userdata();
                double magnitude = Math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z);
                double newMagnitude = Math.sqrt(unit.x * unit.x + unit.y * unit.y + unit.z * unit.z);
                if (newMagnitude == 0) return new LunaVec3(0, 0, 0); // Avoid division by zero
                return new LunaVec3((unit.x / newMagnitude) * magnitude, (unit.y / newMagnitude) * magnitude, (unit.z / newMagnitude) * magnitude);
            }
        });

        // GetMagnitude(Vector3: self): number | Returns the magnitude of this vector.
        methods.set("GetMagnitude", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) {
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                return LuaValue.valueOf(Math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z));
            }
        });

        // SetMagnitude(Vector3: self, number: NewMag): Vector3 | Returns a Vector3 value with an equivalent direction to self but a magnitude equal to NewMag.
        methods.set("SetMagnitude", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue newMag) {
                if (!newMag.isnumber()) {
                    return LuaValue.error("SetMagnitude requires a number.");
                }
                double newMagnitude = newMag.todouble();
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                double currentMagnitude = Math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z);
                if (currentMagnitude == 0) return new LunaVec3(0, 0, 0); // Avoid division by zero
                return new LunaVec3((self.x / currentMagnitude) * newMagnitude, (self.y / currentMagnitude) * newMagnitude, (self.z / currentMagnitude) * newMagnitude);
            }
        });

        // Cross(Vector3: self, Vector3: Other): Vector3 | Cross product of 2 vectors.
        methods.set("Cross", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue other) {
                if (!(other instanceof LunaVec3)) {
                    return LuaValue.error("Cross product requires a Vector3.");
                }
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d otherVec = (Vec3d) ((LunaVec3) other).userdata();
                return new LunaVec3(
                        self.y * otherVec.z - self.z * otherVec.y,
                        self.z * otherVec.x - self.x * otherVec.z,
                        self.x * otherVec.y - self.y * otherVec.x
                );
            }
        });

        // Dot(Vector3: self, Vector3: Other): number | Dot product of 2 vectors.
        methods.set("Dot", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue other) {
                if (!(other instanceof LunaVec3)) {
                    return LuaValue.error("Dot product requires a Vector3.");
                }
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d otherVec = (Vec3d) ((LunaVec3) other).userdata();
                return LuaValue.valueOf(self.x * otherVec.x + self.y * otherVec.y + self.z * otherVec.z);
            }
        });

        // Rotate(Vector3: self, Vector3: Radians): Vector3 | Rotates the given vector across the given amounts in radians.
        methods.set("Rotate", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue radians) {
                if (!radians.isnumber()) {
                    return LuaValue.error("Rotate requires a number (in radians).");
                }
                double angle = radians.todouble();
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                return new LunaVec3(
                        self.x * cos - self.y * sin,
                        self.x * sin + self.y * cos,
                        self.z // Assuming rotation is around the z-axis
                );
            }
        });

        metatable.set("methods", methods);
    }

    // Static initializer block to set up the metatable once, also so I can collapse this chunk xd
    static {
        // `__index` metatable for field access
        metatable.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue k)
            {
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                if (k.isstring())
                {
                    String key = k.tojstring().toLowerCase();
                    if (key.equals("x")) return LuaValue.valueOf(self.x);
                    if (key.equals("y")) return LuaValue.valueOf(self.y);
                    if (key.equals("z")) return LuaValue.valueOf(self.z);

                    // No need for a null check since if it's null I'll return null anyway
                    return methods.get(k);
                }
                return LuaValue.NIL;
            }
        });

        // `__newindex` for immutability
        metatable.set("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                return LuaValue.error("Vector3 is immutable. Please create a new vector and assign it instead.");
            }
        });

        // `__tostring` for string representation
        metatable.set("__tostring", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) {
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                return LuaValue.valueOf("(" + self.x + ", " + self.y + ", " + self.z + ")");
            }
        });

        // `__eq` for equality
        metatable.set("__eq", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaOther) {
                if (!(luaOther instanceof LunaVec3)) return LuaValue.FALSE;
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d other = (Vec3d) ((LunaVec3) luaOther).userdata();
                return LuaValue.valueOf(self.x == other.x && self.y == other.y && self.z == other.z);
            }
        });

        // `__add` for vector addition
        metatable.set("__add", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaOther) {
                if (!(luaOther instanceof LunaVec3)) {
                    return LuaValue.error("Vector addition requires a Vector3.");
                }
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d other = (Vec3d) ((LunaVec3) luaOther).userdata();
                return new LunaVec3(self.x + other.x, self.y + other.y, self.z + other.z);
            }
        });

        // `__sub` for vector subtraction
        metatable.set("__sub", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaOther) {
                if (!(luaOther instanceof LunaVec3)) {
                    return LuaValue.error("Vector subtraction requires a Vector3.");
                }
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                Vec3d other = (Vec3d) ((LunaVec3) luaOther).userdata();
                return new LunaVec3(self.x - other.x, self.y - other.y, self.z - other.z);
            }
        });

        // `__mul` for scalar multiplication
        metatable.set("__mul", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaOther) {
                if (luaOther.isnumber()) {
                    Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                    double scalar = luaOther.todouble();
                    return new LunaVec3(self.x * scalar, self.y * scalar, self.z * scalar);
                }
                return LuaValue.error("Scalar multiplication requires a number.");
            }
        });

        // `__div` for scalar division
        metatable.set("__div", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf, LuaValue luaOther) {
                if (luaOther.isnumber()) {
                    Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                    double scalar = luaOther.todouble();
                    if (scalar == 0) {
                        return LuaValue.error("Cannot divide by zero.");
                    }
                    return new LunaVec3(self.x / scalar, self.y / scalar, self.z / scalar);
                }
                return LuaValue.error("Scalar division requires a number.");
            }
        });

        // `__unm` for vector negation
        metatable.set("__unm", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaSelf) {
                Vec3d self = (Vec3d) ((LunaVec3) luaSelf).userdata();
                return new LunaVec3(-self.x, -self.y, -self.z);
            }
        });
    }

    // Constructor that sets up the Vec3d userdata
    public LunaVec3(double x, double y, double z)
    {
        super(new Vec3d(x, y, z));
        setmetatable(metatable);  // Associate this instance with the static metatable
    }

    public LunaVec3(Vec3d Vector)
    {
        super(Vector);
        setmetatable(metatable);
    }
    // Convert Nbt sequential array to LunaVec3
    public static LunaVec3 fromNbt(NbtList nbt) {
        if (nbt.size() < 3) {
            throw new IllegalArgumentException("NBT list must have at least 3 elements.");
        }
        double x = nbt.getDouble(0);
        double y = nbt.getDouble(1);
        double z = nbt.getDouble(2);
        return new LunaVec3(x, y, z);
    }

    // Convert LunaVec3 to Nbt sequential array
    public NbtList toNbt()
    {
        NbtList nbt = new NbtList();
        nbt.add(NbtDouble.of(this.ToVec3d().x));
        nbt.add(NbtDouble.of(this.ToVec3d().y));
        nbt.add(NbtDouble.of(this.ToVec3d().z));
        return nbt;
    }

    // Lua-accessible function to create a new Vec3 instance
    private static final ThreeArgFunction NewVec3 = new ThreeArgFunction()
    {
        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue z) { return new LunaVec3(x.todouble(), y.todouble(), z.todouble()); }
    };

    public Vec3d ToVec3d()
    { return (Vec3d) this.userdata(); }

    // Cross product of this vector with another vector
    public LunaVec3 Cross(LunaVec3 other) {
        Vec3d self = this.ToVec3d(); // Get the Vec3d representation of this vector
        Vec3d otherVec = other.ToVec3d(); // Get the Vec3d representation of the other vector
        return new LunaVec3(
                self.y * otherVec.z - self.z * otherVec.y,
                self.z * otherVec.x - self.x * otherVec.z,
                self.x * otherVec.y - self.y * otherVec.x
        );
    }


    // Registers Vec3 in the global state
    public static void gAssign(Globals gState)
    { gState.set("Vec3", NewVec3); }

    // Optional overrides to customize type and typename
    @Override
    public int type() { return LuaValue.TUSERDATA; }

    @Override
    public String typename() { return "Vector3"; }
}
