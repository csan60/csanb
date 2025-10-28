local MALE = 2
local FEMALE = 1

local ACTIVITY_FACTOR = {
  [MALE] = { 1.55, 1.64, 1.78, 1.90, 2.10 },
  [FEMALE] = { 1.42, 1.53, 1.64, 1.73, 1.82 },
}

local function round2(number)
  return math.floor(number * 100 + 0.5) / 100
end

local function calc_bmr(info)
  local gender = tonumber(info.gender) == FEMALE and FEMALE or MALE
  local weight = tonumber(info.weight) or 0
  local height = tonumber(info.height) or 0
  local age = tonumber(info.age) or 0

  if gender == FEMALE then
    return 65.5 + 9.6 * weight + 1.8 * height - 4.7 * age, gender
  end
  return 66 + 13.7 * weight + 5 * height - 6.8 * age, gender
end

local function expect_response(level, calorie)
  local response = read_msg(channels.upper, protocols.P_calc, 800)
  if not response or type(response.value) ~= "table" then
    print(string.format("⚠️ 未收到 level=%d 的卡路里回传", level))
    return
  end

  local value = response.value
  local recvLevel = tonumber(value.level or value.Level)
  local recvCalorie = tonumber(value.calorie or value.Calorie)
  local delta = math.abs((recvCalorie or 0) - calorie)

  check(recvLevel == level,
        string.format("✅ Level %d 返回的运动级别正确", level),
        string.format("❌ Level %d 返回的运动级别错误，期望 %d，实际 %s", level, level, tostring(value.level)))

  check(delta <= math.max(0.05 * math.max(1, calorie), 0.5),
        string.format("✅ Level %d 卡路里结果在可接受范围内 (%.2f)", level, recvCalorie or -1),
        string.format("❌ Level %d 卡路里结果偏差过大，期望 %.2f，实际 %s", level, calorie, tostring(value.calorie)))
end

local function send_calorie_for_info(info)
  local bmr, gender = calc_bmr(info)
  local factors = ACTIVITY_FACTOR[gender]
  for level = 0, 4 do
    local factor = factors[level + 1]
    local calorie = round2(bmr * factor)
    write_msg(channels.upper, protocols.P_calc, {
      level = level,
      calorie = calorie,
    })
    etimer.delay(150)
    expect_response(level, calorie)
  end
end

function entry()
  clear(channels.upper)
  etimer.delay(200)

  local infos = test_data.info or {}
  for index, info in ipairs(infos) do
    print(string.format("🚀 开始执行卡路里案例 %d", index))
    send_calorie_for_info(info)
    etimer.delay(200)
  end

  clear(channels.upper)
  etimer.delay(500)

  exit()
end
