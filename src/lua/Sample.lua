ArrowCountDown = ArrowCountDown or 0;

Target = self:Select("@e[type=creeper,sort=nearest,limit=1]")[1];
if (not Target or Target.Destroyed) then return; end;
local Delta = (Target.Position - self.Position);
local dUnit = Delta:GetUnit();

ArrowCountDown = ArrowCountDown - 1;
if (ArrowCountDown > 0) then return; end;
ArrowCountDown = math.random(1, 3);

local HitTime = 20;
local gConst = 0.05;

local nArrow = Summon("spectral_arrow", self.Position + Vec3(0, 3, 0));
local dMag = Delta:GetMagnitude();

local xVel = (Delta.x + math.random(-100, 100) / 75) / HitTime;
local zVel = (Delta.z + math.random(-100, 100) / 75) / HitTime;
local yVel = HitTime * gConst * .5;

nArrow:Rotate(0, 90);
nArrow:ApplyForce(Vec3(xVel, yVel, zVel));