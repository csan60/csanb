local function calc_current_seconds()
  local base = os.time({ year = 2000, month = 1, day = 1, hour = 0, min = 0, sec = 0 })
  local now = os.time()
  if not base or not now then
    return 0
  end
  local diff = now - base
  if diff < 0 then
    return 0
  end
  local maxSeconds = 0xFFFFFFFFFFFF
  if diff > maxSeconds then
    diff = maxSeconds
  end
  return diff
end

local function clamp(value, minValue, maxValue)
  local number = tonumber(value) or 0
  if number < minValue then
    return minValue
  end
  if number > maxValue then
    return maxValue
  end
  return number
end

local function normalize_gender(gender)
  if tonumber(gender) == 1 then
    return 1
  end
  return 2
end

local function expect_drop(label, payload)
  clear(channels.upper)
  write_msg(channels.upper, protocols.P_Info, payload)
  etimer.delay(200)
  local response = read_msg(channels.upper, protocols.P_Info, 400)
  local dropped = response == nil
  check(dropped,
        string.format("✅ %s 被正确丢弃", label),
        string.format("❌ %s 未被丢弃，仍然收到了回应", label))
end

local function validate_response(label, expected)
  local response = read_msg(channels.upper, protocols.P_Info, 800)
  if not response or type(response.value) ~= "table" then
    check(false,
          "",
          string.format("❌ %s 未收到设置信息的回传数据", label))
    return
  end

  local value = response.value
  local gender = tonumber(value.sex or value.Sex)
  local height = tonumber(value.height or value.Height)
  local weight = tonumber(value.weight or value.Weight)
  local age = tonumber(value.age or value.Age)

  check(gender == expected.sex,
        string.format("✅ %s 性别处理正确", label),
        string.format("❌ %s 性别处理错误，期望 %d，实际 %s", label, expected.sex, tostring(value.sex)))
  check(height == expected.height,
        string.format("✅ %s 身高处理正确", label),
        string.format("❌ %s 身高处理错误，期望 %d，实际 %s", label, expected.height, tostring(value.height)))
  check(weight == expected.weight,
        string.format("✅ %s 体重处理正确", label),
        string.format("❌ %s 体重处理错误，期望 %d，实际 %s", label, expected.weight, tostring(value.weight)))
  check(age == expected.age,
        string.format("✅ %s 年龄处理正确", label),
        string.format("❌ %s 年龄处理错误，期望 %d，实际 %s", label, expected.age, tostring(value.age)))
end

local function expect_accept_with_noise(seconds)
  clear(channels.upper)
  local payload = {
    currentSecends = seconds,
    sex = 1,
    height = 170,
    weight = 60,
    age = 25,
  }
  local packed = pack(protocols.P_Info, payload)
  local noise = ebuff.from_bytes("AABBCC")
  local merged = ebuff.merge(noise, packed.value)
  write_buff(channels.upper, merged)
  etimer.delay(200)

  validate_response("冗余字段", {
    sex = normalize_gender(payload.sex),
    height = clamp(payload.height, 0, 200),
    weight = clamp(payload.weight, 0, 150),
    age = clamp(payload.age, 0, 100),
  })
end

function entry()
  clear(channels.upper)
  etimer.delay(200)

  local seconds = calc_current_seconds()

  expect_drop("包头错误", { header = 0xFFFF, currentSecends = seconds, sex = 2, height = 175, weight = 68, age = 30 })
  expect_drop("数据长度错误", { len = 0x0B, currentSecends = seconds, sex = 2, height = 175, weight = 68, age = 30 })
  expect_drop("校验和错误", { check_code = 0x00, currentSecends = seconds, sex = 1, height = 165, weight = 55, age = 25 })
  expect_drop("包尾错误", { tail = 0xABCD, currentSecends = seconds, sex = 1, height = 165, weight = 55, age = 25 })

  expect_accept_with_noise(seconds)

  local payload = {
    currentSecends = seconds,
    sex = 0,
    height = 210,
    weight = 160,
    age = -5,
  }
  write_msg(channels.upper, protocols.P_Info, payload)
  etimer.delay(200)
  validate_response("边界裁剪", {
    sex = normalize_gender(payload.sex),
    height = clamp(payload.height, 0, 200),
    weight = clamp(payload.weight, 0, 150),
    age = clamp(payload.age, 0, 100),
  })

  clear(channels.upper)
  etimer.delay(500)

  exit()
end
