
-- 秒杀Lua脚本
-- 参数说明：
-- ARGV[1]: 优惠券ID
-- ARGV[2]: 用户ID
-- KEYS[1]: 库存key (seckill:stock:{voucherId})
-- KEYS[2]: 用户订单set key (seckill:order:{voucherId})

local voucherId = ARGV[1]
local userId = ARGV[2]
local stockKey = KEYS[1]
local orderSetKey = KEYS[2]

-- 1. 判断库存是否充足
local stock = tonumber(redis.call('GET', stockKey))
if not stock or stock <= 0 then
    return 1  -- 库存不足
end

-- 2. 判断用户是否已经下过单
local isOrdered = redis.call('SISMEMBER', orderSetKey, userId)
if isOrdered == 1 then
    return 2  -- 用户已下单
end

-- 3. 扣减库存
redis.call('DECR', stockKey)

-- 4. 将userId存入优惠券set集合
redis.call('SADD', orderSetKey, userId)

-- 5. 返回成功
return 0
