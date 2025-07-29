-- 原子设置 key，带过期时间，仅当 key 不存在时
return redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2])