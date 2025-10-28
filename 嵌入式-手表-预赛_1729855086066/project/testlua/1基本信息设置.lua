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

local function read_info_response(timeout)
  local response = read_msg(channels.upper, protocols.P_Info, timeout or 500)
  if response and type(response.value) == "table" then
    return response.value
  end
  return nil
end

local function verify_info_case(caseIndex, info, currentSeconds)
  local expected = {
    sex = normalize_gender(info.gender),
    height = clamp(info.height, 0, 200),
    weight = clamp(info.weight, 0, 150),
    age = clamp(info.age, 0, 100),
    currentSecends = currentSeconds,
  }

  local result = read_info_response(800)
  if not result then
    print(string.format("⚠️ 案例 %d 未收到设置信息的回传数据", caseIndex))
    return
  end

  local sex = tonumber(result.sex or result.Sex)
  check(sex == expected.sex,
        string.format("✅ 案例 %d 性别处理正确", caseIndex),
        string.format("❌ 案例 %d 性别处理错误，期望 %d，实际 %s", caseIndex, expected.sex, tostring(result.sex)))

  local height = tonumber(result.height or result.Height)
  check(height == expected.height,
        string.format("✅ 案例 %d 身高处理正确", caseIndex),
        string.format("❌ 案例 %d 身高处理错误，期望 %d，实际 %s", caseIndex, expected.height, tostring(result.height)))

  local weight = tonumber(result.weight or result.Weight)
  check(weight == expected.weight,
        string.format("✅ 案例 %d 体重处理正确", caseIndex),
        string.format("❌ 案例 %d 体重处理错误，期望 %d，实际 %s", caseIndex, expected.weight, tostring(result.weight)))

  local age = tonumber(result.age or result.Age)
  check(age == expected.age,
        string.format("✅ 案例 %d 年龄处理正确", caseIndex),
        string.format("❌ 案例 %d 年龄处理错误，期望 %d，实际 %s", caseIndex, expected.age, tostring(result.age)))
end

function entry()
  clear(channels.upper)
  etimer.delay(200)

  local currentSeconds = calc_current_seconds()
  local cases = test_data.testdata or {}

  for index, info in ipairs(cases) do
    local payload = {
      currentSecends = currentSeconds,
      sex = info.gender,
      height = info.height,
      weight = info.weight,
      age = info.age,
    }

    write_msg(channels.upper, protocols.P_Info, payload)
    etimer.delay(200)

    verify_info_case(index, info, currentSeconds)
    etimer.delay(100)
  end

  clear(channels.upper)
  etimer.delay(500)

  exit()
end
