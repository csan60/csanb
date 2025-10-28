local BASE_TIME = os.time({ year = 2000, month = 1, day = 1, hour = 0, min = 0, sec = 0 }) or 0
local LIMIT_TIME = os.time({ year = 2100, month = 1, day = 1, hour = 0, min = 0, sec = 0 })
local MAX_SECONDS = 0xFFFFFFFFFFFF
if BASE_TIME and LIMIT_TIME then
  MAX_SECONDS = math.max(LIMIT_TIME - BASE_TIME, 0)
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

local function clamp_seconds(value)
  local seconds = tonumber(value) or 0
  if seconds < 0 then
    return 0
  end
  if seconds > MAX_SECONDS then
    return MAX_SECONDS
  end
  return seconds
end

local function normalize_gender(gender)
  if tonumber(gender) == 1 then
    return 1
  end
  return 2
end

local function calc_current_seconds()
  local now = os.time()
  if not BASE_TIME or not now then
    return 0
  end
  return clamp_seconds(now - BASE_TIME)
end

local function read_info_response(timeout)
  local response = read_msg(channels.upper, protocols.P_Info, timeout or 500)
  if response and type(response.value) == "table" then
    return response.value
  end
  return nil
end

local function verify_frame(caseIndex, expected, response)
  local sex = tonumber(response.sex or response.Sex)
  check(sex == expected.sex,
        string.format("✅ 案例 %d 性别处理正确", caseIndex),
        string.format("❌ 案例 %d 性别处理错误，期望 %d，实际 %s", caseIndex, expected.sex, tostring(response.sex)))

  local height = tonumber(response.height or response.Height)
  check(height == expected.height,
        string.format("✅ 案例 %d 身高处理正确", caseIndex),
        string.format("❌ 案例 %d 身高处理错误，期望 %d，实际 %s", caseIndex, expected.height, tostring(response.height)))

  local weight = tonumber(response.weight or response.Weight)
  check(weight == expected.weight,
        string.format("✅ 案例 %d 体重处理正确", caseIndex),
        string.format("❌ 案例 %d 体重处理错误，期望 %d，实际 %s", caseIndex, expected.weight, tostring(response.weight)))

  local age = tonumber(response.age or response.Age)
  check(age == expected.age,
        string.format("✅ 案例 %d 年龄处理正确", caseIndex),
        string.format("❌ 案例 %d 年龄处理错误，期望 %d，实际 %s", caseIndex, expected.age, tostring(response.age)))
end

local function send_and_verify(caseIndex, payload, expected)
  write_msg(channels.upper, protocols.P_Info, payload)
  etimer.delay(300)
  local response = read_info_response(800)

  check(response ~= nil,
        string.format("✅ 案例 %d 收到设置信息的回传数据", caseIndex),
        string.format("❌ 案例 %d 未收到设置信息回传，无法验证", caseIndex))

  if response then
    verify_frame(caseIndex, expected, response)
  end
end

function entry()
  clear(channels.upper)
  etimer.delay(200)

  local baseSeconds = calc_current_seconds()

  local cases = {}
  for _, info in ipairs(test_data.testdata or {}) do
    table.insert(cases, {
      label = "数据文件案例",
      gender = info.gender,
      height = info.height,
      weight = info.weight,
      age = info.age,
      overrideSeconds = nil,
    })
  end

  table.insert(cases, {
    label = "时间上界裁剪",
    gender = 1,
    height = 165,
    weight = 55,
    age = 25,
    overrideSeconds = clamp_seconds(MAX_SECONDS + 86400),
  })

  table.insert(cases, {
    label = "时间下界裁剪",
    gender = 2,
    height = 175,
    weight = 70,
    age = 35,
    overrideSeconds = clamp_seconds(-3600),
  })

  for index, info in ipairs(cases) do
    local payloadSeconds = info.overrideSeconds or baseSeconds
    local expected = {
      sex = normalize_gender(info.gender),
      height = clamp(info.height, 0, 200),
      weight = clamp(info.weight, 0, 150),
      age = clamp(info.age, 0, 100),
      currentSecends = clamp_seconds(payloadSeconds),
    }

    local payload = {
      currentSecends = payloadSeconds,
      sex = info.gender,
      height = info.height,
      weight = info.weight,
      age = info.age,
    }

    send_and_verify(index, payload, expected)
    etimer.delay(150)
  end

  clear(channels.upper)
  etimer.delay(400)

  exit()
end
