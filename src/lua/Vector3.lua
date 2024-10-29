---@meta
---@class Vector3
---@field x number
---@field X number
---@field y number
---@field Y number
---@field z number
---@field Z number
---@operator unm(): Vector3
---@operator add(Vector3): Vector3
---@operator sub(Vector3): Vector3
---@operator mul(number): Vector3
---@operator div(number): Vector3
local temp =
{
    x = 0;
    y = 0;
    z = 0;
};

---Returns a vector equivalent to this vector's direction with a magnitude of 1.
---@return Vector3
function temp:GetUnit() return self; end;

---Returns a vector equal to this vector in magnitude but with the direction of `Direction`.
---@param Direction Vector3
---@return Vector3
function temp:SetUnit(Direction) return self; end;

---Returns the magnitude of this vector.
---@return number
function temp:GetMagnitude() return 0.0; end;

---Returns a vector equal to this vector in direction but with the magnitude of `NewMag`.
---@param NewMag number
---@return Vector3
function temp:SetMagnitude(NewMag) return self; end;

---Returns the cross product of the 2 vectors.
---@param Other Vector3
---@return Vector3
function temp:Cross(Other) return self; end;

---Returns the dot product of the 2 vectors.
---@param Other Vector3
---@return number
function temp:Dot(Other) return 0.0; end;

---Rotates the current vector by the given amount in radians.
---@param Radians Vector3
---@return Vector3
function temp:Rotate(Radians) return self; end;

---Returns the Yaw of this vector.
---@return number
function temp:GetYaw() return 0.0; end;

---Returns the Pitch of this vector.
---@return number
function temp:GetPitch() return 0.0; end;

---Constructs a Vector3 value from the given X, Y, and Z components.
---@param x number
---@param y number
---@param z number
---@return Vector3
function Vec3(x, y, z) return temp; end;