if redis.call("get", KEYS[1]) == ARGV[1] then
	return redis.call("expire", KEYS[1], ARGV[2])
else
	return redis.call("set", KEYS[1], ARGV[1], "EX", ARGV[2], "NX")
end