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

local function manual_confirm(level, calorie, gender)
  local genderText = gender == FEMALE and "女性" or "男性"
  local msg = string.format(
    "运动级别：%d\n性别：%s\n期望显示的每分钟卡路里：%.2f\n若手表显示一致请选择‘是’。",
    level,
    genderText,
    calorie
  )
  local confirmed = ask("yesno", {
    title = "手动确认卡路里显示",
    msg = msg,
    default = true,
  })

  check(confirmed,
        string.format("✅ Level %d 手动确认通过", level),
        string.format("❌ Level %d 手动确认失败，请核对显示数据", level))
end

local function expect_response(level, calorie)
  local response = read_msg(channels.upper, protocols.P_calc, 800)
  if not response or type(response.value) ~= "table" then
    return nil
  end

  local value = response.value
  local recvLevel = tonumber(value.level or value.Level)
  local recvCalorie = tonumber(value.calorie or value.Calorie)
  local delta = math.abs((recvCalorie or 0) - calorie)

  local levelOk = recvLevel == level
  local calorieOk = delta <= math.max(0.05 * math.max(1, calorie), 0.5)

  check(levelOk,
        string.format("✅ Level %d 返回的运动级别正确", level),
        string.format("❌ Level %d 返回的运动级别错误，期望 %d，实际 %s", level, level, tostring(value.level)))

  check(calorieOk,
        string.format("✅ Level %d 卡路里结果在可接受范围内 (%.2f)", level, recvCalorie or -1),
        string.format("❌ Level %d 卡路里结果偏差过大，期望 %.2f，实际 %s", level, calorie, tostring(value.calorie)))

  return true
end

local function send_calorie_for_info(info, baseDelay)
  local bmr, gender = calc_bmr(info)
  local factors = ACTIVITY_FACTOR[gender]
  for level = 0, 4 do
    local factor = factors[level + 1]
    local calorie = round2(bmr * factor)
    write_msg(channels.upper, protocols.P_calc, {
      level = level,
      calorie = calorie,
    })
    etimer.delay(baseDelay)

    local ok = expect_response(level, calorie)
    if not ok then
      manual_confirm(level, calorie, gender)
    end

    etimer.delay(200)
  end
end

local function send_invalid_packets()
  print("🚧 注入异常卡路里帧，验证容错能力")
  local invalidCases = {
    { desc = "包头错误", payload = { header = 0x0000, level = 1, calorie = 120.5 } },
    { desc = "数据长度错误", payload = { len = 0x01, level = 2, calorie = 150.0 } },
    { desc = "校验和错误", payload = { check_code = 0x00, level = 3, calorie = 180.0 } },
    { desc = "包尾错误", payload = { tail = 0x0000, level = 4, calorie = 200.0 } },
  }

  for _, case in ipairs(invalidCases) do
    print(string.format("    -> %s", case.desc))
    write_msg(channels.upper, protocols.P_calc, case.payload)
    etimer.delay(200)
  end

  local packed = pack(protocols.P_calc, { level = 2, calorie = 160.0 })
  local noise = ebuff.from_bytes("55AA")
  local merged = ebuff.merge(noise, packed.value)
  print("    -> 包前冗余字段")
  write_buff(channels.upper, merged)
  etimer.delay(200)
end

function entry()
  ask("ok", { msg = "请保持手表处于运动状态，准备接收每分钟消耗卡路里数据。" })

  clear(channels.upper)
  etimer.delay(200)

  local infos = test_data.info or {}
  if #infos == 0 then
    print("⚠️ 未从数据文件读取到用户信息，使用默认样本。")
    infos = {
      { gender = 2, age = 25, height = 175, weight = 70 },
      { gender = 1, age = 28, height = 165, weight = 55 },
    }
  end

  for index, info in ipairs(infos) do
    print(string.format("🚀 开始执行卡路里案例 %d", index))
    send_calorie_for_info(info, 150)
    etimer.delay(300)
  end

  send_invalid_packets()

  clear(channels.upper)
  etimer.delay(500)

  print("✅ 卡路里输入接口验证结束。")
  exit()
end
