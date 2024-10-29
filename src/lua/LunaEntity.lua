---@meta
---@class LunaEntity
---@field Name string
---@field DisplayName string
---@field Type string
---@field UUID string
---@field IsMob boolean
---@field IsBlock boolean
---@field Position Vector3
---@field Velocity Vector3
---@field Destroyed boolean
---@field powered boolean
local temp = {};

---Returns a list of entities that match the SelectorString. SelectorStrings are the same as regular CommandBlock selectors.
---@param SelectorString string
---@return LunaEntity[]
function temp:Select(SelectorString) return {self}; end;

---Returns true if this entity contains the given tag `TagName`.
---@param TagName string
---@return boolean
function temp:HasTag(TagName) return true; end;

---Removes the tag `TagName` from this entity.
---@param TagName string
function temp:RemoveTag(TagName) end;

---Adds the tag `TagName` to this entity.
---@param TagName any
function temp:AddTag(TagName) end;

---Sets the velocity of this entity to NewVel.
---@param NewVel Vector3
function temp:SetVelocity(NewVel) end;

---Applies a force to this entity equal to `Force`. Similar to self:SetVelocity(self.Velocity + Force).
---@param Force Vector3
function temp:ApplyForce(Force) end;

---Teleports the entity to destination `Dest`.
---@param Dest Vector3
function temp:Teleport(Dest) end;

---Teleports the entity to the destination `Dest` in the world
---@param World ServerWorld
---@param Dest Vector3
function temp:Teleport(World, Dest) end;

---Teleports the entity to the destination `Dest` and rotates the camera to (Yaw, Pitch).
---@param Dest Vector3
---@param Yaw number
---@param Pitch number
function temp:Teleport(Dest, Yaw, Pitch) end;

---Teleports the entity to the destination `Dest` in the world `World` and sets the camera's orientation to (Yaw, Pitch).
---@param World ServerWorld
---@param Dest Vector3
---@param Yaw number
---@param Pitch number
function temp:Teleport(World, Dest, Yaw, Pitch) end;

---Summons a new entity with the given type.
---@param EntityType string | EntityType
---@param Position Vector3?
---@return LunaEntity
function Summon(EntityType, Position) return temp; end;

---@enum (value) EntityType
EntityTypes = {
    Creeper = "minecraft:creeper";
    Skeleton = "minecraft:skeleton";
};

self = temp;